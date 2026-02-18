//> using scala 3.6.3
//> using dep org.virtuslab::scala-yaml::0.3.1
//> using dep co.fs2::fs2-io::3.12.2

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import org.virtuslab.yaml.*

case class PostMeta(author: Option[String]) derives YamlCodec

case class Conf(title: String, category: Option[String], meta: Option[PostMeta])
    derives YamlCodec

case class Post(conf: Conf, content: String, originalYaml: String) {

  def cleanPostUrl(markdown: String): String = {
    // Replace {% post_url YYYY-MM-DD-filename %} with filename.md
    val postUrlPattern = """\{%\s*post_url\s+\d{4}-\d{2}-\d{2}-(.+?)\s*%\}""".r
    postUrlPattern.replaceAllIn(markdown, "$1.md")
  }

  def cleanOtherLinks(markdown: String): String = {
    var cleaned = markdown

    // Replace absolute typelevel.org blog URLs: https://typelevel.org/blog/YYYY/MM/DD/post-name.html with post-name.md
    val typelevelBlogPattern =
      """https://typelevel\.org/blog/\d{4}/\d{2}/\d{2}/([^)\s]+)\.html""".r
    cleaned = typelevelBlogPattern.replaceAllIn(cleaned, "$1.md")

    // Replace relative blog URLs: /blog/YYYY/MM/DD/post-name.html with post-name.md
    val relativeBlogPattern =
      """(?<![a-z])/blog/\d{4}/\d{2}/\d{2}/([^)\s]+)\.html""".r
    cleaned = relativeBlogPattern.replaceAllIn(cleaned, "$1.md")

    // Replace Jekyll site.url variables: {{ site.url }}/... with /...
    val siteUrlPattern = """\{\{\s*site\.url\s*\}\}""".r
    cleaned = siteUrlPattern.replaceAllIn(cleaned, "")

    // Replace .html extensions with .md in relative links (but not absolute URLs starting with http)
    val htmlToMdPattern = """(?<!https?://[^\s)]*)(\.html)""".r
    cleaned = htmlToMdPattern.replaceAllIn(cleaned, ".md")

    // Fix links
    cleaned = cleaned.replace("/conduct.md", "/code-of-conduct/README.md")
    cleaned =
      cleaned.replace("/code-of-conduct.md", "/code-of-conduct/README.md")
    cleaned = cleaned.replace("/projects", "/projects/README.md")
    cleaned = cleaned.replace("/gsoc/ideas", "/gsoc/ideas.md")

    cleaned
  }

  def buildHoconMetadata(date: String): String = {
    val authorLine = conf.meta.flatMap(_.author).map(a => s"  author: $${$a}")
    val dateLine = Some(s"""  date: "$date"""")
    val tagsLine = conf.category.map(c => s"  tags: [$c]")

    List(
      Some("{%"),
      authorLine,
      dateLine,
      tagsLine,
      Some("%}")
    ).flatten.mkString("\n")
  }

  def toLaika(date: String, stage: Int): String = {
    val metadata = buildHoconMetadata(date)
    val title = s"# ${conf.title}"

    stage match {
      case 1 =>
        // Stage 1: Just move to new location, keep original format
        s"---\n$originalYaml---\n\n$content\n"

      case 2 =>
        // Stage 2: HOCON metadata + title, no content changes
        s"$metadata\n\n$title\n\n$content\n"

      case 3 =>
        // Stage 3: Stage 2 + post_url substitution
        val transformedContent = cleanPostUrl(content)
        s"$metadata\n\n$title\n\n$transformedContent\n"

      case _ =>
        // Stage 4+: All transformations
        val transformedContent = cleanOtherLinks(cleanPostUrl(content))
        s"$metadata\n\n$title\n\n$transformedContent\n"
    }
  }
}

object PostParser {
  def parse(path: Path, content: String): Either[Throwable, Post] = {
    // Normalize Windows line endings to Unix
    val normalized = content.replace("\r\n", "\n")
    val parts = normalized.split("---\n", 3)
    if (parts.length < 3) {
      val fn = path.fileName
      Left(new Exception(s"Invalid post '$fn': no YAML front matter found"))
    } else {
      val yamlContent = parts(1)
      val markdownContent = parts(2).trim
      yamlContent.as[Conf].map(conf => Post(conf, markdownContent, yamlContent))
    }
  }
}

object MigratePosts extends IOApp {
  val oldPostsDir = Path("../typelevel.github.com/collections/_posts")
  val newBlogDir = Path("src/blog")

  // Manual renaming map for files that would collide after date stripping
  val renameMap: Map[String, String] = Map(
    "2023-02-23-gsoc.md" -> "gsoc-2023.md",
    "2024-03-02-gsoc.md" -> "gsoc-2024.md",
    "2025-02-27-gsoc.md" -> "gsoc-2025.md"
  )

  def getDateAndName(path: Path): Either[Throwable, (String, String)] = {
    val filename = path.fileName.toString
    val datePattern = """(\d{4}-\d{2}-\d{2})-(.+)""".r
    filename match {
      case datePattern(date, rest) =>
        val newName = renameMap.getOrElse(filename, rest)
        Right((date, newName))
      case _ =>
        Left(new Exception(s"Filename doesn't match pattern: $filename"))
    }
  }

  def readPost(path: Path): IO[String] = Files[IO]
    .readAll(path)
    .through(fs2.text.utf8.decode)
    .compile
    .string

  def writePost(path: Path, content: String): IO[Unit] = fs2.Stream
    .emit(content)
    .through(fs2.text.utf8.encode)
    .through(Files[IO].writeAll(path))
    .compile
    .drain

  def migratePost(sourcePath: Path, stage: Int): IO[String] = for {
    (date, newFilename) <- IO.fromEither(getDateAndName(sourcePath))
    content <- readPost(sourcePath)
    post <- IO.fromEither(PostParser.parse(sourcePath, content))
    laikaContent = post.toLaika(date, stage)
    destPath = newBlogDir / newFilename
    _ <- writePost(destPath, laikaContent)
  } yield newFilename

  def migrateAllPosts(stage: Int): IO[Long] = Files[IO]
    .list(oldPostsDir)
    .filter(_.fileName.toString.matches("""^\d{4}-\d{2}-\d{2}-.+\.md$"""))
    .evalMap(path => migratePost(path, stage))
    .evalMap(newFilename => IO.println(s"Migrated: $newFilename"))
    .compile
    .count

  def run(args: List[String]): IO[cats.effect.ExitCode] = {
    val stage = args.headOption.flatMap(_.toIntOption).getOrElse(4)
    IO.println(s"Running migration with stage $stage") *>
      migrateAllPosts(stage)
        .flatMap(c => IO.println(s"Migrated $c posts"))
        .as(cats.effect.ExitCode.Success)
  }
}
