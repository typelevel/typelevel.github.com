//> using dep org.virtuslab::scala-yaml::0.3.1
//> using dep co.fs2::fs2-io::3.12.2

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.io.file.{Files, Path, Flags}
import org.virtuslab.yaml.*

case class OldAuthor(
    full_name: String,
    twitter: Option[String],
    github: Option[String],
    bio: Option[String]
) derives YamlCodec {
  def toNew(key: String): NewAuthor = {
    // Using GH avatars instead of old `portrait` image for now
    val avatar = github.map(gh => s"https://github.com/$gh.png")
    NewAuthor(
      key = key,
      name = full_name,
      avatar = avatar,
      github = github,
      twitter = twitter,
      bio = bio
    )
  }
}

case class NewAuthor(
    key: String,
    name: String,
    avatar: Option[String],
    github: Option[String],
    twitter: Option[String],
    bio: Option[String]
) {
  def toHocon: String = {
    val avatarLine = avatar.map(av => s"""  avatar: "$av"""")
    val githubLine = github.map(gh => s"  github: $gh")
    val twitterLine = twitter.map(tw => s"  twitter: $tw")
    val bioLine = bio.map(b => s"""  bio: "$b"""")

    val lines = List(
      Some(s"$key {"),
      Some(s"""  name: "$name""""),
      avatarLine,
      githubLine,
      twitterLine,
      bioLine,
      Some("}")
    ).flatten

    lines.mkString("\n") + "\n"
  }
}

object MigrateAuthors extends IOApp.Simple {
  val authorsYamlPath = Path("_data/authors.yml")
  val directoryConfPath = Path("src/blog/directory.conf")
  val alreadyMigrated = Set(
    "armanbilge",
    "djspiewak",
    "jducoeur",
    "valencik",
    "samspills",
    "lukajcb",
    "mpilquist",
    "satabin",
    "hkateu",
    "bpholt",
    "rossabaker",
    "typelevel",
    "foundation"
  )

  def readAuthorsYaml: IO[String] = Files[IO]
    .readAll(authorsYamlPath)
    .through(fs2.text.utf8.decode)
    .compile
    .string

  def appendToDirectoryConf(content: String): IO[Unit] = fs2.Stream
    .emit(content)
    .through(fs2.text.utf8.encode)
    .through(Files[IO].writeAll(directoryConfPath, Flags.Append))
    .compile
    .drain

  def run: IO[Unit] = for {
    yamlContent <- readAuthorsYaml
    authorsMap <- IO.fromEither(yamlContent.as[Map[String, OldAuthor]])
    newAuthors = authorsMap.toList
      .filterNot { case (key, _) => alreadyMigrated.contains(key) }
      .sortBy(_._1)
      .map { case (key, oldAuthor) => oldAuthor.toNew(key) }
    hoconContent = "\n" + newAuthors.map(_.toHocon).mkString("\n")

    _ <- appendToDirectoryConf(hoconContent)
    _ <- IO.println(s"Migrated ${newAuthors.size} authors")
  } yield ()
}
