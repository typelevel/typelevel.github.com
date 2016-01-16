---
layout: post
title: To change types, change values

meta:
  nav: blog
  author: S11001001
  pygments: true
---

*This is the seventh of a series of articles on “Type Parameters and
Type Members”.  You may wish to
[start at the beginning]({% post_url 2015-07-13-type-members-parameters %});
more specifically, this post is meant as a followup to
[the previous entry]({% post_url 2015-07-30-values-never-change-types %}).
However, in a first for this series, it stands on its own, as
introductory matter.*

A program is a system for converting data from one format to another,
which we have endowed with the color of magic.  In typed programming,
we use a constellation of types to mediate this transformation; a
function’s result can only be passed as another function’s argument to
the extent to which those parts of the functions’ types unify.

We rely on the richness of our types in these descriptions.  So it is
natural to want the types to change as you move to different parts of
the process; each change reflects the reality of what has just
happened.  For example, when you parse a string into an AST, your
program’s state has changed types, from `String` to `MyAST`.

But, as we have just seen, due to decisions we have made to simplify
our lives,
[values cannot change types]({% post_url 2015-07-30-values-never-change-types %}),
no matter how important it is to the sanity of our code.  At the same
time, we don’t want to give up the richness of using more than one
type to describe our data.

Fortunately, there is a solution that satisfies these competing
concerns: to change types, change values.  You can’t do anything about
the values you have, but you can create new ones of the right type,
and use those instead.

Type-changing is program organization
-------------------------------------

In values with complex construction semantics, it is common to write
imperative programs that leave “holes” in the data structures using
the terrible `null` misfeature of Java, Scala, and many other
languages.  This looks something like this.

```scala
class Document(filename: Path) {
  // this structure has three parts:
  var text: String = null // ← a body of text,
  var wordIndex: Map[String, List[Int]] = null
    // ↑ an index of words to every
    // occurrence in the text,
  var mostPopular: List[(String, Int)] = null
    // ↑ the most frequently used words
    // in the text, and their number of
    // occurrences
...
```

Now, we must fill in these variables, by computing and assigning to
each in turn.  First, we compute the corpus text.

```scala
  initText()
```

Then, we compute and fill in the word index.  If we didn’t fill in
`text` first, this compiles, but crashes at runtime.

```scala
  initWordIndex()
```

Finally, we figure out which words are most popular.  If we didn’t
fill in `wordIndex` first, this compiles, but crashes.

```scala
  initMostPopular()
```

How do I know that?  Well, I have to inspect the definitions of these
three methods.

```scala
  def initText(): Unit =
    text = Source.fromFile(filename.toFile).mkString

  def initWordIndex(): Unit = {
    val words = """\w+""".r findAllMatchIn text
    wordIndex = words.foldLeft(Map[String, List[Int]]()){
      (m, mtch) =>
        val word = mtch.matched
        val idx = mtch.start
        m + (word -> (idx :: m.getOrElse(word, Nil)))
      }
  }

  def initMostPopular(): Unit =
    mostPopular = wordIndex.mapValues(_.size).toList
      .sortBy(p => 0 - p._2).take(10)
```

This method of organizing object initialization is popular because,
among other properties:

1. it *seems* self-documenting,
2. you don’t have to pass data around, and
3. steps can be customized by subclassing and overriding.

However!  It has the tremendous drawback of preventing the compiler
from helping you get the order of initialization correct.  Go, look;
see if you can spot why I said the latter two calls would crash if you
don’t get the order exactly right.  Now, I have four questions for
you.

1. Would you trust yourself to notice these implicit dependencies
   every time you look at this code?
2. Suppose you commented on the dependencies.  Would you trust these
   comments to be updated when the initialization details change?
3. Would you trust subclasses that customize the initialization to
   respect the order in which we call these three `init` functions?
4. Could you keep track of this if the initialization was
   significantly more complex?  (This *is* a toy example for a blog
   post, after all.)

Ironically, as your initialization becomes more complex, the compiler
becomes less able to help you with uninitialized-variable warnings and
the like.  But, this is not the natural order of things; it is a
consequence of using imperative variable initialization but not
representing this
[variable refinement](https://github.com/facebook/flow/releases/tag/v0.14.0)
in the type system.  By initializing in a different way, we can
recover type safety.

<div class="side-note">
  The implications of refinement, linked above, are much less severe
  than those of unrestricted type-changing of a variable.  So Flow did
  not solve, nor did it aim to solve, those difficulties by
  introducing the refinement feature.
</div>

The four types of `Document`
----------------------------

If we consider `Document` as the simple product of its three state
variables, with some special functions associated with them as
whatever `Document` methods we intend to support, we have a simple
3-tuple.

```scala
(String, Map[String, List[Int]],
 List[(String, Int)])
```

Let us no longer pretend that it is any more complicated than that.

But this cannot be mutated to fill these in as they are initialized,
you say!  Yes, that’s right, we want a *type-changing* transformation.
By *changing values*, this is easy.  There are three phases of
initialization, so four states, including uninitialized.

```scala
Path
String
(String, Map[String, List[Int]])
(String, Map[String, List[Int]], List[(String, Int)])
```

For interesting phases, such as the final one, we might create a `case
class` to hold its contents, instead.  Let us call that class, for
this example, `Doc`.

```scala
final case class Doc
  (text: String, wordIndex: Map[String, List[Int]],
   mostPopular: List[(String, Int)])
```

Finally, we can build 3 functions to take us through these steps.
Each begins by taking one as an argument, and produces the next state
as a return type.

```scala
  def initText(filename: Path): String =
    Source.fromFile(filename.toFile).mkString

  def initWordIndex(text: String): (String, Map[String, List[Int]]) = {
    val words = """\w+""".r findAllMatchIn text
    (text, words.foldLeft(Map[String, List[Int]]()){
       (m, mtch) =>
       val word = mtch.matched
       val idx = mtch.start
       m + (word -> (idx :: m.getOrElse(word, Nil)))
     })
  }

  def initMostPopular(twi: (String, Map[String, List[Int]])): Doc = {
    val (text, wordIndex) = twi
    Doc(text, wordIndex,
        wordIndex.mapValues(_.size).toList
          .sortBy(p => 0 - p._2).take(10))
  }
```

If we have a `Path`, we can get a `Doc` by `(initText _) andThen
initWordIndex andThen initMostPopular: Path => Doc`.  But that hardly
replicates the rich runtime behavior of our imperative version, does
it?  That is, we can do reordering of operations in a larger context
with `Document`, but not `Doc`.  Let us see what that means.

Many docs
---------

Dealing with one document in isolation is one thing, but suppose we
have a structure of `Document`s.

```scala
sealed abstract class DocumentTree
final case class SingleDocument(id: Int, doc: Document)
  extends DocumentTree
final case class DocumentCategory
  (name: String, members: List[DocumentTree])
  extends DocumentTree
```

In the imperative mode, we can batch and reorder initialization.  Say,
for example, we don’t initialize `Document` when we create it.  This
tree then contains `Document`s that contain only `Path`s.  We can walk
the tree, doing step 1 for every `Document`.

```scala
  // add this to DocumentTree
  def foreach(f: Document => Unit): Unit =
    this match {
      case SingleDocument(_, d) => f(d)
      case DocumentCategory(_, dts) => dts foreach (_ foreach f)
    }

// now we can initialize the text everywhere,
// given some dtree: DocumentTree
dtree foreach (_.initText())
```

The way software does, it got more complex.  And we can be ever less
sure that we’re doing things right, under this arrangement.

The four phases problem, stuck in a tree
----------------------------------------

Our tree only supports one type of document.  We could choose the
final one, `Doc`, but there is no way to replicate more exotic
document tree initializations like the one above.

Instead, we want the type of the tree to adapt along with the document
changes.  If we have four states, *Foo*, *Bar*, *Baz*, and *Quux*, we
want four different kinds of `DocumentTree` to go along with them.  In
a language with type parameters, this is easy: we can model those four
as `DocTree[Foo]`, `DocTree[Bar]`, `DocTree[Baz]`, and
`DocTree[Quux]`, respectively, by adding a type parameter.

```scala
sealed abstract class DocTree[D]
final case class SingleDoc[D](id: Int, doc: D)
  extends DocTree[D]
final case class DocCategory[D]
  (name: String, members: List[DocTree[D]])
  extends DocTree[D]
```

Now we need a replacement for the `foreach` that we used with the
unparameterized `DocumentTree` to perform each initialization step on
every `Document` therein.  Now that `DocTree` is agnostic with respect
to the specific document type, this is a little more abstract, but
quite idiomatic.

```scala
  // add this to DocTree
  def map[D2](f: D => D2): DocTree[D2] =
    this match {
      case SingleDoc(id, d) => SingleDoc(id, f(d))
      case DocCategory(c, dts) =>
        DocCategory(c, dts map (_ map f))
    }
```

It’s worth comparing these side by side.  Now we should be able to
step through initialization of `DocTree` with `map`, just as with
`DocumentTree` and `foreach`.

```scala
scala> val dtp: DocTree[Path] = DocCategory("rt", List(SingleDoc(42, Paths.get("hello.md"))))
dtp: tmtp7.DocTree[java.nio.file.Path] = DocCategory(rt,List(SingleDoc(42,hello.md)))

scala> dtp map Doc.initText
res3: tmtp7.DocTree[String] =
DocCategory(rt,List(SingleDoc(42,contents of the hello.md file!)))
```

You wouldn’t avoid writing functions, would you?
------------------------------------------------

There is nothing magical about `DocTree` that makes it especially
amenable to the introduction of a type parameter.  This is *not* a
feature whose proper use is limited to highly abstract or
general-purpose data structures; with its `String`s and `Int`s strewn
about, it is *utterly* domain-specific, “business” code.

In fact, if we were likely to annotate `Doc`s with more data, `Doc`
would be a perfect place to add a type parameter!

```scala
// suppose we add some "extra" data
final case class Doc[A]
  (text: String, wordIndex: Map[String, List[Int]],
   mostPopular: List[(String, Int)],
   extra: A)
```

You can use a type parameter to represent one simple slot in an
otherwise concretely specified structure, as above.  You can
[use one to represent 10 slots](https://bitbucket.org/ermine-language/ermine-writers/src/9ec9a98c30bc9924cc49888895f8832e8ce4f8e1/writers/html/src/main/scala/com/clarifi/reporting/writers/HTMLDeps.scala?at=default#HTMLDeps.scala-37).

Parameterized types are the type system’s version of functions.  They
aren’t just for collections, abstract code, or highly general-purpose
libraries: they’re for *your* code!

Unless you are going to suggest that *functions* are “too academic”.
Or that functions have no place in “business logic”.  Or perhaps that,
while it would be nice to define functions to solve this, that, and
sundry, you’ll just do the quick no-defining-functions hack for now
and maybe come back to add some functions later when “paying off
technical debt”.  *Then*, I’m not sure what to say.

The virtuous circle of FP and types
-----------------------------------

Now we are doing something very close to functional programming.
Moreover, we were led here not by a desire for referential
transparency, nor for purity, but merely for a way to represent the
states of our program in a more well-typed way.

In this series of posts, I have deliberately avoided discussion of
functional programming until this section; my chosen subject is types,
not functional programming.  But the features we have been considering
unavoidably coalesce here into an empirical argument for functional
programming.  Type parameters let us elegantly lift transformations
from one part of our program to another; the intractable complexities
of imperative type-changing direct us to program more functionally, by
computing new values instead of changing old ones, if we want access
to these features.  This, in turn, encourages ever more of our program
to be written in a functional style, just as the switch to different
`Doc` representations induced a switch to different document tree
representations, `map` instead of `foreach`.

Paying it Back
--------------

Likewise, the use of functional programming style feeds back, in the
aforementioned virtuous circle, to encourage the use of stronger
types.

When we wanted stronger guarantees about the initialization of our
documents, and thereby also of the larger structures incorporating
them, we turned to the most powerful tool we have at our disposal for
describing and ensuring such guarantees: the type system.  In so
doing, we induced an explosion of explicit data representations; where
we had two, we now have eight, whose connections to each other are
mediated by the types of functions involved.

With the increase in the number of explicit concepts in the code comes
a greater need for an automatic method of keeping track of all these
connections.  The type system is ideally suited to this role.

<div class="side-note">
  We induced more <em>explicit</em> data representation, not more
  representations overall.  The imperative <code>Document</code> has
  four stages of initialization, at each of which it exhibits
  different behavior.  All we have done is expose this fact to the
  type system level, at which our usage can be checked.
</div>

Don’t miss one!
---------------

As it is declared, the type-changing `DocTree#map` has another
wonderful advantage over `DocumentTree#foreach`.

Let us say that each category should also have a document of its own,
not just a list of subtrees.  In refactoring, we adjust the
definitions of `DocumentCategory` or `DocCategory`.

```scala
// imperative version
final case class DocumentCategory
  (name: String, doc: Document,
   members: List[DocumentTree])
   extends DocumentTree

// functional version
final case class DocCategory[D]
  (name: String, doc: D,
   members: DocTree[D])
  extends DocTree[D]
```

So far, so good.  Next, neither `foreach` nor `map` compile anymore.

```
TmTp7.scala:70: wrong number of arguments for pattern
⤹ tmtp7.DocumentCategory(name: String,doc: tmtp7.Document,
⤹                        members: List[tmtp7.DocumentTree])
      case DocumentCategory(_, dts) =>
                           ^
TmTp7.scala:71: not found: value d
        f(d)
          ^
TmTp7.scala:91: wrong number of arguments for pattern
⤹ tmtp7.DocCategory[D](name: String,doc: D,members: List[tmtp7.DocTree[D]])
      case DocCategory(c, dts) =>
                      ^
TmTp7.scala:92: not enough arguments for method
⤹ apply: (name: String, doc: D, members: List[tmtp7.DocTree[D]]
⤹        )tmtp7.DocCategory[D] in object DocCategory.
Unspecified value parameter members.
        DocCategory(c, dts map (_ map f))
                   ^
```

So let us fix `foreach` in the simplest way possible.

```scala
    //                 added ↓
    case DocumentCategory(_, _, dts) => ...
```

This compiles.  It is wrong, and we can figure out exactly why by
trying the same shortcut with `map`.

```scala
      case DocCategory(c, d, dts) =>
        DocCategory(c, d, dts map (_ map f))
```

We are treating the `d: D` like the `name: String`, just passing it
through.  It is “ignored” in precisely the same way as the `foreach`
ignores the new data.  But this version does not compile!

```scala
TmTp7.scala:90: type mismatch;
 found   : d.type (with underlying type D)
 required: D2
        DocCategory(c, d, dts map (_ map f))
                       ^
```

More broadly, `map` must return a `DocTree[D2]`.  By implication, the
second argument must be a `D2`, not a `D`.  We can fix it by using
`f`.

```scala
      DocCategory(c, f(d), dts map (_ map f))
```

Likewise, we should make a similar fix to `DocumentTree#foreach`.

```scala
    case DocumentCategory(_, d, dts) =>
      f(d)
      dts foreach (_ foreach f)
```

But only in the case of `map` did we get help from the compiler.
That’s because `DocumentTree` is not the only thing to gain a type
parameter in this new design.  When we made `DocTree` take one, it was
only natural to define `map` with one, too.

We can see how this works out by looking at both `foreach` and `map`
as the agents of our practical goal: transformation of the tree by
transforming the documents therein.  `foreach` works like this.

```
              document transformer
               (Document => Unit)
 DocumentTree ~~~~~~~~~~~~~~~~~~~> DocumentTree
 ------------                      -----------------
initial state                      final state
  (old tree)                       (same type, “new”
                                    but same tree)
```

The way `map` looks at `DocTree` is very similar, and we give it the
responsibilities that `foreach` had, so it is unsurprising that the
“shape” we imagine for transformation is similar.

```
               document transformer
                    (D => D2)
    DocTree[D] ~~~~~~~~~~~~~~~~~~~~> DocTree[D2]
 -------------                       ----------------
 initial state                       final state
  (old tree)                         (changed type,
                                      changed value!)
```

The replacement of `D` with `D2` also means that values of type `D`
cannot occur anywhere in the result, as `D` is abstract, so only
appears as `doc` by virtue of being the type parameter passed to
`DocTree` and its data constructors (er, “subclasses”).

As our result type is `DocTree[D2]`, we have two options, considering
only the result type:

1. return a `DocTree` with no `D2`s in its representation, one role of
   `None` and `Nil` in `Option` and `List` respectively, or
2. make `D2`s from the `D`s in the `DocTree[D]` we have in hand, by
   passing them to the ‘document transformer’ `D => D2`.

Similarly, no `DocTree[D]` values can appear anywhere in the result.
As with the `D`s, they must all be transformed or dropped, with a
different ‘empty’ `DocTree` chosen.

The dangers of “simplifying”
----------------------------

Suppose we instead defined `map` as follows.

```scala
  def map(f: D => D): DocTree[D]
```

If you subscribe to the idea of type parameters being for wonky
academics, this is “simpler”.  And it’s fine, I suppose, if you only
have one `D` in mind, one document type in mind.  Setting aside that
we have four, there is another problem.  Let’s take a look at the
“shape” of this transformation.

```
               document transformer
                     (D => D)
    DocTree[D] ~~~~~~~~~~~~~~~~~~~~> DocTree[D]
 -------------                       ----------------
 initial state                       final state
  (old tree)                         (but no promise,
                                      same type!)
```

The problem with a `D => D` transformer is that we can’t make promises
that all our data passed through it.  After all, a source `d` has the
same type as `f(d)`.  We could even get away with

```scala
  def map(f: D => D): DocTree[D] = this
```

`map[D2]` is strictly more general.  **Even if we have only one `D` in
mind for `DocTree`, it still pays to type-parameterize it and to add
‘type-changing’ extra type parameters like `D2`.**

The dangers of missing values
-----------------------------

Have you ever started getting a new bill, then missed a payment
because you thought you were covered for the month?

Have you ever gone on vacation and, in your relief at having not left
anything important at home, left something behind when packing for
your return trip?

This kind of thing cannot be characterized in the manner of “well, I
would just get a runtime error if I didn’t have a type checker, so
it’s fine.”  Yet it simply falls out of the system we have chosen;
moreover, we have barely begun to consider the possibilities.

In this series, I have focused on existential types, which we can in
one sense consider merely abstract types that the compiler checks that
we treat as independent, like `D` and `D2`.  Existential types are
only one natural outcome of the system of type abstraction brought to
us by type parameters; there are many more interesting conclusions,
like the ones described above.

Next, in “Type-changing is abstraction inversion”, we will see how
existentials turn this conceptualization of abstract types on its
head, in a different flavor of program organization.
