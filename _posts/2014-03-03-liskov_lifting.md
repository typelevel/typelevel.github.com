---
layout: post
title: 

meta:
  nav: blog
  author: S11001001
  pygments: true
---

When can Liskov be lifted?

Scalaz avoids variance in the sense of the Scala type parameter annotation, with its associated higher-kind implications, except where it has historically featured variance; even here, variance is vanishing as Scala compiler changes crush it out of practical implementability.

There is a deeply related concept in Scalaz's typeclasses, though: *covariant and contravariant functors*. 'Functor' is traditional shorthand for covariant functor, whereas 'Contravariant' represents contravariant functors.

These concepts are related, but neither subsumes the other. A 'Functor' instance does not require its parameter being Scala-covariant. A type can be Scala-covariant over a parameter without having a legal 'Functor' instance.

Liskov

Liskov, also known as <~< and very close to Scala's own <:<, represents a subtyping relationship, and is defined by the ability to lift it into Scala-covariant and Scala-contravariant parameter positions, like so:

def liftCo[F[+_], A, B](a: A <~< B): F[A] <~< F[B]

As Liskov is, soundly, Scala-variant, this can be implemented without a cast. However, it, necessarily, can only be called with Scala-covariant F.

By definition, applying an A<~<B to a value of type A should yield a value of type B, but must also do nothing but return the value. Nevertheless, for functorial values that are *parametrically sound*, even for Scala-invariant F, it is operationally sound to lift Liskov:

def liftCvf[F[_]: Functor](a: A <~< B): F[A] <~< F[B]

For example, this is sound for scalaz.IList.

Parametrically sound

Because a Liskov must be an operational identity, it is essential that, given any value of F[A], for all supertypes B of A, the representation of F[B] must be identical. You can determine this by analyzing the subclasses of F as an algebraic data type, where the key test is whether A appears in the primitive contravariant position: as the parameter to a function.

For example, an IList of "hi" and "there" has exactly the same representation whether you instantiated the IList with String or with Any. So that is a good first test.

This data type analysis is recursive: a data type is parametrically sound over a parameter if all appearances of that parameter in elements of your data type are also parametrically sound. For example, if your F[A] contains an IList[A] in its representation, you may rely on IList's parametrically sound covariance when considering F's.

Any var, or var-like thing such as an Array, places its parameter in an invariant position, because it features a getter (return type) and setter (parameter type). So its presence in the data model invalidates Liskov lifting if the type parameter appears within it.

Obviously, runtime evidence of a type parameter's value eliminates the possibility of lifting Liskov over that parameter.

You cannot perform this representation analysis without considering all subclasses of a class under consideration. For example, considering only HashSet, collection.immutable.Set appears to allow Liskov lifting. However, TreeSet, a subclass of Set, contains a function (A, A) => Ordering. If *any* representation contains a contradiction like this, Liskov lifting is unsafe. You cannot constrain Liskov application by a runtime test.

If you permit open subclassing, you must either declare the requirement to preserve parametric covariance, or accept that it will be violated.

GADTs

Some features of Scala resist simple ADT analysis, so must be given their due. A "recoverable phantom" implies a type relationship that forbids Liskov-lifting, for example:

sealed trait Gimme[A]
case object GimmeI extends Gimme[Int]

In pattern matching, given a Gimme[A] over unknown A, matching GimmeI successfully recovers the type equality A ~ Int; therefore, Liskov-lifting is unsound for Gimme.

Some other GADTs invalidate covariance. For example:

sealed trait P[A]
case class PP[A, B](a: P[A], b: P[B]) extends P[(A, B)]

The pattern match of a P[A] to PP[_,_] can theoretically determine A ~ (x, y) forSome {type x; type y}, so Liskov cannot be lifted into P. However, not all GADTs invalidate Liskov-lifting:

sealed trait AM[A]
case class FAM[A, B](fa: AM[A], f: A => B) extends AM[B]

Matching AM[A] to FAM[_,_] reveals nothing, and the covariance of B is sound in FAM considered alone, so Liskovs can be lifted into AM.

Should this function exist?

The Scalaz community has settled on a definition of covariant-functoriality that conforms with the principle of parametric soundness. The rejection of Functor instances for the scala.collection.* classes, and for collection.immutable.Set, speak to this. As far as I know, Scalaz contains no Functors that are both Scala-invariant and violate the above rules.

So how do you feel about the provision of a combinator of the type of liftCvf for Scalaz's Functor?
