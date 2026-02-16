{%
  author: ${gvolpe}
  date: "2018-05-09"
  tags: [technical]
%}

# Tagless Final Algebras and Streaming

There have been a couple of really [nice blog posts](https://typelevel.org/blog/2017/12/27/optimizing-final-tagless.html) about `Tagless Final` and some related topics. However, I have faced some design problems when writing some algebras and haven't seen anybody talking about. So please let me introduce this problem to you.

### Algebra definition

Given the following data definition:

```scala
case class ItemName(value: String) extends AnyVal
case class Item(name: ItemName, price: BigDecimal)
```

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

Everything is clear and you might have seen this kind of pattern before, so let's create an interpreter for it:

```scala
import doobie.implicits._
import doobie.util.transactor.Transactor
import cats.effect.Sync

// Doobie implementation (not fully implemented, what matters here are the types).
class PostgreSQLItemRepository[F[_]](xa: Transactor[F])
                                    (implicit F: Sync[F]) extends ItemRepository[F] {

  override def findAll: F[List[Item]] = sql"select name, price from items"
                                           .query[Item]
                                           .to[List]
                                           .transact(xa)

  override def find(name: ItemName): F[Option[Item]] = F.pure(None)
  override def save(item: Item): F[Unit] = F.unit
  override def remove(name: ItemName): F[Unit] = F.unit
}

```

Here we are using [Doobie](http://tpolecat.github.io/doobie/), defined as `A principled JDBC layer for Scala` and one of the most popular DB libraries in the Typelevel ecosystem. And it comes with one super powerful feature: it supports `Streaming` results, since it's built on top of [fs2](https://functional-streams-for-scala.github.io/fs2/).

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
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream

// Doobie implementation (not fully implemented, what matters here are the types).
class StreamingItemRepository[F[_]](xa: Transactor[F])
                                   (implicit F: Sync[F]) extends ItemRepository[F, Stream[F, ?]] {

  override def findAll: Stream[F, Item] = sql"select name, price from items"
                                           .query[Item]
                                           .stream
                                           .transact(xa)

  override def find(name: ItemName): F[Option[Item]] = F.pure(None)
  override def save(item: Item): F[Unit] = F.delay(println(s"Saving item: $item"))
  override def remove(name: ItemName): F[Unit] = F.delay(println(s"Removing item: $item"))
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

That's pretty much it! We managed to abstract over the return type of `findAll` by just adding an extra parameter to our algebra.

### About composition

At this point the avid reader might have thought, what if I want to write a generic function that takes all the items (using `findAll`), applies some discounts and writes them back to the DB (using `save`)?

Short answer is, you might want to define a different algebra where `findAll` and `save` have the same types (eg: both of them are streams) but in case you find yourself wanting to make this work with the current types then let's try and find out!

```scala
class DiscountProcessor[F[_], G[_]: Functor](repo: ItemRepository[F, G], join: G[F[Unit]] => F[Unit]) {

  def process(discount: Double): F[Unit] = {
    val items: G[Item] = repo.findAll.map(item => item.copy(price = item.price * (1 - discount)))
    val saved: G[F[Unit]] = items.map(repo.save)
    join(saved)
  }
}
```

We defined a `join` function responsible for evaluating the effects and flatten the result to `F[Unit]`. As you can see below, this works for both a streaming interpreter and a list interpreter (shout out to [fthomas](https://github.com/fthomas) for proposing this solution):

```scala
object StreamingDiscountInterpreter {

  private val join: Stream[IO, IO[Unit]] => IO[Unit] = _.evalMap(identity).compile.drain

  def apply(repo: ItemRepository[IO, Stream[IO, ?]]): DiscountProcessor[IO, Stream[IO, ?]] =
    new DiscountProcessor[IO, Stream[IO, ?]](repo, join)

}

object ListDiscountInterpreter {

  private val join: List[IO[Unit]] => IO[Unit] = list => Traverse[List].sequence(list).void

  def apply(repo: ItemRepository[IO, List]): DiscountProcessor[IO, List] =
    new DiscountProcessor[IO, List](repo, join)

}
```

While in this case it was possible to make it generic I don't recommend to do this at home because:

1. it involves some extra boilerplate and the code becomes harder to understand / maintain.
2. as soon as the logic gets more complicated you might run out of options to make it work in a generic way.
3. you lose the ability to use the `fs2 DSL` which is super convenient.

What I recommend instead, is to write this kind of logic in the streaming interpreter itself. You could also write a generic program that implements the parts that can be abstracted (eg. applying a discount to an item `f: Item => Item`) and leave the other parts to the interpreter.

### Design alternative

Another possible and very interesting alternative suggested by [Michael Pilquist](https://github.com/mpilquist), would be to define our repository as follows:

```scala
trait ItemRepository[F[_], S[_[_], _]] {
  def findAll: S[F, Item]
}
```

Where the second type parameter matches the shape of `fs2.Stream`. In this case our streaming repository will remain the same (it should just extend `ItemRepository[F, Stream]` instead of `ItemRepository[F, Stream[F, ?]]`) but our in memory interpreter will now rely on `fs2.Stream` instead of a parametric `G[_]`, for example:

```scala
object MemRepositoryAlt extends ItemRepository[Id, Stream] {

  override def findAll: Stream[Id, Item] = {
    sql"select name, price from items"
      .query[Item]
      .stream
      .transact(xa)
  }

}
```

I think it's an alternative worth exploring further that might require a blog post on its own, so I'll leave it here for reference :)

### Source of inspiration

I've come up with most of the ideas presented here during my work on [Fs2 Rabbit](https://gvolpe.github.io/fs2-rabbit/), a stream based client for `Rabbit MQ`, where I make heavy use of this technique as I originally described in [this blog post](https://partialflow.wordpress.com/2018/02/01/a-tale-of-tagless-final-cats-effect-and-streaming-fs2-rabbit-v0-1/).

Another great source of inspiration was [this talk](https://www.youtube.com/watch?v=1h11efA4k8E) given by [Luka Jacobowitz](https://github.com/LukaJCB) at Scale by the Bay.

### Abstracting over the effect type

One thing you might have noticed in the examples above is that both `ItemRepository` interpreters are not fixed to `IO` or `Task` or any other effect type but rather requiring a parametric `F[_]` and an implicit instance of `Sync[F]`. This is a quite powerful technique for both library authors and application developers. Well know libraries such as [Http4s](https://http4s.org/), [Monix](https://monix.io/) and [Fs2](https://functional-streams-for-scala.github.io/fs2/) make a heavy use of it.

And by requiring a `Sync[F]` instance we are just saying that our implementation will need to suspend synchronous side effects.

Once at the edge of our program, commonly the main method, we can give `F[_]` a concrete type. At the moment, there are two options: `cats.effect.IO` and `monix.eval.Task`. But hopefully soon we'll have a `Scalaz 8 IO` implementation as well (fingers crossed).

### Principle of least power

Abstracting over the effect type doesn't only mean that we should require `Sync[F]`, `Async[F]` or `Effect[F]`. It also means that we should only require the minimal typeclass instance that satisfies our predicate. For example:

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
`  for {
    a <- bar[F]
    b <- bar[F]
  } yield a + b
```

The above implementation makes use of a `for-comprehension` which is a syntactic sugar for `flatMap` and `map`, so all we need is a `Monad[F]` instance because we also need an `Applicative[F]` instance for `bar`, otherwise we could just use a `FlatMap[F]` instance.

### Final thoughts

I think we got quite far with all these abstractions, giving us the chance to write clean and elegant code in a pure functional programming style, and there's even more! Other topics worth mentioning that might require a blog post on their own are:

- Dependency Injection
  + Tagless Final + implicits (MTL style) enables DI in an elegant way.
- Algebras Composition
  + It is very common to have multiple algebras with a different `F[_]` implementation. In some cases, `FunctionK` (a.k.a. natural transformation) can be the solution.

What do you think about it? Have you come across a similar design problem? I'd love to hear your thoughts!
