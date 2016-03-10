---
layout: post
title: Information hiding, enforced

meta:
  nav: blog
  author: adelbertc
  pygments: true
---

Code should be reusable. An expression traversing a data structure
shouldn't be written multiple times, it should be pulled out into a
generic traversal function. At a larger scale, a random number generator
shouldn't be written multiple times, but rather pulled out into a
module that can be used by others.

It is important that such abstractions must be done carefully.
Often times a type is visible to the caller, and if the type
is not handled carefully the abstraction can leak.

For example, a set with fast random indexing (useful for random
walks on a graph) can be implemented with a sorted `Vector`.
However, if the `Vector` type is
leaked, the user can use this knowledge to violate the invariant.

```scala
import scala.annotation.tailrec

def binarySearch(i: Int, repr: Vector[Int]): (Boolean, Int) = {
  @tailrec
  def binarySearchAux(minIndex: Int, maxIndex: Int): (Boolean, Int) = {
    val midIndex = minIndex + ((maxIndex - minIndex) / 2)

    if (maxIndex < minIndex) (false, midIndex)
    else {
      if (repr(midIndex) > i) binarySearchAux(minIndex, midIndex - 1)
      else if (repr(midIndex) < i) binarySearchAux(midIndex + 1, maxIndex)
      else (true, midIndex)
    }
  }
  binarySearchAux(0, repr.size - 1)
}

object IntSet {
  type Repr = Vector[Int]

  def empty: Repr = Vector.empty

  def add(i: Int, repr: Repr): Repr = {
    val (isMember, indexOf) = binarySearch(i, repr)
    if (isMember) repr
    else {
      val (prefix, suffix) = repr.splitAt(indexOf)
      prefix ++ Vector(i) ++ suffix
    }
  }

  def contains(i: Int, repr: Repr): Boolean =
    binarySearch(i, repr)._1
}
```

```scala
import IntSet._
// import IntSet._

val good = add(1, add(10, add(5, empty)))
// good: IntSet.Repr = Vector(1, 5, 10)

val goodResult = contains(10, good)
// goodResult: Boolean = true

val bad = good.reverse // We know it's a Vector!
// bad: scala.collection.immutable.Vector[Int] = Vector(10, 5, 1)

val badResult = contains(10, bad)
// badResult: Boolean = false

val bad2 = Vector(10, 5, 1) // Alternatively..
// bad2: scala.collection.immutable.Vector[Int] = Vector(10, 5, 1)

val badResult2 = contains(10, bad2)
// badResult2: Boolean = false
```

The issue here is the user knows more about the representation than they
should. The function `add` enforces the sorted invariant on each insert,
and the function `contains` leverages this to do an efficient look-up.
Because the `Vector` definition of `Repr` is exposed, the user is
free to create any `Vector` they wish which may violate the invariant,
thus breaking `contains`.

In general, the **name** of the representation type is needed but the
**definition** is not. If the definition is hidden, the user is only able to
work with the type to the extent the module allows. This is precisely
the notion of information hiding. If this can be enforced by the type
system, modules can be swapped in and out without worrying about breaking
client code.

# Quantification
It turns out there is a [well understood principle][understandingTypes]
behind this idea called *existential quantification*. Contrast with
universal quantification which says "for all", existential quantification
says "there exists."

Below is an encoding of universal quantification via parametric polymorphism.

```scala
trait Universal {
  def apply[A]: A => A
}
```

Here `Universal#apply` says *for all* choices of `A`, a function `A => A` can be
written. In the [Curry-Howard Isomorphism][propositionsAsTypes], a profound
relationship between logic and computation, this translates to "for all propositions
`A`, `A` impies `A`." It is therefore acceptable to write the following, which picks
`A` to be `Int`.

```scala
def intUniversal(u: Universal): Int => Int =
  (i: Int) => u.apply(i)
// intUniversal: (u: Universal)Int => Int
```

Existential quantification can also be written in Scala.

```scala
trait Existential {
  type A

  def apply: A => A
}
```

Note that this is just one way of encoding existentials - for a deeper
discussion, refer to the excellent [Type Parameters and Type Members][typeParamsMembers]
blog series.

The type parameter on `apply` has been moved up to a type member of the trait.
Practically, this means every instance of `Existential` must pick **one** choice of
`A`, whereas in `Universal` the `A` was parameterized and therefore free. In the
language of logic, `Existential#apply` says "there exists some `A` such that
`A` implies `A`." This "there exists" is the crux of the error when trying
to write a corresponding `intExistential` function.

```scala
def intExistential(e: Existential): Int => Int =
  (i: Int) => e.apply(i)
// <console>:19: error: type mismatch;
//  found   : i.type (with underlying type Int)
//  required: e.A
//          (i: Int) => e.apply(i)
//                              ^
```

In code, the type in `Existential` is chosen per-instance, so there is no way
of knowing what the actual type chosen is. In logical terms, the only guarantee is
that there exists some proposition that satisfies the implication, but it is not
necessarily the case (and often is not) it holds for all propositions.

# Abstract types
In the ML family of languages (e.g. Standard ML, OCaml), existential quantification
and thus information hiding, is achieved through [type members][abstractExistential].
Programs are organized into [modules][ocamlModules] which are what contain these
types.

In Scala, this translates to organizing code with the object system, using the same
type member feature to hide representation. The earlier example of `IntSet` can then
be written:

```scala
/** Abstract signature */
trait IntSet {
  type Repr

  def empty: Repr
  def add(i: Int, repr: Repr): Repr
  def contains(i: Int, repr: Repr): Boolean
}

/** Concrete implementation */
object VectorIntSet extends IntSet {
  type Repr = Vector[Int]

  def empty: Repr = Vector.empty

  def add(i: Int, repr: Repr): Repr = {
    val (isMember, indexOf) = binarySearch(i, repr)
    if (isMember) repr
    else {
      val (prefix, suffix) = repr.splitAt(indexOf)
      prefix ++ Vector(i) ++ suffix
    }
  }

  def contains(i: Int, repr: Repr): Boolean =
    binarySearch(i, repr)._1
}
```

As long as client code is written against the signature, the
representation cannot be leaked.


```scala
def goodUsage(set: IntSet) = {
  import set._
  val s = add(1, add(10, add(5, empty)))
  contains(5, s)
}
// goodUsage: (set: IntSet)Boolean
```

If the user tries to assert the representation type, the type
checker prevents it **at compile time**.

```scala
def badUsage(set: IntSet) = {
  import set._
  val s = add(10, add(1, empty))

  // Maybe it's a Vector
  s.reverse
  contains(10, Vector(10, 5, 1)) //
}
// <console>:23: error: value reverse is not a member of set.Repr
//          s.reverse
//            ^
// <console>:24: error: type mismatch;
//  found   : scala.collection.immutable.Vector[Int]
//  required: set.Repr
//          contains(10, Vector(10, 5, 1)) //
//                             ^
```

# Parametricity
Abstract types enforce information hiding at the definition site (the definition
of `IntSet` is what hides `Repr`). There is another mechanism that enforces information
hiding, which pushes the constraint to the use site.

Consider implementing the following function.

```scala
def foo[A](a: A): A = ???
```

Given nothing is known about `a`, the only possible thing `foo` can do is return `a`. If
instead of a type parameter the function was given more information..

```scala
def bar(a: String): String = "not even going to use `a`"
```

..that information can be leveraged to do unexpected things. This is similar to
the first `IntSet` example when knowledge of the underlying `Vector` allowed unintended
behavior to occur.

From the outside looking in, `foo` is universally quantified - the caller gets to
pick any `A` they want. From the inside looking out, it is
[existentially quantified][existentialInside] - the implementation knows only as much
about `A` as there are constraints on `A` (in this case, nothing).

Consider another function `listReplace`.

```scala
def listReplace[A, B](as: List[A], b: B): List[B] = ???
```

Given the type parameters, `listReplace` looks fairly constrained. The name and signature
suggests it takes each element of `as` and replaces it with `b`, returning a new list.
However, even knowledge of `List` can lead to type checking implementations with strange behavior.

```scala
// Completely ignores the input parameters
def listReplace[A, B](as: List[A], b: B): List[B] = List.empty[B]
```

Here, knowledge of `List` allows the implementation
to create a list out of thin air and use that in the implementation. If instead `listReplace`
only knew about some `F[_]` where `F` is a `Functor`, the implementation becomes much more
constrained.

```scala
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

implicit val listFunctor: Functor[List] =
  new Functor[List] {
    def map[A, B](fa: List[A])(f: A => B): List[B] =
      fa.map(f)
  }

def replace[F[_]: Functor, A, B](fa: F[A], b: B): F[B] =
  implicitly[Functor[F]].map(fa)(_ => b)
```

```scala
replace(List(1, 2, 3), "typelevel")
// res8: List[String] = List(typelevel, typelevel, typelevel)
```

Absent any knowledge of `F` other than the ability to `map` over it, `replace` is
forced to do the correct thing. Put differently, irrelevant information about `F` is hidden.

The fundamental idea behind this is known as parametricity, made popular by Philip Wadler's
seminal [Theorems for free!][theoremsForFree] paper. The technique is best summarized by the
following excerpt from the paper:

> Write down the definition of a polymorphic function on a piece of paper. Tell me its type,
> but be careful not to let me see the function's definition. I will tell you a theorem that
> the function satisfies.

# Why types matter
Information hiding is a core tenet of good program design, and it is important to make
sure it is enforced. Underlying information hiding is existential quantification,
which can manifest itself in computation through abstract types and
parametricity. Few languages support defining abstract type members, and fewer
yet support higher-kinded types used in the `replace` example. It is therefore
to the extent that a language's type system is expressive that
[abstraction can be enforced][tapl].

*This blog post was tested with Scala 2.11.7 using [tut][tut].*

[abstractExistential]: http://dl.acm.org/citation.cfm?id=45065 "Abstract types have existential type"
[existentialInside]: {% post_url 2016-01-28-existential-inside %} "It’s existential on the inside"
[ocamlModules]: https://realworldocaml.org/v1/en/html/files-modules-and-programs.html "Real World OCaml: Files, Modules, and Programs"
[propositionsAsTypes]: http://homepages.inf.ed.ac.uk/wadler/topics/history.html#propositions-as-types "Propositions as Types - Philip Wadler"
[tapl]: https://www.cis.upenn.edu/~bcpierce/tapl/ "Types and Programming Languages - Benjamin C. Pierce"
[theoremsForFree]: http://dl.acm.org/citation.cfm?id=99404 "Theorems for free!"
[typeParamsMembers]: {% post_url 2015-07-13-type-members-parameters %} "Type members are [almost] type parameters"
[tut]: https://github.com/tpolecat/tut "tut: doc/tutorial generator for scala"
[understandingTypes]: http://dl.acm.org/citation.cfm?id=6042 "On understanding types, data abstraction, and polymorphism - Luca Cardelli, Peter Wegner"
