---
layout: post
title: Towards Scalaz (Part 2)

meta:
  nav: blog
  author: adelbertc
  pygments: true
---

A lot of people see Scalaz as a hard fringe, ivory tower,
not suited for real-world applications library, which is
unfortunate. The goal of this blog post series is to introduce
various components of Scalaz, and hopefully through this
allow folks to gain an understanding towards the power of
Scalaz.

As a prerequisite, I assume knowledge of type classes as they
are implemented and used in Scala, higher kinded types,
and sum types (e.g. `Option/Some/None`, `Either/Left/Right`).

For a tutorial/review on (higher) kinds, I recommend the following resources:

* [Scala: Types of a higher kind](http://blogs.atlassian.com/2013/09/scala-types-of-a-higher-kind/)
* [Generics of a Higher Kind](http://adriaanm.github.io/files/higher.pdf)
* [SO: What is a higher kinded type in Scala?](http://stackoverflow.com/questions/6246719/what-is-a-higher-kinded-type-in-scala)

## Part 2: Summations of a Higher Kind

[Last time]({% post_url 2013-10-13-towards-scalaz-1 %}) we left off after
writing our own generic `sum` function:

```scala
import scalaz.Monoid

def sumGeneric[A](l: List[A])(implicit A: Monoid[A]): A =
  l.foldLeft(A.zero)((x, y) => A.append(x, y))
```

This allowed us to sum a list not only of numeric types like
`Int`, but also others that could be added and had a "zero" such as
`String` via string concatenation and the empty string, as well as
`List[A]` via list concatenation and the empty list.

But, we can do better! Why limit ourselves to `List`? What if we want
to sum over a `Vector`, or even a tree? We *could* use `Seq` and that
would allow us to pass in `List` or `Vector`, but it still brings up
the problem of trees, and any other data structure that may not fit
the `Seq` bill.

### What do we want? Folds!
Recall when we "came up with" `Semigroup` and `Monoid` last time -
what did we do? We simply looked at what operations we needed
(`add/append` and `zero`) and factored it out into a type class.
Let's try doing the same this time.

So what are we doing with `List` in our implementation? Nothing much
really, we're just folding over it. If we think about it, we could
"fold" over say, a tree as well. Let's take this operation out into
a type class, and aptly name it `Foldable`.

```scala
trait Foldable[F[_]] {
  // Instead of requiring the contents to be monoidal, let's
  // make it flexible by allowing a fold as long as we can convert
  // the contents to a type that has a `Monoid`.
  def foldMap[A, B](fa: F[A])(f: A => B)(implicit B: Monoid[B]): B
}
```

And let's implement instances of this type class for `List` and our own
`Tree`.

Our tree definition:

```scala
sealed trait Tree[A]
case class Node[A](value: A, left: Tree[A], right: Tree[A]) extends Tree[A]
case class Leaf[A]() extends Tree[A]
```

and our instances:

```scala
object Foldable {
  implicit val listIsFoldable: Foldable[List] =
    new Foldable[List] {
      def foldMap[A, B](fa: List[A])(f: A => B)(implicit B: Monoid[B]): B =
        fa.foldLeft(B.zero)((acc, elem) => B.append(acc, f(elem)))
    }

  implicit val treeIsFoldable: Foldable[Tree] =
    new Foldable[Tree] {
      def foldMap[A, B](fa: Tree[A])(f: A => B)(implicit B: Monoid[B]): B =
        fa match {
          case Leaf() =>
            B.zero
          case Node(value, left, right) =>
            B.append(f(value), B.append(foldMap(left)(f), foldMap(right)(f)))
        }
    }
}
```

and finally, our new summing function:

```scala
def sumGeneric[F[_], A](fa: F[A])(implicit F: Foldable[F], A: Monoid[A]): A =
  fa.foldMap(identity)
```

### Scalaz to the rescue
As with last time, Scalaz defines the `Foldable` type class for us. However,
to really be "foldable", not only should you define `foldMap`, but `foldRight`
as well. Some of you may be wondering why `foldRight` and not `foldLeft`, or both?
The reasons for this decision are that

* `foldLeft` can be defined in terms of `foldRight` (a fun exercise is to try this for yourself)
* `foldLeft` fails on infinite lists (think `Stream` in Scala)

That being said, Scalaz defines instances of `Foldable` for many of the standard
Scala types (`List`, `Vector`, `Stream`, `Option`), as well as its own (`Tree`, `EphemeralStream`).
The methods available on the type class not only include `foldMap` and `foldRight` which
are required to be implemented, but several derived ones as well including `fold` (`foldMap` with
`identity`), `foldLeft`, `toList/IndexedSeq/Stream`, among others.

So our code with `scalaz.Foldable` now looks like:

```scala
import scalaz.{ Foldable, Monoid }

// Note that this is equivalent to scalaz.Foldable#fold
def sumGeneric[F[_], A](fa: F[A])(implicit F: Foldable[F], A: Monoid[A]): A =
  fa.fold
```

Note that the implementation of the function is rather plain, but that's a good thing!
This shows the level of genericity type classes, folds,  and Scalaz is capable of. If you ever
find yourself needing to fold something down, look at the methods available on
`scalaz.Foldable`. By simply adding an instance of `Foldable` to your `F[_]` by implementing
the two methods above, you get "for free" a bunch of
[derived ones](http://docs.typelevel.org/api/scalaz/stable/7.0.4/doc/#scalaz.Foldable)!

### An Aside: Taming the Elephant
In recent days, the word "Hadoop" has become synonymous with "big data." The MapReduce
system made popular by [Google](http://research.google.com/archive/mapreduce.html)
has made it's way into several companies looking to glean information from their data.

Why am I mentioning this in a typelevel.scala blog post? Well, think about the reduce phase –
what is really happening? For a particular key, we're given a list of values emitted
for that key, and we want to reduce those values into a single value. Sound familiar?
Sounds a bit like `fold`, doesn't it? Note that not all reductions in MapReduce have to follow
monoid laws, but a surprising amount do as demonstrated by Twitter's
[Algebird](https://github.com/twitter/algebird) project.

Going back to `fold`, recall that in order to just `fold` we need to have something
`Foldable` that contains something that already has a `Monoid` instance. A more general
approach, as taken by `scalaz.Foldable`, is to also provide a `foldMap` function which
lets us also pass in a function *map*ping each element of the `Foldable` to something
that is a `Monoid`, and reduce over that instead.

So. Given something, say a `List[A]`, we want to *Map* each element of the list to
an element of a type that has a `Monoid` instance, and then we want to *Reduce* the
list down to a single value. What is this? All together now: MapReduce!

Unfortunately, Hadoop MapReduce by itself does not give you anything like a `List`.
Fortunately, our good friends at [NICTA](http://www.nicta.com.au/) have developed
and open sourced the wonderful [Scoobi](https://github.com/nicta/scoobi) project,
which abstracts over Hadoop MapReduce by providing a `List`-like interface, called a
`DList` (distributed list). Users treat the `DList` very similarly to how they would
a regular Scala `List`, and perform operations on it that get compiled down into
MapReduce jobs. Such operations include not only the familiar (and expected) `map`
and `reduce` combinators, but also our friends `foldMap` and `fold`. While `DList`'s
do not have a proper `Foldable` instance due to the difficulty of implementing `foldRight`
for the MapReduce, I find it to be a great example of the power of abstractions and
genericity abstract algebra and Scalaz provides to us as programmers.

## Further Reading

* [List Folds at BFPG](http://tmorris.net/posts/list-folds-bfpg/index.html)
* [A tutorial on the universality and expressiveness of folds](http://www.cs.nott.ac.uk/~gmh/fold.pdf)

## Getting Help

If you have any questions/comments/concerns, feel free to hop onto the IRC channel on
Freenode at `#scalaz`.
