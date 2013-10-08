---
layout: post
title: Towards Scalaz - Part 1

meta:
  nav: blog
  author: adelbertc
  pygments: true
---

# Towards Scalaz - Part 1
A lot of people see Scalaz as a hard fringe, ivory tower,
not suited for real-world applications library, which is
unfortunate. The goal of this blog post series is to introduce
various components of Scalaz, and hopefully through this
allow folks to gain an understanding towards the power of
Scalaz.

## Prerequisites
I assume knowledge of type classes as they are implemented
and used in Scala.

## Motivation
Our motivation for the inaugural post of the series will be
summing a `List` of something. Lets start out with `Int`,
which is simple enough.

```scala
def sum(l: List[Int]): Int = l.reduce(_ + _)
```

And this works (kind of, it fails on empty `List`s but we'll get to that).
But what if we want to sum a `List[Double]`?

```scala
def sumDoubles(l: List[Double]): Double = l.reduce(_ + _)
```

The code is the same, modulo the type parameter. In fact, the
code would be the same whether it is `Int`, `Double`, or `BigInt`.
Being the good programmers that we are, let's make this generic
in that respect with the help of `scala.math.Numeric`.

```scala
def sumNumeric[A](l: List[A])(implicit A: Numeric[A]): A =
  l.reduce(A.plus)
```

## Problem
Awesome. We can now sum `List[Int]`, `List[Double]`, `List[BigInt]`,
and many more.

But let's give this a bit more thought - what if we wanted to
"sum" a `List[String]` - that is, we concatenate all the `String`s
together to create one large `String` ?

```scala
def sumStrings(l: List[String]): String = l.reduce(_ + _)
```

This looks exactly like summing `Int` and `Double`s! This however
does not work with our `sumNumeric` - there is no (sane) way to define
a `Numeric[String]`.

Another way to look at this is that we only use the `plus` method
on `Numeric`, never any of the other methods that also make sense
for numeric types. So while our function works for summing a List
of numeric types, it does not work for anything else that is not
numeric but can still be "added" (`String` and string concatenation,
`List[A]` and `List#++`).

## Making it generic
So what do we want? We want a type class that only requires instances
to be able to "add" two `A`s to get another `A`.

```scala
trait Adder[A] {
  def add(x: A, y: A): A
}
```

And let's define an instance of `Adder` for all `Numeric` types and `String`.

```scala
object Adder {
  implicit def numericHasAdder[A](implicit A: Numeric[A]): Adder[A] =
    new Adder[A] {
      def add(x: A, y: A): A = A.plus(x, y)
    }

  implicit val stringHasAdder: Adder[String] =
    new Adder[String] {
      def add(x: String, y: String): String = x + y
    }
}
```

And heres our shiny new generic summer function!

```scala
def sumGeneric[A](l: List[A])(implicit A: Adder[A]): A =
  l.reduce(A.add)
```

And now this works for `Int`, `Double`, `String`, and many more.

A good exercise at this point is to define an `Adder` instance for `List[A]`.

## Making an Exception
What happens when we pass in an empty `List` to our summer function though?
We get an exception! How do we prevent this? A common answer I get is
"Oh I know it won't happen" - this is not ideal, we want to guarantee safety
as much as possible without having to rely on human judgement.

How then do we write a safer summer function? Lets turn to an alternative
way of implementing sum on `List[Int]`.

```scala
// Old, bad version
def sum(l: List[Int]): Int = l.reduce(_ + _)

// Shiny new version
def sum(l: List[Int]): Int = l.foldLeft(0)(_ + _)
```

What happens now when we pass an empty `List` into the sum function? We get 0,
not an exception! Note that before all we gave the program was a binary
operation (what `Adder` defines), where now we give a binary option *and* a
"zero" or starting value (the 0). As it stands, we cannot write this with
`Adder` since it has no "zero".

It may be tempting to just add a `zero` method to `Adder`, but then we may run
into the same issues we had with `Numeric` later on - we dont *always* need
a "zero", sometimes a binary operation is good enough. So instead, let's create
an `AdderWithZero` type class.

```scala
trait AdderWithZero[A] extends Adder[A] {
  def zero: A
}
```

Note that while you dont see the `add` method in here, the fact
it `extends Adder` without implementing the `add` method propagates the need to
implement that method, so programmers who want to create an `AdderWithZero[A]` instance
need to implement both.

Programmers can now write functions that depend only on `Adder`, or perhaps if they
need a bit more power use `AdderWithZero`. Types that have `AdderWithZero` instances
also have `Adder` instances automatically due to subtyping.

Lets move our `Adder` instances to the `AdderWithZero` object.

```scala
object AdderWithZero {
  implicit def numericHasAdderZero[A](implicit A: Numeric[A]): AdderWithZero[A] =
    new AdderWithZero[A] {
      def add(x: A, y: A): A = A.plus(x, y)
      der zero: A = A.zero
    }

  implicit val stringHasAdder: Adder[String] =
    new Adder[String] {
      def add(x: String, y: String): String = x + y
      der zero: String = ""
    }
}
```

And finally, our shiny new generic sum function!

```scala
def sumGeneric[A](l: List[A])(implicit A: AdderWithZero[A]): A =
  l.foldLeft(A.zero)(A.add)
```

Hurrah!

## Plot Twist
It turns out that our `Adder` and `AdderWithZero` isnt just us being
sly and clever, but an actual thing! They are called `Semigroup` and
`Monoid` (respectively), taken from the wonderful field of abstract algebra. Abstract
algebra is a field dedicated to studying algebraic structures as opposed
to just numbers as we may be used to. The field looks into what properties
and operations various structures have in common, such as integers and
matrices. For instance, wee can add two integers, as well as two matrices of the same size.
This is analogous to how we noticed the `add` worked on not only `Numeric`
but `String` and `List[A]` as well! This is the kind of generecity we're looking for.

Heres what `sumGeneric` looks like in Scalaz land.

```scala
import scalaz.Monoid

def sumGeneric[A](l: List[A])(implicit A: Monoid[A]): A =
  l.foldLeft(A.zero)((x, y) => A.append(x, y))
```

Thankfully we dont have to create our own versions of `Semigroup` and `Monoid` -
Scalaz has one for us! In fact, the developers of Scalaz have been kind enough to define
several `Monoid` instances for common types such as `Numeric`, `String`, `List[A]`, etc.
There are also instances for tuples - if we have a tuple, say of type (A, B, C),
and all three types have `Monoid` instances themselves, then the whole tuple has an
instance where the `zero` is the tuple `(A.zero, B.zero, C.zero)` and the `append` is
appending corresponding pairs between the two tuples. Look for instances that may already
be defined before defining your own on existing types.


## Law-Abiding Citizen
To close todays post off, I confess one thing - defining a `Monoid` (and `Semigroup`) instance
should not be done without some thought - it is not enough that you simply have a zero and
a binary operation - to truly have a `Monoid` or `Semigroup` certain laws must be obeyed.
These laws are as follows:

Call the `append` operation `+` and the `zero` `0`. Arbitrary values of type `A` will be
referred to as `a`, `b`, etc.

### Semigroup Laws
+ must be associative. That is:

```
(a + b) + c === a + (b + c)
```

### Monoid Laws
In addition to the `Semigroup` law for the binary operation, there is:

```
(a + 0) === (0 + a) === a
```

To check these laws, Scalaz provides [ScalaCheck](https://github.com/scalaz/scalaz/tree/scalaz-seven/scalacheck-binding)
bindings to help you, but that is a topic for another day.

Note that a particular type can have several `Semigroup` or `Monoid`s that make sense.
For instance, `Int` has a `Monoid` on `(+, 0)` as well as on `(*, 1)`. Convince yourself
(using the above laws) that this makes sense.

This raises the question of how we get both `+` and `*` `Monoid`s for `Int` without
making `scalac` freak out about ambiguous implicit values. The answer to this is tagged
types, again a topic for another day.

### IRC
If you have any questions/comments/concerns, feel free to hop onto the IRC channel on
Freenode at `#scalaz`.
