---
layout: page
title: "Projects"

meta:
  nav: projects
  canonical: projects
  pygments: true
---

Projects
========

<table style="font-weight: bold;">
  <tr>
    <td><a class="btn" href="https://github.com/scalaz/scalaz/tree/scalaz-seven">{% include octocat.html %}</a></td>
    <td><a href="{{ site.baseurl }}/projects/scalaz">scalaz</a></td>
  </tr>
  <tr>
    <td><a class="btn" href="https://github.com/milessabin/shapeless">{% include octocat.html %}</a></td>
    <td><a href="{{ site.baseurl }}/projects/shapeless">shapeless</a></td>
  </tr>
  <tr>
    <td><a class="btn" href="https://github.com/non/spire">{% include octocat.html %}</a></td>
    <td><a href="{{ site.baseurl }}/projects/spire">spire</a></td>
  </tr>
  <tr>
    <td><a class="btn" href="https://github.com/typelevel/scalaz-contrib">{% include octocat.html %}</a></td>
    <td>scalaz-contrib</td>
  </tr>
  <tr>
    <td><a class="btn" href="https://github.com/typelevel/scalaz-specs2">{% include octocat.html %}</a></td>
    <td>scalaz-specs2</td>
  </tr>
  <tr>
    <td><a class="btn" href="https://github.com/typelevel/shapeless-contrib">{% include octocat.html %}</a></td>
    <td>shapeless-contrib</td>
  </tr>
</table>

## SBT configuration

If you want to use the latest releases of our libraries, just put these lines into your `build.sbt` file.

```scala
libraryDependencies ++= Seq(
  "org.scalaz"     %% "scalaz-core" % "7.0.0-M8",
  "com.chuusai"    %% "shapeless"   % "1.2.4",
  "org.spire-math" %% "spire"       % "0.3.0"
)
```
