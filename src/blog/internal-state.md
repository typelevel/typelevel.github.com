---
layout: post
title: Making internal state functional
category: technical

meta:
  nav: blog
  author: S11001001
  pygments: true
---

*This is the ninth of a series of articles on “Type Parameters and
Type Members”.*

Scala’s
[`CanBuildFrom` API](http://www.scala-lang.org/api/2.11.8/scala/collection/generic/CanBuildFrom.html)
is relatively well-founded and flexible;
[in combination with GADTs, it can provide that flexibility in a fully type-safe way](https://bitbucket.org/S11001001/record-map#markdown-header-using-gadts-to-find-fast-paths-safely),
if users choose not to circumvent it with typecasting.

However, it is designed in a purely mutable way; you cannot write a
useful `CanBuildFrom` that does not rely on mutation, and you cannot
use the API externally in a functional way.

Let’s design an alternative to `CanBuildFrom` that makes sense in a
purely functional context, allowing both implementers and users to
avoid unsightly mutation.

Spoiler warning! Our first pass will have one glaring inelegance. We
will use concepts from previous articles in *Type Parameters and Type
Members* to “invert the abstraction”, which will greatly simplify the
design. Once you’re comfortable with the “inversion”, you can skip the
intermediate step and use this technique directly in your own designs.

## Disallowing functional approaches

The pattern of use of `CanBuildFrom` is

1. `apply` the CBF to produce a
   [`Builder`](http://www.scala-lang.org/api/2.11.8/scala/collection/mutable/Builder.html).
2. Call `+=` and `++=` methods to “fill up” the `Builder`.
3. Call `result` to “finalize” or “commit” to the final structure.

```scala
import collection.generic.CanBuildFrom

val cbf = implicitly[CanBuildFrom[Nothing, Int, List[Int]]]
val b = cbf()
b += 3
b ++= Seq(4, 5)
b.result()

res0: List[Int] = List(3, 4, 5)
```

Let’s set aside that this is only suited to eager collections, not
lazy ones like
[`Stream`](http://www.scala-lang.org/api/2.11.8/scala/collection/immutable/Stream.html). You
can tell the problem by types: `+=` and `++=` have the return type
`this.type`. Effectively, this means that if their implementations are
purely functional, all they can do is return `this`:

```scala
  def +=(elem: Elem) = this
  def ++=(elems: TraversableOnce[Elem]) = this
```

Aside from the informal contract of `Builder`, which suggests that
calls to these methods perform a side effect, the types enforce that
they *must* perform any useful work by means of side effects.

Returning `this.type` permits these methods to be called in a
superficially functional style:

```scala
b.+=(3)
 .++=(Seq(4, 5))
 .result()

res1: List[Int] = List(3, 4, 5)
```

This retouch is only skin-deep, and can’t repair the defect making
`CanBuildFrom` unsuitable for functional programs, but it implies that
a functional alternative lurks nearby. Let’s go looking for it.

## Step 1: explicit `Builder` state

First, we need to take the essential mutation out of `Builder`. That
means it needs to provide an initial state, and the other methods must
use it as a parameter and return value.

1. We’ll add a new method to return the initial state.
2. `+=` and `++=` will take that state as an argument, returning the
   new state instead of `this.type`.
3. `result` will take the final state as an argument, still producing
   the result collection.

While the intermediate state *might* be the same as the final state,
we don’t want to require that. So `Builder` also gains a type
parameter to represent the type of state, `S`.

```scala
trait FunBuilder[S, -Elem, +To] {
  /** Produce the initial state. */
  def init: S

  // note everywhere 'S' was added
  def +=(s: S, elem: Elem): S
  def ++=(s: S, elems: TraversableOnce[Elem]): S
  def result(s: S): To
}
```

## A sample `FunBuilder`

We can incrementally build a
[`Vector`](http://www.scala-lang.org/api/2.11.8/scala/collection/immutable/Vector.html),
but it may not be the most efficient way. Instead, let’s try to
accumulate a
[`List`](http://www.scala-lang.org/api/2.11.8/scala/collection/immutable/List.html),
then construct the `Vector` once we’re done.

```scala
class VectorBuilderList[A]
    extends FunBuilder[List[A], A, Vector[A]] {

  def init = List()
  
  def +=(s: List[A], elem: A) = elem :: s
  
  def ++=(s: List[A], elems: TraversableOnce[A]) =
    elems.toList reverse_::: s
  
  def result(s: List[A]) =
    s.toVector.reverse
}

val vbl = new VectorBuilderList[Int]
vbl.result(vbl.++=(vbl.+=(vbl.init, 2), Seq(3, 4)))

res0: scala.collection.immutable.Vector[Int] = Vector(2, 3, 4)
```

(There’s a problem with `CanBuildFrom` now, but we’ll hold off fixing
it.)

## A slightly different Builder

Maybe it would be better to optimize for the `++=` “bulk add” method,
though.

```scala
class VectorBuilderListList[A]
    extends FunBuilder[List[Traversable[A]], A, Vector[A]] {
  def init = List()
  
  def +=(s: List[Traversable[A]], elem: A) =
    Traversable(elem) :: s
    
  def ++=(s: List[Traversable[A]], elems: TraversableOnce[A]) =
    elems.toTraversable :: s
    
  def result(s: List[Traversable[A]]) =
    s.foldLeft(Vector[A]()){(z, as) => as ++: z}
}

val vbll = new VectorBuilderListList[Int]
vbll.result(vbll.++=(vbll.+=(vbll.init, 2), Seq(3, 4)))

res0: scala.collection.immutable.Vector[Int] = Vector(2, 3, 4)
```

## Hide your state

The type of these builders are different, even though their usage is
the same. This design also exposes what was originally *internal*
state as part of the API. Luckily, `CanBuildFrom` makes a point of
this when we try to integrate `FunBuilder` into our own CBF version;
there’s nowhere to put the `S` type parameter.

```scala
trait FunCanBuildFrom[-From, -Elem, +To] {
  def apply(): FunBuilder[S, Elem, To]
}

…/FCBF.scala:42: not found: type S
  def apply(): FunBuilder[S, Elem, To]
                          ^
```

We can hide the state by forcing the caller to deal with the builder
in a state-generic context. One way to do this is with a generic
continuation.

```scala
trait BuilderCont[+Elem, -To, +Z] {
  def continue[S](builder: FunBuilder[S, Elem, To]): Z
}

// in FunCanBuildFrom...
  def apply[Z](cont: BuilderCont[Elem, To, Z]): Z
```

Now we can implement a `FunCanBuildFrom` that can use either of the
`FunBuilder`s we’ve defined.

```scala
class VectorCBF[A](bulkOptimized: Boolean)
    extends FunCanBuildFrom[Any, A, Vector[A]] {
  def apply[Z](cont: BuilderCont[A, Vector[A], Z]) =
    if (bulkOptimized)
      cont continue (new VectorBuilderListList)
    else
      cont continue (new VectorBuilderList)
}
```

Take a look at the type flow. The caller of `apply` is the one who
decides the `Z` type. But the `apply` implementation chooses the `S`
to pass to `continue`, which cannot know any more about what that
state type is. (It can even choose different types based on runtime
decisions.) Information hiding is restored.

```scala
val cbf = new VectorCBF[Int](true)
cbf{new BuilderCont[Int, Vector[Int], Vector[Int]] {
  def continue[S](vbl: FunBuilder[S, Int, Vector[Int]]) =
    vbl.result(vbl.++=(vbl.+=(vbl.init, 2), Seq(3, 4)))
}}

res1: Vector[Int] = Vector(2, 3, 4)
```

Now the code using the `FunBuilder` can’t fiddle with the
`FunBuilder`’s state values; it can only rewind to previously seen
states, a norm to be expected in functional programming with
persistent state values.

## Existential types are abstraction inversion

This is rather a lot of inconvenient ceremony, though. Instead of
passing a continuation that receives the `S` type as an argument along
with the `FunBuilder`, let’s just have `apply` return the type along
with the `FunBuilder`. We have a tool for returning a pair of type and
value using that type.

```scala
  def apply(): FunBuilder[_, Elem, To]
```

Remember that existential types are pairs.

Having collapsed callee-of-callee back to caller perspective, let’s
apply the rule of thumb from
[the first post in this series]({% post_url 2015-07-13-type-members-parameters %}).

> A type parameter is usually more convenient and harder to screw up, but if you intend to use it existentially in most cases, changing it to a member is probably better.

The usual case will be from the perspective of a CBF user, so the
usual use of the `S` parameter is existential. So let’s turn it into
the equivalent type member.

```scala
// rewrite the heading of FunBuilder as
trait FunBuilder[-Elem, +To] {
  type S
  
// and FunCanBuildFrom#apply as
  def apply(): FunBuilder[Elem, To]

// and the parameter S moves to a member
// for all implementations so far;
// fix until compile or see appendix
```

And we can see the information stays hidden.

```scala
scala> val cbf = new VectorCBF[Int](true)
cbf: fcbf.VectorCBF[Int] = fcbf.VectorCBF@4363e2ba

scala> val vb = cbf()
vb: fcbf.FunBuilder[Int,Vector[Int]] = fcbf.VectorBuilderListList@527c222e

scala> val with1 = vb.+=(vb.init, 2)
with1: vb.S = List(List(2))

scala> val with2 = vb.++=(with1, Seq(2, 3))
with2: vb.S = List(List(2, 3), List(2))

scala> vb.result(with2)
res0: Vector[Int] = Vector(2, 2, 3)
```

As in
[“Values never change types”]({% post_url 2015-07-30-values-never-change-types %}#naming-the-existential),
`vb.S` is abstract, existential, irreducible.

## Last minute adjustments

`Builder` had to be separate from `CanBuildFrom` because the latter
had to be stateless, with `Builder` needing to be stateful. Now that
both are stateless, the `FunBuilder` API can probably be collapsed
into `FunCanBuildFrom`.

This leaves the question, what about the mutable-state `Builder`s?
They can mutate the `S`, returning the input state from `+=` and
`++=`. You can’t use `S` values to rewind such a `FunBuilder`, but you
couldn’t before, anyway.

In the next part, “Avoiding refinement with dependent method types”,
we’ll look at the meaning of Scala’s “dependent method types” feature,
using it to replace some more type parameters with type members in
non-existential use cases.

*This article was tested with Scala 2.11.8.*

## Appendix: final `FunBuilder` examples

The rewrite from `S` type parameter to member in the `FunBuilder`
implementations is a boring, mechanical transform, but I’ve included
it here for easy reference.

```scala
class VectorBuilderList[A]
    extends FunBuilder[A, Vector[A]] {
  type S = List[A]

  def init = List()
  
  def +=(s: S, elem: A) = elem :: s
  
  def ++=(s: S, elems: TraversableOnce[A]) =
    elems.toList reverse_::: s
  
  def result(s: S) =
    s.toVector.reverse
}

class VectorBuilderListList[A]
    extends FunBuilder[A, Vector[A]] {
  type S = List[Traversable[A]]

  def init = List()
  
  def +=(s: S, elem: A) =
    Traversable(elem) :: s
    
  def ++=(s: S, elems: TraversableOnce[A]) =
    elems.toTraversable :: s
    
  def result(s: S) =
    s.foldLeft(Vector[A]()){(z, as) => as ++: z}
}

class VectorCBF[A](bulkOptimized: Boolean)
    extends FunCanBuildFrom[Any, A, Vector[A]] {
  def apply() =
    if (bulkOptimized)
      new VectorBuilderListList
    else
      new VectorBuilderList
}
```
