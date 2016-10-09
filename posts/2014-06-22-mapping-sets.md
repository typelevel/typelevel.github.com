---
layout: post
title: How can we map a Set?

meta:
  nav: blog
  author: puffnfresh
  pygments: true
---

Scalaz used to have a `scalaz.Functor` for `scala.collection.Set` but
it was [eventually removed](https://github.com/scalaz/scalaz/pull/276)
because it relied on
[Any's == method](http://www.scala-lang.org/api/2.10.3/index.html#scala.Any). You
can read more about why `Functor[Set]` is a bad idea at
[Fake Theorems for Free](http://failex.blogspot.jp/2013/06/fake-theorems-for-free.html).

If `Set` had been truly parametric, we wouldn't have been able to
define a `Functor` in the first place. Luckily, a truly parametric Set
has recently been added to Scalaz as `scalaz.ISet`, with preliminary
benchmarks also showing some nice performance improvements. I highly
recommend using `ISet` whenever you can!

Now we can see the problem more clearly; the type of `map` on `ISet`
is too restrictive to be used inside of a `Functor` because of the
`scalaz.Order` constraint:

```scala
def map[B: Order](f: A => B): ISet[B]
```

And it might seem like we've lost something useful by not having a
`Functor` available. For example, we can't write the following:

```scala
val nes = OneAnd("2014-05-01", ISet.fromList("2014-06-01" :: "2014-06-22" :: Nil)) // a non-empty Set
val OneAnd(h, t) = nes.map(parseDate)
```

Which is because the `map` function on `scalaz.OneAnd` requires a
`scalaz.Functor` for the `F[_]` type parameter, which is `ISet` in the
above example.

But we have a solution! It's called
[Coyoneda](http://docs.typelevel.org/api/scalaz/nightly/#scalaz.Coyoneda)
(also known as the Free Functor) and it'll hopefully be able to
demonstrate why not having `Functor[ISet]` available has no
fundamental, practical consequences.

Coyoneda
[can be defined in Scala](http://blog.higher-order.com/blog/2013/11/01/free-and-yoneda/)
like so:

```scala
trait Coyoneda[F[_], A] {
  type I
  def k: I => A
  def fi: F[I]
}
```

There are just three parts to it:

1. `I` - an existential type
2. `k` - a mapping from `I` to `A`
3. `fi` - a value of `F[I]`

We can create a couple of functions to help with constructing a
Coyoneda value:

```scala
def apply[F[_], A, B](fa: F[A])(_k: A => B): Coyoneda[F, B] { type I = A } =
  new Coyoneda[F, B] {
    type I = A
    val k = _k
    val fi = fa
  }

def lift[F[_], A](fa: F[A]): Coyoneda[F, A] = Coyoneda(fa)(identity[A])
```

The constructors allow any type constructor to become a Coyoneda value:

```scala
val s: Coyoneda[ISet, Int] = Coyoneda.lift(ISet.fromList(1 :: 2 :: 3 :: Nil))
```

Now here's the special part; we can define a `Functor` for all
Coyoneda values:

```scala
implicit def coyonedaFunctor[F[_]]: Functor[({type λ[α] = Coyoneda[F, α]})#λ] =
  new Functor[({type λ[α] = Coyoneda[F,α]})#λ] {
    def map[A, B](ya: Coyoneda[F, A])(f: A => B) = Coyoneda(ya.fi)(f compose ya.k)
  }
```

What's interesting is that the `F[_]` type does *not* have to have a
`Functor` defined for the Coyoneda to be mapped!

Let's use this to try out our original example. We'll define a type
alias to make things a bit cleaner:

```scala
type ISetF[A] = Coyoneda[ISet, A]
```

And we can use this new type instead of a plain `ISet`:

```scala
// Scala has a really hard time with inference here, so we have to help it out.
val functor = OneAnd.oneAndFunctor[ISetF](Coyoneda.coyonedaFunctor[ISet])
import functor.functorSyntax._

val nes = OneAnd[ISetF, String]("2014-05-01", Coyoneda.lift(ISet.fromList("2014-06-01" :: "2014-06-22" :: Nil)))
val OneAnd(h, t) = nes.map(parseDate)
```

So we've been able to map the Coyoneda! But how do we do something
useful with it?

We couldn't define a `Functor` because it needs `scalaz.Order` on the
output type, but we can use the `map` method directly on `ISet`. We
can use that function by running the Coyoneda like so:

```scala
// Converts ISetF back to an ISet, using ISet#map with the Order constraint
val s = t.fi.map(t.k).insert(h)
```

And we're done!

We've been able to use Coyoneda to treat an `ISet` as a `Functor`,
even though its map function is too constrained to have one defined
directly. This same technique applies to `scala.collection.Set` and
any other type-constructor which would otherwise require a
[restricted `Functor`](http://okmij.org/ftp/Haskell/types.html#restricted-datatypes). I
hope this has demonstrated that `Functor[Set]` not existing has no
practical consequences, other than scalac not being as good at
type-inference.
