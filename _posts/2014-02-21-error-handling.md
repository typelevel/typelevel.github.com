---
layout: post
title: How do I error handle thee?
meta:
  nav: blog
  author: adelbertc
  pygments: true
---

Scala has several ways to deal with error handling, and often times people
get confused as to when to use what. This post hopes to address that.

_Let me count the ways._

## `Option`

People coming to Scala from Java-like languages are often told `Option` is
a replacement for `null` or exception throwing. Say we have a function that
creates some sort of interval, but only allows intervals where the lower bound
comes first.

```scala
class Interval(val low: Int, val high: Int) {
  if (low > high)
    throw new Exception("Lower bound must be smaller than upper bound!")
}
```

Here we want to create an `Interval`, but we want to ensure that the lower bound
is smaller than the upper bound. If it isn't, we throw an exception. The idea here
is to have some sort of "guarantee" that if at any point I'm given an `Interval`,
the lower bound is smaller than the upper bound (otherwise an exception would have
been thrown).

However, throwing exceptions breaks our ability to reason about a function/program.
Control is handed off to the call site, and we hope the call site catches it – if not,
it propagates further up until at some point something catches it, or our program
crashes. We'd like something a bit cleaner than that.

Enter `Option` – given our `Interval` constructor, construction may or may not succeed.
Put another way, after we enter the constructor, we may or may not have a valid
`Interval`. `Option` is a type that represents a value that may or may not be there;
it can either be `Some` or `None`. Let's use what's called a _smart constructor_.

```scala
final class Interval private(val low: Int, val high: Int)

object Interval {
  def apply(low: Int, high: Int): Option[Interval] =
    if (low <= high) Some(new Interval(low, high))
    else None
}
```

We make our class `final` so nothing can inherit from it, and we make our constructor
private so nobody can create an instance of `Interval` without going through our own
smart constructor function, `Interval.apply`. Our `apply` function takes some relevant
parameters, and returns an `Option[Interval]` that may or may not contain our constructed
`Interval`. Our function does not arbitrarily kick control back to the call site due
to an exception and we can reason about it much more easily.

## `Either` and `scalaz.\/`

So, `Option` gives us `Some` or `None`, which is all we need if there is only one thing
that could go wrong. For instance, the standard library's `Map[K, V]` has a function `get`
that given a key of type `K`, returns `Option[V]` – clearly if the key exists, the associated
value is returned (wrapped in a `Some`). If the key does not exist, it returns a `None`.

But sometimes one of several things can go wrong. Let's say we have some wonky type that
wants a string that is exactly of length 5 and another string that is a palindrome.

```scala
final class Wonky private(five: String, palindrome: String)

object Wonky {
  def validate(five: String, palindrome: String): Option[Wonky] =
    if (five.size != 5) None
    else if (palindrome != palindrome.reverse) None
    else Some(new Wonky(five, palindrome))
}

/* Somewhere else.. */
val w = Wonky.validate(x, y) // say this returns None
```

Clearly something went wrong here, but we don't know what. If the strings were sent over
from some front end via JSON or something, when we send an error back hopefully we have
something more descriptive than "Something went wrong." What we want is instead of `None`,
we want something more descriptive. We can look into `Either` for this, where we use
`Left` to hold some sort of error value (similar to `None`), and `Right` to hold a successful
one (similar to `Some`).

To manipulate such values that may or may not exist (presumably obtained from functions that may or may not
fail), we use monadic functions such as `flatMap`, often in the form of monad comprehensions, or
for comprehensions as Scala calls them.

```scala
val x = ...
val y = ...

for {
  a <- foo(x)
  b <- bar(a)
  c <- baz(y)
  d <- quux(b, c)
} yield d
```

In the case of `Option`, if any of `foo/bar/baz/quux` returns a `None`, that `None` simply
gets threaded through the rest of the computation – no `try/catch` statements marching off
the right side of the screen!

For comprehensions in Scala require the type we're working with to have `flatMap` and
`map`. `flatMap`, along with `pure` and some laws, are the requisite functions needed
to form a monad – `map` can be defined in terms of `flatMap` and `pure`.
With `scala.util.Either` however, we don't have those – we have
to use an explicit conversion via `Either#right` or `Either#left` to get a
`RightProjection` or `LeftProjection` (respectively), which specifies in what direction we bias
the `map` and `flatMap` calls. The convention however, is that the right side is the "correct"
(or "right", if you will) side and the left represents the failure case, but it is tedious to
continously call `Either#right` on values of type `Either` to achieve this.

Thankfully, we have an alternative in the [Scalaz](http://typelevel.org/) library via
`scalaz.\/` (I just pronounce this "either" – some say disjoint union or just "or"), a right-biased
version of `scala.util.Either` – that is, calling `\/#map` maps over the value if it's in
a "right" (`scalaz.\/-`), otherwise if it's "left" (`scalaz.-\/`) it just threads it through
without touching it, much like how `Option` behaves. We can therefore alter the earlier function:

```scala
sealed abstract class WonkyError
case class MustHaveLengthFive(s: String) extends WonkyError
case class MustBePalindromic(s: String) extends WonkyError

final class Wonky private(five: String, palindrome: String)

object Wonky {
  def validate(five: String, palindrome: String): WonkyError \/ Wonky =
    if (five.size != 5) -\/(MustHaveLengthFive(five))
    else if (palindrome != palindrome.reverse) -\/(MustBePalindromic(palindrome))
    else \/-(new Wonky(five, palindrome))
}

/* Somewhere else.. */
val w = Wonky.validate(x, y)
```

`scalaz.\/` also has several useful methods not found on `Either`.

## `Try`

As of Scala 2.10, we have `scala.util.Try` which is essentially an either, with the left type
fixed as `Throwable`. There are two problems (that I can think of at this moment) with this:

1. We want to avoid exceptions where we can.
2. It violates the monad laws.

A big factor in our ability to deal with all these error handling types nicely
is using their monadic properties in for comprehensions.

For an explanation of the monad laws, there is a nice post
[here](http://eed3si9n.com/learning-scalaz/Monad+laws.html) describing them (using Scala). `Try`
violates the left identity.

```scala
def foo[A, B](a: A): Try[B] = throw new Exception("oops")

foo(1) // exception is thrown

Try(1).flatMap(foo) // scala.util.Failure
```

This can cause unexpected behavior when used, perhaps in a monad/for comprehension. Furthermore,
`Try` encourages the use of `Throwable`s which breaks control flow and parametricity.
While it certainly may be convenient to be able to wrap an arbitrarily code block with the `Try` constructor
and let it catch any exception that may be thrown, we still recommend using an algebraic data type
describing the errors and using `YourErrorType \/ YourReturnType`.

## `scalaz.Validation`

Going back to our previous example with validating wonky strings, we see an improvement that
could be made.

```scala
sealed abstract class WonkyError
case class MustHaveLengthFive(s: String) extends WonkyError
case class MustBePalindromic(s: String) extends WonkyError

final class Wonky private(five: String, palindrome: String)

object Wonky {
  def validate(five: String, palindrome: String): WonkyError \/ Wonky =
    if (five.size != 5) -\/(MustHaveLengthFive(five))
    else if (palindrome != palindrome.reverse) -\/(MustBePalindromic(palindrome))
    else \/-(new Wonky(five, palindrome))
}

/* Somewhere else.. */
val w = Wonky.validate("foo", "bar") // -\/(MustHaveLengthFive("foo"))
```

The fact that one string must have a length of 5 can be checked and reported separately from the other 
being palindromic. Note that in the above example `"foo"` does not satisfy the length requirement,
and `"bar"` does not satisfy the palindromic requirement, yet only `"foo"`'s error is reported
due to how `\/` works. What if we want to report any and all errors that could be reported
("foo" does not have a length of 5 and "bar" is not palindromic)?

If we want to validate several properties at once, and return any and all validation errors,
we can turn to `scalaz.Validation`. The modified function would look something like:

```scala
sealed abstract class WonkyError
case class MustHaveLengthFive(s: String) extends WonkyError
case class MustBePalindromic(s: String) extends WonkyError

final class Wonky private(five: String, palindrome: String)

object Wonky {
  def checkFive(five: String): ValidationNel[WonkyError, String] =
    if (five.size != 5) MustHaveLengthFive(five).failNel
    else five.success

  def checkPalindrome(p: String): ValidationNel[WonkyError, String] =
    if (p != p.reverse) MustBePalindromic(p).failNel
    else p.success

  def validate(five: String, palindrome: String): ValidationNel[WonkyError, Wonky] =
    (checkFive(five) |@| checkPalindrome(palindrome)) { (f, p) => new Wonky(f, p) }
}

/* Somewhere else.. */
// Failure(NonEmptyList(MustHaveLengthFive("foo"), MustBePalindromic("bar")))
Wonky.validate("foo", "bar")

// Failure(NonEmptyList(MustBePalindromic("bar")))
Wonky.validate("monad", "bar")

// Success(Wonky("monad", "radar"))
Wonky.validate("monad", "radar")
```

Awesome! However, there is one caveat – we cannot in good conscience use
`scalaz.Validation` in a for comprehension. Why? Because there is no valid
monad for it. `Validation`'s accumulative nature works via its `Applicative`
instance, but due to how the instance works, there is no consistent monad
(every monad is an applicative functor, where monadic bind is consistent with
applicative apply). However, you can use the `Validation#disjunction` function to
convert it to a `scalaz.\/`, which can then be used in a for comprehension.

One more thing to note: in the above code snippet I used
`ValidationNel`, which is just a type alias.
`ValidationNel[E, A]` stands for for 
`Validation[NonEmptyList[E], A]` – the actual `Validation` will take
anything on the left side that is a `Semigroup`, and `ValidationNel` is
provided as a convenience as often times you may want a non-empty
list of errors describing the various errors that happened in a function.
However, you can do several interesting things with other semigroups.
