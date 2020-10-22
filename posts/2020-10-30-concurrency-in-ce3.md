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
Effect. We will look at examples that are written in terms of the 
`cats.effect.IO` effect type implementation, however many of the concepts 
discussed in the post are shared with other effect types like Monix and ZIO.

## Concurrency
The word "concurrency" is very much polysemic in nature.

Concurrency as a program structuring technique is an incredibly powerful tool
for writing programs with multiple interactions. Imagine we were writing a web
server 

It enables us design these kinds of programs in a modular fashion where each 
interaction is described in isolation. This separation is crucial; we can code
each interaction independently. The runtime is then responsible for executing or 
evaluating the effects of each interaction in such a way that they appear to 
happen at the same time. 



Talk about threads, cost, pooling
Talk about Scala Future

## Fibers
A fiber is a logical thread, or a sequence of effects which are bound together
via `flatMap`. 

Fibers are a similar construct to goroutines in Go or userland threads 
elsewhere.

## Scheduling

Preemptive multitasking
Cooperative multitasking

autoyielding
spawning a fiber
safely spawning a fiber

## Parallelism

Parallelism is about simultaneous execution whereas concurrency is about
interleaved execution. Concurrency can be achieved both with and without
parallelism. JVM and JavaScript platforms as examples.

## Special thanks

Fabio Labella for his Scala world talk
