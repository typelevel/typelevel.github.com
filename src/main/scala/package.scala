package org.typelevel.blog

import coursier._
import java.io.File
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}
import scalaz.\/
import scalaz.concurrent.Task
import scalaz.std.list._
import scalaz.syntax.traverse._

object `package` {

  val repositories = Seq(
    Cache.ivy2Local,
    MavenRepository("https://repo1.maven.org/maven2"),
    MavenRepository("https://dl.bintray.com/tpolecat/maven/")
  )

  implicit def eitherToTry[T](e: Throwable \/ T): Try[T] =
    e.fold(Failure(_), Success(_))

  def resolve(start: Resolution): Try[List[File]] = {
    val logger = new Cache.Logger {
      override def downloadingArtifact(url: String, file: File) =
        println(s"[blog] Downloading artifact from $url ...")
      override def downloadedArtifact(url: String, success: Boolean) = {
        val file = url.split('/').last
        if (success)
          println(s"[blog] Successfully downloaded $file")
        else
          println(s"[blog] Failed to download $file")
      }
    }

    val fetch = Fetch.from(repositories, Cache.fetch(logger = Some(logger)))

    start.process.run(fetch).unsafePerformSyncAttempt.flatMap { resolution =>
      if (!resolution.isDone)
        \/.left(new RuntimeException("resolution did not converge"))
      else if (!resolution.conflicts.isEmpty)
        \/.left(new RuntimeException(s"resolution has conflicts: ${resolution.conflicts.mkString(", ")}"))
      else if (!resolution.errors.isEmpty)
        \/.left(new RuntimeException(s"resolution has errors: ${resolution.errors.mkString(", ")}"))
      else {
        Task.gatherUnordered(
          resolution.artifacts.map(artifact =>
            Cache.file(artifact, logger = Some(logger)).run
          )
        ).unsafePerformSyncAttempt.map(_.sequenceU).flatMap(_.leftMap(err => new RuntimeException(err.describe)))
      }
    }
  }

}
