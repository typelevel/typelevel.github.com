---
layout: post
title: Higher-kinded types: the difference between giving up, and moving forward

meta:
  nav: blog
  author: S11001001
  pygments: true
---

As its opening sentence reminds the reader–a point often missed by
many reviewers– the book
[*Functional Programming in Scala*](https://www.manning.com/books/functional-programming-in-scala)
is not a book about Scala. This [wise] choice occasionally manifests
in peculiar ways.

For example, you can go quite far into the book implementing its
exercises in languages with simpler type systems. Chapters 1-8 and 10
port quite readily to
[Java [8]](https://github.com/sbordet/fpinscala-jdk8) and C#. So
*Functional Programming in Scala* can be a very fine resource for
learning some typed functional programming, even if such languages are
all you have to work with. Within these chapters, you can remain
blissfully unaware of the limitations imposed on you by these
languages' type systems.

However, there is a point of inflection in the book at chapter 11. You
can pass through with a language such as [OCaml](https://ocaml.org/),
Scala, Haskell, [PureScript](http://www.purescript.org/), or one of a
few others. However, users of Java, C#, F#,
[Elm](http://elm-lang.org/), and many others may proceed no further,
and must turn back here.

![Various languages' chapter 11 support](/img/media/hkt-inflection.png)

Here is where abstracting over type constructors, or "higher-kinded
types", comes into play. At this point in the book, you can give up,
or proceed with a sufficiently powerful language. Let's see how this
happens.

## Functional combinators

The bread and butter of everyday functional programming, the
"patterns" if you like, is the implementation of standard functional
combinators for your datatypes, and more importantly the comfortable,
confident use of these combinators in your program.

For example, confidence with `bind`, also known as `>>=` or `flatMap`,
is very important. The best way to acquire this comfort is to
reimplement it a bunch of times, so *Functional Programming in Scala*
has you do just that.

```scala
def flatMap[B](f: A => List[B]): List[B] // in List[A]
def flatMap[B](f: A => Option[B]): Option[B] // in Option[A]
def flatMap[B](f: A => Either[E, B]): Either[E, B] // in Either[E, A]
def flatMap[B](f: A => State[S, B]): State[S, B] // in State[S, A]
```

## All `flatMap`s are the same

The similarity between these functions' types is the most obvious
surfacing of their 'sameness'. (Unless you wish to count their names,
which I do not.) That sameness is congruent: when you write functions
using `flatMap`, in any of the varieties above, these functions
inherit a sort of sameness from the underlying `flatMap` combinator.

For example, supposing we have `map` and `flatMap` for a type, we can
'tuple' the values within.

```scala
def tuple[A, B](xs: List[A], ys: List[B]): List[(A, B)] =
  xs.flatMap{a =>
    ys.map((a, _))}
    
def tuple[A, B](xs: Option[A], ys: Option[B]): Option[(A, B)] =
  xs.flatMap{a =>
    ys.map((a, _))}
    
def tuple[E, A, B](xs: Either[E, A], ys: Either[E, B]): Either[E, (A, B)] =
  xs.flatMap{a =>
    ys.map((a, _))}
    
def tuple[S, A, B](xs: State[S, A], ys: State[S, B]): State[S, (A, B)] =
  xs.flatMap{a =>
    ys.map((a, _))}
```

*Functional Programming in Scala* contains several such functions,
such as `sequence`. These are each implemented for several types, each
time with potentially the same code, if you remember to look back and
try copying and pasting a previous solution.

## To parameterize, or not to parameterize

In programming, when we encounter such great sameness -- not merely
similar code, but *identical* code -- we would like the opportunity to
*parameterize*: extract the parts that are different to arguments, and
recycle the common code for all situations.

In `tuple`'s case, what is different are

1. the `flatMap` and `map` implementations, and
2. the **type constructor**: `List`, `Option`, `State[S, ...]`, what
   have you.

We have a way to pass in implementations; that's just higher-order
functions, or 'functions as arguments'. For the type constructor, we
need 'type-level functions as arguments'.

```scala
def tuplef[F[_], A, B](xs: F[A], ys: F[B]): F[(A, B)] = ???
```

We've handled 'type constructor as argument', and will add the
`flatMap` and `map` implementations in a moment. First, let's learn
how to read this.

## Reading a higher-kinded type

Confronted with a type like this, it's helpful to sit back and muse on
the nature of a function for a moment.

Functions are given meaning by substitution of their arguments.

```scala
def double(x: Int) = x + x
```

`double` remains "an abstraction" until we *substitute for x*; in
other words, pass an argument.

```
double(2)    double(5)
2 + 2        5 + 5
4            10
```

But this isn't enough to tell us *what `double` is*; all we see from
these tests is that `double` sometimes returns 4, sometimes 10,
sometimes maybe other things. We must imagine what `double` does in
common *for all possible arguments*.

Likewise, we give meaning to type-parameterized definitions like
`tuplef` by substitution. The parameter declaration `F[_]` means that
`F` may not be a simple type, like `Int` or `String`, but instead a
one-argument type constructor, like `List` or `Option`. Performing
these substitutions for `tuplef`, we get

```scala
// original, as above
def tuplef[F[_], A, B](xs: F[A], ys: F[B]): F[(A, B)]

// F = List
def tupleList[A, B](xs: List[A], ys: List[B]): List[(A, B)]

// F = Option
def tupleOpt[A, B](xs: Option[A], ys: Option[B]): Option[(A, B)]
```

More complicated and powerful cases are available with other kinds of
type constructors, such as by partially applying. That's how we can
fit `State`, `Either`, and other such types with two or more
parameters into the `F` parameter.

```scala
// F = Either[E, ...]
def tupleEither[E, A, B](xs: Either[E, A], ys: Either[E, B]): Either[E, (A, B)]

// F = State[S, ...]
def tupleState[S, A, B](xs: State[S, A], ys: State[S, B]): State[S, (A, B)]
```

Just as with `double`, though this isn't the whole story of `tuplef`,
its true meaning arises from the common way in which it treats *all
possible* `F` arguments. That is where higher kinds start to get
interesting.

## Implementing functions with higher-kinded type

The type of `tuplef` expresses precisely our intent -- the idea of
"multiplying" two `F`s, tupling the values within -- but cannot be
implemented as written. That's because we don't have functions that
operate on `F`-constructed values, like `xs: F[A]` and `ys: F[B]`. As
with any value of an ordinary type parameter, these are opaque.

In Scala, there are a few ways to pass in the necessary functions. One
option is to implement a `trait` or `abstract class` that itself uses
a higher-kinded type parameter or abstract type constructor. Here are
a couple possibilities.

```scala
trait Bindic[F[_], +A] {
  def map[B](f: A => B): F[B]
  def flatMap[B](f: A => F[B]): F[B]
}

trait BindicTM[+A] {
  type F[X]
  def map[B](f: A => B): F[B]
  def flatMap[B](f: A => F[B]): F[B]
}
```

Note that we must use higher kinds to support our higher kinds;
otherwise, we can't write the return types for `map` and `flatMap`.

```scala
trait BindicBad[F] {
  def map[B](f: A => B): F ???
            // where is the B supposed to go?
```

Now we make every type we'd like to support either inherit from or
implicitly convert to `Bindic`, such as `List[+A] extends
Bindic[List, A]`, and write `tuplef` as follows.

```scala
def tupleBindic[F[_], A, B](xs: Bindic[F, A], ys: Bindic[F, B]): F[(A, B)] =
  xs.flatMap{a =>
    ys.map((a, _))}
```

## Escaping two bad choices

There are two major problems with `Bindic`'s representation of `map`
and `flatMap`, ensuring its wild unpopularity in the Scala functional
community, though it still appears in some places, such as
[in Ermine](https://github.com/ermine-language/ermine-parser/blob/cc77bf6e150a16129744d18d69022f7b5902814f/src/main/scala/scalaparsers/Monadic.scala).

1. The choices of inheritance and implicit conversion are both bad in
   different ways. Implicit conversion propagates very poorly -- it
   doesn't compose, after all, and fails as soon as we do something
   innocent like put the value-to-be-converted into a tuple.
   Inheritance leaves its own mess: modifying a type to add new,
   nonessential operations, and the weird way that `F` is declared in
   the method type parameters above.
2. The knowledge required to work out the new type signature above is
   excessively magical. There are rules about when implicit conversion
   happens, how much duplication of the reference to `Bindic` is
   required to have the `F` parameter infer correctly, and even how
   many calls to `Bindic` methods are performed. For example, we'd
   have to declare the `F` parameter as `F[X] <: Bindic[F, X]` if we
   did one more trailing `map` call. But then we wouldn't support
   implicit conversion cases anymore, so we'd have to do something
   else, too.

As a result of all this magic, generic functions over higher kinds
with OO-style operations tend to be ugly; note how much `tuplef`
looked like the `List`-specific type, and how little `tupleBindic`
looks like either of them.

But we still really, really want to be able to write this kind of
generic function. Luckily, we have a Wadler-made alternative.

## Typeclasses constrain higher-kinded types elegantly

To constrain `F` to types with the `flatMap` and `map` we need, we use
typeclasses instead. For `tuplef`, that means we leave `F` abstract,
and leave the types of `xs` and `ys` as well as the return type
unchanged, but add an implicit argument, the "typeclass instance",
which is a first-class representation of the `map` and `flatMap`
operations.

```scala
trait Bind[F[_]] {
  // note the new ↓ fa argument
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}
```

Then we define instances for the types we'd like to have this on:
`Bind[List]`, `Bind[Option]`, and so on, as seen in chapter 11 of
*Functional Programming in Scala*.

Now we just add the argument to `tuplef`.

```scala
def tupleTC[F[_], A, B](xs: F[A], ys: F[B])
           (implicit F: Bind[F]): F[(A, B)] =
  F.flatMap(xs){a =>
    F.map(ys)((a, _))}
```

We typically mirror the typeclass operations back to methods with an
implicit conversion -- unlike with `Bindic`, this has no effect on
exposed APIs, so is benign. Then, we can remove the `implicit F`
argument, replacing it by writing `F[_]: Bind` in the type argument
list, and write the method body as it has been written before, with
`flatMap` and `map` methods.

There's another major reason to prefer typeclasses, but let's get back
to *Functional Programming in Scala*.

## Getting stuck

I've just described many of the practical mechanics of writing useful
functions that abstract over type constructors, but *all this is moot
if you cannot abstract over type constructors*. The fact that Java
provides no such capability is not an indicator that they have
sufficient abstractions to replace this missing feature: it is simply
an abstraction that they do not provide you.

**Oh, you would like to factor this common code? Sorry, you are
stuck. You will have to switch languages if you wish to proceed.**

## Don't get stuck on the second order

`map` functions are obvious candidates for essential parts of a usable
library for functional programming. This is the first-order
abstraction -- it eliminates the concrete loops, recursive functions,
or `State` lambda specifications, you would need to write otherwise.

When we note a commonality in patterns and define an abstraction over
that commonality, we move "one order up". When we stopped simply
defining functions, and started taking functions as arguments, we
moved from the first order to the second order.

It is not enough for a modern general-purpose functional library in
Scala to simply have a bunch of `map` functions. It must also provide
the second-order feature: the ability to *abstract over* `map`
functions, as well as many, many other functions numerous type
constructors have in common. Let's not give up; let's move forward.

*This article was tested with Scala 2.11.7 and
[fpinscala](https://github.com/fpinscala/fpinscala) 5b0115a answers,
with the addition of the method variants of `List#map` and
`List#flatMap`.*
