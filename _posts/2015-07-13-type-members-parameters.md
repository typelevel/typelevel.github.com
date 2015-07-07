---
layout: post
title: Type members are [almost] type parameters

meta:
  nav: blog
  author: S11001001
  pygments: true
---

Type members are [almost] type parameters
=========================================

Type members like `Member`

```scala
class Blah {
  type Member
}
```

and parameters like `Param`

```scala
class Blah2[Param]
```

have more similarities than differences.  The choice of which to use
for a given situation is usually a matter of convenience.  In brief, a
rule of thumb: **if you intend to use a parameter existentially in
most cases, changing it to a member is probably better; a parameter is
more convenient and harder to screw up in most circumstances**.

We will discuss what on earth that means, among other things.  More
broadly, though, in this series of articles on *Type Parameters and
Type Members*, I want to tackle a variety of Scala types that look
very different, but are really talking about the same thing, or
almost.

Two lists, all alike
--------------------

To illustrate, let's see two versions of the functional list.  It's
typically not used existentially, so the usual choice of parameter
over member fits our rule of thumb above.  It's instructive anyway, so
let's see it.

```scala
sealed abstract class PList[T]
final case class PNil[T]() extends Plist[T]
final case class PCons[T](head: T, tail: PList[T])

sealed abstract class MList {self =>
  type T
  def uncons: Option[MCons {type T = self.T}]
}
sealed abstract class MNil extends MList
sealed abstract class MCons extends MList {self =>
  val head: T
  val tail: MList {type T = self.T}
}
```

We're not quite done; we're missing a way to *make* `MNil`s and
`MCons`es, which `PNil` and `PCons` have already provided for
themselves, by virtue of being `case class`es.  But it's already
pretty clear that a type parameter is a more straightforward way to
define this data type.

The instance creation takes just a bit more scaffolding for our
examples:

```scala
def MNil[T0](): MNil {type T = T0} =
  new MNil {
    type T = T0
  }

def MCons[T0](hd: T0, tl: MList {type T = T0})
  : MCons {type T = T0} =
  new MCons {
    type T = T0
    val head = hd
    val tail = tl
  }
```

Why all the {type T = ...}?
---------------------------

After all, isn't the virtue of type members that we don't have to pass
the type around everywhere?

Let's see what happens with that theory.  Suppose we remove only one
of the refinements above, the one in `val tail`, so `class MCons`
looks like this:

```scala
sealed abstract class MCons extends MList {self =>
  val head: T
  val tail: MList
}
```

Now let us put a couple members into the list, and add them together.

```scala
> val nums = MCons(2, MCons(3, MNil()))
TODO

> nums.head
TODO

> nums.tail.uncons.map(_.head)
TODO ERROR
```

When we took the refinement off of `tail`, we eliminated any evidence
about what its `type T` might be.  We only know that it must be *some
type*.

**In terms of type parameters, `MList` is like `PList[_]`, and `MList
{type T = Int}` is like `PList[Int]`.** For the former, we say that
the member, or parameter, is *existential*.

When is existential OK?
-----------------------

Despite this limitation, there are functions that can be written on
the existential version.  Here's the simplest:

```scala
def mlength(xs: MList): Int =
  xs.uncons match {
    case None => 0
    case Some(c) => 1 + mlength(c.tail)
  }
```

For the type parameter equivalent, the parameter on the argument is
usually carried out or *lifted* to the function, like so:

```scala
def plength[T](xs: PList[T]): Int =
  xs match {
    case PNil() => 0
    case PCons(_, t) => 1 + plength(t)
  }
```

By the conversion rules above, though, we should be able to write an
existential equivalent of `mlength`, and indeed we can:

```scala
def plength2(xs: PList[_]): Int =
  xs match {
    case PNil() => 0
    case PCons(_, t) => 1 + plength2(t)
  }
```

There's another simple rule we can follow when determining whether we
can rewrite in an existential manner.

1. When a type parameter appears only in one argument, and
2. appears nowhere in the result type,

we should always, ideally, be able to write the function in an
existential manner.

You can demonstrate this to yourself by having the parameterized
variant (e.g. `plength`) call the existential variant, and, voilà, it
compiles, so it must be right.

This hints at what is usually, though not always, an advantage for
type parameters: you have to ask for an existential, rather than
silently getting one just because you forgot a refinement.  The
mistake of `mdropFirst2`'s signature, to fail to relate the output
type to the input type strongly enough, is an easy one to make when
it's the default behavior.

There is another mistake that type members open you up to. I have been
using the very odd type parameter -- and member -- name `T`.  Java
developers will find this choice very ordinary, but the name of choice
in Scala is `A`.  So suppose I attempted to correct `mdropFirst2`'s
type as follows:

```scala
def mdropFirst3[T0](xs: MList {type A = T0}) =
  TODO copy definition
```

This method compiles, but I cannot invoke it!

```scala
> mdropFirst3(MNil[Int]())
TODO error

> mdropFirst3(MCons(42, MNil()))
TODO error
```

That's because `MList {type A = T0}` is a perfectly reasonable
intersection type: values of this type have *both* the type `MList` in
their supertype tree somewhere, *and* a type member named `A`, which
is bound to `T0`.  That `MList` has no such type member is irrelevant
to the intersection and refinement of types in Scala.
TODO implications of that

Equivalence as a learning tool
------------------------------

Scala is large enough that very few understand all of it.  Moreover,
there are many aspects of it that are poorly understood in general.

So why focus on how different features are similar?  When we
understand one area of Scala well, but another one poorly, we can form
sensible ideas about the latter by drawing analogies with the former.
This is how we solve problems with computers in general: we have an
informal model in our heads, which we translate to a mathematical
statement that a program can interpret, and it gives back a result
that we can translate back to our informal model.

My guess is that type parameters are much better understood than type
members, but that existentials via type members are better understood
than existentials introduced by `_` or `forSome`.  By knowing about
equivalences and being able to discover more, you have a powerful tool
for understanding unfamiliar aspects of Scala: just translate the
problem back to what you know, and think about what it means there,
because the conclusion will still hold when you translate it back.

In this vein, we will next generalize the above rule about existential
methods, discovering a simple tool for determining whether two method
types *in general* are equivalent, and so that things you know about
one easily carry over to the other.
