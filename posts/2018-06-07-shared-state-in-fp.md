---
layout: post
title: Shared State in Functional Programming

meta:
  nav: blog
  author: gvolpe
  pygments: true

tut:
  scala: 2.12.4
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.4
    - org.typelevel::cats-core:1.1.0

---

Newcomers to functional programming (FP) are often very confused about the proper way to share state without breaking purity and end up having a mix of pure and impure code that [defeats the purpose](https://queue.acm.org/detail.cfm?id=2611829) of having pure FP code in the first place.

This is the reason that has motivated me to write a beginner friendly guide :)

## Use Case

We have a program that runs three computations at the same time and updates the internal state to keep track of the
tasks that have been completed. When all the tasks are completed we request the final state and print it out.

You should get an output similar to the following one:

```
Starting process #1
Starting process #2
Starting process #3
  ... 3 seconds
Done #2
  ... 5 seconds
Done #1
  ... 10 seconds
Done #3
List(#2, #1, #3)
```

We'll use the concurrency primitive `Ref[IO, List[String]]` to represent our internal state because it's a great fit.

### Getting started

So this is how we might decide to start writing our code:

```scala
import cats.effect._
import cats.effect.concurrent.Ref
import cats.instances.list._
import cats.syntax.all._

import scala.concurrent.duration._

object sharedstate extends IOApp {

  var myState: Ref[IO, List[String]] = _

  def putStrLn(str: String): IO[Unit] = IO(println(str))

  val process1: IO[Unit] = {
    putStrLn("Starting process #1") *>
      IO.sleep(5.seconds) *>
      myState.update(_ ++ List("#1")) *>
      putStrLn("Done #1")
  }

  val process2: IO[Unit] = {
    putStrLn("Starting process #2") *>
      IO.sleep(3.seconds) *>
      myState.update(_ ++ List("#2")) *>
      putStrLn("Done #2")
  }

  val process3: IO[Unit] = {
    putStrLn("Starting process #3") *>
      IO.sleep(10.seconds) *>
      myState.update(_ ++ List("#3")) *>
      putStrLn("Done #3")
  }

  def masterProcess: IO[Unit] = {
    myState = Ref.of[IO, List[String]](List.empty[String]).unsafeRunSync()
    val ioa = List(process1, process2, process3).parSequence.void
    ioa *> myState.get.flatMap(rs => putStrLn(rs.toString))
  }

  override def run(args: List[String]): IO[ExitCode] =
    masterProcess.as(ExitCode.Success)

}
```

We defined a `var myState: Ref[IO, List[String]]` initialized as `null` so we can create it on startup and all the child processes can have access to it. A so called `global state`.

But now we try to run our application and we encounter our first ugly problem: `NullPointerException` on line 19. All the processes are defined by using `myState` which has not yet been initialized. So an easy way to fix it is to define all our processes as `lazy val`.

```scala
lazy val process1: IO[Unit] = ???
lazy val process2: IO[Unit] = ???
lazy val process3: IO[Unit] = ???
```

That worked, brilliant! We have an application that meets the business criteria and most importantly it works!

### Rethinking our application

But let's take a step back and review our code once again, there are at least two pieces of code that should have caught your attention:

```scala
var myState: Ref[IO, List[String]] = _
```

We are using `var` and initializing our state to `null`, OMG! Also the workaround of `lazy val` should get you thinking...

And here's the second obvious one:

```scala
myState = Ref.of[IO, List[String]](List.empty[String]).unsafeRunSync()
```

We require our `myState` to be of type `Ref[IO, List[String]` but the smart constructor gives us an `IO[Ref[IO, List[String]]]` so we are "forced" to call `unsafeRunSync()` to get our desired type. And there's a reason for that, the creation of a `Ref[F, A]` is side-effectful, therefore it needs to be wrapped in `IO` to keep the purity.

But wait a minute... that `unsafeRunSync()` is something that you should only see at the edge of your program, most commonly in the `main` method that is invoked by the `JVM` and that is impure by nature (of type `Unit`). But because we are using `IOApp` we shouldn't be calling any operations which names are prefixed with `unsafe`.

You say to yourself, yes, I know this is bad and ugly but I don't know a better way to share the state between different computations and this works. But we know you have heard that funcional programming is beautiful so why doing this?

### Functional Programming

Okay, can we do better? Of course we do and you wouldn't believe how simple it is!

Let's get started by getting rid of that ugly `var myState` initialized to `null` and pass it as parameter to the processes that need to access it:

```scala
import cats.effect._
import cats.effect.concurrent.Ref
import cats.instances.list._
import cats.syntax.all._

import scala.concurrent.duration._

object sharedstate extends IOApp {

  def putStrLn(str: String): IO[Unit] = IO(println(str))

  def process1(myState: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("Starting process #1") *>
      IO.sleep(5.seconds) *>
      myState.update(_ ++ List("#1")) *>
      putStrLn("Done #1")
  }

  def process2(myState: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("Starting process #2") *>
      IO.sleep(3.seconds) *>
      myState.update(_ ++ List("#2")) *>
      putStrLn("Done #2")
  }

  def process3(myState: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("Starting process #3") *>
      IO.sleep(10.seconds) *>
      myState.update(_ ++ List("#3")) *>
      putStrLn("Done #3")
  }

  def masterProcess: IO[Unit] = {
    val myState: Ref[IO, List[String]] = Ref.of[IO, List[String]](List.empty[String]).unsafeRunSync()

    val ioa = List(process1(myState), process2(myState), process3(myState)).parSequence.void
    ioa *> myState.get.flatMap(rs => putStrLn(rs.toString))
  }

  override def run(args: List[String]): IO[ExitCode] =
    masterProcess.as(ExitCode.Success)

}
```

Great! We got rid of that `global state` and we are now passing our `Ref`as a parameter. Remember that it is a concurrency primitive meant to be accesed and modified in concurrent scenarios, so we are safe here.

Notice how all our processes are now defined as `def processN(myState: Ref[IO, List[String]])`.

### A well known method: flatMap!

Now, we still have that `unsafeRunSync()` hanging around our code, how can we get rid of it? The answer is `flatMap`!!!

```scala
def masterProcess: IO[Unit] =
  Ref.of[IO, List[String]](List.empty[String]).flatMap { myState =>
    val ioa = List(process1(myState), process2(myState), process3(myState)).parSequence.void
    ioa *> myState.get.flatMap(rs => putStrLn(rs.toString))
  }
```

You only need to call `flatMap` once up in the call chain where you call the processes to make sure they all share the same state. If you don't do this, a new `Ref` will be created every time you `flatMap` (remember creating a `Ref` is side effectful!) and thus your processes will not be sharing the same state changing the behavior of your program.

We now have a purely functional code that shares state in a simple and pure fashion. Here's the entire FP program:

```scala
import cats.effect._
import cats.effect.concurrent.Ref
import cats.instances.list._
import cats.syntax.all._

import scala.concurrent.duration._

object sharedstate extends IOApp {

  def putStrLn(str: String): IO[Unit] = IO(println(str))

  def process1(myState: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("Starting process #1") *>
      IO.sleep(5.seconds) *>
      myState.update(_ ++ List("#1")) *>
      putStrLn("Done #1")
  }

  def process2(myState: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("Starting process #2") *>
      IO.sleep(3.seconds) *>
      myState.update(_ ++ List("#2")) *>
      putStrLn("Done #2")
  }

  def process3(myState: Ref[IO, List[String]]): IO[Unit] = {
    putStrLn("Starting process #3") *>
      IO.sleep(10.seconds) *>
      myState.update(_ ++ List("#3")) *>
      putStrLn("Done #3")
  }

  def masterProcess: IO[Unit] =
    Ref.of[IO, List[String]](List.empty[String]).flatMap { myState =>
      val ioa = List(process1(myState), process2(myState), process3(myState)).parSequence.void
      ioa *> myState.get.flatMap(rs => putStrLn(rs.toString))
    }

  override def run(args: List[String]): IO[ExitCode] =
    masterProcess.as(ExitCode.Success)

}
```

## Mutable reference

As I mentioned in one of the sections above, the creation of `Ref[F, A]` is side-effectful. So what does this mean? Does it write to disk? Does it perform HTTP Requests? Not exactly.

It all comes down to wanting to keep the property of *referential transparency* while sharing and mutating state. So let's again put up an example to follow up along with some explanation:

```scala
var a = 0
def set(n: Int) = a = n
def get: Int = a
```

Here we have imperative and impure code that mutates state. So we can try wrapping things in `IO` to keep side effects under control:

```scala
class IORef {
  var a: Int = 0
  def set(n: Int) = IO(a = n)
  def get: Int = IO(a)
}
```

This is way better since now the mutation is encapsulated within `IORef` but we are now pushing some resposibility to whoever creates an `IORef`. Consider this:

```scala
val ref = new IORef()
```

If we have two or more references to `ref` in our code, they will be referring to the same mutable state and we don't really want that. We can make sure this doesn't happen and a way to achieve this is to wrap the creation of `IORef` in `IO`:

```scala
private class IORef {
  var a: Int = 0
  def set(n: Int) = IO(a = n)
  def get: Int = IO(a)
}

object IORef {
  def apply: IO[IORef] = IO(new IORef)
}
```

We have now regained purity. So whenever you create an `IORef` you'll get an `IO[IORef]` instead of a mutable reference to `IORef`. This means that when you invoke `flatMap` on it twice you'll get two different mutable states, and this is the power of `Referential Transparency`. It gives you way more control than having a `val ref` hanging around in your code and gives you ***local reasoning***.

*All these examples are written in terms of `IO` for the sake of simplicity but in practice they are polymorphic on the effect type.*

## Applying the technique in other libraries

Although in the example above we only see how it's done with the `cats-effect` library, this principle expands to other FP libraries as well.

For example, when writing an `http4s` application you might need to create an `HttpClient` that needs to be used by more than one of your services. So again, create it at startup and `flatMap` it once:

```scala
object HttpServer extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    for {
      httpClient <- Http1Client.stream[IO]()
      endpoint1  = new HttpEndpointOne[IO](httpClient)
      endpoint2  = new HttpEndpointTwo[IO](httpClient)
      exitCode   <- BlazeBuilder[F]
                      .bindHttp(8080, "0.0.0.0")
                      .mountService(endpoint1)
                      .mountService(endpoint2)
                      .serve
    } yield exitCode

}

class HttpEndpointOne[F[_]](client: Client[F]) { ... }
class HttpEndpointTwo[F[_]](client: Client[F]) { ... }
```

When writing `fs2` applications you can apply the same technique, for example between processes that share a `Queue`, `Topic`, `Signal`, `Semaphore`, etc.

Remember that if you are forced to call `unsafeRunSync()` other than in your `main` method it might be a *code smell*.

## Conclusion

To conclude this post I would like to give a big shout out to [@SystemFW](https://github.com/SystemFw) who has been untiringly teaching this concept in the Gitter channels. And here's a quote from his response on [Reddit](https://www.reddit.com/r/scala/comments/8ofc8j/shared_state_in_pure_functional_programming_github/e050wy2/):

> At the end of the day all the benefits from *referential transparency* boil down to being able to understand and build code compositionally. That is, understanding code by understanding individual parts and putting them back together, and building code by building individual parts and combining them together. This is only possible if *local reasoning* is guaranteed, because otherwise there will be weird interactions when you put things back together, and referential transparency is *defined* as something that guarantees local reasoning.

> In the specific case of state sharing, this gives rise to a really nice property: since the only way to share is passing things as an argument, *the regions of sharing are exactly the same of your call graph*, so you transform an important aspect of the behaviour ("who shares this state?") into a straightforward syntactical property ("what methods take this argument"?). This makes shared state in pure FP a lot easier to reason about than its side-effectful counterpart imho.

In simple terms, remind yourself about this: **"flatMap once and pass the reference as an argument!"**
