---
layout: index
title: "scalaz"

meta:
  nav: projects
  canonical: "projects/scalaz"
  pygments: true
---

<div class="jumbotron">
  <h1>scalaz</h1>
  <p class="lead">Functional programming for Scala</p>
</div>

Scalaz provides purely functional data structures to complement those from the Scala standard library. It defines a set of foundational type classes (e.g. `Functor`, `Monad`) and corresponding instances for a large number of data structures.

This page describes the new and upcoming release of Scalaz 7. For more information, please refer to our [Wiki](https://github.com/scalaz/scalaz/wiki).

## Example

```scala
scala> import scalaz._

scala> import std.option._, std.list._ // functions and type class instances for Option and List

scala> Apply[Option].apply2(some(1), some(2))((a, b) => a + b)
res0: Option[Int] = Some(3)

scala> Traverse[List].traverse(List(1, 2, 3))(i => some(i))
res1: Option[List[Int]] = Some(List(1, 2, 3))

scala> import syntax.bind._ // syntax for the Bind type class (and its parents)

scala> List(List(1)).join
res2: List[Int] = List(1)

scala> List(true, false).ifM(List(0, 1), List(2, 3))
res3: List[Int] = List(0, 1, 2, 3)
```
