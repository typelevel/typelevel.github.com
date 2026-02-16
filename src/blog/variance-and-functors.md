---
layout: post
title: Of variance and functors
category: technical

meta:
  nav: blog
  author: adelbertc
  pygments: true
---

Scala's type system allows us to annotate type parameters with their variance: covariant, contravariant, invariant.
Variance allows us to define the subtyping relationships between type constructors – that is, under which
conditions `F[A]` is a subtype of `F[B]`.

Similarly in functional programming, there are covariant functors, contravariant functors, and invariant functors. The
similarity in names is not coincidental.

# Covariance
The common example is `List[+A]` which is covariant in its type parameter, denoted by the `+` next to the `A`.
A type constructor with a covariant type parameter means that if there is a subtyping relationship between the
type parameter, there is a subtyping relationship between the two instances of the type constructor.
For example if we have a `List[Circle]`, we can substitute it anywhere we have a `List[Shape]`.

## Read
Another example of covariance is in parsing, for example in the following `Read` type class.

```scala
trait Read[+A] {
  def read(s: String): Option[A]
}
```

It makes sense to make `Read` covariant because if we can read a subtype, then we can read the supertype by
reading the subtype and throwing away the subtype-specific information. For instance, if we can read a
`Circle`, we can read a valid `Shape` by reading the `Circle` and ignoring any `Circle`-specific information.

## Array
A type that cannot safely be made covariant is `Array`. If `Array` were covariant, we could substitute
an `Array[Circle]` for an `Array[Shape]`. This can get us in a nasty situation.

```scala
val circles: Array[Circle] = Array.fill(10)(Circle(..))
val shapes: Array[Shape] = circles // works only if Array is covariant
shapes(0) = Square(..) // Square is a subtype of Shape
```

If `Array` was covariant this would compile fine, but fail at runtime. In fact, Java arrays are
covariant and so the analogous Java code would compile, throwing an `ArrayStoreException` when
run. The compiler accepts this because it is valid to upcast an `Array[Circle]` into an `Array[Shape]`,
and it is valid to insert a `Shape` into an `Array[Shape]`. However the runtime representation of
`shapes` is still an `Array[Circle]` and inserting a `Square` into that isn't allowed.

## Read-only and covariance
In general, a type can be made safely covariant if it is read-only. If we know how to read a specific type, we know
how to read a more general type by throwing away any extra information. `List` is safe to to make
covariant because it is immutable and we can only ever read information off of it. With `Array`, we
cannot make it covariant because we are able to write to it.

## Functor
As we've just seen, covariance states that when `A` subtypes `B`, then `F[A]` subtypes `F[B]`. Put differently,
if `A` can be turned into a `B`, then `F[A]` can be turned into an `F[B]`. We can encode this behavior
literally in the notion of a `Functor`.

```scala
trait Functor[F[_]] {
  def map[A, B](f: A => B): F[A] => F[B]
}
```

This is often encoded slightly differently by changing the order of the arguments:

```scala
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}
```

We can implement `Functor` for `List` and `Read`.

```scala
val listFunctor: Functor[List] =
  new Functor[List] {
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa match {
      case Nil => Nil
      case a :: as => f(a) :: map(as)(f)
    }
  }

val readFunctor: Functor[Read] =
  new Functor[Read] {
    def map[A, B](fa: Read[A])(f: A => B): Read[B] =
      new Read[B] {
        def read(s: String): Option[B] =
          fa.read(s) match {
            case None => None
            case Some(a) => Some(f(a))
          }
      }
  }
```

With that we can do useful things like

```scala
val circles: List[Circle] = List(Circle(..), Circle(..))
val shapes: List[Shape] = listFunctor.map(circles)(circle => circle: Shape) // upcast

val parseCircle: Read[Circle] = ...
val parseShape: Read[Shape] = readFunctor.map(parseCircle)(circle => circle: Shape) // upcast
```

or more generally:

```scala
def upcast[F[_], A, B <: A](functor: Functor[F], fb: F[B]): F[A] =
  functor.map(fb)(b => b: A)
```

`upcast`'s behavior does exactly what covariance does – given some supertype `A` (`Shape`) and a subtype `B` (`Circle`),
we can mechanically (and safely) turn an `F[B]` into an `F[A]`. Put differently, anywhere we expect an `F[A]` we can provide
an `F[B]`, i.e. covariance. For this reason, `Functor` is sometimes referred to in full as covariant functor.

# Contravariance
Contravariance flips the direction of the relationship in covariance – an `F[Shape]` is considered a
subtype of `F[Circle]`. This seems strange – when I was first learning about variance I couldn't
come up with a situation where this would make sense.

If we have a `List[Shape]` we cannot safely treat it as a `List[Circle]` – doing so comes with all the usual
warnings about downcasting. Similarly if we have a `Read[Shape]`, we cannot treat it as a `Read[Circle]` –
we know how to parse a `Shape`, but we don't know how to parse any additional information `Circle` may need.

## Show
It appears fundamentally read-only types cannot be treated as contravariant. However, given that contravariance
is covariance with the direction reversed, can we also reverse the idea of a read-only type? Instead of reading
a value *from* a `String`, we can write a value *to* a `String`.

```scala
trait Show[-A] {
  def show(a: A): String
}
```

`Show` is the other side of `Read` – instead of going from a `String` to an `A`, we go from an `A` into
a `String`. This reversal allows us to define contravariant behavior – if we are asked to provide a way
to show a `Circle` (`Show[Circle]`), we can give instead a way to show just a `Shape`. This is a valid
substitution because we can show a `Circle` by throwing away `Circle`-specific information and showing just
the `Shape` bits. This means that `Show[Shape]` is a subtype of `Show[Circle]`, despite `Circle` being a
subtype of `Shape`.

In general, we can show (or write) a subtype if we know how to show a supertype by tossing away subtype-specific
information (an upcast) and showing the remainder. Again, this means `Show[Supertype]` is substitutable, or a
subtype of, `Show[Subtype]`.

For similar reasons that read-only types can be made covariant, write-only types can be made contravariant.

## Array, again
`Array`s cannot be made contravariant either. If they were, we could do unsafe reads:

```scala
val shapes: Array[Shape] = Array.fill(10)(Shape(..), Shape(..))
val circles: Array[Circle] = shapes // Works only if Array is contravariant
val circle: Circle = circles(0)
```

`circle`, having been read from an `Array[Circle]` has type `Circle`. To the compiler this would be fine, but
at runtime, the underlying `Array[Shape]` may give us a `Shape` that is not a `Circle` and crash the program.

## Contravariant
Our `Functor` interface made explicit the behavior of covariance - we can define a similar interface that
captures contravariant behavior. If `B` can be used where `A` is expected, then `F[A]` can be used where an
`F[B]` is expected. To encode this explicitly:

```scala
trait Contravariant[F[_]] {
  // Alternative encoding:
  // def contramap[A, B](f: B => A): F[A] => F[B]

  // More typical encoding
  def contramap[A, B](fa: F[A])(f: B => A): F[B]
}
```

We can implement an instance for `Show`.

```scala
val showContravariant: Contravariant[Show] =
  new Contravariant[Show] {
    def contramap[A, B](fa: Show[A])(f: B => A): Show[B] =
      new Show[B] {
        def show(b: B): String = {
          val a = f(b)
          fa.show(a)
        }
      }
  }
```

Here we are saying if we can show an `A`, we can show a `B` by turning a `B` into an `A` before showing it.
Upcasting is a specific case of this, when `B` is a subtype of `A`.

```scala
def contraUpcast[F[_], A, B >: A](contra: Contravariant[F], fb: F[B]): F[A] =
  contra.contramap(fb)((a: A) => a: B)
```

Going back to `Shape`s and `Circle`s, we can show a `Circle` by upcasting it into a `Shape` and showing that.

# Function variance
We observed that read-only types are covariant and write-only types are contravariant. This can be
seen in the context of functions and what function types are subtypes of others.

## Parameters
An example function:

```scala
// Right now we only care about the input
def squiggle(circle: Circle): Unit = ???

// or

val squiggle: Circle => Unit = ???
```

What type is a valid subtype of `Circle => Unit`? An important note is we're not
looking for what subtypes we can *pass in* to the function, we are looking for a value with a type
that satisfies the entirety of the function type `Circle => Unit`.

A first guess may involve some subtype of `Circle` like `Dot` (a circle with a radius of 0), such
as `Dot => Unit`.

```scala
val squiggle: Circle => Unit =
  (d: Dot) => d.someDotSpecificMethod()
```

This doesn't work – we are asserting with the moral equivalent of a downcast that any
`Circle` input to the function is a `Dot`, which is not safe to assume.

What if we used a supertype of `Circle`?

```scala
val squiggle: Circle => Unit =
  (s: Shape) => s.shapeshift()
```

This is valid – from the outside looking in we have a function that takes a `Circle` and
returns `Unit`. Internally, we can take any `Circle`, upcast it into a `Shape`, and go from there.
Showing things a bit differently reveals better the relationship:

```scala
type Input[A] = A => Unit
val inputSubtype: Input[Shape] = (s: Shape) => s.shapeshift()
val input: Input[Circle] = inputSubtype
```

We have `Input[Shape] <: Input[Circle]`, with `Circle <: Shape`, so function parameters are contravariant.

The type checker enforces this when we try to use covariant type parameters in contravariant positions.

```scala
scala> trait Foo[+A] { def foo(a: A): Int = 42 }
<console>:15: error: covariant type A occurs in contravariant position in type A of value a
       trait Foo[+A] { def foo(a: A): Int = 42 }
                               ^
```

Since type parameters are contravariant, a type in that position cannot also be covariant. To solve this
we "reverse" the constraint imposed by the covariant annotation by parameterizing with a supertype `B`.

```scala
scala> trait Foo[+A] { def foo[B >: A](a: B): Int = 42 }
defined trait Foo
```

## Return
Let's do the same exercise with function return types.

```scala
val squaggle: Unit => Shape = ???
```

Since using the supertype seemed to work with parameters, let's pick a supertype here, `Object`.

```scala
val squaggle: Unit => Shape =
  (_: Unit) => somethingThatReturnsObject()
```

For similar issues with using a subtype for the input parameter, we cannot use
a supertype for the output. The function type states the return type is `Shape`, but we're
returning an `Object` which may or may not be a valid `Shape`. As far as the type checker is concerned,
this is invalid and the checker rejects the program.

Trying instead with a subtype:

```scala
val squaggle: Unit => Shape =
  (_: Unit) => Circle(..)
```

This makes sense – the function type says it returns a `Shape` and inside we return a `Circle` which is
a perfectly valid `Shape`.

As before, rephrasing the type signatures leads to some insights.

```scala
type Output[A] = Unit => A
val outputSubtype: Output[Circle] = (_: Unit) => Circle(..)
val output: Output[Shape] = outputSubtype
```

That is `Output[Circle] <: Output[Shape]` with `Circle <: Shape` – function return types are covariant.

Again the type checker will enforce this:

```scala
scala> trait Bar[-A] { def bar(): A = ??? }
<console>:15: error: contravariant type A occurs in covariant position in type ()A of method bar
       trait Bar[-A] { def bar(): A = ??? }
                           ^
```

As before, we solve this by "reversing" the contraint imposed by the variance annotation.

```scala
scala> trait Bar[-A] { def bar[B <: A](): B = ??? }
defined trait Bar
```

## All together now
Function inputs are contravariant and function outputs are covariant. Taking the previous examples together,
a function type `Shape => Circle` can be put in a place expecting a function type `Circle => Shape`.

We arrived at this conclusion by observing the behavior of subtype variance and the corresponding functors. Taken
in the context of functional programming where the only primitive is a function, we can draw a conclusion in
the other direction. Where function inputs are contravariant, types in positions where computations are
done (e.g. input or read-only positions) are also contravariant (similarly for covariance).

# Invariance
Unannotated type parameters are considered invariant – the only relationship that holds is if a type `A`
is equal to a type `B`, then `F[A]` is equal to `F[B]`. Otherwise different instantiations of a
type constructor have no relationship with one another. Given invariant
`F[_]`, an `F[Circle]` is not a subtype of `F[Shape]` – you need to explicitly provide the conversion.

## Array once more
`Array`s are invariant in Scala because they can be neither covariant nor contravariant. If we make it
covariant, we can get unsafe writes. If we make it contravariant, we can get unsafe reads. Since
read-only types can only be covariant and write-only types contravariant, our compromise is to make
types that support both invariant.

In order to treat an `Array` of one type as an `Array` of another, we need to have conversions
in both directions. This must be provided manually as the type checker has no way of knowing what the
conversion would be.

## Invariant
Similar to (covariant) `Functor` and `Contravariant`, we can write `Invariant`.

```scala
trait Invariant[F[_]] {
  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
}
```

For demonstration purposes we write our own `Array` type

```scala
class Array[A] {
  private var repr = ListBuffer.empty[A]

  def read(i: Int): A =
    repr(i)
  def write(i: Int, a: A): Unit =
    repr(i) = a
}
```

and define `Invariant[Array]`.

```scala
val arrayInvariant: Invariant[Array] =
  new Invariant[Array] {
    def imap[A, B](fa: Array[A])(f: A => B)(g: B => A): Array[B] =
      new Array[B] {
        // Convert read A to B before returning – covariance
        override def read(i: Int): B =
          f(fa.read(i))

        // Convert B to A before writing – contravariance
        override def write(i: Int, b: B): Unit =
          fa.write(i, g(a))
      }
  }
```

## Serialization
Another example of a read-write type that doesn't involve `Array`s (or mutation) can be
found by just combining the `Read` and `Show` interfaces:

```scala
trait Serializer[A] extends Read[A] with Show[A] {
  def read(s: String): Option[A]
  def show(a: A): String
}
```

`Serializer` both reads (from a `String`) and writes (to a `String`). We can't make it
covariant because that would cause issues with `show`, and we can't make it contravariant
because that would cause issues with `read`. Therefore our only choice is to keep it
invariant.

```scala
val serializerInvariant: Invariant[Serializer] =
  new Invariant[Serializer] {
    def imap[A, B](fa: Serializer[A])(f: A => B)(g: B => A): Serializer[B] =
      new Serializer[B] {
        def read(s: String): Option[B] = fa.read(s).map(f)
        def show(b: B): String = fa.show(g(b))
      }
  }
```

# Bringing everything together
We can see the `Invariant` interface is more general than both `Functor` and `Contravariant` –
where `Invariant` requires functions going in both directions, `Functor` and `Contravariant` only
require one. We can make `Functor` and `Contravariant` subtypes of `Invariant` by ignoring
the direction we don't care about.

```scala
trait Functor[F[_]] extends Invariant[F] {
  def map[A, B](fa: F[A])(f: A => B): F[B]

  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B] =
    map(fa)(f)
}

trait Contravariant[F[_]] extends Invariant[F] {
  def contramap[A, B](fa: F[A])(f: B => A): F[B]

  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B] =
    contramap(fa)(g)
}
```

Going back to treating `Array` and `Serializer` as a read/write store, if we make it read-only (like a read-only
handle on a resource) we can safely treat it as if it were covariant. If we are asked to read
`Shape`s and we know how to read `Circle`s, we can read a `Circle` and upcast it into a `Shape`
before handing it over.

Similarly if we make it write-only (like a write-only handle on a resource) we can safely treat
it as contravariant. If we are asked to store `Circle`s and we know how to store `Shape`s,
we can upcast each `Circle` into a `Shape` before storing it.

Variance manifests in two levels: one at the type level where subtyping relationships are defined, and
the other at the value level where it is encoded as an interface which certain types can conform to.

## One more thing
Thus far we have seen the three kinds of variances Scala supports:

```scala
1. invariance: A = B → F[A] = F[B]
2. covariance: A <: B → F[A] <: F[B]
3. contravariance: A >: B → F[A] <: F[B]
```

This gives us the following graph:

```
   invariance
     ↑   ↑
    /      \
   -        +
```

Completing the diamond implies a fourth kind of variance, one that takes contravariance and
covariance together. This is known as phantom variance or anyvariance, a variance with no constraints on the
type parameters: `F[A] = F[B]` regardless of what `A` and `B` are. Unfortunately Scala's type system is
missing this kind of variance which leaves us just short of a nice diamond, but we can still encode it
in an interface.

```scala
trait Phantom[F[_]] {
  def pmap[A, B](fa: F[A]): F[B]
}
```

Given any `F[A]`, we can turn that into an `F[B]`, for all choices of `A` and `B`. With this power we can
implement covariant and contravariant functors.

```scala
trait Phantom[F[_]] extends Functor[F] with Contravariant[F] {
  def pmap[A, B](fa: F[A]): F[B]

  def map[A, B](fa: F[A])(f: A => B): F[B] = pmap(fa)

  def contramap[A, B](fa: F[A])(f: B => A): F[B] = pmap(fa)
}
```

This completes our diamond of variance.

```
   invariance
     ↑   ↑
    /      \
   -        +
   ↑        ↑
    \      /
    phantom
```
