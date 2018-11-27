---
layout: post
title: Error handling in Http4s with classy optics – Part 2

meta:
  nav: blog
  author: gvolpe
  pygments: true

tut:
  scala: 2.12.7
  binaryScala: "2.12"
  scalacOptions:
    - -Ypartial-unification
  plugins:
    - org.spire-math::kind-projector:0.9.9
  dependencies:
    - org.scala-lang:scala-library:2.12.7
    - org.typelevel::cats-core:1.4.0
    - org.typelevel::cats-effect:1.1.0-M1
    - org.http4s::http4s-blaze-server:0.20.0-M3
    - org.http4s::http4s-circe:0.20.0-M3
    - org.http4s::http4s-dsl:0.20.0-M3
    - io.circe::circe-core:0.10.0
    - io.circe::circe-generic:0.10.0
    - co.fs2::fs2-core:1.0.0
    - com.olegpy::meow-mtl:0.2.0
    - com.chuusai::shapeless:2.3.3

---

This is a continuation of my [previous blog post](https://typelevel.org/blog/2018/08/25/http4s-error-handling-mtl.html). Make sure you have read that one before continuing here.

I recently gave a 20 minutes talk on `classy optics` at the unconference of [Scale by the Bay](http://scale.bythebay.io/) where I also talked about this error handling technique and on my way back home I was still thinking of different ways of doing this. So, after some exploratory work, I came up with a few different alternatives.

Something that made me cringe and that a few of my colleagues at work were not happy with was that the algebras had no association with the error type defined in `HttpErrorHandler[F, E]` so the type-safety was down to the programmer's discipline and in this case the compiler was not able to do much.

When working with `EitherT[F, E, A]` or a bifunctor `IO[E, A]` we have a clear error type whereas by just relying on a single `F[A]` with a `MonadError[F, Throwable]` instance we lose this property. However, most of the time we only care about mapping a few business errors to Http responses if working on a REST API.

As of now I still prefer the latter for a number of reasons. Here's a comparison table of all the different approaches I can think of:

|            | EitherT[IO, E, ?] | IO[E, A] | IO[A] | IO[A] + Classy Optics |
|------------|-------------------|----------|-------|-----------------------|
| Concise code style | ✘ | ✔ | ✔ | ✔ |
| Polymorphic code | ✔ | ✘ | ✔ | ✔ |
| Performance overhead | **2x** | ✔ | ✔ | ✔ |
| Double error channel | ✔ | ✔ | ✘ | ✘ |

With "Double error channel" I mean that we can signal failure in two different ways:

- `EitherT[IO, E, ?]` can either fail with `Left` or with `IO.raiseError` (that can also be a caught exception).
- `IO[E, A]` can either fail with an `E` or by throwing an exception when `A` was expected. This is called "unrecoverable" errors.

And this is not a bad thing at all! It is like this by design and we can have the same property when writing polymorphic code using `cats-effect` while still keeping it simple. Here's one way:

### Error Channel

In the previous blog post we defined the algebras as a single trait. In this case we are going to try a different encoding but first we need to introduce an `ErrorChannel[F, E]` typeclass where the error type is a subtype of `Throwable` to be compatible with the error type of the `cats-effect` typeclasses:

```tut:book:silent
trait ErrorChannel[F[_], E <: Throwable] {
  def raise[A](e: E): F[A]
}
```

An instance can be derived for any `ApplicativeError[F, Throwable]` so we don't need to write it manually for every error type.

```tut:book:silent
import cats.ApplicativeError

object ErrorChannel {
  def apply[F[_], E <: Throwable](implicit ev: ErrorChannel[F, E]) = ev

  implicit def instance[F[_], E <: Throwable](implicit F: ApplicativeError[F, Throwable]): ErrorChannel[F, E] =
    new ErrorChannel[F, E] {
      override def raise[A](e: E) = F.raiseError(e)
    }

  object syntax {
    implicit class ErrorChannelOps[F[_]: ErrorChannel[?[_], E], E <: Throwable](e: E) {
      def raise[A]: F[A] = ErrorChannel[F, E].raise[A](e)
    }
  }
}
```

### User Algebra

Our `UserAlg` will now be defined as an `abstract class` instead in order to be able to add typeclass constraint.

```tut:book:silent
case class User(username: String, age: Int)
case class UserUpdateAge(age: Int)

abstract class UserAlg[F[_]: ErrorChannel[?[_], E], E <: Throwable] {
  def find(username: String): F[Option[User]]
  def save(user: User): F[Unit]
  def updateAge(username: String, age: Int): F[Unit]
}
```

And here's the ADT of the possible errors that may arise (notice the `extends Exception` part):

```tut:book:silent
sealed trait UserError extends Exception
case class UserAlreadyExists(username: String) extends UserError
case class UserNotFound(username: String) extends UserError
case class InvalidUserAge(age: Int) extends UserError
```

### User Interpreter

Here's a similar `UserAlg` interpreter to the one presented in the previous post. Note that in a real-life project an interpreter will more likely connect to a database instead of using an in-memory representation based on `Ref`.

The interesting part is that in order to construct a `UserAlg[F, UserError]` we now need an `ErrorChannel[F, UserError]` instance in scope. This will be the chosen strategy to report errors in the context of `F`.

```tut:book:silent
import cats.effect.{ Concurrent, Sync }
import cats.effect.concurrent.Ref
import cats.syntax.all._

object UserInterpreter {

  def mkUserAlg[F[_]: Sync](implicit error: ErrorChannel[F, UserError]): F[UserAlg[F, UserError]] =
    Ref.of[F, Map[String, User]](Map.empty).map { state =>
      new UserAlg[F, UserError] {
        private def validateAge(age: Int): F[Unit] =
          if (age <= 0) error.raise(InvalidUserAge(age)) else ().pure[F]

        override def find(username: String): F[Option[User]] =
          state.get.map(_.get(username))

        override def save(user: User): F[Unit] =
          validateAge(user.age) *>
            find(user.username).flatMap {
              case Some(_) =>
                error.raise(UserAlreadyExists(user.username))
//                error.raise(new Exception("asd")) // Does not compile
//                Sync[F].raiseError(new Exception("")) // Should be considered an unrecoverable failure
              case None =>
                state.update(_.updated(user.username, user))
            }

        override def updateAge(username: String, age: Int): F[Unit] =
          validateAge(age) *>
            find(username).flatMap {
              case Some(user) =>
                state.update(_.updated(username, user.copy(age = age)))
              case None =>
                error.raise(UserNotFound(username))
            }
      }
    }

}
```

Notice that we could still call `Sync[F].raiseError(new Exception("boom"))` and it will still compile. However, if we choose to use `ErrorChannel` to signal business errors we will have the compiler on our side and it'll warn us when we try to raise an error that is not part of the ADT we have declared. So signaling error in a different way should just be considered unrecoverable. These are the same semantics you get when working with `EitherT[IO, Throwable, ?]` as shown in the comparison table at the beginning.

### Http Error Handler

Here's the same `HttpErrorHandler` defined in the previous blog post:

```tut:book:silent
import cats.{ ApplicativeError, MonadError }
import cats.data.{ Kleisli, OptionT }
import org.http4s._

trait HttpErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object RoutesHttpErrorHandler {
  def apply[F[_]: ApplicativeError[?[_], E], E <: Throwable](
      routes: HttpRoutes[F]
  )(handler: E => F[Response[F]]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT {
        routes.run(req).value.handleErrorWith(e => handler(e).map(Option(_)))
      }
    }
}

object HttpErrorHandler {
  def apply[F[_], E <: Throwable](implicit ev: HttpErrorHandler[F, E]) = ev

  def mkInstance[F[_]: ApplicativeError[?[_], E], E <: Throwable](
      handler: E => F[Response[F]]
  ): HttpErrorHandler[F, E] =
    (routes: HttpRoutes[F]) => RoutesHttpErrorHandler(routes)(handler)
}
```

### Http Routes with error handling

Now let's look at the new implementation of `UserRoutes` using the error-type algebra:

```tut:book:silent
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class PreUserRoutesMTL[F[_]: Sync](users: UserAlg[F, UserError]) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" / username =>
      users.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None       => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req.as[User].flatMap { user =>
        users.save(user) *> Created(user.username.asJson)
      }

    case req @ PUT -> Root / "users" / username =>
      req.as[UserUpdateAge].flatMap { userUpdate =>
        users.updateAge(username, userUpdate.age) *> Created(username.asJson)
      }
  }

  def routes(implicit H: HttpErrorHandler[F, UserError]): HttpRoutes[F] =
    H.handle(httpRoutes)

}
```

Notice that in contrary to the example shown in the previous blog post there is now a relationship between `UserAlg` and `HttpErrorHandler`: the error type is the same. However, this is not enforced by the compiler. ***Can we be more strict about it?***

We could define a generic `Routes[F, E]`:

```tut:book:silent
abstract class Routes[F[_], E <: Throwable](implicit H: HttpErrorHandler[F, E]) extends Http4sDsl[F] {
  protected def httpRoutes: HttpRoutes[F]
  val routes: HttpRoutes[F] = H.handle(httpRoutes)
}
```

But we'll also need something else to connect the error types of the algebra and the http error handler:

```tut:book:silent
abstract class UserRoutes[F[_]: HttpErrorHandler[?[_], E], E <: Throwable](
    users: UserAlg[F, E]
) extends Routes[F, E]
```

That's it! We are now enforcing this relationship at compile time. Let's see how the `HttpRoutes` looks like:

```tut:book:silent
class UserRoutesAlt[F[_]: HttpErrorHandler[?[_], UserError]: Sync](
    users: UserAlg[F, UserError]
) extends UserRoutes(users) {

  protected val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" / username =>
      users.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None       => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req
        .as[User]
        .flatMap { user =>
          users.save(user) *> Created(user.username.asJson)
        }

    case req @ PUT -> Root / "users" / username =>
      req
        .as[UserUpdateAge]
        .flatMap { userUpdate =>
          users.updateAge(username, userUpdate.age) *> Ok(username.asJson)
        }
  }

}
```

Neat! Right? If we try to change the error type of `UserAlg` it wouldn't compile!

### More than one algebra per Http Route

In most of my programs I tend to specify an `HttpRoute` per algebra. But what if we wanted to just define a single `HttpRoute` that uses multiple algebras? There are a couple of options.

Let's first define a new ADT of errors and a new algebra to illustrate the problem:

**Catalog Error**

```tut:book:silent
sealed trait CatalogError extends Exception
case class ItemAlreadyExists(item: String) extends CatalogError
case class CatalogNotFound(id: Long) extends CatalogError
```

**CatalogAlg**

```tut:book:silent
case class Item(name: String) extends AnyVal

abstract class CatalogAlg[F[_]: ErrorChannel[?[_], E], E <: Throwable] {
  def find(id: Long): F[List[Item]]
  def save(id: Long, item: Item): F[Unit]
}
```

**HttpRoutes with multiple algebras**

Here we have an `HttpRoutes` that makes use of two algebras with different error types:

```tut:book:silent
class UserRoutesMTL[F[_]: Sync](
    users: UserAlg[F, UserError],
    catalog: CatalogAlg[F, CatalogError]
) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = ???

  def routes(
    implicit H1: HttpErrorHandler[F, UserError],
             H2: HttpErrorHandler[F, CatalogError]
  ): HttpRoutes[F] =
    H2.handle(H1.handle(httpRoutes))

}
```

It works! But it's not as elegant as we would like it to be and if we add more algebras this would quicky get out of control.

***Can we generalize this pattern?***

### Shapeless Coproduct

We can define our error type as a coproduct of different errors, in our case `UserError` and `CatalogError`. For example:

```scala
import shapeless._

def routes[F[_]](implicit H: HttpErrorHandler[F, UserError :+: CatalogError :+: CNil]) = ???
```

However, this doesn't compile because the error type is no longer a subtype of `Throwable`. It is now a `Coproduct` :/. But...

We might be able to derive an instance for a coproduct of errors if we have an instance of `HttpErrorHandler[F, E]` for each error type. Let's give it a try! We need to define a new typeclass `CoHttpErrorHandler`:

```tut:book:silent
import shapeless._

trait CoHttpErrorHandler[F[_], Err <: Coproduct] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object CoHttpErrorHandler {
  def apply[F[_], Err <: Coproduct](implicit ev: CoHttpErrorHandler[F, Err]) = ev

  implicit def cNilInstance[F[_]]: CoHttpErrorHandler[F, CNil] =
    (routes: HttpRoutes[F]) => routes

  implicit def consInstance[F[_], E <: Throwable, T <: Coproduct](
      implicit H: HttpErrorHandler[F, E],
      CH: CoHttpErrorHandler[F, T]
  ): CoHttpErrorHandler[F, E :+: T] =
    (routes: HttpRoutes[F]) => CH.handle(H.handle(routes))
}
```

Voilà! We introduced a `CoHttpErrorHandler` where the error type is a coproduct and the instance can only be derived if each type is a subtype of `Throwable` making it impossible to define an invalid coproduct. So it compiles! But how do we use it?

### HttpRoutes for a coproduct of errors

```tut:book:silent
class CoUserRoutesMTL[F[_]: Sync](
    users: UserAlg[F, UserError],
    catalog: CatalogAlg[F, CatalogError]
) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = ???

  def routes(implicit CH: CoHttpErrorHandler[F, UserError :+: CatalogError :+: CNil]): HttpRoutes[F] =
    CH.handle(httpRoutes)

}
```

Yay!!! Now this is more elegant and generic so we can re-use the same pattern in different routes. But now again we have lost the relationship between the error types of the algebras and the error type of `CoHttpErrorHandler`. So maybe we could do something similar to what we have done previously?

It's possible but in the case of coproducts we need to introduce some boilerplate...

**CoRoutes**

```tut:book:silent
abstract class CoRoutes[F[_], E <: Coproduct](implicit CH: CoHttpErrorHandler[F, E]) extends Http4sDsl[F] {
  protected def httpRoutes: HttpRoutes[F]
  val routes: HttpRoutes[F] = CH.handle(httpRoutes)
}
```

This one is pretty basic and similar to `Routes` defined before.

**CoUserRoutes**

```tut:book:silent
abstract class CoUserRoutes[
    F[_]: CoHttpErrorHandler[?[_], E],
    A <: Throwable,
    B <: Throwable,
    E <: Coproduct: =:=[?, A :+: B :+: CNil]
](
    users: UserAlg[F, A],
    catalog: CatalogAlg[F, B]
) extends CoRoutes[F, E]

type CustomError = UserError :+: CatalogError :+: CNil
```

Here we have a couple of constraints:

- `F[_]` needs to have an instance of `CoHttpErrorHandler[F, E]`.
- `A` and `B` are the error types of the two algebras.
- `E` needs to be a `Coproduct` of type `A :+: B :+: CNil`.

**HttpRoutes with multiple algebras - Strict version**

```tut:book:silent
class CoUserRoutesMTL[F[_]: CoHttpErrorHandler[?[_], CustomError]: Sync](
    users: UserAlg[F, UserError],
    catalog: CatalogAlg[F, CatalogError]
) extends CoUserRoutes(users, catalog) {

  protected val httpRoutes: HttpRoutes[F] = ???

}
```

Now we are saying that the error type of our `CoHttpErrorHandler` is a coproduct of each error type of the algebras. And we wouldn't be able to change the error type of any of them without getting a compiler error.

### Source code

You can see all the compiling examples [here](https://github.com/gvolpe/classy-optics). Make sure you check out all the different branches.

### Conclusion

The last approach is probably too much but we have demonstrated that it's possible to push the boundaries to make our application very type-safe. However, we also need to consider the trade-offs of writing more boilerplate.

Personally, I settle for the previous approach where the error type of the algebra matches the error type of the `HttpErrorHandler` even if it requires a bit more of discipline. The choice is yours! Just make sure you understand the trade-offs of every mechanism.

I hope you have enjoyed this post and please do let me know if you have other ideas to keep broadening my understanding!

Thanks for reading :)
