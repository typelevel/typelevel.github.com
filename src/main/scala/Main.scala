package org.typelevel.blog

import org.yaml.snakeyaml.Yaml

object Main extends App {

  val posts = BuildInfo.tutInput.listFiles().toList.filter { file =>
    file.isFile() && file.getName.endsWith(".md")
  }.map(Post(_))

  posts.foreach(_.process())

}
