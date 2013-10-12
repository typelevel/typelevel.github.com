---
layout: post
title: How to use Spire's Ops macros in your own projet

meta:
  nav: blog
  author: d_m
  pygments: true
---

What are Spire's Ops macros?
============================

What are Spire's Ops macros?
----------------------------

Spire's type classes abstract over very basic operators like `+` and
`*`.  Since these are normally very fast operations on the JVM, things
like boxing and object allocations that happen on a per-operator basis
will cause generic code to be much slower than its direct equivalent.

Efficient, generic numeric programming is Spire's raison d'être. One
of the tricks we've come up with is a collection of macros which make
the kinds of implicit conversions used with type classes more
efficient.

How implicit operators on type classes usually work
---------------------------------------------------

When using type classes in Scala, we often rely on implicit
conversions to "add" operators to an otherwise generic type. In this
example, `foo1` is the code that the programmer writes, and `fooN` is
the code after implicits are resolved.

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

The thing to notice here is that we will instantiate an `ev.Ops` instance
every time the user calls `>` on a value of type `A`. This is not a big deal
in most cases, but for a call that is normally quite fast it can add up when
done many (e.g. millions) of times.

It's possible to work around this, but the result is usually ugly:

```scala
def bar[A](x: A, y: A)(implicit ev: Ordering[A]): A =
  ev.gt(x, y)
```

The `ev` parameter contains the method we actually want to call (`gt`), so
this code avoids instantiating an implicit object by calling it directly. This
can get really ugly. Compare these two methods:

```scala
def qux1[A: Field](x: A, y: A): A =
  ((x pow 2) + (y pow 2)).sqrt

def qux2[A](x: A, y: A)(implicit ev: Field[A]): A =
  ev.sqrt(ev.plus(ev.pow(x, 2), ev.pow(y, 2)))
```

If you don't think that `qux2` is not very readable, you are not alone.

At this point, it looks like we can either write clean, readable code
(`qux1`), or we can code defensively to avoid object allocations (`qux2`).
Most programmers will just choose one or the other (probably the former) and
go on with their lives.

However, since this issue affects Spire deeply, it's worth spending a bit more
time seeing what can be done.

Having our cake and eating it too
---------------------------------

Let's look at another example, to compare how the "nice" and "fast" code
snippets look after implicits are resolved:

```scala
def niceBefore[A: Ring](x: A, y: A): A =
  (x + y) * z

def niceAfter[A](x: A, y: A)(implicit ev: Ring[A]): A =
  new RingOps(new RingOps(x)(ev).+(y))(ev).*(z)

def fast[A](x: A, y: A)(implicit ev: Ring[A]): A =
  ev.times(ev.plus(x, y), z)
```

As we can see, `niceAfter` and `fast` are actually pretty similar. If we
wanted to transform `niceAfter` into `fast`, we'd just have to:

1. Figure out the appropriate ASCII "name" for symbolic operators.
   In this example, `+` becomes `plus` and `*` becomes `times`.

2. Rewrite the object instantiation and method call, calling the method on
   `ev` instead and passing `x` and `y` as arguments. For example,
   `new Ops(x)(ev).foo(y)` would become `ev.foo(x, y)`.

In a nutshell, this transformation is what Spire's Ops macros do.

Using the Ops macros
--------------------

Here's `Sized`, a simple type class to help illustrate how to use the macros.
The idea here is that anything that can be thought of as having a `size` (a
positive integer) is allowed. We consider a size of zero to be empty.

Implementations are provided for `Char`, `Map`, and `List`:

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

Notice that `List` overrides some of the "default" implementations to be more
efficient.

We'd like to be able to call these methods directly on a generic type `A`
provided that we have an implicit instance of `Sized[A]` available. So let's
define a `SizedOps` class, using Spire's Ops macros:

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

Of course, there's always some fine-print. In this case, it's important that
the implicit class use the same parameter names as above. The constructor
parameter must be called `lhs` and the method parameter (if any) should be
called `rhs`. Also, unary operators (methods that take no parameters, like
`size`) must have parenthesis.

Symbolic Names
--------------

The previous example illustrates the macro rewriting method calls to avoid
allocations (#2) but what about mapping symbolic operators to method names
(#1)? These work like you'd expect as well:

```scala
trait HasTimes[A] {
  def times(x: A, y: A): A
}

object Implicits {
  implicit class TimesOps[A: HasTimes](lhs: A) {
    def *(rhs: A): A = macro Ops.binop[A, A]
  }
}
```

Currently, the Ops macros have a large (but Spire-specific)
[mapping](https://github.com/non/spire/blob/9eaa5c34549b7fe85c223f207f0790873075c048/macros/src/main/scala/spire/macros/Ops.scala#L143)
from symbols to names. However, your project may want to use different names
(or different symbols). What then?

For now, you are out of luck. In Spire 0.7.0, we plan to make it possible to
extend a trait and provide your own map. This will make it easier for other
libraries that make heavy use of implicit symbolic operators (e.g. Scalaz) to
use these macros as well.

Other considerations
--------------------

One thing you might wonder is how this feature interacts with specialization.
The good news is that since this is a compile-time transformation that happens
before the specialization phase, you don't need to worry about specializing
your implicit ops classes! If your type class trait is specialized, and if you
invoke the implicit from a specialized (or non-generic) context, the result
will also be specialized.

Evaluating the macros at compile-time also means that if there are problems
with the macro, you'll find out about those at compile-time as well. While we
expect that many projects will benefit from the Ops macros, they were designed
specifically for Spire so its possible that your project will discover
problems, or need new features.

If you do end up using these macros, please let us know how they work. If you
have problems, please open an issue, and if you have bug fixes (or new
features) feel free to open a pull request!

Conclusion
----------

We are used to thinking about abstractions as coming with a cost. This means
we often end up doing mental accounting: Is it worth making this generic? Can
I afford this syntactic sugar? What will the runtime impact of this code be?
These concerns condition us to expect that code can either be beautiful or
fast, but not both.

By removing the cost of implicit object instantiation, Spire's Ops macros help
raise the abstraction ceiling, without compromising performance. Our goal is
to close the gap between direct and generic performance in Scala, and to
encourage the widest possible use of generic types and type classes in Scala.
