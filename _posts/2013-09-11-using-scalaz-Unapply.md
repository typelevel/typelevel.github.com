---
layout: post
title: Using scalaz.Unapply

meta:
  nav: blog
  author: S11001001
  pygments: true
---

Once you've started really taking advantage of Scalaz's typeclasses
for generic programming, you might have noticed a need to write
typelambdas to use some of your neat abstractions, or use syntax like
`traverse` or `kleisli` with a strangely-shaped type as an argument.
Here's a simple generalization, a `List`-based `traverse`.

```scala
import scalaz.Applicative, scalaz.syntax.applicative._

def sequenceList[F[_]: Applicative, A](xs: List[F[A]]): F[List[A]] =
  xs.foldRight(List.empty[A].point[F])((a, b) => ^(a, b)(_ :: _))
```

This works fine for a while.

```scala
scala> import scalaz.std.option._
import scalaz.std.option._

scala> sequenceList(List(some(1),some(2)))
res1: Option[List[Int]] = Some(List(1, 2))

scala> sequenceList(List(some(1),none))
res2: Option[List[Int]] = None
```

The problem
-----------

The type of the input in the above example, `List[Option[Int]]`, can be
neatly destructured into the `F` and `A` type params needed by
`sequenceList`.  It has the “shape” `F[x]`, so `F` can be picked out by
Scala easily.

Consider something else with a convenient `Applicative` instance,
though.

```scala
scala> import scalaz.\/
import scalaz.$bslash$div

scala> sequenceList(List(\/.right(42), \/.left(NonEmptyList("oops"))))
<console>:23: error: no type parameters for method 
  sequenceList: (xs: List[F[A]])(implicit evidence$1: scalaz.Applicative[F])F[List[A]]
  exist so that it can be applied to arguments
  (List[scalaz.\/[scalaz.NonEmptyList[String],Int]])
 --- because ---
argument expression's type is not compatible with formal parameter type;
 found   : List[scalaz.\/[scalaz.NonEmptyList[String],Int]]
 required: List[?F]

              sequenceList(List(\/.right(42), \/.left(NonEmptyList("oops"))))
              ^
```

Here, `?F` meaning it couldn't figure out that you meant `({type λ[α]
= NonEmptyList[String] \/ α})#λ`.

```scala
scala> sequenceList[({type λ[α] = NonEmptyList[String] \/ α})#λ, Int
                  ](List(\/.right(42), \/.left(NonEmptyList("oops"))))
res5: scalaz.\/[scalaz.NonEmptyList[String],List[Int]] =
        -\/(NonEmptyList(oops))
```

The problem is that `NonEmptyList[String] \/ Int` has the shape
`F[A, B]`, with `F` of kind `* -> * -> *` after a fashion, whereas the
`F` it wants must have kind `* -> *`, and Scala kinds aren't curried
at all.

Finding an `Unapply` instance
-----------------------------

`Unapply`, though, *does* have implicit instances matching the
`F[A, B]` shape, `unapplyMAB1` and `unapplyMAB2`, in its companion so
effectively always visible.  What's special about them is that their
type parameters match the “shape” you're working with, `F[A, B]`.

You should
[look at their source](https://github.com/scalaz/scalaz/blob/v7.0.3/core/src/main/scala/scalaz/Unapply.scala#L210)
to follow along.

Let's see if one of them works.  For implicit resolution to finish,
it's important that *exactly* one of them works.

```scala
scala> import scalaz.Unapply
import scalaz.Unapply

scala> Unapply.unapplyMAB1[Applicative, \/, NonEmptyList[String], Int]
<console>:23: error: could not find implicit value for parameter
TC0: scalaz.Applicative[[α]scalaz.\/[α,Int]]
              Unapply.unapplyMAB1[Applicative, \/, NonEmptyList[String], Int]
                                 ^

scala> Unapply.unapplyMAB2[Applicative, \/, NonEmptyList[String], Int]
res7: scalaz.Unapply[scalaz.Applicative,
                     scalaz.\/[scalaz.NonEmptyList[String],Int]]{
        type M[X] = scalaz.\/[scalaz.NonEmptyList[String],X];
        type A = Int
      } = scalaz.Unapply_0$$anon$13@5402af61
```

Here, the type `res7.M` represents the typelambda being passed to
`sequenceList`.  You can see that work.

```scala
scala> sequenceList[res7.M, res7.A](
                   List(\/.right(42), \/.left(NonEmptyList("oops"))))
res8: res7.M[List[res7.A]] = -\/(NonEmptyList(oops))

scala> res8 : NonEmptyList[String] \/ List[Int]
res9: scalaz.\/[scalaz.NonEmptyList[String],List[Int]] =
        -\/(NonEmptyList(oops))
```

The `res8` conformance test shows that Scala can still reduce the
path-dependent `res7.M` and `res7.A` types at this level, outside
`sequenceList`.

Searching for the right shape
-----------------------------

Implicit resolution can pick the call to `unapplyMAB2` partly because
it can pick all of its type parameters without weird typelambda
structures.  But in Scalaz, we use typeclasses to guide its choice.

Why didn't `unapplyMAB1` work?  In this case, you can trust `scalac`
to say exactly the right thing: it looked for
`Applicative[[α]scalaz.\/[α,Int]]`, and didn't find one.  Sure enough,
`\/` being right-biased means we don't offer that instance.

Incidentally, if you were to introduce that instance, you'd break code
relying on right-biased `Unapply` resolution to work.

`unapplyMAB2` needs evidence of `TC[({type λ[α] = M0[A0, α]})#λ]`.
But that's okay, because we have that, where `TC=Applicative`,
`M0=\/`, and `A0=NonEmptyList[String]`!

```scala
scala> Applicative[({type λ[α] = \/[NonEmptyList[String], α]})#λ]
res10: scalaz.Applicative[[α]scalaz.\/[scalaz.NonEmptyList[String],α]]
         = scalaz.DisjunctionInstances2$$anon$1@2f658816
```

Scala doesn't need to figure out any typelambda itself for this to
work; we did everything by putting the typelambda right into
`unapplyMAB2`'s evidence requirement, so it just has to find the
conforming implicit value.

Using `Unapply` generically
---------------------------

Now you can write a `sequenceList` wrapper that works for `\/` and
many other shapes, including user-provided shapes in the form of new
`Unapply` implicit instances.  If you're using Scala 2.9 (still?!) you
need to add `-Ydependent-method-types` to `scalacOptions` to write
this function.

```scala
def sequenceListU[FA](xs: List[FA])
                     (implicit U: Unapply[Applicative, FA]
                     ): U.M[List[U.A]] =
  sequenceList(U.leibniz.subst(xs))(U.TC)
```

Instead of `xs` being `List[F[A]]`, it's `List[FA]`, and that's
destructured into `U.M` and `U.A`.  The latter are path-dependent
types on `U`, the conventional name of the `Unapply` parameter.  We
have also followed the convention of naming the `Unapply`-taking
variant function ending with a `U`.

And that works great!

```scala
scala> sequenceListU(List(\/.right(42), \/.left(NonEmptyList("oops"))))
res11: scalaz.\/[scalaz.NonEmptyList[String],List[Int]] =
   -\/(NonEmptyList(oops))
```

Of course, there's that strange-looking function body to consider,
still.

Using the `U` evidence
----------------------

The type equalities of the original `U.M` and `U.A` to the original
types can be seen where `res8` is refined to `res9` above.  But only
the *caller* of the function knows those equalities, because it
produced and supplied the `unapplyMAB2` call, which has a structural
type containing those equalities.

The body of `sequenceListU` doesn't know those things.  In particular,
it *still* can't pick type parameters to pass to `sequenceList`
without a little help.

The `leibniz` member is a reified type equality of `FA === U.M[U.A]`,
meaning those are the same at the type level, even though Scala can't
see it in this context.  It represents genuine evidence that those two
types are equal, and is much more powerful than scala-library's own
`=:=`.  We're using the core Leibniz operator, `subst`, directly to
prove that, *as a consequence of that type equality*, `List[FA] ===
List[U.M[U.A]]` is *also* a type equality, and that therefore this
[constant-time] coercion is valid.  This lifting is applicable in all
contexts, not just covariant ones like `List`'s.  Take a look at
[the full API](https://github.com/scalaz/scalaz/blob/v7.0.3/core/src/main/scala/scalaz/Leibniz.scala)
for more, though you'll typically just need to come up with the right
type parameter for `subst`.

You can't ask for an `Unapply` and *also* ask for an
`Applicative[U.M]`; Scala won't allow it.  So, because we needed to
resolve the typeclass anyway to find the `Unapply` implicit to use, we
just cart it along with the `U` and give it to the function, which
almost always needs to use it anyway.  Because it's not implicitly
available, you usually need to grab it, `U.TC`, and use it directly.

Using in `scalaz.syntax`
------------------------

`map` comes from functor syntax; it's not a method on `Function1`.  So
how come this works?

```
scala> import scalaz.std.function._
import scalaz.std.function._

scala> ((_:Int) + 42) map (_ * 33)
res13: Int => Int = <function1>

scala> res13(1)
res14: Int = 1419
```

When you import syntax, as `Functor` syntax was imported with
`scalaz.syntax.applicative._` above, you get at least two conversions:
the plain one, like `ToFunctorOps[F[_],A]`, which works if you have
the right shape, and the fancy one, `ToFunctorOpsUnapply[FA]`, which
uses an `Unapply` to effectively invoke `ToFunctorOps` as in the
above.  The latter is lower-priority, so Scala will pick the former if
the value has the `F[A]` shape.

That gives access to all the methods in `FunctorOps`, and other ops
classes, with only one special `U`-taking method.  If you have several
functions operating on the same value type, or you can make that type
similar with Leibnizian equality as implicit arguments to your
methods, I suggest grouping them in this way, too, to cut down on
boilerplate.

Provide both anyway
-------------------

We sometimes get asked “why not just provide the `Unapply` version of
the function or ops?”

We do it, and suggest it for your own code, despite the confusion,
because it's easier to work with real type equalities than with
Leibnizian equality, which you can do in your “real” function
implementation, and as seen in `res8` above, the path-dependent type
resolution can leave funny artifacts in the inferred result.  Here's
an extreme example from
[an earlier demonstration](https://groups.google.com/d/msg/scalaz/9zAIGETrePI/o1rBsOcWJWAJ).

```scala
scala> val itt = IdentityT lift it
itt: IdentityT[scalaz.Unapply[scalaz.Monad,IdentityT[scalaz.Unapply[scalaz.Monad,Identity[Int]]
                                                       {type M[X] = Identity[X]; type A = Int}#M,
                                                     Int]]
                 {type M[X] = IdentityT[scalaz.Unapply[scalaz.Monad,Identity[Int]]
                                          {type M[X] = Identity[X]; type A = Int}#M,
                                        X];
                  type A = Int}#M,
               Int]
  = IdentityT(IdentityT(Identity(42)))
```

Credits
-------

[Jason Zaugg](https://twitter.com/retronym) implemented Scalaz
`Unapply`, based on
[ideas](https://issues.scala-lang.org/browse/SI-2712?focusedCommentId=55239&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-55239)
from [Miles Sabin](https://twitter.com/milessabin) and
[Paul Chiusano](https://github.com/pchiusano).

Leibnizian equality was implemented for Scalaz by
[Edward Kmett](https://github.com/ekmett).

[Lars Hupel](https://twitter.com/larsr_h)'s talk
([slides](https://speakerdeck.com/larsrh/seven-at-one-blow-new-and-polished-features-in-scalaz-7),
[video](https://www.youtube.com/watch?feature=player_embedded&v=KzoqOVD7mvE))
on the features in the then-upcoming Scalaz 7 at nescala 2013,
including `Unapply`, gave me the missing “guided by typeclasses”
detail, inspiring me to tell more people about the whole thing at the
conference, and then, much later, write it down here.

*This article was tested with Scala 2.10.2 & Scalaz 7.0.3.*
