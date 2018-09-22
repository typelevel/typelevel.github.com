package org.typelevel.blog

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.yaml.snakeyaml.Yaml

object Main extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val posts = BuildInfo.tutInput.listFiles().toList.filter { file =>
    file.isFile() && file.getName.endsWith(".md")
  }.map(Post(_))

  val future = Future.traverse(posts)(_.process)

  Await.result(future, Duration.Inf)

}
