---
layout: post
title: Typelevel Native
category: technical

meta:
  nav: blog
  author: armanbilge
---

We recently published several major projects for the [Scala Native] platform, notably [Cats Effect], [FS2], and [http4s]. This blog post explores what this new platform means for the Typelevel ecosystem as well as how it works under-the-hood.

### What is Scala Native?

[Scala Native] is an optimizing ahead-of-time compiler for the Scala language. Put simply: it enables you to **compile Scala code directly to native executables**.

It is an ambitious project following in the steps of [Scala.js]. Instead of targeting JavaScript, the Scala Native compiler targets the [LLVM] IR and uses its toolchain to generate native executables for a range of architectures, including x86, ARM, and in the near future web assembly.

### Why is this exciting?

**For Scala in general**, funnily enough I think [GraalVM Native Image] does a great job summarizing the advantages of native executables, namely:
* instant startup that immediately achieves peak performance, without requiring warmup or the heavy footprint of the JVM
* packagable into small, self-contained binaries for easy deployment and distribution

It is worth mentioning that in benchmarks Scala Native handily beats GraalVM Native Image on startup time, runtime footprint, and binary size.

Moreover, breaking free from the JVM is an opportunity to design a runtime specifically optimized for the Scala language itself. This is the true potential of the Scala Native project.

**For Typelevel in particular**, Scala Native opens new doors for leveling up our ecosystem. Our flagship libraries are largely designed for deploying high performance I/O-bounded microservices and for the first time ever **we now have direct access to kernel I/O APIs**.

I am also enthusiastic to use Cats Effect with (non-Scala) native libraries that expose a C API. [`Resource`] and more generally [`MonadCancel`] are powerful tools for safely navigating manual memory management with all the goodness of error-handling and cancelation.

### How can I try it?

Christopher Davenport has put up a [scala-native-ember-example](https://github.com/ChristopherDavenport/scala-native-ember-example) and reported some [benchmark results](#ember-native-benchmark)!

### How does it work?

The burden of cross-building the Typelevel ecosystem for Scala Native fell almost entirely to [Cats Effect] and [FS2].

#### event loop runtime

**To cross-build Cats Effect for Native we had to get creative** because Scala Native currently does not support multithreading (although it will in the next major release). This is a similar situation to the JavaScript runtime, which is also fundamentally single-threaded. But an important difference is that JS runtimes are implemented with an [event loop] and offer callback-based APIs for scheduling timers and performing non-blocking I/O.

Meanwhile, Scala Native core does not implement an event loop nor offer such APIs. There is the [scala-native-loop] project, which wraps the [libuv] event loop runtime, but we did not want to bake such an opinionated dependency into Cats Effect core.

Fortunately Daniel Spiewak had the fantastic insight that the “dummy runtime” which I created to initially cross-build Cats Effect for Native could be reformulated into a legitimate event loop implementation by extending it with the capability to “poll” for I/O events: a `PollingExecutorScheduler`.

The [`PollingExecutorScheduler`] implements both [`ExecutionContext`] and [`Scheduler`] and maintains two queues:
- a queue of tasks (read: fibers) to execute 
- a priority queue of timers (read: `IO.sleep(...)`), sorted by expiration

It also defines an abstract method:
```scala
def poll(timeout: Duration): Boolean
```

The idea of this method is very similar to `Thread.sleep()` except that besides sleeping it may also “poll” for I/O events. It turns out that APIs like this are ubiquitous in C libraries that perform I/O.

To demonstrate the API contract, consider invoking `poll(3.seconds)`:

*I have nothing to do for the next 3 seconds. So wake me up then, or earlier if there is an incoming I/O event that I should handle. But wake me up no later!*

*Oh, and don’t forget to tell me whether there are still outstanding I/O events (`true`) or not (`false`) so I know if I need to call you again. Thanks!*

Thus, with tasks, timers, and the capability to poll for I/O, we can express the event loop algorithm. A single iteration of the loop looks like this:

1. Check the current time and execute any expired timers.

2. Execute up to 64 tasks, or until there are none left. We limit to 64 to ensure we are fair to timers and I/O.

3. Poll for I/O events. There are three cases to consider:
  - **There is at least one task to do.** Call `poll(0.nanos)`, so it will process any available I/O events and then immediately return control.
  - **There is at least one outstanding timer**. Call `poll(durationToNextTimer)`, so it will sleep until the next I/O event arrives or the timeout expires, whichever comes first.
  - **There are no tasks to do and no outstanding timers.**  Call `poll(Duration.Infinite)`, so it will sleep until the next I/O event arrives.

This algorithm is not a Cats Effect original: the [libuv event loop] works in essentially the same way. It is however a first step towards to the much grander Cats Effect [I/O Integrated Runtime Concept]. The big idea is that every `WorkerThread` in the `WorkStealingThreadPool` that underpins the Cats Effect JVM runtime can run an event loop exactly like the one described above, for exceptionally high-performance I/O.

#### non-blocking I/O

**So, how do we implement `poll`?** The bad news is that the answer is OS-specific, which is a large reason why projects such as libuv exist. Furthermore, the entire purpose of polling is to support non-blocking I/O, which falls outside of the scope of Cats Effect. This brings us to FS2, and specifically the [`fs2-io`] module where we want to implement non-blocking TCP [`Socket`]s.

One such polling API is [epoll], available only on Linux:

```c
#include <sys/epoll.h>

int epoll_create1(int flags);

int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);

int epoll_wait(int epfd, struct epoll_event *events,
                      int maxevents, int timeout);
```

After creating an epoll instance (identified by a file descriptor) we can register sockets (also identified by file descriptors) with `epoll_ctl`. Typically we will register to be notified of the “read-ready” (`EPOLLIN`) and “write-ready” (`EPOLLOUT`) events on that socket. Finally, the actual polling is implemented with `epoll_wait`, which sleeps until the next I/O event is ready or the `timeout` expires.

As previously mentioned, these sorts of polling APIs are ubiquitous and not just for working directly with sockets. For example, [libcurl](https://curl.se/libcurl/) (the C library behind the well-known CLI) exposes a function for polling for I/O on all ongoing HTTP connections.

```c
#include <curl/curl.h>
 
CURLMcode curl_multi_poll(CURLM *multi_handle,
                          struct curl_waitfd extra_fds[],
                          unsigned int extra_nfds,
                          int timeout_ms,
                          int *numfds);
```

Indeed, this function underpins the `CurlExecutorScheduler` in [http4s-curl].

On macOS and BSDs the [kqueue] API plays an analogous role to epoll. We will not talk about Windows today :)

Long story short, I did not want the FS2 codebase to absorb all of this cross-OS complexity. So in collaboration with Lee Tibbert we repurposed my cheeky [epollcat] experiment into an actual library implementing JDK NIO APIs (specifically, [`AsynchronousSocketChannel`] and friends). Since these are the same APIs used by the JVM implementation of `fs2-io`, it actually enables this code to be completely shared.

[epollcat] implements an `EpollExecutorScheduler` for Linux and a `KqueueExecutorScheduler` for macOS. They additionally provide an API for monitoring a socket file descriptor for read-ready and write-ready events.

```scala
def monitor(fd: Int, reads: Boolean, writes: Boolean)(
  cb: EventNotificationCallback
): Runnable // returns a `Runnable` to un-monitor the file descriptor

trait EventNotificationCallback {
  def notifyEvents(readReady: Boolean, writeReady: Boolean): Unit
}
```

These are then used to implement the callback-based `read` and `write` methods of the JDK `AsynchronousSocketChannel`.

It is worth pointing out that the JVM actually implements `AsynchronousSocketChannel` with an event loop as well.  The difference is that on the JVM, this event loop is used only for I/O and runs on a separate thread from the compute pool used for fibers and the scheduler thread used for timers. Meanwhile, epollcat is an example of an I/O integrated runtime where fibers, timers, and I/O are all managed on a single thread.

#### TLS

**The last critical piece of the cross-build puzzle was a [TLS] implementation** for [`TLSSocket`] and related APIs in FS2. Although the prospect of this was daunting, in the end it was actually fairly straightforward to directly integrate with [s2n-tls], which exposes a well-designed and well-documented C API. This is effectively the only non-Scala dependency required to use the Typelevel stack on Native.

Finally, special thanks to Ondra Pelech and Lorenzo Gabriele for cross-building [scala-java-time] and [scala-java-locales] for Native and David Strawn for developing [idna4s]. These projects fill important gaps in the Scala Native re-implementation of the JDK that were essential to seamless cross-building.

And ... that is pretty much it. **From here, any library or application that is built using Cats Effect and FS2 cross-builds for Scala Native effectively for free.** Three spectacular examples of this are:

* [http4s] Ember, a server+client duo with HTTP/2 support
* [Skunk], a Postgres client
* [rediculous], a Redis client

These libraries in turn unlock projects such as [feral], [Grackle], and [smithy4s].

### What’s next and how can I get involved?

Please try the Typelevel Native stack! And even better deploy it, and do so loudly!

Besides that, here is a brain-dump of project ideas and existing projects that would love contributors. I am happy to help folks get started on any of these, or ideas of your own!

* Creating example applications, templates, and tutorials.
  - If you are lacking inspiration, try cross-building existing examples such as [fs2-chat], [kitteh-redis], [Jobby].
  - Spread the word: [you-forgot-a-percentage-sign-or-a-colon].

* Cross-building existing libraries and developing new, Typelevel-stack ones:
  - Go [feral] and implement a pure Scala [custom AWS Lambda runtime] that cross-builds for Native.
  - A pure Scala [gRPC] implementation built on http4s would be fantastic, even for the JVM. Christopher Davenport has published a [proof-of-concept][grpc-playground].
  - [fs2-data] has pure Scala support for a plethora of data formats. The [http4s-fs2-data] integration needs your help to get off the ground!
  - Lack of cross-platform cryptography is one of the remaining sore points in cross-building. I started the [bobcats] project to fill the gap but I am afraid it needs love from a more dedicated maintainer.

* Integrations with native libraries:
  - I kick-started [http4s-curl] and would love to see someone take the reigns!
  - An [NGINX Unit] server backend for http4s promises exceptional performance. [snunit] pioneered this approach.
  - Using [quiche] for HTTP/3 looks yummy!
  - An idiomatic wrapper for [SQLite]. See also [davenverse/sqlite-sjs#1] which proposes cross-platform API backed by Doobie on the JVM.

* Developing I/O-integrated runtimes:
  - [epollcat] supports Linux and macOS and has plenty of opportunity for optimization and development.
  - A [libuv]-based runtime would have solid cross-OS support, including Windows. Prior art in [scala-native-loop].
  - Personally I am excited to work on an [io_uring] runtime.

* Tooling. Anton Sviridov has spear-headed two major projects in this area:
  - [sbt-vcpkg] is working hard to solve the native dependency problem.
  - [sn-bindgen] generates Scala Native bindings to native libraries directly from `*.h` header files. I found it immensely useful while working on http4s-curl, epollcat, and the s2n-tls integration in FS2.
  - Also: we are _badly_ in need of a pure Scala port of the [Java Microbenchmark Harness]. Not the whole thing obviously, but just enough to run the existing Cats Effect benchmarks for example.

* Scala Native itself. Lots to do there!

### Ember native benchmark

```console
$ hey -z 30s http://localhost:8080

Summary:
  Total:    30.0160 secs
  Slowest:    0.3971 secs
  Fastest:    0.0012 secs
  Average:    0.0131 secs
  Requests/sec:    3815.4647

  Total data:    1145250 bytes
  Size/request:    10 bytes

Response time histogram:
  0.001 [1]    |
  0.041 [114486]    |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.080 [7]    |
  0.120 [5]    |
  0.160 [5]    |
  0.199 [3]    |
  0.239 [5]    |
  0.278 [3]    |
  0.318 [4]    |
  0.357 [3]    |
  0.397 [3]    |


Latency distribution:
  10% in 0.0119 secs
  25% in 0.0121 secs
  50% in 0.0122 secs
  75% in 0.0125 secs
  90% in 0.0133 secs
  95% in 0.0224 secs
  99% in 0.0234 secs

Details (average, fastest, slowest):
  DNS+dialup:    0.0000 secs, 0.0012 secs, 0.3971 secs
  DNS-lookup:    0.0000 secs, 0.0000 secs, 0.0011 secs
  req write:    0.0000 secs, 0.0000 secs, 0.0013 secs
  resp wait:    0.0131 secs, 0.0011 secs, 0.3941 secs
  resp read:    0.0000 secs, 0.0000 secs, 0.0010 secs

Status code distribution:
  [200]    114525 responses
```

[`AsynchronousSocketChannel`]: https://docs.oracle.com/javase/8/docs/api/java/nio/channels/AsynchronousSocketChannel.html
[bobcats]: https://github.com/typelevel/bobcats
[Cats Effect]: https://typelevel.org/cats-effect/
[custom AWS Lambda runtime]: https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
[davenverse/sqlite-sjs#1]: https://github.com/davenverse/sqlite-sjs/pull/1
[`ExecutionContext`]: https://www.scala-lang.org/api/2.13.8/scala/concurrent/ExecutionContext.html
[event loop]: https://javascript.info/event-loop
[epoll]: https://man7.org/linux/man-pages/man7/epoll.7.html
[epollcat]: https://github.com/armanbilge/epollcat
[feral]: https://github.com/typelevel/feral
[FS2]: https://fs2.io/
[fs2-chat]: https://github.com/typelevel/fs2-chat/
[fs2-data]: https://github.com/satabin/fs2-data/
[`fs2-io`]: https://fs2.io/#/io
[GraalVM Native Image]: https://www.graalvm.org/22.2/reference-manual/native-image/
[Grackle]: https://github.com/gemini-hlsw/gsp-graphql
[gRPC]: https://grpc.io/
[grpc-playground]: https://github.com/ChristopherDavenport/grpc-playground
[http4s]: https://http4s.org/
[http4s-curl]: https://github.com/http4s/http4s-curl/
[http4s-fs2-data]: https://github.com/http4s/http4s-fs2-data
[idna4s]: https://github.com/typelevel/idna4s
[I/O Integrated Runtime Concept]: https://github.com/typelevel/cats-effect/discussions/3070
[io_uring]: https://en.wikipedia.org/wiki/Io_uring
[Jobby]: https://github.com/keynmol/jobby/
[kitteh-redis]: https://github.com/djspiewak/kitteh-redis
[kqueue]: https://www.freebsd.org/cgi/man.cgi?query=kqueue&sektion=2
[Java Microbenchmark Harness]: https://github.com/openjdk/jmh
[libuv]: https://github.com/libuv/libuv/
[libuv event loop]: https://docs.libuv.org/en/v1.x/design.html#the-i-o-loop
[libcurl]: https://curl.se/libcurl/
[LLVM]: https://llvm.org/
[`MonadCancel`]: https://typelevel.org/cats-effect/docs/typeclasses/monadcancel
[NGINX Unit]: https://unit.nginx.org/
[`PollingExecutorScheduler`]: https://github.com/typelevel/cats-effect/blob/7ca03db50342773a79a01ecf137d953408ac6b1d/core/native/src/main/scala/cats/effect/unsafe/PollingExecutorScheduler.scala
[quiche]: https://github.com/cloudflare/quiche
[rediculous]: https://github.com/davenverse/rediculous
[`Resource`]: https://typelevel.org/cats-effect/docs/std/resource
[sbt-vcpkg]: https://github.com/indoorvivants/sbt-vcpkg/
[ScalablyTyped]: https://scalablytyped.org/
[Scala Native]: https://scala-native.org/
[Scala.js]: https://www.scala-js.org/
[scala-java-locales]: https://github.com/cquiroz/scala-java-locales
[scala-java-time]: https://github.com/cquiroz/scala-java-time
[scala-native-loop]: https://github.com/scala-native/scala-native-loop/
[`Scheduler`]: https://github.com/typelevel/cats-effect/blob/236a0db0e95be829de34d7a8e3c06914738b7b06/core/shared/src/main/scala/cats/effect/unsafe/Scheduler.scala
[Skunk]: https://github.com/tpolecat/skunk
[smithy4s]: https://disneystreaming.github.io/smithy4s/
[`Socket`]: https://www.javadoc.io/doc/co.fs2/fs2-docs_2.13/latest/fs2/io/net/Socket.html
[SQLite]: https://www.sqlite.org/index.html
[snunit]: https://github.com/lolgab/snunit
[sn-bindgen]: https://github.com/indoorvivants/sn-bindgen
[s2n-tls]: https://github.com/aws/s2n-tls
[TLS]: https://en.wikipedia.org/wiki/Transport_Layer_Security\
[`TLSSocket`]: https://www.javadoc.io/doc/co.fs2/fs2-docs_2.13/latest/fs2/io/net/tls/TLSSocket.html
[you-forgot-a-percentage-sign-or-a-colon]: https://youforgotapercentagesignoracolon.com/
