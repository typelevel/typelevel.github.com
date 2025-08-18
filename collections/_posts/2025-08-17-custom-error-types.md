---
layout: post
title: Custom Error Types Using Cats Effect and MTL
category: technical

meta:
  nav: blog
  author: djspiewak
---

One of the most famous and longstanding limitations of the Cats Effect `IO` type (and the Cats generic typeclasses) is the fact that the only available error channel is `Throwable`. This stands in contrast to bifunctor or polyfunctor techniques, which add a typed error channel within the monad itself. You can see this easily in type signatures: `IO[String]` indicates an `IO` which returns a `String` or may produce a `Throwable` error. Something like `BIO[ParseError, String]` would represent a `BIO` that produces a `String` *or* raises a `ParseError`. The latter type signature is more general than `Throwable`, since it allows for user-specified error types, and it's somewhat more explicit about where errors can and cannot occur.

In a meaningful sense, this type of bifunctor error encoding is analogous to *checked* exceptions in Java, whereas monofunctor error encoding (like Cats Effect's `IO`) is analogous to *unchecked* exceptions. Both are valid design decisions for an effect type, but they come with different benefits and tradeoffs.

Cats has long been quite prescriptive about monofunctor effects, in part because this considerably simplifies the compositional integration space. Libraries like Fs2, Http4s, Calico, and so many more are able to build on top of parametric effects (the famous `F[_]`) with a consistent understanding of what error channels are available and how they're going to behave. This has *very* subtle interactions with concurrent logic and resource handling, and by insisting on a monofunctor calculus, the Cats ecosystem is able to maintain very strong properties with relatively simple implementations in these areas.

However, the core problem of custom error types doesn't *really* go away. Parsing is a great example of this. For example, Circe has a `ParsingFailure` type which carries a specific JSON parse error message as well as some associated traceback context. While this type does happen to extend `Exception`, and thus can be raised within an `IO`, it's not necessarily *right* for it to do so. This is common, but arguably it's only common because of the prevalence of monofunctors.

A standard solution to this problem, if you *don't* want to extend `Exception` with your error types, is to simply return `Either` everywhere. Unfortunately, that results in a lot of type signatures which look like this:

```scala
def parse(input: String): IO[Either[Failure, Result]] = ???
```

And then of course, everything you do with that result must be explicitly `flatMap`ped into the `Either`, and higher-order control flow libraries like Fs2 will often need some extra coaxing in order to make everything work the way you want it to. This gets old in a hurry, which often results in reaching for alternatives like `EitherT`. That way lies madness.

## Capabilities

The good news is that we now have a better answer here, and one which composes perfectly with the existing (and future) ecosystem, maintains all relevant concurrency properties, and which type-infers extremely well, particularly in Scala 3. The answer has been to double down on the relatively little-used implicit capabilities library for Cats, known under the very misleading name of Cats MTL.

The name "Cats MTL" comes from Haskell's MTL package, which in turn was pretty aptly named: "Monad Transformer Library". Haskell's MTL is entirely oriented around making it easier and more ergonomic to manipulate monad transformer *stacks*, which is to say, multiple layers of datatypes like `EitherT`, `Kleisli`, and so on. Monad transformer stacks are extremely difficult to work with, both in Scala and in Haskell, and so over time people progressively evolved techniques involving typeclasses in Haskell and implicits in Scala to more ergonomically manipulate composable effect types. Cats MTL was rooted in an adaptation of some of these ideas.

Over time though, we've learned that monad transformer datatypes *themselves* are often too clunky and even unnecessary. They work well in a few contexts, most notably local scopes (i.e. within the body of a single method), but they're generally the wrong solution for the problem. Quite notably, while the Cats Effect concurrent typeclasses do *work* on monad transformer stacks and derive lawful results, the practical outcomes can be very unintuitive. For that reason, it's generally not advisable to use types like `EitherT` or `IorT` composed together with libraries like Fs2 or similar.

However, the basic idea of MTL itself, divorced from the *datatypes* (like `EitherT`), is actually a very good one. At its core, MTL is just about expressing capabilities available within a given scope using implicit evidence. Capabilities can be things like parallelism, resource safety, error handling, dependency injection, sequential composition, or similar. When done correctly, this can be a very powerful and lightweight way of expressing compositional effects with a high degree of granularity and type safety. It's not a coincidence that this is exactly the route being explored by many of the researchers working on Scala academically!

## Scoped Error Capabilities

The problem has been to find a way to blend all of these constructs together in a way that practically *works* with the ecosystem, is syntactically lightweight, has pleasant type inference and errors, and doesn't confuse the heck out of anyone who touches it. That is a problem we feel we have now solved, at least with errors.

```scala
import cats.effect.IO
import cats.mtl.{Handle, Raise}

// define a domain error type
enum ParseError:
  case UnclosedBracket
  case MissingSemicolon
  case Other(msg: String)

// use that error type in some function
def parse[F[_]](input: String)(using Raise[F, ParseError], Monad[F]): F[Result] =
  // do some hardcore parsing
  if missingBracket then
    UnclosedBracket.raise[F]
  else if missingSemicolon then
    MissingSemicolon.raise[F]
  else
    result.pure[F]

// use allow/rescue like try/catch to create scoped error handling
val program: IO[Result] = Handle.allow[ParseError]:
  for
    x <- parse(inputX)
    y <- parse(inputY)
    _ <- IO.println(s"successfully parsed $x and $y")
  yield ()
.rescue:
  case ParseError.UnclosedBracket => IO.println("you didn't close your brackets")
  case ParseError.MissingSemicolon => IO.println("you missed your semicolons very much")
  case Other(msg) => IO.println(s"error: $msg")
```

There's a lot to unpack here! At the very beginning we define a custom error type, `ParseError`. This is just a domain error like any other, and you'll note that it *doesn't* extend `Exception` or `Throwable` or similar. Without Cats MTL, we would generally have to wrap this error up in `Either` in all our function's result types, if we wanted to use it (similar to what Circe does). In this case though, instead of adding the error to the result type, we added a `using` parameter to our `parse` function!

Specifically, what we're doing here when we say `using Raise[F, ParseError]` is that the `parse` method requires the ability to raise (but not handle!) errors of type `ParseError`. This is a bit like saying `throws ParseError` in Java, except it isn't an exception!

Later on, in the body of `parse`, we use this `Raise` capability to call the `raise` method, producing errors in failure cases. This is a bit like the `throw` keyword, but again with our own custom domain error type. Btw, if we had expanded our `Monad[F]` using into something like `MonadError[F, Throwable]` or, more aggressively, `Async[F]`, we would have *also* had the ability to raise any error of type `Throwable` using the same syntax! In this case though, `parse` is only able to raise domain errors.

As an aside, the `F[_]` here could be instantiated with many different monadic types. While we're using `IO` in production, perhaps we would want to test this function using `Either[ParseError, A]` as our type. This is very much supported! And in fact, if you did this, the `Raise` would have been implicitly materialized by Cats MTL, since `Either` has an obvious implementation of that function.

Finally, at the end of the snippet above, we define `program` using the brand new syntax: `allow`/`rescue`. This is where things get *very* fancy. What we're doing here is we're introducing a new lexical scope (indented after the `allow[ParseError]:`) in which it is valid to `raise` an error of type `ParseError`. You should think of this as being very similar to `try`/`catch`, except it works with effect types like `IO` and any error type you define (not just `Throwable`). Within this scope, we write code as usual, and we're allowed to call the `parse` function. Note that if we had tried to call `parse` *outside* of this scope, it would have been a compile error informing us that we're missing the `Raise` capability.

At the end of the `allow` scope, we call `.rescue`, and this requires us to pass a function which *handles* any errors which could have been raised by the body of the `allow`. This works exactly like `catch`, except with your own domain error types. In this case, we are apparently just logging the existence of the errors and moving on with our life, because we do some printing and away we go, but you could imagine perhaps returning a custom HTTP error code, or triggering some fallback behavior, or really any other error handling logic.

### Scala 2

Oh, and just in case you were wondering, this syntax *does* work on Scala 2 as well, it's just a bit less fancy! Here's the same snippet from above, but with 100% more braces and a lot more explicit types:

```scala
import cats.effect.IO
import cats.mtl.{Handle, Raise}

// define a domain error type
sealed trait ParseError extends Product with Serializable

object ParseError {
  case class UnclosedBracket extends ParseError
  case class MissingSemicolon extends ParseError
  case class Other(msg: String) extends ParseError
}

// use that error type in some function
def parse[F[_]](input: String)(implicit r: Raise[F, ParseError], m: Monad[F]): F[Result] = {
  // do some hardcore parsing
  if (missingBracket)
    UnclosedBracket.raise[F]
  else if (missingSemicolon)
    MissingSemicolon.raise[F]
  else
    result.pure[F]
}

// use allow/rescue like try/catch to create scoped error handling
val program: IO[Result] = Handle.allowF[IO, ParseError] { implicit h =>
  for {
    x <- parse[IO](inputX)
    y <- parse[IO](inputY)
    _ <- IO.println(s"successfully parsed $x and $y")
  } yield ()
} rescue {
  case ParseError.UnclosedBracket => IO.println("you didn't close your brackets")
  case ParseError.MissingSemicolon => IO.println("you missed your semicolons very much")
  case Other(msg) => IO.println(s"error: $msg")
}
```

We need to do a lot more hand-holding for the compiler by using the `allowF` function instead of `allow`, but in general this is very much the same idea!

## Under the Hood

Behind the scenes, this functionality is doing two very creative things. First, as the Scala 2 snippet hints, we're introducing a new implicit within the local scope of the function passed to `allow`/`allowF`. This is one of Scala's more unique features and we're leveraging it quite heavily. In Scala 3, we're able to hide this syntax *entirely* by using context functions (the `A ?=> B` syntax), but in Scala 2 we need to use the `implicit x =>` lambda syntax in order to make this work. (as an aside, note that this syntax does not parse if you attempt to explicitly specify the type of `x`, but that's okay because you don't ever need to do that anyway)

That implicit is introduced targeting the effect type we passed to `allowF`, or in Scala 3's case, the type which was inferred from the return. In this case, that type is `IO`! In other words, you don't need to be using parametric effects (`F[_]`) in order to make all this work! `Raise[IO, ParseError]` is a totally valid `Raise` instance, and it's exactly what we have in scope here. Or rather, we actually have `Handle[IO, ParseError]` (which extends `Raise`), which gives us the ability to both raise *and* handle errors.

Once the scope is closed, syntactically, we force the user to supply an error handler to ensure that any errors which were raised and unhandled within the body are correctly managed. This is a pretty logical way of setting up your error handling, and precisely mirrors the way that you would do this same thing with a more imperative direct syntax like `try`/`catch`/`throw`/`throws`.

In the way way deep underdark of the implementation, this whole thing works at runtime by creating what we call a "submarine error". Specifically, we have a local traceless exception type called `Submarine` inside of the `allow` implementation which extends `RuntimeException`. When you `raise` a custom domain error (`ParseError` in this case), we use `Submarine` to "submerge" your error within the `Throwable` error channel of the enclosing effect – in this case, `IO`. Since we catch this error at the boundary, this whole process is entirely invisible to you *unless* you write something like `handleErrorWith` and catch all `Throwable`-typed errors within the scope, in which case you might see something of type `Submarine`. The correct thing to do with this error type, should you see it, depends considerably on exactly *why* you're writing `handleErrorWith`, and as it turns out this is exactly the whole point!

By implementing this functionality without extending the number of actual error channels within the effect type (either with a bifunctor or something like `EitherT`), we ensure that everything continues to compose correctly around all resource handling, structured and unstructured concurrency, and otherwise-oblivious generic library code which has no idea what your domain errors are or how they might behave. Even in the case of an explicit `handleErrorWith`, you might be adding that type of error handler because you're writing some logic which must make *certain* that there is no possible way to short-circuit without passing through your handler (e.g. perhaps you're trying to make sure that some critical resource is cleaned up), or alternatively you may just be trying to observe `Throwable` errors to log and re-raise them, or any number of other things you *might* be doing with the error channel that we don't have any insight into.

Rather than trying to impose a particular multi-channel composition semantic on your code, we simply stick with a single error channel with known and well-understood supremacy semantics, and everything else follows from there.

## Conclusion

Hopefully you find this technique helpful! This has been in the works for a *surprisingly* long time (I think it was first suggested in the Typelevel Discord about two or three years ago), and it was Thanh Le ([@lenguyenthanh](https://github.com/lenguyenthanh)) who ultimately pushed it over the line. Huge shoutout!

Even more excitingly, this is a bit of a taste of the next phase of the effect type ecosystem. Scala is continuing to move heavily in the direction of implicit capabilities for these types of behaviors, and while efforts such as Caprese are still a long way from bearing real-world fruit, much of the work that is being done in that direction also creates the primitives needed to encode a compositional capabilities ecosystem for our existing production effect types, such as Cats Effect `IO`!

Cats MTL will continue to evolve in this area, with an eye towards advancing the capabilities and improving syntax and ergonomics of this type of functionality both now and in the future.
