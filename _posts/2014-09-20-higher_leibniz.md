---
layout: post
title: Higher Leibniz

meta:
  nav: blog
  author: S11001001
  pygments: true
---

Higher `Leibniz`
================

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
  ab.subst[Lambda[x => LeibF[F[A, ?], F[x, ?]]]](LeibF.refl[F[A, ?]])
```

Despite that we are positively buried in type lambdas (yet moderated
by [Kind Projector](https://github.com/non/kind-projector)) now,
absolutely!

As an exercise, adapt your `symm` and `compose` methods from the last
part for `LeibF`, by only changing type parameters and switching any
`refl` references.

```scala
def symm[A[_], B[_]](ab: LeibF[A, B]): LeibF[B, A]
def compose[A[_], B[_], C[_]](ab: LeibF[A, B], bc: LeibF[B, C]): Leib[A, C]
```

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
to get Scala to do.
