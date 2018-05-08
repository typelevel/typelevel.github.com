---
layout: post
title: Designing a Tagless Final application

meta:
  nav: blog
  author: gvolpe
  pygments: true

tut:
  scala: 2.12.4
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.4
    - org.typelevel::cats-core:1.1.0

---

There have been a couple of really [nice blog posts](https://typelevel.org/blog/2017/12/27/optimizing-final-tagless.html) about `Tagless Final` and some related topics. However, I have faced some design problems when writing some algebras and haven't seen anybody talking about. So please let me introduce this problem to you.

### Algebra definition

Consider the following algebra:

```scala
trait ItemRepository[F[_]] {
  def findAll: F[List[Item]]
  def find(name: ItemName): F[Option[Item]]
  def save(item: Item): F[Unit]
  def remove(name: ItemName): F[Unit]
}
```

Let's go through each method's definition:

- `findAll` needs to return many Items, obtainable inside a context: `F[List[Item]]`.
- `find` might or might not return an Item inside a context: `F[Option[Item]]`.
- `save` and `remove` will perform some actions without returning any actual value: `F[Unit]`.

Everything is clear and you might have seen this kind of patter before, so let's create an interpreter for it:

```scala
import cats.effect.Sync

// Pretend this is the real implementation... What matters for the example are the types.
class PostgreSQLItemRepository[F[_]](implicit F: Sync[F]) extends ItemRepository[F] {
  private val mockItem = Item(ItemName("laptop"), BigDecimal(1500))

  override def findAll: F[List[Item]] = F.pure(List(mockItem))
  override def find(name: ItemName): F[Option[Item]] = F.pure(Some(mockItem))
  override def save(item: Item): F[Unit] = F.unit
  override def remove(name: ItemName): F[Unit] = F.unit
}

```

Here we have a fake `PostgreSQL` interpreter for the `ItemRepository` algebra, but just pretend that is a real implementation.

One of the most popular DB libraries in the Typelevel ecosystem is [Doobie](http://tpolecat.github.io/doobie/), defined as `A principled JDBC layer for Scala`. And it comes with one super powerful feature: it support `Streaming` results, thanks to [fs2](https://functional-streams-for-scala.github.io/fs2/).

Now it could be very common to have a huge amount of `Item`s in our DB that a `List` will not fit into memory and / or it will be a very expensive operation. So we might want to stream the results of `findAll` instead of have them all in memory on a `List`, making `Doobie` a great candidate for the job. But wait... We have a problem now. Our `ItemRepository` algebra has fixed the definition of `findAll` as `F[List[Item]]` so we won't be able to create an interpreter that returns a streaming result instead.

### Rethinking our algebra

We should think about abstracting over that `List` and two of the most common abstractions that immediately come to mind are `Foldable` and `Traverse`. But although these typeclasses are very useful, they are not enough to represent a stream of values, so we should come up with a better abstraction.

Well, it seems that our options are either adding another higher-kinded parameter `G[_]` to our algebra or just define an abstract member `G[_]`. So let's go with the first one:

```scala
trait ItemRepository[F[_], G[_]] {
  def findAll: G[Item]
  def find(name: ItemName): F[Option[Item]]
  def save(item: Item): F[Unit]
  def remove(name: ItemName): F[Unit]
}
```

Great! This looks good so far.

### Streaming support interpreter

Now let's write a new `PostgreSQL` interpreter with streaming support:

```scala
import fs2.Stream

// Pretend this is the real implementation... What matters for the example are the types.
class StreamingItemRepository[F[_]](implicit F: Sync[F]) extends ItemRepository[F, Stream[F, ?]] {
  private val mockItem = Item(ItemName("laptop"), BigDecimal(1500))

  override def findAll: Stream[F, Item] = Stream.eval(F.pure(mockItem))
  override def find(name: ItemName): F[Option[Item]] = F.pure(None)
  override def save(item: Item): F[Unit] = F.unit
  override def remove(name: ItemName): F[Unit] = F.unit
}

```

Voilà! We got our streaming implementation of `findAll`.

### Test interpreter

That's all we wanted, but what about testing it? Sure, we might prefer to have a simple implementation by just using a plain `List`, so what can we possibly do?

```scala
object MemRepository extends ItemRepository[Id, List] {
  private val mem = MutableMap.empty[String, Item]

  override def findAll: List[Item] = mem.headOption.map(_._2).toList
  override def find(name: ItemName): Id[Option[Item]] = mem.get(name.value)
  override def save(item: Item): Id[Unit] = mem.update(item.name.value, item)
  override def remove(name: ItemName): Id[Unit] = {
    mem.remove(name.value)
    ()
  }
}
```

That's pretty much it! We managed to abstract over the type return by `findAll` by adding an extra parameter to our algebra.

### Source of inspiration

I've come up with most of the ideas presenting until now during my work on [Fs2 Rabbit](https://gvolpe.github.io/fs2-rabbit/), a stream based client for `Rabbit MQ`, where I make heavy use of this technique as I originally described in [this blog post](https://partialflow.wordpress.com/2018/02/01/a-tale-of-tagless-final-cats-effect-and-streaming-fs2-rabbit-v0-1/).

Another great source of inspiration was [this talk](https://www.youtube.com/watch?v=1h11efA4k8E) given by [Luka Jacobowitz](https://github.com/LukaJCB) at Scale by the Bay.

### Abstracting over the effect type

One thing you might have noticed on the examples above is that both `PostgreSQL` interpreters are not fixed to `IO` or `Task` or any other effect type. They are just requiring a parametric `F[_]` and an implicit instance of `Sync[F]`. This is a quite powerful technique for both library authors and application developers. Well know libraries such as [Http4s](https://http4s.org/), [Monix](https://monix.io/) and [Fs2](https://functional-streams-for-scala.github.io/fs2/) make a heavy use of it.

By requiring a `Sync[F]` instance we are just saying that our implementation will need to suspend synchronous side effects.

Once at the edge of our program, commonly the main method, we can give the `F[_]` type a concrete implementation. At the moment, there are two options: `cats.effect.IO` and `monix.eval.Task`. And hopefully soon we'll have a `Scalaz 8 IO` implementation if everything goes well.

### Principle of least power

Abstracting over the effect type doesn't only mean that we should require `Sync[F]`, `Async[F]` or `Effect[F]`. It also means that we should only require the minimal instance that covers the actual implementation. For example:

```scala
import cats.Functor
import cats.implicits._

def bar[F[_]: Applicative]: F[Int] = 1.pure[F]

def foo[F[_]: Functor](fa: F[Int]): F[String] =
  fa.map(_.toString)
```

Here our `bar` method just returns a pure value in the `F` context, thus we need an `Applicative[F]` instance that defines `pure`. On the other hand, our `foo` method just converts the inner `Int` into `String`, what we call a pure data transformation. So all we need here is a `Functor[F]` instance. Another example:

```scala
import cats.Monad

def fp[F[_]: Monad]: F[String] =
  for {
    a <- bar[F]
    b <- bar[F]
  } yield a + b
```

The above implementation makes use of a `for-comprehention` which is a sintactic sugar for `flatMap` and `map`, so all we need is a `Monad[F]` instance because we also need an `Applicative[F]` instance for `bar`, otherwise we could just use a `FlatMap[F]` instance.

### Final thoughts

I think we got quite far with all these abstractions, giving us the change to write clean and elegant code in a pure functional programming style, but there's more. Other topics that should be worth mention but might require a blog post on their own are:

- Dependency Injection
  + Tagless Final + implicits (MTL style) enables DI in an elegant way.
- Algebras Composition
  + It is very common to have multiple algebras with a different `F[_]` implementation. In some cases, `FunctionK` (a.k.a. Natural Transformation) could be the solution.

What do you think about it? Have you come across a similar design problem? I'd love to hear your thoughts!
