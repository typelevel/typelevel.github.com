---
layout: post
title: Values never change types

meta:
  nav: blog
  author: S11001001
  pygments: true
---

*This is the sixth of a series of articles on “Type Parameters and
Type Members”.  If you haven’t yet, you should
[start at the beginning]({% post_url 2015-07-13-type-members-parameters %}),
which introduces code we refer to throughout this article without
further ado.*

In a subtyping language such as Java or Scala, first-class
existentials are a helpful ally for modeling values and methods that
abstract over subtypes in different ways.  In a language with
mutation, such as Java or Scala, existentials help deal with certain
kinds of state changes without violating type safety.

But values, *themselves*, never change types.  When you first practice
with existentials, the Java and Scala compilers seem to become
veritable minefields of type errors—do something slightly different in
your code, and everything falls apart.  But this is just about
[nothing being a free lunch](http://blog.higher-order.com/blog/2014/12/21/maximally-powerful/):
**the wide variety of values meeting any given existential type,
combined with the possibility for mutation, means a sound typechecker
must be very conservative about what it permits when using those
values**.

So, in this article, we’ll explore some of these type error pitfalls,
see why it’s perfectly reasonable for the compilers to complain about
these pieces of code, and fix the errors using the equivalence rules
we’ve learned about in previous articles.  This is a big topic, so in
the next article, we’ll talk about taking advantage of the freedoms
that the compilers are concerned about in this one.

Aliasing prevents replacement
-----------------------------

Let’s say you have a list of strings, `strs: List[String]`.  But you
want that value to change: you want it to be a `List[Int]` instead.
Maybe every value in the list parses to an integer and you want to
“map in place”; maybe you want to use `-1` for every string that can’t
be parsed.

Generic systems like those of Java, Scala, and the ML family don’t let
you do this for a few reasons.

First, with regard to structures like `List`, let’s suppose we are
adding this feature to the type system, and that the list has type
`List[String]` before replacement, and `List[Int]` after.  What type
does it have when we’re halfway done?  The type system requires us to
*guarantee* that there are no strings, only ints, left once we’re
done; how do we track our progress?  Remember that vague promises of
“I tested the code” are meaningless to the type system; you have to
*prove* it mathematically.  This is a solvable problem, but the known
solutions far complicate the type system beyond the design goals of
these languages: the cost is far too high for the benefit.

Second, let us suppose that we’ve solved the first problem.  Or, let
us suppose that we introduce a structure for which this isn’t a
problem.

```scala
final case class OneThing[T](var value: T)
```

There, no chance of “halfway done” there!  But something else happens.

```scala
val strs: OneThing[String] = OneThing("hi")
strs.value = 42
// won't compile, just a thought experiment
```

Now, presumably, completely aside from the value, the variable `strs`
has *changed types* to `OneThing[Int]`.  Not just the value in it, the
variable itself.  OK, so what if that variable came from someplace
else?

```scala
def replaceWithInt(ote: OneThing[_]): Unit =
  t.value = 42

val strs: OneThing[String] = OneThing("hi")
replaceWithInt(strs)
// also won't compile, thought experiment
```

Now, the type of `replaceWithInt` must contain a note that “by the
way, the type of `ote`, and any variables that refer to it, and any
variables that refer to *those* variables, and so on until it stops,
will change as a result of this call”.

This is a problem of *aliases*, all the locations that may refer to a
value.  If you change the type of the value, you also have to update
every reference to it, at compile time!  This is *the type system*;
your promise that you have no other references is not good enough.
You have to *prove* it.

As with the previous problem, the known solutions to this problem
would complicate the type systems of Java and Scala beyond their
design goals.  In a sense, **this aspect of their type systems can be
considered to encourage functional programming**.  A type-changing map
that builds a new list of the new type, or what have you, instead of
mutating the old one, is *trivial* in the Java/Scala generics systems.
There are no chances of aliasing problems, because no one could
possibly have an unknown reference to the value you just constructed.
This is a even more obvious design choice in the ML family languages,
which make no secret of favoring functional programming.

Assignment rewrites existentials
--------------------------------

[We saw earlier]({% post_url 2015-07-16-method-equiv %}) that a simple get from a
`List<?>`, followed by adding that value right back to the same list,
didn’t work, but if we took that `xs` and passed it to a
type-parameterized version, everything worked fine.  Why is that?

If you have a *mutable* variable of an existential type, the
existentialized part of the type may have different [type] values at
different parts of the program.  Let’s use
[the `StSource` from the projection post]({% post_url 2015-07-23-type-projection %}#a-good-reason-to-use-type-members).
Note that the `S` member is existential, because we did not bind it.

```scala
var mxs: StSource[String] = StSource(0)(x => (x.toString, x + 1))
// at this point in the program, the S is Int
val s1 = mxs.init
// now I'm going to change mxs
mxs = StSource("ab")(x => (x, x.reverse))
// at this point in the program, the S is String
mxs.emit(s1)

TmTp6.scala:14: type mismatch;
 found   : tmtp6.Tmtp4Funs.s1.type (with underlying type tmtp4.StSource[String]#S)
 required: _2.S where val _2: tmtp4.StSource[String]
mxs.emit(s1)
         ^
```

And it’s good that this happens, because the value we got from `init`
is definitely incompatible with the argument type to `emit`.

If we don’t want to track when this happens—and we certainly can’t
decide, in all cases, when a mutable variable such as this has been
overwritten so as to change its existentials, given the freedoms
afforded by Java—how can we treat a mutable variable with existentials
as safe?  The type system makes a simplifying assumption: *every
reference to the variable gets fresh values to fill in the
existentials*.

If it helps, you can think of a mutable variable as an *immutable*
variable that wraps its type with an extra layer.  In fact, that's
[what scalac does when you capture a `var`](https://github.com/scala/scala/blob/v2.11.7/src/library/scala/runtime/ObjectRef.java#L14-L17).
So `mxs` is, in a sense, of type `Ref[StSource[String]]`, where

```scala
trait Ref[T] {
  def value: T
  def update(t: T): Unit
}
```

So, by substitution, the variable `mxs` is really a pair of functions,
`() => StSource[String]` and `StSource[String] => Unit`.  The “getter”
returns `StSource[String]`; each time you invoke that getter, you
might get an `StSource[String]` with a different `S` member, because
the `forSome` effectively occurs inside the body, as described in
[the substitutions of “Nested existentials”]({% post_url 2015-07-27-nested-existentials %}#what-if-we-list-different-existentials).

Of course, this means you can take advantage of this in your own
designs, to get *some* of the behavior of a type-changing value
mutation.  The use of variable references to delineate existentials
means that, even when we replace `mxs` above, the behavior of the
variable in the context of an instance hasn’t really changed, so
nothing about the containing value’s type has changed.  We thus
preserve our property, that values never change types.

When you make a single reference, it has to be consistent; subsequent
mutations have no effect on the value we got from that reference.  So
we can pass that reference somewhere that asserts that this doesn’t
happen in its own context, such as a type-parameterized
≡*<sub><small>m</small></sub>* method.  If you have a mutable variable
of type `List<T>`, even if you don’t know what `T` is, you know that
any updates to that variable will keep the same `T`.

Making variables read-only matters
----------------------------------

If I change the variable to `final` in Java, and remove mutation, I
shouldn’t have this problem anymore.  Surprisingly, I do; this is what
happened in
[the original `copyToZero` example]({% post_url 2015-07-16-method-equiv %}),
where the argument was declared `final`.  I assume that this is just a
simplifying assumption in `javac`, that the extra guarantee of
unchanging existentials offered by `final` isn’t understood by the
compiler.

In the case of Scala, though, **when you are using existential type
members, Scala can understand the implications of an immutable
variable, declared with `val`**.

```scala
val imxs: StSource[String] = StSource(0)(x => (x.toString, x + 1))
val s1 = imxs.init
// you can do whatever you want here; the compiler will stop you from
// changing imxs
imxs.emit(s1)
```

It can’t pull off this trick for type parameters, having just as much
trouble as Java there.  So this is another reason for
[our original rule of thumb]({% post_url 2015-07-13-type-members-parameters %}).

Naming the existential
----------------------

The benefit we get from
[passing `copyToZeroP`’s argument to `copyToZeroT`]({% post_url 2015-07-16-method-equiv %})
is that we *name* the existential for the single reference to the
argument that we make.  We name it `T` there, for the scope of its
invocation.

Likewise, in Scala, each `val` introduces, while it is in scope, each
existential member it has, as a type name.  There are
[a lot of rules in Scala](http://www.scala-lang.org/files/archive/spec/2.11/03-types.html#paths)
for exactly when this happens, but you may want to simply experiment.
We got a hint of what that name is
[when we used `StSource` existentially in the REPL]({% post_url 2015-07-23-type-projection %}#type-parameters-see-existentially).
Here’s the previous example again, with a type annotation for `s1`.

```scala
val imxs: StSource[String] = StSource(0)(x => (x.toString, x + 1))
val s1: imxs.S = imxs.init
imxs.emit(s1)
```

We have gained convenience, not power, with this *path-dependent
types* feature; we can always pass into a type-parameterized local
method, with only the inconvenience of having to write out the whole
polymorphic method and call dance.  Moreover, this is nowhere near
[a solution to the type projection problem]({% post_url 2015-07-23-type-projection %}#a-failed-attempt-at-simplified-emitting);
there are too many things that a type parameter can do that we can’t
with this feature.  But we’ll dive into that in a later post.

By-name existential arguments aren’t equivalent!
------------------------------------------------

There is another little corner case in method equivalence to consider.

```scala
def copyToZeroNP(xs: => PList[_]): Unit
def copyToZeroNT[T](xs: => PList[T]): Unit
```

These method types are not equivalent!  That’s because `copyToZeroNP`
can be called with a by-name *that evaluates to a list with a
different element type each time*; `copyToZeroNT` doesn’t allow this.

```scala
def time: PList[_] = {
  val t = System.currentTimeMillis
  if (t % 2 == 0) PCons("even", PNil())
  else PCons(66, PNil())
}

copyToZeroNP(time)  // ok
copyToZeroNT(time)  // not ok
```

In effect, `=>` is like a type constructor; we can think of these
arguments as `byname[PList[_]]` and `byname[PList[T]]`.  So we have
exactly the same problem as we had with
[`plenLength` and `plenLengthTP`]({% post_url 2015-07-27-nested-existentials %}#method-equivalence-%E2%80%A6-broken).

Unfortunately,
[Scala currently accepts this, where it shouldn’t](https://issues.scala-lang.org/browse/SI-9419).

The difference between these two methods gives us a hint about working
with existentials: if we can shift the scope for a given existential
outside the function that keeps giving us different types on every
reference, we might have an easier time working with it, even if we
can’t change it to a `val`; maybe it needs to be lazy and not saved,
for example.  So, despite the occasional convenience of path-dependent
types, **type parameterized methods are still your best friends when
working with existential types**.

In
[the next article, “To change types, change values”]({% post_url 2015-09-21-change-values %}),
we’ll look at some programs that make use of the two kinds of “type
changing” discussed above.  After that, we’ll finally talk about
methods that *return* values of existential type, rather than merely
taking them as arguments.

*This article was tested with Scala 2.11.7.*
