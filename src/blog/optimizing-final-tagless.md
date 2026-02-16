---
layout: post
title: Optimizing Tagless Final – Saying farewell to Free
category: technical

meta:
  nav: blog
  author: lukajcb
  pygments: true

---

The Tagless Final encoding has gained some steam recently, with some people hailing 2017 as the year of Tagless Final.
Being conceptually similar to the Free Monad, different comparisons have been brought up and the one trade-off that always comes up is the lack or the difficulty of inspection of tagless final programs and in fact, I couldn't find a single example on the web.
This seems to make sense, as programs in the tagless final encoding aren't values, like programs expressed in terms of free structures. 
However, in this blog post, I'd like to dispell the myth that inspecting and optimizing tagless final programs is more difficult than using `Free`.

Without further ado, let's get into it, starting with our example algebra, a very simple key-value store:

```scala
trait KVStore[F[_]] {
  def get(key: String): F[Option[String]]
  def put(key: String, a: String): F[Unit]
}
```


To get the easiest example out of the way, here's how to achieve parallelism in a tagless final program:

```scala
import cats._
import cats.implicits._

def program[M[_]: FlatMap, F[_]](a: String)(K: KVStore[M])(implicit P: Parallel[M, F]) =
  for {
    _ <- K.put("A", a)
    x <- (K.get("B"), K.get("C")).parMapN(_ |+| _)
    _ <- K.put("X", x.getOrElse("-"))
  } yield x
```

This programs makes use of the `cats.Parallel` type class, that allows us to make use of the `parMapN` combinator to use independent computations with a related `Applicative` type. This is already much simpler than doing the same thing with `Free` and `FreeApplicative`. For more info on `Parallel` check out the cats docs [here](https://typelevel.org/cats/typeclasses/parallel.html).

However this is kind of like cheating, we're not really inspecting the structure of our program at all, so let's look at an example where we actually have access to the structure to do optimizations with.

Let's say we have the following program:

```scala
def program[F[_]: Apply](F: KVStore[F]): F[List[String]] =
    (F.get("Cats"), F.get("Dogs"), F.put("Mice", "42"), F.get("Cats"))
      .mapN((f, s, _, t) => List(f, s, t).flatten)
```

Not a very exciting program, but it has some definite optimization potential.
Right now, if our KVStore implementation is an asynchronous one with a network boundary, our program will make 4 network requests sequentially if interpreted with the standard `Apply` instance of something like `cats.effect.IO`.
We also have a duplicate request with the `"Cats"`-key.

So let's look at what we could potentially do about this.
The first thing we should do, is extract the static information.
The easiest way to do so, is to interpret it into something we can use using a `Monoid`.
This is essentially equivalent to the `analyze` function commonly found on `FreeApplicative`.

Getting this done, is actually quite simple, as we can use `cats.Const` as our `Applicative` data type, whenever the lefthand side of `Const` is a `Monoid`. 
I.e. if `M` has a `Monoid` instance, `Const[M, A]` has an `Applicative` instance.
You can read more about `Const` [here](https://typelevel.org/cats/datatypes/const.html).

```scala
val analysisInterpreter: KVStore[Const[(Set[String], Map[String, String]), ?]] =
  new KVStore[Const[(Set[String], Map[String, String]), ?]] {
    def get(key: String) = Const((Set(key), Map.empty))
    def put(key: String, a: String) = Const((Set.empty, Map(key -> a)))
  }

program(analysisInterpreter).getConst
// res0: (Set[String], Map[String,String]) = (Set(Cats, Dogs),Map(Mice -> 42))

```

By using a Tuple of `Set` and `Map` as our `Monoid`, we now get all the unique keys for our `get` and `put` operations.
Next, we can use this information to recreate our program in an optimized way.

```scala
def optimizedProgram[F[_]: Applicative](F: KVStore[F]): F[List[String]] = {
  val (gets, puts) = program(analysisInterpreter).getConst

  puts.toList.traverse { case (k, v) => F.put(k, v) } 
    *> gets.toList.traverse(F.get).map(_.flatten)
}
```

And we got our first very simple optimization.
It's not much, but we can imagine the power of this technique.
For example, if we were using something like `GraphQL`, we could sum all of our `get` requests into one large request, so only one network roundtrip is made.
We could imagine similar things for other use cases, e.g. if we're querying a bunch of team members that all belong to the same team, it might make sense to just make one request to all the team's members instead of requesting them all individually.

Other more complex optimizations could involve writing a new interpreter with the information we gained from our static analysis.
One could also precompute some of the computations and then create a new interpreter with those computations in mind.

Embedding our Applicative program inside a larger monadic program is also trivial:

```scala
def program[F[_]: Apply](mouse: String)(F: KVStore[F]): F[List[String]] =
  (F.get("Cats"), F.get("Dogs"), F.put("Mice", mouse), F.get("Cats"))
    .mapN((f, s, _, t) => List(f, s, t).flatten)

def optimizedProgram[F[_]: Applicative](mouse: String)(F: KVStore[F]): F[List[String]] = {
  val (gets, puts) = program(mouse)(analysisInterpreter).getConst

  puts.toList.traverse { case (k, v) => F.put(k, v) } 
    *> gets.toList.traverse(F.get).map(_.flatten)
}

def monadicProgram[F[_]: Monad](F: KVStore[F]): F[Unit] = for {
  mouse <- F.get("Mice")
  list <- optimizedProgram(mouse.getOrElse("64"))(F)
  _ <- F.put("Birds", list.headOption.getOrElse("128"))
} yield ()
```

Here we refactor our `optimizedProgram` to take an extra parameter `mouse`. Then in our larger `monadicProgram`, we perform a `get` operation and then apply its result to `optimizedProgram`.

So now we have a way to optimize our one specific program, next we should see if we can introduce some abstraction.
Sadly Scala lacks Rank-N types, which makes this a bit difficult as we'll see.

First we'll have to look at the shape of a generic program, they usually are functions from an interpreter `Algebra[F]` to an expression inside the type constructor `F`, such as `F[A]`.

```scala
type Program[Alg[_[_]], F[_], A] = Alg[F] => F[A]
```

The problem of Rank-N types becomes apparent when we want to write a function where we interpret our program with two different interpreters, as we did before when interpreting into `Const`:

```scala
def optimize[Alg[_[_]], F[_]: Applicative, A, M: Monoid]
  (program: Alg[F] => F[A])
  (extract: Alg[Const[M, ?]])
  (restructure: M => F[A]): Alg[F] => F[A] = { interp =>

    val m = program(extract).getConst // error: type mismatch;
    // found   : extract.type (with underlying type Alg[[β$0$]cats.data.Const[M,β$0$]])
    // required: Alg[F]

    restructure(m)
  }
```
So, because of the lack of Rank-N types, this simple definition for our program is not enough to say that our program works for ALL type constructors `F[_]: Applicative`.

Fortunately there is a workaround, albeit requiring a bit more boilerplate:

```scala
trait Program[Alg[_[_]], A] {
  def apply[F[_]: Applicative](interpreter: Alg[F]) : F[A]
}

def optimize[Alg[_[_]], F[_]: Applicative, A, M: Monoid]
  (program: Program[Alg, A])
  (extract: Alg[Const[M, ?]])
  (restructure: M => F[A]): Alg[F] => F[A] = { interp =>
    val m = program(extract).getConst

    restructure(m)
  }
```

And now it should compile without a problem.
Now we should be able to express our original optimization with this new generic approach:

```scala
def program[F[_]: Apply](mouse: String)(F: KVStore[F]): F[List[String]] =
  (F.get("Cats"), F.get("Dogs"), F.put("Mice", mouse), F.get("Cats"))
    .mapN((f, s, _, t) => List(f, s, t).flatten)

def wrappedProgram(mouse: String) = new Program[KVStore, List[String]] {
  def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program(mouse)(alg)
}

def optimizedProgram[F[_]: Applicative](mouse: String)(F: KVStore[F]): KVStore[F] => F[List[String]] = 
  optimize(wrappedProgram(mouse))(analysisInterpreter) { case (gets, puts) =>
    puts.toList.traverse { case (k, v) => F.put(k, v) } *> gets.toList.traverseFilter(F.get)
  }
```

So far so good, we've managed to write a function to generically optimize tagless final programs.
However, one of the main advantages of tagless final is that implementation and logic should be separate concerns.
With what we have right now, we're violating the separation, by mixing the optimization part with the program logic part.
Our optimization should be handled by the interpreter, just as the sequencing of individual steps of a monadic program is the job of the target `Monad` instance.

One way to go forward, is to create a typeclass that requires certain algebras to be optimizable.
This typeclass could be written using the generic function we wrote before, so let's see what we can come up with:


```scala
trait Optimizer[Alg[_[_]], F[_]] {
  type M

  def monoidM: Monoid[M]
  def monadF: Monad[F]

  def extract: Alg[Const[M, ?]]
  def rebuild(m: M, interpreter: Alg[F]): F[Alg[F]]

  def optimize[A](p: Program[Alg, A]): Alg[F] => F[A] = { interpreter =>
    implicit val M: Monoid[M] = monoidM
    implicit val F: Monad[F] = monadF

    val m: M = p(extract).getConst

    rebuild(m, interpreter).flatMap(interp => p(interp))
  }
}
```

This might look a bit daunting at first, but we'll go through it bit by bit.
First we define our type class `Optimizer` parameterized by an algebra `Alg[_[_]]` and a type constructor `F[_]`.
This means we can define different optimizations for different algebras and different target types.
For example, we might want a different optimization for a production `Optimizer[KVStore, EitherT[Task, E, ?]]` and a testing `Optimizer[KVStore, Id]`.
Next, for our interpreter we need a `Monoid M` for our static analysis, however we don't to parameterize our `Optimizer` with an extra type parameter, since the actual type of `M` isn't necessary for the API, so we use an abstract type member instead.

Next we need actual `Monoid` and `Monad` instances for `F[_]` and `M` respectively.
The other two functions should seem familiar, the `extract` function defines an interpreter to get an `M` out of our program.
The `rebuild` function takes that value of `M` and the interpreter and produces an `F[Alg[F]]`, which can be understood as an `F` of an interpreter.
This means that we can statically analyze a program and then use the result of that to create a new optimized interpreter and this is exactly what the `optimize` function does.
This is also why we needed the `Monad` constraint on `F`, we could also get away with returning just a new interpreter `Alg[F]` from the `rebuild` method and get away with an `Applicative` constraint, but we can do more different things this way.

We'll also define some quick syntax sugar for this type class to make using it a tiny bit more ergonomic.

```scala
implicit class OptimizerOps[Alg[_[_]], A](val value: Program[Alg, A]) extends AnyVal {
  def optimize[F[_]: Monad](interp: Alg[F])(implicit O: Optimizer[Alg, F]): F[A] =
    O.optimize(value)(interp)
}
```

Let's see what our program would look like with this new functionality:

```scala
def monadicProgram[F[_]: Monad](F: KVStore[F])(implicit O: Optimizer[KVStore, F]): F[Unit] = for {
  mouse <- F.get("Mice")
  list <- wrappedProgram(mouse.getOrElse("64")).optimize(F)
  _ <- F.put("Birds", list.headOption.getOrElse("128"))
} yield ()
```

Looking good so far, now all we need to run this is an actual instance of `Optimizer`.
We'll use a Monix `Task` for this and for simplicity our new optimization will only look at the `get` operations:

```scala
implicit val kvStoreTaskOptimizer: Optimizer[KVStore, Task] = new Optimizer[KVStore, Task] {
  type M = Set[String]

  def monoidM = implicitly

  def monadF = implicitly

  def extract = new KVStore[Const[Set[String], ?]] {
    def get(key: String) = Const(Set(key))
    def put(key: String, a: String): Const[Set[String], Unit] = Const(Set.empty)
  }

  def rebuild(gs: Set[String], interp: KVStore[Task]): Task[KVStore[Task]] =
    gs.toList
      .parTraverse(key => interp.get(key).map(_.map(s => (key, s))))
      .map(_.flattenOption.toMap)
      .map { m =>
        new KVStore[Task] {
          override def get(key: String) = m.get(key) match {
            case v @ Some(_) => v.pure[Task]
            case None => interp.get(key)
          }

          def put(key: String, a: String): Task[Unit] = interp.put(key, a)
        }
      }

}
```

Our `Monoid` type is just a simple `Set[String]` here, as the `extract` function will only extract the `get` operations inside the `Set`.
Then with the `rebuild` we build up our new interpreter.
First we want to precompute all the values of the program.
To do so, we just run all the operations in parallel and put them into a `Map`, while discarding values where the `get` operation returned `None`.
Now when we have that precomputed `Map`, we'll create a new interpreter with it, that will check if the key given to `get` operation is in the precomputed `Map` instead of performing an actual request.
We can then lift the value into a `Task[Option[String]]`.
For all the `put` operations, we'll simply run the interpreter.

Now we should have a great optimizer for `KVStore` programs interpreted into a `Task`.
Let's see how we did by interpreting into a silly implementation that only prints whenever you use one of the operations:

```scala
object TestInterpreter extends KVStore[Task] {
  def get(key: String): Task[Option[String]] = Task {

    println("Hit network for " + key)

    Option(key + "!")
  }

  def put(key: String, a: String): Task[Unit] = Task {
    println("Put something: " + a)

    ()
  }
}
```

Now let's run our program with this interpreter and the optimizations!

```scala
monadicProgram(TestInterpreter).runAsync
// Hit network for Mice
// Hit network for Cats
// Hit network for Dogs
// Put something: Mice!
// Put something: Cats!
```

And it works, we've now got a principled way to write programs that can then be potentially optimized.



## Conclusion

Designing a way to completely separate the problem description from the actual problem solution is fairly difficult. The tagless final encoding allows us one such fairly simple way.
Using the technique described in this blog post, we should be able to have even more control over the problem solution by inspecting the structure of our program statically.
We've seen a few roadblocks along the way, such as the lack of Rank-N types in Scala, but we might be able to come up with a macro for that in the future, making it even more ergonomic.
Another thing we haven't covered here, are programs with multiple algebras, which is quite a bit more complex as you can surely imagine, maybe that will be the topic of a follow up blog post.

The code is published [right here](https://github.com/LukaJCB/sphynx), but might still change after getting a feeling for which API feels best.

What kind of problems and techniques would you like to see with regards to tagless final?
Would love to hear from you in the comments!
