---
layout: post
title: Why is ADT pattern matching allowed?

meta:
  nav: blog
  author: S11001001
  pygments: true
---

One of the rules of
[the Scalazzi Safe Scala Subset](https://dl.dropboxusercontent.com/u/7810909/talks/parametricity/4985cb8e6d8d9a24e32d98204526c8e3b9319e33/parametricity.pdf)
is “no type casing”; in other words, testing the type via
`isInstanceOf` or type patterns isn’t allowed.  It’s one of the most
important rules therein for preservation of free theorems.  Common
functional programming practice in Scala *seems* to violate this rule
in a subtle way.  However, as we will see, that practice carves out a
very specific exception to this rule that, morally, isn’t an exception
at all, carrying convenient advantages and none of the drawbacks.

Why forbid type tests?
----------------------

With the “no type tests” rule, we forbid writing functions like this:

```scala
def revmaybe[T](xs: List[T]): List[T] = {
  val allInts = xs.forall{case _:Int => true
                          case _ => false}
  if (allInts) xs.reverse else xs
}
```

Which violates the
[free theorem](http://failex.blogspot.com/2013/06/fake-theorems-for-free.html)
of `revmaybe`’s type `revmaybe(xs map f) = revmaybe(xs) map f`, as
follows.

```scala
val xs = List(1, 2, 3)
def f(i:Int) = Some(i)

scala> revmaybe(xs map f)
res2: List[Some[Int]] = List(Some(1), Some(2), Some(3))

scala> revmaybe(xs) map f
res3: List[Some[Int]] = List(Some(3), Some(2), Some(1))
```

ADTs are OK to go
-----------------

On the other hand, the Scalazzi rules are totally cool with pattern
matching to separate the parts of
[ADTs](https://www.haskell.org/haskellwiki/Algebraic_data_type).  For
example, this is completely fine.

```scala
def headOption[T](xs: List[T]): Option[T] = xs match {
  case x :: _ => Some(x)
  case _ => None
}
```

Even more exotic matches, where we bring type information forward into
runtime, are acceptable, as long as they’re in the context of ADTs.

```scala
sealed abstract class Expr[T]
final case class AddExpr(x: Int, y: Int) extends Expr[Int]

def eval[T](ex: Expr[T]): T = ex match {
  case AddExpr(x, y) => x + y
}
```

ADTs use type tests
-------------------

Let’s look at the compiled code of the `eval` body, specifically, the
`case` line.

```nasm
         2: aload_2       
         3: instanceof    #60                 // class adts/AddExpr
         6: ifeq          39
         9: aload_2       
        10: checkcast     #60                 // class adts/AddExpr
        13: astore_3      
        14: aload_3       
        15: invokevirtual #63                 // Method adts/AddExpr.x:()I
        18: istore        4
        20: aload_3       
        21: invokevirtual #66                 // Method adts/AddExpr.y:()I
        24: istore        5
        26: iload         4
        28: iload         5
        30: iadd          
```

So, instead of calling `unapply` to presumably check whether `AddExpr`
matches, scalac checks and casts its argument to `AddExpr`.  Why does
it do that?  Let’s see if we could use `AddExpr.unapply` instead.

```scala
scala> AddExpr.unapply _
res4: adts.AddExpr => Option[(Int, Int)] = <function1>
```

In other words, the `unapply` call can’t tell you whether an `Expr` is
an `AddExpr`; it can’t be called with arbitrary `Expr`.

The only actual check here is inserted by scalac as part of compiling
the pattern match expression, and it is a type test, supposedly
verboten under Scalazzi rules.  `headOption`, too, is implemented with
type tests and casts, not `unapply` calls.

We’ve exhorted Scala users to avoid type tests, but then turn around
and say that type tests are OK!  What’s going on?

An equivalent form
------------------

In every case where we use pattern matching on an ADT, there’s an
equivalent way we could write the expression without pattern matching,
by adding an encoding of the whole ADT as a method on the class or
trait we use as the base type.  Let’s redefine the `Option` type with
such a method to see how this is done.

```scala
sealed abstract class Maybe[T] {
  def fold[Z](nothing: => Z, just: T => Z): Z
}

final case class MNothing[T]() extends Maybe[T] {
  override def fold[Z](nothing: => Z, just: T => Z): Z =
    nothing
}

final case class Just[T](get: T) extends Maybe[T] {
  override def fold[Z](nothing: => Z, just: T => Z): Z =
    just(get)
}
```

It’s key to our reasoning that we completely avoid `match` in our
implementations; in other words, the `fold` is *matchless*.

With the `fold` method, the following two expressions are equivalent,
notwithstanding scalac’s difficulty optimizing the latter, even in the
presence of inlining.

```scala
(selector: Maybe[A]) match {
  case MNothing() => "default case"
  case Just(x) => justcase(x)
}

selector.fold("default case", x => justcase(x))
```

It’s a simple formula: the fold takes as many arguments as there are
cases, always returns the given, sole type parameter, and each
argument is a function that results in that same parameter.  There’s a
free theorem that a `fold` implementation on data structures without
recursion, like `Maybe`, can only invoke one of these arguments and
return the result directly, just as the pattern match does.

If you prefer the clarity of named cases, just use Scala’s named
arguments.  Here’s that last fold:

```scala
selector.fold(nothing = "default case",
              just = x => justcase(x))
```

GADT folds
----------

Encoding `Expr` is a little bit more complicated.  For the full power
of the type, we have to turn to `Leibniz` to encode the matchless
`fold`.

```scala
import scalaz.Leibniz, Leibniz.{===, refl}

sealed abstract class Expr2[T] {
  def fold[Z](add: (Int, Int, Int === T) => Z,
              concat: (String, String, String === T) => Z): Z
}
```

What does this mean?  The type `Int === T`, seen in the `add` argument
signature, is inhabited if and only if the type `T` **is** the type
`Int`.  So an implementation of `fold` can only call the `add`
function if it can prove that type equality.  There is, of course, one
that can:

```scala
final case class AddExpr2(x: Int, y: Int) extends Expr2[Int] {
  override
  def fold[Z](add: (Int, Int, Int === Int) => Z,
              concat: (String, String, String === Int) => Z): Z =
    add(x, y, refl)
}
```

Not only does `AddExpr2` know that `Expr2`’s type parameter is `Int`,
we must make the type substitution when implementing methods from
`Expr2`!  At that point it is enough to mention `refl`, the evidence
that every type is equal to itself, to satisfy `add`’s signature.

This may seem a little magical, but it is no less prosaic than
implementing `java.lang.Comparable` by making this substitution.  So
you can do this sort of thing every day even in Java.

```java
public interface Comparable<T> {
  int compareTo(T o);
}

class MyData implements Comparable<MyData> {
  @Override
  public int compareTo(MyData o) {   // note T is replaced by MyData
    // ...
  }
}
```

If only Java had higher kinds, you could go the rest of the way and
actually implement
[GADTs](https://www.haskell.org/haskellwiki/Generalised_algebraic_datatype#Motivating_example).

Moving on, let’s see another case for `Expr2`, and finally to tie it
all together, `eval2` with some extra constant data in for good
measure.

```scala
final case class ConcatExpr2(x: String, y: String) extends Expr2[String] {
  override
  def fold[Z](add: (Int, Int, Int === String) => Z,
              concat: (String, String, String === String) => Z): Z =
    concat(x, y, refl)
}

def eval2[T](ex: Expr2[T]): T =
  ex.fold((x, y, intIsT) => intIsT(1 + x + y),
          (x, y, strIsT) => strIsT("one" + x + y))
```

Using the `Leibniz` proof is, unfortunately, more involved than
producing it in the fold implementations.  See my previous posts,
[“A function from type equality to Leibniz”]({% post_url 2014-07-02-type_equality_to_leibniz %})
and
[“Higher Leibniz”]({% post_url 2014-09-20-higher_leibniz %}),
for many
details on applying `Leibniz` proof to make type transformations.

While the pattern matching `eval` didn’t have to explicitly apply type
equality evidence -- it *just knew* that `Int` was `T` when the
`IntExpr` pattern matched -- Scala has holes in its implementation,
discussed in the aforementioned posts on `Leibniz`, that sometimes
make the above implementation strategy an attractive choice even
though pattern matching is available.

We could, but that’s good enough, so we won’t
---------------------------------------------

You might have noticed that adding another case to `Expr` caused us
not only to implement an extra `fold`, but to add another argument to
the base `fold` to represent the new case, and then go through every
implementation to add that argument.  This isn’t so bad for just two
cases, but indeed has quadratic growth, to the point that adding a new
case to a large datatype is a majorly annoying project all by itself.

There is an interesting property of `fold`, though: the strategy isn’t
available for our first function, `revmaybe`, to discriminate
arguments of arbitrary type!  To do that, we would have to add a
signature like this to `Any`.

```scala
def fold[Z](int: Int => Z, any: Any => Z): Z = any(this)
// and, in the body of class Int
override def fold[Z](int: Int => Z, any: Any => Z): Z = int(this)
```

Obviously, you cannot do this.

You can only add `fold` methods to types you know; I can only call
`fold` in `expr2` by virtue of the fact that I know that the argument
has type `Expr2[T]` for some `T`.  If the argument was just `T`, I
wouldn’t have enough static type information to call `fold`.  So the
use of `fold`s doesn’t break parametricity.  Equivalently, **a pattern
match that could be implemented using a matchless fold also does not
break parametricity**.

As we have seen, it is unfortunately inconvenient to actually go
through the bother of writing `fold` methods, when pattern matching is
there.  But it is enough to reason that *we could* write a matchless
`fold` and replace the pattern matching with it, to prove that the
pattern matching is safe, no matter how many underlying type tests
scalac might use to implement it.

A simple test follows: **if you could write a matchless fold, and use
that instead, the pattern match is type-safe**.

A selector subtlety
-------------------

Here’s a pattern match that violates parametricity.

```scala
selector match {
  case MNothing() => "default case"
  case Just(x) => justcase(x)
}
```

Wait, but didn’t we rewrite that using a `fold` earlier?  Not quite.
Oh, I didn’t mention?  The type of `selector` is `T`, because we’re in
a function like this:

```scala
def notIdentity[T](selector: T) =
  // match expression above goes here
```

Scala will permit this pattern match to go forward.  It doesn’t
require us to prove that the selector is of the ADT root type we
happened to define; that’s an arbitrary point as far as Scala’s
subtyping system is concerned.  All that is required is that the
static type of `selector` be a supertype of each of `MNothing[_]` and
`Just[_]`, which `T` is, not being known to be more refined than
`Any`.

The test works here, though!  What is ambiguous to scalac is a bright
line in our reasoning. We can’t define a matchless `fold` that can be
invoked on this `selector`, so we reach the correct conclusion, that
the match violates parametricity.

The rule revisited
------------------

So we’ve carved out a clear “exception” to the “no type tests”
Scalazzi rule, and seen that it isn’t an exception at all.  There’s a
straightforward test you can apply to your pattern matches,

**If and only if I could, hypothetically, write a matchless fold, or
use an existing one, and rewrite this in its terms, this pattern
match is safe.**

but beware the subtle case where the match’s selector has a wider type
than you anticipated.

Finally, this is a rule specifically about expressions that don’t
violate our ability to reason about code.  This doesn’t hold for
arbitrary type-unsafe rewrites: that you could write a program safely
means you *should* write it safely.  Unlike arbitrary rewrites into
nonfunctional code, the pattern match uses no
non-referentially-transparent and no genuinely non-parametric
expressions.

*This article was tested with Scala 2.11.4 and Scalaz 7.1.0.*
