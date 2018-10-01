---
layout: post
title: Variance of Monad Transformers

meta:
  nav: blog
  author: ceedubs
  pygments: true

tut:
  scala: 2.12.7
  binaryScala: "2.12"
  scalacOptions:
    - "-Ypartial-unification"
  dependencies:
    - org.scala-lang:scala-library:2.12.7
    - org.typelevel::cats-core:1.4.0
    - io.circe::circe-core:0.9.3

---

A question that [repeatedly](https://github.com/typelevel/cats/issues/556) [pops](https://github.com/typelevel/cats/issues/2310) [up](https://github.com/typelevel/cats/issues/2538) about [Cats](https://typelevel.org/cats/) is why monad transformer types like `OptionT` and `EitherT` aren't covariant like their `Option` and `Either` counterparts. This blog post aims to answer that question.

# Covariance

What does it mean to say that `Option` is covariant? It means that an `Option[B]` is allowed to be treated as an `Option[A]` if `B` is a subtype of `A`. For example:

```tut:silent
abstract class Err(val msg: String)
final case class NotFound(id: Long) extends Err(s"Not found: $id")
```

```tut:book
val optionNotFound: Option[NotFound] = Some(NotFound(42L))

optionNotFound: Option[Err]
```

Great. If you want to treat your `Option[NotFound]` as an `Option[Err]`, you are free to.

This is made possible because the `Option` type is declared as `sealed abstract class Option[+A]`, where the `+` in front of the `A` means that it is covariant in the `A` type parameter.

What happens if we try to do the same with `OptionT`?

```tut:silent
import cats.Eval
import cats.data.OptionT
```

```tut:book
val optionTNotFound: OptionT[Eval, NotFound] = OptionT(Eval.now(optionNotFound))
```

```tut:book:fail
optionTNotFound: OptionT[Eval, Err]
```

The compiler complains that an `OptionT[Eval, NotFound]` is _not_ an `OptionT[Eval, Err]`, but it also suggests that we may be able to fix this by using `+A` like is done with `Option`.

`OptionT` in Cats is defined as:

```scala
final case class OptionT[F[_], A](value: F[Option[A]])
```

Let's try to make the suggested change with an experimental `MyOptionT` structure:

```tut:book:fail
final case class MyOptionT[F[_], +A](value: F[Option[A]])
```

Now it's complaining that `A` is a covariant type but shows up in an invariant position. We'll discuss more about what this means later in the post, but for now let's just try declaring `F` as covariant:

```tut:silent
final case class CovariantOptionT[F[+_], +A](value: F[Option[A]])
```

```tut:book
val covOptionTNotFound: CovariantOptionT[Eval, NotFound] = CovariantOptionT(Eval.now(optionNotFound))

covOptionTNotFound: CovariantOptionT[Eval, Err]
```

Woohoo! Problem solved, right?

Well, not exactly. This works great if `F` is in fact covariant, but what if it's not? For example, the JSON library [circe](https://circe.github.io/circe/) has a `Decoder` type that _could_ be covariant but isn't (at least as of circe 0.10.0). With the invariant `OptionT` in Cats we can do something like this:

```tut:silent
import io.circe.Decoder

def defaultValueDecoder[A](defaultValue: A, optionDecoder: Decoder[Option[A]]): Decoder[A] =
  OptionT(optionDecoder).getOrElse(defaultValue)
```

However, we can't do the same with our `CovariantOptionT`, because we can't even create an `OptionT` where the `F` type isn't covariant:

```tut:book:fail
def wrap[A](optionDecoder: Decoder[Option[A]]): CovariantOptionT[Decoder, A] =
  CovariantOptionT[Decoder, A](optionDecoder)
```

In this particular case, `Decoder` _could_ be declared as covariant, but it's not. It would be unfortunate to lose the ability to use a monad transformer because a 3rd party library chose not to make a type covariant. And perhaps more importantly, sometimes you might want to use an `OptionT` with an `F` type that fundamentally isn't covariant in nature, such as `Monoid` (which is invariant) or `Order` (which is contravariant in nature and is declared as invariant in its type definition). Later in this post there will be some examples of using `OptionT` with a contravariant functor type (`Eq`) to gain acess to a handy `contramap` operation.

# Workaround

It's completely reasonable to want to be able to take your `OptionT[Eval, NotFound]` and treat it as an `OptionT[Eval, Err]`, and in some cases it may only be typed as `OptionT[Eval, NotFound]` because of type inference picking a more specific type than you intended. Luckily Cats has some handy methods to make this easy:

```tut:silent
import cats.implicits._
```

```tut:book
optionTNotFound.widen[Err]
```

The `.widen` method is available for any type that has a `Functor`. If you are working with a type that represents a `Bifunctor`, such as `EitherT`, then you can use `widen` for the type on the right and `leftWiden` for the type on the left:

```tut:silent
import cats.data.EitherT
```

```tut:book
val eitherTNotFound: EitherT[Eval, NotFound, Some[Int]] = EitherT.leftT[Eval, Some[Int]](NotFound(42L))

eitherTNotFound.widen[Option[Int]]

eitherTNotFound.leftWiden[Err]
```

Similarly there is a `narrow` method for contravariant functors:

```tut:silent
import cats.Eq
```

```tut:book
val optionTEqErr: OptionT[Eq, Err] = OptionT(Eq[Option[String]]).contramap((err: Err) => err.msg)

optionTEqErr.narrow[NotFound]
```

# Back to that compile error...

Let's return to that `covariant type A occurs in invariant position` compile error that was triggered when we tried to declare `final case class MyOptionT[F[_], +A](value: F[Option[A]])`. Why did this happen?

Pretend for a minute that the Scala compiler _did_ allow us to do this. We could then write:

```tut:silent
val eqOptionNotFound: Eq[Option[NotFound]] = Eq.instance[Option[NotFound]]{
  case (None, None) => true
  case (None, Some(_)) => false
  case (Some(_), None) => false
  case (Some(x), Some(y)) => x.id === y.id
}
```

```tut:silent:fail
val optionTEqNotFound: MyOptionT[Eq, NotFound] = MyOptionT(eqOptionNotfound) // only allowed in our pretend world

val optionTEqErr: MyOptionT[Eq, Err] = optionTEqNotFound // only allowed in our pretend world
```

Because `MyOptionT` would be covariant in `A`, this `MyOptionT[Eq, NotFound]` could be treated as a `MyOptionT[Eq, Err]`. That is, it would have a `value` that is an `Eq[Option[Err]]`. But if you look at how we implemented our equality check, it's taking two `NotFound` instances and comparing their `id` fields. For a general `Err`, we have no guarantee that it will be a `NotFound` and that it will have an `id` field. We can't treat an `Eq[Option[NotFound]]` as an `Eq[Option[Err]]`, because `Eq` is contravariant and _not_ covariant in nature. The `covariant type A occurs in invariant position` message that the scala compiler gave us was the compiler correctly identifying that our code was unsound.

# Conclusion

There are some use-cases in which having monad transforers such as `OptionT` and `EitherT` be defined as covariant would be convenient, such as helping to nudge type inference in the right direction. However, if Cats were to define these types as covariant, it would eliminate the possibility of using them with non-covariant types. Forcing covariance in Cats would be overly restrictive, since third-party libraries might not declare types as covariant, and monad transformers can be useful for invariant a contravariant types. Luckily, methods such as `widen`, `leftWiden`, and `narrow` provide concise solutions to turning a monad transformer (or other type that forms a functor) into the type that you need.
