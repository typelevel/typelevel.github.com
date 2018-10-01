---
layout: post
title: Product with Serializable

meta:
  nav: blog
  author: ceedubs
  pygments: true

tut:
  scala: 2.12.4
  binaryScala: "2.12"
---

A somewhat common Scala idiom is to make an `abstract` type extend `Product with Serializable`. There isn't an obvious reason to do this, and people have asked me a number of times why I've done this. While I don't think that `Product` or `Serializable` are particularly good abstractions, there's a reason that I extend them.

Let's say that I'm writing a simple enum-like `Status` type:

```scala
object EnumExample1 {
  sealed abstract class Status
  case object Pending extends Status
  case object InProgress extends Status
  case object Finished extends Status
}
```

Now let's create a `Set` of statuses that represent incomplete items:

```scala
import EnumExample1._
```

```scala
val incomplete = Set(Pending, InProgress)
// incomplete: scala.collection.immutable.Set[Product with Serializable with EnumExample1.Status] = Set(Pending, InProgress)
```

Here, I didn't give in explicit return type to `incomplete` and you may have noticed that the compiler inferred a somewhat bizarre one: `Set[Product with Serializable with Status]`. Why is that?

The compiler generally tries to infer the most specific type possible. Usually this makes sense. If you write `val x = 3` you probably don't want it to infer `val x: Any = 3`. And in the example above, I didn't want the return type for `incomplete` to be inferred as `Any` or even `Set[Any]`. However, the compiler was a bit _too_ clever and realized that not only is every item in the set an instance of `Status`, they are also instances of `Product` and `Serializable` since every `case object` (and `case class`) automatically extends `Product` and `Serializable`. Therefore, when it calculates the least upper bound (LUB) of the types in the set, it comes up with `Product with Serializable with Status`.

While there's nothing inherently wrong with the return type of `Product with Serializable with Status`, it is verbose, it wasn't what I intended, and in certain situations it might cause inference issues. Luckily there's a simple workaround to get the inferred type that I want:

```scala
object EnumExample2 {
  // note the `extends` addition here
  sealed abstract class Status extends Product with Serializable
  case object Pending extends Status
  case object InProgress extends Status
  case object Finished extends Status
}
```

```scala
import EnumExample2._
```

```scala
val incomplete = Set(Pending, InProgress)
// incomplete: scala.collection.immutable.Set[EnumExample2.Status] = Set(Pending, InProgress)
```

Now since `Status` itself already includes `Product` and `Serializable`, `Status` is the LUB type of `Pending`, `InProgress`, and `Finished`.
