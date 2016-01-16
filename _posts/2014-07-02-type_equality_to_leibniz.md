---
layout: post
title: A function from type equality to Leibniz

meta:
  nav: blog
  author: S11001001
  pygments: true
---

The Scala standard library provides evidence of two types being equal
at the data level: a value of type
[`(A =:= B)`](http://www.scala-lang.org/api/2.11.1/scala/Predef$$$eq$colon$eq.html)
witnesses that `A` and `B` are the same type. Accordingly, it provides
an implicit conversion from `A` to `B`. So you can write `Int`-summing
functions on your generic foldable types.

```scala
final case class XList[A](xs: List[A]) {
  def sum(implicit ev: A =:= Int): Int =
    xs.foldLeft(0)(_ + _)
}
```

That works because `ev` is inserted as an implicit conversion over
that lambda's second parameter.

Fragility
---------

That's not really what we want, though. In particular, flipping `A`
and `Int` in the `ev` type declaration will break it:

```
….scala:5: overloaded method value + with alternatives:
  (x: Int)Int <and>
  (x: Char)Int <and>
  (x: Short)Int <and>
  (x: Byte)Int
 cannot be applied to (A)
    xs.foldLeft(0)(_ + _)
                     ^
```

That doesn't make sense, though. Type equality is symmetric: Scala
knows it goes both ways, so why is this finicky?

Additionally, we apply the conversion for each `Int`. It is a logical
implication that, if `A` is `B`, then `List[A]` must be `List[B]` as
well. But we can't get that cheap, single conversion without a cast.

Substitution
------------

Scalaz instead provides
[`Leibniz`](http://docs.typelevel.org/api/scalaz/stable/7.0.4/doc/scalaz/Leibniz.html),
a more perfect type equality. A simplified version follows, which we
will use for the remainder.

```scala
sealed abstract class Leib[A, B] {
  def subst[F[_]](fa: F[A]): F[B]
}
```

This reads “`Leib[A, B]` can replace `A` with `B` in **any** type
function”. That “any” is pretty important: it gives us both the
theorem that we want, and a tremendous consequent power that gives us
most of what we can get in Scala from value-level type equality, by
choosing the right `F` type parameter to `subst`.

What could it be?
-----------------

Following the Scalazzi rules, where no `null`, type testing or
casting, or `AnyRef`-defined functions are permitted, what might go in
the body of that function? Even if you know what `A` is, as a `Leib`
implementer, it's hidden behind the unknown `F`. Even if you know that
`B` is a supertype of `A`, you don't know that `F` is covariant,
[by scalac or otherwise]({% post_url 2014-03-09-liskov_lifting %}).
Even if you know that `A` is `Int` and `B` is `Double`, what are you
going to do with that information?

So there's only one thing this `Leib` could be, because you **do**
have an `F` of *something*.

```scala
implicit def refl[A]: Leib[A, A] = new Leib[A, A] {
  override def subst[F[_]](fa: F[A]): F[A] = fa
}
```

Every type is equal to itself. Every well-formed `Leib` instance
starts out this way, in this function.

Recovery
--------

So, it's great that *we* know the implication of the `subst` method's
generality. But that's not good enough; we had that with `=:=`
already. We want to write well-typed operations that represent all the
implications of the `Leib` type equality as *new* `Leib`s representing
*those* type equalities.

First, let's solve the original problem, using infix type application
to show the similarity to `=:=`:

```scala
def sum2(implicit ev: A Leib Int): Int =
  ev.subst[List](xs).foldLeft(0)(_ + _)
```

There is no more implicit conversion, the result of `subst` is the same
object as the argument, and `[List]` would be inferred, but I have
merely specified it for clarity in this example.

This doesn't compose, though. What if, having `subst`ed `Int` into
that `List` type, I now want to `subst` `List[A]` for `List[Int]` in
some type function? Specifically, what about a `Leib` that represents
that type equality? To handle that, we can `subst` into `Leib` itself!

```scala
def lift[F[_], A, B](ab: Leib[A, B]): Leib[F[A], F[B]] =
  ab.subst[Lambda[X => Leib[F[A], F[X]]]](Leib.refl[F[A]])
```

Again, the final `[F[A]]` could be inferred.

As an exercise, define the `symm` and `compose` operations, which
represent that `Leib` is symmetric and transitive as well. Hints: the
`symm` body is the same except for the type parameters given, and
`compose` doesn't use `refl`.

```scala
def symm[A, B](ab: Leib[A, B]): Leib[B, A]
def compose[A, B, C](ab: Leib[A, B], bc: Leib[B, C]): Leib[A, C]
```

Leib power
----------

In Scalaz, `Leibniz` is already defined, and
[used in a few places](https://github.com/scalaz/scalaz/blob/v7.0.6/core/src/main/scala/scalaz/syntax/TraverseSyntax.scala#L22-L26).
Though their `subst` definitions are completely incompatible at the
scalac level, they have a weird equivalence due to the awesome power
of `subst`.

```scala
import scalaz.Leibniz, Leibniz.===

def toScalaz[A, B](ab: A Leib B): A === B =
  ab.subst[A === ?](Leibniz.refl)

def toLeib[A, B](ab: A === B): A Leib B =
  ab.subst[A Leib ?](Leib.refl)
```

…where `?` is to type-lambdas as `_` is to Scala lambdas, thanks to
[the Kind Projector plugin](https://github.com/non/kind-projector#kind-projector).

And so it would be with any pair of `Leibniz` representations with such
`subst` methods that you might define. Unfortunately, `=:=` cannot
participate in this universe of isomorphisms; it lacks the `subst`
method that serves as the `Leibniz` certificate of authenticity. You can
get a `=:=` from a `Leibniz`, but not vice versa.

Why would you want that weak sauce anyway?

Looking up
----------

These are just the basics.  Above:

* The weakness of Scala's own `=:=`,
* the sole primitive `Leibniz` operator `subst`,
* how to logically derive other type equalities,
* the isomorphism between each `Leibniz` representation and all
  others.

[In the next part]({% post_url 2014-09-20-higher_leibniz %}), we'll
look at:

* Why it matters that `subst` always executes to use a type equality,
* the Haskell implementation,
* higher-kinded type equalities and their `Leibniz`es,
* why
  [the `=:=` singleton trick](https://github.com/scala/scala/blob/v2.11.1/src/library/scala/Predef.scala#L399-L402)
  is unsafe,
* simulating GADTs with `Leibniz` members of data constructors.

*This article was tested with Scala 2.11.1, Scalaz 7.0.6, and Kind
Projector 0.5.2.*
