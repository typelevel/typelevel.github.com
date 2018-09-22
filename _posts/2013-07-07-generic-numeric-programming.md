---
layout: post
title: An Intro to Generic Numeric Programming with Spire

meta:
  nav: blog
  author: tixxit
  pygments: true
---

In this post I'd like to introduce you to what I have been calling *generic
numeric programming*.

What is Generic Numeric Programming?
------------------------------------

What do we mean by generic numeric programming? Let's take a simple example; we
want to add 2 numbers together. However, we don't want to restrict ourselves to
a particular type, like `Int` or `Double`, instead we just want to work with
some *generic* type `A` that can be added. For instance:

```scala
def add[A](x: A, y: A): A = x + y
```

Of course, this won't compile since `A` has no method `+`. What we are really
saying is that we want `A` to be some type that *behaves like a number*. The
usual OO way to achieve this is by creating an interface that defines our
desired behaviour. This is less than ideal, but if we were to go this route,
our `add` function might look like this:

```scala
trait Addable[A] { self: A =>
  def +(that: A): A
}

def add[A <: Addable[A]](x: A, y: A): A = x + y
```

We've created an interface that defines our `+` method, and then bound our type
parameter `A` to subsume this interface. The main problem with this is that we
can't directly use types out of our control, like those that come in the
standard library (ie. `Int`, `Long`, `Double`, `BigInt`, etc). The only option
would be to wrap these types, which means extra allocations and either explicit
or implicit conversions, neither of which are good options.

A better approach is to use type classes. A discussion on type classes is out
of the scope of this post, but they let us express that the type `A` must have
some desired behaviour, without inheritence. Using the type class pattern, we
could write something like this:

```scala
trait Addable[A] {
  // Both arguments must be provided. Addable works with the type A, but
  // does not extend it.
  def plus(x: A, y: A): A
}

// This class adds the + operator to any type A that is Addable,
// by delegating to that Addable's `plus` method.
implicit class AddableOps[A](lhs: A)(implicit ev: Addable[A]) {
  def +(rhs: A): A = ev.plus(lhs, rhs)
}

// We use a context bound to require that A has an Addable instance.
def add[A: Addable](x: A, y: A): A = x + y
```

We can then easily add implementations for any numeric type, regardless if we
control it or not, or even if it is a primitive type:

```scala
implicit object IntIsAddable extends Addable[Int] {
  def plus(x: Int, y: Int): Int = x + y
}

add(5, 4)
```

This is, more or less, the approach Spire takes.

Why be Generic?
---------------

Why be generic? The flippant answer I could give is: why not? I do hope that
after reading this, that is an acceptable answer to you, but I know that's not
what you came here for.

The first reason is the obvious one; sometimes you want to run the same
algorithm, but with different number types. Euclid's GCD algorithm is the same
whether you are using `Byte`, `Short`, `Int`, `Long`, or `BigInt`. Why implement
it only for 1, when you could do it for all 5? Worse; why implement it 5
times, when you need only implement it once?

Another reason is that you want to push certain trade-offs, such as speed vs
precision to the user of your library, rather than making the decision for
them. `Double` is fast, but has a fixed precision. `BigDecimal` is slow, but
can have much higher precision. Which one do you use? When in doubt, let
someone else figure it out!

A last great reason is that it let's you write less tests and can make
testing much less hairy.

### One Algorithm, Many Types

So, what does a generic version of Euclid's GCD algorithm look like? Spire
strives to make generic numeric code look more or less like what you'd write
for a direct implementation. So, let's let you compare; first up, the direct
implementation:

```scala
def euclidGcd(x: Int, y: Int): Int =
  if (y == 0) x
  else euclidGcd(y, x % y)
```

With Spire, we can use the `spire.math.Integral` type class to rewrite this as:

```scala
import spire.math.Integral
import spire.implicits._

def euclidGcd[A: Integral](x: A, y: A): A =
  if (y == 0) x
  else euclidGcd(y, x % y)
```

The 2 methods are almost identical, save the `Integral` context bound.
`Integral` gives us many methods we expect integers to have, like addition,
multiplication, and euclidean division (quotient + remainder).

Because Spire provides default implicit instances of `Integral` for all of the
integral types that come in the Scala standard library, we can immediately use
`euclidGcd` to find the GCD of many integer types:

```scala
euclidGcd(42, 96)
euclidGcd(42L, 96L)
euclidGcd(BigInt(42), BigInt(96))
```

This is much better than writing 5 different versions of the same algorithm!
With Spire, you can actually do away with `euclidGcd` altogether, as `gcd`
comes with `Integral` anyways:

```scala
spire.math.gcd(BigInt(1), BigInt(2))
```

### Performance vs Precision

Another benefit of generic numeric programming, is that you can push the choice
of numeric type off to someone else. Rather than hardcode a method or data
structure using `Double`, you can simple require some `Fractional` type.

I actually first found a need for generic numeric programming after I had
implemented a swath of algorithms with double precision floating point
arithmetic, only to find out that the minor precision errors were causing
serious correctness issues. The obvious fix was to just to use an exact type,
like `spire.math.Rational`, which would've worked for many of my purposes.
However, many of the algorithms actually worked fine with doubles or even
integers, where as others required exact n-roots (provided by a number type
like `spire.math.Real`). Being more precise meant everything got slower, even
when it didn't need to be. Being less precise meant some algorithms would
occasionally return wrong answers. Abstracting out the actual number type
used meant I didn't have to worry about these issues. I could make the choice
later, when I knew a bit more about my data, performance and precision
requirements.

We can illustrate this using a simple algorithm to compute the mean of some
numbers.

```scala
import spire.math._
import spire.implicits._

// Note: It is generally better to use an incremental mean.
def mean[A: Fractional](xs: A*): A = xs.reduceLeft(_ + _) / xs.size
```

Here, we don't care what type `A` is, as long as it can be summed and divided. 
If we're working with approximate measurements, perhaps finding the mean of a
list of `Double`s is good enough:

```scala
mean(0.5, 1.5, 0.0) 
// = 0.6666666666666666
```

Or perhaps we'd like an exact answer back instead:

```scala
import spire.math.Rational

mean(Rational(1, 2), Rational(3, 2), Rational(0))
// = Rational(2, 3)
```

The main thing here is that as a user of the `mean` function, I get to choose
whether I'd prefer the speed of `Double` or the precision or `Rational`. The
algorithm itself looks no different, so why not give the user the choice?

### Better Testing

One of the best things is that if you write test code that abstracts over the
number type, then you can re-use your tests for many different types. Spire
makes great use of this, to ensure instances of our type classes obey the rules
of algebra and that the number types in Spire (Rational, Complex, UInt, etc)
are fundamentally correct.

There is another benefit though -- you can ignore the subtleties of floating
point arithmetic in your tests if you want! If your code works with any number
type, then you can test with an exact type such as `spire.math.Rational` or
`spire.math.Real`. No more epsilons and NaNs. You shouldn't let this excuse you
from writing numerically stable code, but it may save you many false negatives
in your build system, while also making you more confident that the fundamentals
are correct.

This is a big topic, deserving of its own blog post (you know who you are), so
I'll leave this here.

What Abstractions Exist?
------------------------

We've already seen `Integral`, which can be used wherever you need something
that acts like an integer. We also saw the modulus operator, `x % y`, but not
integer division. Spire differentiates between *integer division* and *exact
division*. You perform integer division with `x /~ y`. To see it in action,
let's use an overly complicated function to negate an integer:

```scala
import spire.math._
import spire.implicits._

def negate[A: Integral](x: A) = -(42 * (x /~ 42) + x % 42)
```

Instances of `Integral` exist for `Byte`, `Short`, `Int`, `Long` and `BigInt`.

Another type class Spire provides is `Fractional[A]`, which is used for things
that have "exact" division. "Exact" is in quotes, since `Double` or
`BigDecimal` division isn't really exact, but it's close enough that we give 
them a pass. `Fractional` also provides `x.sqrt` and `x nroot k` for taking the
roots of a number.

```scala
def distance[A: Fractional](x: A, y: A): A = (x ** 2 + y ** 2).sqrt
```

Note that `Fractional[A] <: Integral[A]`, so anything you can do with
`Integral`, you can do with `Fractional[A]` too. Here, we can use `distance`
to calculate the length of the hypotenuse with `Double`s, `Float`s,
`BigDecimal`s, or some of Spire's number types like `Real` or `Rational`.

Lastly, you often have cases where you just don't care if `/` means exact or
integer division, or whether you are taking the square root of an `Int` or a
`Double`. For this kind of catch-all work Spire provides `Numeric[A]`.

Why Spire?
----------

If you've already hit the types of problems solved by generic numeric
programming, then you may have seen that `scala.math` also provides `Numeric`,
`Integral`, and `Fractional`, so why use Spire? Well, we originally created
Spire largely due to the problems with the type classes as they exist in Scala.

To start, Scala's versions aren't specialized, so they only worked with boxed
versions of primitive types. The operators in Scala also required boxing, which
means you need to trade-off performance for readability. They also aren't very
useful for a lot of numeric programming; what about nroots, trig functions,
unsigned types, etc?

Spire also provides many more useful (and specialized) type classes. Some are
ones you'd expect, like `Eq` and `Order`, while others define more basic
algebras than `Numeric` and friends, like `Ring`, `Semigroup`, `VectorSpace`,
etc.

There are many useful number types that are missing from Scala in Spire, such
as `Rational`, `Complex`, `UInt`, etc.

Spire was written by people who actually use it. I somewhat feel like Scala's
Numeric and friends weren't really used much after they were created, other
than for Scala's NumericRange support (ie. `1.2 to 2.4 by 0.1`). They miss
many little creature comforts whose need becomes apparent after using Scala's
type classes for a bit.

### Spire is Fast

One of Spire's goals is that the performance of generic code shouldn't suffer.
Ideally, the generic code should be as fast as the direct implementation. Using
the GCD implementation above as an example, we can compare Spire vs. Scala vs.
a direct implementation. I've put the
[benchmarking code up as a Gist](https://gist.github.com/tixxit/5695365).

    gcdDirect:        29.981   1.00 x gcdDirect
    gcdSpire:         30.094   1.00 x gcdDirect
    gcdSpireNonSpec:  36.903   1.23 x gcdDirect
    gcdScala:         38.989   1.30 x gcdDirect

For another example, we can look at the code to
[find the mean of an array](https://gist.github.com/tixxit/5695365).

    meanDirect:        10.592   **1.00 x gcdDirect**
    meanSpire:         10.638   **1.00 x gcdDirect**
    meanSpireNonSpec:  13.434   **1.26 x gcdDirect** 
    meanScala:         19.388   **1.83 x gcdDirect**

Spire achieves these goals fairly simply. All our type classes are
`@specialized`, so when using primitives types you can avoid boxing. We then use macros to remove
the boxing normally required for the operators by the implicit conversions.

Using `@specialized`, both `gcdSpire` and `meanSpire` aren't noticably slower
than the direct implementation. We can see the slow down caused by dropping
`@specialized` in `gcdSpireNonSpec` and `meanSpireNonSpec`. The difference
between `gcdSpireNonSpec` and `gcdScala` is because Spire doesn't allocate an
object for the `%` operator (using macros to remove the allocation). The
difference is even more pronounced between `meanSpireNonSpec` and `meanScala`.

### More than just `Numeric`, `Integral`, and `Fractional`

The 3 type classes highlighted in this post are just the tip of the iceberg.
Spire provides a whole slew of type classes in `spire.algebra`. This package
contains type classes representing a wide variety of algebraic structures,
such as `Monoid`, `Group`, `Ring`, `Field`, `VectorSpace`, and more. The 3 type
classes discussed above provide a good starting point, but if you use Spire in
your project, you will probably find yourself using `spire.algebra` more and
more often. If you'd like to learn more, you can [watch my talk on abstract
algebra in Scala](http://www.youtube.com/watch?v=xO9AoZNSOH4).

As an example of using the algebra package, `spire.math.Integral` is simply
defined as:

```scala
import spire.algebra.{ EuclideanRing, IsReal }

trait Integral[A] extends EuclideanRing[A]
                     with IsReal[A] // Includes Order[A] with Signed[A].
                     with ConvertableFrom[A]
                     with ConvertableTo[A]
```

Whereas `spire.math.Fractional` is just:

```scala
import spire.algebra.{ Field, NRoot }

trait Fractional[A] extends Integral[A] with Field[A] with NRoot[A]
```

### Many New Number Types

Spire also adds many new useful number types. Here's an incomplete list:

- **spire.math.Rational** is a fast, exact number type for working with
  rational numbers,
- **spire.math.Complex[A]** is a parametric number type for complex numbers,
- **spire.math.Number** is a boxed number type that strives for flexibility
  of use,
- **spire.math.Interval** is a number type for interval arithmetic,
- **spire.math.Real** is a number type for exact geometric computation that
  provides exact n-roots, as well as exact division,
- **spire.math.{UByte,UShort,UInt,ULong}** unsigned integer types, and
- **spire.math.Natural** an arbitrary precision unsigned integer type.

### Better Readability

Spire also provides better operator integration with `Int` and `Double`. For
instance, `2 * x` or `x * 2` will just work for any `x` whose type has an
`Ring`. On the other hand, Scala requires something like
`implicitly[Numeric[A]].fromInt(2) * x` which is much less readable. This
also goes for working with fractions; `x * 0.5` will just work, if `x` has a
`Field`.

Try It Out!
-----------

Spire has a basic algebra that let's us work generically with numeric types. It
does this without sacrificing readability or performance. It also provides many
more useful abstractions and concrete number types. This means you can write
less code, write less tests, and worry less about concerns like performance vs.
precision. If this appeals to you, then you should try it out!

There is some basic information on getting up-and-running with Spire in SBT on
[Spire's project page](https://github.com/non/spire). If you have any further
questions, comments, suggestions, criticism or witticisms you can say what you
want to say on the [Spire mailing list](https://groups.google.com/forum/#!forum/spire-math)
or on IRC on Freenode in `#spire-math`.
