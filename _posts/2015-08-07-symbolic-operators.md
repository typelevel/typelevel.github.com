---
layout: post
title: Symbolic operators and type classes for Cats

meta:
  nav: blog
  author: non
  pygments: true
  mathjax: true
---

This post is an introduction into how operators are implemented in Cats and has been originally published in [August 2015](https://gist.github.com/non/3abdb35a72c39276d3d9).
Some more details can be found in the [previous post]({% post_url 2015-08-06-machinist %}).

One of the simplest and most recognizable type classes is the semigroup.
This type class abstracts over the ability to combine values of a certain
type in an associative manner.

<div class="side-note">
  What does <em>associativity</em> mean?
  We call an operation $\oplus$ associative, if for all $a$, $b$ and $c$, $a \oplus (b \oplus c) = (a \oplus b) \oplus c$ holds.
  Read more about this in the <a href="https://github.com/non/algebra#algebraic-properties-and-terminology">README of the algebra repository</a>.
</div>

Cats provides `cats.Semigroup[A]` to model semigroups.
The `combine` method takes two values of the type `A` and returns an `A` value.

In addition, Cats defines syntax allowing the binary operator `|+|` to be
used in place of the `combine` method.

Small example
-------------

Here is a small method that provides a generic way to combine the elements
of a list:

```scala
import cats.Semigroup
import cats.implicits._

def gsum[A: Semigroup](values: List[A]): Option[A] =
  if (values.isEmpty) None else Some(values.reduceLeft((x, y) => x |+| y))
```

(A similar method is built into Cats as `Semigroup.combineAllOption`.)

How does it work?
-----------------

One of the parts of `gsum` that might be hard to understand is where
the `|+|` method comes from. Since `x` and `y` are totally generic values
(of type `A`) how can we call a method on them?

To boil the example down further, consider this simpler example:

```scala
import cats.implicits._
19 |+| 20 // produces 39
```

How does this work? We know that the `Int` type does not have a `|+|` method.
Experienced Scala developers will suspect that implicits play a role here,
but what are the details?

In detail
---------

Let's walk through how the expression `19 |+| 20` is compiled.

First, a `|+|` method is needed on `Int`. Since
[`Int`](https://github.com/scala/scala/blob/v2.11.7/src/library/scala/Int.scala)
does not provide one, the compiler searches for an implicit conversion to a
type that *does* have a `|+|` method.

Due to our import, it will find the
[`semigroupSyntax[A]`](https://github.com/non/cats/blob/82dbf4076572dfbb6e29dd49875f5e5d929f80be/core/src/main/scala/cats/syntax/semigroup.scala#L8)
method, which returns a type that has a `|+|` method (specifically
[`SemigroupOps[A]`](https://github.com/non/cats/blob/82dbf4076572dfbb6e29dd49875f5e5d929f80be/core/src/main/scala/cats/syntax/semigroup.scala#L12)).
However, `semigroupSyntax` requires an implicit `Semigroup[A]` value to be in scope.
Do we have a `Semigroup[Int]` in scope?

Yes we do. Our import also provides an implicit value [`intGroup`](https://github.com/non/cats/blob/82dbf4076572dfbb6e29dd49875f5e5d929f80be/std/src/main/scala/cats/std/anyval.scala#L23)
of type `AdditiveCommutativeGroup[Int]`. Leaving aside what *additive*, *commutative*,
and *group* mean here, this is a subtype of `Semigroup[Int]`, so it matches.

Let's continue with our current example. At this point we have gone from:

```scala
19 |+| 20 // produces 39
```

to:

```scala
semigroupSyntax[Int](19)(intGroup) |+| 20
```

But we aren't out of the woods yet! We still need to see how this expression
is evaluated.

Of macros and machinists
------------------------

Looking at how the `|+|` method is
[implemented](https://github.com/non/cats/blob/82dbf4076572dfbb6e29dd49875f5e5d929f80be/core/src/main/scala/cats/syntax/semigroup.scala#L13)
reveals the cryptic `macro Ops.binop[A, A]`. What is this?

Following the rabbit hole farther, we come to
[`cats.macros.Ops`](https://github.com/non/cats/blob/82dbf4076572dfbb6e29dd49875f5e5d929f80be/macros/src/main/scala/cats/macros/Ops.scala#L6)
which provides the macro implementation that `|+|` is using. Aside from a
[suggestively named](https://github.com/non/cats/blob/82dbf4076572dfbb6e29dd49875f5e5d929f80be/macros/src/main/scala/cats/macros/Ops.scala#L18)
item in the `operatorNames` map, we don't have any clues what is going on.

The [machinist](https://github.com/typelevel/machinist) project was created
to optimize exactly this kind of implicit syntax problem. What will happen here
is that `operatorNames` describes how to rewrite expressions using type
classes. Long-story short, we will transform:

```scala
semigroupSyntax[Int](19)(intGroup) |+| 20
```

into:

```scala
intGroup.combine(19, 20)
```

The aforementioned suggestive map item tells us that we should rewrite the `|+|` operator
to method calls on the given type class (i.e. `intGroup`) using `.combine`.

Finishing up
------------

Just to confirm that we're done, let's look at what `intGroup.combine` will do.
We started with a call to `AdditiveCommutativeGroup[Int]`, which will find
[`intAlgebra`](https://github.com/non/algebra/blob/v0.3.1/std/shared/src/main/scala/algebra/std/int.scala#L12).
Then we call the [`.additive`](https://github.com/non/algebra/blob/v0.3.1/core/src/main/scala/algebra/ring/Additive.scala#L93)
method on it to produce a `CommutativeGroup[Int]`.

So putting that together, we can see that calling `intGroup.combine(19, 20)`
will call `intAlgebra.plus(19, 20)`, and that this is defined as `19 + 20`,
as we would expect.

Whew!

Conclusion
----------

This is a lot of machinery. The incredibly terse and expressive syntax it
enables is quite nice, but you can see that even leaving out one import
will cause the whole edifice to come tumbling down.

The easiest way to use Cats is to just import `cats.implicits._`. That
way, you can be sure that you have all of it. There are individual imports
from `cats.syntax` and `cats.std` which can be used to pinpoint the exact
values and method you want to put into scope, but getting these right
can be a bit tricky, especially for newcomers.

Some more examples of Machinist can be found in the [README](https://github.com/typelevel/machinist/blob/v0.4.1/README.md#examples).

Errata
------

You may also decide that the syntax convenience is not worth it. To write our
original example without syntax implicits (but still using implicit values)
you could say:

```scala
import cats.Semigroup
import cats.implicits._

def gsum[A](values: List[A])(implicit s: Semigroup[A]): Option[A] =
  if (values.isEmpty) None else Some(values.reduceLeft((x, y) => s.combine(x, y)))

// values.reduceLeft(s.combine) would also work
```

Whether to use syntax implicits or explicit method calls is mostly a matter
of preference. Personally, I like using syntax explicits to help make generic
code read in a clearer manner, but as always, your mileage may vary.
