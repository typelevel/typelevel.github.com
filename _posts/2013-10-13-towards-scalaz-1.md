---
layout: post
title: Towards Scalaz (Part 1)

meta:
  nav: blog
  author: adelbertc
  pygments: true
  mathjax: true
---

A lot of people see Scalaz as a hard fringe, ivory tower,
not suited for real-world applications library, which is
unfortunate. The goal of this blog post series is to introduce
various components of Scalaz, and hopefully through this
allow folks to gain an understanding towards the power of
Scalaz.

As a prerequisite, I assume knowledge of type classes as they
are implemented and used in Scala.

## Part 1: Learning to Add

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

### Problem
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

### Making it generic
So what do we want? We want a type class that only requires instances
to be able to "add" two `A`s to get another `A`.

```scala
trait Addable[A] {
  def plus(x: A, y: A): A
}
```

And let's define an instance of `Addable` for all `Numeric` types and `String`.

```scala
object Addable {
  implicit def numericIsAddable[A](implicit A: Numeric[A]): Addable[A] =
    new Addable[A] {
      def plus(x: A, y: A): A = A.plus(x, y)
    }

  implicit val stringIsAddable: Addable[String] =
    new Addable[String] {
      def plus(x: String, y: String): String = x + y
    }
}
```

And here's our shiny new generic summer function!

```scala
def sumGeneric[A](l: List[A])(implicit A: Addable[A]): A =
  l.reduce(A.plus)
```

And now this works for `Int`, `Double`, `String`, and many more.

A good exercise at this point is to define an `Addable` instance for `List[A]`.

### Making an Exception
What happens when we pass in an empty `List` to our summer function though?
We get an exception! How do we prevent this? A common answer I get is
"Oh I know it won't happen" – this is not ideal, we want to guarantee safety
as much as possible without having to rely on human judgement.

How then do we write a safer summer function? Lets turn to an alternative
way of implementing sum on `List[Int]`.

```scala
// Old, bad version
def sum(l: List[Int]): Int = l.reduce(_ + _)

// Shiny, new version
def sum(l: List[Int]): Int = l.foldLeft(0)(_ + _)
```

What happens now when we pass an empty `List` into the sum function? We get 0,
not an exception! Note that before all we gave the program was a binary
operation (what `Addable` defines), where now we give a binary option *and* a
"zero" or starting value (the 0). As it stands, we cannot write this with
`Addable` since it has no "zero".

It may be tempting to just add a `zero` method to `Addable`, but then we may run
into the same issues we had with `Numeric` later on – we don't *always* need
a "zero", sometimes a binary operation is good enough. So instead, let's create
an `AddableWithZero` type class.

```scala
trait AddableWithZero[A] extends Addable[A] {
  def zero: A
}
```

Note that while you dont see the `plus` method in here, the fact
it `extends Addable` without implementing the `plus` method propagates the need to
implement that method, so programmers who want to create an `AddableWithZero[A]` instance
need to implement both.

Programmers can now write functions that depend only on `Addable`, or perhaps if they
need a bit more power use `AddableWithZero`. Types that have `AddableWithZero` instances
also have `Addable` instances automatically due to subtyping.

Lets move our `Addable` instances to the `AddableWithZero` object.

```scala
object AddableWithZero {
  implicit def numericIsAddableZero[A](implicit A: Numeric[A]): AddableWithZero[A] =
    new AddableWithZero[A] {
      def plus(x: A, y: A): A = A.plus(x, y)
      def zero: A = A.zero
    }

  implicit val stringIsAddableZero: AddableWithZero[String] =
    new AddableWithZero[String] {
      def plus(x: String, y: String): String = x + y
      def zero: String = ""
    }
}
```

And finally, our shiny new generic sum function!

```scala
def sumGeneric[A](l: List[A])(implicit A: AddableWithZero[A]): A =
  l.foldLeft(A.zero)(A.plus)
```

Hurrah!

### Plot Twist
It turns out that our `Addable` and `AddableWithZero` type classes is not just us being
sly and clever, but an actual thing! They are called `Semigroup` and
`Monoid` (respectively), taken from the wonderful field of abstract algebra. Abstract
algebra is a field dedicated to studying algebraic structures as opposed
to just numbers as we may be used to. The field looks into what properties
and operations various structures have in common, such as integers and
matrices. For instance, we can add two integers, as well as two matrices of the same size.
This is analogous to how we noticed the `plus` worked on not only `Numeric`
but `String` and `List[A]` as well! This is the kind of generecity we're looking for.

Here's what `sumGeneric` looks like in Scalaz land.

```scala
import scalaz.Monoid

def sumGeneric[A](l: List[A])(implicit A: Monoid[A]): A =
  l.foldLeft(A.zero)((x, y) => A.append(x, y))
```

Thankfully we dont have to create our own versions of `Semigroup` and `Monoid` –
Scalaz has one for us! In fact, the developers of Scalaz have been kind enough to define
several `Monoid` instances for common types such as `Numeric`, `String`, `List[A]`, etc.
There are also instances for tuples – if we have a tuple, say of type `(A, B, C)`,
and all three types have `Monoid` instances themselves, then the whole tuple has an
instance where the `zero` is the tuple `(A.zero, B.zero, C.zero)` and the `plus` is
appending corresponding pairs between the two tuples. Look for instances that may already
be defined before defining your own on existing types.

<div class="side-note">
  If you are interested in learning more about numeric programming, check out
  the <a href="https://github.com/non/spire">spire</a> library, as well as the
  accompanying post about <a href="{% post_url 2013-07-07-generic-numeric-programming %}">
  generic numeric programming</a>.
</div>


### Law-Abiding Citizen
To close this post off, I confess one thing: defining a `Monoid` (and `Semigroup`) instance
should not be done without some thought. It is not enough that you simply have a zero and
a binary operation – to truly have a `Monoid` or `Semigroup` certain laws must be obeyed.
These laws are as follows:

Call the `plus` operation $+$ and the `zero` value $0$. Arbitrary values of type `A` will be
referred to as $a$, $b$, etc.

The `Semigroup` law requires $+$ to be associative. That is:

<div style="text-align:center;">
	$(a + b) + c = a + (b + c)$
</div/>

In addition to the `Semigroup` law for the binary operation, the `Monoid` law relates
$+$ and $0$:

<div style="text-align:center;">
  $(a + 0) = (0 + a) = a$
</div>

To check these laws, Scalaz provides [ScalaCheck](https://github.com/scalaz/scalaz/tree/v7.0.4/scalacheck-binding)
bindings to help you, but that is a topic for another day.

Note that a particular type can have several `Semigroup` or `Monoid`s that make sense.
For instance, `Int` has a `Monoid` on $(+, 0)$ as well as on $(*, 1)$. Convince yourself
(using the above laws) that this makes sense.

This raises the question of how we get both $+$ and $*$ `Monoid`s for `Int` without
making `scalac` freak out about ambiguous implicit values. The answer is "tagged types",
again a topic for another day.

## Getting Help

If you have any questions/comments/concerns, feel free to hop onto the IRC channel on
Freenode at `#scalaz`.
