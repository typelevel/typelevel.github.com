---
layout: event

title: "Typelevel Summit NYC"
short_title: "NYC Summit"
date_string: "March 23, 2017"
location: "26 Bridge Street, Brooklyn"
description: "One day of recorded talks and one day of unconference, co-located with NE Scala."

poster_hero: "/img/media/nyc.jpg"
poster_thumb: "/img/media/nyc-thumb.jpg"

sponsors_section: true
location_section: true

featured: true

schedule:
  - time: "--:--"
    speakers: ["adelbertc"]
    title: "Let the Scala compiler work for you"
    summary: "Programming in some languages can feel like you’re working for the compiler - the type checker is naggy, the type system limiting, and much of your code is extraneous. This is backwards. The compiler should be working for you, helping you check your code, allowing you to express the abstractions you want, and enabling you to write clean, beautiful code. In Scala we are lucky to have such a compiler. In this talk we will explore a variety of techniques, libraries, and compiler plugins for Scala that demonstrate the utility of having a compiler that works for you."
  - time: "--:--"
    speakers: ["sofiacole"]
    title: "Adopting Scala: The Next Steps" 
    summary: "Six months into learning Scala, I summarised my experience and delivered a talk to help others going through the same process. This covered effective learning methods, an initial list of topics, and some tips so that others could be effective quickly whilst avoiding some common mistakes. Over a year later, I will reflect on those methods and their result, talk about how I extended my knowledge of functional programming, and explore how to introduce key concepts without feeling overwhelmed. My aim is to present the insights and challenges encountered when learning functional programming to make the experience as approachable as possible."
  - time: "--:--"
    speakers: ["oweinreese"]
    title: "A Tale of Two Tails: The Second Act"
    summary: "TwoTails is a compiler plugin written to add support to Scala for mutual tail recursion. While Trampolines or trampolined style recursion solve the direct need, they require explicit construction by a developer and add overhead in the form of additional data structures. Unfortunately, building a “native” solution directly into Scalac without using trampolines is not a straight forward task, even with basic tail recursion. In the latest version, a second compilation scheme has been introduced solving an issue peculiar to the JVM which the first scheme was not able to properly address. I’ll discuss both the motivation behind this new scheme and the trade-offs entailed by using it, highlighting which is more appropriate given your circumstances."
  - time: "--:--"
    speakers: ["danielasfregola"]
    title: "Easy and Efficient Data Validation with Cats"
    summary: "Often when we create a client/server application, we need to validate the requests: can the user associated to the request perform this operation? Can they access or modify the data? Is the input well-formed? When the data validation component in our application is not well designed, the code can quickly become not expressive enough and probably difficult to maintain. Business rules don’t help, adding more and more requirements to add in our validation, making it more and more complex to clearly represent and maintain. At the same time when the validation fails, it should be fairly straight forward to understand why the request was rejected, so that actions can be taken accordingly. This talk introduces Cats, a Scala library based on category theory, and some of its most interesting components for data validation. In particular, we’ll discuss some options to achieve efficient and expressive data validation. We will also argue that, compared to other options in the language, Cats is particularly suited for the task thanks to its easy-to-use data types and more approachable syntax. Throughout the talk, you will see numerous examples on how data validation can be achieved in a clean and robust way, and how we can easily integrate it in our code, without any specific knowledge of category theory."
  - time: "--:--"
    speakers: ["dscleaver"]
    title: "Finding the Free Way"
    summary: "Free Monads are quickly being adopted as the best technique for developing in a pure functional style. Unfortunately, the details for how to best apply them is often left as “an exercise for the reader.” Recently my team began using Free Monads to build Web Services within the Play Framework. We wanted to use Free Monads in an easy to follow way with minimum boilerplate, while still slotting naturally into the Play Framework. In this talk I’ll outline how we took some wrong turns, hit a few potholes, but ultimately found a way to use Free that works for us."
  - time: "--:--"
    speakers: ["longcao"]
    title: "Frameless: A More Well-Typed Interface for Spark"
    summary: "With Spark 2.0, Spark users were introduced to the Dataset API, which sought to combine the static guarantees of types (much like in RDDs) with enhancements from Spark SQL’s Catalyst optimizer, which were previously only available to more a weakly typed DataFrame API. In this introductory level talk, we’ll take a brief look at some of the rough edges encountered when working with Datasets and how Frameless, a Typelevel library attempting to add a more well-typed veneer over Spark, can help."
  - time: "--:--"
    speakers: ["ratan"]
    title: "Introduction to Recursion Schemes"
    summary: "Recursion is one of the most fundamental tools in the functional programmer’s kit. As with most fundamental tools, it’s quite powerful, and likely, too powerful for most applications. Abstracting away the explicit recursion from algorithms can make them easier to reason about, understand and maintain. Separating description of the program from interpretation, is a pattern we often see in functional programming. This talk is about applying that idea to recursive algorithms. This talk will attempt to be as self-contained as possible and will hopefully make {cata|ana|para|apo}morphisms less intimidating by showing the internals of how they could be implemented with as few parts of Scala as possible."

---

## About the Summit

The third Typelevel Summit will once again be co-located with the [Northeast Scala Symposium](http://www.nescala.org/) in New York City, with one day of recorded talks and one day of (shared) unconference.
The Summit will happen on March 23, NE Scala on March 24, and finally, the unconference on March 25.

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](http://typelevel.org/conduct.html).

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Tickets

This year, we're offering a combined ticket for both NE Scala and the Summit.
We're trying very hard to keep the ticket price low, as this is – as always – not a for-profit venture.
Participation at the shared unconference will be free of charge.

<a class="btn large" href="https://ti.to/northeast-scala-symposium/northeast-scala-symposium-2017">Buy tickets</a>

## Venue

This event will take place at [26 Bridge Event Space](http://www.26bridge.com/), 26 Bridge Street, Brooklyn, New York.
Note that the picture on Street View is outdated!
For more information about the venue and accommodation, check out the [NE Scala website](http://www.nescala.org/).

{% include venue_map.html %}

## Sponsors

If you would like to talk to us about sponsorship options, please get in touch with us:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>
