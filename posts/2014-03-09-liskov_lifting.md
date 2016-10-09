---
layout: post
title: When can Liskov be lifted?

meta:
  nav: blog
  author: S11001001
  pygments: true
---

Scalaz avoids
[variance in the sense of the Scala type parameter annotation](http://docs.scala-lang.org/tutorials/tour/variances.html),
with its associated higher-kind implications, except where it has
historically featured variance; even here, variance is vanishing as
[unsoundness in its released implementations is discovered](https://github.com/scalaz/scalaz/pull/630).

There is a deeply related concept in Scalaz's typeclasses, though:
*covariant and contravariant
functors*. [`Functor`](http://docs.typelevel.org/api/scalaz/stable/7.0.4/doc/#scalaz.Functor)
is traditional shorthand for covariant functor, whereas
[`Contravariant`](http://docs.typelevel.org/api/scalaz/stable/7.0.4/doc/#scalaz.Contravariant)
represents contravariant functors.

These concepts are related, but neither subsumes the other. A
`Functor` instance does not require its parameter to be
Scala-covariant. A type can be Scala-covariant over a parameter
without having a legal `Functor` instance.

`Liskov`
--------

[`Liskov`](http://docs.typelevel.org/api/scalaz/stable/7.0.4/doc/#scalaz.Liskov),
also known as `<~<` and very close to Scala's own
[`<:<`](http://www.scala-lang.org/api/current/#scala.Predef$$$less$colon$less),
represents a subtyping relationship, and is defined by the ability to
lift it into Scala-covariant and Scala-contravariant parameter
positions, like so:

```scala
def liftCo[F[+_], A, B](a: A <~< B): F[A] <~< F[B]
def liftCt[F[-_], A, B](a: A <~< B): F[B] <~< F[A]
```

As `Liskov` is, soundly, Scala-variant, this can be implemented
without a cast. However, it can only be called with Scala-covariant
`F`.

By definition, applying an `A <~< B` to a value of type `A` should
yield a value of type `B`, but must also do nothing but return the
value; in other words, it is an *operational identity*. Despite the
limitation of `liftCo`, for functorial values that are *parametrically
sound*, even for Scala-invariant `F`, it is operationally sound to
lift `Liskov`, though impossible to implement without exploiting Scala
soundness holes:

```scala
def liftCvf[F[_]: Functor](a: A <~< B): F[A] <~< F[B]
```

For example,
[this is sound for `scalaz.IList`](https://github.com/scalaz/scalaz/blob/v7.1.0-M5/core/src/main/scala/scalaz/IList.scala#L434-L437).

But `IList[Int]` isn't a subtype of `IList[Any]`!
-------------------------------------------------

Sure, as far as Scala is concerned.  But `Liskov` is all about making
claims that can't directly be proven due to the language's
limitations.  Haskell allows you to constrain functions with type
equalities, which is very important when working with type families;
Scala doesn't, so we get
[`Leibniz`](http://docs.typelevel.org/api/scalaz/stable/7.0.4/doc/#scalaz.Leibniz)
instead.

A type is a set of values.  Where *Y* is a supertype of *X*, every
value in *X* is in *Y*.  Since `IList[String]("hi", "there")` has the
same representation as `IList[Any]("hi", "there")`, they are the same
value.  This is true for *all* `IList[String]`s, but the opposite is
not true; therefore, `IList[Any]` is an `IList[String]` supertype,
regardless of what Scala knows.

So doing a casting `Liskov` lift, like that into `IList`, is
essentially “admitted” in a proof system sense.  You are saying, “I
can't prove that this subtype relationship holds, but it does, so
assume it.”

**To decide whether an admitted `A <~< B` is sound**: suppose that the
compiler admits that subtyping relationship.  Can it then draw
incorrect conclusions, about the sets of values, derived from that
assumption?  This is the cardinal rule.

By extension, **to decide whether an `F` permits Liskov lifting**:
does the above rule pass given `F[A] <~< F[B]` *for all* `A`, `B`
where `B` is a supertype of `A`?

Parametrically sound covariance
-------------------------------

Because a `Liskov` must be an operational identity, it is essential
that, given any value of `F[A]`, for all supertypes `B` of `A`, the
representation of `F[B]` must be identical.  You can determine this by
analyzing the subclasses of `F` as an algebraic data type, where the
key test is to ensure that `A` *never* appears in the primitive
contravariant position: as the parameter to a function.  This test is
not quite enough to prove that `Liskov` lifting is sound, but it gets
us most of the way.

For example, an `IList` of `"hi"` and `"there"` has exactly the same
representation whether you instantiated the `IList` with `String` or
with `Any`. So that is a good first test. If a class changes its
construction behavior based on manifest type information, or its basic
data construction functions violate
[the rules of parametricity](http://failex.blogspot.com/2013/06/fake-theorems-for-free.html),
that is a good sign that the data type cannot follow these rules.

This data type analysis is recursive: a data type being variant in a
parametrically sound way over a parameter requires that all
appearances of that parameter in elements of your data type are also
parametrically sound in that way. For example, if your `F[A]` contains
an `IList[A]` in its representation, you may rely on `IList`'s
parametrically sound covariance when considering `F`'s.

Any `var`, or `var`-like thing such as an `Array`, places its
parameter in an invariant position, because it features a getter
(return type) and setter (parameter type). So its presence in the data
model invalidates `Liskov` lifting if the type parameter appears
within it.

Obviously, runtime evidence of a type parameter's value eliminates the
possibility of lifting `Liskov` over that parameter.

You cannot perform this representation analysis without considering
all subclasses of a class under consideration. For example,
considering only
[`HashSet`](http://www.scala-lang.org/api/current/#scala.collection.immutable.HashSet),
[`collection.immutable.Set`](http://www.scala-lang.org/api/current/#scala.collection.immutable.Set)
appears to allow `Liskov` lifting. However,
[`TreeSet`](http://www.scala-lang.org/api/current/#scala.collection.immutable.TreeSet),
a subclass of `Set`, contains a function `(A, A) => Ordering`. If
*any* representation contains a contradiction like this, `Liskov`
lifting is unsafe. You cannot constrain `Liskov` application by a
runtime test.

If you permit open subclassing, you must either declare the
requirement to preserve parametric covariance, or accept that it will
be violated, and so forbid `Liskov` lifting.

Data that doesn't use a type parameter doesn't affect its parametric
soundness.  For example, here `A` is invariant, but `B` is covariant:

```scala
final case class VA[A, B](xs: Set[A], ys: IList[B])
```

GADTs
-----

Some features of Scala resist simple ADT analysis, so must be
considered separately from the above.  Despite their sound covariance
considering only the representational rules in the previous section,
they still break the cardinal rule by allowing the compiler to make
invalid assumptions about the sets of values.  A “recoverable phantom”
implies a type relationship that forbids `Liskov`-lifting, for
example:

```scala
sealed trait Gimme[A]
case object GimmeI extends Gimme[Int]
```

In pattern matching, given a `Gimme[A]` over unknown `A`, matching
`GimmeI` successfully recovers the type equality `A ~ Int`; therefore,
`Liskov`-lifting is unsound for `Gimme`.  For example, lifting
`Int <~< Any`, applying to `GimmeI`, and matching, gives us
`Any ~ Int`, which is nonsense.

We can reason about this type equality as a value member of `GimmeI`
of type `Leibniz[⊥, ⊤, A, Int]`, which places `A` in a
representationally invariant position.

Some other GADTs invalidate covariance. For example:

```scala
sealed trait P[A]
case class PP[A, B](a: P[A], b: P[B]) extends P[(A, B)]
```

The pattern match of a `P[A]` to `PP[_,_]` can theoretically determine
`A ~ (x, y) forSome {type x; type y}`, so `Liskov` cannot be lifted
into `P`.

However, not all GADTs invalidate `Liskov`-lifting:

```scala
sealed trait AM[A]
case class FAM[A, B](fa: AM[A], f: A => B) extends AM[B]
```

Matching `AM[A]` to `FAM[_,_]` reveals nothing about `A`; its use of
GADTs only introduces a new existential unrelated to `A`.  Considering
only `B`, as the `A` parameter is called in `FAM`, its covariance is
sound in `FAM`, so `Liskov`s can be lifted into `AM`.

Contravariance
--------------

`Liskov`s can also be lifted into parametrically sound contravariant
positions.  This looks a bit like:

```scala
def liftCtf[F[_]: Contravariant, A, B](a: A <~< B): F[B] <~< F[A]
```

Analysis of parametrically sound contravariance is essentially the
same as that for covariance.  The only difference is that, for `F[A]`,
`A` can *only* appear in the primitive contravariant position: the
function parameter type.

With regard to recursion, the “flipping” behavior of
Scala-contravariance applies.  For example, this data type is soundly
contravariant over `A`:

```scala
final case class IOf[A](f: IList[A] => Unit)
```

`IList` is soundly covariant over `A`, and `IList[A]` appears in
soundly contravariant position, making `A` contravariant.  Meanwhile,
`A` is soundly *co*variant in this data type built upon `IOf`:

```scala
final case class IOf2[A](f: IOf[IOf[A]])
```

Some surprises
--------------

Despite the unsoundness of `Liskov`-lifting into `Gimme` earlier, it
may seem surprising that Scala allows:

```scala
sealed trait GimmeC[+A]
case object GimmeCI extends Gimme[Int]
```

Moreover, this isn't a bug; it's perfectly sound.  That is because,
while matching `GimmeI` causes Scala to infer `A ~ Int`, it won't do
that for `GimmeCI`!  Scala can soundly determine that `A ⊇ Int` when
it matches `GimmeCI`, but I do not think it even goes so far as to do
that as of this writing.  We can't blame Scala for this difference;
Scala has declared up front that its type system encodes what it
believes, and is *our* responsibility to follow the cardinal rule of
not violating its assumptions if we lift `Liskov` into `Gimme`.

As stated earlier, `Liskov` cannot be lifted into
`collection.immutable.Set`; `TreeSet` exists to trivially demonstrate
the problem, but even if `TreeSet` was not there, we would not be able
to honestly do it because `c.i.Set` is open to new subclasses that
could perform similar violations.  However, despite lacking a
`Functor`, `scalaz.ISet` *does* allow `Liskov`-lifting.
[Do the ADT analysis yourself, if you like.](https://github.com/scalaz/scalaz/blob/ac8c4684ef89f1b950e71237819d78f573e552ea/core/src/main/scala/scalaz/ISet.scala#L552-L561)
Well, so, once you convert your `ISet[Int]` to `ISet[Any]`, you can't
do many operations on it, but that's neither here nor there.

Should this function exist?
---------------------------

The Scalaz community has settled on a definition of
covariant-functoriality that conforms with the principle of parametric
soundness. The rejection of `Functor` instances
[for the `scala.collection.*` classes](https://github.com/scalaz/scalaz/pull/307),
which have subclasses with mutable values over their parameters, and
[for `collection.immutable.Set`](https://github.com/scalaz/scalaz/pull/276),
which has the `TreeSet` case stated above and violates parametricity
in the construction of `HashSet`s, speak to this. As far as I know,
Scalaz contains no `Functor`s that are both Scala-invariant and
violate the rules delineated above.

So how do you feel about the provision of a combinator of the type of
`liftCvf` for Scalaz's `Functor`?
