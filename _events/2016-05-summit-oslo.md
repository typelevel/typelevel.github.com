---
layout: event

title: "Typelevel Summit Oslo"
short_title: "Oslo Summit"
date_string: "May 4, 2016"
location: "Teknologihuset"
description: "One day of talks, co-located with flatMap(Oslo)."

poster_hero: "/img/media/oslo.jpg"
poster_thumb: "/img/media/oslo-thumb.jpg"

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
  - time: "9:15"
    title: How to bake "How to Bake Pi"
    speakers: ["cheng"]
    summary: "Mathematics is a very misunderstood subject.  Many people associate it only with painful experiences with childhood, or think it's all about numbers and calculations, or that it's a cold subject with clear rights and wrongs. My mission is to bring my love of mathematics to more people, and as part of this journey I need to show the beauty and the power of abstract thinking. In this talk I will present my experiences of this, starting with the book I wrote for a very general audience, and the Category Theory course I teach to art students at the School of the Art Institute of Chicago. Using a variety of surprising examples, I will show that it is possible to convince maths phobics and maths sceptics that abstract mathematics can be relevant and useful for everyone."
  - time: "10:15"
    title: "Break"
    break: true
  - time: "10:30"
    title: "A Year living Freely"
    speakers: ["cwmyers"]
    summary: "The Free monad and the Interpreter Pattern has gained significant interest in the Scala community of late.  It is a pattern that has helped unlock the problems of separating pure functions from effects.  At REA Group we have had an explosion of interest in FP and Scala in the last two years.  Beginning with just a couple of experienced functional programmers to now multiple teams and dozens of developers, we have experienced the growing pains of introducing FP and Scala to a large organisation.  The Free monad has been a key element  in our journey.  As we grew, we were particularly conscious of what patterns we could lay down, especially for beginners, that promoted the integral values of FP such as referential transparency and to allow obvious ways that software should grow.  After many experiments and much research, we discovered that the Free monad and interpreter pattern has been something that tangibly isolates effects, maintains referential transparency, subsumes dependency injection, is modular and is surprisingly accessible to FP/Scala new comers.  This talk briefly covers the mechanics of the Free monad and the interpreter pattern but largely looks at how a year with the Free monad has allowed us to make novice teams productive while they learn and embrace FP and Scala."
  - time: "11:10"
    title: "What is macro-compat and why you might be interested in using it"
    speakers: ["dwijnand"]
    summary: "Despite macros being an experimental feature of Scala, a number of libraries find them to provide great value and choose to make use of them. However in different Scala versions the macro support and API is different. That means that libraries that cross-build for multiple Scala versions have then had to deal with these differences. Macro-compat is a solution to this problem. In this talk I will introduce macro-compat, starting with an overview of the problems it's trying to solve, the prior art of how these problems are dealt with, how to use it and how it works."
  - time: "11:25"
    title: "Break"
    break: true
  - time: "11:45"
    title: "Monitoring and controlling power plants with Monix"
    speakers: ["alexandru"]
    summary: "This talk is about my experience in dealing with modeling behavior by processing asynchronous soft-real time signals from different source using Monix, the library for building asynchronous and event-based logic.  It's an experience report from my work at E.On, in monitoring and controlling power plants. We do this by gathering signals in real time and modeling state machines that give us the state in which an asset is in. The component, for lack of inspiration named Asset-Engine, is the one component in the project that definitely adheres to FP principles, the business logic being described with pure functions and data-structures and the communication being handled by actors and by Observable streams.  I want to show how I pushed side effects at the edges, in a very pragmatic setup."
  - time: "12:25"
    title: "Fetch: Simple & Efficient data access"
    speakers: ["dialelo"]
    summary: "Fetch is a Scala library for simplifying and optimizing access to data such as files systems, databases, or web services. These data sources usually have a latency cost, and we often have to trade code clarity for performance when querying them. We can easily end up with code that complects the business logic performed on the data we're fetching with explicit synchronization or optimizations such as caching and batching. Fetch can automatically request data from multiple sources concurrently, batch multiple requests to the same data source, and cache previous requests' results without having to use any explicit concurrency construct. It does so by separating data fetch declaration from interpretation, building a tree with the data dependencies where you can express concurrency with the applicative bind, and sequential dependency with monadic bind. It borrows heavily from the Haxl (Haskell, open sourced) and Stitch (Scala, not open sourced) projects. This talk will cover the problem Fetch solves, an example of how you can benefit from using it, and a high-level look at its implementation."
  - time: "12:45"
    title: "Lunch Break"
    break: true
  - time: "14:00"
    title: "Decorate your types with refined"
    speakers: ["fthomas"]
    summary: "Scala has a powerful type system that allows to create very expressive types. But sometimes we need guarantees about our values beyond what the type system can usually check, for example integers in the range from zero to fifty-nine, or chars that are either a letter or a digit. One way to realize these constraints is known as smart constructors, where the construction mechanism validates at runtime that our values satisfy the constraint. Unfortunately this technique requires some boilerplate and always incur runtime checks even if the values are kown at compile-time. This talk will introduce a library for refining types with type-level predicates, which abstracts over smart constructors. We'll go from the idea of refinement types to examples of the library using the rich set of predicates it provides, and show how it can be used at compile- and runtime alike. On that way we'll see how we can make good use of literal-based singleton types that are proposed in SIP-23. I'll also demonstrate how refined integrates with other libraries like circe, Monocle, or scodec."
  - time: "14:40"
    title: "Discovering Types (from Strings) with Cats and Shapeless"
    speakers: ["jmerritt"]
    summary: "This talk is about a simple problem which can be solved using parts of Cats and Shapeless. While helping data scientists to use the nice, well-typed Scala tools that we build for them, we are often presented with tabular data in raw text files (CSV, PSV, etc.). These files usually have some consistent, but unknown, internal schema. Data scientists are often familiar with dynamic languages like R and Python, in which fields can be parsed speculatively, or on-demand by particular operations at runtime. They usually expect Scala tools to do the same, and they particularly dislike having to specify schemas manually up-front. This mis-match can be addressed by a spectrum of different approaches, which range from handling types outside the language proper (boo! - but it works quite well in practice), to discovering and pre-generating a schema that can be used for compile-time checking. The problem of discovering the schemas of these files in a composable way makes for an interesting tour of some features of Shapeless and Cats. It's useful for beginners because the problem is quite easy to understand. I'll discuss some approaches to this, some of the remaining challenges, and provide attendees with enough background to implement the basics of a working system. I'll focus specifically on a solution that involves Cats and Shapeless for schema pre-generation, rather than macro-based approaches of manifesting schemas."
  - time: "14:55"
    title: "Break"
    break: true
  - time: "15:15"
    title: "Building functional programs with bananas, catalysts, shacl's and shapes"
    speakers: ["InTheNow"]
    summary: "This is a talk that combines both the practical, but often overlooked, topic of SBT with cutting edge distributed data technologies. The practical aspect is presented by giving an overview of catalysts, where it came from (Scalaz and banana-rdf, actually), how it evolved and how it came to be what and where it is today; and why it should be used. The evolution of catalysts then leads naturally to why current build systems play such an import role in language ecosystems and why these ecosystems can't work as they are today. This is where RDF naturally has a place, along with Shapes and Shapes Constraint Language (SHACL)."
  - time: "15:55"
    title: "Growing a DSL for financial calculations"
    speakers: ["jqno"]
    summary: "Rabobank is a Dutch multinational banking and financial services company headquartered in Utrecht, the Netherlands. One of their services is providing mortgage loans. Determining the height of the loans involves some rather complex calculations. They were struggling to represent these calculations in an understandable and reliably testable way for both domain experts and developers. We helped them develop an internal DSL in Scala that allows them to express these complex calculations in an idiomatic way that is not just easy to read for both developers and business analysts, but more testable as well. Harnessing functional programming principles and the strong Scala compiler, it also provides full typesafety with a syntax that lies very close to human language, allowing fully typesafe constructs such as 'amount per month' and 'amount per year'. In this talk, I will explain the concepts behind the DSL, how we implemented them without adding any dependencies to the project (except ScalaTest, of course), and the design decisions we had to make along the way."
  - time: "16:25"
    title: "Break"
    break: true
  - time: "16:45"
    title: "Dotty and types: the story so far"
    speakers: ["smarter"]
    summary: "Dotty is a new, experimental compiler for Scala. One of the main goal of Dotty is to provide a better type system for Scala that is both theoretically sound and better in practice. In this talk I'll focus on some of the practical improvements to the type system we've made in Dotty, like the new type parameter inference algorithm that, while not formally specified, should be easier to reason about and work in more cases. I will also try to shed some light on the challenges we face, like getting a set of features (like union types, singleton types and type inference) to interact well with each other, or properly implementing higher-kinded types."

sponsors:
  - name: "Commonwealth Bank of Australia"
    logo: "/img/media/sponsors/commbank.png"
    link: "https://www.commbank.com.au/"
    type: "platinum"
    height: 40
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
  - name: "Arktekk"
    logo: "/img/media/sponsors/arktekk.png"
    link: "http://www.arktekk.no/"
    type: "gold"
    height: 80
  - name: "Lightbend"
    logo: "/img/media/sponsors/lightbend.png"
    link: "https://www.lightbend.com/"
    type: "silver"
    height: 60
---

## About the Summit

The second Typelevel Summit will be co-located with <a href="http://2016.flatmap.no/">flatMap(Oslo)</a>.

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

<a class="btn large" href="https://www.eventbrite.co.uk/e/typelevel-summit-oslo-tickets-21637542472">Buy tickets</a>

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}

Thanks to the generous private supporters (in alphabetic order): Frank S. Thomas, Eric Torreborre, and the anonymous patrons.

## Venue

This event will take place at Teknologihuset.

{% include venue_map.html %}
