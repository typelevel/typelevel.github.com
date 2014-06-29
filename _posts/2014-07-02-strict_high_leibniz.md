---
layout: post
title: Strict high Leibniz

meta:
  nav: blog
  author: S11001001
  pygments: true
---

Strict high `Leibniz`
=====================

Strictly necessarily strict
---------------------------

The word "witness" implies that Leibniz is a passive bystander in your
function; sitting back and telling you that some type is equal to
another type, otherwise content to let the *real* code do the real
work. The fact that Leibniz lifts into functions (which are a member
of the *everything* set, you'll agree) might reinforce the notion that
Leibniz is spooky action at a distance.

But one of the nice things about Leibniz is that there's really no
cheating: the value with its shiny new type is dependent on the
Leibniz actually existing, and its subst, however much a glorified
identity function it might be, completing successfully.

To see this in action, let's check in with the bastion of not
evaluating stuff, Haskell.

{-# LANGUAGE RankNTypes, PolyKinds #-}

module Leib
  (
    Leib()
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

newtype On c f a b = On {runOn :: c (f a) (f b)}
