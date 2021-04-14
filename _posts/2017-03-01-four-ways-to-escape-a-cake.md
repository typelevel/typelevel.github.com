---
layout: post
title: Four ways to escape a cake

meta:
  nav: blog
  author: S11001001
  pygments: true

tut:
  scala: 2.12.1
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.1
---

The mixin style of importing in which classes and traits are defined
within traits, as seen in `scala.reflect.Universe`, ScalaTest, and
other Scala styles, seems to be infectious. By that, I mean once you
define something in a trait to be mixed in, to produce another
reusable module that calls that thing, you must define *another*
trait, and so must anyone using *your* module, and so on and so forth.
You effectively become “trapped in the cake”.

However, we can use type parameters that represent **singleton types**
to write functions that are polymorphic over these “cakes”, without
being defined as part of them or mixed in themselves. For example, you
can use this to write functions that operate on elements of a
reflection universe, without necessarily passing that universe around
all over the place.

Well, for the most part. Let’s see how far this goes.

## Our little universe

Let’s set aside the heavyweight real-world examples I mentioned above
in favor of a small example. Then, we should be able to explore the
possibilities in this simpler space.

```scala
final case class LittleUniverse() {
  val haystack: Haystack = Haystack()

  final case class Haystack() {
    def init: Needle = Needle()
    def iter(n: Needle): Needle = n
  }
  
  final case class Needle()
}
```

*For brevity, I’ve defined member `class`es, but this article equally
applies if you are using abstract `type`s instead, as any Functional
programmer of pure, virtuous heart ought to!*

Suppose we have a universe.

```scala
scala> val lu: LittleUniverse = LittleUniverse()
lu: LittleUniverse = LittleUniverse()
```

The thing that Scala does for us is not let `Haystack`s and `Needle` s
from one universe be confused with those from another.

```scala
val anotherU = LittleUniverse()

scala> lu.haystack.iter(anotherU.haystack.init)
<console>:14: error: type mismatch;
 found   : anotherU.Needle
 required: lu.Needle
       lu.haystack.iter(anotherU.haystack.init)
                                          ^
```

The meaning of this error is “you can’t use one universe’s `Haystack`
to `iter` a `Needle` from another universe”.

This doesn’t look very important given the above code, but it’s a
*real* boon to more complex scenarios. You can set up a lot of
interdependent abstract invariants, verify them all, and have the
whole set represented with the “index” formed by the singleton type,
here `lu.type` or `anotherU.type`.

##  Working with a universe on hand

Refactoring in macro-writing style seems to be based upon passing the
universe around everywhere. We can do that.

```scala
def twoInits(u: LittleUniverse): (u.Needle, u.Needle) =
  (u.haystack.init, u.haystack.init)
  
def stepTwice(u: LittleUniverse)(n: u.Needle): u.Needle =
  u.haystack.iter(u.haystack.iter(n))
```

The most important feature we’re reaching for with these fancy
dependent method types, and the one that we have to *keep* reaching
for if we want to write sane functions outside the cake, is
**preserving the singleton type index**.

```scala
scala> twoInits(lu)
res3: (lu.Needle, lu.Needle) = (Needle(),Needle())

scala> stepTwice(anotherU)(anotherU.haystack.init)
res4: anotherU.Needle = Needle()
```

These values are ready for continued `iter`ing, or whatever else
you’ve come up with, in the confines of their respective
universes. That’s because they’ve “remembered” where they came from.

By contrast, consider a simple replacement of the path-dependencies
with a type projection.

```scala
def brokenTwoInits(u: LittleUniverse)
    : (LittleUniverse#Needle, LittleUniverse#Needle) =
  (u.haystack.init, u.haystack.init)

scala> val bti = brokenTwoInits(lu)
bti: (LittleUniverse#Needle, LittleUniverse#Needle) = (Needle(),Needle())
```

That seems to be okay, until it’s time to actually use the result.

```scala
scala> lu.haystack.iter(bti._1)
<console>:14: error: type mismatch;
 found   : LittleUniverse#Needle
 required: lu.Needle
       lu.haystack.iter(bti._1)
                            ^
```

The return type of `brokenTwoInits` “forgot” the index, `lu.type`.

## Getting two needles without a universe

When we pass a `LittleUniverse` to the above functions, we’re also
kind of passing in a constraint on the singleton type created by the
argument variable. That’s how we know that the returned `u.Needle` is
a perfectly acceptable `lu.Needle` in the caller scope, when we pass
`lu` as the universe.

However, as the contents of a universe become more complex, there are
many more interactions that need not involve a universe at all, at
least not directly.

```scala
def twoInitsFromAHaystack[U <: LittleUniverse](
    h: U#Haystack): (U#Needle, U#Needle) =
  (h.init, h.init)
  
scala> val tifah = twoInitsFromAHaystack[lu.type](lu.haystack)
tifah: (lu.Needle, lu.Needle) = (Needle(),Needle())
```

Since we didn’t pass in `lu`, how did it know that the returned
`Needle`s were `lu.Needle`s?

1. The type of `lu.haystack` is `lu.Haystack`.
2. That type is shorthand for `lu.type#Haystack`.
3. We passed in `U = lu.type`, and our argument meets the resulting
   requirement for a `lu.type#Haystack` (after expanding `U`).
4. The type of the expression `h.init` is
   `u.Needle forSome {val u: U}`. We use an existential because the
   relevant variable (and its singleton type) is not in scope.
5. This type *widens* to `U#Needle`, satisfying the expected return
   type.

This seems like a more complicated way of doing things, but it’s very
freeing: by not being forced to *necessarily* pass the universe around
everywhere, you’ve managed to escape the cake’s clutches much more
thoroughly. You can also write syntax enrichments on various members
of the universe that don’t need to talk about the universe’s value,
just its singleton type.

Unless, you know, the index appears in contravariant position.

## Syntactic `stepTwice`

One test of how well we’ve managed to escape the cake is to be able to
write enrichments that deal with the universe. This is a little
tricky, but quite doable if you have the universe’s value.

With the advent of `implicit class`, this became a little easier to do
wrongly, but it’s a good start.

```scala
implicit class NonWorkingStepTwice(val u: LittleUniverse) {
  def stepTwiceOops(n: u.Needle): u.Needle =
    u.haystack.iter(u.haystack.iter(n))
}
```

That compiles okay, but seemingly can’t actually be used!

```scala
scala> lu stepTwiceOops lu.haystack.init
<console>:15: error: type mismatch;
 found   : lu.Needle
 required: _1.u.Needle where val _1: NonWorkingStepTwice
       lu stepTwiceOops lu.haystack.init
                                    ^
```

There’s a hint in that we had to write `val u`, not `u`, nor `private
val u`, in order for the `implicit class` itself to compile. This
signature tells us that there’s an *argument* of type
`LittleUniverse`, and a *member* `u: LittleUniverse`. However, whereas
with the function examples above, we [and the compiler] could trust
that they’re one and the same, we have no such guarantee here. So we
don’t know that an `lu.Needle` is a `u.Needle`. We didn’t get far
enough, but we don’t know that a `u.Needle` is an `lu.Needle`, either.

## Relatable variables

Instead, we have to expand a little bit, and take advantage of a very
interesting, if obscure, element of the type equivalence rules in the
Scala language.

```scala
class WorkingStepTwice[U <: LittleUniverse](val u: U) {
  def stepTwice(n: u.Needle): u.Needle =
    u.haystack.iter(u.haystack.iter(n))
}

implicit def WorkingStepTwice[U <: LittleUniverse](u: U)
    : WorkingStepTwice[u.type] =
  new WorkingStepTwice(u)
```

*Unfortunately, the ritual of expanding the `implicit class` shorthand
is absolutely necessary; the `implicit class` won’t generate the
dependent-method-typed implicit conversion we need.*

Now we can get the proof we need.

```scala
scala> lu stepTwice lu.haystack.init
res7: _1.u.Needle forSome { val _1: WorkingStepTwice[lu.type] } = Needle()

// that's a little weird, but reduces to what we need
scala> res7: lu.Needle
res8: lu.Needle = Needle()
```

How does this work?

1. Implicitly convert `lu`, giving us a `conv:
   WorkingStepTwice[lu.type]`.
2. This means that `conv.u: lu.type`, by expansion of `U`.
3. This in turn means that `conv.u.type <: lu.type`.

The next part is worth taking in two parts. It may be worth
having
[§3.5.2 “Conformance”](http://www.scala-lang.org/files/archive/spec/2.12/03-types.html#conformance) of
the language spec open for reference. First, let’s consider the return
type (a covariant position), which is simpler.

1. The return type expands to `conv.u.type#Needle`.
2. The ninth conformance bullet point tells us that the left side of a
   `#` projection is covariant, so because `conv.u.type <: lu.type`
   (see above), the return type *widens* to `lu.type#Needle`.
3. For this, `lu.Needle` is a shorthand.

It was far longer until I realized how the argument type works. You’ll
want to scroll up on the SLS a bit, to the “Equivalence” section. Keep
in mind that we are trying to widen `lu.Needle` to `conv.u.Needle`,
which is the reverse of what we did for the return type.

1. Our argument’s type expands to `lu.type#Needle`.
2. The second bullet point under “Equivalence” says that “If a path
   *p* has a singleton type *q*`.type`, then *p*`.type` ≡ *q*`.type`.”
   From this, we can derive that `conv.u.type = lu.type`. This is a
   stronger conclusion than we reached above!
3. We substitute the left side of the `#` using the equivalence,
   giving us `conv.u.type#Needle`.

I cannot characterize this feature of the type system as anything
other than “really freaky” when you first encounter it. It seems like
an odd corner case. Normally, when you write `val x: T`, then `x.type`
is a *strict* subtype of `T`, and you can count on that, but this
carves out an exception to that rule. It is sound, though, and an
absolutely essential feature!

```scala
val sameLu: lu.type = lu

scala> sameLu.haystack.iter(lu.haystack.init)
res9: sameLu.Needle = Needle()
```

Without this rule, even though we have given it the most specific type
possible, `sameLu` couldn’t be a *true* substitute for `lu` in all
scenarios. That means that in order to make use of singleton type
indices, we would be forever beholden to the *variable* we initially
stored the value in. I think this would be *extremely inconvenient*,
structurally, in almost all useful programs.

With the rule in place, we can fully relate the `lu` and `conv.u`
variables, to let us reorganize how we talk about universes and values
indexed by their singleton types in many ways.

## A pointless argument

Let’s try to hide the universe. We don’t need it, after all. We can’t
refer to `u` in the method signature anymore, so let’s try the same
conversion we used with `twoInitsFromAHaystack`. We already have the
`U` type parameter, after all.

```scala
class CleanerStepTwice[U <: LittleUniverse](private val u: U) {
  def stepTwiceLively(n: U#Needle): U#Needle =
    ???
}

implicit def CleanerStepTwice[U <: LittleUniverse](u: U)
    : CleanerStepTwice[u.type] =
  new CleanerStepTwice(u)
```

This has the proper signature, and it’s cleaner, since we don’t expose
the unused-at-runtime `u` variable anymore. We could refine a little
further, and replace it with a `U#Haystack`, just as with
`twoInitsFromAHaystack`.

This gives us the same interface, with all the index preservation we
need. Even better, it infers a nicer return type.

```scala
scala> def trial = lu stepTwiceLively lu.haystack.init
trial: lu.Needle
```

Now, let’s turn to implementation.

```scala
class OnceMoreStepTwice[U <: LittleUniverse](u: U) {
  def stepTwiceFinally(n: U#Needle): U#Needle =
    u.haystack.iter(u.haystack.iter(n))
}

<console>:18: error: type mismatch;
 found   : U#Needle
 required: OnceMoreStepTwice.this.u.Needle
           u.haystack.iter(u.haystack.iter(n))
                                           ^
```

This is the last part of the escape! If this worked, we could *fully
erase* the `LittleUniverse` from most code, relying on the pure
type-level index to prove enough of its existence! So it’s a little
frustrating that it doesn’t quite work.

Let’s break it down. First, the return type is fine.

1. Since `u: U`, `u.type <: U`. (This is true, and useful, in the
   scope of `u`, which is now invisible to the caller.)
2. `iter` returns a `u.type#Needle`.
    - Note: since `u` is not in scope for the caller, if we returned
      this as is, it would effectively widen to the existentially
      bound `u.type#Needle forSome {val u: U}`. But the same logic in
      the next step would apply to that type.
3. By the `#` left side covariance, `u.type#Needle` widens to
   `U#Needle`.

Pretty simple, by the standards of what we’ve seen so far.

## Contravariance is the root of all…

But things break down when we try to call `iter(n)`. Keep in mind that
`n: U#Needle` and the expected type is `u.Needle`. Specifically: since
we don’t know in the implementation that `U` is a singleton type, we
can’t use the “singleton type equivalence” rule on it! But suppose
that we *could*; that is, **suppose that we could constrain `U` to be
a singleton type**.

1. The argument type is `U#Needle`.
2. By singleton equivalence, since `u: U` and `u` is stable, so
   `u.type = U`.
3. By substituting the left-hand side of the `#`, we get
   `u.type#Needle`.
4. This shortens to `u.Needle`.

If we are unable to constrain `U` in this way, though, we are
restricted to places where `U` occurs in covariant position when using
cake-extracted APIs.  We can invoke functions like `init`, because
they only have the singleton index occurring in covariant position.

Invoking functions like `iter`, where the index occurs in
contravariant or invariant position, requires being able to add this
constraint, so that we can use singleton equivalence directly on the
type variable `U`.  This is quite a bit trickier.

## Extracting more types

We have the same problem with the function version.

```scala
def stepTwiceHaystack[U <: LittleUniverse](
    h: U#Haystack, n: U#Needle): U#Needle =
  h.iter(h.iter(n))

<console>:18: error: type mismatch;
 found   : U#Needle
 required: _1.Needle where val _1: U
         h.iter(h.iter(n))
                       ^
```

Let’s walk through it one more time.

1. `n: U#Needle`.
2. `h.iter` expects a `u.type#Needle` for all `val u: U`.
3. **Suppose that we constrain `U` to be a singleton type**:
    1. [The existential] `u.type = U`, by singleton equivalence.
    2. By `#` left side equivalence, `h.iter` expects a `U#Needle`.

The existential variable complicates things, but the rule is sound.

As a workaround, it is commonly suggested to extract the member types
in question into separate type variables. This works in some cases,
but let’s see how it goes in this one.

```scala
def stepTwiceExUnim[N, U <: LittleUniverse{type Needle = N}](
    h: U#Haystack, n: N): N = ???
```

This looks a lot weirder, but should be able to return the right type.

```scala
scala> def trial2 = stepTwiceExUnim[lu.Needle, lu.type](lu.haystack, lu.haystack.init)
trial2: lu.Needle
```

But this situation is complex enough for the technique to not work.

```scala
def stepTwiceEx[N, U <: LittleUniverse{type Needle = N}](
    h: U#Haystack, n: N): N =
  h.iter(h.iter(n))

<console>:18: error: type mismatch;
 found   : N
 required: _1.Needle where val _1: U
         h.iter(h.iter(n))
                       ^
```

Instead, we need to index `Haystack` *directly* with the `Needle`
type, that is, add a type parameter to `Haystack` so that its `Needle`
arguments can be talked about completely independently of the
`LittleUniverse`, and then to write `h: U#Haystack[N]`
above. Essentially, this means that any time a type talks about
another type in a `Universe`, you need another type parameter to
redeclare a little bit of the relationships between types in the
universe.

The problem with this is that we already declared those relationships
by declaring the universe! All of the non-redundant information is
represented in the singleton type index. So even where the above
type-refinement technique works (and it does in many cases), it’s
*still* redeclaring things that ought to be derivable from the “mere”
fact that `U` is a singleton type.

## The fact that it’s a singleton type

*(The following is based on enlightening commentary by Daniel Urban on
an earlier draft.)*

Let’s examine the underlying error in `stepTwiceEx` more directly.

```scala
scala> def fetchIter[U <: LittleUniverse](
    h: U#Haystack): U#Needle => U#Needle = h.iter
<console>:14: error: type mismatch;
 found   : _1.type(in method fetchIter)#Needle
             where type _1.type(in method fetchIter) <: U with Singleton
 required: _1.type(in value $anonfun)#Needle
             where type _1.type(in value $anonfun) <: U with Singleton
           h: U#Haystack): U#Needle => U#Needle = h.iter
                                                    ^
```

It’s a good thing that this doesn’t compile. If it did, we could do

```scala
fetchIter[LittleUniverse](lu.haystack)(anotherU.haystack.init)
```

Which is unsound.

[§3.2.1 “Singleton Types”](http://www.scala-lang.org/files/archive/spec/2.12/03-types.html#singleton-types) of
the specification mentions this `Singleton`, which is in a way related
to singleton types.

> A *stable type* is either a singleton type or a type which is
> declared to be a subtype of trait `scala.Singleton`.

Adding `with Singleton` to the upper bound on `U` causes `fetchIter`
to compile! This is sound, because we are protected from the above
problem with the original `fetchIter`.

```scala
def fetchIter[U <: LittleUniverse with Singleton](
    h: U#Haystack): U#Needle => U#Needle = h.iter

scala> fetchIter[lu.type](lu.haystack)
res3: lu.Needle => lu.Needle = $$Lambda$1397/1159581520@683e7892

scala> fetchIter[LittleUniverse](lu.haystack)
<console>:16: error: type arguments [LittleUniverse] do not conform
                     to method fetchIter's type parameter bounds
                     [U <: LittleUniverse with Singleton]
       fetchIter[LittleUniverse](lu.haystack)
                ^
```

Let’s walk through the logic for `fetchIter`. The expression `h.iter`
has type `u.Needle => u.Needle` for some `val u: U`, and our goal type
is `U#Needle => U#Needle`. So we have two subgoals: prove
`u.Needle <: U#Needle` for the covariant position (after `=>`), and
`U#Needle <: u.Needle` for the contravariant position (before `=>`).

First, covariant:

1. Since `u: U`, `u.type <: U`.
2. Since the left side of `#` is covariant, #1 implies
   `u.type#Needle <: U#Needle`.
3. This re-sugars to `u.Needle <: U#Needle`, which is the goal.

Secondly, contravariant. We’re going to have to make a best guess
here, because it’s not entirely clear to me what’s going on.

1. Since [existential] path `u` has a singleton type `U` (if we define
   “has a singleton type” as “having a type *X* such that
   *X*` <: Singleton`”), so `u.type = U` by the singleton equivalence.
2. Since equivalence implies conformance, according to the first
   bullet under “Conformance”, #1 implies `U <: u.type`.
3. Since the left side of `#` is covariant, #2 implies that
   `U#Needle <: u.type#Needle`.
4. This resugars to `U#Needle <: u.Needle`, which is the goal.

I don’t quite understand this, because `U` doesn’t *seem* to meet the
requirements for “singleton type”, according to the definition of
singleton types. However, I’m *fairly* sure it’s sound, since type
stability seems to be the property that lets us avoid the
universe-mixing unsoundness. Unfortunately, it only seems to work with
*existential* `val`s; we seem to be out of luck with `val`s that the
compiler can still see.

```scala
// works fine!
def stepTwiceSingly[U <: LittleUniverse with Singleton](
    h: U#Haystack, n: U#Needle): U#Needle = {
  h.iter(h.iter(n))
}

// but alas, this form doesn't
class StepTwiceSingly[U <: LittleUniverse with Singleton](u: U) {
  def stepTwiceSingly(n: U#Needle): U#Needle =
    u.haystack.iter(u.haystack.iter(n))
}

<console>:15: error: type mismatch;
 found   : U#Needle
 required: StepTwiceSingly.this.u.Needle
           u.haystack.iter(u.haystack.iter(n))
                                            ^
```

We can work around this by having the second form invoke the first
with the `Haystack`, thus “existentializing” the universe. I imagine
that *most*, albeit not all, cakes can successfully follow this
strategy.

So, finally, we’re almost out of the cake.

1. Escape covariant positions with universe variable: complete.
2. Escape contravariant/invariant positions with universe variable:
   complete.
3. Escape covariant positions with universe *singleton type*:
   complete!
4. Escape contravariant/invariant positions with universe singleton
   type: 90% there!

*This article was tested with Scala 2.12.1.*
