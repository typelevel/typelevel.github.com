//> using scala 3.6.3
//> using dep org.virtuslab::scala-yaml::0.3.1
//> using dep co.fs2::fs2-io::3.12.2
//> using dep com.typesafe:config:1.4.5

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import org.virtuslab.yaml.*
import com.typesafe.config.{Config, ConfigFactory}
import java.time.LocalDate
import scala.jdk.CollectionConverters.*

case class ScheduleItem(
    time: String,
    title: String,
    speakers: Option[List[String]],
    summary: Option[String]
) derives YamlCodec

case class Sponsor(
    name: String,
    logo: String,
    link: String,
    `type`: String,
    height: Option[Int]
) derives YamlCodec

case class Meta(meetup: Option[String]) derives YamlCodec

case class EventConfig(
    title: String,
    short_title: Option[String],
    date_string: String,
    location: String,
    description: String,
    poster_hero: Option[String],
    poster_thumb: Option[String],
    schedule: Option[List[ScheduleItem]],
    sponsors: Option[List[Sponsor]],
    meta: Option[Meta]
) derives YamlCodec

case class Event(conf: EventConfig, content: String, originalYaml: String) {

  def loadSpeakerDirectory(): Map[String, String] =
    try {
      val config = ConfigFactory.parseFile(
        Path("src/blog/directory.conf").toNioPath.toFile
      )
      val speakerNames = config
        .root()
        .keySet()
        .asScala
        .toList
        .map { key =>
          key -> config.getConfig(key).getString("name")
        }
        .toMap
      speakerNames
    } catch {
      case _: Exception => Map.empty[String, String]
    }

  def cleanOtherLinks(markdown: String): String = {
    var cleaned = markdown

    // https://typelevel.org/blog/YYYY/MM/DD/post-name.html -> post-name.md
    val typelevelBlogPattern =
      """https://typelevel\.org/blog/\d{4}/\d{2}/\d{2}/([^)\s]+)\.html""".r
    cleaned = typelevelBlogPattern.replaceAllIn(cleaned, "$1.md")

    // /blog/YYYY/MM/DD/post-name.html -> post-name.md
    val relativeBlogPattern =
      """(?<![a-z])/blog/\d{4}/\d{2}/\d{2}/([^)\s]+)\.html""".r
    cleaned = relativeBlogPattern.replaceAllIn(cleaned, "$1.md")

    val siteUrlPattern = """\{\{\s*site\.url\s*\}\}""".r
    cleaned = siteUrlPattern.replaceAllIn(cleaned, "")

    // Replace {{ page.meta.meetup }} with just the URL
    val meetupPattern = """\{\{\s*page\.meta\.meetup\s*\}\}""".r
    cleaned = conf.meta.flatMap(_.meetup) match {
      case Some(meetupUrl) => meetupPattern.replaceAllIn(cleaned, meetupUrl)
      case None            => meetupPattern.replaceAllIn(cleaned, "")
    }

    // Replace .html extensions with .md in relative links (but not absolute URLs starting with http)
    val htmlToMdPattern = """(?<!https?://[^\s)]*)(\\.html)""".r
    cleaned = htmlToMdPattern.replaceAllIn(cleaned, ".md")

    cleaned = cleaned.replace("/projects", "/projects/README.md")
    cleaned = cleaned.replace("/conduct.html", "/code-of-conduct/README.md")
    cleaned =
      cleaned.replace("/code-of-conduct.html", "/code-of-conduct/README.md")

    cleaned
  }

  def buildHoconMetadata(date: String, eventDate: String, eventLocation: String, tags: List[String]): String =
    s"""|{%
        |  laika.html.template: event.template.html
        |  date: "$date"
        |  event-date: "$eventDate"
        |  event-location: "$eventLocation"
        |  tags: ${tags.mkString("[", ", ", "]")}
        |%}""".stripMargin

  def generateScheduleMarkdown(): String = conf.schedule
    .map { scheduleItems =>
      val tableRows = scheduleItems
        .map { item =>
          val timeColumn = item.time

          val talkColumn = if (item.speakers.isEmpty) {
            item.title
          } else {
            item.speakers
              .map { speakers =>
                val speakerDirectory = loadSpeakerDirectory()
                val speakerNames = speakers
                  .map(s => speakerDirectory.getOrElse(s, s))
                  .mkString(", ")
                item.summary match
                  case Some(value) =>
                    s"@:style(schedule-title)${item.title}@:@ @:style(schedule-byline)${speakerNames}@:@ ${value}"
                  case None => s"**${item.title}**<br/>${speakerNames}"
              }
              .getOrElse(item.title)
          }

          s"| ${timeColumn} | ${talkColumn} |"
        }
        .mkString("\n")

      s"""|| Time | Talk |
        ||------|------|
        |$tableRows""".stripMargin
    }
    .getOrElse("")

  def generateSponsorsHtml(): String = {
    conf.sponsors
      .map { sponsors =>
        val sponsorsByType = sponsors.groupBy(_.`type`)

        val sections = List("platinum", "gold", "silver").flatMap {
          sponsorType =>
            sponsorsByType.get(sponsorType).map { typeSponsors =>
              val sponsorCells = typeSponsors
                .map { sponsor =>
                  s"@:style(bulma-cell bulma-has-text-centered)[@:image(${sponsor.logo}) { alt: ${sponsor.name}, title: ${sponsor.name}, style: legacy-event-sponsor }](${sponsor.link})@:@"
                }
                .mkString("\n")

              s"""|### ${sponsorType.capitalize}
                  |@:style(bulma-grid bulma-is-col-min-12)
                  |$sponsorCells
                  |@:@""".stripMargin
            }
        }

        sections.mkString("\n\n")
      }
      .getOrElse("")
  }

  def toLaika(date: String, stage: Int): String = {
    val tags = Option.when(conf.title.contains("Summit"))("summits").toList ::: "events" :: Nil
    val metadata = buildHoconMetadata(date, conf.date_string, conf.location, tags)
    val title = s"# ${conf.title}"
    val image =
      conf.poster_hero.map(img => s"![${conf.title}]($img)").getOrElse("")

    stage match {
      case 1 =>
        // Stage 1: Just move to new location, keep original format
        s"---\n$originalYaml---\n\n$content\n"

      case 2 =>
        // Stage 2: HOCON metadata + title, no content changes
        s"$metadata\n\n$title\n\n$image\n\n$content\n"

      case 3 =>
        // Stage 3: Stage 2 + link cleaning
        val transformedContent = cleanOtherLinks(content)
        s"$metadata\n\n$title\n\n$image\n\n$transformedContent\n"

      case _ =>
        // Stage 4+: Use original content and replace Jekyll includes with generated HTML
        val transformedContent = cleanOtherLinks(content)

        // Replace Jekyll includes with generated HTML
        var processedContent = transformedContent

        // Remove schedule assign and replace schedule include
        val scheduleAssignPattern =
          """\{\%\s*assign\s+schedule\s*=\s*page\.schedule\s*%\}\s*""".r
        processedContent =
          scheduleAssignPattern.replaceAllIn(processedContent, "")

        val schedulePattern = """\{\%\s*include\s+schedule\.html\s*%\}""".r
        val scheduleReplacement =
          if (conf.schedule.isDefined) generateScheduleMarkdown() else ""
        processedContent =
          schedulePattern.replaceAllIn(processedContent, scheduleReplacement)

        // Replace sponsors include
        val sponsorsPattern = """\{\%\s*include\s+sponsors\.html\s*%\}""".r
        val sponsorsReplacement =
          if (conf.sponsors.isDefined) generateSponsorsHtml() else ""
        processedContent =
          sponsorsPattern.replaceAllIn(processedContent, sponsorsReplacement)

        // Remove venue_map includes (not supported)
        val venueMapPattern = """\{\%\s*include\s+venue_map\.html\s*%\}""".r
        processedContent = venueMapPattern.replaceAllIn(processedContent, "")

        s"$metadata\n\n$title\n\n$image\n\n$processedContent\n"
    }
  }
}

object EventParser {
  def parse(path: Path, content: String): Either[Throwable, Event] = {
    // Normalize Windows line endings to Unix
    val normalized = content.replace("\r\n", "\n")
    val parts = normalized.split("---\n", 3)
    if (parts.length < 3) {
      val fn = path.fileName
      Left(new Exception(s"Invalid event '$fn': no YAML front matter found"))
    } else {
      val yamlContent = parts(1)
      val markdownContent = parts(2).trim
      yamlContent
        .as[EventConfig]
        .map(conf => Event(conf, markdownContent, yamlContent))
    }
  }
}

object MigrateEvents extends IOApp {
  val oldEventsDir = Path("collections/_events")
  val newBlogDir = Path("src/blog")

  def getDateAndName(path: Path): Either[Throwable, (String, String)] = {
    val filename = path.fileName.toString
    val datePattern = """(\d{4}-\d{2}-\d{2})-(.+)""".r
    filename match {
      case datePattern(date, rest) =>
        Right((date, filename)) // Keep full filename
      case _ =>
        Left(new Exception(s"Filename doesn't match pattern: $filename"))
    }
  }

  def readEvent(path: Path): IO[String] = Files[IO]
    .readAll(path)
    .through(fs2.text.utf8.decode)
    .compile
    .string

  def writeEvent(path: Path, content: String): IO[Unit] = fs2.Stream
    .emit(content)
    .through(fs2.text.utf8.encode)
    .through(Files[IO].writeAll(path))
    .compile
    .drain

  def migrateEvent(sourcePath: Path, stage: Int): IO[String] = for {
    (date, fullFilename) <- IO.fromEither(getDateAndName(sourcePath))
    content <- readEvent(sourcePath)
    event <- IO.fromEither(EventParser.parse(sourcePath, content))
    laikaContent = event.toLaika(date, stage)
    destPath = newBlogDir / fullFilename
    _ <- writeEvent(destPath, laikaContent)
  } yield fullFilename

  def migrateAllEvents(stage: Int): IO[Long] = Files[IO]
    .list(oldEventsDir)
    .filter(_.fileName.toString.matches("""^\d{4}-\d{2}-\d{2}-.+\.md$"""))
    .evalMap(path => migrateEvent(path, stage))
    .evalMap(fullFilename => IO.println(s"Migrated: $fullFilename"))
    .compile
    .count

  def run(args: List[String]): IO[cats.effect.ExitCode] = {
    val stage = args.headOption.flatMap(_.toIntOption).getOrElse(4)
    IO.println(s"Running migration with stage $stage") *>
      migrateAllEvents(stage)
        .flatMap(c => IO.println(s"Migrated $c events"))
        .as(cats.effect.ExitCode.Success)
  }
}
