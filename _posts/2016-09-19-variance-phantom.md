---
layout: post
title: Choosing variance for a phantom type

meta:
  nav: blog
  author: S11001001
  pygments: true
---

When you use a type parameter to abstract over actual data in your
ADT, there is typically only one
[variance]({% post_url 2016-02-04-variance-and-functors %}) that makes
sense, if you choose to incorporate subtyping into your designs at
all. This is
[the natural, “parametrically sound” variance]({% post_url 2014-03-09-liskov_lifting %}#parametrically-sound-covariance).

```scala
sealed abstract class MyModel[P, I, -T, +V]

final case class Running[I, T, V](
  run: (I, T) => V
) extends MyModel[String, I, T, V]

final case class Inject[I](
  config: I
) extends MyModel[Int, I, Any, Nothing]
```

There are only four interesting possibilities, each of which is
illustrated above.

1. `V` occurs in only covariant positions, so can be marked covariant.
2. `T` occurs in only contravariant positions, so can be marked
   contravariant.
3. `I` occurs in a pattern that meets neither of standards 1 and 2, so
   may only be marked invariant, while still making sense.
4. `P` meets *both* standards 1 and 2, so…now what?

The fourth case is interesting to me, firstly, because the design of
variance in Scala has not accounted for it; it is “phantom”,
[the missing fourth variance]({% post_url 2016-02-04-variance-and-functors %}#one-more-thing).
I like to write it as I did in
[“The missing diamond of Scala variance”](https://failex.blogspot.com/2016/09/the-missing-diamond-of-scala-variance.html):

```scala
sealed abstract class MyModel[👻P, I, -T, +V]
```

Second, and more practically, it illuminates the role of variance in
pattern matching in a way that can be difficult to see with that
confusing data in the way.

## It can be covariant

The rule for a type parameter being parametrically covariant says we
have to look at all the positions in the data where the type parameter
occurs; if every one of them is a covariant position, then the type
parameter may be marked covariant. Consider a simplified version of
`MyModel`.

```scala
sealed abstract class Gimme[P]
case object AStr extends Gimme[String]
case object AnInt extends Gimme[Int]
```

The type parameter `P` appears in no positions, so it vacuously
satisfies the requirement “every occurrence is in covariant position”.

So let us mark `P` covariant.

```scala
sealed abstract class Gimme[+P]
// otherwise the same
```

## It can be contravariant

The rule for contravariance is also based on the occurrences of the
type parameter: if every occurrence is in contravariant position, then
the type parameter may be contravariant.

This rule seems to be contradict the rule for covariance, except that
all “every” statements are always true when the set under
consideration is empty.

1. Set **S** is empty.
2. Every element of set **S** is a dog.
3. No element of set **S** is a dog.

2 and 3 can be true at the same time, but only if 1 is true, too. So
let us mark `P` contravariant, in a renamed ADT.

```scala
sealed abstract class Gotme[-P]
case object UnStr extends Gotme[String]
case object UnInt extends Gotme[Int]
```

## The usual relationships

Since you can choose any variance for phantom parameters, the
important question is: what kind of type relationships should
exist within my ADT?

At first, this seems to be merely a question of how values of `Gimme`
and `Gotme` types ought to widen.

1. Every `Gimme[Cat]` is a `Gimme[Animal]`, and
2. every `Gotme[Animal]` is a `Gotme[Cat]`. Moreover,
3. every `Gimme[Nothing]` is a `Gimme[T]` no matter what `T` is, and
4. every `Gotme[Any]` is a `Gotme[T]` no matter what `T` is.

Obviously, if neither of these behaviors—the 1/3 nor the 2/4—is
desirable, you shouldn’t use variance. In my experience, this is the
case for most phantom types. If one is desirable, then it may be fine,
but there’s more to consider.

## Extracting the covariant

Pattern-matching on the covariant `Gimme` reveals fully safe type
information. Unlike `ClassTag` and `TypeTag`, which are egregiously
broken for this use case, this method of carrying type information
forward into runtime is closed and Scalazzi-safe.

What type information is revealed?

```scala
def gimme[P](g: Gimme[P]): (P, P) = g match {
  case AStr =>
    // implicitly[P =:= String]   both
    // implicitly[P <:< String]   fail
    implicitly[String <:< P]
    ("hi", "there")
  case AnInt => (42, 84)
}
```

If we left `Gimme`’s type parameter invariant, all three tests above
would succeed. In the case of this code, on the other hand,

1. `AStr.type` (the type of `AStr`) widens to `Gimme[String]`,
2. `Gimme[String]` can widen to `Gimme[P]` as long as `P` is a
   *supertype* of `String`.

Because we’re reversing this process, we have to assume that #2 could
have happened.

The expression `("hi", "there")` still compiles because `P`, while
otherwise mysterious, *surely is* a supertype of `String`. So the two
`String`s can widen to `P`.

Things do not work out so well for all such functions.

## Extracting the contravariant

Matching on the contravariant `Gotme` likewise reveals fully safe
type information.

```scala
def mklength[P](g: Gotme[P]): P => Int = g match {
  case UnStr =>
    // implicitly[P =:= String]   will fail
    implicitly[P <:< String]
    // implicitly[String <:< P]   will fail
    (s: String) => s.length
  case UnInt => identity[Int]
}
```

Now `P <:< String`, which failed for the covariant form but succeeds
for the contravariant. On the other hand, we lost `String <:< P`,
which only works for the covariant form. That’s because

1. `UnStr.type` widens to `Gotme[String]`;
2. `Gotme[String]` can widen to `Gotme[P]` as long as `P` is a
   *subtype* of `String`.

In the covariant form, we knew that every `String` was a `P`. In this
code, we know instead that every `P` is a `String`. Functions that can
handle any `String` are thus able to handle any `P`, logically, so the
type `String => Int` widens to `P => Int`.

## Extracting the invariant

`gimme` would not work with the contravariant GADT; likewise,
`mklength` would not work with the covariant GADT.

An invariant GADT supports both, as well as some supported by
neither. For example, we could produce a `(P, P) => P` from a pattern
match. We can do this because the equivalent of `AStr` for invariant
`Gimme` tells us `P = String`, so all three `implicitly` checks
succeed.

From the behavior of pattern matching over these three sorts of GADTs,
I take away two lessons about variance in Scala.

1. It is impractical to infer variance in Scala, because you cannot
   mechanically infer what sort of GADT pattern matching functions
   ought to be possible to write.
2. The type flexibility of a generic type with variance comes at the
   cost of decreased flexibility in pattern-matching
   code. [There ain’t no such thing as a free lunch.](https://en.wikipedia.org/wiki/TANSTAAFL)

## A GADT skolem

The “reverse widening” of pattern matching lifts the veil on one of
the more confusing references in type errors, a “GADT skolem”.

```scala
def uncons[A](as: List[A]): Option[::[A]] = as match {
  case c@(_ :: _) => Some(c)
  //                      ↑
  // [error] type mismatch;
  //  found   : ::[?A1] where type ?A1 <: A
  //            (this is a GADT skolem)
  //  required: ::[A]
  case _ => None
}
```

These “GADT skolems” appear all the time in sensible, compiling
code. Take a `List` with some variance carelessly tossed in.

```scala
sealed abstract class MyList[+A]
final case class MyCons[A](head: A, tail: MyList[A])
  extends MyList[A]
case object MyNil extends MyList[Nothing]
```

Constructing `MyCons[String]`, here’s what can happen.

1. `MyCons[String]` widens to `MyList[String]`.
2. `MyList[String]` can widen to `MyList[U]` for any supertype `U` of
   `String`.

So in this code, we cannot reverse `MyList[A]` down to
`MyCons[A]`. But we *can* get `MyList[L]`, where `L` is an otherwise
mysterious subtype of `A`. `L` is the GADT skolem, similar to `?A1` in
the above compiler error. The difference is that this code compiles.

```scala
def drop1[A](as: MyList[A]): MyList[A] =
  as match {
    case MyNil => MyNil
    case MyCons(_, tl) => tl
    // tl: MyList[L]  (L is a GADT skolem)
    // L <: A, therefore
    // MyList[L] <: MyList[A] by covariance
  }
```

## `MyList`’s type parameter is a phantom

We saw earlier that variance has a strong influence on the usability
of pattern matching. `MyList` has something important in common with
`Gimme`: the class definition does not use `A`, it only *defines*
it. So the scalac-enforced variance rules do not apply, and we can
make `MyList` contravariant instead.

```scala
sealed abstract class BadList[-A]
final case class BadCons[A](head: A, tail: BadList[A])
  extends BadList[A]
case object BadNil extends BadList[Any]
```

Curiously, `drop1` still works.

```scala
def baddrop1[A](as: BadList[A]): BadList[A] =
  as match {
    case BadNil => BadNil
    case BadCons(_, tl) => tl
    // tl: BadList[U]  (U is a GADT skolem)
    // A <: U, therefore
    // BadList[U] <: BadList[A] by contravariance
  }
```

Other obvious functions will not work for non-obvious reasons.

```scala
def badHeadOption[A](as: BadList[A]): Option[A] =
  as match {
    case BadNil => None
    case BadCons(hd, _) => Some(hd)
    // [error] type mismatch;   ↑
    //  found   : hd.type (with underlying type Any)
    //  required: A
  }
```

This fails because the skolem from a contravariant parameter is a
supertype instead of subtype. So

1. `hd: U` (`U` is a GADT skolem),
2. `A <: U`,
3. we’re stuck; there is no `A` value.

This is not to imply something as silly as “covariance good,
contravariance bad”; you can just as well get these errors by marking
a parameter covariant that can only meaningfully be marked
contravariant. If anything, contravariance is more important than
covariance. The problem you must face is that the compiler is less
helpful in determining what “meaningful” marking, if any, should be
applied.

`MyModel`, from the beginning of this article, demonstrates three
situations in which each supported variance is natural. You may use it
as a guide, but its sanity is not compiler-checked. Your variances’
sanity, or lack thereof, only becomes apparent when implementing
practical functions over a datatype.

## Extracting the phantom

Suppose the phantom variance was defined, and we revisit the
`String`-and-`Int` GADT one more time.

```scala
sealed abstract class BooGimme[👻P]
case object BooStr extends BooGimme[String]
case object BooInt extends BooGimme[Int]
```

The trouble with letting the compiler infer covariance or
contravariance is that, on the face of it, either is as good as the
other. With phantom, we choose both.

But this variance makes the GADT utterly useless. Consider how
`BooStr` becomes `BooGimme[P]`.

1. `BooStr` widens to `BooGimme[String]`.
2. `BooGimme[String]` can widen to `BooGimme[P]` where `P` is…oops,
   there are no conditions this time! `P` can be anything at all and
   the widen will still work.

The match tells us nothing about the type parameter; all three of the
type relationship checks via `implicitly` from the examples above
fail. We maximize the flexibility of the type parameter at the cost of
making GADT pattern matching impossible.

Likewise, if you mark `MyList[A]`’s type parameter phantom, there are
no bounds on the GADT skolem, so there’s little you can do with the
elements of the list.

## The case for choosing no variance

My `scalac` error message pet peeve is the one suggesting that you
should add a variance annotation. This message treats the addition of
variance like a mechanical change: “if it compiles, it works”. On the
contrary, we have seen that

1. The flexibility of variance costs flexibility elsewhere;
2. the compiler cannot predict how this might harm your APIs’
   practicality;
3. the semantics of pattern matching are more complex in the face of
   variance.

Even if variance is applicable to your datatype, these costs, and the
cost of the additional complexity burden, should give you pause. Yet,
I stand by the claim I made in “The missing diamond
of Scala variance”: subtyping is incomplete without variance, so if
variance is too complicated, so is subtyping.

I don’t think subtyping—and its necessary component, variance—are too
complex for the working programmer to understand. Indeed, it can be a
fascinating exercise, with plenty of practical implications.

But, to me, the consequence of working out such exercises is that
neither variance nor subtyping ought to be used in the design of
practical programs, especially when higher-kinded type parameters and
members are available, offering far more flexibility at a better
price. There is no need to struggle in the face of all-too-often
missing features.
