package org.typelevel.blog

import coursier._
import coursier.util.Parse
import java.io.File
import java.net.URLClassLoader
import scala.concurrent.{ExecutionContext, Future}

case class Tut(
  scala: String,
  binaryScala: String,
  dependencies: List[String],
  scalacOptions: List[String],
  plugins: List[String]
) {

  val tutResolution: Resolution = Resolution(Set(
    Dependency(Module("org.tpolecat", s"tut-core_$binaryScala"), BuildInfo.tutVersion)
  ))

  private def mkDependencies(strs: List[String]): Set[Dependency] =
    strs.map { dep =>
      val (mod, v) = Parse.moduleVersion(dep, binaryScala).right.get
      Dependency(mod, v)
    }.toSet

  val allDependencies: Set[Dependency] = mkDependencies(dependencies) + Dependency(Module("org.scala-lang", "scala-library"), scala)

  val libResolution: Resolution = Resolution(allDependencies)

  val pluginExclusions = Set("scala-compiler", "scala-library", "scala-reflect").map(("org.scala-lang", _))

  val pluginDependencies: Set[Dependency] = mkDependencies(plugins).map(_.copy(exclusions = pluginExclusions))

  val pluginResolution: Resolution = Resolution(pluginDependencies)

  def invoke(file: File)(implicit ec: ExecutionContext): Future[Unit] = {
    val tutClasspath = resolve(tutResolution).get
    val libClasspath = resolve(libResolution).get
    val pluginClasspath = resolve(pluginResolution).get

    val classLoader = new URLClassLoader(tutClasspath.map(_.toURI.toURL).toArray, null)
    val tutClass = classLoader.loadClass("tut.TutMain")
    val tutMain = tutClass.getDeclaredMethod("main", classOf[Array[String]])

    val commandLine = Array(
      file.toString,
      BuildInfo.tutOutput.toString,
      ".*",
      "-classpath",
      libClasspath.mkString(File.pathSeparator),
      s"-Xplugin:${pluginClasspath.mkString(File.pathSeparator)}"
    ) ++ scalacOptions

    Future {
      tutMain.invoke(null, commandLine)
    }
  }
}

case class FrontMatter(tut: Tut)
