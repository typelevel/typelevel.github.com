---
layout: post
title: Typelevel Toolkit
category: technical

meta:
  nav: blog
  author: zetashift
  pygments: true
---

Getting started in the wondrous world of functional programming using [Typelevel libraries](https://typelevel.org/projects) can be daunting. Before you can even write your first pure "Hello, World!" you'll need to install a Java runtime, editor tooling and build tools. Then you'll need to setup some project using [sbt]() or [mill](). As an added consequence, after all the setup, the idea of using these battle-tested libraries for small scripts will seem like a chore. This is where [Typelevel Toolkit](https://typelevel.org/toolkit/) comes in. It provides an easy start for beginning and experienced developers with Scala and functional programming.

# `scala-cli` to the rescue

[scala-cli](https://scala-cli.virtuslab.org/) is a command-line interface to quickly develop and experiment with Scala, it's even on track to [becoming the new scala command](https://docs.scala-lang.org/sips/scala-cli.html). The interface has a lot of advantages, but one of the most important ones is that it makes learning, developing and building Scala scripts and small applications friction-less and easy to use.

You can get `scala-cli` by following the installation instructions described here: [https://scala-cli.virtuslab.org/docs/overview](https://scala-cli.virtuslab.org/docs/overview#installation)#installation. The great part here is that once you have `scala-cli` installed, it will take care of the rest: Java runtimes, editor tooling, compilation targets(including a native target!) and you can even use dependencies in your scripts!

## An example of setting up your script

First off, let's create a new directory that will contain our script(s).

```sh
mkdir myscript && cd myscript
```

Now we can use `scala-cli` to create all the files necessary for editor tooling to work:

```sh
scala-cli setup-ide .
```

Finally, let's create a Scala file and try to compile it.

```sh
touch Main.scala
scala-cli compile --watch Main.scala
```


The last command also includes a `--watch` flag, so we can hack on our script and `scala-cli` will try to compile the file on every save.
This creates a very nice feedback loop!

# Putting the fun in functional

[Typelevel Toolkit](https://typelevel.org/toolkit/) uses `scala-cli` and Typelevel libraries to provide a runway for your next Scala script or command-line interface. You can use the toolkit with `scala-cli` with just a single line and you'll get [Cats Effect](https://typelevel.org/cats-effect/), [fs2](https://fs2.io) and a few other libraries to develop scripts quickly using pure functional programming. 

Typelevel Toolkit shines with `scala-cli`, but it can also be used by sbt or mill if that is preferred.
More concretely this means your next ad-hoc script won't be Bash or Python spaghetti, but Scala code that can be a joy to hack on as time goes on, without the boilerplate.

You can use the toolkit by using a
[scala-cli directive](https://scala-cli.virtuslab.org/docs/guides/using-directives)

```scala
//> using dep "org.typelevel::toolkit::0.0.4"
```

This will pull in the `typelevel/toolkit` dependency and then you're just an import away from your first pure functional "Hello, World!":

```scala
import cats.effect.*

object Hello extends IOApp.Simple {
  def run = IO.println("Hello, World!")
}
```

You can compile and run this program by using a single command: `scala-cli run Main.scala`.


A "Hello, World!" is only the start, the goal here is to make functional programming friendly and practical. As such, Typelevel toolkit comes with [examples](https://typelevel.org/toolkit/examples.html) that introduces beginners on how one can use the included libraries to achieve common tasks.


For the full list of libraries included in Typelevel Toolkit, please see the overview: [https://typelevel.org/toolkit/#overview](https://typelevel.org/toolkit/#overview). If you feel like anything is missing, [join the discussion](https://github.com/typelevel/toolkit/issues/1).

# We can have nice things

Typelevel libraries are production-proven, well tested, build upon rock solid semantics, and almost all have Scala 3 support.
However their entry-point is higher than your usual scripting language. Pure functional programming has a reputation of being hard to learn, and Typelevel Toolkit is a way to play in that world, without learning an entire ecosystem first.

The toolkit includes, among others, a [HTTP client](https://http4s.org/), [CSV parsing library](https://fs2-data.gnieh.org/documentation/csv/) and functions for [handling files cross-platform](https://fs2.io/#/io), **with support for other runtimes besides the JVM**. This means that your scripts can run in a [JavaScript environment](https://scala-cli.virtuslab.org/docs/guides/scala-js), thanks to [scala-js](https://www.scala-js.org/). Or you can use [scala-native](https://github.com/scala-native/scala-native) to get a [native binary](https://scala-cli.virtuslab.org/docs/guides/scala-native), just like Rust and Go!

`scala-cli` again, makes things easy for us by having simple commands to compile to a certain target:

```sh
# To compile to JavaScript
scala-cli Main.scala --js  

# To target native
scala-cli Main.scala --native
```

If you just want to explore Typelevel Toolkit, you can quickly open a REPL using the following command:

```sh
scala-cli repl --dep "org.typelevel::toolkit::0.0.4"
```

With `scala-cli`, there a few other cool things you get here:
- [export current build into sbt or mill](https://scala-cli.virtuslab.org/docs/guides/sbt-mill)
- [create and share gists](https://scala-cli.virtuslab.org/docs/cookbooks/gists)
- [package your script using GraalVM](https://scala-cli.virtuslab.org/docs/cookbooks/native-images)

# Summary

`scala-cli` is great. It's easy to get started with and great to use. Typelevel Toolkit leverages its versatility and provides a "pure functional standard library" in a single directive. This will enable you to create and develop scripts fast with high refactorability, an awesome developer experience and lots of functions that compose well! All those benefits while remaining beginner-friendly.