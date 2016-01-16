---
layout: post
title: Type members are [almost] type parameters

meta:
  nav: blog
  author: S11001001
  pygments: true
---

*This is the first of a series of articles on “Type Parameters and Type
Members”.*

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
rule of thumb: **a type parameter is usually more convenient and
harder to screw up, but if you intend to use it existentially in most
cases, changing it to a member is probably better**.

Here, and in later posts, we will discuss what on earth that means,
among other things.  In this series of articles on *Type Parameters
and Type Members*, I want to tackle a variety of Scala types that look
very different, but are really talking about the same thing, or
almost.

Two lists, all alike
--------------------

To illustrate, let’s see two versions of
[the functional list](http://www.artima.com/pins1ed/working-with-lists.html).
Typically, it isn’t used existentially, so the usual choice of
parameter over member fits our rule of thumb above.  It’s instructive
anyway, so let’s see it.

```scala
sealed abstract class PList[T]
final case class PNil[T]() extends PList[T]
final case class PCons[T](head: T, tail: PList[T]) extends PList[T]

sealed abstract class MList {self =>
  type T
  def uncons: Option[MCons {type T = self.T}]
}
sealed abstract class MNil extends MList {
  def uncons = None
}
sealed abstract class MCons extends MList {self =>
  val head: T
  val tail: MList {type T = self.T}
  def uncons = Some(self: MCons {type T = self.T})
}
```

We’re not quite done; we’re missing a way to *make* `MNil`s and
`MCons`es, which `PNil` and `PCons` have already provided
for themselves, by virtue of being `case class`es.  But it’s already
pretty clear that *a type parameter is a more straightforward way to
define this particular data type*.

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

Why all the `{type T = ...}`?
-----------------------------

After all, isn’t the virtue of type members that we don’t have to pass
the type around everywhere?

Let’s see what happens when we attempt to apply that theory.  Suppose
we remove only one of the
[*refinement*s](http://www.scala-lang.org/files/archive/spec/2.11/03-types.html#compound-types)
above, as these `{...}` rainclouds at the type level are called.
Let’s remove the one in `val tail`, so `class MCons` looks like this:

```scala
sealed abstract class MCons extends MList {self =>
  val head: T
  val tail: MList
}
```

Now let us put a couple members into the list, and add them together.

```scala
scala> val nums = MCons(2, MCons(3, MNil())): MCons{type T = Int}
nums: tmtp.MCons{type T = Int} = tmtp.MList$$anon$2@3c649f69

scala> nums.head
res1: nums.T = 2

scala> res1 + res1
res2: Int = 4

scala> nums.tail.uncons.map(_.head)
res3: Option[nums.tail.T] = Some(3)

scala> res3.map(_ - res2)
<console>:21: error: value - is not a member of nums.tail.T
       res3.map(_ - res2)
                  ^
```

When we took the refinement off of `tail`, we eliminated any evidence
about what its `type T` might be.  We only know that *it must be some
type*.  That’s what *existential* means.

**In terms of type parameters, `MList` is like `PList[_]`, and `MList
{type T = Int}` is like `PList[Int]`.**  For the former, we say that
the member, or parameter, is existential.

When is existential OK?
-----------------------

Despite the limitation implied by the error above, there *are* useful
functions that can be written on the existential version.  Here’s one
of the simplest:

```scala
def mlength(xs: MList): Int =
  xs.uncons match {
    case None => 0
    case Some(c) => 1 + mlength(c.tail)
  }
```

For the type parameter equivalent, the parameter on the argument is
usually carried out or *lifted* to the function, like so:

```scala
def plengthT[T](xs: PList[T]): Int =
  xs match {
    case PNil() => 0
    case PCons(_, t) => 1 + plengthT(t)
  }
```

By the conversion rules above, though, we should be able to write an
existential equivalent of `mlength` for `PList`, and indeed we can:

```scala
def plengthE(xs: PList[_]): Int =
  xs match {
    case PNil() => 0
    case PCons(_, t) => 1 + plengthE(t)
  }
```

There’s another simple rule we can follow when determining whether we
can rewrite in an existential manner.

1. When a type parameter appears only in one argument, and
2. appears nowhere in the result type,

we should always, ideally, be able to write the function in an
existential manner.  (We will discuss why it’s only “ideally” in
[the next article]({% post_url 2015-07-16-method-equiv %}).)

You can demonstrate this to yourself by having the parameterized
variant (e.g. `plengthT`) call the existential variant
(e.g. `plengthE`), and, voilà, it compiles, so it must be right.

This hints at what is usually, though not always, **an advantage for
type parameters: you have to ask for an existential, rather than
silently getting one just because you forgot a refinement**.  We will
discuss
[what happens when you forget one in a later post]({% post_url 2015-07-19-forget-refinement-aux %}).

Equivalence as a learning tool
------------------------------

Scala is large enough that very few understand all of it.  Moreover,
there are many aspects of it that are poorly understood in general.

So why focus on how different features are similar?  When we
understand one area of Scala well, but another one poorly, we can form
sensible ideas about the latter by drawing analogies with the former.
This is how we solve problems with computers in general: we create an
informal model in our heads, which we translate to a
mathematical statement that a program can interpret, and it gives back
a result that we can translate back to our informal model.

My guess is that type parameters are much better understood than type
members, but that existentials via type members are better understood
than existentials introduced by `_` or `forSome`, though I’d wager
that neither form of existential is particularly well understood.

By knowing about equivalences and being able to discover more, you
have a powerful tool for understanding unfamiliar aspects of Scala:
just translate the problem back to what you know and think about what
it means there, because the conclusion will still hold when you
translate it forward.  (Category theorists, eat your hearts out.)

In this vein, we will next generalize the above rule about existential
methods, discovering a simple tool for determining whether two
method types *in general* are equivalent, whereby things you know
about one easily carry over to the other.  We will also explore
methods that *cannot* be written in the existential style, at least
under Scala’s restrictions.

That all happens in
[the next part, “When are two methods alike?”]({% post_url 2015-07-16-method-equiv %}).

*This article was tested with Scala 2.11.7.*
