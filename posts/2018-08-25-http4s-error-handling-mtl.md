---
layout: post
title: Error handling in Http4s with classy optics

meta:
  nav: blog
  author: gvolpe
  pygments: true

tut:
  scala: 2.12.6
  binaryScala: "2.12"
  scalacOptions:
    - -Ypartial-unification
  plugins:
    - org.spire-math::kind-projector:0.9.8
  dependencies:
    - org.scala-lang:scala-library:2.12.6
    - org.typelevel::cats-core:1.1.0
    - org.typelevel::cats-effect:1.0.0-RC2
    - org.http4s::http4s-blaze-server:0.19.0-M1
    - org.http4s::http4s-circe:0.19.0-M1
    - org.http4s::http4s-dsl:0.19.0-M1
    - io.circe::circe-core:0.10.0-M1
    - io.circe::circe-generic:0.10.0-M1
    - co.fs2::fs2-core:1.0.0-M1
    - com.olegpy::meow-mtl:0.1.1

---

As a longtime `http4s` user I keep on learning new things and I'm always trying to come up with the best practices for writing http applications. This time I want to talk about my latest achievements in error handling within the context of an http application where it basically means mapping each business error to the appropiate [http response](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes).

So let's get started by putting up an example of an http application with three different endpoints that interacts with a `UserAlgebra` that may or may not fail with some specific errors.

If you are one of those who don't like to read and prefer to jump straight into the code please find it [here](https://gist.github.com/gvolpe/3fa32dd1b6abce2a5466efbf0eca9e94) :)

### User Algebra

We have a simple `UserAlgebra` that let us perform some actions such as finding and persisting users.

```tut:book:silent
case class User(username: String, age: Int)
case class UserUpdateAge(age: Int)

trait UserAlgebra[F[_]] {
  def find(username: String): F[Option[User]]
  def save(user: User): F[Unit]
  def updateAge(username: String, age: Int): F[Unit]
}
```

And also an ADT of the possible errors that may arise. I'll explain later in this post why it extends `Exception`.

```tut:book:silent
sealed trait UserError extends Exception
case class UserAlreadyExists(username: String) extends UserError
case class UserNotFound(username: String) extends UserError
case class InvalidUserAge(age: Int) extends UserError
```

### User Interpreter

And here we have a simple interpreter for our `UserAlgebra` for demonstration purposes so you can have an idea on how the logic would look like. In a real-life project an interpreter will more likely connect to a database instead of using an in-memory representaion based on `Ref`.

```tut:book:silent
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._

object UserInterpreter {

  def create[F[_]](implicit F: Sync[F]): F[UserAlgebra[F]] =
    Ref.of[F, Map[String, User]](Map.empty).map { state =>
      new UserAlgebra[F] {
        private def validateAge(age: Int): F[Unit] =
          if (age <= 0) F.raiseError(InvalidUserAge(age)) else F.unit

        override def find(username: String): F[Option[User]] =
          state.get.map(_.get(username))

        override def save(user: User): F[Unit] =
          validateAge(user.age) *>
            find(user.username).flatMap {
              case Some(_) =>
                F.raiseError(UserAlreadyExists(user.username))
              case None =>
                state.update(_.updated(user.username, user))
            }

        override def updateAge(username: String, age: Int): F[Unit] =
          validateAge(age) *>
            find(username).flatMap {
              case Some(user) =>
                state.update(_.updated(username, user.copy(age = age)))
              case None =>
                F.raiseError(UserNotFound(username))
            }
      }
    }

}
```

### Http Routes

The following implementation of `UserRoutes` applies the tagless final encoding and the concept of "abstracting over the effect type" where we do not commit to a particular effect until the edge of our application.

```tut:book:silent
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl

class UserRoutes[F[_]: Sync](userAlgebra: UserAlgebra[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" / username =>
      userAlgebra.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req.as[User].flatMap { user =>
        userAlgebra.save(user) *> Created(user.username.asJson)
      }

    case req @ PUT -> Root / "users" / username =>
      req.as[UserUpdateAge].flatMap { userUpdate =>
        userAlgebra.updateAge(username, userUpdate.age) *> Ok(username)
      }
  }

}
```

Now this particular implementation is missing a very important part: error handling. If we use the `UserAlgebra`'s interpreter previously defined we will clearly miss the three errors defined by the `UserError` ADT.

***NOTE: If you are not familiar with these concepts make sure you check out [my talk at Scala Matsuri](https://youtu.be/pGfj_l-h3M8?t=887) early this year where I also talk about error handling in http applications using the Http4s library.***

### Http Error Handling

Okay let's just go ahead and add some error handling to our http route by taking advantange of the `MonadError` instance defined by our constraint `Sync[F]` and making use of the syntax provided by `cats`:

```tut:book:silent
class UserRoutesAlt[F[_]: Sync](userAlgebra: UserAlgebra[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" / username =>
      userAlgebra.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req.as[User].flatMap { user =>
        userAlgebra.save(user) *> Created(user.username.asJson)
      }.handleErrorWith {
        case UserAlreadyExists(username) => Conflict(username.asJson)
      }

    case req @ PUT -> Root / "users" / username =>
      req.as[UserUpdateAge].flatMap { userUpdate =>
        userAlgebra.updateAge(username, userUpdate.age) *> Ok(username.asJson)
      }.handleErrorWith {
        case InvalidUserAge(age) => BadRequest(s"Invalid age $age".asJson)
      }
  }

}
```

Now we can say this implementation is quite elegant! We are handling and mapping business errors to the according http response and our code compiles without any warning whatsoever. But wait... We are not handling the `UserNotFound` error and the compiler didn't tell us about it! That's not cool and we as functional programmers believe in types because we can know what a function might do just by looking at the types but here it seems we hit the wall.

The problem is that our constraint of type `Sync` from `cats-effect` has a `MonadError` instance with its type error fixed as `Throwable`. So the compiler can't help us here since this type is too generic. And we can't add a constraint for `MonadError[F, UserError]` because we would get an "ambigous implicits" error with two instances of `MonadError` in scope.

So, what can we do about it?

### Next level MTL: Optics

I heard sometime ago about Classy Optics (Lenses, Prisms, etc) when I was learning Haskell and watched [this amazing talk](https://www.youtube.com/watch?v=GZPup5Iuaqw) by George Wilson but I never got to use this concept in Scala until now!

Well first, let me give you a quick definition of `Lens`es and `Prism`s. In a few words we can define:

- `Lens`es as getters and setters that compose making the accessing of nested data structure's fields quite easy.
- `Prism`s as first-class pattern matching that let us access branches of an ADT and that also compose.

And `Classy Optics` as the idea of "associate with each type a typeclass full of optics for that type".

***So what am I talking about and how can these concepts help us solving the http error handling problem?***

Remember that I defined the `UserError` ADT by extending `Exception`?

```scala
sealed trait UserError extends Exception
case class UserAlreadyExists(username: String) extends UserError
case class UserNotFound(username: String) extends UserError
case class InvalidUserAge(age: Int) extends UserError
```

Well there's a reason! By making `UserError` a subtype of `Exception` (and by default of `Throwable`) we can take advantage of `Prisms` by going back and forth in the types. See what I'm going yet?

`UserRoute` has a `Sync[F]` constraint, meaning that we have available a `MonadError[F, Throwable]` instance, but we would like to have `MonadError[F, UserError]` instead to leverage the Scala compiler. The caveat is that the error types need to be of the same family so we can derive a `Prism` that can navigate the errors types in one direction or another. But how do we derive it?

#### Cats Meow MTL

Fortunately our friend [Oleg Pyzhcov](https://twitter.com/oleg_pyzhcov) has created this great library named [meow-mtl](https://github.com/oleg-py/meow-mtl) that makes heavy use of [Shapeless](https://github.com/milessabin/shapeless) in order to derive `Lenses` and `Prisms` and it provides instances for some `cats-effect` compatible datatypes.

And two of the supported typeclasses are `ApplicativeError` and `MonadError` as long as the error type is a subtype of `Throwable` to make it compatible with `cats-effect`. So we can do something like this:

```tut:book:silent
import cats.MonadError
import cats.effect.IO
import com.olegpy.meow.hierarchy._ // All you need is this import!
import scala.util.Random

case class CustomError(msg: String) extends Throwable

def customHandle[F[_], A](f: F[A], fallback: F[A])(implicit ev: MonadError[F, CustomError]): F[A] =
  f.handleErrorWith(_ => fallback)

val io: IO[Int] = IO(Random.nextInt(2)).flatMap { case 1 => IO.raiseError(new Exception("boom")) }
customHandle(io, IO.pure(123))
```

#### Generalizing Http Error Handling

Now back to our use case. We can't have a `MonadError[F, UserError]` constraint because there's already a `MonadError[F, Throwable]` in scope given our `Sync[F]` constraint. But it turns out we can make this work if we also abstract over the error handling by introducing an `HttpErrorHandler` algebra where the error type is a subtype of `Throwable`.

```tut:book:silent
trait HttpErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object HttpErrorHandler {
  def apply[F[_], E <: Throwable](implicit ev: HttpErrorHandler[F, E]) = ev
}
```

`UserRoutes` can now have an additional constraint of type `HttpErrorHandler[F, UserError]` so we clearly know what kind of errors we are dealing with and can have the Scala compiler on our side.

```tut:book:silent
class UserRoutesMTL[F[_]: Sync](userAlgebra: UserAlgebra[F])(implicit H: HttpErrorHandler[F, UserError]) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "users" / username =>
      userAlgebra.find(username).flatMap {
        case Some(user) => Ok(user.asJson)
        case None => NotFound(username.asJson)
      }

    case req @ POST -> Root / "users" =>
      req.as[User].flatMap { user =>
        userAlgebra.save(user) *> Created(user.username.asJson)
      }

    case req @ PUT -> Root / "users" / username =>
      req.as[UserUpdateAge].flatMap { userUpdate =>
        userAlgebra.updateAge(username, userUpdate.age) *> Created(username.asJson)
      }
  }

  val routes: HttpRoutes[F] = H.handle(httpRoutes)

}
```

We are basically delegating the error handling (AKA mapping business errors to appropiate http responses) to a specific algebra.

We also need an implementation for this algebra in order to handle errors of type `UserError` but first we can introduce a `RoutesHttpErrorHandler` object that encapsulates the repetitive task of handling errors given an `HttpRoutes[F]`:

```tut:book:silent
import cats.ApplicativeError
import cats.data.{Kleisli, OptionT}

object RoutesHttpErrorHandler {
  def apply[F[_], E <: Throwable](routes: HttpRoutes[F])(handler: E => F[Response[F]])(implicit ev: ApplicativeError[F, E]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes.run(req).value.handleErrorWith { e => handler(e).map(Option(_)) }
      }
    }
}
```

And our implementation:

```tut:book:silent
class UserHttpErrorHandler[F[_]](implicit M: MonadError[F, UserError]) extends HttpErrorHandler[F, UserError] with Http4sDsl[F] {
  private val handler: UserError => F[Response[F]] = {
    case InvalidUserAge(age) => BadRequest(s"Invalid age $age".asJson)
    case UserAlreadyExists(username) => Conflict(username.asJson)
    case UserNotFound(username) => NotFound(username.asJson)
  }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
  }
```

If we forget to handle some errors the compiler will shout at us ***"match may not be exhaustive!"*** That's fantastic :)

#### Wiring all the components

And the last part will be the wiring of all these components where we need to include the `meow-mtl` import to figure out the derivation of the instances we need in order to make this work. It'll look something like this if using `cats.effect.IO`:

```tut:book:silent
import com.olegpy.meow.hierarchy._

implicit val userHttpErrorHandler: HttpErrorHandler[IO, UserError] = new UserHttpErrorHandler[IO]

UserInterpreter.create[IO].flatMap { UserAlgebra =>
  val routes = new UserRoutesMTL[IO](UserAlgebra)
  IO.unit // pretend this is the rest of your program
}
```

### Final thoughts

This is such an exciting time to be writing pure functional programming in Scala! The Typelevel ecosystem is getting richer and more mature, having an amazing set of libraries to solve business problems in an elegant and purely functional way.

I hope you have enjoyed this post and please do let me know if you know of better ways to solve this problem in the comments!

And last but not least I would like to thank all the friendly folks I hang out with in the `cats-effect`, `cats`, `fs2` and `http4s` Gitter channels for all the time and effort they put (*for free*) into making this community an amazing space.

**UPDATE:** See the new article [Error handling in Http4s with classy optics – Part 2](https://typelevel.org/blog/2018/11/28/http4s-error-handling-mtl-2.html).


