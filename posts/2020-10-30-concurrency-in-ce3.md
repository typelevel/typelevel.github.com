---
layout: post
title: Concurrency in Cats Effect 3

meta:
  nav: blog
  author: rahsan

tut:
  scala: 2.12.11
  binaryScala: "2.12"
  scalacOptions:
    - -language:higherKinds
  dependencies:
    - org.typelevel::cats-effect:3.0.0-M1
---

Cats Effect 3 is just around the corner! The library has seen several major 
changes between 2.0 and 3.0, so in an effort to help the ecosystem migrate, 
we will be releasing a series of blog posts that cover topics ranging from 
the typeclass hierarchy to concurrent data structures to tracing. If you would 
like to see a blog post about a particular subject, don't hesitate to reach out
on Github or Gitter!

Writing concurrent code is notoriously one of the most difficult problems in
software engineering. 

Cats Effect presents a simple concurrency model that application and library
developers can write programs against without having to deal with a wide range
of problems. Crucially, many of these problems are eliminated because Cats
Effect and the greater ecosystem adhere to functional programming principles.
In fact, most application developers need not concern themselves with the
details of concurrency because libraries or frameworks will often abstract 
them away! However, having insight into such details like fibers and scheduling
can be crucial in understanding the runtime and scalability properties of an 
application.

In this post, we will focus on the concurrency model that underpins Cats 
Effect. We will look at examples of concurrent programs that are written in 
terms of the `cats.effect.IO` effect type implementation, however many of the 
concepts discussed in the post are shared with other effect types like Monix 
and ZIO. 

TODO: talk about how http4s abstracts concurrency from users

Before diving into Cats Effect, let's review what concurrency is, why it's
useful, and why it's hard. This will provide the motivation for the concurrency
model that we present later.

### Concurrency
Concurrent programming is about designing programs in which multiple logical
threads of control are executing. Logical threads are simply a description of a 
sequence of discrete steps. When writing programs in languages like Java
or C++, these "logical threads" are typically native threads that are scheduled 
by the operating system. When writing programs with a library like Cats Effect,
these "logical threads" are fibers, which we will talk about more later.

One crucial aspect of concurrency is nondeterministic execution. Multiple 
logical threads are executed in such a way that the effects of each thread are
interleaved in an indeterminate order. This interleaving is what enables
threads to execute "at the same time."

Another perspective is that concurrency generates a partial order 
among all the effects of all the logical threads in a program. Effects _within_
a logical thread are ordered with respect to program order. Effects _between_
multiple logical threads are generally not ordered unless there is some form
of synchronization between them.

#### Concurrency is useful
Concurrency is a necessary tool when designing high-performance applications
that are responsive and resilient. These applications typically involve 
multiple interactions or tasks that must happen at the same time. For example, 
a multiplayer game listens for keyboard input, renders graphics, plays sound 
effects, and communicates with a network server. Similarly, a web service 
processes many requests for many clients while communicating with other 
services, caches, and databases.

Traditional sequential programming models quickly become inadequate when 
applied to building responsive programs with multiple interactions. A
multiplayer game must perform all of the tasks described above at the same 
time; it wouldn't be a very good game if we couldn't respond to player input
while playing a sound! A web service that processes only one request at a time
before moving onto the next would exhibit very poor performance. Concurrency 
enables us to structure our program so that each interaction is confined to its
own logical thread, all of which execute at the same time.

Concurrency complements modular program design. Interactions like graphics
rendering and network communication are largely unrelated, so it would be 
tedious to design a program that constantly switches between rendering and
network socket operations. Instead of having one comple thread that performs 
every interaction, we can confine each interaction to one logical thread. This 
allows us to think about and code each interaction independently of eachother 
while having the assurance that they occur at the same time. The modularity that 
concurrency affords makes for programs that are much easier to understand, 
maintain, and evolve.

#### Concurrency is hard
Concurrency is notoriously responsible for 

bugs, race conditions, deadlocks

partial order, sequential consistency

One of the most popular concurrency constructs are OS threads, commonly
referred to as just "threads." Like its name suggests, threads are a type of
logical thread

Cats Effect takes the perpsective that while concurrency is an necessary 
technique for building useful applications, existing tools for achieving
concurrency are both unsafe and tedious to work with. One of the main goals of
Cats Effect is to give users a concurrency model that is safe and simple to 
use.

### Fibers
The concurrency model of Cats Effect is built upon fibers. A fiber is a 
logical thread that encapsulates the execution of an `IO[A]` program, which is 
a sequence of effects that are bound together via `flatMap`. Fibers can execute
concurrently with respect to each other.

TODO: Fibers are the most basic unit of concurrency in Cats Effect.

Concretely, the fiber of some effect `IO[A]` is represented by the type 
`FiberIO[A]`. The execution of a `FiberIO[A]` terminates with one of three 
possible outcomes, which are encoded by the datatype `OutcomeIO[A]`:

1. `Succeeded`: indicates success with a value of type `A`
2. `Errored`: indicates failure with a value of type `Throwable`
3. `Canceled`: indicates abnormal termination via cancellation

Additionally, a fiber may never produce an outcome, in which case it is said to 
be nonterminating.

The three basic actions on fibers are `start`, `join`, and `cancel`.

#### Starting fibers
The most basic action of concurrency in Cats Effect is to start or spawn a new
fiber. This requests to the scheduler to begin the concurrent execution of a
program `IO[A]` inside a new logical thread. The effects of the current fiber
and the spawned fiber are interleaved in a nondeterministic fashion.

Let's take a look at an example that demonstrates spawning and its 
nondeterministic behavior. In the following program, the main fiber spawns a 
second fiber that prints `A` 100 times and then prints `B` 100 times, after 
which it exits.

```scala
import cats.effect.{IO, ExitCode}
import cats.implicits._

object ExampleOne extends IOApp {
  def repeat(letter: String): IO[Unit] =
    IO.print(letter).replicateA(100)

  override def run(args: List[String]): IO[ExitCode]
    for {
      _ <- (repeat("A") *> repeat("B")).start
      _ <- (repeat("C") *> repeat("D"))
    } yield ExitCode.Success
}
```

Here is the output from an execution of the program on my computer:

```
AAAAAAAAAAACACACACACACACACACACACCCACACACACACACAACACACACACACACACACACACAACACACACACACACACACACACACAACACCACACAACACACACACACACACACACACACACACACACACACACACACACCACACACACACACACACACACACACACACACAACACACCCCBCBCBCBCBCBCBCBBCBCBDBDBDBBDDBDBDBDBDBDBDBDBBDBDBDBDBDBDBDBDBDBDBBDDBDBDBBDBDBBDBDDBBDDBBDBDBDBDDBDBDBDDBDBBDBDBBDBDBDBDDBDBDBDBDBDBDBDBDBDDBDBDBDBDBDBDBDBDBDBDBBDBDDBDBDBDBDBDBBDBDBDDBBDBDBDBDBDDDDDDDDDDDD
```

We can observe that there is no consistent ordering between the effects of
separate fibers; it is completely nondeterministic! However, the effects
_within_ a given fiber are always sequentially consistent, as dictated by 
program order; `A` is never printed after `B`, and `C` is never printed
after `D`.

TODO: should we talk about `racePair`?

#### Joining fibers
A fiber can wait on the result of another fiber by calling `FiberIO#join`. 
This semantically blocks the first fiber until the second fiber has 
terminated and then returns the outcome of that fiber.

```scala
import cats.effect.{IO, ExitCode}
import cats.implicits._

object ExampleTwo extends IOApp {
  def repeat(letter: String): IO[Unit] =
    IO.print(letter).replicateA(100)

  override def run(args: List[String]): IO[ExitCode]
    for {
      _ <- (repeat("A") *> repeat("B")).start
      _ <- (repeat("C") *> repeat("D"))
    } yield ExitCode.Success
}
```

Notice how `join` imposes an ordering on the execution of the two fibers: the 
main fiber will never start the rocket until after the countdown sequence has
completed.

#### Canceling fibers
A fiber can be canceled after its execution begins by calling `FiberIO#cancel`.
This semantically blocks the current fiber until the target fiber has 
finalized and terminated, and then returns the outcome of that fiber.

Cats Effect's cancellation model is mostly out-of-scope of this post, but it
will be discussed in detail in a future post. In the meantime, please visit the
Scaladoc for `MonadCancel` and `GenSpawn`.

cheap, context switching speed, pooling not necessary

#### Costs
Fibers are exceptionally lightweight compared to OS threads; 

### Scheduling
Fibers are multiplexed over a pool of OS threads. This is commonly referred to
as M-to-N scheduling or 

Preemptive multitasking
Cooperative multitasking

autoyielding
`cede`
spawning a fiber
safely spawning a fiber

### Communication
We have already seen how fibers can directly communicate with each other via
`start`, `join`, and `cancel`. Shared memory is an alternative means by which 
fibers can indirectly communicate and synchronize with each other.



#### `Ref`
`Ref` is a concurrent data structure that serves as a mutable container. It can
be thought of as the pure, functional equivalent of `AtomicReference`.

#### `Deferred`
`Deferred` is a concurrent data structure that serves as a condition variable.



`Ref` and `Deferred` are commonly used together to build all sorts of 
concurrent finite state machines.

### Parallelism
Parallelism is about simultaneous execution whereas concurrency is about
interleaved execution. Concurrency can be achieved both with and without
parallelism. JVM and JavaScript platforms as examples.

Concurrency can exploit parallelism, but parallelism is not necessary to
achieve concurrency. Concurrency is nondeterminsitic, but parallelism is
not necessarily nondeterministic.



TODO: background instead of start

## Special thanks

Fabio Labella for his Scala world talk


TODO: What is the mean idea of this post?
