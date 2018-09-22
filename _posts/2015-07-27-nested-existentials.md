---
layout: post
title: Nested existentials

meta:
  nav: blog
  author: S11001001
  pygments: true
  mathjax: true
---

*This is the fifth of a series of articles on “Type Parameters and
Type Members”.  If you haven’t yet, you should
[start at the beginning]({% post_url 2015-07-13-type-members-parameters %}),
which introduces code we refer to throughout this article without
further ado.*

Let’s consider a few values of type `MList`:

```scala
val estrs: MList = MCons("hi", MCons("bye", MNil())): MList.Aux[String]

val eints: MList = MCons(21, MCons(42, MNil())): MList.Aux[Int]

val ebools: MList = MCons(true, MCons(false, MNil())): MList.Aux[Boolean]
```

Recall
[from the first part]({% post_url 2015-07-13-type-members-parameters %}#why-all-the-type-t)
that the equivalent type in `PList` style is `PList[_]`.  Now, these
variables all have the “same” type, by virtue of forgetting what their
specific element type is, though you know that every value of, for
example, `estrs` has the same type.

What if we list *different* existentials?
-----------------------------------------

Lists hold values of the same type, and as you might expect, you can
put these three lists in another list:

```scala
val elists: PList[MList] = 
  PCons(estrs, PCons(eints, PCons(ebools, PNil())))
```

Again, the equivalent is `PList[PList[_]]`.  We can see what this
means merely by doing substitution in the `PList` type.

```scala
sealed abstract class PList
final case class PNil() extends PList
final case class PCons(head: MList, tail: PList)
// don't compile this, it's a thought process
```

Equivalently, `head` would have type `PList[_]`, a homogeneous list of
unknown element type, just like `MList`.

Method equivalence … broken?
----------------------------

But we come to a problem.  Suppose we wish to count the elements of
doubly-nested lists.

```scala
def plenLength(xss: PList[PList[_]]): Int =
  plenLengthTP(xss)

def plenLengthTP[T](xss: PList[PList[T]]): Int =
  xss match {
    case PNil() => 0
    case PCons(h, t) => plengthT(h) + plenLengthTP(t)
  }

TmTp5.scala:16: no type parameters for method plenLengthTP:
⤹ (xss: tmtp.PList[tmtp.PList[T]])Int exist so that it
⤹ can be applied to arguments (tmtp.PList[tmtp.PList[_]])
 --- because ---
argument expression's type is not compatible with formal parameter type;
 found   : tmtp.PList[tmtp.PList[_]]
 required: tmtp.PList[tmtp.PList[?T]]
```

According to our equivalence test, neither of these methods works to
implement the other!  This despite
[the “simple rule” we have already discussed]({% post_url 2015-07-13-type-members-parameters %}#when-is-existential-ok).
Here’s the error the other way.

```scala
TmTp5.scala:20: type mismatch;
 found   : tmtp.PList[tmtp.PList[T]]
 required: tmtp.PList[tmtp.PList[_]]
```

The problem with calling `plenLengthTP` from `plenLength` is *there is
no one `T` we can choose, even an unspeakable one, to call
`plenLengthTP`*.  That’s what the `?T` and the “no type parameters”
phrasing in the first error above means.

This is an accurate compiler error because `PList[PList[_]]` means
`PList[PList[E] forSome {type E}]`.  Let’s see the substitution again.

```scala
sealed abstract class PList
final case class PNil() extends PList
final case class PCons(head: PList[E] forSome {type E}, tail: PList)
// don't compile this, it's a thought process
```

Java has the same problem.  See?

```java
int llLength(final List<List<?>> xss) {
    return llLengthTP(xss);
}

<T> int llLengthTP(final List<List<T>> xss) {
    return 0;  // we only care about types in this example
}

TmTp5.java:7:  error: method llLengthTP in class TmTp5
⤹ cannot be applied to given types;
    return llLengthTP(xss);
           ^

// or, with llLengthTP calling llLength
TmTp5.java:11:  error: incompatible types: List<List<T>>
⤹ cannot be converted to List<List<?>>
    return llLength(xss);
                    ^
```

This discovery, which I made for myself
[in the depths of the Ermine Java code](https://bitbucket.org/ermine-language/ermine-writers/src/c63d4060a74f1c8520ea1c8c3ba51ebd5d269780/writers/javafx/src/main/java/com/clarifi/reporting/writers/jfx/table/JFXTables.java?at=default#JFXTables.java-163)
(though it was certainly already well-known to others), was my first
clue, personally, that the term
[“wildcard” was a lie, as discussed in a previous part]({% post_url 2015-07-16-method-equiv %}#why-are-existentials-harder-to-think-about).

Scoping existential quantifiers
-------------------------------

The difference is, in Scala, we can write an equivalent for
`plenLengthTP`, using the Scala-only
[`forSome` *existential quantifier*](http://www.artima.com/pins1ed/combining-scala-and-java.html#29.3).

```scala
def plenLengthE(xss: PList[PList[E]] forSome {type E}): Int =
  plenLengthTP(xss)
```

Of course, this type doesn’t mean the same thing as `plenLength`’s
type; for both `plenLengthE` and `plenLengthTP`, we demand proof that
each sublist in the argument has the same element type, which is not a
condition satisfied by either `PList[PList[_]]` or its equivalent
`PList[MList]`.

<div class="side-note">
  <p>The reason you can’t invoke <code>plenLength</code> from
  <code>plenLengthTP</code> is complicated, even for this article.  In
  short, <code>plenLength</code> demands evidence that,
  <em>supposing</em> <code>PList</code> had a method taking an
  argument of the element type,
  e.g. <code>def lookAt(x: T): Unit</code>, it could do things like
  <code>xss.lookAt(PList("hi", PNil()))</code>.  In
  <code>plenLengthTP</code>, this hypothetical method could only be
  invoked with empty lists, or lists gotten by inspecting
  <code>xss</code> itself.</p>

  <p>That no such method exists is irrelevant for the purposes of this
  reasoning; we have written the definition of <code>PList</code> in a
  way that scalac assumes that such a method may exist.  You can
  determine the consequences yourself by adding the
  <code>lookAt</code> method to <code>PList</code>, repeating the
  above substitution for <code>PList</code>, and thinking about the
  meaning of the resulting <code>def lookAt(x:
  PList[E] forSome {type E}): Unit</code>.</p>
</div>

Let’s examine the meaning of the type
`PList[PList[E]] forSome {type E}`.  It requires a little bit more
mental suspension.

```scala
// Let there be some unknown (abstract)
type E
// then the structure of the value is
sealed abstract class PList
final case class PNil() extends PList
final case class PCons(head: PList[E], tail: PList)
// don't compile this, it's a thought process
```

By moving the `forSome` *existential scope* outside the outer `PList`,
we also move the existential type variable outside of the whole
structure, substituting *the same* variable for each place we’re
expanding the type under consideration.  Once the `forSome` scope
extends over the whole type, Scala can pick that type as the parameter
to `plenLengthTP`.

This isn’t possible in Java at all; `PList<PList<?>>` is your only
choice, as **`?` in Java, like `_` in Scala, is always scoped to
exactly one level outside**.  So in Java, you simply can’t write
`plenLengthE`’s type.  Luckily, the type-parameter equivalent is
perfectly expressible.

What happens when I move the existential scope?
-----------------------------------------------

Of course, moving the scope makes the type mean something different,
which you can tell by counting how many `E`s there will be in a value.
A `PList[PList[_]]` is a list of lists where each list may have a
different, unknown element type, like `elists`.  A
`PList[PList[E]] forSome {type E}` is a list of lists where you still
don’t know the inner element type, but you know it’s the same for each
sublist.  We can tell that because, in the expansion, there’s only one
`E`, whereas the expansion for the former has an `E` introduced in
each `head` value.

So for the latter it is type-correct to, say, move elements from one
sublist to another; you know that, whichever pair of sublists you
choose to make this trade, they have the same element type.  But you
*don’t know that* for `PList[PList[_]]`.

Similarly, also by substitution, `PList[_] => Int` is a function that
takes `PList`s of any element type and returns `Int`, like `plengthE`.
You can figure this out by substituting for
[`Function1#apply`](https://github.com/scala/scala/blob/v2.11.7/src/library/scala/Function1.scala#L32-L36):

```scala
def apply(v1: T1): R
def apply(v1: PList[_]): Int
```

But `(PList[E] => Int) forSome {type E}` is a function that takes
`PList`s of *one specific* element type that we don’t know.

```scala
// Let there be some unknown (abstract)
type E
// then the method is
def apply(v1: List[E]): Int
```

It’s easy to use existential scoping to create functions that are
impossible to call and other values that are impossible to use besides
functions.  This is almost one of those:

```scala
def badlength: (PList[E] => Int) forSome {type E} = plengthE
badlength(??? : PList[Int])

TmTp5.scala:29: type mismatch;
 found   : tmtp.PList[Int]
 required: tmtp.PList[E] where type E
badlength(??? : PList[Int])
              ^
```

But in this case, there is one way we can call this function: with an
empty list.  Whatever the `E` is, it will be inferred when we call
`PNil()`.  So `badlength(PNil())` works.

There is a broader theme here hinted at by the interaction between
`PNil` and `badlength`: **the most efficient, most easily understood
way to work with values of existential type is with type-parameterized
methods**.  But we’ll get to that later.

Back to type members
--------------------

Let us translate the working existential variant we discovered above
to the `PList[MList]` form of the function, though.  What is the
existential equivalent to `mlenLengthTP`?

```scala
def mlenLengthTP[T](xss: PList[MList.Aux[T]]): Int =
  xss match {
    case PNil() => 0
    case PCons(h, t) => mlength(h) + mlenLengthTP(t)
  }

def mlenLength(xss: PList[MList]): Int =
  mlenLengthTP(xss)

TmTp5.scala:38: type mismatch;
 found   : tmtp.PList[tmtp.MList]
 required: tmtp.PList[tmtp.MList.Aux[this.T]]
  mlenLengthTP(xss)
               ^
```

`MList` is equivalent to `MList {type T = E} forSome {type E}`.  We
can prove that directly in Scala.

```scala
scala> implicitly[MList =:= (MList {type T = E} forSome {type E})]
res0: =:=[tmtp.MList,tmtp.MList{type T = E} forSome { type E }] = <function1>
```

That’s why we could use `runStSource` to infer a type parameter for
the existential `S` in
[the last post]({% post_url 2015-07-23-type-projection %}#type-parameters-see-existentially):
the scope is on the outside, so there’s exactly one type parameter to
infer.  So the scoping problem now looks very similar to the
`PList`-in-`PList` problem, and we can write:

```scala
def mlenLengthE(xss: PList[MList.Aux[E]] forSome {type E})
  : Int = mlenLengthTP(xss)
```

A triangular generalization
---------------------------

Once again, `mlenLengthE` demands proof that each sublist of `xss` has
the same element type, by virtue of the position of its `forSome`
scope.  We can’t satisfy that with `elists`.

```scala
mlenLengthE(elists)
```

Or, we *shouldn’t* be able to, anyway.
[Sometimes, the wrong thing happens.](https://issues.scala-lang.org/browse/SI-9410)
We get the right error when we try to invoke `mlenLengthTP`.

```scala
mlenLengthTP(elists)

TmTp5.scala:43: type mismatch;
 found   : tmtp.PList[tmtp.MList]
 required: tmtp.PList[tmtp.MList.Aux[this.T]]
    (which expands to)  tmtp.PList[tmtp.MList{type T = this.T}]
mlenLengthTP(elists)
             ^
```

So we have `mlenLengthE` $\equiv\_m$ `mlenLengthTP`.  `mlenLength`,
however, is incompatible with both; neither is more general than the
other!  What we really want is a function that is more general than
all three, and subsumes all their definitions.  Here it is, in two
variants: one half-type-parameterized, the other wholly existential.

```scala
def mlenLengthTP2[T <: MList](xss: PList[T]): Int =
  xss match {
    case PNil() => 0
    case PCons(h, t) => mlength(h) + mlenLengthTP2(t)
  }

def mlenLengthE2(xss: PList[_ <: MList]): Int =
  xss match {
    case PNil() => 0
    case PCons(h, t) => mlength(h) + mlenLengthTP2(t)
  }
```

We’ve woven a tangled web, so here are, restated, the full
relationships for the `MList`-in-`PList` functions above.

1. `mlenLengthTP2` $\equiv\_m$ `mlenLengthE2`
2. `mlenLengthTP` $\equiv\_m$ `mlenLengthE`
3. $\neg($`mlenLength` $<:\_m$ `mlenLengthE`$)$
4. $\neg($`mlenLengthE` $<:\_m$ `mlenLength`$)$
5. $\neg($`mlenLength` $<:\_m$ `mlenLengthTP`$)$
6. $\neg($`mlenLengthTP` $<:\_m$ `mlenLength`$)$
7. `mlenLengthTP2` $<\_m$ `mlenLengthTP`
8. `mlenLengthTP2` $<\_m$ `mlenLength`
9. `mlenLengthTP2` $<\_m$ `mlenLengthE`
10. `mlenLengthE2` $<\_m$ `mlenLengthTP`
11. `mlenLengthE2` $<\_m$ `mlenLength`
12. `mlenLengthE2` $<\_m$ `mlenLengthE`

Moreover, the full existential in `mlenLengthE2` is shorthand for:

```scala
PList[E] forSome {
  type E <: MList {
    type T = E2
  } forSome {type E2}
}
```

…a nested existential, though not in the meaning I intend in the title
of this article.  You can prove it with `=:=`, as above.

And I say all this simply as a means of saying that *this* is what
you’re signing up for when you decide to “simplify” your code by using
type members instead of parameters and leaving off the refinements
that make them concrete.

In
[the next part, “Values never change types”]({% post_url 2015-07-30-values-never-change-types %}),
we’ll get some idea of why working with existential types can be so
full of compiler errors, especially when allowing for mutation and
impure functions.

*This article was tested with Scala 2.11.7 and Java 1.8.0_45.*
