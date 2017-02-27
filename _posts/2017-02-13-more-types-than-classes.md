---
layout: post
title: There are more types than classes

meta:
  nav: blog
  author: S11001001
  pygments: true

tut:
  scala: 2.12.1
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.1
---

As programmers, we are very incautious with our use of the word
“type”. The concept of “type” is sufficiently abstract and specific
that we are tempted to understand it by analogy, so much that we begin
to confuse analogy with sameness.

The colloquial “runtime type”, a fair approximation of “class”, makes
it tempting to equate _types_ with “classes, interfaces, traits, that
sort of thing”, which I will name _classes_ for the rest of this
article. But they aren’t the same. The type system is much richer and
more interesting than the class system, even in Java.

To appreciate this richness, we must stop thinking of types as classes
and stop drawing conclusions from that weak analogy. Luckily, the
compiler will readily reveal how unlike classes types are, if we ask
it some simple questions.

## One value with class, many variables with type

```scala
val greeting: String = "hi there!"
```

Here I have constructed a `String` and assigned it to a variable. (I
have also constructed the `char` array in the `String` and various
other details, but immediately handed those off to the `String` and
forgotten about them.) This value has class `String`. It has several
classes, really.

1. `String`
2. `java.io.Serializable`
3. `CharSequence`
4. `Comparable[String]`
5. `Object`/`AnyRef`

That seems like a lot of classes for one value. And they are genuine
classes of `greeting`, though 2-5 are all implied by #1.

`greeting` also has all five of these _types_. We can ask the compiler
to verify that this _type_ truth holds, entirely separately from the
class truth.

```scala
scala> (greeting: String, greeting: java.io.Serializable,
        greeting: CharSequence, greeting: Comparable[String],
        greeting: AnyRef)
res3: (String, java.io.Serializable, CharSequence,
       Comparable[String], AnyRef) =
  (hi there!,hi there!,hi there!,hi there!,hi there!)
```

So we have exhausted the classes, but aren’t quite done with types.

```scala
scala> greeting: greeting.type
res0: greeting.type = hi there!
```

`greeting.type` is not like the other five types we just tested. It is
a strict subtype of `String`, and has no class with the same name.

```scala
// If and only if call compiles, A is a subtype of B.
def conformance[A, B >: A]: Unit = ()

scala> conformance[greeting.type, String]

scala> conformance[String, greeting.type]
<console>:14: error: type arguments [String,greeting.type] do not conform
              to method conformance's type parameter bounds [A,B >: A]
```

Fine, we can accept that object identity is represented at the type
level without our universe imploding, by inventing the theory that
this is about object identity; hold on, though:

```scala
scala> val salutation = greeting
salutation: String = hi there!
```

Fine, `salutation` is just another name for `greeting`, right?

```scala
scala> conformance[salutation.type, String]

scala> implicitly[greeting.type =:= salutation.type]
<console>:14: error: Cannot prove that greeting.type =:= salutation.type.
```

Now we have seven. I’ll spare you spelling out the induction: each new
variable defined like `salutation` will yield a new alias with a
distinct type. This is not about _objects_; this is about _variables_!

```scala
// find a type for the literal "hi there!"
scala> val literalHiThere = shapeless.Witness("hi there!")
literalHiThere: shapeless.Witness.Aux[String("hi there!")] = shapeless.Witness$$anon$1@1d1537bb

scala> conformance[greeting.type, literalHiThere.T]
<console>:15: error: type arguments [greeting.type,literalHiThere.T] do not conform
              to method conformance's type parameter bounds [A,B >: A]

scala> conformance[literalHiThere.T, greeting.type]
<console>:15: error: type arguments [literalHiThere.T,greeting.type] do not conform
              to method conformance's type parameter bounds [A,B >: A]
```

As local variables are a strictly compile-time abstraction, and we
have anyway seen that the numbers don’t match up, that should be the
end of the “types are classes” confusion for you. But maybe this is
just some Scala oddity! And anyhow I haven’t even begun to demonstrate
the overwhelming richness of the type model as it blindingly outshines
the paucity of the class model. Let’s go further.

## No values, infinite types: method type parameters

To our small program of a greeting, we can add a small method.

```scala
def pickGreeting[G](grt: G, rand: Int) = grt

scala> pickGreeting(greeting, 42)
res9: String = hi there!
```

It seems like `G` must be `String`, because the argument passed to
`pickGreeting` is a string, and in that case so must its return value
be, according to the implementation. And from the perspective of this
call, [outside]({% post_url 2016-01-28-existential-inside %})
`pickGreeting`’s implementation, it is `String` indeed.

But that implementation’s perspective matters, too; it is also part of
our program. And it sees things quite differently. We can ask its
thoughts on the matter by adding to its body

```scala
def pickGreeting[G](grt: G, rand: Int) = {
  implicitly[G =:= String]
  grt
}

<console>:12: error: Cannot prove that G =:= String.
         implicitly[G =:= String]
                   ^
```

In fact, `G` bears no direct relationship to `String` at all.

```scala
// replace implicitly with
conformance[G, String]

<console>:13: error: type arguments [G,String] do not conform
              to method conformance's type parameter bounds [A,B >: A]
         conformance[G, String]
                    ^

// or with
conformance[String, G]

<console>:13: error: type arguments [String,G] do not conform
              to method conformance's type parameter bounds [A,B >: A]
         conformance[String, G]
                    ^
```

Let’s apply the pigeonhole principle. Imagine that you had a list of
every class that ever was or ever will be. Imagine that, somehow, all
of these classes, from `String` to
`AbstractFactoryMethodProxyBuilder`, were on your classpath, available
to your program.

Next, imagine that you had the time and inclination to try the `=:=`
test with every last one of these classes.

```scala
implicitly[G =:= javax.swing.JFrame]
implicitly[G =:= AbstractFactoryMethodProxyBuilder]
// ad [in]finitum
```

Your search will be futile; every class on your list-of-every-class
will give the same compiler error we got with `String`.

So, since `G` is not equal to anything on this list, it must be
something else that doesn’t appear on the list. Because this list
contains all classes, `G` must be something other than a class.

### It must not necessarily be anything

It seems like it might be convenient to say “well, in this program `G`
is only ever `String` by substitution, so therefore it is, even if the
compiler doesn’t see that.” However, thinking like this misses out on
the second key advantage of type parameterization, the one not based
on multiplicity of substitution, or the type-safety of callers:
blindness.

The implementation of type-parameterized classes and methods are
required to treat each type parameter uniquely, uniformly, and without
prejudice. The compiler enforces this by making the implementation
blind to what that parameter, like `G`, could be. It can only use what
the caller, the “outside”, has told it about `G`—arguments whose
types contain `G`, like `List[G]`, `(G, G) => G`, or `G` itself, like
the argument to `pickGreeting`. This
is
[information-hiding at the type level]({% post_url 2016-03-13-information-hiding %});
if you find information-hiding a useful tool for implementing correct
programs, you will find the same of the fresh, unique, and mysterious
types induced by each introduction of a type parameter.

<div class="side-note">
  Each operation a language permits by default, not via an argument,
  on values of a type parameter is a leak in this abstraction. This
  includes testing the value’s class, converting to string, and
  comparing to other values of supposedly utter mystery for
  equality. The ability to create a “default” value is also a leak. A
  function is always permitted to ask only that of these that it needs
  from the caller; make them default, and this design choice is taken
  away. That is why <code>Object#equals</code> is little better for
  type-safety than reflection-based calls, and why total type erasure
  is a desirable feature rather than a design flaw—plugging these
  leaks gives the programmer as much freedom to abstract by
  information-hiding as she wishes.
</div>

### How many calls are there?

Put another way, when implementing the code in the scope of a type
parameter, your implementation must be equally valid _for all_
possible `G` substitutions, including the ones that haven’t been
invented yet. This is why we call it _universal_ quantification.

But it is not merely each declaration of a type parameter that yields
a distinct type—each call does! Consider two consecutive calls to
`pickGreeting`.

```scala
pickGreeting(greeting, 42)
pickGreeting(33, 84)
```

Externally, there are two `G` types. However, the possibility of
writing this demands another level of uniqueness treatment when
typechecking `pickGreeting`’s definition: whatever `G` is now, like
`String`, it might be something else in the next call, like `Int` in
the above example. With recursion, it might even be two different
things at the same time. There’s nothing to hold this at two, either:
there may be an unbounded number of substitutions for a given type
parameter within a single program, at a single point in time.

While `G` _may_ be the same between two invocations of `pickGreeting`,
it might not. So we have no choice but to treat the `G` types of _each
call_ as separate types. There may be infinitely many calls, so there
are so many types.

Incidentally, the same happens for singleton types. Each time `val
greeting` comes into scope, it induces a separate singleton type. It
is easy enough to arrange for an unbounded number of scope entries in
a particular program. This isn’t so practical as the type parameter
phenomenon, though.

## More types from variable copies

Suppose we’d like to wait a while to compute our greeting. We can
define a type-and-class to represent that conveniently.

```scala
// like Coyoneda Id, if that helps
sealed abstract class Later[A] {
  type I
  val i: I
  val f: I => A
}

def later[In, A](now: In)(later: In => A)
  : Later[A] = new Later[A] {
    type I = In
    val i = now
    val f = later
}

val greeting3 = later(3){
  n => List.fill(n)("hi").mkString(" ")
}
```

How many _classes_ are involved here, in the type of `greeting3`?

1. `Later`, obviously;
2. `Function1`, the `greeting3.f` overall class;
3. `String`, the output type of `greeting3.f`;
4. `Int`, the `I` type.

How many types?

The first difference is that `greeting3.I` is not `Int`.

```scala
scala> implicitly[greeting3.I =:= Int]
<console>:14: error: Cannot prove that greeting3.I =:= Int.
       implicitly[greeting3.I =:= Int]
                 ^
```

They are unrelated for much the same reason as `G` was unrelated to
`String` in the previous example: the only things code following
`val greeting3` may know are those embodied in the `greeting3.i` and
`greeting3.f` members. You can almost think of them as “arguments”.

But that’s not all.

```scala
val salut3 = greeting3

scala> greeting3.f(greeting3.i)
res11: String = hi hi hi

scala> salut3.f(salut3.i)
res12: String = hi hi hi

scala> greeting3.f(salut3.i)
<console>:14: error: type mismatch;
 found   : salut3.i.type (with underlying type salut3.I)
 required: greeting3.I
       greeting3.f(salut3.i)
                          ^

scala> implicitly[greeting3.I =:= salut3.I]
<console>:14: error: Cannot prove that greeting3.I =:= salut3.I.
```

Just like every call to `pickGreeting` induces a new `G` type, each
simple `val` copy of `greeting3` will induce a new, unique `I`
type. It doesn’t matter that they’re all the same value; this is a
matter of variables, not values, just as with singleton types.

But that’s _still_ not all.

## One value with class, many variable _references_ with types

The preceding is more delicate than it seems.

```scala
var allo = greeting3

scala> allo.f(allo.i)
<console>:13: error: type mismatch;
 found   : Later[String]#I
 required: _1.I where val _1: Later[String]
       allo.f(allo.i)
                   ^
```

All we have done differently is use a mutable `var` instead of an
immutable `val`. Why is this enough to throw a wrench in the works?

Suppose you had another _value_ of the `Later[String]` type.

```scala
val bhello = later("olleh")(_.reverse)
```

The `I` substitution here is `String`. So the `f` takes a `String`
argument, and the `I` is a `String`.

`bhello` is of a compatible type with the `allo` var. So this
assignment will work.

```scala
allo = bhello
```

In a sense, when this mutation occurs, the `I` type _also_ mutates,
from `Int` to `String`. But that isn’t quite right; types cannot
mutate.

Suppose that this assignment happened in the middle of that line of
code that could not compile. We could imagine the sequence of events,
were it permitted.

1. `allo.f` (which is `greeting3.f`) evaluates. It is the function
   `(n: Int) => List.fill(n)("hi").mkString(" ")`.
2. The `allo = bhello` assignment occurs.
3. `allo.i` (which is `bhello.i`) evaluates. It is the string
   `"olleh"`.
4. We attempt to pass `"olleh"` as the `(n: Int)` argument to complete
   the evaluation, and get stuck.

Just as it makes no difference what concrete substitutions you make
for `G`, it makes no difference whether such an assignment could ever
happen in your specific program; the compiler takes it as a
possibility because you declared a `var`. (`def allo = greeting3` gets
the same treatment, lest you think non-functional programs get to have
all the fun here.) Each _reference_ to `allo` gets a new `I` type
member. That failing line of code had two `allo` references, so was
working with two incompatible `I` types.

Since the number of references to a variable in a program is also
unbounded...you get the picture.

<div class="side-note">
  This also occurs with existential type parameters, which are equally
  expressive to type members. Accordingly, Java <em>also</em>
  generates new types from occurrences of expressions of existential
  type.
</div>

## How do we tell the two apart?

All of this is simply to say that we must be working with two separate
concepts here.

1. The _runtime_ shape and properties of the _values_ that end up
   flying around when a program actually _runs_. **This we call
   class.**
2. The _compile-time_, statically-discoverable shape and properties of
   the _expressions_ that fly around when a program is
   _written_. **This we call type.**

The case with `var` is revealing. Maybe the `I` type will always be
the same for a given mutable variable. But demonstrating that this
holds true for _one_ run of the program (#1, class) isn’t nearly good
enough to _prove_ that it will be true for _all_ runs of the program
(#2, type).

We refuse to apply the term “type” to the #1, ‘class’ concept because
it does not live up to the name. The statement “these two types are
the same” is another level of power entirely; “these two values have
the same class” is extraordinarily weak by comparison.

It is tempting to use the term “runtime type” to refer to
classes. However, in the case of Scala, as with all type systems
featuring parametric polymorphism, classes are so dissimilar to types
that the similar-sounding term leads to false intuition, not helpful
analogy. It is a detriment to learning, not an aid.

Types are compile-time, and classes are runtime.

### When are types real?

The phase separation—compile-time versus runtime—is the key to
the strength of types in Scala and similar type systems. The static
nature of types means that the truths they represent must be
universally quantified—true in all possible cases, not just some
test cases.

We need this strength because the phase separation forbids us from
taking into account anything that cannot be known about the program
without running it. We need to think in terms of “could happen”, not
“pretty sure it doesn’t”.

## How do classes give rise to types?

There appears to be some overlap between the classes of `greeting` and
its types. While `greeting` has the _class_ `String`, it also has the
_type_ `String`.

We want types to represent static truths about the expressions in a
program. That’s why it makes sense to include a “model of the classes”
in the type system. When we define a class, we also define an
associated type or family of types.

When we use a class to construct a value, as in `new Blob`, we would
like to assign as much specific meaning to that expression as we can
at compile time. So, because we know right now that this expression
will make a value of class `Blob`, we assign it the type `Blob` too.

### How do the types disappear?

There’s a common way to throw away type information in Scala,
especially popular in object-oriented style.

```scala
val absGreeting: CharSequence = greeting
```

`absGreeting` has the same value as `greeting`, so it has the same
five classes. However, it only has two of those five types, because we
threw away the other three statically. It has lost some other types,
too, namely `greeting.type`, and acquired some new ones, namely
`absGreeting.type`.

Once a value is constructed, the expression will naturally cast off
the types specifying its precise identity, as it moves into more
abstract contexts. Ironically, the best way to preserve that
information as it passes through abstract contexts is to take
advantage of purely abstract types—type parameters and type
members.

```scala
scala> pickGreeting[greeting.type](greeting, 100)
res16: greeting.type = hi there!
```

While the implementation must treat its argument as being of the
abstract type `G`, the caller knows that the more specific
`greeting.type` must come out of that process.

### How do the types come back?

There is a feature in Scala that lets you use class to get back _some_
type information via a dynamic, runtime test.

```scala
absGreeting match {
  case hiAgain: String =>
    conformance[hiAgain.type, String] // will compile
}
```

The name “type test” for this feature is poorly chosen. The
_conclusion_ affects the type level—`hiAgain` is, indeed, proven
statically to be of type `String`—but the _test_ occurs only at
the class level.

The compiler will tell you about this limitation sometimes.

```scala
def pickGreeting2[G](grt: G, rand: Int): G =
  ("magic": Any) match {
    case ok: G => ok
    case _ => sys error "failed!"
  }

<console>:13: warning: abstract type pattern G is unchecked
              since it is eliminated by erasure
           case ok: G => ok
                    ^
```

But reflecting the runtime classes back to compile-time types is a
subtle art, and the compiler often can’t explain exactly what you got
wrong.

```scala
def pickGreeting3[G](grt: G, rand: Int): G =
  grt match {
    case _: String =>
      "Surely type G is String, right?"
    case _ => grt
  }

<console>:14: error: type mismatch;
 found   : String("Surely type G is String, right?")
 required: G
             "Surely type G is String, right?"
             ^
```

I’ve touched upon this
mistake
[in previous articles]({% post_url 2014-07-06-singleton_instance_trick_unsafe %}#types-are-erased),
but it’s worth taking at least one more look. Let’s examine how
tempting this mistake is.

`String` is a `final class`. So it is true that `G` can contain no
more specific class than `String`, if the first `case` matches. For
example, given `trait MyAwesomeMixin`, `G` cannot be
`String with MyAwesomeMixin` if this `case` succeeds, because that
can’t be instantiated; you would need to create a subclass of `String`
that implemented `MyAwesomeMixin`.

This pattern match isn’t enough evidence to say that `G` is exactly
`String`. There are still other class-based types it could be, like
`Serializable`.

```scala
pickGreeting3[java.io.Serializable](greeting, 4055)
```

Instead, it feels like this pattern match confirms `Serializable` as a
possibility, instead of denying it.

But we don’t need `G = String` for this code to compile; we only need
`G >: String`. If that was true, then `"Surely type G is String,
right?"`, a `String`, could simply upcast to `G`.

However, even `G >: String` is unproven. There are no subclasses of
`String`, but there are infinitely many _subtypes_ of
`String`. Including the `G` created by each entry into
`pickGreeting3`, every abstract and existential type bounded by
`String`, and every singleton type of `String` variable definitions.

This mistake is, once again, confusing a demonstration of one case
with a proof. Pattern matching tells us a great deal about one value,
the `grt` argument, but very little about the type `G`. All we know
for sure is that “`grt` is of type `G`, and also of type `String`, so
these types overlap by at least one value.” In the type system, if you
don’t know something for sure, you don’t know it at all.

## Classes are a concrete source of values

In the parlance of functional Scala, concrete classes are often called
“data constructors”.

When you are creating a value, you must ultimately be concrete about
its class, at the bottom of all the abstractions and indirections used
to hide this potentially messy detail.

```scala
scala> def pickGreeting4[G]: G = new G
<console>:12: error: class type required but G found
       def pickGreeting4[G]: G = new G
                                     ^
```

You’ll have to do something else here, like take an argument
`() => G`, to let `pickGreeting4` construct `G`s.

The truly essential role that classes play is that they encapsulate
instructions for constructing concrete values of various types. In a
safe program, this is the only feature of classes you’ll use.

In Scala, classes leave fingerprints on the values that they
construct, without fail. This is merely an auxiliary part of their
primary function as value factories, like a “Made in `class Blah`”
sticker on the back. Pattern matching’s “type tests” work by checking
this fingerprint of construction.

## Most runtime “type test” mechanisms do not work for types

These fingerprints only come from classes, not types. So “type tests”
only work for “classy” types, like `String` and `MyAwesomeMixin`. They
also work for specific singleton types because construction also
leaves an “object identity” fingerprint that the test can use.

The
[`ClassTag` typeclass](http://www.scala-lang.org/api/2.12.1/scala/reflect/ClassTag.html) does
not change this restriction. When you add a `ClassTag` or `TypeTag`
context bound, you also prevent that type parameter from working with
most types.

```scala
scala> implicitly[reflect.ClassTag[greeting3.I]]
<console>:14: error: No ClassTag available for greeting3.I
       implicitly[reflect.ClassTag[greeting3.I]]
                 ^
```

As such, judicious use of `ClassTag` is not a great solution to
excessive use of type tests in abstract contexts. There are so many
more types than classes that this is to confine the expressivity of
your types to a very small, class-reflective box. Set them free!

## “But doesn’t Python/JavaScript/&c have both types and classes at runtime?”

In JavaScript, there’s a very general runtime classification of values
called “type”, meant to classify built-in categories like `string`,
`number`, and the like.

```javascript
>> typeof "hi"
"string"
>> typeof 42
"number"
>>> typeof [1, 'a']
"object"
```

Defining a class with the new `class` keyword doesn’t extend this
partition with new “types”; instead, it further subdivides _one_ of
those with a separate classification.

```javascript
>> class Foo() {}
>> class Bar() {}
>> typeof (new Foo)
"object"
>> typeof (new Bar)
"object"
>> new Foo().constructor
function Foo()
>> new Bar().constructor
function Bar()
```

So, if you treat JavaScript’s definition of the word “type” as
analogous to the usage in this article, then yes, JavaScript has
“runtime types”.

But JavaScript can only conveniently get away with this because its
static types are uninteresting. It has one type—the type of all
values—and no opportunities to do interesting type-level modeling,
at least not as part of the standard language.

Hence, JavaScript is free to repurpose the word “type” for a flavor of
its classes, because our “types” aren’t a tool you make much use of in
JavaScript. But when you come back to Scala, Haskell, the ML family,
et al, you need a word for the static concept once again.

## Thinking about types as _just_ classes leads to incorrect conclusions

Setting aside the goal of principled definition of terms, this
separation is the one that makes the most sense for a practitioner of
Scala. Consider the practicalities:

Types and classes have different behavior, are equal and unequal
according to different rules, and there are a lot more types than
classes. So we need different words to distinguish them.

Saying “compile-time type” or “runtime type” is not a practical
solution—no one wants to speak such an unwieldy qualifier every
time they refer to such a commonly-used concept.

While I’ve given a sampling of the richness of the type system in this
article, it’s not necessary to know that full richness to appreciate
or remember the difference between the two: types are static and
compile-time; classes are dynamic and runtime.

*This article was tested with Scala 2.12.1, Shapeless 2.3.2, and
Firefox 53.0a2.*
