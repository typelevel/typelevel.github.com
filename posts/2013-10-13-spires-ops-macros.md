---
layout: post
title: How to use Spire's Ops macros in your own project

meta:
  nav: blog
  author: non
  pygments: true
---

## What are Spire's Ops macros?

Spire's type classes abstract over very basic operators like `+` and
`*`.  These operations are normally very fast. This means that any
extra work that happens on a per-operation basis (like boxing or
object allocation) will cause generic code to be slower than its
direct equivalent.

Efficient, generic numeric programming is Spire's raison d'être. We
have developed a set of Ops macros to avoid unnecessary object
instantiations at compile-time. This post explains how, and
illustrates how you can use these macros in your code!

## How implicit operators on type classes usually work

When using type classes in Scala, we rely on implicit conversions to
"add" operators to an otherwise generic type.

In this example, `A` is the generic type, `Ordering` is the type
class, and `>` is the implicit operator. `foo1` is the code that the
programmer writes, and `foo4` is a translation of that code after
implicits are resolved, and syntactic sugar is expanded.

```scala
import scala.math.Ordering
import Ordering.Implicits._

def foo1[A: Ordering](x: A, y: A): A =
  x > y

def foo2[A](x: A, y: A)(implicit ev: Ordering[A]): A =
  x > y

def foo3[A](x: A, y: A)(implicit ev: Ordering[A]): A =
  infixOrderingOps[A](x)(ev) > y

def foo4[A](x: A, y: A)(implicit ev: Ordering[A]): A =
  new ev.Ops(x) > y
```

(This is actually slightly wrong. The expansion to `foo4` won't happen
until runtime, when `infixOrderingOps` is called. But it helps
illustrate the point.)

Notice that we instantiate an `ev.Ops` instance for every call to
`>`. This is not a big deal in many cases, but for a call that is
normally quite fast it will add up when done many (e.g. millions) of
times.

It is possible to work around this:

```scala
def bar[A](x: A, y: A)(implicit ev: Ordering[A]): A =
  ev.gt(x, y)
```

The `ev` parameter contains the method we actually want (`gt`), so
instead of instantiating `ev.Ops` this code calls `ev.gt` directly.
But this approach is ugly. Compare these two methods:

```scala
def qux1[A: Field](x: A, y: A): A =
  ((x pow 2) + (y pow 2)).sqrt

def qux2[A](x: A, y: A)(implicit ev: Field[A]): A =
  ev.sqrt(ev.plus(ev.pow(x, 2), ev.pow(y, 2)))
```

If you have trouble reading `qux2`, you are not alone.

At this point, it looks like we can either write clean, readable code
(`qux1`), or code defensively to avoid object allocations (`qux2`).
Most programmers will just choose one or the other (probably the
former) and go on with their lives.

However, since this issue affects Spire deeply, we spent a bit more
time looking at this problem to see what could be done.

## Having our cake and eating it too

Let's look at another example, to compare how the "nice" and "fast"
code snippets look after implicits are resolved:

```scala
def niceBefore[A: Ring](x: A, y: A): A =
  (x + y) * z

def niceAfter[A](x: A, y: A)(implicit ev: Ring[A]): A =
  new RingOps(new RingOps(x)(ev).+(y))(ev).*(z)

def fast[A](x: A, y: A)(implicit ev: Ring[A]): A =
  ev.times(ev.plus(x, y), z)
```

As we can see, `niceAfter` and `fast` are actually quite similar. If
we wanted to transform `niceAfter` into `fast`, we'd just have to:

1. Figure out the appropriate name for symbolic operators. In this
   example, `+` becomes `plus` and `*` becomes `times`.

2. Rewrite the object instantiation and method call, calling the
   method on `ev` instead and passing `x` and `y` as arguments. In
   this example, `new Ops(x)(ev).foo(y)` becomes `ev.foo(x, y)`.

In a nutshell, this transformation is what Spire's Ops macros do.

Using the Ops macros
--------------------

Your project must use Scala 2.10+ to be able to use macros.

To use Spire's Ops macros, you'll need to depend on the `spire-macros`
package. If you use SBT, you can do this by adding the following line
to `build.sbt`:

```scala
libraryDependencies += "org.spire-math" %% "spire-macros" % "0.6.1"
```

You will also need to enable macros at the declaration site of your
ops classes:

```scala
import scala.language.experimental.macros
```

## Let's see an example

Consider `Sized`, a type class that abstracts over the notion of
having a size. Type class instances for `Char`, `Map`, and `List` are
provided in the companion object. Of course, users can also provide
their own instances.

Here's the code:

```scala
trait Sized[A] {
  def size(a: A): Int
  def isEmpty(a: A): Boolean = size(a) == 0
  def nonEmpty(a: A): Boolean = !isEmpty(a)
  def sizeCompare(x: A, y: A): Int = size(x) compare size(y)
}

object Sized {
  implicit val charSized = new Sized[Char] {
    def size(a: Char): Int = a.toInt
  }

  implicit def mapSized[K, V] = new Sized[Map[K, V]] {
    def size(a: Map[K, V]): Int = a.size
  }

  implicit def listSized[A] = new Sized[List[A]] {
    def size(a: List[A]): Int = a.length
    override def isEmpty(a: List[A]): Boolean = a.isEmpty
    override def sizeCompare(x: List[A], y: List[A]): Int = (x, y) match {
      case (Nil, Nil) => 0
      case (Nil, _) => -1
      case (_, Nil) => 1
      case (_ :: xt, _ :: yt) => sizeCompare(xt, yt)
    }
  }
}
```

(Notice that `Sized[List[A]]` overrides some of the "default"
implementations to be more efficient, since taking the full length of
a list is an O(n) operation.)

We'd like to be able to call these methods directly on a generic type
`A` when we have an implicit instance of `Sized[A]` available. So
let's define a `SizedOps` class, using Spire's Ops macros:

```scala
import spire.macrosk.Ops
import scala.language.experimental.macros

object Implicits {
  implicit class SizedOps[A: Sized](lhs: A) {
    def size(): Int = macro Ops.unop[Int]
    def isEmpty(): Boolean = macro Ops.unop[Boolean]
    def nonEmpty(): Boolean = macro Ops.unop[Boolean]
    def sizeCompare(rhs: A): Int = macro Ops.binop[A, Int]
  }
}
```

That's it!

Here's what it would look like to use this type class:

```scala
import Implicits._

def findSmallest[A: Sized](as: Iterable[A]): A =
  as.reduceLeft { (x, y) =>
    if ((x sizeCompare y) < 0) x else y
  }

def compact[A: Sized](as: Vector[A]): Vector[A] =
  as.filter(_.nonEmpty)

def totalSize[A: Sized](as: Seq[A]): Int =
  as.foldLeft(0)(_ + _.size)
```

Not bad, eh?

## The fine print

Of course, there's always some fine-print.

In this case, the implicit class **must** use the same parameter names
as above. The constructor parameter to `SizedOps` **must** be called
`lhs` and the method parameter (if any) **must** be called
`rhs`. Also, unary operators (methods that take no parameters, like
`size`) **must** have parenthesis.

How the macros handle classes with multiple constructor parameters, or
multiple method parameters? They don't. We haven't needed to support
these kinds of exotic classes, but it would probably be easy to extend
Spire's Ops macros to support other shapes as well.

If you fail to follow these rules, or if your class has the wrong
shape, your code will fail to compile. So don't worry. If your code
compiles, it means you got it right!

## Symbolic names

The previous example illustrates rewriting method calls to avoid
allocations, but what about mapping symbolic operators to method
names?

Here's an example showing the mapping from `*` to `times`:

```scala
trait CanMultiply[A] {
  def times(x: A, y: A): A
}

object Implicits {
  implicit class MultiplyOps[A: CanMultiply](lhs: A) {
    def *(rhs: A): A = macro Ops.binop[A, A]
  }
}

object Example {
  import Implicits._

  def gak[A: CanMultiply](a: A, as: List[A]): A =
    as.foldLeft(a)(_ * _)
  }
}
```

Currently, the Ops macros have a large (but Spire-specific)
[mapping](https://github.com/non/spire/blob/9eaa5c34549b7fe85c223f207f0790873075c048/macros/src/main/scala/spire/macros/Ops.scala#L143)
from symbols to names. However, your project may want to use different names
(or different symbols). What then?

For now, you are out of luck. In Spire 0.7.0, we plan to make it
possible to use your own mapping. This should make it easier for other
libraries that make heavy use of implicit symbolic operators
(e.g. Scalaz) to use these macros as well.

## Other considerations

You might wonder how the Ops macros interact with
specialization. Fortunately, macros are expanded before the
specialization phase. This means you don't need to worry about it! If
your type class is specialized, and you invoke the implicit from a
specialized (or non-generic) context, the result will be a specialized
call.

(Of course, using Scala's specialization is tricky, and deserves its
own blog post. The good news is that type classes are some of the
easiest structures to specialize correctly in Scala.)

Evaluating the macros at compile-time also means that if there are
problems with the macro, you'll find out about those at compile-time
as well. While we expect that many projects will benefit from the Ops
macros, they were designed specifically for Spire so it's possible
that your project will discover problems, or need new features.

If you do end up using these macros,
[let us know how](https://groups.google.com/forum/#!forum/spire-math)
they work for you. If you have problems, please open an
[issue](https://github.com/non/spire/issues), and if you have bug
fixes (or new features) feel free to open a
[pull request](https://github.com/non/spire/pulls)!

## Conclusion

We are used to thinking about abstractions having a cost. So we often
end up doing mental accounting: "Is it worth making this generic? Can
I afford this syntactic sugar? What will the runtime impact of this
code be?" These condition us to expect that code can either be
beautiful or fast, but not both.

By removing the cost of implicit object instantiation, Spire's Ops
macros raise the abstraction ceiling. They allow us to make free use
of type classes without compromising performance. Our goal is to close
the gap between direct and generic performance, and to encourage the
widest possible use of generic types and type classes in Scala.
