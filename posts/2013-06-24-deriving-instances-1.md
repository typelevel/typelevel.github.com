---
layout: post
title: Deriving Type Class Instances (Part 1)

meta:
  nav: blog
  author: larsrh
  pygments: true
  mathjax: true
---

This is a three-part blog series on how to leverage Scala macros to generate type class instances for case classes (or sets of case classes).
First, we will explore the underlying abstractions and how we can possibly get rid of all the boilerplate code.
Second, we will dive into the details, e.g. how the macro is implemented.
Third, we will see how we can verify that our newly-generated instances are actually obeying the laws of the corresponding type class.

## Motivating example

Assume that you have a `case class` representing vectors in three-dimensional space:

```scala
case class Vector3D(x: Int, y: Int, z: Int)
```

Now you want to implement addition on this class.
Currently, you have to do that manually:

```scala
def +(that: Vector3D): Vector3D =
  Vector3D(this.x + that.x, this.y + that.y, this.z + that.z)
```

If you are writing some code involving three-dimensional vectors, chances are that you also have to deal with two-dimensional ones:

```scala
case class Vector2D(x: Int, y: Int) {
  def +(that: Vector2D): Vector2D =
    Vector2D(this.x + that.x, this.y + that.y)
}
```

Observe that the hand-written implementation of `+` is quite repetitive.
We want to avoid that sort of boilerplate code as much as possible.

In this post, we will introduce an abstraction over the *addition* operation, namely *semigroups*,
and introduce a macro-based facility which allows you to get the implementation of `+` for free.
In the end, the only thing you will have to write is this:

```scala
implicit val vector2DSemigroup = TypeClass[Semigroup, Vector2D]
implicit val vector3DSemigroup = TypeClass[Semigroup, Vector3D]
```

That is still a little bit of boilerplate, right? How about:

```scala
import Semigroup.auto._
```

This will give you `Semigroup` instances for *all* of your data types – with zero boilerplate!

But first, let us introduce all the related concepts properly.

## Part 1: Abstracting all the things

<div class="side-note">
  If you are already familiar with type classes in general and algebraic structures in particular, you can safely skip this and the next section.
  Keep in mind though that we are dealing with classes for types of kind $*$ only. Type classes for $* \rightarrow *$ are different and not supported.
</div>

Type classes are an incredibly useful abstraction mechanism, originally introduced in Haskell.
If you have been using some of the typelevel.scala libraries already, you probably know how type classes and their instances are represented in Scala: as traits and implicits.
In the following section, we will get started with an example type class from abstract algebra, which is implemented in *spire*.

### Group theory

Group theory is a very important field of research in mathematics and has a very broad range of applications, especially in computer science.
One of the most fundamental structures is a *semigroup*, which consists of a set of elements equipped with one operation (often called *append*, *mplus*, or similarly; in textbooks you will often find $\circ$ or $\oplus$).
Additionally, the operation has to obey the *law of associativity*, meaning that for any three values $s\_1, s\_2,$ and $s\_3$, it does not matter if you append $s\_1$ and $s\_2$ first and then append $s\_3$, or append $s\_2$ and $s\_3$ first and then append $s\_1$ and the result of that.
In other words, the precise order in which the steps of a larger operation are executed does not matter.
A good analogy here is when flattening a list:
On the surface, you just do not care if it proceeds by splitting the list recursively or if the concatenation is done sequentially by folding.

<div class="side-note">
  In fact, some list operations actually require associativity. From the Scaladoc of the <code>fold</code> method on <code>Seq</code>:
  <blockquote>
    Folds the elements of this collection or iterator using the specified associative binary operator.
    The order in which operations are performed on elements is unspecified and may be nondeterministic. 
  </blockquote>
  This allows a particular collection implementation to use whichever order is most efficient.
</div>

Lists are already a good example for a semigroup: Any `List[T]` is a semigroup, with the semigroup operation being list concatenation!
A `Map[K, V]` is a semigroup too, given that `V` is a semigroup.
The operation is just "merging" two maps, and if you have two duplicate keys, you can use the semigroup operation for `V`.

Enough examples. We can represent the concept of a semigroup in Scala using a trait:

```scala
trait Semigroup[S] {
  def append(s1: S, s2: S): S
}
```

Obviously, we can also implement a semigroup for base types like `Int`. An instance could look like this:

```scala
implicit val intInstance = new Semigroup[Int] {
  def append(s1: Int, s2: Int) = s1 + s2
}
```

In other words, we just use the built-in addition function.

<div class="side-note">If you want to know more about applications of abstract algebra in programming, especially in <em>spire</em>, head over to YouTube and watch <a href="http://www.youtube.com/watch?v=xO9AoZNSOH4">an introduction by Tom Switzer</a>.</div>

### Composing instances

Now suppose you are working with three-dimensional images.
Most likely, you will encounter a data structure for vectors (or points), which we recall from above:

```scala
case class Vector3D(x: Int, y: Int, z: Int)
```

And since you know your maths, you also know that vectors can be added, and that vector addition forms a semigroup!
Hence, a semigroup instance for `Vector3D` is the next logical step.

```scala
implicit val vectorInstance = new Semigroup[Vector3D] {
  def append(u: Vector3D, v: Vector3D) =
    Vector3D(u.x + v.x, u.y + v.y, u.z + v.z)
}
```

Now, that was a bit tedious, right? We would love to have a way the compiler could write that instance for us.
(I mean, it already generates reasonable defaults for `equals`, `hashCode` and `toString`, so why not for that?)

In any case, you can see a pattern here: Each element of the case class is added separately.
Here, we could have even delegated the addition to our `intInstance` from above.

In essence, what we need is a way to combine smaller instances (e.g. for `Int`) into larger instances (e.g. for `Vector3D` consisting of three `Int`s).
Luckily, this is completely mechanic. As an exercise, try writing the following instance:

```scala
implicit def tupleInstance[A, B](implicit A: Semigroup[A], B: Semigroup[B]) =
  new Semigroup[(A, B)] {
    def append(t1: (A, B), t2: (A, B)): (A, B) = ???
  }
```

### Representing data types

Once we know how to produce an instance for a pair, we can apply that two times and obtain an instance for a triple.
However, there are still two problems here:

1. We would like an instance for `Vector3D`, but we have an instance for `(Int, Int, Int)`.
2. This is still a lie. We actually have an instance for `(Int, (Int, Int))`.

Let us address these problems now. The following sections assume familiarity with `HList`s, as implemented in *shapeless*.

<div class="side-note">
  If you are not familiar with <code>HList</code>s yet,
  watch Miles Sabin's <a href="http://www.youtube.com/watch?v=GDbNxL8bqkY">talk about <em>shapeless</em></a> at the Northeast Scala Symposium 2012.
  There's also a <a href="http://apocalisp.wordpress.com/2010/06/08/type-level-programming-in-scala/">blog series</a> exploring type-level programming in general by Mark Harrah.
</div>

Now, we want to generate an instance for `Vector3D` and countless other data types.
That means that we cannot just special-case for every possible data type, but we have to abstract over them.
The trick is actually quite simple:
For the purposes of automatic instance derivation, we temporarily convert data types into a canonical *representation* using `HList`s, where each case class parameter corresponds to an element in the `HList`.

In our example, that representation is `Int :: Int :: Int :: HNil`.
Yes, that type is completely equivalent to `Vector3D`, and you can implement the conversion functions straightforwardly:

```scala
def to(vec: Vector3D): Int :: Int :: Int :: HNil =
  vec.x :: vec.y :: vec.z :: HNil

def from(hlist: Int :: Int :: Int :: HNil) =
  ??? // fun exercise!
```

Because we are lazy, we let a macro automatically generate the `to` and `from` methods.
We will see in the second part of the series how that works.
For now, just assume that you can invoke some method, magic happens, and you get the conversions out.

### Using the representation

At this point, we have a canonical representation for arbitrary case classes.
We will also assume that there are `Semigroup` instances for each of its elements.
Now we would like to combine those base instances into an instance for the representation.
We need two implicits for that:

```scala
implicit val nilInstance =
  new Semigroup[HNil] {
    def append(x: HNil, y: HNil) = HNil
  }

implicit def consInstance[H, T <: HList](implicit val H: Semigroup[H], T: Semigroup[T]) =
  new Semigroup[H :: T] {
    // actual implementation doesn't matter that much
    def append(x: H :: T, y: H :: T) = ???
  }
```

The key insight is that the compiler can come up with an instance for `Int :: Int :: Int :: HNil`, just because these two implicits are in scope.

Now we just need a way to get an instance for `Vector3D`.

```scala
def subst[A, B](to: A => B, from: B => A, instance: Semigroup[B]) =
  new Semigroup[A] {
    def append(a1: A, a2: A) =
      from(instance.append(to(a1), to(a2))
  }
```

Easy enough, right?
To get our much-wanted `Semigroup[Vector3D]`, we ask the compiler to make an instance its `HList` representation, conjure the conversion functions and plug all that stuff into the `subst` machine. Voilà, done!
Add some teaspoons of macros, and we are able to write

```scala
Semigroup.derive[Vector3D]
```

Are we done yet? No. We can go even further.

### Abstracting over type classes

`Semigroup` is not the only type class around. For example, there is a whole tower of classes from group theory for varying use cases. Then there are some type classes from _scalaz_:

* `Show` provides a way to convert a value to a `String`
* `Equal` for type-safe equality
* `Order` provides total ordering on values

... and many more!

Another key insight is that _all_ of those classes are able to deal with `HLists` and also support the `subst` operation.
Hence, one could be tempted to write:

```scala
Show.derive[Vector3D]
Equal.derive[Vector3D]
// and more
```

I hate duplication, though. I do not want to implement the `derive` macro over and over again.
Now, if only there was a way to abstract over common functionality of types ...

### A type class called "TypeClass"

"What," I hear you saying, "the `TypeClass` type class? You can't be serious!"

I am serious.

We use type classes to abstract over types.
`Semigroup` abstracts over types which offer some sort of addition functionality.

However, type classes are themselves just types in Scala.
Thus, we can use type classes to abstract over type classes.
We are defining a type class which abstracts over type classes whose instances can be combined to form larger instances.

So, without further ado:

```scala
trait TypeClass[C[_]] {
  def nil: C[HNil]
  def cons[H, T <: HList](H: C[H], T: C[T]): C[H :: T]
  def subst[A, B](to: A => B, from: B => A, instance: C[B]): C[A]
}
```

This should actually be not too surprising. We already know exactly how to implement `TypeClass[Semigroup]`.
If we put this implementation into the companion object of `Semigroup`, it will be available for the macro to use.

### Wrapping it up

How can this actually be used?
The work can be roughly divided between three roles:

1. The macro author, who has to implement all the nitty-gritty details of the derivation process.

   That is already done and implemented in _shapeless_.
   The upcoming 2.0.0 release will contain all the necessary bits and pieces, but requires at least Scala 2.10.2 (it will not work for 2.10.1 or earlier).
   If you are brave, try the latest snapshot version which is available on Sonatype.
2. The library author, who defines type classes, fundamental instances thereof, and of course the necessary `TypeClass` instances.

   These are usually contained in the libraries you use, but the last part will additionally require a bridge library.
   But fear not, those bridge libraries already exist, at least for the typelevel.scala libraries, and can be readily added as dependency for your build.
   Head over to the <a href="https://github.com/typelevel/shapeless-contrib#readme">GitHub project</a>, we will keep you posted for when a new version comes out.
   We also plan to put a compatibility chart on this site.
3. The library user, who defines data types and wants to get instances without all the boilerplate.

   This is the simplest task of all: All you have to do is to put

   ```scala
   implicit val myInstance = TypeClass[Semigroup, Vector3D]
   // or
   import Semigroup.auto._
   ```

   somewhere into your scope, and you are done!

   Providing "explicit" implicit declarations for each type class instance provides the tightest control over your implicit scope and ensures you only have the instances that you want.
   Importing `auto` reduces the boilerplate to the absolute minimum, which is often desirable, but might result in more instances being materialized than you expect.
   Which to choose is partly a matter of taste and partly a function of the size and complexity of the scopes you are importing in to:
   large or complex scopes might favour explicit declarations; tighter, simpler scopes might favour `auto`.


### What's next?

The next article in this series will:

* explore implementation details of the macro, and off-loading some work to the compiler
* introduce another operation on type class instances beyond products
* provide more examples


<div class="updated">
  <strong>Edit:</strong> This post has been updated to expand the motivating example and change the wording a little. Thanks to Miles Sabin for his suggestions.
</div>
