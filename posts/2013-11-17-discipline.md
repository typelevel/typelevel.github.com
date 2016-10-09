---
layout: post
title: Law Enforcement using Discipline

meta:
  nav: blog
  author: larsrh
  pygments: true
---

Some nine or ten months ago, [Spire](http://github.com/non/spire)'s project structure underwent a major reorganization.
Simultaneously, the [Scalacheck](http://www.scalacheck.org/) bindings were refactored, completely overhauling the law-checking infrastructure.

Requirements
------------

The main goal was to make it easy to check that instances of Spire's type classes adhere to the set of algebraic laws of the respective type classes.
[Scalaz](https://github.com/scalaz/scalaz) also has such an infrastructure, so why not take that one?
The problem is that in Spire, the hierarchy of type classes is a little bit more complex:

One one hand, there is a "generic" tower of type classes including `Semigroup`, `Monoid` and the like, where each successive type extends its predecessor.
On the other hand, this tower is replicated *twice* for their "additive" and "multiplicative" counterparts.
These classes are isomorphic, up to the semantics, and hence naming of their operations.

This distinction is quite useful, because now one can write:

```scala
trait Semiring[A] extends AdditiveMonoid[A] with MultiplicativeSemigroup[A] {
  // ...
}
```

without clashes between the additive and multiplicative binary operations.
Also, a semiring can now be quite naturally treated as an additive monoid and a multiplicative semigroup (but not as a generic semigroup, which would be ambiguous).
(One could consider this the *third* hierarchy of algebraic type classes in spire.)

When checking laws, we do not want to repeat the same laws over and over again.
Hence, we need some way to express that certain type classes share laws with others which are not necessarily in the same type hierarchy.


Interface
---------

The implementation fundamentally depends on Scalacheck.
To be more specific, it uses `Prop` as the elementary unit of testing.

Now, a set of named `Prop`s do not quite suffice as the "law" of a type class.
First, to avoid ambiguous naming, let us call the complete law of a type class (including dependencies), a "rule set".

To satisfy our requirement of having dependencies from (potentially) different hierarchies, we will distinguish *parents* and *bases*.
A *parent* is a rule set of a type class in the same hierachy, whereas a *base* can come from everywhere.
This distinction is expressed with the use of path-dependent types:

```scala
trait Laws {

  trait RuleSet {
    def name: String
    def bases: Seq[(String, Laws#RuleSet)] = Seq()
    def parents: Seq[RuleSet] = Seq()
    def props: Seq[(String, Prop)] = Seq()

    // ...
  }

}
```

As we can see, `parents` uses type `RuleSet`, which constrains parents to the same outer `Laws` instance.
In contrast, `bases` uses the type `Laws#RuleSet` which means that bases can come from other instances of `Laws`.

When you define type classes, the general idea is to define one instance of `Laws` for each *hierarchy* of type classes.
Coming back to the Spire example, that could look like this:

```scala
trait GroupLaws[A] {
  def semigroup(implicit A: Semigroup[A]): RuleSet = new RuleSet {
    def name = "semigroup"
    def props = // ...
  }

  def monoid(implicit A: Monoid[A]): RuleSet = new RuleSet {
    def name = "monoid"
    def parents = Seq(semigroup)
    def props = // ...
  }
}

trait AdditiveLaws[A] {
  def groupLaws: GroupLaws[A]

  def semigroup(implicit A: AdditiveSemigroup[A]): RuleSet = new RuleSet {
    def name = "additive semigroup"

    // `.additive` converts an additive X to a generic X
    def bases = Seq("additive" → groupLaws.semigroup(A.additive))
  }

  def monoid(implicit A: AdditiveMonoid[A]): RuleSet = new RuleSet {
    def name = "additive monoid"

    def bases = Seq("additive" → groupLaws.monoid(A.additive))
    def parent = Seq(semigroup)
  }
}
```

This now clearly expresses the intention:

* A monoid is a semigroup.
* An additive semigroup should satisfy the laws of a semigroup.
* An additive monoid is an additive semigroup and should satisfy the laws of a monoid.

Note that in the definitions inside `AdditiveLaws`, no properties have been restated.
The system will automatically take care that all the properties of the parents and the bases are being checked.

Obviously, this is not very interesting yet, because so far it could have been achieved by other means.
If you are interested in more complex examples, check the sources of Spire:
There are a couple of examples where the additive and multiplicative versions have extra checks which are not covered by the generic version.


Implementation
--------------

Now, the question is how to compute the set of all properties which need to be checked.
A naïve algorithm would just recursively traverse all bases and parents, and check the union of all the property sets.

However, this leads to unnecessary work.
Consider the rule set of an additive monoid.
There, the properties of semigroup would be included twice:
once via the semigroup base of the additive semigroup parent, and once via the semigroup parent of the monoid base.

While checking properties twice certainly does no damage, we still do not want to pay for that overhead.
Hence, a slightly smarter algorithm is used.
We compute the set of all properties of a certain class by taking the union of these sets:

* the properties of the class itself
* recursively, the properties of all its parents (ignoring their bases)
* recursively, the set of all properties of its bases

In order to present the user a more transparent output, the names of the properties are hierarchical.
When a base is pulled in as dependency, their properties are additionally prefixed with the name of the base.
This should make it very easy to see where exactly a property came from.

There is a slight complication, though.
Recall the definition of a semiring in spire, which is given above.
A semiring actually consists of two different semigroups of which we must check the laws separately.
At this point, it is not immediately clear what would happen with the presented algorithm.
With just a minor clarification it turns out that this is not actually a problem:
The rule set of a semiring specifies two bases (one for the additive component and one for the multiplicative component), and we only need to make sure that they have different names.
Laws pulled in via different bases are considered different, and are hence not conflated.

Usage
-----

Previously, this new law checking infrastructure was tailored to be used just in Spire.
Since it is useful outside of Spire, too, it has recently been generalized and pulled out into a separate project: [Discipline](https://github.com/typelevel/discipline).

In there, you can find a stripped-down example of the Spire use case.

Furthermore, there is integration with Specs2 and ScalaTest.
You just have to extend the `specs2.Discipline` (or `scalatest.Discipline`, respectively) trait, and write

```scala
checkAll("Int", RingLaws[Int].ring /* put your own `RuleSet` here */)
```

and rule sets are expanded and turned into individual tests automatically.
For a Specs2-based tests, this will result in the following output (similar for ScalaTest):

```
[info] ring laws must hold for Int
[info]
[info]  + ring.additive:group.base:group.associative
[info]  + ring.additive:group.base:group.identity
[info]  + ring.additive:group.base:group.inverse
[info]  + ring.multiplicative:monoid.base:monoid.associative
[info]  + ring.multiplicative:monoid.base:monoid.identity
[info]  + ring.distributive
```

Observe that the associativity law for semigroups shows up twice (additive and multiplicative), but not four times (as would have happened with the naïve algorithm).

In the future, we will investigate whether Scalaz can also be migrated towards Discipline, for a more unified approach to law checking.
