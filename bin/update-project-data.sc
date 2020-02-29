#!/usr/bin/env amm

import $ivy.`io.circe::circe-yaml:0.12.0`
import $ivy.`io.circe::circe-parser:0.12.3`
import $ivy.`io.circe::circe-generic:0.12.3`
import $ivy.`com.lihaoyi::requests:0.4.7`
import $ivy.`com.lihaoyi::os-lib:0.6.2`

import io.circe.yaml
import io.circe.yaml.syntax._
import io.circe.parser
import io.circe._
import io.circe.generic.auto._

val yamlPath = os.pwd / "_data" / "projects.yml"

val content = os.read(yamlPath)
val githubToken = sys.env("GITHUB_TOKEN")

final case class RepoMeta(
  description: String,
  stargazers_count: Long,
  homepage: String
)

def fetchRepoMeta(org: String, repo: String): RepoMeta = {
  val url = s"https://api.github.com/repos/$org/$repo"
  val resp = requests.get(url, headers = Map("Authorization" -> s"token $githubToken"))
  if (resp.is2xx) {
    val js = parser.parse(resp.text).toTry.get
    js.as[RepoMeta].toTry.get
  } else throw new Exception(s"Non 2xx response from $url, $resp")
}

val result = for {
  js <- yaml.parser.parse(content)
} yield js
.mapArray(_.map(_.hcursor.downField("projects").withFocus(_.mapArray(_.map(_.mapObject(transformProjectObj)))).top.get))

def transformProjectObj(projectObj: JsonObject): JsonObject = {
  val githubUrl = projectObj("github").get.asString.get
  val orgRepo = githubUrl.split("/").takeRight(2)
  val repoMeta = fetchRepoMeta(orgRepo(0), orgRepo(1))
  projectObj.deepMerge(JsonObject.fromMap(Map(
      "description" -> Json.fromString(repoMeta.description),
      "stars" -> Json.fromLong(repoMeta.stargazers_count)
    ) ++ (if (repoMeta.homepage.isEmpty) None else Some("homepage" -> Json.fromString(homepage)))
  ))
}

os.write.over(yamlPath, result.right.get.asYaml.spaces2)
