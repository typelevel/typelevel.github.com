{%
  laika.html.template: event.template.html
  date: "2016-03-02"
  event-date: "March 2-3, 2016"
  event-location: "Hub's Cira Centre"
  tags: [summits, events]
%}

# Typelevel Summit Philadelphia

![Typelevel Summit Philadelphia](/img/media/philly.jpg)

## About the Summit

The first Typelevel Summit was co-located with the <a href="http://www.nescala.org/">Northeast Scala Symposium</a> in Philadelphia, with one day of recorded talks and one day of unconference.

You can find photos from the summit [here](https://goo.gl/photos/P7DDsz68koHCXrAo8). Thanks to Brian Clapper and
Alexy Khrabrov who [also](https://drive.google.com/folderview?id=0B5w3iJKynGZJaWFUbWJOZzNETU0) [documented](http://meetup.bythebay.photo/Conferences/Typelevel/Typelevel-2016-Philadelphia/) the event.

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!
Check our <a href="/">front page</a> for upcoming events.

## Speakers and Schedule

| Time | Talk |
|------|------|
| 8:30 | Registration |
 9:00 | Opening Remarks |
 9:10 | @:style(schedule-title)Becoming a cat(s) person@:@ @:style(schedule-byline)Adelbert Chang@:@ Want to contribute to Cats? Let’s head over to the Cats Issues list and do some live coding! Along the way we will see how the codebase is organized, the various bits of automation provided, and how you can use our various channels to get feedback on your work. |
 9:40 | Break |
 9:55 | @:style(schedule-title)End to End and On The Level@:@ @:style(schedule-byline)Dave Gurnell@:@ This talk answers the burning question 'Can I build a complete web service using solely Typelevel libraries?' In Scala we are spoiled for choice for web frameworks, database layers, JSON libraries, and a thousand other essential tools for application development. So much so, it's easy to become a victim of choice paralysis when starting a new project. There's so much choice, many developers favour groups of libraries that work well together. The Typesafe Reactive Platform (colloquially the 'Typesafe Stack'), is widely known as a set of interoperable libraries providing all the functionality required to build entire web applications without looking elsewhere. Enter Typelevel, endorsing a fleet of interoperable free/open source libraries providing all manner of functionality. The phrase 'Typelevel Stack' has been used frequently in the community, raising some intersting questions: Can we build complete web services using Typelevel libraries alone? What would that look like? What will the developer experience be like in terms of tooling, support, and documentation? In this talk, Dave will discuss his adventures building a web framework completely 'on the level', capturing thoughts on design, process, documentation, support, and community along the way. |
 10:35 | @:style(schedule-title)Probabilistic Programming: What It Is and How It Works@:@ @:style(schedule-byline)Noel Welsh@:@ Probabilistic programming is the other Big Thing to happen in machine learning alongside deep learning. It is also closely tied to functional programming. In this talk I will explain the goals of probabilistic programming and how we can implement a probabilistic programming language in Scala. Probabilistic models are one of the main approaches in machine learning. Probabilistic programming aims to make expressive probabilistic models cheaper to develop. This is achieved by expressing the model within an embedded DSL, and then compiling learning (inference) algorithms from the model description. This automates one of the main tasks in building a probabilistic model, and provides the same benefits as a compiler for a traditional high-level language. With the close tie of functional programming to mathematics, and the use of techniques like the free monad, functional programming languages are an ideal platform for embedding probabilistic programming. |
 11:05 | Break |
 11:20 | @:style(schedule-title)Introducing Typelevel Scala into an OO environment@:@ @:style(schedule-byline)Marcus Henry, Jr.@:@ Its difficult enough trying to introduce a new language into an established environment. This problem is compounded when the new language comes with a paradigm shift. This talk will detail one process which successfully introduced Functional Scala into an Object Oriented Java shop. The talk will explain how to bridge the OO-FP impedance mismatch when communicating ideas across project boundaries. The discussion will focus on migrating from Java style mutability, loops, get/set and coupling into Typelevel style immutability, combinators, case classes and type classes. |
 12:00 | @:style(schedule-title)Efficient compiler passes using Cats, Monocle, and Shapeless@:@ @:style(schedule-byline)Greg Pfeil@:@ Centered around a new standalone recursion scheme library (Matryoshka), this talk shows how to take advantage of various Typelevel projects to write many conceptually-independent data transformations, but have them efficiently combined into a small number of passes. Matryoshka also uses other Typelevel projects, including kind-projector and simulacrum. |
 12:30 | Lunch Break |
 14:00 | @:style(schedule-title)Keynote: Dependently-Typed Haskell@:@ @:style(schedule-byline)Stephanie Weirich@:@ Is Haskell a dependently typed programming language? The Glasgow Haskell Compiler's many type-system features, such as Generalized Algebraic Datatypes (GADTs), datatype promotion, multiparameter type classes, type families, and more recent extensions give programmers the ability to encode domain-specific invariants in their types. Clever Haskell programmers have used these features to enhance the reasoning capabilities of static type checking. But how far have we come? Could we do more? |
 15:00 | Break |
 15:20 | @:style(schedule-title)Evaluation in Cats: the Good, the Bad, and the Lazy@:@ @:style(schedule-byline)Erik Osheim@:@ A unique part of Cats' design is its Eval type. This type abstracts over evaluation strategies, and is the primary way to encode laziness in Cats APIs. It also includes a trampoline to allow safe, efficient implementations of algorithms that require laziness. Eval serves as a building block for other types, such as the Streaming data type and the Foldable type class. This talk will cover the basic design of Eval. It will walk through several different examples to help explain how the evalutation strategies work, cover some common pitfalls, and show off some interesting uses of laziness. It will also try to highlight some of the shortcomings of laziness in Scala, as well as alternate approaches. |
 15:40 | @:style(schedule-title)Easy, intuitive, direct-style syntax for Monad-comprehensions!@:@ @:style(schedule-byline)Chris Vogt, Chris Hodapp@:@ Easy, intuitive, direct-style syntax for monad comprehensions! Like Scala async or SBT .value, but generalized to any monad. Implemented, ready to be used and requiring only vanilla Scala 2.10/2.11 and blackbox macros. Future extensions could include automatic use of Applicative where possible, support for more embedded control-flow operations, comprehensions over multiple compatible monads at once for user-defined notions of compatible and compiler tweaks for syntactic improvements. |
 16:00 | @:style(schedule-title)Scala Exercises@:@ @:style(schedule-byline)Raúl Raja Martínez@:@ Scala Exercises is a web based community tool open sourced by 47 Degrees. It contains multiple koan and free form style exercises maintained by library authors and maintainers to help you master some of the most important tools in the Scala Ecosystem. Version 2 comes with a brand new backend and exercise tracking where you can login simply using your Github account and track your progress throughout exercises and libraries. Version 2 will launch with exercises for the stdlib, Cats, Shapeless and other well known libraries and frameworks part of the Scala ecosystem. |
 16:15 | Break |
 16:30 | @:style(schedule-title)From Simulacrum to Typeclassic@:@ @:style(schedule-byline)mpilquist@:@ Simulacrum simplifies development of type class libraries. It is used in a number of open source libraries, including Cats. In this talk, we’ll tour the features of Simulacrum, and look at the forthcoming Typeclassic project, which merges Simulacrum with complementary projects like machinist and export-hook. |

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

### Platinum
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/47_degrees.png) { alt: 47 Degrees, title: 47 Degrees, style: legacy-event-sponsor }](http://www.47deg.com/)@:@
@:@

### Gold
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/underscore.png) { alt: Underscore, title: Underscore, style: legacy-event-sponsor }](http://underscore.io/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/verizon.png) { alt: Verizon, title: Verizon, style: legacy-event-sponsor }](http://www.verizonwireless.com/)@:@
@:@

### Silver
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/lightbend.png) { alt: Lightbend, title: Lightbend, style: legacy-event-sponsor }](https://www.lightbend.com/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/mediamath.png) { alt: MediaMath, title: MediaMath, style: legacy-event-sponsor }](http://www.mediamath.com/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/comcast.png) { alt: Comcast, title: Comcast, style: legacy-event-sponsor }](http://www.comcast.com/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/box.png) { alt: Box, title: Box, style: legacy-event-sponsor }](http://www.box.com/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/scotiabank.png) { alt: Scotiabank, title: Scotiabank, style: legacy-event-sponsor }](http://www.scotiabank.com/)@:@
@:@

Thanks to the generous private supporters (in alphabetic order):
Steve Buzzard, Jeff Clites, Ryan Delucchi, Pedro Furlanetto, Rob Norris, Erik Osheim, Michael Pilquist, SlamData, Stewart Stewart, Frank S. Thomas, and the anonymous patrons.
