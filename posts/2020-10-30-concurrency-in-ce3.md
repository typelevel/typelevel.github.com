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

### Introduction

Rewrite the following paragram

Cats Effect presents a simple concurrency model that application and library
developers can write programs against without having to deal with a wide range
of problems. Crucially, many of these problems are eliminated because Cats
Effect and the greater ecosystem adhere to functional programming principles.
In fact, most application developers need not concern themselves with the
details of concurrency because libraries or frameworks will often abstract 
them away! However, having insight into such details like fibers and scheduling
can be crucial in understanding the runtime and scalability properties of an 
application.

In this post, we will focus on the concurrency model that serves as the 
foundation for Cats Effect and many of its abstractions. We will look at 
examples of concurrent programs that are written in terms of the 
`cats.effect.IO` effect type implementation, but many of the concepts we
discuss are not exclusive to `IO` and are shared by other effect types.

TODO: talk about how http4s abstracts concurrency from users

Before going any further, let's briefly review what concurrency is, why it is
useful, and why it is notoriously tedious to work with. 

### Concurrency
Concurrent programming is about designing programs in which multiple logical
threads of control are executing at the same time. There is a bit to unpack 
in this definition: what are logical threads and what does it mean for them to 
execute "at the same time?"

TODO: description of a sequence? discrete effects

A logical thread is merely a description of a sequence of discrete steps. When 
writing programs in traditional high-level languages like Java or C++, these 
"logical threads" are typically represented by native threads that are managed 
by the operating system. The steps that comprise these threads are native 
processor instructions or VM bytecode instructions. When writing programs with
Cats Effect, these "logical" threads are represented by fibers, and the steps
that comprise them are `IO` instructions.

What does it mean for logical threads to execute at the same time? Because the 
steps of logical thread are discrete, the steps of many logical threads can be 
interleaved together. This interleaving is largely influenced by external 
factors such as scheduler preemption and I/O operations, so it is usually 
nondeterministic, that is, in the absence of synchronization, there is no 
guarantee that the effects of distinct logical threads occur in some particular
order.

TODO: possibly talk about parallelism

TODO: possibly delete this next paragraph

Another perspective is that concurrency generates a partial order 
among all the effects of all the logical threads in a program. Effects _within_
a logical thread are ordered with respect to program order. Effects _between_
multiple logical threads are not ordered unless there is some form of 
synchronization between them.

#### Concurrency is useful
Concurrency is a necessary tool for designing high-performance applications
that are responsive and resilient. These applications typically involve 
multiple interactions or tasks that must happen at the same time. For example, 
a computer game needs to listen for keyboard input, play sound effects, and 
run game loop logic, all while rendering graphics to the screen. A multiplayer 
game must also communicate with a network server to exchange game state 
information.

Traditional sequential programming models quickly become inadequate when 
applied to building these kinds of responsive programs. The computer game 
program must perform all of the tasks described above at the same time; it 
wouldn't be a very good game if we couldn't respond to player input while 
playing a sound! Concurrency enables us to structure our program so that 
each interaction is confined to its own logical thread(s), all of which 
execute at the same time.

Concurrency complements modular program design. Interactions like graphics
rendering and network communication are largely unrelated; it would be tedious
and messy to design a program that constantly switches between graphics and
audio I/O operations. Instead of having one complex thread that performs 
every interaction, we can confine each interaction to its own logical thread. 
This allows us to think about and code each interaction independently of each 
other while having the assurance that they occur at the same time. The 
modularity that concurrency affords makes for programs that are much easier to 
understand, maintain, and evolve.

#### Concurrency is hard
Concurrency is notoriously responsible for 

bugs, race conditions, deadlocks

partial order, sequential consistency
blocking threads
Scala future and asynchronicity

One of the most popular concurrency constructs are OS threads, commonly
referred to as just "threads." Like its name suggests, threads are a type of
logical thread

Cats Effect takes the perpsective that concurrency is an necessary technique 
for building useful applications, but existing tools for achieving concurrency 
are largely unsafe and tedious to work with. One of the main goals of Cats 
Effect is to provide users a concurrency model that is safe and simple to use.

### Fibers
The concurrency model of Cats Effect is built upon fibers rather than (native) 
threads. Unlike native threads, fibers are not associated with any system 
resources; they exist and are scheduled completely within the userspace 
process. These fibers typically run on a small pool of native threads. This mode 
of concurrency reaps several major benefits:

1. Fibers are incredibly cheap to create so we can create hundreds of 
thousands of them without thrashing.
2. It is extremely fast to context-switch between fibers.
3. Blocking a fiber doesn't necessarily block a native thread; this is called
semantic blocking.

TODO: Fibers are the most basic unit of concurrency in Cats Effect.

Concretely, a fiber is a logical thread that encapsulates the execution of an 
`IO[A]` program, which is a sequence of `IO` effects that are bound together 
via `flatMap`. Fibers are logical threads, so they execute concurrently.

The active execution of some fiber of an effect `IO[A]` is represented by the 
type `FiberIO[A]`. The execution of a `FiberIO[A]` terminates with one of three 
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

Let's take a look at an example that demonstrates spawning and the interleaving 
of multiple fibers. In the following program, the main fiber spawns a second 
fiber that prints `A` 100 times and then prints `B` 100 times, after which it
exits.

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

Here is one possible output from an execution of the program:

```
AAAAAAAAAAACACACACACACACACACACACCCACACACACACACAACACACACACACACACACACACAACACACACACACACACACACACACAACACCACACAACACACACACACACACACACACACACACACACACACACACACACCACACACACACACACACACACACACACACACAACACACCCCBCBCBCBCBCBCBCBBCBCBDBDBDBBDDBDBDBDBDBDBDBDBBDBDBDBDBDBDBDBDBDBDBBDDBDBDBBDBDBBDBDDBBDDBBDBDBDBDDBDBDBDDBDBBDBDBBDBDBDBDDBDBDBDBDBDBDBDBDBDDBDBDBDBDBDBDBDBDBDBDBBDBDDBDBDBDBDBDBBDBDBDDBBDBDBDBDBDDDDDDDDDDDD
```

We can observe that there is no consistent ordering between the effects of
separate fibers; it is completely nondeterministic! However, the effects
_within_ a given fiber are always sequentially consistent, as dictated by 
program order; `A` is never printed after `B`, and `C` is never printed
after `D`.

TODO: should we talk about `racePair`?
TODO: should we combine the join and cancel sections?

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

### Communication
We have already seen how fibers can directly communicate with each other via
`start`, `join`, and `cancel`. Shared memory is an alternative means by which 
fibers can indirectly communicate and synchronize with each other.



#### `Ref`
`Ref` is a concurrent data structure that represents a mutable container for
state. It can be thought of as the pure, functional equivalent of 
`AtomicReference`.

#### `Deferred`
`Deferred` is a concurrent data structure that represents a condition variable.
It can be used as a way to semantically block fibers until some arbitrary
condition has been fulfilled.



`Ref` and `Deferred` are commonly used together to build all sorts of 
concurrent finite state machines.


### Scheduling
In this section, we cover some of the internals of Cats Effect's `IO` runtime
system. In particular, we will look at how fibers are scheduled on JVM and JS 
runtimes.

Fibers are multiplexed over a pool of OS threads. This is commonly referred to
as M-to-N scheduling or 

Preemptive multitasking
Cooperative multitasking

autoyielding
`cede`
spawning a fiber
safely spawning a fiber


TODO: should we move this to the beginning?

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
