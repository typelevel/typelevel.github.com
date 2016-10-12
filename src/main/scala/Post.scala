package org.typelevel.blog

import coursier._
import java.io.{File, FileInputStream}
import java.nio.file.{Files, StandardCopyOption}
import org.yaml.snakeyaml.Yaml
import scala.util.Try

case class Post(file: File) {

  lazy val frontMatter: Option[FrontMatter] = Try {
    val yaml = new Yaml()
    val stream = new FileInputStream(file)
    val any = yaml.loadAll(stream).iterator.next()
    stream.close()
    any
  }.flatMap(YAML.decodeTo[FrontMatter]).toOption

  lazy val out: File =
    new File(BuildInfo.tutOutput.toString + File.separator + file.getName)

  def outdated(): Boolean =
    !(out.exists() && out.isFile() && file.lastModified() <= out.lastModified())

  def process(): Unit =
    if (outdated()) {
      println(s"[blog] Processing ${file.getName} ...")
      BuildInfo.tutOutput.mkdirs()

      frontMatter match {
        case Some(FrontMatter(tut)) => 
          tut.invoke(file)
        case None =>
          println("[blog] No tut header, copying.")
          Files.copy(file.toPath, out.toPath, StandardCopyOption.REPLACE_EXISTING)
      }
    }
    else {
      println(s"[blog] Skipping ${file.getName} (up to date).")
    }

}
