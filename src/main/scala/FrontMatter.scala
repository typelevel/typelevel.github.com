package org.typelevel.blog

import coursier._
import coursier.util.Parse
import java.io.File
import java.net.URLClassLoader

case class Tut(scala: String, binaryScala: String, dependencies: List[String]) {

  val tutResolution: Resolution = Resolution(Set(
    Dependency(Module("org.tpolecat", s"tut-core_$binaryScala"), BuildInfo.tutVersion)
  ))

  val libResolution: Resolution = Resolution(dependencies.map { dep =>
    val (mod, v) = Parse.moduleVersion(dep, binaryScala).right.get
    Dependency(mod, v)
  }.toSet)

  def invoke(file: File): Unit = {
    val tutClasspath = resolve(tutResolution).get
    val libClasspath = resolve(libResolution).get

    val classLoader = new URLClassLoader(tutClasspath.map(_.toURI.toURL).toArray, null)
    val tutClass = classLoader.loadClass("tut.TutMain")
    val tutMain = tutClass.getDeclaredMethod("main", classOf[Array[String]])

    val commandLine = Array(
      file.toString,
      BuildInfo.tutOutput.toString,
      ".*",
      "-classpath",
      libClasspath.mkString(File.pathSeparator)
    )

    tutMain.invoke(null, commandLine)
  }
}

case class FrontMatter(tut: Tut)
