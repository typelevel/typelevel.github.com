---
layout: post
title: Let's build ourselves a small ScalaCheck

meta:
  nav: blog
  author: larsrh
  pygments: true
  mathjax: true

tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - org.scala-lang:scala-library:2.11.8
---

_[ScalaCheck](http://scalacheck.org/) is a well-known property-based testing library, based on ideas from Haskell's [QuickCheck](https://hackage.haskell.org/package/QuickCheck).
It is also a [Typelevel project](/projects).
In this post, I'd like to show some of the underlying mechanisms, stripped down to the bare minimum._

Testing with properties is well-understood in academia and widely used in parts of the industry – namely the parts which embrace functional programming.
However, the design space of property-testing libraries is rather large.
I think it is high time to talk about various tradeoffs done in libraries.
Here, I'd like to contribute by implementing a ScalaCheck clone from scratch using a very similar design and explaining the design choices along the way.

## The first design decision

The basic point of a property testing library is providing an interface looking roughly like this:

```tut:book:silent
class Prop {
  def check(): Unit = ???
}

object Prop {
  def forAll[A](prop: A => Boolean): Prop = ???
}
```

Now, you can use that in your test code:

```tut:book:fail:silent
Prop.forAll { (x: Int) =>
  x == x
}
```

This expresses that you have a property which is parameterized on a single integer number.
Hence, the library must somehow provide these integer numbers.
The original Haskell QuickCheck, ScalaCheck and many other libraries use a _random generator_ for this.
This comes with a number of advantages:

* It is relatively simple and efficient to implement.
* Random number generators compose exceedingly well.
* The confidence in the tests can be increased by just generating more inputs.
* Depending on the random distributions of the generators used, you have chances that both “exotic” and “common” inputs are covered.
* In practice, it turns out that random generators are decent at finding edge cases.

But it is also not without problems:

* For more complex inputs, the default generators are basically useless, because they will produce invalid input most of the time.
* Filtering random values before feeding them into the property can dramatically slow down the whole process.
* By default, it is non-deterministic (but there are remedies available).
* Generation of _random functions_ to be used as inputs for higher-order properties is quite round-about.

Of course, there are other possible design choices:

* [SmallCheck](https://hackage.haskell.org/package/smallcheck) instead enumerates _all_ values up to a certain size.
  For example, you can specify that you want to test some function over integer lists with all lists up to size 5, containing all integers between -5 and +5.
  In some situations, namely when your input is finite, you can even _exhaustively_ check all inputs, which is equivalent to a _proof_ that your program is correct.
  The disadvantage is that even for small sizes, the input space may explode exponentially or worse (e.g. when generating lists of lists).
* [Isabelle](https://isabelle.in.tum.de) Quickcheck supports multiple modes, including _narrowing_, which is a form of symbolically exploring the search space.
  This is based on Haskell's [Lazy SmallCheck](https://hackage.haskell.org/package/lazysmallcheck) (see also the [paper by Runciman et.al.](https://www.cs.york.ac.uk/fp/smallcheck/smallcheck.pdf)).
  The basic idea is that we can try to evaluate properties with _partially-defined inputs_ and refine them on demand.

**For this post, we're assuming that random generation is a given.**

## The second design decision

> Do we want to do this purely or poorly?

Of course, this motto is tongue-in-cheek.
Just because something isn't _pure_ doesn't mean that it is _poor._

To understand the design space here, let's focus on the smallest building block: A primitive random generator.
There are two possible ways to model this.
The _mutable_ way is what Java, Scala and many other languages offer in their libraries:

```tut:book:silent
trait Random {
  def nextInt(min: Int, max: Int): Int
  def nextFloat(): Float
  def nextItem[A](pool: List[A]): A
}
```

By looking at the types alone, we can already see that two subsequent calls of `nextInt` will produce different results; the interface is thus _impure._

The _pure_ way is to make the internal state (also known as “seed” in the context of random generators) explicit:

```tut:book:silent:reset
trait Seed {
  def nextInt(min: Int, max: Int): (Int, Seed)
  def nextFloat: (Float, Seed)
  def nextItem[A](pool: List[A]): (A, Seed)
}

object Seed {
  def init(): Seed = ???
}
```

Because this is difficult to actually use (don't mix up the `Seed` instances and use them twice!), one would wrap this into a state monad:

```tut:book:silent
class Random[A](private val op: Seed => (A, Seed)) { self =>
  def run(): A = op(Seed.init())._1

  def map[B](f: A => B): Random[B] =
    new Random[B]({ seed0 =>
      val (a, seed1) = self.op(seed0)
      (f(a), seed1)
    })

  def flatMap[B](f: A => Random[B]): Random[B] =
    new Random[B]({ seed0 =>
      val (a, seed1) = self.op(seed0)
      f(a).op(seed1)
    })
}

object Random {
  def int(min: Int, max: Int): Random[Int] = new Random(_.nextInt(min, max))
  val float: Random[Float] = new Random(_.nextFloat)
}
```

Now we can use Scala's `for` comprehensions:

```tut:book
for {
  x <- Random.int(-5, 5)
  y <- Random.int(-3, 3)
} yield (x, y)
```

The tradeoffs here are the usual when we're talking about functional programming in Scala: Reasoning ability, convenience, performance, … 
In the pure case, there are also multiple other possible encodings, including free monads.
Luckily, this blog covers that topic in another [post]({% post_url 2016-09-21-edsls-part-1 %}).

**For this post, we're assuming that mutable state is a given.**

## The third design decision

Asynchronous programming is all the rage these days.
This means that many functions will not return plain values of type `A`, but rather `Future[A]`, `Task[A]` or some other similar type.
For our testing framework, this poses a challenge:
If our properties call such asynchronous functions, the framework needs to know how to deal with a lot of `Future[Boolean]` values.
On the JVM, although not ideal, we could fall back to blocking on the result and proceed as usual.
On [Scala.js](http://www.scala-js.org/), this won't fly, because you just can't block in JavaScript.

Most general-purpose testing frameworks, like Specs2, have a [story about this](https://etorreborre.github.io/specs2/guide/SPECS2-3.8.5/org.specs2.guide.Matchers.html), enabling asynchronous checking of assertions.

In theory, it's not a problem to support this in a property testing library.
But in practice, there are some complications:

- Has the library been designed that way? If not, can we change it to support it?
  This is a real problem: It took quite some time and some significant refactorings to support `Future`s in [ScalaTest](http://www.scalatest.org/user_guide/async_testing).
- Should random generators also return `Future` values?
  We can easily imagine wanting to draw from a pool of inputs stemming from a database, or possibly to get better randomness from [random.org](https://www.random.org/).
  (The latter is a joke.)
- What async type constructor should we support?
  [The built-in one](http://www.scala-lang.org/files/archive/api/2.11.8/#scala.concurrent.Future)?
  [Monix' `Task`](https://monix.io/docs/2x/eval/task.html)?
  [fs2's `Task`](https://github.com/functional-streams-for-scala/fs2/blob/v0.9.1/core/shared/src/main/scala/fs2/Task.scala)?
  All of them?

If in the first design decisions we had chosen exhaustive generators, this problem would be even tougher, because designing a correct effectful stream type (of all possible inputs) is not trivial.

**For this post, we're assuming that we're only interested in synchronous properties, or can always block.**
However, I'd like to add, I'd probably try to incorporate async properties right from the start if I were to implement a testing library from scratch.

## The fourth design decision

Let's summarize what we have so far:

1. randomly generated inputs
2. ... using a stateful primitive generator
3. synchronous properties

Now, I'd like to talk about how to “package” random generators.
Earlier, we've only seen random integer and floating-point numbers, but of course, we want something more complex, including custom data structures.
It is convenient to abstract over this and specify the concept of a _generator_ for type `A`.
The idea is to make a generator for a type as “general” as possible and then provide combinators to compose them.

```tut:book:silent:reset
import scala.util.Random

trait Gen[T] {
  def generate(rnd: Random): T
}

object Gen {
  val int: Gen[Int] = new Gen[Int] {
    def generate(rnd: Random): Int =
      rnd.nextInt()
  }
}
```

An obvious combinator is a generator for tuples:

```tut:book:silent
def zip[T, U](genT: Gen[T], genU: Gen[U]): Gen[(T, U)] = new Gen[(T, U)] {
  def generate(rnd: Random): (T, U) =
    (genT.generate(rnd), genU.generate(rnd))
}
```

But we still have a problem:
There is currently no way to talk about the _size_ of the generated inputs.
Let's say we want to check an expensive algorithm over lists, for example with a complexity of $\mathcal O(n^3)$ over lists.
A naive implemenation of a list generator would take a a random size, and then give you some generator for lists.
The problem arises at the use site: Whenver you want to change the size of the generated inputs, you need to change the expression constructing the generator.

But we'd like to do better here:

**For this post, there should be a way to specify a maximum size of generated values, together with a way to influence that size in the tests without having to modify the generators.**

Here's how we can do that:

```tut:book:silent:reset
import scala.util.Random

trait Gen[T] {
  def generate(size: Int, rnd: Random): T
}

object Gen {

  val int: Gen[Int] = new Gen[Int] {
    def generate(size: Int, rnd: Random): Int = {
      val range = size * 2 + 1
      rnd.nextInt(range) - size
    }
  }

  def list[A](genA: Gen[A]): Gen[List[A]] = new Gen[List[A]] {
    def generate(size: Int, rnd: Random): List[A] = {
      val length = rnd.nextInt(size + 1)
      List.fill(length)(genA.generate(size, rnd))
    }
  }

}
```

We can now check this:

```tut:book:silent
def printSample[T](genT: Gen[T], size: Int, count: Int = 10): Unit = {
  val rnd = new Random()
  for (i <- 0 until size)
    println(genT.generate(size, rnd))
}
```

```tut
printSample(Gen.int, 10)
printSample(Gen.int, 3)
printSample(Gen.list(Gen.int), 10)
printSample(Gen.list(Gen.int), 3)
```

That's already pretty cool.
But there's another hidden design decision here:
We're using the same size on all sub-elements in the generated thing.
For example, in `Gen.list`, we're just passing the size through to the child generator.

SmallCheck does that differently: The “size” is defined to be the total number of constructors in the generated value.
For integer numbers, the “number of constructors” is basically the number itself.
For example, the value `List(1, 2)` has size $2$ in our framework (length of the list), but size $1 + 2 + 2 = 5$ in SmallCheck (roughly: size of all elements plus length of list).

Of course, our design decision might mean that stuff grows too fast.
The explicit size parameter can be used to alleviate that, especially for writing recursive generators:

```tut:book
def recList[T](genT: Gen[T]): Gen[List[T]] = new Gen[List[T]] {
  // extremely stupid implementation, don't use it
  def generate(size: Int, rnd: Random): List[T] =
    if (rnd.nextInt(size + 1) > 0)
      genT.generate(size, rnd) :: recList(genT).generate(size - 1, rnd)
    else
      Nil
}

printSample(recList(Gen.int), 10)
```

We can also provide a combinator for this:

```tut:book:silent
def resize[T](genT: Gen[T], newSize: Int): Gen[T] = new Gen[T] {
  def generate(size: Int, rnd: Random): T =
    genT.generate(newSize, rnd)
}
```

That one is useful because in reality ScalaCheck's `generate` method takes some more parameters than just the size.
Some readers might be reminded that this is just the reader monad and its `local` combinator in disguise.

## Some sugar

In order to make these generators nicely composable, we can leverage `for` comprehensions.
We just need to implement `map`, `flatMap` and `withFilter`:

```tut:book:silent:reset
import scala.util.Random

trait Gen[T] { self =>
  def generate(size: Int, rnd: Random): T

  // Generate a value and then apply a function to it
  def map[U](f: T => U): Gen[U] = new Gen[U] {
    def generate(size: Int, rnd: Random): U =
      f(self.generate(size, rnd))
  }

  // Generate a value and then use it to produce a new generator
  def flatMap[U](f: T => Gen[U]): Gen[U] = new Gen[U] {
    def generate(size: Int, rnd: Random): U =
      f(self.generate(size, rnd)).generate(size, rnd)
  }

  // Repeatedly generate values until one passes the check
  def withFilter(p: T => Boolean): Gen[T] = new Gen[T] {
    def generate(size: Int, rnd: Random): T = {
      val candidate = self.generate(size, rnd)
      if (p(candidate))
        candidate
      else // try again
        generate(size, rnd)
    }
  }
}

object Gen {

  // unchanged from above

  val int: Gen[Int] = new Gen[Int] {
    def generate(size: Int, rnd: Random): Int = {
      val range = size * 2 + 1
      rnd.nextInt(range) - size
    }
  }

  def list[A](genA: Gen[A]): Gen[List[A]] = new Gen[List[A]] {
    def generate(size: Int, rnd: Random): List[A] = {
      val length = rnd.nextInt(size + 1)
      List.fill(length)(genA.generate(size, rnd))
    }
  }

}
```

Look how simple composition is now:

```tut:invisible
def printSample[T](genT: Gen[T], size: Int, count: Int = 10): Unit = {
  val rnd = new Random()
  for (i <- 0 until size)
    println(genT.generate(size, rnd))
}
```

```tut:book
case class Frac(numerator: Int, denominator: Int)

val fracGen: Gen[Frac] =
  for {
    num <- Gen.int
    den <- Gen.int
    if den != 0
  } yield Frac(num, den)

printSample(fracGen, 10)
```

And we can even read the construction nicely: “First draw a numerator, then draw a denominator, then check that the denominator is not zero, then construct a fraction.”

As a side note: `Gen` as it is right now is _definitely not_ a valid monad, because it internally relies on mutable state.
But in my opinion, it is still justified to offer the `map` and `flatMap` methods, but don't give a `Monad` instance.
This prevents you from shoving `Gen` into functions which expect lawful monads.

It's still tedious to having to construct these generators by hand.
Both QuickCheck and ScalaCheck introduce a thin layer atop generators, called `Arbitrary`.
This is just a type class which contains a generator, nothing more.
Here's how it would look like in Scala:

```tut:book:silent
trait Arbitrary[T] {
  def gen: Gen[T]
}

// in practice we would put that into the companion object
//object Arbitrary {

  implicit val arbitraryInt: Arbitrary[Int] = new Arbitrary[Int] {
    def gen = Gen.int
  }

//}
```

Based on this definition, ScalaCheck provides a lot of pre-defined instances for all sorts of types.
For your custom types, the idea is that you define a low-level generator and wrap it into an implicit `Arbitrary`.
Then, in your tests, you just use the implicitly provided generator, and avoid to drop down to constructing them manually.

The purpose of the additional layer is explained easily: It is common to have multiple `Gen[T]` for the same `T` depending on which context it is needed in.
But there should only be one `Arbitrary[T]` for each `T`.
For example, you might have `Gen[Int]` for positive and negative integers, but you only have a single `Arbitrary[Int]` which covers all integers.
You use the latter when you actually need to supply an integer to your property, and the former to construct more complex generators, like for `Frac` above.

## The fifth design decision

This is where everything really comes together.
We're now looking at how to use `Gen` to implement the desired `forAll` function we've seen early in the introduction of the post, and how that is related to the `Prop` type I didn't define.
I'll readily admit that the following isn't really a design decision per se, because we'll be guided by the presence of type classes in Scala.
Still, one could reasonably structure this differently, and in fact, the design of the `Prop` type in e.g. QuickCheck is much more complex than what you'll see.

The rest of this post will now depart from the way it's done in ScalaCheck, although the ideas are still similar.
Instead, I'll try to show a simplified version without introducing complications required to make it work nicely.

Let's start with the concept of a _property._
A property is something that we can _run_ and which returns a _result_.
The result should ideally be something like a boolean: Either the property holds or it doesn't.
But one of the main features of any property testing library is that it will return a counterexample for the inputs where the property doesn't hold.
Hence, we need to store this counterexample in the failure case.

```tut:book:silent
sealed trait Result
case object Success extends Result
final case class Failure(counterexample: List[String]) extends Result

object Result {
  def fromBoolean(b: Boolean): Result =
    if (b)
      Success
    else
      // if it's false, it's false; no input has been produced,
      // so the counterexample is empty
      Failure(Nil)
}
```

You'll note that I've used `List[String]` here, because in the end we only want to print the counterexample on the console.
ScalaCheck has a dedicated `Pretty` type for that.
We could do even more fancy things here if we wanted to, but let's keep it simple.

Now we define the `Prop` type:

```tut:book:silent
trait Prop {
  def run(size: Int, rnd: Random): Result
}
```

What's missing is a way to construct properties.
Sure, we could implement the trait manually in our tests, but that would be tedious.
Type classes to the rescue!
We call something _testable_ if it can be converted to a `Prop`:

```tut:book:silent
trait Testable[T] {
  def asProp(t: T): Prop
}

// in practice we would put these into the companion object
//object Testable {

  // Booleans can be trivially converted to a property:
  // They are already basically a `Result`, so no need
  // to run anything!
  implicit val booleanIsTestable: Testable[Boolean] = new Testable[Boolean] {
    def asProp(t: Boolean): Prop = new Prop {
      def run(size: Int, rnd: Random): Result =
        Result.fromBoolean(t)
    }
  }

  // Props are already `Prop`s.
  implicit val propIsTestable: Testable[Prop] = new Testable[Prop] {
    def asProp(t: Prop): Prop = t
  }

//}
```

Now we're all set:

```tut:book:silent
def forAll[I, O](prop: I => O)(implicit arbI: Arbitrary[I], testO: Testable[O]): Prop =
  new Prop {
    def run(size: Int, rnd: Random): Result = {
      val input = arbI.gen.generate(size, rnd)
      val subprop = testO.asProp(prop(input))
      subprop.run(size, rnd) match {
        case Success =>
          Success
        case Failure(counterexample) =>
          Failure(input.toString :: counterexample)
      }
    }
  }
```

Let's unpack this step by step.

1. We're taking a function from `I => O`.
   This is supposed to be our parameterized property, for example `{ (x: Int) => x == x }`.
   Because we abstracted over values that can be generated (`Arbitrary`) and things that can be tested (`Testable`), the input and output types are completely generic.
   In the `implicit` block, we're taking the instructions of how to fit everything together.
2. We're constructing a `Prop`; that is, a thing that we can run and that produces a boolean-ish `Result`.
3. To run the property, we need to construct a random input.
   We can use the `Gen[I]` which we get from the `Arbitrary[I]`.
4. We pass that `I` into the parameterized property.
   To stick with the example, we evaluate the anonymous function `{ (x: Int) => x == x }` at input `5`, and obtain `true`.
5. We convert the result to a `Prop` again.
   This allows us to recursively nest `forAll`s, for example when we need two inputs.
6. We run the resulting property and check if it fails.
   If it does, we prepend the generated input to the counterexample.
   In the nested scenario, this allows us to see all generated inputs and the order in which we sticked them into the property.

At this point we should look at an example.

```tut:book
val propReflexivity =
  forAll { (x: Int) =>
    x == x
  }
```

Cool, but how do we run this?

Remember that our tool is supposed to evaluate a property on multiple inputs.
All these evaluations will produce a `Result`.
Hence, we need to merge those together into a single result.
We'll also define a convenient function that runs a property multiple times on different sizes:

```tut:book:silent
def merge(rs: List[Result]): Result =
  rs.foldLeft(Success: Result) {
    case (Failure(cs), _) => Failure(cs)
    case (Success, Success) => Success
    case (Success, Failure(cs)) => Failure(cs)
  }

def check[P](prop: P)(implicit testP: Testable[P]): Unit = {
  val rnd = new Random()
  val rs =
    for (size <- 0 to 100)
    yield testP.asProp(prop).run(size, rnd)
  merge(rs.toList) match {
    case Success =>
      println("✓ Property successfully checked")
    case Failure(counterexample) =>
      val pretty = counterexample.mkString("(", ", ", ")")
      println(s"✗ Property failed with counterexample: $pretty")
  }
}
```

Let's run our property!

```tut
check(propReflexivity)
```

... and how about something wrong?

```tut
check(forAll { (x: Int) =>
  x > x
})
```

## Some more sugar

Okay, we're almost done.
The only tedious thing that remains is that we have to use the `forAll` combinator, especially in the nested case.
It would be great if we could just use `check` and pass it a function.
But since we've used type classes for everything, we're in luck!

```tut:book:silent
implicit def funTestable[I : Arbitrary, O : Testable]: Testable[I => O] = new Testable[I => O] {
  def asProp(f: I => O): Prop =
    // wait for it ...
    // ...
    // ...
    // it's really simple ...
    forAll(f)
}
```

Now we can check our functions even easier!

```tut
check { (x: Int) =>
  x == x
}

check { (x: Int) =>
  x > x
}

check { (x: Int) => (y: Int) =>
  x + y == y + x
}

check { (x: Int) => (y: Int) =>
  x + y == x * y
}
```

## The end

I'm going to stop here, although there are still some things to explore:

* How to get rid of the boilerplate to generate “boring” data structures?
* How to generate functions?
* How to improve usability?
* How to show useful counterexamples?
* How to test the library itself?
* How to make sure that your generators produce reasonable values?
* How to make sure that your generators cover a wide range of values?
* ...
