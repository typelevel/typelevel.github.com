---
layout: post
title: When implicitly isn't specific enough

meta:
  nav: blog
  author: S11001001
  pygments: true
---

When working with implicit-encoded dependent function types, such as
`scalaz.Unapply` and numerous Shapeless operations, you'd frequently
like to acquire instances of those functions to see what types get
calculated for them.

For example, `++` on Shapeless `HList`s is driven by `Prepend`:

```scala
def ++[S <: HList](suffix : S)(implicit prepend : Prepend[L, S])
  : prepend.Out = prepend(l, suffix)
```

So given some `HList`s, we can expect to be able to combine them in a
couple ways.  First, by using the syntax function above, and then by
acquiring a value of `prepend`'s type directly and invoking it, just
as in the body of the above function.

```scala
import shapeless._, ops.hlist._
import scalaz._, std.string._, std.tuple._, syntax.applicative._

scala> val ohi = 1 :: "hi" :: HNil
ohi: shapeless.::[Int,shapeless.::[String,shapeless.HNil]]
        = 1 :: hi :: HNil

scala> ohi ++ ohi
res0: shapeless.::[Int,shapeless.::[String,shapeless.::[Int,shapeless.::[String,shapeless.HNil]]]] = 1 :: hi :: 1 :: hi :: HNil

scala> val ohipohi = implicitly[Prepend[String :: Int :: HNil, String :: Int :: HNil]]
ohipohi: shapeless.ops.hlist.Prepend[
           shapeless.::[String,shapeless.::[Int,shapeless.HNil]],
           shapeless.::[String,shapeless.::[Int,shapeless.HNil]]]
  = shapeless.ops.hlist$Prepend$$anon$58@13399e98

scala> ohipohi(ohi, ohi)
res3: ohipohi.Out = 1 :: hi :: 1 :: hi :: HNil
```

Back over in Scalaz, for purposes of an `Applicative` instance,
`(String, Int)` selects its second type parameter.  Just as the
`To*OpsUnapply` functions acquire `Unapply` instances to do their
work:

```scala
implicit def ToApplicativeOpsUnapply[FA](v: FA)(implicit F0: Unapply[Applicative, FA]) =
  new ApplicativeOps[F0.M,F0.A](F0(v))(F0.TC)
```

We can acquire an instance and use it.

```scala
scala> val t2ap = implicitly[Unapply[Applicative, (String, Int)]]
t2ap: scalaz.Unapply[scalaz.Applicative,(String, Int)] =
scalaz.Unapply_0$$anon$13@18214797

scala> t2ap.TC.point(42)
res5: t2ap.M[Int] = ("",42)
```

The mysterious result
---------------------

Now let's get that first element out of that tuple we got by calling
`point`.

```scala
scala> res5._1
<console>:31: error: value _1 is not a member of t2ap.M[Int]
              res5._1
                   ^
```

Uh, huh?  Let's try adding the `HList`s we got from `ohipohi` before.

```scala
cala> res3 ++ res3
<console>:32: error: could not find implicit value for parameter
              prepend: shapeless.ops.hlist.Prepend[ohipohi.Out,ohipohi.Out]
              res3 ++ res3
                   ^
```

The clue is in the type report in the above: path-dependent type
members of `t2ap` and `ohipohi` appear.  That wouldn't be a problem,
normally, as we know what they are, but **they're existential** to
Scala.

```scala
scala> implicitly[t2ap.M[Int] =:= (String, Int)]
<console>:30: error: Cannot prove that t2ap.M[Int] =:= (String, Int).
              implicitly[t2ap.M[Int] =:= (String, Int)]
                        ^
```

`implicitly` only gives what you ask for
----------------------------------------

The explanation lies with the `implicitly` calls we made to acquire
the specific dependent functions we wanted to use.  Let's look at the
definition of `implicitly` and see if it can enlighten:

```scala
def implicitly[T](implicit e: T): T
```

In other words, `implicitly` returns exactly what you asked for,
type-wise.  Recall the inferred type of `ohipohi` when it was defined:

```scala
ohipohi: shapeless.ops.hlist.Prepend[
           shapeless.::[String,shapeless.::[Int,shapeless.HNil]],
           shapeless.::[String,shapeless.::[Int,shapeless.HNil]]]
```

Not coincidentally, *this is the exact type we gave as a type
parameter to `implicitly`*.  What's important is that `Out`, the type
member of `Prepend` that determines its result type, is existential in
both cases.

In other words, the rule of `implicitly` is “you asked for it, you got
it”.

A more specific `implicitly`
----------------------------

The answer here is to simulate the weird way in which dependent method
types, like `++` and `ToApplicativeOpsUnapply`, can pass through extra
type information about their implicit parameters that would otherwise
be lost.  We do this by reinventing `implicitly`.

The first try is obvious: follow the comment in the `Predef.scala`
source and give `implicitly` a singleton type result.

```scala
def implicitly2[T <: AnyRef](implicit e: T): T with e.type = e

scala> val ohipohi2 = implicitly2[Prepend[Int :: String :: HNil, Int :: String :: HNil]]
ohipohi2: shapeless.ops.hlist.Prepend[
              shapeless.::[Int,shapeless.::[String,shapeless.HNil]],
              shapeless.::[Int,shapeless.::[String,shapeless.HNil]]]
     with e.type = shapeless.ops.hlist$Prepend$$anon$58@4abe65da

scala> ohipohi2(ohi, ohi)
res9: ohipohi2.Out = 1 :: hi :: 1 :: hi :: HNil

scala> res9 ++ res9
<console>:33: error: could not find implicit value for parameter
              prepend: shapeless.ops.hlist.Prepend[ohipohi2.Out,ohipohi2.Out]
              res9 ++ res9
                   ^
```

Not quite good enough.

An even more, albeit less, specific `implicitly`
------------------------------------------------

I think it's strange that the above doesn't work, but we can deal with
it by being a little more specific.

```scala
def implicitlyDepFn[T <: DepFn2[_,_]](implicit e: T)
    : T {type Out = e.Out} = e

scala> val ohipohi3 = implicitlyDepFn[Prepend[Int :: String :: HNil, Int :: String :: HNil]]
ohipohi3: shapeless.ops.hlist.Prepend[
              shapeless.::[Int,shapeless.::[String,shapeless.HNil]],
              shapeless.::[Int,shapeless.::[String,shapeless.HNil]]]{
                type Out = shapeless.::[Int,shapeless.::[String,
                            shapeless.::[Int,shapeless.::[String,shapeless.HNil]]]]
          } = shapeless.ops.hlist$Prepend$$anon$58@7306572f

scala> ohipohi3(ohi, ohi)
res11: ohipohi3.Out = 1 :: hi :: 1 :: hi :: HNil

scala> res11 ++ res11
res12: shapeless.::[Int,shapeless.::[String,shapeless.::[Int,shapeless.::[String,
       shapeless.::[Int,shapeless.::[String,shapeless.::[Int,shapeless.::[String,
       shapeless.HNil]]]]]]]]
   = 1 :: hi :: 1 :: hi :: 1 :: hi :: 1 :: hi :: HNil
```

Now that's more like it.  The trick is in the return type of
`implicitlyDepFn`, which includes the structural refinement `{type Out
= e.Out}`.

Again, it's weird that this structural refinement isn't subsumed by
the return type `e.type` from `implicitly2`'s definition, but I'm not
sure it's wrong, either, given the ephemeral nature of type stability.

Thankfully, most of the evidence for dependent function types in
Shapeless extends from the `DepFn*` traits, so you only need one of
these special `implicitly` variants for each, rather than one for each
individual dependent function type you wish to acquire instances of in
this way.

And likewise with `Unapply`
---------------------------

We can similarly acquire instances of `scalaz.Unapply` conveniently.
I believe this function will be supplied with Scalaz 7.0.6, and it is
[already included in the 7.1 development branch](https://github.com/scalaz/scalaz/pull/621),
so you will be able to write `Unapply[TC, type]` to get instances as
with plain typeclass lookup in Scalaz, but it's easy enough to define
yourself.

```scala
def unap[TC[_[_]], MA](implicit U: Unapply[TC, MA]): U.type {
  type M[A] = U.M[A]
  type A = U.A
} = U

scala> val t2ap2 = unap[Applicative, (String, Int)]
t2ap2: U.type{type M[A] = (String, A); type A = Int} 
  = scalaz.Unapply_0$$anon$13@3adb9933

scala> t2ap2.TC.point(42)
res13: (String, Int) = ("",42)

scala> res13._1
res14: String = ""
```

*This article was tested with Scala 2.10.3, Scalaz 7.0.5, and
Shapeless 2.0.0-M1.*
