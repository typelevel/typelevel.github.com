scalaVersion := "2.11.8"

name := "typelevel-blog-tut"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "1.0.0-M14",
  "io.get-coursier" %% "coursier-cache" % "1.0.0-M14",
  "com.chuusai" %% "shapeless" % "2.3.0",
  "org.yaml" % "snakeyaml" % "1.17"
)

lazy val tutInput = SettingKey[File]("tutInput")
lazy val tutOutput = SettingKey[File]("tutOutput")
lazy val tutVersion = SettingKey[String]("tutVersion")

tutInput := (baseDirectory in ThisBuild).value / "posts"
tutOutput := (baseDirectory in ThisBuild).value / "_posts"
tutVersion := "0.4.4"

watchSources ++= (tutInput.value ** "*.md").get

cleanFiles += tutOutput.value
cleanFiles += (baseDirectory in ThisBuild).value / "_site"

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](tutInput, tutOutput, tutVersion)

buildInfoPackage := "org.typelevel.blog"
