---
layout: post
title: What happens when I forget a refinement?

meta:
  nav: blog
  author: S11001001
  pygments: true
---

*This is the third of a series of articles on “Type Parameters and
Type Members”.  If you haven’t yet, you should
[start at the beginning]({% post_url 2015-07-13-type-members-parameters %}),
which introduces code we refer to throughout this article without
further ado.*

As I mentioned
[in the previous article]({% post_url 2015-07-16-method-equiv %}#when-are-two-methods-less-alike),
the error of the `mdropFirstE` signature, taking `MList` and returning
merely `MList`, was to fail to relate the input element type to the
output element type.  This mistake is an easy one to make when failure
is the default behavior.

By contrast, **when we try this with `PList`, the compiler helpfully
points out our error**.

```scala
def pdropFirst(xs: PList): PList = ???

TmTp3.scala:6: class PList takes type parameters
  def pdropFirst(xs: PList): PList = ???
                             ^
TmTp3.scala:6: class PList takes type parameters
  def pdropFirst(xs: PList): PList = ???
                     ^
```

What happens when I misspell a refinement?
------------------------------------------

There is another mistake that type members open you up to. I have been
using the very odd type parameter—and member—name `T`.
Java developers will find this choice very ordinary, but the name of
choice for the discerning Scala programmer is `A`.  So suppose I
attempted to correct `mdropFirstE`’s type as follows:

```scala
def mdropFirstE2[T0](xs: MList {type A = T0}) =
  xs.uncons match {
    case None => MNil()
    case Some(c) => c.tail
  }
```

This method compiles, but I cannot invoke it!

```scala
> mdropFirstE2(MNil[Int]())
<console>:20: error: type mismatch;
 found   : tmtp.MNil{type T = Int}
 required: tmtp.MList{type A = ?}
       mdropFirstE2(MNil[Int]())
                             ^

> mdropFirstE2(MCons[Int](42, MNil[Int]()))
<console>:20: error: type mismatch;
 found   : tmtp.MCons{type T = Int}
 required: tmtp.MList{type A = ?}
       mdropFirstE2(MCons[Int](42, MNil[Int]()))
                              ^
```

That’s because `MList {type A = T0}` is a perfectly reasonable
intersection type: values of this type have *both* the type `MList` in
their supertype tree somewhere, *and* a type member named `A`, which
is bound to `T0`.  In terms of subtyping relationships:

```scala
MList {type A = T0} <: MList
// and unrelatedly,
MList {type A = T0} <: {type A = T0}
```

That `MList` has no such type member `A` is irrelevant to the
intersection and refinement of types in Scala.  This type means “an
instance of the trait `MList`, with a type member named `A` set
to `T0`”.  This type member `A` could come from another trait mixed
with `MList` or an inline subclass.  Whether such a thing is
impossible to instantiate—due to `sealed`, `final`, or anything
else—is also irrelevant; **types with no values are meaningful and
useful in both Java and Scala**.

Why `T0`?  What’s `Aux`?
------------------------

A few of the methods on `MList` we have seen so far take a type
parameter `T0` instead of `T`.  This is just a mnemonic trick; I’m
saying “I would write `T` here if `scalac` would let me”, which I have
borrowed from
[`scalaz.Unapply`](https://github.com/scalaz/scalaz/blob/v7.1.3/core/src/main/scala/scalaz/Unapply.scala#L217).
Let’s try to implement `def MNil` taking a `T` instead.

```scala
def MNil[T](): MNil {type T = T} =
  new MNil {
    type T = T
  }

// Scala complains, though:
TmTp3.scala:15: illegal cyclic reference involving type T
  def MNil[T](): MNil {type T = T} =
                                ^
```

This is a scoping problem; the refinement type makes the member `T`
shadow our method type parameter `T`.  We dealt with the problem in
`MList#uncons` and `MCons#tail` as well, way back in section “Two
lists, all alike” of
[the first part]({% post_url 2015-07-13-type-members-parameters %}#two-lists-all-alike),
in those cases by outer-scoping the `T` as
`self.T` instead.

**When defining a type with members, you should define an `Aux` type
in your companion that converts the member to a type parameter.** The
name `Aux` is a convention I have borrowed from
[Shapeless ops](https://github.com/milessabin/shapeless/blob/shapeless-2.2.4/core/src/main/scala/shapeless/ops/hlists.scala#L1501).
This is pretty much boilerplate; in this case, add this to
`object MList`:

```scala
type Aux[T0] = MList {type T = T0}
```

Now you can write `MList.Aux[Int]` instead of `MList {type T = Int}`.
Here’s `mdropFirstT`’s signature rewritten in this style.

```scala
def mdropFirstT2[T](xs: MList.Aux[T]): MList.Aux[T] = ???
```

Furthermore, because the member `T` is not in scope for `Aux`’s type
parameter position, you can take method type parameters named `T` and
sensibly write `MList.Aux[T]` without the above error.  You can see
this in the immediately preceding example.  But, stepping back a bit,
this should be considered an advantage for type parameters more
generally; `PList` doesn’t have this problem in the first place.

**Using `Aux` also helps you avoid the errors of forgetting to specify
or misspelling a type member**, as described at the beginning of this
article.  With `Aux`, as with ordinary parameterized types, a missing
argument is caught by the compiler, and misspelling the parameter name
is impossible.

In
[the next part, “Type projection isn’t that specific”]({% post_url 2015-07-23-type-projection %}),
we’ll see why something that, at first glance, seems
like a workable alternative to either refinement or the `Aux` trick,
doesn’t work out as well as people wish it would.

*This article was tested with Scala 2.11.7.*
