---
layout: post
title: Refactoring with Monads

meta:
  nav: blog
  author: mtomko
  pygments: true

tut:
  scala: 2.12.6
  binaryScala: "2.12"
  dependencies:
    - org.scala-lang:scala-library:2.12.6
    - org.typelevel::cats-core:1.2.0
---

```tut:invisible
import cats.MonadError

import scala.util.Try

case class Sql(s: String) extends AnyVal

class Row {
  def apply[A](k: String): A = ???
}

object DB {

  def unsafeQueryUnique(q: Sql): Try[Row] = ???

  def queryUnique[F[_]](q: Sql)(implicit me: MonadError[F, Throwable]): F[Row] = ???

}

implicit class SqlHelper(private val sc: StringContext) extends AnyVal {
  def sql(args: Any*): Sql = Sql(args.mkString)
}
```

I was recently cleaning up some Scala code I'd written a few months
ago when I realized I had been structuring code in a very confusing
way for a very long time. At work, we've been trying to combat the
inevitable spaghetti of code that gets written by different authors at
different times, as requirements inevitably change. We all know that
code should be made up of short, easily digestible functions but we
don't always get guidance on how to achieve that. In the presence of
error handling and nested data structures, the problem gets even
harder.

The goal of this blog post is to describe a concrete strategy for
structuring code so that the overall flow of control is clear to the
reader, even months later; and so the smaller pieces are both
digestible and testable. I'll start by giving an example function,
operating on some nested data types. Then I'll explore some ways to
break it into smaller pieces. The key insight is that we can use
computational effects in the form of
[monads](https://typelevel.org/cats/typeclasses/monad.html) (more
specifically,
[MonadError](https://typelevel.org/cats/api/cats/MonadError.html)) to
wrap smaller pieces and ultimately, compose them into an
understandable sequence of computations.

### Example domain: reading a catalog

Let's not worry about `MonadError` yet, but instead look at some
example code. Consider a situation where you need to translate data
from one domain model to another one with different restrictions, and
controlled vocabularies. This can happen in a number of places in a
program, for instance reading a database or an HTTP request to
construct a domain object.

Suppose we need to read an object from a relational database.
Unfortunately, rows in the table may represent objects of a variety of
types so we have to read the row and build up the object graph
accordingly. This is the boundary between the weakly typed wilderness
and the strongly typed world within our program.

Say our database table represents a library catalog, which might have
print books and ebooks. We'd like to look up a book by ID and get back
a nicely typed record.

Here's a simple table

| id | title                  | author         | format | download_type |
|----|------------------------|----------------|--------|---------------|
| 45 | Programming In Haskell | Hutton, Graham | print  | null          |
| 46 | Programming In Haskell | Hutton, Graham | ebook  | epub          |
| 49 | Programming In Haskell | Hutton, Graham | ebook  | pdf           |

We can define a simple domain model:

```tut:silent
sealed trait Format
case object Print extends Format
case object Digital extends Format
object Format {
  def fromString(s: String): Option[Format] = ???
}

sealed trait DownloadType
case object Epub extends DownloadType
case object Pdf extends DownloadType
object DownloadType {
  def fromString(s: String): Option[DownloadType] = ???
}

sealed trait Book extends Product with Serializable {
  def id: Int
  def title: String
  def author: String
  def format: Format
}

case class PrintBook(
    id: Int,
    title: String,
    author: String,
) extends Book {
  override val format: Format = Print
}

case class EBook(
    id: Int,
    title: String,
    author: String,
    downloadType: DownloadType
) extends Book {
  override val format: Format = Digital
}

case class ParseError(str: String) extends Exception(str)

```

We want to be able to define a method such as:

```scala
def findBookById(id: Int): Try[Book] = ???
```

### Monolithic function
One trivial definition of `findBookById` might be:

```tut:silent
import scala.util.{Failure, Success, Try}

def findBookById(id: Int): Try[Book] = {
  // unsafeQueryUnique returns a `Try[Row]`
  DB.unsafeQueryUnique(sql"""select * from catalog where id = $id""").flatMap { row =>
    // pick out the properties every book possesses
    val id = row[Int]("id")
    val title = row[String]("title")
    val author = row[String]("author")
    val formatStr = row[String]("format")

    // now start to determine the types - get the format first
    Format.fromString(formatStr) match {
      case None =>
        Failure(new ParseError(formatStr))
      case Some(Print) =>
        // for print books, we can construct the book and return immediately
        Success(PrintBook(id, title, author))
      case Some(Digital) =>
        // for digital books we need to handle the download type
        row[Option[String]]("download_type") match {
          case None =>
            Failure(new AssertionError(s"download type not provided for digital book $id"))
          case Some(downloadStr) =>
            DownloadType.fromString(downloadStr) match {
              case None     => Failure(new ParseError(downloadStr))
              case Some(dt) => Success(EBook(id, title, author, dt))
            }
        }
    }
  }
}
```

Depending on your perspective, that is arguably a long function. If
you think it is not so long, pretend that the table has a number of
other fields that must also be conditionally parsed to construct a
`Book`.

### Tail refactoring
One possible approach is a a strategy I'm going to call
"tail-refactoring", for lack of a better description. Basically, each
function does a little work or some error checking, and then calls the
next appropriate function in the chain.

You can imagine what kind of code will result. The functions are
smaller, but it's hard to describe what each function does, and
functions occasionally have to carry along additional parameters that
they will ignore except to pass deeper into the call chain. Let's take
a look at an example refactoring:

```tut:silent
import scala.util.{Failure, Success, Try}

def extractEBook(
    id: Int,
    title: String,
    author: String,
    downloadTypeStrOpt: Option[String]): Try[EBook] =
  downloadTypeStrOpt match {
    case None => Failure(new AssertionError())
    case Some(downloadTypeStr) =>
      DownloadType.fromString(downloadTypeStr) match {
        case None =>
          Failure(new ParseError(downloadTypeStr))
        case Some(dt) =>
          Success(EBook(id, title, author, dt))
      }
  }

def extractBook(
    id: Int,
    title: String,
    author: String,
    formatStr: String,
    downloadTypeStrOpt: Option[String]): Try[Book] =
  Format.fromString(formatStr) match {
    case None => Failure(new ParseError(formatStr))
    case Some(Print) =>
      Success(PrintBook(id, title, author))
    case Some(Digital) =>
      extractEBook(id, title, author, downloadTypeStrOpt)
  }

def findBookById(id: Int): Try[Book] =
  DB.unsafeQueryUnique(sql"""select * from catalog where id = $id""").flatMap { row =>
    val id = row[Int]("id")
    val title = row[String]("title")
    val author = row[String]("author")
    val formatStr = row[String]("format")
    val downloadTypeStr = row[Option[String]]("download_type")
    extractBook(id, title, author, formatStr, downloadTypeStr)
  }
```

As you can see, this form has more manageably-sized functions,
although they are still a little long. You can also see that the flow
of control is distributed through all three functions, which means
understanding the logic enough to modify or test it requires
understanding all three functions both individually and as a whole. To
follow the logic, we must trace the functions like a recursive descent
parser.

### Refactoring with Monads
Without throwing exceptions and catching them at the top, it's going
to be hard to do substantially better than the "tail-refactoring"
approach, unless we start to make use of the fact that we're working
with `Try`, a data type that supports `flatMap`. More precisely, `Try`
has a monad - recall that monads let us model computational effects
that take place in sequence.

Let's try to factor out smaller functions, each returning `Try`, and
then use a for-comprehension to specify the sequence of operations:

```tut:silent
import scala.util.{Failure, Success, Try}

def tryParse[A](s: String, parse: String => Option[A]): Try[A] = {
  parse(s)
    .map(Success(_))
    .getOrElse(Failure(new ParseError(s)))
}

def parseFormat(s: String): Try[Format] = tryParse(s, Format.fromString)

def parseDownloadType(o: Option[String], id: Int): Try[DownloadType] = {
  o.map(tryParse(_, DownloadType.fromString))
    .getOrElse(Failure(new AssertionError(s"download type not provided for digital book $id")))
}

def findBookById(id: Int): Try[Book] =
  for {
    row <- DB.unsafeQueryUnique(sql"""select * from catalog where id = $id""")
    format <- parseFormat(row[String]("format"))
    (id, title, author) = (row[Int]("id"), row[String]("title"), row[String]("author"))
    book <- format match {
      case Print =>
        Success(PrintBook(id, title, author))
      case Digital =>
        parseDownloadType(row[Option[String]]("download_type"), id)
          .map(EBook(id, title, author, _))
    }
  } yield book
```

It's less code, the functions are smaller, and the top-level function
dictates the entire flow of control. No function takes more than 2
arguments. These are testable, understandable functions. This version
really shows the power of using Monads to sequence computation.

Now we are truly making use of the fact that `Try` has a monad and not
just another container class. We can simply describe the "happy path"
and trust `Try` to short-circuit computation if something erroneous or
unexpected occurs. In that case, `Try` captures the error and stops
computation there. The code does this without the need for explicit
branching logic.

### Abstracting effect type
Now, let's take this one step further - here's where we achieve
buzzword compliance. Let's abstract away from the effect, `Try`, and
instead make use of
[MonadError](https://typelevel.org/cats/api/cats/MonadError.html).
This lets us use a more diverse set of effect types, from
[IO](https://typelevel.org/cats-effect/datatypes/io.html) to
[Task](https://monix.io/docs/3x/eval/task.html), so we can execute our
function in whatever asynchronous context we wish. This has the feel
of a tagless final strategy (although we aren't worrying about
describing interpreters here).

Here we go:

```tut:silent
import cats.MonadError
import cats.implicits._

def tryParse[F[_], A](s: String, parse: String => Option[A])(
    implicit me: MonadError[F, Throwable]): F[A] = {
  parse(s)
    .map(me.pure)
    .getOrElse(me.raiseError(new ParseError(s)))
}    

def parseFormat[F[_]](s: String)(implicit me: MonadError[F, Throwable]): F[Format] =
  tryParse[F, Format](s, Format.fromString)

def parseDownloadType[F[_]](o: Option[String], id: Int)(
    implicit me: MonadError[F, Throwable]): F[DownloadType] = {
  o.map(tryParse[F, DownloadType](_, DownloadType.fromString))
    .getOrElse(
      me.raiseError(new AssertionError(s"download type not provided for digital book $id")))
}      

def findBookById[F[_]](id: Int)(implicit me: MonadError[F, Throwable]): F[Book] =
  for {
    row <- DB.queryUnique[F](sql"""select * from catalog where id = $id""")
    format <- parseFormat[F](row[String]("format"))
    (id, title, author) = (row[Int]("id"), row[String]("title"), row[String]("author"))
    book <- format match {
      case Print =>
        me.pure(PrintBook(id, title, author))
      case Digital =>
        parseDownloadType[F](row[Option[String]]("downloadType"), id)
          .map(EBook(id, title, author, _))
    }
  } yield book
```

The code isn't much more complicated than the version using `Try` but
it adds a lot of flexibility. In a synchronous context, we could still
use `Try`. In that case, however, the database call is executed
eagerly, which means the function isn't referentially transparent. We
can make the function referentially transparent by using a monad such
as `IO` or `Task` as the effect type and delaying the evaluation of
the database call until "the end of the universe".

### Refactoring strategy

When faced with a similar refactoring problem, consider whether you
can break the problem into a sequence of independently executable
steps, each of which can be wrapped in a monad. If so, begin by
describing the control flow in your refactored function with a monadic
for-comprehension. Don't define the individual functions that comprise
the steps of the for-comprehension until you have filled in the
`yield` at the end. You can use pseudocode or stubs to minimize the
amount of code churn at the beginning. This is a great time to shuffle
steps around and work out exactly what arguments are needed and when,
as well as where they are coming from.

Once the top level function looks plausible, begin implenting the
steps of the for-comprehension. You can replace the stubs or
pseudocode you wrote by refactoring code from your original function.
If the original code did not operate in a monadic context, recall that
you can convert a simple function `A => B` to `F[A] => F[B]` using
[lift](https://typelevel.org/cats/api/cats/Monad.html#lift[A,B](f:A=%3EB):F[A]=%3EF[B])
(thanks,
[Functor](https://typelevel.org/cats/typeclasses/functor.html)!). This
makes converting your existing code even easier.

### Conclusion
In this post, we have seen how we can use monads as an aid in
refactoring code to improve both readability and testability. We have
also demonstrated that we can do this in many cases without needing to
specify the monad in use _a priori_. As a result, we gain the
flexibility to choose the appropriate monad for our application,
independently of the program logic.
