---
layout: post
title: Rethinking MonadError

meta:
  nav: blog
  author: lukajcb
  pygments: true

tut:
  scala: 2.12.4
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.4
    - org.typelevel::cats-core:1.1.0

---

`MonadError` is a very old type class, hackage shows me it was originally added in 2001, long before I had ever begun doing functional programming, just check the [hackage page](https://hackage.haskell.org/package/mtl-2.2.2/docs/Control-Monad-Error-Class.html).
In this blog post I'd like to rethink the way we use `MonadError` today.
It's usually used to signal that a type might be capable of error handling and is basically like a type class encoding of `Either`s ability to short circuit.
That makes it pretty useful for building computations from sequences of values that may fail and then halt the computation or to catch those errors in order to resume the computation.
It's also parametrized by its error type, making it one of the most common example of multi-parameter type classes.
Some very common instances include `Either` and `IO`, but there are a ton more.

We can divide instances into 3 loosely defined groups:

First we have simple data types like `Either`, `Option` or `Ior` (with `Validated` not having a `Monad` instance). 

Secondly we've got the `IO`-like types, the various `IO`s, `Task`s and the like. These are used to suspend side effects which might have errors and therefore need to be able to handle these.

Thirdly and least importantly, we have monad transformers, which get their instances from their respective underlying monads. Since they basically just propagate their underlying instances we're only going to talk about the first two groups for now.

The simple data types all define `MonadError` instances, but I wager they're not actually used as much. This is because `MonadError` doesn't actually allow us to deconstruct e.g. an `Either` to actually handle the errors. We'll see more on that later, next let's look at the `IO`-like types and their instances.

`cats.effect.IO` currently defines a `MonadError[IO, Throwable]`, meaning that it's fully able to raise and catch errors that might be thrown during evaluation of encapsulated side effects.
Using `MonadError` with these effect types seems a lot more sensical at first, as you can't escape `IO` even when you handle errors, so it looks like it makes sense to stay within `IO` due to the side effect capture. 

The problem I see with `MonadError` is that it does not address the fundamental difference between these two types of instances. I can pattern match an `Option[A]` with a default value to get back an `A`. With `IO` that is just not possible. So these two groups of types are pretty different, when does it actually make sense to abstract over both of them?
Well, it turns out there a few instances where it might be useful, but as we'll see later, I'm proposing something that will be equally useful to both groups.

Now before we continue, let's look at the `MonadError` type class in a bit more detail.
`MonadError` currently comprises two parts, throwing and catching errors.
To begin let's have a look at the `throw` part, sometimes also called `MonadThrow`:

```scala
trait MonadError[F[_], E] extends Monad[F] {
  def raiseError[A](e: E): F[A]

  ...
}
```

This looks fine for now, but one thing that strikes me is that the `F` type seems to "swallow" errors.
If we look at `F[A]` we have no clue that it might actually yield an error of type `E`, that fact is not required to be represented at all.
However, that's not a really big issue, so now let's look at the `catch` part:

```scala
trait MonadError[F[_], E] extends MonadThrow[F, E] {
  ...

  def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]
}
```

Immediately I have a few questions, if the errors are handled, why does it return the exact same type?
Furthermore if this is really supposed to handle errors, what happens if I have errors in the `E => F[A]` function? 
This is even more blatant in the `attempt` function:

```scala
trait MonadError[F[_], E] extends Monad[F] {
  def attempt[A](fa: F[A]): F[Either[E, A]]
}
```

Here there is no way the outer `F` still has any errors, so why does it have the same type?
Shouldn't we represent the fact that we handled all the errors in the type system?
This means you can't actually observe that the errors are now inside `Either`. That leads to this being fully legal code:

```scala
import cats.implicits._
// import cats.implicits._

Option(42).attempt.attempt.attempt.attempt
// res0: Option[Either[Unit,Either[Unit,Either[Unit,Either[Unit,Int]]]]] = Some(Right(Right(Right(Right(42)))))
```

Another example that demonstrates this is the fact that calling `handleError`, which looks like this:

```scala
def handleError[A](fa: F[A])(f: E => A): F[A]
```
also returns an `F[A]`. This method takes a pure function `E => A` and thus can not fail during recovery like `handleErrorWith`, yet it still doesn't give us any sign that it doesn't throw errors.
For `IO`-like types this is somewhat excusable as something like an unexceptional `IO` is still very uncommon, but for simple data types like `Either` or `Some` that function should just return an `A`, since that's the only thing it can be.
Just like with `attempt`, we can infinitely chain calls to `handleError`, as it will never change the type.

Ideally our type system should stop us from being able to write this nonsensical code and give us a way to show anyone reading the code that we've already handled errors.
Now I'm not saying that the functions on `MonadError` aren't useful, but only that they could be more constrained and thus more accurate in their representation. 


For this purpose let's try to write a different `MonadError` type class, one that's designed to leverage the type system to show when values are error-free, we'll call it `MonadBlunder` for now.

To mitigate the problems with `MonadError` we have a few options, the first one I'd like to present is using two different type constructors to represent types that might fail and types that are guaranteed not to. So instead of only a single type constructor our `MonadBlunder` class will have two:

```scala
trait MonadBlunder[F[_], G[_], E]
```

Our type class now has the shape `(* -> *) -> (* -> *) -> * -> *`, which is quite a handful, but I believe we can justify its usefulness.
The first type parameter `F[_]` will represent our error-handling type, which will be able to yield values of type `E`.
The second type parameter `G[_]` will represent a corresponding type that does not allow any errors and can therefore guarantee that computations of the form `G[A]` will always yield a value of type `A`.

Now that we figured out the shape, let's see what we can actually do with it.
For throwing errors, we'll create a `raiseError` function that should return a value inside `F`, as it will obviously be able to yield an error.

```scala
trait MonadBlunder[F[_], G[_], E] {
  def raiseError[A](e: E): F[A]
}
```

This definition looks identical to the one defined one `MonadError` so let's move on to error-handling.
For handled errors, we want to return a value inside `G`, so our `handleErrorWith` function should indeed return a `G[A]`:

```scala
trait MonadBlunder[F[_], G[_], E] {
  ...

  def handleErrorWith[A](fa: F[A])(f: E => F[A]): G[A]
}
```

Looks good so far, right? 
Well, we still have the problem that `f` might return an erronous value, so if we want to guarantee that the result won't have any errors, we'll have to change that to `G[A]` as well:

```scala
trait MonadBlunder[F[_], G[_], E] {
  ...

  def handleErrorWith[A](fa: F[A])(f: E => G[A]): G[A]
}
```

And now we're off to a pretty good start, we fixed one short coming of `MonadError` with this approach.

Another approach, maybe more obvious to some, might be to require the type constructor to take two arguments, one for the value and one for the error type.
Let's see if we can define `raiseError` on top of it:

```scala
trait MonadBlunder[F[_, _]] {
  def raiseError[E, A](e: E): F[E, A]

  ...
}
```

This looks pretty similar to what we already have, though now we have the guarantee that our type doesn't actually "hide" the error-type somewhere.
Next up is `handleErrorWith`. Ideally after we handled the error we should again get back a type that signals that it doesn't have any errors. 
We can do exactly that by choosing an unhabited type like `Nothing` as our error-type:


```scala
trait MonadBlunder[F[_, _]] {
  ...

  def handleErrorWith[E, A](fa: F[E, A])(f: E => F[Nothing, A]): F[Nothing, A]
}
```

And this approach works as well, however now we've forced the two type parameter shape onto implementors. This `MonadBlunder` has the following kind `(* -> * -> *) -> *`.
This means we can very easily define instances for types with two type parameters like `Either`.
However, one issue might be that it's much easier to fit a type with two type parameters onto a type class that expects a single type constructor `(* -> *)` than to do it the other way around.

For example try to implement the above `MonadBlunder[F[_, _]]` for the standard `cats.effect.IO`.
It's not going to be simple, whereas with the first encoding we can easily encode both `Either` and `IO`. For this reason, I will continue this article with the first encoding using the two different type constructors.

Next we're going to look at laws we can define to make sense of the behaviour we want.
The first two laws should be fairly obvious. 
If we `flatMap` over a value created by `raiseError` it shouldn't propogate:

```scala
def raiseErrorStops(e: E, f: A => F[A]): Boolean =
  F.raiseError[A](e).flatMap(f) === F.raiseError[A](e)
```

Next we're going to formulate a law that states, that raising an error and then immediatly handling it with a given function should be equivalent to just calling that function on the error value:

```scala
def raiseErrorHandleErrorWith(e: E, f: E => G[A]): Boolean =
  raiseError[A](e).handleErrorWith(f) === f(e)
```

Another law could state that handling errors for a pure value lifted into the `F` context does nothing and is equal to the pure value in the `G` context:

```scala
def handleErrorPureIsPure(a: A, f: E => G[A]): Boolean =
  a.pure[F].handleErrorWith(f) === a.pure[G]
```

Those should be good for now, but we'll be able to find more when we add more derived functions to our type class.
Also note that none of the laws are set in stone, these are just the ones I came up with for now, it's completely possible that we'll need to revise these in the future.

Now let's focus on adding extra functions to our type class. `MonadError` offer us a bunch of derived methods that can be really useful. For most of those however we need access to methods like `flatMap` for both `F` and `G`, so before we figure out derived combinators, let's revisit how exactly we define the type class.

The easiest would be to give both `F` and `G` a `Monad` constraint and move on. 
But then we'd have two type classes that both define a `raiseError` function extends `Monad`, and we wouldn't be able to use them together, since that would cause ambiguities and as I've said before, the functions on `MonadError` are useful in some cases.

Instead, since I don't really like duplication and the fact that we're not going to deprecate `MonadError` overnight, I decided to extend `MonadBlunder` from `MonadError` for the `F` type, to get access to the `raiseError` function.
If `raiseError` and `handleErrorWith` were instead separated into separate type classes (as is currently the case in the PureScript prelude), we could extend only the `raiseError` part.
This also allows us to define laws that our counterparts of functions like `attempt` and `ensure` are consistent with the ones defined on `MonadError`.
So the type signature now looks like this (expressed in Haskell, since it's easier on the eyes):
```haskell
class (MonadError f e, Monad g) => MonadBlunder f g e | f -> e, f -> g where
  ...
```

In Scala, we can't express this as nicely, so we're going to have to use something close to the `cats-mtl` encoding:

```scala
trait MonadBlunder[F[_], G[_], E] {
  val monadErrorF: MonadError[F, E]
  val monadG: Monad[G]
  
  ...
}
```

Now since this means that any instance of `MonadBlunder` will also have an instance of `MonadError` on `F`, we might want to rename the functions we've got so far.
Here's a complete definition of what we've come up with with `raiseError` removed and `handleErrorWith` renamed to `handleBlunderWith`:

```scala
trait MonadBlunder[F[_], G[_], E] {
  val monadErrorF: MonadError[F, E]
  val monadG: Monad[G]

  def handleBlunderWith[A](fa: F[A])(f: E => G[A]): G[A]
}
```

Now let us go back to defining more derived functions for `MonadBlunder`.
The easiest probably being `handleError`, so let's see if we can come up with a good alternative:

```scala
trait MonadBlunder[F[_], G[_], E] {
  ...

  def handleBlunder[A](fa: F[A])(f: E => A): G[A] = 
    handleBlunderWith(fa)(f andThen (_.pure[G]))
}
```

This one is almost exactly like `handleBlunderWith`, but takes a function from `E` to `A` instead of to `G[A]`. We can easily reuse `handleBlunderWith` by using `pure` to go back to `E => G[A]`.

Next another function that's really useful is `attempt`.
Our alternative, let's call it `endeavor` for now, should return a value in `G` instead, which doesn't have a `MonadError` instance and therefore can not make any additional calls to `endeavor`:


```scala
trait MonadBlunder[F[_], G[_], E] {
  ...

  def endeavor[A](fa: F[A]): G[Either[E, A]] =
    handleBlunder(fa.map(Right(_)))(Left(_))
}
```

The implementation is fairly straightforward as well, we just handle all the errors by lifting them into the left side of an `Either` and map successful values to the right side of `Either`.

Next, let's look at the dual to `attempt`, called `rethrow` in Cats. 
For `MonadError` it turns an `F[Either[E, A]]` back into an `F`, but we're going to use our unexceptional type again:

```scala
trait MonadBlunder[F[_], G[_], E] {
  ...

  def absolve[A](fa: G[Either[E, A]]): F[A] = ???
}
```


But looking at this signature, we quickly realize that we need a way to get back to `F[A]` from `G[A]`.
So we're going to add another function to our minimal definition:

```scala
trait MonadBlunder[F[_], G[_], E] {
  val monadErrorF: MonadError[F, E]
  val monadG: Monad[G]

  def handleBlunderWith[A](fa: F[A])(f: E => G[A]): G[A]

  def accept[A](ga: G[A]): F[A]

}
```

This function `accept`, allows us to lift any value without errors into a context where errors might be present.

We can now formulate a law that values in `G` never stop propagating, so `flatMap` should always work, we do this by specifying that calling `handleBlunder` after calling `accept` on any `G[A]`, is never going to actually change the value:

```scala
def gNeverHasErrors(ga: G[A], f: E => A): Boolean =
  accept(ga).handleBlunder(f) === ga
```

Now we can go back to implementing the `absolve` function:

```scala
def absolve[A](gea: G[Either[E, A]]): F[A] =
  accept(gea).flatMap(_.fold(raiseError[A], _.pure[F]))
``` 

Now that we've got the equivalent of both `attempt` and `rethrow`, let's add a law that states that the two should cancel each other out:

```scala
def endeavorAbsolve(fa: F[A]): Boolean =
  absolve(fa.endeavor) === fa
```

We can also add laws so that `handleBlunder` and `endeavor` are consistent with their counterparts now that we have `accept`:

```scala
def deriveHandleError(fa: F[A], f: E => A): Boolean =
  accept(fa.handleBlunder(f)) === fa.handleError(f)

def deriveAttempt(fa: F[A]): Boolean =
  accept(fa.endeavor) === fa.attempt
```

One nice thing about `attempt`, is that it's really easy to add a derivative combinator that doesn't go to `F[Either[E, A]]`, but to the isomorphic monad transformer `EitherT[F, E, A]`.
We can do the exact same thing with `endeavor`:


```scala
def endeavorT[A](fa: F[A]): EitherT[G, E, A] =
  EitherT(endeavor(fa))
```

One last combinator I'd like to "port" from `MonadError` is the `ensureOr` function.
`ensureOr` turns a successful value into an error if it does not satisfy a given predicate.
We're going to name the counterpart `assureOr`:

```scala
def assureOr[A](ga: G[A])(error: A => E)(predicate: A => Boolean): F[A] =
  accept(ga).flatMap(a =>
    if (predicate(a)) a.pure[F] else raiseError(error(a))
  )
```

This plays nicely with the rest of our combinators and we can again add a law that dictates it must be consistent with `ensureOr`:

```scala
def deriveEnsureOr(ga: G[A])(error: A => E)(predicate: A => Boolean): Boolean =
  ensureOr(accept(ga))(error)(predicate) === assureOr(ga)(error)(predicate)
```

Now we have a great base to work with laws that should guarantee principled and sensible behaviour.
Next we'll actually start defining some instances for our type class.

The easiest definitions are for `Either` and `Option`, though I'm not going to cover both, as the instances for `Option` can simply be derived by `Either[Unit, A]`and I'm going to link to the code at the end.
For `Either[E, A]`, when we handle all errors of type `E`, all we end up with is `A`, so the corresponding `G` type for our instance should be `Id`.
That leaves us with the following definition:

```scala
implicit def monadBlunderEither[E]: MonadBlunder[Either[E, ?], Id, E] =
  new MonadBlunder[Either[E, ?], Id, E] {
    val monadErrorF = MonadError[Either[E, ?], E]
    val monadG = Monad[Id]

    def handleBlunderWith[A](fa: Either[E, A])(f: E => A): A = fa match {
      case Left(e) => f(e)
      case Right(a) => a
    }

    def accept[A](ga: A): Either[E, A] = Right(ga)
  }
```

Fairly straightforward, as `Id[A]` is just `A`, but with this instance we can already see a small part of the power we gain over `MonadError`.
When we handle errors with `handleBlunder`, we're no longer "stuck" inside the `Either` Monad, but instead have a guarantee that our value is free of errors.
Sometimes it'll make sense to stay inside `Either`, but we can easily get back into `Either`, so we have full control over what we want to do.

Next up, we'll look at `IO` and the type that inspired this whole blog post `UIO`.
`UIO` is equivalent to an `IO` type where all errors are handled and is short for "unexceptional IO".
`UIO` currently lives inside my own `cats-uio` library, but if things go well, we might see it inside `cats-effect` eventually. This would also work for `IO` types who use two type parameters `IO[E, A]` where the first represents the error type and the second the actual value. There you'd choose `IO[E, A]` as the `F` type and `IO[Nothing, A]` as the `G` type. `IO[Nothing, A]` there is equivalent to `UIO[A]`.

As one might expect, you can not simply go from `IO[A]` to `UIO[A]`, but we'll need to go from `IO[A]` to `UIO[Either[E, A]]` instead, which if you look at it, is exactly the definition of `endeavor`.
Now let's have a look at how the `MonadBlunder` instance for `IO` and `UIO` looks:

```scala
implicit val monadBlunderIO: MonadBlunder[IO, UIO, Throwable] = 
  new MonadBlunder[IO, UIO, Throwable] {
    val monadErrorF = MonadError[IO, Throwable]
    val monadG = Monad[UIO]

    def handleBlunderWith[A](fa: IO[A])(f: Throwable => UIO[A]): UIO[A] =
      UIO.unsafeFromIO(fa.handleErrorWith(f andThen accept))

    def accept[A](ga: UIO[A]): IO[A] = UIO.runUIO(ga)
  }
```

And voila! We've got a fully working implementation that will allow us to switch between these two types whenever we have a guarantee that all errors are handled.
This makes a lot of things much simpler.
For example, if one wants to use `bracket` with `UIO`, you just need to `flatMap` to the finalizer, as `flatMap` is always guaranteed to not short-circuit.

We can also define instances for `EitherT` and `OptionT` (being isomorphic to `EitherT[F, Unit, A]`), where the corresponding unexceptional type is just the outer `F`, so `endeavor` is just a call to `.value`: 

```scala
implicit def catsEndeavorForEitherT[F[_]: Monad, E]: MonadBlunder[EitherT[F, E, ?], F, E] =
  new MonadBlunder[EitherT[F, E, ?], F, E] {
    val monadErrorF = MonadError[EitherT[F, E, ?], E]
    val monadG = Monad[F]

    override def endeavor[A](fa: EitherT[F, E, A]): F[Either[E, A]] =
      fa.value

    def handleBlunderWith[A](fa: EitherT[F, E, A])(f: E => F[A]): F[A] =
      fa.value.flatMap {
        case Left(e) => f(e)
        case Right(a) => a.pure[F]
      }

    def accept[A](ga: F[A]): EitherT[F, E, A] =
      EitherT.liftF(ga)

  }
```

Finally, it's also possible to create instances for other standard monad transformers like `WriterT`, `ReaderT` or `StateT` as long as their underlying monads themselves have instances for `MonadBlunder`, as is typical in mtl.
As their implementations are very similar we'll only show the `StateT` transformer instance:

```scala
implicit def catsEndeavorForStateT[F[_], G[_], S, E]
  (implicit M: MonadBlunder[F, G, E]): MonadBlunder[StateT[F, S, ?], StateT[G, S, ?], E] =
    new MonadBlunder[StateT[F, S, ?], StateT[G, S, ?], E] {
      implicit val F: MonadError[F, E] = M.monadErrorF
      implicit val G: Monad[G] = M.monadG

      val monadErrorF = MonadError[StateT[F, S, ?], E]
      val monadG = Monad[StateT[G, S, ?]]

      def accept[A](ga: StateT[G, S, A]): StateT[F, S, A] = ga.mapK(new (G ~> F) {
        def apply[T](ga: G[T]): F[T] = M.accept(ga)
      })

      def handleBlunderWith[A](fa: StateT[F, S, A])(f: E => StateT[G, S, A]): StateT[G, S, A] =
        IndexedStateT(s => M.handleBlunderWith(fa.run(s))(e => f(e).run(s)))

    }
```

In practice this means we can call `handleBlunderWith` on things like `StateT[IO, S, A]` and get back a `StateT[UIO, S, A]`. Pretty neat!
You can also create instances for pretty much any `MonadError` using `Unexceptional`, e.g.: `MonadBlunder[Future, Unexceptional[Future, ?], Throwable]`. The `Unexceptional` type is designed to turn any erroring type into one that doesn't throw errors by catching them with `attempt`. 

## Conclusion

In this article, I've tried to present the argument that `MonadError` is insufficient for principled error handling.
We also tried to build a solution that deals with the shortcomings described earlier.
Thereby it seeks not to replace, but to expand on `MonadError` to get a great variety of error handling capabilities.
I believe the `MonadBlunder` type class, or whatever it will be renamed to, can be a great addition not just to the Cats community, but to the functional community at large, especially as it's much easier to express in languages like `PureScript` and `Haskell`.

For now, all of the code lives inside the [cats-uio repo](https://github.com/LukaJCB/cats-uio), which houses the `MonadBlunder` type class the `UIO` data type and the `Unexceptional` data type.
I hope that this blog post gave a motivation as to why I created the library and why it might be nice to adopt some of its features into the core typelevel libraries.

Note again, that none of this is final or set in stone and before it arrives anywhere might still change a lot, especially in regards to naming (which I'm not really happy with at the moment), so if you have any feedback of any sorts, please do chime in! Would love to hear your thoughts and thank you for reading this far!
