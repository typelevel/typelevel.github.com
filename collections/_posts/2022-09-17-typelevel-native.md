---
layout: post
title: Typelevel Native
category: technical

meta:
  nav: blog
  author: armanbilge
---



#### What is Scala Native?

Scala Native is an optimizing ahead-of-time compiler for the Scala language. Put simply: it enables you to **compile Scala code directly to native executables**.

It is an ambitious project following in the steps of Scala.js. Instead of targeting JavaScript, the Scala Native compiler targets the [LLVM] IR and uses its toolchain to generate native executables for a range of architectures, including x86, ARM/M1, and in the near future web assembly.

#### Why is this exciting?

**For Scala in general**, funnily enough I think [GraalVM Native Image] does a great job summarizing the advantages of native executables, namely:
* instant startup that immediately achieves peak performance, without requiring warmup or the heavy footprint of the JVM
* packagable into small, self-contained binaries for easy deployment and distribution

It is worth mentioning that in benchmarks Scala Native handily beats GraalVM Native Image on startup time, runtime footprint, and binary size.

Moreover, breaking free from the JVM is an opportunity to design a runtime specifically optimized for the Scala language itself. This is the true potential of the Scala Native project.

**For Typelevel in particular**, Scala Native opens new doors for leveling up our libraries. 

W

I am also excited at the prospect of using Cats Effect to work with (non-Scala) native libraries exposing a C API. [`Resource`] and more generally [`MonadCancel`] provide the necessary combinators for safely navigating manual memory managment.

#### How does it work?

The burden of cross-building the Typelevel ecosystem for Scala Native falls to [Cats Effect] and mostly to [FS2].

From there, : any project

#### How can I try it?

Christopher Davenport has put up a [scala-native-ember-example](https://github.com/ChristopherDavenport/scala-native-ember-example) and reported some benchmark results!


#### What's next and how can I get involved?

* Please try the Typelevel Native stack! And even better deploy it, and do so loudly!
* Cross-building existing libraries and developing new, Typelevel stack ones
  - a pure Scala [gRPC] implementation built on http4s would be fantastic, even for the JVM. Christopher Davenport has published [proof-of-concept][grpc-playground].
  - the fledgling [otel4s] project will need pure Scala backends
* 
  - An [NGINX Unit] server backend for http4s promises exceptional performance
  - I kick-started [http4s-curl] 
* Developing I/O-integrated runtimes.
  - [epollcat] supports Linux and macOS and has plenty of opportunity for optimization.
  - A [libuv]-based runtime
  - 
* Tooling. Anton Sviridov has spear-headed two major projects in this area:
  - [sn-bindgen]
  - [sbt-vcpkg]

#### Ember example benchmark

```
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

[Cats Effect]: https://typelevel.org/cats-effect/
[epollcat]: https://github.com/armanbilge/epollcat
[FS2]: https://fs2.io/
[GraalVM Native Image]: https://www.graalvm.org/22.2/reference-manual/native-image/
[gRPC]: https://grpc.io/
[grpc-playground]: https://github.com/ChristopherDavenport/grpc-playground
[http4s]: https://http4s.org/
[http4s-curl]: https://github.com/http4s/http4s-curl/
[libuv]: https://github.com/libuv/libuv/
[LLVM]: https://llvm.org/
[NGINX Unit]: https://unit.nginx.org/
[io_uring]: https://en.wikipedia.org/wiki/Io_uring
[sbt-vcpkg]: https://github.com/indoorvivants/sbt-vcpkg/
[Scala Native]: https://scala-native.org/
[sn-bindgen]: https://github.com/indoorvivants/sn-bindgen
[`MonadCancel`]: https://typelevel.org/cats-effect/docs/typeclasses/monadcancel
[`Resource`]: https://typelevel.org/cats-effect/docs/std/resource