---
layout: post
title: A tale on Semirings

meta:
  nav: blog
  author: lukajcb
  pygments: true

tut:
  scala: 2.12.7
  binaryScala: "2.12"
  scalacOptions:
    - "-Ypartial-unification"
    - "-language:higherKinds"
    - "-language:implicitConversions"
    - "-language:experimental.macros"
  dependencies:
    - com.github.mpilquist::simulacrum:0.14.0
  plugins:
    - org.scalamacros:paradise_2.12.7:2.1.0
---



Ever wondered why sum types are called sum types?
Or maybe you've always wondered why the `<*>` operator uses exactly these symbols?
And what do these things have to do with Semirings?
Read this article and find out!


----
We all know and use `Monoid`s and `Semigroup`s.
They're super useful and come with properties that we can directly utilize to gain a higher level of abstractions at very little cost.
Sometimes, however, certain types can have multiple `Monoid` or `Semigroup` instances.
An easy example are the various numeric types where both multiplication and addition form two completely lawful monoid instances.

In abstract algebra there is a an algebraic class for types with two `Monoid` instances that interact in a certain way.
These are called `Semiring`s (sometimes also `Rig`) and they are defined as two `Monoid`s with some special laws that define the interactions between them.
Because they are often used to describe numeric data types we usually classify them as *Additive* and *Multiplicative*. 
Just like with numeric types the laws of `Semiring` state that multiplication has to distribute over addition and multiplying a value with the additive identity (i.e. zero) absorbs the value and becomes zero.

There are different ways to encode this as type classes and different libraries handle this differently, but let's look at how the [algebra](https://typelevel.org/algebra/) project handles this.
Specifically, it defines a separate `AdditiveSemigroup` and `MultiplicativeSemigroup` and goes from there.

```tut:silent
import simulacrum._

@typeclass trait AdditiveSemigroup[A] {
  def +(x: A)(y: A): A
}

@typeclass trait AdditiveMonoid[A] extends AdditiveSemigroup[A] {
  def zero: A
}

@typeclass trait MultiplicativeSemigroup[A] {
  def *(x: A)(y: A): A
}

@typeclass trait MultiplicativeMonoid[A] extends MultiplicativeSemigroup[A] {
  def one: A
}
```

A `Semiring` is then just an `AdditiveMonoid` coupled with a `MultiplicativeMonoid` with the following extra laws:

1. Additive commutativity. i.e. `x + y === y + x`
2. Right Distributivity. i.e. `(x + y) * z === (x * z) + (y * z)`
3. Left Distributivity. i.e. `x * (y + z) === (x * y) + (x * z)`
4. Right absorption. i.e. `x * zero === zero`
5. Left absorption. i.e. `zero * x === zero`

To define it as a typeclass, we simply extend from both additive and multiplicative monoid:

```tut:silent
@typeclass trait Semiring[A] extends MultiplicativeMonoid[A] with AdditiveMonoid[A]
```

Now we have a `Semiring` class, that we can use with the various numeric types like `Int`, `Long`, `BigDecimal` etc, but what else is a `Semiring` and why dedicate a whole blog post to it?

It turns out a lot of interesting things can be `Semiring`s, including `Boolean`s, `Set`s and [animations](https://bkase.github.io/slides/algebra-driven-design/#/).
One very interesting thing I'd like to point out is that we can form a `Semiring` homomorphism from types to their number of possible inhabitants.
What the hell is that?
Well, bear with me for a while and I'll try to explain step by step.

Okay, so let's start with what I mean by possible inhabitants.
Every type has a specific number of values it can possibly have, e.g. a `Boolean` has 2 possible inhabitants, because it has two possible values: `true` and `false`.

So `Boolean` has two, how many do other primitive types have?
`Byte` has 2^8, `Short` has 2^16, `Int` has 2^32 and `Long` has 2^64.
So far so good, that makes sense, what about something like `String`?
`String` is an unbounded type and therefore theoretically has infinite number of different inhabitants (practically of course, we don't have infinite memory, so the actual number may vary depending on your system).

For what other types can we determine their number of possible inhabitants? 
Well a couple of easy ones are `Unit`, which has exactly one value it can take and also `Nothing`, which has 0 possible values.

That's neat, maybe we can encode this in actual code.
We could create a type class that should be able to give us the number of inhabitants for any type we give it:

```tut:silent
trait PossibleInhabitants[A] {
  def number: BigInt
}

object PossibleInhabitants {
  def of[A: PossibleInhabitants]: BigInt = apply[A].number

  def apply[A: PossibleInhabitants]: PossibleInhabitants[A] = implicitly
}
```

Awesome!
Now let's try to define some instances for this type class:

```tut:silent
implicit def booleanNumberOfInhabitants = new PossibleInhabitants[Boolean] {
  def number: BigInt = BigInt(2)
}

implicit def longNumberOfInhabitants = new PossibleInhabitants[Long] {
  def number: BigInt = BigInt(2).pow(64)
}

implicit def intNumberOfInhabitants = new PossibleInhabitants[Int] {
  def number: BigInt = BigInt(2).pow(32)
}

implicit def shortNumberOfInhabitants = new PossibleInhabitants[Short] {
  def number: BigInt = BigInt(2).pow(16)
}

implicit def byteNumberOfInhabitants = new PossibleInhabitants[Byte] {
  def number: BigInt = BigInt(2).pow(8)
}

implicit def unitNumberOfInhabitants = new PossibleInhabitants[Unit] {
  def number: BigInt = 1
}

implicit def nothingNumberOfInhabitants = new PossibleInhabitants[Nothing] {
  def number: BigInt = 0
}
```

Alright, this is cool, let's try it out in the REPL!

```tut
PossibleInhabitants.of[Int]

PossibleInhabitants.of[Unit]

PossibleInhabitants.of[Long]
```

Cool, but this is all very simple, what about things like ADTs?
Can we encode them in this way as well?
Turns out, we can, we just have to figure out how to handle the basic product and sum types.
To do so, let's look at an example of both types.
First, we'll look at a simple product type: `(Boolean, Byte)`.

How many inhabitants does this type have? 
Well, we know `Boolean` has 2 and `Byte` has 256.
So we have the numbers from `-127` to `128` once with `true` and once again with `false`.
That gives us `512` unique instances.
Hmmm....

`512` seems to be double `256`, so maybe the simple solution is to just multiply the number of inhabitants of the first type with the number of inhabitants of the second type.
If you try this with other examples, you'll see that it's exactly true, awesome!
Let's encode that fact in a type class instance:

```tut:silent
implicit def tupleNumberOfInhabitants[A: PossibleInhabitants, B: PossibleInhabitants] =
  new PossibleInhabitants[(A, B)] {
    def number: BigInt = PossibleInhabitants[A].number * PossibleInhabitants[B].number
  }
```

Great, now let's look at an example of a simple sum type: `Either[Boolean, Byte]`.
Here the answer seems even more straight forward, since a value of this type can either be one or the other, we should just be able to add the number of inhabitants of one side with the number of inhabitants of the other side.
So `Either[Boolean, Byte]` should have `2 + 256 = 258` number of inhabitants. Cool!

Let's also code that up and try and confirm what we learned in the REPL:

```tut:silent
implicit def eitherNumberOfInhabitants[A: PossibleInhabitants, B: PossibleInhabitants] =
  new PossibleInhabitants[Either[A, B]] {
    def number: BigInt = PossibleInhabitants[A].number + PossibleInhabitants[B].number
  }
```

```tut
PossibleInhabitants.of[(Boolean, Byte)]

PossibleInhabitants.of[Either[Boolean, Byte]]

PossibleInhabitants.of[Either[Int, (Boolean, Unit)]]
```

So using sum types seem to add the number of inhabitants whereas product types seem to multiply the number of inhabitants.
That makes a lot of sense given their names!


So what about that homomorphism we talked about earlier? 
Well, a homomorphism is a structure-preserving mapping function between two algebraic structures of the same type (in this case a semiring).

This means that for any two values `x` and `y` and the homomorphism `f`, we get 
1. `f(x * y) === f(x) * f(y)`
2. `f(x + y) === f(x) + f(y)`

Now this might seem fairly abstract, but it applies exactly to what we just did.
If we *"add"* two types of `Byte` and `Boolean`, we get an `Either[Byte, Boolean]` and if we apply the homomorphism function, `number` to it, we get the value `258`.
This is the same as first calling `number` on `Byte` and then adding that to the result of calling `number` on `Boolean`.

And of course the same applies to multiplication and product types.
However, we're still missing something from a valid semiring, we only talked about multiplication and addition, but not about their respective identities.

What we did see, though is that `Unit` has exactly one inhabitant and `Nothing` has exactly zero.
So maybe we can use these two types to get a fully formed Semiring?

Let's try it out!
If `Unit` is `one` then a product type of any type with `Unit` should be equivalent to just the first type.

Turns out, it is, we can easily go from something like `(Int, Unit)` to `Int` and back without losing anything and the number of inhabitants also stay exactly the same.

```tut
PossibleInhabitants.of[Int]

PossibleInhabitants.of[(Unit, Int)]

PossibleInhabitants.of[(Unit, (Unit, Int))]
```

Okay, not bad, but how about `Nothing`?
Given that it is the identity for addition, any type summed with `Nothing` should be equivalent to that type. 
Is `Either[Nothing, A]` equivalent to `A`?
It is! Since `Nothing` doesn't have any values an `Either[Nothing, A]` can only be a `Right` and therefore only an `A`, so these are in fact equivalent types.

We also have to check for the absorption law that says that any value mutliplied with the additive identity `zero` should be equivalent to `zero`.
Since `Nothing` is our `zero` a product type like `(Int, Nothing)` should be equivalent to `Nothing`.
This also holds, given the fact that we can't construct a `Nothing` so we can never construct a tuple that expects a value of type `Nothing` either.

Let's see if this translates to the number of possible inhabitants as well:

Additive Identity:
```scala
scala> PossibleInhabitants.of[Either[Nothing, Boolean]]
res0: BigInt = 2

scala> PossibleInhabitants.of[Either[Nothing, (Byte, Boolean)]]
res1: BigInt = 258
```

Absorption:
```scala
scala> PossibleInhabitants.of[(Nothing, Boolean)]
res0: BigInt = 0

scala> PossibleInhabitants.of[(Nothing, Long)]
res1: BigInt = 0
```

Nice! 
The only thing left now is distributivity. 
In type form this means that `(A, Either[B, C])` should be equal to `Either[(A, B), (A, C)]`.
If we think about it, these two types should also be exactly equivalent, woohoo!

```tut

PossibleInhabitants.of[(Boolean, Either[Byte, Short])]

PossibleInhabitants.of[Either[(Boolean, Byte), (Boolean, Short)]]
```

## Higher kinded algebraic structures

Some of you might have heard of the `Semigroupal` type class. 
But why is it called that, and what is its relation to a `Semigroup`?
Let's find out!

First, let's have a look at `Semigroupal`:

```scala
@typeclass trait Semigroupal[F[_]] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
}
```

It seems to bear some similarity to `Semigroup`, we have two values which we somehow combine, and it also shares `Semigroup`s associativity requirement.

So far so good, but the name `product` seems a bit weird.
It makes sense given we combine the `A` and the `B` in a tuple, which is a product type, but if we're using products, maybe this isn't a generic `Semigroupal` but actually a multiplicative one? 
Let's fix this and rename it!

```tut:silent
@typeclass trait MultiplicativeSemigroupal[F[_]] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
}
```

Next, let us have a look at what an additive `Semigroupal` might look like.
Surely, the only thing we'd have to change is going from a product type to a sum type:

```tut:silent
@typeclass trait AdditiveSemigroupal[F[_]] {
  def sum[A, B](fa: F[A], fb: F[B]): F[Either[A, B]]
}
```

Pretty interesting so far, can we top this and add identities to make `Monoidal`s?
Surely we can! For addition this should again be `Nothing` and `Unit` for multiplication:

```tut:silent
@typeclass trait AdditiveMonoidal[F[_]] extends AdditiveSemigroupal[F] {
  def nothing: F[Nothing]
}

@typeclass trait MultiplicativeMonoidal[F[_]] extends MultiplicativeSemigroupal[F] {
  def unit: F[Unit]
}
```

So now we have these fancy type classes, but how are they actually useful?
Well, I'm going to make the claim that these type classes already exist in cats today, just under different names.

Let's first look at the `AdditiveMonoidal`.
It is defined by two methods, `nothing` which returns an `F[Nothing]` and `sum` which takes an `F[A]` and an `F[B]` to create an `F[Either[A, B]]`.

What type class in Cats could be similar?
First, we'll look at the `sum` function and try to find a counterpart for `AdditiveSemigroupal`.
Since we gave the lower kinded versions of these type classes symbolic operators, why don't we do the same thing for `AdditiveSemigroupal`?

Since it is additive it should probably contain a `+` somewhere and it should also show that it's inside some context.

Optimally it'd be something like `[+]`, but that's not a valid identifier so let's try `<+>` instead!

```scala
def <+>[A, B](fa: F[A], fb: F[B]): F[Either[A, B]]
```

Oh! The `<+>` function already exists in cats as an alias for `combineK` which can be found on `SemigroupK`, but it's sort of different, it takes two `F[A]`s and returns an `F[A]`, not quite what we have here.

Or is it?
These two functions are actually the same, and we can define them in terms of one another as long as we have an invariant functor:

```scala
def sum[A, B](fa: F[A], fb: F[B]): F[Either[A, B]]

def combineK[A](x: F[A], y: F[A]): F[A] = {
  val feaa: F[Either[A, A]]] = sum(x, y)
  feaa.imap(_.merge)(Right(_))
}
```

So our `AdditiveSemigroupal` is equivalent to `SemigroupK`, so probably `AdditiveMonoidal` is equivalent to `MonoidK`, right?

Indeed, and we can show that quite easily.

`MonoidK` adds an `empty` function with the following definition:

```scala
def empty[A]: F[A]
```

This function uses a universal quantifier for `A`, which means that it works for any `A`, which then means that it cannot actually include any particular `A` and is therefore equivalent to `F[Nothing]` which is what we have for `AdditiveMonoidal`.

Excellent, so we found counterparts for the additive type classes, and we already now that `MultiplicativeSemigroupal` is equivalent to `cats.Semigroupal`.
So the only thing left to find out is the counterpart of `MultiplicativeMonoidal`.

I'm going to spoil the fun and make the claim that `Applicative` is that counterpart.
`Applicative` adds `pure`, which takes an `A` and returns an `F[A]`.
`MultiplicativeMonoidal` adds `unit`, which takes no parameters and returns an `F[Unit]`.
So how can we go from one to another? 
Well the answer is again using a functor:

```scala
def unit: F[Unit]

def pure(a: A): F[A] = unit.imap(_ => a)(_ => ())
```

`Applicative` uses a covariant functor, but as we've shown above it also works for invariant functors.
`Applicative` also uses `<*>` as an alias for using `product` together with `map`, which seems like further evidence that our intuition that its a multiplicative type class is correct.

So in cats right now we have `<+>` and `<*>`, is there also a type class that combines both similar to how `Semiring` combines `+` and `*`?

There is, it is called `Alternative`, it extends `Applicative` and `MonoidK` and if we were super consistent we'd call it a `Semiringal`:


```tut:silent
@typeclass 
trait Semiringal[F[_]] extends MultiplicativeMonoidal[F] with AdditiveMonoidal[F]
```

Excellent, now we've got both `Semiring` and a higher kinded version of it.
Unfortunately the lower kinded version can't be found in Cats yet, but hopefully in a future version it'll be available as well.

If it were available, we could derive a `Semiring` for any `Alternative` the same we can derive a `Monoid` for any `MonoidK` or `Applicative`.
We could also lift any `Semiring` back into `Alternative`, by using `Const`, just like we can lift `Monoid`s into `Applicative` using `Const`.

To end this blog post, we'll have a very quick look on how to do that.

```scala
import Semiring.ops._

case class Const[A, B](getConst: A)

implicit def constSemiringal[A: Semiring] = new Semiringal[Const[A, ?]] {
  def sum[B, C](fa: Const[A, B], fb: Const[A, C]): Const[A, Either[B, C]] =
    Const(fa.getConst + fb.getConst)

  def product[B, C](fa: Const[A, B], fb: Const[A, C]): Const[A, (B, C)] =
    Const(fa.getConst * fb.getConst)

  def unit: Const[A, Unit] =
    Const(Semiring[A].one)

  def nothing: Const[A, Nothing] =
    Const(Semiring[A].zero)
}
```

## Conclusion

Rings and Semirings are very interesting algebraic structures and even if we didn't know about them we've probably been using them for quite some time.
This blog post aimed to show how `Applicative` and `MonoidK` relate to `Monoid` and how algebraic data types form a semiring and how these algebraic structures are pervasive throughout Scala and other functional programming languages.
For me personally, realizing how all of this ties together and form some really satisfying symmetry was really mind blowing and I hope this blog post can give some good insight on recognizing these interesting similarities throughout Cats and other libraries based on different mathematical abstractions.



## Addendum

This article glossed over commutativity in the typeclass encodings.
Commutativity is very important law for semrings and the code should show that.
However, since this post already contained a lot of different type class definitions, adding extra commutative type class definitions that do nothing but add laws felt like it would distract from what is trying to be taught.
