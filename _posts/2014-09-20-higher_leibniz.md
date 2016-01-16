---
layout: post
title: Higher Leibniz

meta:
  nav: blog
  author: S11001001
  pygments: true
---

We’ve previously seen
[the basic implementation and motivation for `scalaz.Leibniz`]({% post_url 2014-07-02-type_equality_to_leibniz %}).
But there’s still quite a bit more to this traditionally esoteric
member of the Scalaz collection of well-typed stuff.

Strictly necessarily strict
---------------------------

The word “witness” implies that `Leibniz` is a passive bystander in
your function; sitting back and telling you that some type is equal to
another type, otherwise content to let the *real* code do the real
work.  The fact that `Leibniz` lifts into functions (which are a
member of the *everything* set, you’ll agree) might reinforce the
notion that `Leibniz` is spooky action at a distance.

But one of the nice things about `Leibniz` is that there’s really no
cheating: the value with its shiny new type is dependent on the
`Leibniz` actually existing, and its `subst`, however much a glorified
identity function it might be, completing successfully.

To see this in action, let’s check in with the bastion of not
evaluating stuff, Haskell.

The Haskell implementation
--------------------------

```haskell
{-# LANGUAGE RankNTypes, PolyKinds #-}
module Leib
  ( Leib()
  , subst
  , lift
  , symm
  , compose
  ) where

import Data.Functor

data Leib a b = Leib {
  subst :: forall f. f a -> f b
}

refl :: Leib a a
refl = Leib id

lift :: Leib a b -> Leib (f a) (f b)
lift ab = runOn . subst ab . On $ refl

newtype On c f a b = On {
  runOn :: c (f a) (f b)
}

symm :: Leib a b -> Leib b a
symm ab = runDual . subst ab . Dual $ refl

newtype Dual c a b = Dual {
  runDual :: c b a
}

compose :: Leib b c -> Leib a b -> Leib a c
compose = subst
```

We use [newtypes](http://www.haskell.org/haskellwiki/Newtype) in place
of type lambdas, and a value instead of a method, but the
implementation is otherwise identical.

It’s really there
-----------------

OK.  Let’s try to make a fake `Leib`.

```haskell
badForce :: Leib a b
badForce = Leib $ \_ -> error "sorry for fibbing"
```

The following code will signal an error only if forcing the *head
cons* of the `subst`ed list signals such an error.  We never give
Haskell the chance to force anything else.

```haskell
λ> subst (badForce :: Leib Int String) [42] `seq` 33
*** Exception: sorry for fibbing
```

Oh well, let’s try to bury it behind combinators.

```haskell
λ> subst (symm . symm $ badForce :: Leib Int String) [42] `seq` 33
*** Exception: sorry for fibbing
λ> subst (compose refl $ badForce :: Leib Int String) [42] `seq` 33
*** Exception: sorry for fibbing
```

Hmm.  We have two properties:

1. The `id` from `refl`?  The type-substituted data actually goes
   through that function.  The same goes for the `subst` method in
   Scala.
2. When using `Leibniz` combinators, the strictness forms a chain to
   all underlying `Leibniz` evidence.  If there are any missing
   values, the transform will also fail.

Higher kinded `Leibniz`
-----------------------

Let’s try a variant on `Leib`.

```scala
sealed abstract class LeibF[G[_], H[_]] {
  def subst[F[_[_]]](fa: F[G]): F[H]
}
```

This reads “`LeibF[G, H]` can replace `G` with `H` in **any** type
function”.  But, whereas the
[kind](https://blogs.atlassian.com/2013/09/scala-types-of-a-higher-kind/)
of the types that Leib discusses is `*`, for `LeibF` it’s `*->*`.  So,
`LeibF[List, List]` exhibits that the *type constructors* `List` and
`List` are equal.

```scala
implicit def refl[G[_]]: LeibF[G, G] = new LeibF[G, G] {
  override def subst[F[_[_]]](fa: F[G]): F[G] = fa
}
```

Interestingly, except for the kinds of type parameters, these
definitions are exactly the same as for `Leib`.  Does that hold for
lift?

```scala
def lift[F[_[_], _], A[_] , B[_]](ab: LeibF[A, B]): LeibF[F[A, ?], F[B, ?]] =
  ab.subst[Lambda[x[_] => LeibF[F[A, ?], F[x, ?]]]](LeibF.refl[F[A, ?]])
```

Despite that we are positively buried in type lambdas (yet moderated
by [Kind Projector](https://github.com/non/kind-projector)) now,
absolutely!

As an exercise, adapt your `symm` and `compose` methods from the last
part for `LeibF`, by only changing type parameters and switching any
`refl` references.

```scala
def symm[A[_], B[_]](ab: LeibF[A, B]): LeibF[B, A]
def compose[A[_], B[_], C[_]](ab: LeibF[A, B], bc: LeibF[B, C]): LeibF[A, C]
```

You can write a `Leibniz` and associated combinators for types of
*any* kind; the principles and implementation techniques outlined
above for types of kind `*->*` apply to all kinds.

Whence `PolyKinds`?
-------------------

You have to define a new `Leib` variant and set of combinators for
each kind you wish to support.  There is no need to do this in
Haskell, though.

```haskell
λ> :k Leib []
Leib [] :: (* -> *) -> *
λ> :t refl :: Leib [] []
refl :: Leib [] [] :: Leib [] []
λ> :t lift (refl :: Leib [] [])
lift (refl :: Leib [] []) :: Leib (f []) (f [])
λ> :t compose (refl :: Leib [] [])
compose (refl :: Leib [] []) :: Leib a [] -> Leib a []
```

In Haskell, we can take advantage of the fact that the actual
implementations are kind-agnostic, by having those definitions be
applicable to all kinds via
[the `PolyKinds` language extension](http://www.haskell.org/ghc/docs/7.8.3/html/users_guide/kind-polymorphism.html),
mentioned at the top of the Haskell code above.  No such luck in
Scala.

Better GADTs
------------

[In a post from a couple months ago](http://d.hatena.ne.jp/xuwei/20140706/1404612620),
Kenji Yoshida outlines an interesting way to simulate the missing
type-evidence features of Scala’s GADT support with `Leibniz`.  This
works in Haskell, too, in case you are comfortable with turning on
[`RankNTypes`](http://www.haskell.org/ghc/docs/7.8.3/html/users_guide/other-type-extensions.html#universal-quantification)
but not
[`GADTs`](http://www.haskell.org/ghc/docs/7.8.3/html/users_guide/data-type-extensions.html#gadt)
somehow.

Let’s examine Kenji’s GADT.

```scala
sealed abstract class Foo[A, B]
final case class X[A]() extends Foo[A, A]
final case class Y[A, B](a: A, b: B) extends Foo[A, B]
```

For completeness, let’s also see the Haskell version, including the
function that demands so much hoop-jumping in Scala, but just works in
Haskell.

```haskell
{-# LANGUAGE GADTs #-}
module FooXY where

data Foo a b where
  X :: Foo a a
  Y :: a -> b -> Foo a b

hoge :: Foo a b -> f a c -> f b c
hoge X bar = bar
```

Note that the Haskell type system understands that when `hoge`’s first
argument’s data constructor is `X`, the type variables `a` and `b`
must be the same type, and therefore by implication the argument of
type `f a c` must also be of type `f b c`.  This is what we’re trying
to get Scala to understand.

```scala
def hoge1[F[_, _], A, B, C](foo: Foo[A, B], bar: F[A, C]): F[B, C] =
  foo match {
    case X() => bar
  }
```

This transliteration of the above Haskell `hoge` function fails to
compile, as Kenji notes, with the following:

```scala
…/LeibnizArticle.scala:39: type mismatch;
 found   : bar.type (with underlying type F[A,C])
 required: F[B,C]
      case X() => bar
                  ^
```

The overridden `cata` method
----------------------------

Kenji introduces a `cata` method on `Foo` to constrain use of the
`Leibniz.force` hack, while still providing external code with usable
`Leibniz` evidence that can be lifted to implement `hoge`.  However,
by implementing the method in a slightly different way, we can use
`refl` instead.

```scala
sealed abstract class Foo[A, B] {
  def cata[Z](x: (A Leib B) => Z, y: (A, B) => Z): Z
}

final case class X[A]() extends Foo[A, A] {
  def cata[Z](x: (A Leib A) => Z, y: (A, A) => Z) =
    x(Leib.refl)
}

final case class Y[A, B](a: A, b: B) extends Foo[A, B] {
  def cata[Z](x: (A Leib B) => Z, y: (A, B) => Z) =
    y(a, b)
}
```

Now we can replace the pattern match (and all other such pattern
matches) with an equivalent `cata` invocation.

```scala
def hoge2[F[_, _], A, B, C](foo: Foo[A, B], bar: F[A, C]): F[B, C] =
  foo.cata(x => x.subst[F[?, C]](bar),
           (_, _) => sys error "nonexhaustive")
```

So why can we get away with `Leib.refl`, whereas the function version
Kenji presents cannot?  Compare the `cata` signature in `Foo` versus
`X`:

```scala
  def cata[Z](x: (A Leib B) => Z, y: (A, B) => Z): Z
  def cata[Z](x: (A Leib A) => Z, y: (A, A) => Z): Z
```

We supplied `A` for both the `A` and `B` type parameters in our
`extends` clause, so that substitution also applies in all methods
from `Foo` that we’re implementing, including `cata`.  At that point
it’s obvious to the compiler that `refl` implements the requested
`Leib`.

Incidentally, a similar style of substitution underlies the definition
of `refl`.

The `Leib` member
-----------------

What if we don’t want to write or maintain an overriding-style `cata`?
After all, that’s an n² commitment.  Instead, we can incorporate a
`Leib` value in the GADT.  First, let’s see what the equivalent
Haskell is, without the `GADTs` extension:

```haskell
data Foo a b = X (Leib a b) | Y a b

hoge :: Foo a b -> f a c -> f b c
hoge (X leib) bar = runDual . subst leib . Dual $ bar
```

We needed `RankNTypes` to implement `Leib`, of course, but perhaps
that’s acceptable.  It’s useful in
[Ermine](https://ermine-language.github.io/), which supports rank-N
types but not GADTs as of this writing.

The above is simple enough to port to Scala, though.

```scala
sealed abstract class Foo[A, B]
final case class X[A, B](leib: Leib[A, B]) extends Foo[A, B]
final case class Y[A, B](a: A, b: B) extends Foo[A, B]

def hoge3[F[_, _], A, B, C](foo: Foo[A, B], bar: F[A, C]): F[B, C] =
  foo match {
    case X(leib) => leib.subst[F[?, C]](bar)
  }
```

It feels a little weird that `X` now must retain `Foo`’s
type-system-level separation of the two type parameters.  But this
style may more naturally integrate in your ADTs, and it is much closer
to the original non-working `hoge1` implementation.

It also feels a little weird that you have to waste a slot carting
around this evidence of type equality.  As demonstrated in section
“It’s really there” above, though, *it matters that the instance
exists*.

You can play games with this definition to make it easier to supply
the wholly mechanical `leib` argument to `X`, e.g. adding it as an
`implicit val` in the second parameter list so it can be imported and
implicitly supplied on `X` construction.  The basic technique is
exactly the same as above, though.

`Leibniz` mastery
-----------------

This time we talked about

* Why it matters that `subst` always executes to use a type equality,
* the Haskell implementation,
* higher-kinded type equalities and their `Leibniz`es,
* simulating GADTs with `Leibniz` members of data constructors.

*This article was tested with Scala 2.11.2,
[Kind Projector](https://github.com/non/kind-projector) 0.5.2, and
[GHC](http://www.haskell.org/platform/) 7.8.3.*
