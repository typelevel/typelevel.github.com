---
layout: post
title: Equivalence versus Equality

meta:
  nav: blog
  author: TomasMikula
  pygments: true
  mathjax: true

tut:
  scala: 2.12.1
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.1
    - com.github.tomasmikula::hasheq:0.3
---

_This is a guest post by Tomas Mikula. It was initially published as a [document](https://github.com/TomasMikula/hasheq/blob/017f289caac398723501b194cd2b36c4584df638/Equivalence-Equality.md) in the [hasheq](https://github.com/TomasMikula/hasheq). It has been slightly edited and is being republished here with the permission of the original author._

This article describes what we mean when we say that the data structures in this library are _equivalence-aware_ in a _type-safe_ fashion.

## Equivalence

_Set_ is a data structure that doesn't contain _duplicate_ elements. An implementation of _Set_ must therefore have a way to compare elements for _"sameness"_.
A useful notion of sameness is _equivalence_, i.e. a binary relation that is _reflexive_, _symmetric_ and _transitive_.
Any reasonable implementation of _Set_ is equipped with _some_ equivalence relation on its element type.

Here's the catch: For any type with more than one inhabitant there are _multiple_ valid equivalence relations.
We cannot (in general) pick one that is suitable in all contexts.
For example, are these two binary trees _same_?

```
  +            +
 / \          / \
1   +        +   3
   / \      / \
  2   3    1   2
```

It depends on the context. They clearly have different structure, but they are both binary search trees containing the same elements.
For a balancing algorithm, they are different trees, but as an implementation of _Set_, they represent the same set of integers.

## Equality

Despite the non-uniqueness, there is one equivalence relation that stands out: _equality_.
Two objects are considered _equal_ when they are _indistinguishable_ to an observer.
Formally, equality is required to have the _substitution property:_

<p>\[ \forall a,b \in A, \forall f \in (A \to B): a=_A b \implies f(a)=_B f(b) \]</p>

(Here, $=\_A$ denotes equality on $A$, $=\_B$ denotes equality on $B$.)

Equality is the finest equivalence: whenever two elements are _equal_, they are necessarily _equivalent_ with respect to every equivalence.

## Choices in libraries

Popular Scala libraries take one of these two approaches when dealing with comparing elements for _"sameness"_.

The current approach of [cats](https://github.com/typelevel/cats/) is _equality_.
Instances of the `cats.Eq[A]` typeclass are required to have all the properties of equality, including the substitution property above.
The problem with this approach is that for some types, such as `Set[Int]`, equality is too strict to be useful:
Are values `Set(1, 2)` and `Set(2, 1)` _equal_?
For that to be true, they have to be indistinguishable by any function.
Let's try `(_.toList)`:

```scala
scala> Set(1, 2).toList == Set(2, 1).toList
res0: Boolean = false
```

So, `Set(1, 2)` and `Set(2, 1)` are clearly _not_ equal.
As a result, we cannot use `Set[Int]` in a context where equality is required (without cheating).

On the other hand, [scalaz](https://github.com/scalaz/scalaz/) uses unspecified _equivalence_.
Although the name `scalaz.Equal[A]` might suggest _equality_, instances of this typeclass are only tested for properties of _equivalence_.
As mentioned above, there are multiple _valid_ equivalence relations for virtually any type.
When there are also multiple _useful_ equivalences for a type, we are at risk of mixing them up (and the fact that they are usually resolved as implicit arguments only makes things worse).

## Equivalence-aware sets (a.k.a. setoids)

Let's look at how _we_ deal with this issue. We define typeclass `Equiv` with an extra type parameter that serves as a _"tag"_ identifying the meaning of the equivalence.

```scala
trait Equiv[A, Eq] {
  def equiv(a: A, b: A): Boolean
}
// defined trait Equiv
```

For the compiler, the "tag" is an opaque type. It only has specific meaning for humans. The only meaning it has for the compiler is that different tags represent (intensionally) different equivalence relations.

An _equivalence-aware_ data structure then carries in its _type_ the tag of the equivalence it uses.

```scala
import hasheq._
// import hasheq._

import hasheq.immutable._
// import hasheq.immutable._

import hasheq.std.int._
// import hasheq.std.int._
```

```scala
scala> HashSet(1, 2, 3, 4, 5)
res0: hasheq.immutable.HashSet[Int] = HashSetoid(5, 1, 2, 3, 4)
```

What on earth is `HashSetoid`?
A [_setoid_](https://en.wikipedia.org/wiki/Setoid) is an _equivalence-aware set_.
`HashSetoid` is then just a setoid implementated using hash-table.
Let's look at the definition of `HashSet`:

```scala
type HashSet[A] = HashSetoid[A, Equality.type]
```

So `HashSet` is just a `HashSetoid` whose equivalence is _equality_.
To create an instance of `HashSet[Int]` above, we needed to have an implicit instance of `Equiv[Int, Equality.type]` in scope.

```scala
implicitly[Equiv[Int, Equality.type]]
```

For the compiler, `Equality` is just a rather arbitrary singleton object.
It only has the meaning of mathematical _equality_ for us, humans.

There is a convenient type alias provided for _equality_ relation:

```scala
type Equal[A] = Equiv[A, Equality.type]
```

```scala
implicitly[Equal[Int]]
```

So how do we deal with the problem of set equality mentioned above, i.e. that `HashSet(1, 2)` and `HashSet(2, 1)` are not truly _equal_?
We just don't provide a definition of equality for `HashSet[Int]`.

```scala
scala> implicitly[Equal[HashSet[Int]]]
<console>:22: error: could not find implicit value for parameter e: hasheq.Equal[hasheq.immutable.HashSet[Int]]
       implicitly[Equal[HashSet[Int]]]
                 ^
```

But that means we cannot have a `HashSet[HashSet[Int]]`!
(Remember, for a `HashSet[A]`, we need an instance of `Equal[A]`, and we just showed we don't have an instance of `Equal[HashSet[Int]]`.)

```scala
scala> HashSet(HashSet(1, 2, 3, 4, 5))
<console>:22: error: could not find implicit value for parameter A: hasheq.Hash[hasheq.immutable.HashSet[Int]]
       HashSet(HashSet(1, 2, 3, 4, 5))
              ^
```

But we can have a `HashSetoid[HashSet[Int], E]`, where `E` is _some_ equivalence on `HashSet[Int]`.

```scala
scala> HashSet.of(HashSet(1, 2, 3, 4, 5))
res5: hasheq.immutable.HashSetoid[hasheq.immutable.HashSet[Int],hasheq.immutable.Setoid.ContentEquiv[Int,hasheq.Equality.type]] = HashSetoid(HashSetoid(5, 1, 2, 3, 4))
```

`HashSet.of(elems)` is like `HashSet(elems)`, except it tries to infer the equivalence on the element type, instead of requiring it to be equality.

Notice the _equivalence tag_: `Setoid.ContentEquiv[Int, Equality.type]`.
Its meaning is (again, for humans only) that two setoids are equivalent when they contain the same elements (here, of type `Int`), as compared by the given equivalence of elements (here, `Equality`).

The remaining question is: How does this work in the presence of _multiple useful equivalences?_

Let's define another equivalence on `Int` (in addition to the provided equality).

```scala
// Our "tag" for equivalence modulo 10.
// This trait will never be instantiated.
sealed trait Mod10

// Provide equivalence tagged by Mod10.
implicit object EqMod10 extends Equiv[Int, Mod10] {
  def mod10(i: Int): Int = {
    val r = i % 10
    if (r < 0) r + 10
    else r
  }
  def equiv(a: Int, b: Int): Boolean = mod10(a) == mod10(b)
}

// Provide hash function compatible with equivalence modulo 10.
// Note that the HashEq typeclass is also tagged by Mod10.
implicit object HashMod10 extends HashEq[Int, Mod10] {
  def hash(a: Int): Int = EqMod10.mod10(a)
}
```

Now let's create a "setoid of sets of integers", as before.

```scala
scala> HashSet.of(HashSet(1, 2, 3, 4, 5))
res13: hasheq.immutable.HashSetoid[hasheq.immutable.HashSet[Int],hasheq.immutable.Setoid.ContentEquiv[Int,hasheq.Equality.type]] = HashSetoid(HashSetoid(5, 1, 2, 3, 4))
```

This still works, because `HashSet` requires an _equality_ on `Int`, and there is only one in the implicit scope (the newly defined equivalence `EqMod10` is _not_ equality).
Let's try to create a "setoid of setoids of integers":

```scala
scala> HashSet.of(HashSet.of(1, 2, 3, 4, 5))
<console>:24: error: ambiguous implicit values:
 both method hashInstance in object int of type => hasheq.Hash[Int]
 and object HashMod10 of type HashMod10.type
 match expected type hasheq.HashEq[Int,Eq]
       HashSet.of(HashSet.of(1, 2, 3, 4, 5))
                            ^
```

This fails, because there are now more equivalences on `Int` in scope.
(There are now also multiple hash functions, which is what the error message actually says.)
We need to be more specific:

```scala
scala> HashSet.of(HashSet.of[Int, Mod10](1, 2, 3, 4, 5))
res15: hasheq.immutable.HashSetoid[hasheq.immutable.HashSetoid[Int,Mod10],hasheq.immutable.Setoid.ContentEquiv[Int,Mod10]] = HashSetoid(HashSetoid(5, 1, 2, 3, 4))
```

Finally, does it **prevent mixing up equivalences**? Let's see:

```scala
scala> val s1 = HashSet(1,  2,  3,         11, 12, 13    )
s1: hasheq.immutable.HashSet[Int] = HashSetoid(1, 13, 2, 12, 3, 11)

scala> val s2 = HashSet(    2,  3,  4,  5,         13, 14)
s2: hasheq.immutable.HashSet[Int] = HashSetoid(5, 14, 13, 2, 3, 4)

scala> val t1 = HashSet.of[Int, Mod10](1,  2,  3,         11, 12, 13    )
t1: hasheq.immutable.HashSetoid[Int,Mod10] = HashSetoid(1, 2, 3)

scala> val t2 = HashSet.of[Int, Mod10](    2,  3,  4,  5,         13, 14)
t2: hasheq.immutable.HashSetoid[Int,Mod10] = HashSetoid(5, 2, 3, 4)
```

Combining compatible setoids:

```scala
scala> s1 union s2
res16: hasheq.immutable.HashSetoid[Int,hasheq.Equality.type] = HashSetoid(5, 14, 1, 13, 2, 12, 3, 11, 4)

scala> t1 union t2
res17: hasheq.immutable.HashSetoid[Int,Mod10] = HashSetoid(5, 1, 2, 3, 4)
```

Combining incompatible setoids:

```scala
scala> s1 union t2
<console>:26: error: type mismatch;
 found   : hasheq.immutable.HashSetoid[Int,Mod10]
 required: hasheq.immutable.HashSetoid[Int,hasheq.Equality.type]
       s1 union t2
                ^

scala> t1 union s2
<console>:26: error: type mismatch;
 found   : hasheq.immutable.HashSet[Int]
    (which expands to)  hasheq.immutable.HashSetoid[Int,hasheq.Equality.type]
 required: hasheq.immutable.HashSetoid[Int,Mod10]
       t1 union s2
                ^
```


## Conclusion

We went one step further in the direction of type-safe equivalence in Scala compared to what is typically seen out in the wild today.
There is nothing very sophisticated about this encoding.
I think the major win is that we can design APIs so that the extra type parameter (the "equivalence tag") stays unnoticed by the user of the API as long as they only deal with _equalities_.
As soon as the equivalence tag starts requesting our attention (via an ambiguous implicit or a type error), it is likely that the attention is justified.

*This article was tested with Scala 2.11.8 and hasheq version 0.3.*
