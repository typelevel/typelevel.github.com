---
layout: event

title: "Typelevel Summit Philadelphia"
short_title: "Philadelphia Summit"
date_string: "March 2-3, 2016"
location: "Hub's Cira Centre"
description: "One day of recorded talks and one day of unconference, co-located with NE Scala."

poster_hero: "/img/media/philly.jpg"
poster_thumb: "/img/media/philly-thumb.jpg"

location_section: true
sponsors_section: true

featured: true

schedule:
  - time: "8:30"
    title: "Registration"
    break: true
  - time: "9:00"
    title: "Opening Remarks"
    break: true
  - time: "9:10"
    title: "Becoming a cat(s) person"
    speakers: ["adelbertc"]
    summary: "Want to contribute to Cats? Let’s head over to the Cats Issues list and do some live coding! Along the way we will see how the codebase is organized, the various bits of automation provided, and how you can use our various channels to get feedback on your work."
  - time: "9:40"
    title: "Break"
    break: true
  - time: "9:55"
    title: "End to End and On The Level"
    speakers: ["davegurnell"]
    summary: "This talk answers the burning question 'Can I build a complete web service using solely Typelevel libraries?' In Scala we are spoiled for choice for web frameworks, database layers, JSON libraries, and a thousand other essential tools for application development. So much so, it's easy to become a victim of choice paralysis when starting a new project. There's so much choice, many developers favour groups of libraries that work well together. The Typesafe Reactive Platform (colloquially the 'Typesafe Stack'), is widely known as a set of interoperable libraries providing all the functionality required to build entire web applications without looking elsewhere. Enter Typelevel, endorsing a fleet of interoperable free/open source libraries providing all manner of functionality. The phrase 'Typelevel Stack' has been used frequently in the community, raising some intersting questions: Can we build complete web services using Typelevel libraries alone? What would that look like? What will the developer experience be like in terms of tooling, support, and documentation? In this talk, Dave will discuss his adventures building a web framework completely 'on the level', capturing thoughts on design, process, documentation, support, and community along the way."
  - time: "10:35"
    title: "Probabilistic Programming: What It Is and How It Works"
    speakers: ["noelwelsh"]
    summary: "Probabilistic programming is the other Big Thing to happen in machine learning alongside deep learning. It is also closely tied to functional programming. In this talk I will explain the goals of probabilistic programming and how we can implement a probabilistic programming language in Scala. Probabilistic models are one of the main approaches in machine learning. Probabilistic programming aims to make expressive probabilistic models cheaper to develop. This is achieved by expressing the model within an embedded DSL, and then compiling learning (inference) algorithms from the model description. This automates one of the main tasks in building a probabilistic model, and provides the same benefits as a compiler for a traditional high-level language. With the close tie of functional programming to mathematics, and the use of techniques like the free monad, functional programming languages are an ideal platform for embedding probabilistic programming."
  - time: "11:05"
    title: "Break"
    break: true
  - time: "11:20"
    title: "Introducing Typelevel Scala into an OO environment"
    speakers: ["dreadedsoftware"]
    summary: "Its difficult enough trying to introduce a new language into an established environment. This problem is compounded when the new language comes with a paradigm shift. This talk will detail one process which successfully introduced Functional Scala into an Object Oriented Java shop. The talk will explain how to bridge the OO-FP impedance mismatch when communicating ideas across project boundaries. The discussion will focus on migrating from Java style mutability, loops, get/set and coupling into Typelevel style immutability, combinators, case classes and type classes."
  - time: "12:00"
    title: "Efficient compiler passes using Cats, Monocle, and Shapeless"
    speakers: ["sellout"]
    summary: "Centered around a new standalone recursion scheme library (Matryoshka), this talk shows how to take advantage of various Typelevel projects to write many conceptually-independent data transformations, but have them efficiently combined into a small number of passes. Matryoshka also uses other Typelevel projects, including kind-projector and simulacrum."
  - time: "12:30"
    title: "Lunch Break"
    break: true
  - time: "14:00"
    title: "Keynote: Dependently-Typed Haskell"
    speakers: ["sweirich"]
    summary: "Is Haskell a dependently typed programming language? The Glasgow Haskell Compiler's many type-system features, such as Generalized Algebraic Datatypes (GADTs), datatype promotion, multiparameter type classes, type families, and more recent extensions give programmers the ability to encode domain-specific invariants in their types. Clever Haskell programmers have used these features to enhance the reasoning capabilities of static type checking. But how far have we come? Could we do more?"
  - time: "15:00"
    title: "Break"
    break: true
  - time: "15:20"
    title: "Evaluation in Cats: the Good, the Bad, and the Lazy"
    speakers: ["non"]
    summary: "A unique part of Cats' design is its Eval type. This type abstracts over evaluation strategies, and is the primary way to encode laziness in Cats APIs. It also includes a trampoline to allow safe, efficient implementations of algorithms that require laziness. Eval serves as a building block for other types, such as the Streaming data type and the Foldable type class. This talk will cover the basic design of Eval. It will walk through several different examples to help explain how the evalutation strategies work, cover some common pitfalls, and show off some interesting uses of laziness. It will also try to highlight some of the shortcomings of laziness in Scala, as well as alternate approaches."
  - time: "15:40"
    title: "Easy, intuitive, direct-style syntax for Monad-comprehensions!"
    speakers: ["cvogt", "clhodapp"]
    summary: "Easy, intuitive, direct-style syntax for monad comprehensions! Like Scala async or SBT .value, but generalized to any monad. Implemented, ready to be used and requiring only vanilla Scala 2.10/2.11 and blackbox macros. Future extensions could include automatic use of Applicative where possible, support for more embedded control-flow operations, comprehensions over multiple compatible monads at once for user-defined notions of compatible and compiler tweaks for syntactic improvements."
  - time: "16:00"
    title: "Scala Exercises"
    speakers: ["raulraja"]
    summary: "Scala Exercises is a web based community tool open sourced by 47 Degrees. It contains multiple koan and free form style exercises maintained by library authors and maintainers to help you master some of the most important tools in the Scala Ecosystem. Version 2 comes with a brand new backend and exercise tracking where you can login simply using your Github account and track your progress throughout exercises and libraries. Version 2 will launch with exercises for the stdlib, Cats, Shapeless and other well known libraries and frameworks part of the Scala ecosystem."
  - time: "16:15"
    title: "Break"
    break: true
  - time: "16:30"
    title: "From Simulacrum to Typeclassic"
    speakers: ["mpilquist"]
    summary: "Simulacrum simplifies development of type class libraries. It is used in a number of open source libraries, including Cats. In this talk, we’ll tour the features of Simulacrum, and look at the forthcoming Typeclassic project, which merges Simulacrum with complementary projects like machinist and export-hook."

sponsors:
  - name: "47 Degrees"
    logo: "/img/media/sponsors/47_degrees.png"
    link: "http://www.47deg.com/"
    type: "platinum"
    height: 100
  - name: "Underscore"
    logo: "/img/media/sponsors/underscore.png"
    link: "http://underscore.io/"
    type: "gold"
    height: 50
  - name: "Verizon"
    logo: "/img/media/sponsors/verizon.png"
    link: "http://www.verizonwireless.com/"
    type: "gold"
    height: 50
  - name: "Typesafe"
    logo: "/img/media/sponsors/typesafe.png"
    link: "https://www.typesafe.com/"
    type: "silver"
    height: 60
  - name: "MediaMath"
    logo: "/img/media/sponsors/mediamath.png"
    link: "http://www.mediamath.com/"
    type: "silver"
    height: 100
  - name: "Comcast"
    logo: "/img/media/sponsors/comcast.png"
    link: "http://www.comcast.com/"
    type: "silver"
    height: 70
  - name: "Scotiabank"
    logo: "/img/media/sponsors/scotiabank.png"
    link: "http://www.scotiabank.com/"
    type: "silver"
    height: 40
---

## About the Summit

The first Typelevel Summit will be co-located with the <a href="http://www.nescala.org/">Northeast Scala Symposium</a> in Philadelphia.
There will be one day of recorded talks and one day of unconference.

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

<a class="btn large" href="https://www.eventbrite.co.uk/e/typelevel-summit-us-tickets-20778897241">Buy tickets</a>

## Speakers and Schedule

### Day 1

{% include schedule.html %}

### Day 2

_Unconference TBD_

## Venue

This event will take place at Hub's Cira Centre, next to 30th Street Station.

{% include venue_map.html %}

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}

### Supporters

If you are the higher-kinded type who would like to support the Summit as well (or instead of) attending we are offering several levels of Supporter tickets and a Donor option which will help us fund speaker and attendee bursaries.
These are available on <a href="https://www.eventbrite.co.uk/e/typelevel-summit-us-tickets-20778897241">Eventbrite</a>.

Thanks to the generous supporters (in alphabetic order):
Steve Buzzard, Jeff Clites, Ryan Delucchi, Pedro Furlanetto, Rob Norris, Erik Osheim, Michael Pilquist, SlamData, Stewart Stewart, Frank S. Thomas, and the anonymous patrons.

If you would like to talk to us about sponsorship options, please get in touch with us:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>

## Assistance and Bursaries

We are providing bursaries and assistance for speakers and attendees. You can read more about this [on our blog]({% post_url 2016-01-14-summit_assistance %}) and apply using the button below:

<a class="btn large" href="https://docs.google.com/a/underscoreconsulting.com/forms/d/1hhia7etHm_UT4WnQS7JTyGE03z-2-T1xJGujOkvacjs/viewform">Apply for bursaries / speaker assistance</a>
