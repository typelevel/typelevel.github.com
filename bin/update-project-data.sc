#!/usr/bin/env amm

import $ivy.`io.circe::circe-yaml:0.12.0`
import $ivy.`io.circe::circe-parser:0.12.3`
import $ivy.`com.lihaoyi::requests:0.4.7`

import io.circe.yaml
import io.circe.parser
import io.circe._

val content = scala.io.Source.fromFile("_data/projects.yml").mkString

def fetchDescription(org: String, repo: String): String = {
  val url = s"https://api.github.com/repos/$org/$repo"
  val resp = requests.get(url)
  if (resp.is2xx) {
    val js = parser.parse(resp.text).toTry.get
    val obj = js.asObject.getOrElse(throw new Exception(s"$js is not a JSON object"))
    obj("description").get.asString.get
  } else throw new Exception(s"Non 2xx response from $url, $resp")
}

val result = for {
  js <- yaml.parser.parse(content)
} yield js
.mapArray(_.map(_.hcursor.downField("projects").withFocus(_.mapArray(_.map(_.mapObject(transformProjectObj)))).top.get))

def transformProjectObj(projectObj: JsonObject): JsonObject = {
  val githubUrl = projectObj("github").get.asString.get
  val orgRepo = githubUrl.split("/").takeRight(2)
  val desc = Json.fromString(fetchDescription(orgRepo(0), orgRepo(1)))
  projectObj.add("description", desc)
}

println(result)
