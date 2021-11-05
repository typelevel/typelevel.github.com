---
layout: post
title: The singleton instance trick is unsafe

meta:
  nav: blog
  author: S11001001
  pygments: true
---

*Also, the “fake covariance” trick.*

Sometimes, Scala programmers notice a nice optimization they can use
in the case of a class that has an invariant type parameter, but in
which that type parameter
[appears in variant or phantom position in the actual data involved]({% post_url 2014-03-09-liskov_lifting %}).
[`=:=`](http://www.scala-lang.org/api/2.11.1/scala/Predef$$$eq$colon$eq.html)
is an
[example of the phantom case](https://github.com/scala/scala/blob/v2.11.1/src/library/scala/Predef.scala#L398).

```scala
sealed abstract class =:=[From, To] extends (From => To) with Serializable
```

[`scala.collection.immutable.Set`](http://www.scala-lang.org/api/2.11.1/scala/collection/immutable/Set.html)
is an example of the covariant case.

Here is the optimization, which is very similar to
[the `Liskov`-lifting previously discussed]({% post_url 2014-03-09-liskov_lifting %}):
a “safe” cast of the invariant type
parameter can be made, because all operations on the casted result
remain sound.
[Here it is for Set](https://github.com/scala/scala/blob/9fc098dd0dcf1825ec55501716b4f2a0a6d197ae/src/library/scala/collection/immutable/HashSet.scala#L170),
an example of the “fake covariance” trick:

```scala
override def toSet[B >: A]: Set[B] = this.asInstanceOf[Set[B]]
```

And
[here it is for `=:=`](https://github.com/scala/scala/blob/v2.11.1/src/library/scala/Predef.scala#L399-L402),
an example of the “singleton instance” trick.

```scala
private[this] final val singleton_=:= = new =:=[Any,Any] {
  def apply(x: Any): Any = x
}
object =:= {
  implicit def tpEquals[A]: A =:= A = singleton_=:=.asInstanceOf[A =:= A]
}
```

Unless you are using
[the Scalazzi safe Scala subset](https://dl.dropboxusercontent.com/u/7810909/talks/parametricity/4985cb8e6d8d9a24e32d98204526c8e3b9319e33/parametricity.pdf),
which forbids referentially nontransparent and nonparametric
operations, *these tricks are unsafe*.

Types are erased
----------------

Many people are confused that they cannot write functions like this:

```scala
def addone[A](x: A): A = x match {
  case s: String => s + "one"
  case i: Int => i + 1
}
```

Being given an error as follows.

```scala
<console>:8: error: type mismatch;
 found   : String
 required: A
         case s: String => s + "one"
                             ^
<console>:9: error: type mismatch;
 found   : Int
 required: A
         case i: Int => i + 1
                          ^
```

Let’s consider only one case, the first. In the right-hand side (RHS)
of this case, you have not proved that `A` is `String` at all! You
have only proved that, in addition to definitely having type `A`, `x`
also definitely has type `String`. In type relationship language,

```scala
x.type <: A
x.type <: String
```

All elephants are grey and are also animals, but it does not follow
that all grey things are animals or vice versa. If you use a cast to
“fix” this, you have produced type-incorrect code, period.

Type recovery
-------------

Under special circumstances, however, information about a type
parameter can be recovered, safe and sound. Take this:

```scala
abstract class Box[A]
case class SBox(x: String) extends Box[String]
case class IBox(x: Int) extends Box[Int]

def addone2[A](b: Box[A]): A = b match {
  case SBox(s) => s + "one"
  case IBox(x) => x + 1
}
```

This compiles, and I don’t even have to have data in the box to get at
the type information that `A ~ String` or `A ~ Int`.  Consider the
first case.  On the RHS, I have

```scala
b.type <: SBox <: Box[String]
b.type <: Box[A]
```

In addition, **<code>A</code> is invariant**, so after going up to
`Box[String]`, `b` couldn’t have widened that type parameter, or
changed it in any way, without an unsafe cast.  Additionally, our
supertype tree cannot contain `Box` twice with different parameters.
So we have proved that `A` is `String`, because we proved that
`Box[A]` is `Box[String]`.

This is very useful when defining
[GADTs](http://www.haskell.org/haskellwiki/GADTs_for_dummies).

Partial type recovery
---------------------

Let’s consider a similar ADT with the type parameter marked variant.

```scala
abstract class CovBox[+A]
case class CovSBox(x: String) extends CovBox[String]

def addone3[A](b: CovBox[A]): A = b match {
  case CovSBox(s) => s + "one"
}
```

This works too, because in the RHS of the case, we proved that:

```scala
b.type <: CovSBox <: CovBox[String] <: CovBox[A]
String <: A
```

The only transform in type `A` could have possibly undergone is a
widening, which *must* have begun at `String`. A similar example can
be derived for contravariance.

Singleton surety
----------------

In our first example, there is one type that we know must be a subtype
of `A`, no matter what!

```scala
def addone[A <: AnyRef](x: A): A = x: x.type
```

(Scala doesn’t like it when we talk about singleton types without an
`AnyRef` upper bound at least.  But the underlying principle holds for
all value types.)

Where `x` is an `A` of stable type, `x.type <: A` for all possible `A`
types.  You might say, “that’s uninteresting; obviously `x` is an `A`
in this code.”  But that isn’t what we’re talking about; our premise
is that **any** value of type `x.type` is also an `A`!

So if we could prove that something else had the singleton type
`x.type`, we would also prove that it shared all of `x`’s types!  We
can do that with a singleton type pattern, which is implemented
(soundly in 2.11) with a reference comparison.  Scala lets us use
*some* of the resulting implications.

```scala
final case class InvBox[A](b: A)
def maybeeq[A, B](x: InvBox[A], y: InvBox[B]): A = y match {
  case _: x.type => y.b
}
```

Unsafety
--------

To which you might protest, “there’s only one value of any singleton
type!”  Well, yes.  And here’s where our seemingly innocent
optimization turns nasty.  If you'll recall, it depends upon treating
a value with multiple types via an unsafe cast.

```scala
def unsafeCoerce[A, B]: A => B = {
  val a = implicitly[A =:= A]
  implicitly[B =:= B] match {
    case _: a.type => implicitly[A =:= B]
  }
}

def unsafeCoerce2[A, B]: A => B = {
  val n = Set[Nothing]()
  val b = n.toSet[B]
  n.toSet[A] match {
    case _: b.type => implicitly[A =:= B]
  }
}
```

Both of these compile to what is in essence an identity function.

```scala
scala> Some(unsafeCoerce[String, Int]("hi"))
res0: Some[Int] = Some(hi)

scala> Some(unsafeCoerce2[String, Int]("hi"))
res1: Some[Int] = Some(hi)
```

In our invariant `Box` example we decided that, as it was impossible
to change the type parameter without an unsafe cast, we could use that
knowledge in the consequent types. In `unsafeCoerce`, where `?`
represents the value before the match keyword:

```scala
?.type <: a.type <: (A =:= A)
?.type <: (B =:= B)
A ~ B
```

In `unsafeCoerce2`,

```scala
?.type <: b.type <: Set[B]
?.type <: Set[A]
A ~ B
```

**There is nothing wrong with Scala making this logical inference. The
“optimization” of that cast is not safe.**

Let me reiterate: **Scala’s type inference surrounding pattern
matching should not be “fixed” to make unsafe casts “safer” and steal
our GADTs. Unsafe code is unsafe.**

Scalazzi safe Scala subset saves us
-----------------------------------

For types like these, it is not possible to exploit this unsafety
without a reference check, which is what a singleton type pattern
compiles to.  As the Scalazzi safe subset forbids referentially
nontransparent operations, if you follow its rules, these
optimizations become safe again.

This is just yet another of countless ways in which following the
Scalazzi rules makes your code safer and easier to reason about.

That isn’t to say it’s impossible to derive a situation where the
optimization exposes an `unsafeCoerce` in Scalazzi code.  However, you
must specially craft a type in order to do so.

```scala
abstract class Oops[A] {
  def widen[B>:A]: Oops[B] = this.asInstanceOf[Oops[B]]
}
case class Bot() extends Oops[Nothing]

def unsafeCoerce3[A, B]: A => B = {
  val x = Bot()
  x.widen[A] match {
    case Bot() => implicitly[A <:< B]
  }
}
```

The implication being

```scala
?.type <: Bot <: Oops[Nothing]
?.type <: Oops[A]
Nothing ~ A
```

Scalaz
[uses the optimization under consideration in `scalaz.IList`](https://github.com/scalaz/scalaz/blob/v7.0.6/core/src/main/scala/scalaz/IList.scala#L436-L437).
So would generalized `Functor`-based `Liskov`-lifting, as discussed at
the end of [“When can Liskov be lifted?”]({% post_url 2014-03-09-liskov_lifting %}),
were it to be implemented.  However, these cases do not fit the bill
for exploitation from Scalazzi-safe code.

On the other hand, the singleton type pattern approach may be used in
*all* cases where the optimization may be invoked by a caller,
including standard library code where some well-meaning contributor
might add such a harmless-seeming avoidance of memory allocation
without your knowledge.  Purity pays, and often in very nonobvious
ways.

*This article was tested with Scala 2.11.1.*
