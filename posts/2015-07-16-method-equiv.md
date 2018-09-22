---
layout: post
title: When are two methods alike?

meta:
  nav: blog
  author: S11001001
  pygments: true
  mathjax: true
---

*This is the second of a series of articles on “Type Parameters and
Type Members”.  If you haven’t yet, you should
[start at the beginning]({% post_url 2015-07-13-type-members-parameters %}),
which introduces code we refer to throughout this article without
further ado.*

[In the last part]({% post_url 2015-07-13-type-members-parameters %}#when-is-existential-ok),
we just saw two method types that, though different, are effectively
the same: those of `plengthT` and `plengthE`.  We have rules for
deciding when an existential parameter can be lifted into a method
type parameter—or a method type parameter lowered to an
existential—but there are other pairs of method types I want to
explore that are the same, or very close.  So let’s talk about how we
determine this equivalence.

A method *R* is more general than or as general as *Q* if *Q* may be
implemented by only making a call to *R*, passing along the arguments.
By more general, we mean *R* can be invoked in all the situations that
*Q* can be invoked in, and more besides.  Let us call the result of
this test $R <:\_m Q$ (where $<:\_m$ is pronounced “party duck”); if
the test of *Q* making a call to *R* fails, then $\neg(R <:\_m Q)$.

If $Q <:\_m R$ and $R <:\_m Q$, then the two method types are
*equivalent*; that is, neither has more expressive power than the
other, since each can be implemented merely by invoking the other and
doing nothing else.  We write this as $Q \equiv\_m R$.  Likewise, if
$R <:\_m Q$ and $\neg(Q <:\_m R)$, that is, *Q* can be written by
calling *R*, but not vice versa, then *R* is *strictly more general*
than *Q*, or $R <\_m Q$.

What the concrete method—the one actually doing stuff, not invoking
the other one—does is irrelevant, for the purposes of this test,
because this is about types.  That matters because sometimes, in
Scala, as in Java, the body will compile in one of the methods, but
not the other.  Let’s see an example that doesn’t compile.

```scala
import scala.collection.mutable.ArrayBuffer

def copyToZero(xs: ArrayBuffer[_]): Unit =
  xs += xs(0)

TmTp2.scala:9: type mismatch;
 found   : (some other)_$1(in value xs)
 required: _$1(in value xs)
    xs += xs(0)
            ^
```

Likewise, the Java version has a similar problem, though the error
message doesn’t give as good a hint as to what’s going on.

```java
import java.util.List;

void copyToZero(final List<?> xs) {
    xs.add(xs.get(0));
}

TmTp2.java:11:  error: no suitable method found for add(CAP#1)
        xs.add(xs.get(0));
          ^
```

Luckily, in both Java and Scala, we have an *equivalent* method type,
from lifting the existential (misleadingly called *wildcard* in Java
terminology) to a method type parameter.

We can apply this transformation to put the method implementation
somewhere it will compile.

```scala
def copyToZeroE(xs: ArrayBuffer[_]): Unit =
  copyToZeroP(xs)

private def copyToZeroP[T](xs: ArrayBuffer[T]): Unit =
  xs += xs(0)
```

Similarly, in Java,

```java
void copyToZeroE(final List<?> xs) {
    copyToZeroP(xs);
}

<T> void copyToZeroP(final List<T> xs) {
    final T zv = xs.get(0);
    xs.add(zv);
}
```

The last gives a hint as to what’s going on, both here and in the
compiler errors above: in `copyToZeroP`’s body, the list element type
has a name, `T`; we can use the name to create variables, and the
compiler can rely on the name as well.  The compiler, ideally,
shouldn’t care about whether the name can be written, but that one of
the above compiles and the other doesn’t is telling.

If you were to define a variable to hold the result of getting the
first element in the list in either version of `copyToZeroE`, how
would you do that?  In Java, the reason this doesn’t work is
straightforward: you would have to declare the variable to be of type
`Object`, but that type isn’t specific enough to allow the variable to
be used as an argument to `xs.add`.

Scala’s type-inferred variables don’t help here; Scala considers the
existential type to be scoped to `xs`, and makes the definition of
`zv` independent of `xs` by breaking the type relationship, and
crushing the inferred type of `zv` to `Any`.

```scala
def copyToZeroE(xs: ArrayBuffer[_]): Unit = {
  val zv = xs(0)
  xs += zv
}

TmTp2.scala:19: type mismatch;
 found   : zv.type (with underlying type Any)
 required: _$1
    xs += zv
          ^
```

When we call the type-parameterized variant to implement the
existential variant, with the real implementation residing in the
former, we are just helping the compiler along by using the equivalent
method type; in the simpler case of the former, both `scalac` and
`javac` manage to infer that the type `T` should be the (otherwise
unspeakable) existential.  **Method equivalence and generality make it
possible to write methods, safely, that could not be written
directly.**

Why are existentials harder to think about?
-------------------------------------------

I think we, as humans, may have even more difficulty with the lack of
names for existentials than the compilers do.  The name “unspeakable”,
which I have borrowed from Jon Skeet’s *C# in Depth*, is telling: even
in our heads, our thought processes are shaped by language.  We tame
the mathematics of programming with symbols, with names.  Existentials
and their “unspeakable” names rob us of the tools to talk about them,
to think about them.

Java has done its practitioners two great disservices here.  One: by
calling its existentials “wildcards”.  They are not “wildcards”, in
any commonly or uncommonly understood sense.  If you suppose your
preexisting notions of “wildcards” to apply to these much more exotic
creatures, you will confidently stroll into the darkness until you
trip and fall off a cliff.  They are only *superficially* “wildcards”.
The effect of this sorry attempt at avoiding new terminology is
chiefly to cheat Java programmers out of learning what’s really going
on.  (We will explore some of this more exotic behavior
[in a later post]({% post_url 2015-07-27-nested-existentials %}).)

Two: by
[encouraging use of existential signatures](https://docs.oracle.com/javase/tutorial/extra/generics/methods.html)
like `mdropFirstE` over parameterized versions like `mdropFirstT` that
do not require the same kind of mental gymnastics.

For lifting these type parameters is how we can reclaim the power we
lost in the debacle of the unspeakable names.  We name them, and in so
doing can once more talk and think about them without exhausting
ourselves by gesticulating wildly, comforting ourselves with
fairytales of “wildcards”.  Because in parameter lifting, we have
found a *true* analogy.

When are two methods less alike?
--------------------------------

Now, let’s examine another pair of methods, and apply our test to
them.

Let’s say we want to write the equivalent of this method for `MList`.

```scala
def pdropFirst[T](xs: PList[T]): PList[T] =
  xs match {
    case PNil() => PNil()
    case PCons(_, t) => t
  }
```

According to the `PList` ⇔ `MList` conversion rules given
[in the previous article]({% post_url 2015-07-13-type-members-parameters %}#when-is-existential-ok),
section “Why all the `{type T = ...}`?”, the equivalent for `MList`
should be

```scala
def mdropFirstT[T0](xs: MList {type T = T0})
  : MList {type T = T0} =
  xs.uncons match {
    case None => MNil()
    case Some(c) => c.tail
  }
```

Let us try to drop the refinements.  That seems to compile:

```scala
def mdropFirstE(xs: MList): MList =
  xs.uncons match {
    case None => MNil()
    case Some(c) => c.tail
  }
```

It certainly looks nicer.  However, while `mdropFirstE` can be
implemented by calling `mdropFirstT`, passing the type parameter
`xs.T`, the opposite is not true; `mdropFirstT` $<\_m$ `mdropFirstE`,
or, `mdropFirstT` is *strictly more general*.

In this case, the reason is that `mdropFirstE` fails to relate the
argument’s `T` to the result’s `T`; you could implement `mdropFirstE`
as follows:

```scala
def mdropFirstE[T0](xs: MList): MList =
  MCons[Int](42, MNil())
```

The stronger type of `mdropFirstT` forbids such shenanigans.  However,
I can just tell you that largely because I’m already comfortable with
existentials; how could you figure that out if you’re just starting
out with these tools?  You don’t have to; the beauty of the
equivalence test is that you can apply it mechanically.  **Knowing
nothing about the mechanics of the parameterization and existentialism
of the types involved, you can work out with the equivalence test**
that `mdropFirstT` $<\_m$ `mdropFirstE`, and therefore, that you can’t
get away with simply dropping the refinements.

Method likeness and subtyping, all alike
----------------------------------------

If you know what the symbol `<:` means in Scala, or perhaps you’ve
read
[SLS §3.5 “Relations between types”](http://www.scala-lang.org/files/archive/spec/2.11/03-types.html#relations-between-types),
you might think, “gosh, method equivalence and generality look awfully
familiar.”

Indeed, the thing we’re talking about is very much like subtyping and
type equality!  In fact, every type-equal pair of methods *M*₁ and
*M*₂ also pass our method equivalence test, and every pair of methods
*M*₃ and *M*₄ where $M\_3 <: M\_4$ passes our *M*₄-calls-*M*₃ test.
So $M\_1 \equiv M\_2$ implies $M\_1 \equiv\_m M\_2$, and
$M\_3 <: M\_4$ implies $M\_3 <:\_m M\_4$.

We even follow many of the same rules as the type relations.  We have
transitivity: if *M*₁ can call *M*₂ to implement itself, and *M*₂ can
call *M*₃ to implement itself, obviously we can snap the pointer and
have *M*₁ call *M*₃ directly.  Likewise, every method type is
equivalent to itself: reflexivity.  Likewise, if a method *M*₁ is
strictly more general than *M*₂, obviously *M*₂ cannot be strictly
more general than *M*₁: antisymmetricity.  And we even copy the
relationship between ≡ and <: themselves: just as $T\_1 \equiv T\_2$
implies $T\_1 <: T\_2$, so $R \equiv\_m Q$ implies $R <:\_m Q$.

Scala doesn’t understand the notion of method equivalence we’ve
defined above, though.  So you can’t, say, implement an abstract
method in a subclass using an equivalent or more general form, at
least directly; you have to `override` the Scala way, and call the
alternative form yourself, if that’s what you want.

I do confess to one oddity in my terminology: **the method that has
more specific type is *the more general method*.** I hope the example
of `mdropFirstT` $<:\_m$ `mdropFirstE` justifies my choice.
`mdropFirstT` has more specific type, and rejects more
implementations, such as the one that returns a list with `42` in it
above.  Thus, it has fewer implementations, in the same way that more
specific types have fewer values inhabiting them.  But it can be used
in more circumstances, so it is “more general”.  The generality in
terms of when a method can be used is directly proportional to the
specificity of its type.

Java’s edge of insanity
-----------------------

Now we have enough power to demonstrate that Scala’s integration with
Java generics is faulty.  Or, more fairly, that Java’s generics are
faulty.

Consider this method type, in Scala:

```scala
def goshWhatIsThis[T](t: T): T
```

This is a pretty specific method type; there are not too many
implementations.  Of course you can always perform a side effect; we
don’t track that in Scala’s type system.  But what can it return?
Just `t`.

Specifically, you can’t return `null`:

```scala
TmTp2.scala:36: type mismatch;
 found   : Null(null)
 required: T
  def goshWhatIsThis[T](t: T): T = null
                                   ^
```

Well now, let’s convert this type to Java:

```java
public static <T> T holdOnNow(T t) {
    return null;
}
```

We got away with that!  And, indeed, we can call `holdOnNow` to
implement `goshWhatIsThis`, and vice versa; they’re *equivalent*.  But
the type says we can’t return `null`!

The problem is that Java adds an implicit upper bound, because it
assumes generic type parameters can only have class types chosen for
them; in Scala terms, `[T <: AnyRef]`.  If we encode this constraint
in Scala, Scala gives us the correct error.

```scala
def holdOnNow[T <: AnyRef](t: T): T = TmTp2.holdOnNow(t)

def goshWhatIsThis[T](t: T): T = holdOnNow(t)

TmTp2.scala:38: inferred type arguments [T] do not conform
⤹ to method holdOnNow's type parameter bounds [T <: AnyRef]
  def goshWhatIsThis[T](t: T): T = holdOnNow(t)
                                   ^
```

This is forgivable on Scala’s part, because it’d be annoying to add
`<: AnyRef` to your generic methods just because you called some Java
code and it’s probably going to work out fine.  I blame `null`, and
while I’m at it, I blame `Object` having any methods at all, too.
We’d be better off without these bad features.

In
[the next part, “What happens when I forget a refinement?”]({% post_url 2015-07-19-forget-refinement-aux %}),
we’ll talk about what happens when you forget refinements for things
like `MList`, and how you can avoid that while simplifying your
type-member-binding code.

*This article was tested with Scala 2.11.7 and Java 1.8.0_45.*
