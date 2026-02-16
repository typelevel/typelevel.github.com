{%
  author: ${S11001001}
  date: "2017-12-20"
  tags: [technical]
%}

# Who implements the typeclass instance?

The typeclass pattern in Scala invites you to place
implementation-specific knowledge directly in the typeclass instances,
with the interface defined as the typeclass’s abstract interface.

However, GADTs permit a different organization of code. It is even
possible to define a typeclass that seems to do nothing at all, yet
still permits full type-safe typeclass usage.

The possibilities between these two extremes form a design space. If
you wish to practice ad-hoc polymorphism in Scala, this space is well
worth exploring.

## A glorified overloader

Refactoring a set of overloads into a typeclass is a fine way to get
some free flexibility and dynamism, because expressing overloads as a
typeclass gives you free fixes for common overload problems.

1. Methods calling the overloaded method do not themselves need to be
   overloaded just to avoid suppressing the flexibility of the
   overload beneath.  (See `addThree` and `zipAdd` below for
   examples.)
1. Return-type overloading works, even in Scala, where it does not
   when attempting to write overloads in the Java style, i.e. multiple
   methods with the same name.
1. Overloads may be defined as recursive type rules, admitting a
   combinatorial explosion or even infinite “effective overloads”.

Let’s make a quick example of something like a typical overload.

```scala
object OverAdd {
  def add(x: Int, y: Int): Int = x + y
  
  def add(x: String, y: String): String = s"$x$y"
  
  def add[A](l: Vector[A], r: Vector[A]): Vector[A] =
    l ++ r
}
```

This mechanically translates to a newly introduced type, some implicit
instances of that type, and a function to let us call `add` the same
way we used to.

```scala
// typeclasses are often defined with trait, but this is not required
final case class Adder[A](addImpl: (A, A) => A)

// easier if all implicits are in this block
object Adder {
  implicit val addInts: Adder[Int] = Adder((x, y) => x + y)
  
  implicit val addStrings: Adder[String] =
    Adder((x, y) => s"$x$y")
    
  implicit def addVects[A]: Adder[Vector[A]] =
    Adder((l, r) => l ++ r)
}

// and to tie it back together
def add[A](x: A, y: A)(implicit adder: Adder[A]): A =
  adder.addImpl(x, y)
```

## Overloaded wrapping without overloading

While a bit more ceremonious, this allows us to write some nice
functions more easily. Here’s a function to add three values.

```scala
def addThree[A: Adder](l: A, m: A, r: A): A =
  add(l, add(m, r))
```

`addThree` supports all three “overloads” of `add`.

```scala
scala> addThree(1, 2, 3)
res0: Int = 6

scala> addThree("a", "ba", "cus")
res1: String = abacus

scala> addThree(Vector(1, 1), Vector(0, 0), Vector(1, 0, 0, 1))
res2: scala.collection.immutable.Vector[Int] =
  Vector(1, 1, 0, 0, 1, 0, 0, 1)
```

With the overload style, we need three variants of this function, too,
each with the exact same body. The typeclass version need only be
written once, and automatically supports new overloads, that is, new
instances of `Adder`.

Same with this function.

```scala
def zipAdd[A: Adder](l: List[A], r: List[A]): List[A] =
  l zip r map {case (x, y) => add(x, y)}
```

Functions like `addThree` and `zipAdd` are called *derived
combinators*. The more that you can do in derived combinators, the
more abstract and orthogonal your program will be.

```
   +=============+            |   +=============+
   |   derived   |  (open     |   |  primitive  |  (closed
   | combinators |    set)    |   | combinators |      set)
   +=============+            |   +=============+
                              |
   +----------+               |    +----------+
   | addThree |---→---→---→---→---→| Adder    |
   +----------+       calls   |    | -addImpl |    +===========+
    ↑                       |→---→ +----------+    | Instances |
    | +--------+            | |                    +===========+
    | | zipAdd |---→---→---→- |
    | +--------+    calls     |  +------+ +---------+ +-------+
    ↑        ↑                |  | Ints | | Strings | | Vects |
    |   calls|                |  +------+ +---------+ +-------+
    |      +-----+            |      |
    |      | ??? |            |      |
    ↑      +-----+            |      |
    |  (derived combinators          ↓
    |   can derive from each other)  ---→---→---→
    |                                           |
    ↑          -------------------------------  |
    |          To evaluate `addThree(1, 2, 3)`  |
    |          -------------------------------  ↓
    |          1. Fetch `Adder` implicitly      |
    |-←---←---←---←---←---←---←---←---←---←---←-|
               2. Pass to `addThree`
               3. `addThree` uses the abstract interface to
                  invoke the primitive `add` combinator on what,
                  to it, is an abstract type, `A`.
```

## Infinite overloads via recursion

Making derived combinators easier to write is very useful, but
typeclasses go further by letting you describe overloading rules that
would be impossible with normal overloading.

Given that I can add `Int` and `Int` together, I should be able to add
`(Int, Int)` and `(Int, Int)` to get `(Int, Int)`.

```scala
implicit val addIntPairs: Adder[(Int, Int)] =
  Adder{case ((x1, x2), (y1, y2)) =>
    (x1 + y1, x2 + y2)}
    
scala> add((2, 7), (3, 8))
res3: (Int, Int) = (5,15)
```

But I should also be able to add pairs of `String`. And `(Int,
String)` pairs. And `(String, Vector[Boolean])` pairs. And pairs of
pairs of pairs.

Typeclasses let you declare newly supported types recursively, with an
implicit argument list to the `implicit def`.

```scala
implicit def addPairs[A: Adder, B: Adder]
    : Adder[(A, B)] =
  Adder{case ((a1, b1), (a2, b2)) =>
    (add(a1, a2), add(b1, b2))
  }
```

## Surely this must be going somewhere new

If you’re familiar with type classes, all this must be old hat. But
this time, we’re going to expand the boundaries of the typeclass
design space, by exploiting *GADT pattern matching*.

We could have designed the `Adder` type class to include `addThree` as
a primitive combinator, and implemented it afresh for each of the four
instances we’ve defined so far, as well as any future instances
someone might define. Thinking orthogonally, however, shows us that
there’s a more primitive concept which strictly generalizes it: if we
primitively define a two-value adder, we can use it to add three
items, simply by using it twice.

This has a direct impact on how we structure the functions related to
`Adder`. The primitives must be split up, their separate
implementations appearing directly in the implicit instances. Derived
combinators may occur anywhere that is convenient to us: outside the
typeclass for full flexibility of location, or within the typeclass
for possible overrides for performance.

But how much of the primitive implementations must occur in the
instances, really?

## Empty tags as instances

There is a progression of design refinements here.

1. Ad hoc overloads, Java-style, impossible to abstract over.
1. Flip into a typeclass.
1. Refine the primitive/derived distinction to minimize code in
   instances.

For some typeclasses, *no* code needs to be put in the instances. For
example, if we want to support only `Int`, `String`, and `Vector`,
here is a perfectly sensible typeclass definition.

```scala
sealed trait ISAdder[A]

object ISAdder {
  implicit object AddInts extends ISAdder[Int]
  implicit object AddStrs extends ISAdder[String]
  
  final case class AddVects[E]() extends ISAdder[Vector[E]]
  
  implicit def addVects[E]: ISAdder[Vector[E]] =
    AddVects()
}
```

If the instances cannot add values of the types indicated by the type
parameters, surely that code must exist somewhere! And it has a place,
in the definition of `add`.

If you recall, this method merely called `addImpl` on the typeclass
instance before. Now there is no such thing; the instances are empty.

Well, they are not quite empty; they contain a type.  So we can define
`add`, with complete type safety, as follows.

```scala
def isadd[A](x: A, y: A)(implicit adder: ISAdder[A]): A =
  adder match {
    case ISAdder.AddInts => x + y
    case ISAdder.AddStrings => s"$x$y"
    case ISAdder.AddVects() =>
      x ++ y
  }
```

More specifically, they contain a runtime tag, which allows
information about the type of `A` to be extracted with a pattern
match. For example, determining that `adder` is `AddInts` reveals that
`A = Int`, because that’s what the `extends` clause says.  This is
*GADT pattern matching*.

The `Vector` case is a little tricky here, because we can only
determine that `A` is `Vector[e]` *for some unknown e*, but that’s
enough information to invoke `++` and get a result also of `Vector[e]`
for the same `e`.

You can see this in action by using a [variable type
pattern](https://groups.google.com/d/msg/scala-user/JlCsy48poIU/DjsQDnzeZboJ)
to assign the name `e` (a lowercase type parameter is required for
this usage), so you can refer to it in types.

```scala
    case _: ISAdder.AddVects[e] =>
      (x: Vector[e]) ++ y
```

## The lowercase `e` names a GADT skolem

In the `AddVects[e]` pattern immediately above, `e` is a *variable
type pattern*. This is a type that exists only in the scope of the
`case`.

It’s *existential* because we don’t know what it is, only that it is
*some type* and we don’t get to pick here what that is. In this way,
it is no different from a type parameter’s treatment by the
implementation, which is
[existential on the inside]({% post_url 2016-01-28-existential-inside %}).

It’s a *GADT skolem* because it was bound by the pattern matching
mechanism to a “fresh” type, unequal to any other. Recall the way
`AddVects` was defined:

```scala
AddVects[E] extends ISAdder[Vector[E]]
```

Matching `ISAdder` with `AddVects` doesn’t tell us anything about
bounds on the type passed to `AddVects` at construction time. This
isn’t true of all
[GADT skolems]({% post_url 2016-09-19-variance-phantom %}#a-gadt-skolem),
but is only natural for this one.

`scalac` will create this GADT skolem *regardless of whether we give
it a name*. In the pattern `case AddVects()`, it’s still known that
`A = Vector[e]` for some `e`; the only difference is that you haven’t
bound the `e` name, so you can’t actually refer to this *unspeakable*
type.

Usually, you do not need to assign names such as `e` to such types;
`_` is sufficient.  However, if you have problems getting `scalac` to
apply all the type equalities it ought to know about, a good first
step is to assign names to any skolems and try type
ascriptions. You’ll need a variable type pattern in other situations
that don’t infer, too. By contrast, with the `e` name bound, we can
confirm that `x: Vector[e]` in the above example, and `y` is
sufficiently well-typed for the whole expression to type-check.

## Porting `addPairs` and other recursive cases

Suppose we add support for pairs to `ISAdder`.

```scala
final case class AddPairs[A, B](
    val fst: ISAdder[A],
    val snd: ISAdder[B]
  ) extends ISAdder[(A, B)]
```

This *should* permit us to pattern-match in `isadd` to make complex
determinations about the `A` type given to `isadd`. This *ought to be*
a big win for GADT-style typeclasses, allowing “short-circuiting”
patterns that work in an obvious way.

```scala
// this pattern means A=(Int, String)
case AddPairs(AddInts, AddStrs) =>

// this pattern means A=(ea, Vector[eb])
// where ea and eb are GADT skolems
case AddPairs(fst, _: AddVects[eb]) =>

// here, A=(ea, eb) (again, GADT skolems)
// calling `isadd` recursively is the most
// straightforward implementation
case AddPairs(fst, snd) =>
  val (f1, s1) = x
  val (f2, s2) = y
  (isadd(f1, f2)(fst), isadd(s1, s2)(snd))
```

The final `case`’s body is fine. `scalac` effectively introduces
skolems `ea` and `eb` so that `A = (ea, eb)`, `fst: Adder[ea]`, and so
on, and everything lines up nicely. We are not so lucky with the other
cases.

```scala
....scala:76: pattern type is incompatible with expected type;
 found   : ISAdder.AddInts.type
 required: ISAdder[Any]
      case AddPairs(AddInts, AddStrs) =>
                    ^
....scala:76: pattern type is incompatible with expected type;

 found   : ISAdder.AddStrs.type
 required: ISAdder[Any]
      case AddPairs(AddInts, AddStrs) =>
                             ^
....scala:79: pattern type is incompatible with expected type;
 found   : ISAdder.AddVects[eb]
 required: ISAdder[Any]
      case AddPairs(fst, _: AddVects[eb]) =>
                            ^
```

This is nonsensical; the underlying code is sound, we just have to go
the long way around so that `scalac` doesn’t get confused. Instead of
the above form, you must assign names to the `AddPairs` skolems as we
described above, and do a sub-pattern-match.

```scala
case p: AddPairs[ea, eb] =>
  val (f1, s1) = x
  val (f2, s2) = y
  (p.fst, p.snd) match {
    case (AddInts, AddStrs) =>
    case (fst, _: AddVects[eb]) =>
    case (fst, snd) =>
```

Note that we had to give up on the `AddPairs` pattern entirely,
because

1. More complex situations require type ascription.
1. You cannot ascribe with skolems unless you’ve bound the skolems to
   names with variable type patterns.
1. You can’t use variable type patterns with the structural
   “ADT-style” patterns; you must instead use inelegant and
   inconvenient (non-variable) type patterns. (This may be
   [improved in Typelevel Scala 4](https://github.com/typelevel/scala/blob/typelevel-readme/notes/typelevel-4.md#type-arguments-on-patterns-pull5774-paulp).)

Yet this remains entirely up to shortcomings in the current pattern
matcher implementation. An improved pattern matcher could make the
nice version work, safely and soundly.

As such, I don’t want these shortcomings to discourage you from trying
out the pure type-tagging, “GADT-style” typeclasses. It is simply
nicer for many applications, and you aren’t going to code yourself
into a hole with them, because should you wind up in the buggy
territory we’ve been exploring, there’s still a way out.

## Same typeclass, new “primitive” combinators

“Empty” typeclasses like `ISAdder` contain no implementations of
primitive combinators, only “tags”. As such, they are in a sense the
purest form of “typeclass”; *to classify types* is the beginning and
end of what they do!

Every type that is a member of the “class of types” `ISAdder` is
either

1. the type `Int`,
1. the type `String`,
1. a type `Vector[e]`, where `e` is any type, or
1. a type `(x, y)` where `x` and `y` are types that are *also* in the
   `ISAdder` class.

This is the end of `ISAdder`’s definition; in particular, there is
nothing here about “adding two values to get a value”. All that
is said is what types are in the class!

Given this ‘undefinedness’, if we have another function we want to
write over the exact same class-of-types, we can just write it without
making any changes to `ISAdder`.

```scala
def backwards[X](x: X)(implicit adder: ISAdder[X]): X = adder match {
  case AddInts => -x
  case AddStrs => x.reverse
  case _: AddVects[e] => x.reverse
  case p: AddPairs[ea, eb] =>
    val (a, b): (ea, eb) = x
    (backwards(a)(p.fst), backwards(b)(p.snd))
}
```

Set aside the question of whether the class of “backwards-able” types
ought to remain in lockstep with the class of “addable”
types. Supposing that it *should*, the class need be defined only
once.

More practically speaking, if you expose the subclasses of a typeclass
to users of your library, they can define primitives “in lockstep”,
too.  The line between primitive and derived combinators is also
blurred: a would-be derived combinator can pattern-match on the
typeclass to supply special cases for improved performance, becoming
“semi-primitive” in the process.  You decide whether these are good
things or not.

## Hybrid “clopen” typeclasses

Pattern-matching typeclass GADTs is subject to the same exhaustiveness
concerns and compiler warnings as pattern-matching ordinary ADTs. If
you eliminate a `case` from `def isadd`, you’ll see something like

```scala
....scala:57: match may not be exhaustive.
It would fail on the following input: AddInts
    adder match {
    ^
```

We could unseal `ISAdder`, which would eliminate the warning, but
wouldn’t really solve anything. The function would still crash upon
encountering the missing case.

Pattern matches of unsealed hierarchies typically include a “fallback”
case, code used when none of the “special” cases match. However, for
pure typeclasses like `ISAdder`, this strategy is a dead end
too. Consider a hypothetical fallback case.

```scala
  case _ => ???
```

Each of the other patterns in `isadd`, by their success, taught us
something useful about the `A` type parameter. For example, `case
AddInts` tells us that `A = Int`, and accordingly `x: Int` and `y:
Int`. It also meant that the expected result type of that block is
also `Int`. That’s plenty of information to actually implement
“adding”.

By contrast, `case _` tells us *nothing* about the `A` type. We don’t
know anything new about `x`, `y`, or the type of value we ought to
return. All we can do is return either `x` or `y` without further
combination; while this is a sort of “adding” [in abstract
algebra](https://hackage.haskell.org/package/base-4.10.1.0/docs/Data-Monoid.html#t:First),
there’s a good chance it’s not really what the caller was expecting.

Instead, we can reformulate a closed typeclass like `ISAdder` with one
extension point, where the typeclass is specially encoded in the usual
“embedded implementation” style. It’s closed and open, so
[“clopen”](https://mail.haskell.org/pipermail/haskell-cafe/2014-April/113373.html).

## `sealed` doesn’t seal subclasses

Our GADT typeclass instances work by embedding type information within
the instances, to be rediscovered at runtime. To support open
extension, we need a data case that contains *functions* instead of
types. We know how to encode that, because that is how standard,
non-GADT typeclasses work.

```scala
sealed trait ISOAdder[A]

trait ExtISOAdder[A] extends ISOAdder[A] {
  val addImpl: (A, A) => A
}

object ISOAdder {
  implicit object AddInts extends ISOAdder[Int]
  implicit object AddStrs extends ISOAdder[String]
  
  final class AddVects[A] extends ISOAdder[Vector[A]]
  
  implicit def addVects[A]: ISOAdder[Vector[A]] =
    new AddVects
    
  def isoadd[A](x: A, y: A)(implicit adder: ISOAdder[A]): A =
    adder match {
      case AddInts => x + y
      case AddStrs => s"$x$y"
      case _: AddVects[e] =>
        (x: Vector[e]) ++ y
      // NB: no unchecked warning here, which makes sense
      case e: ExtISOAdder[A] =>
        e.addImpl(x, y)
    }
}
```

By sealing `ISOAdder`, we ensure that the pattern match in `isoadd`
remains exhaustive. However, one of those cases, `ExtISOAdder`, admits
new subclasses, itself! This is fine because no matter how many
subclasses of `ExtISOAdder` we make, they’ll still match the last
pattern of `isoadd`.

We could also define `ExtISOAdder` as a `final case class`. The point
is that you can make this “extension point” in your otherwise-closed
typeclass using whatever style you like.

One caveat, though: “clopen” typeclasses cannot have arbitrary new
primitive combinators added to them. They are like ordinary open
typeclasses in that regard. Consider a version of `backwards` for
`ISOAdder`: what you could do in the `ExtISOAdder` case?

## Whoever you like

With type parameters vs. members, you can get pretty far with
[the “rule of thumb”]({% post_url 2015-07-13-type-members-parameters %}#when-is-existential-ok).
Beyond that, even bugs in `scalac` typechecking can guide you to the
“right” choice.

There is no similar rule for this design space. It might seem that
typeclass newcomers might have an easier time with the OO-style
“unimplemented method” signposts in the open style, but I have also
seen them lament the loss of flexibility that would be provided by the
GADT style.

Likewise, as an advanced practitioner, your heart will be rent by the
tug-of-war between the boilerplate of the open style and the
pattern-matcher’s finickiness with the GADT style. You may then be
tempted to adopt the hybrid ‘clopen’ style, but this, too, is too
often a form of design excess.

Given all that, the only help I can offer, aside from describing the
design space above, is “pick whichever you like”. You know your
program; if you are not sure which will be nicer, try both!

*This article was tested with Scala 2.12.4.*
