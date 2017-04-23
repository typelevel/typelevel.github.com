---
layout: event

title: "Typelevel Summit Copenhagen"
short_title: "Copenhagen Summit"
date_string: "June 3, 2017"
location: "Comwell Conference Center Copenhagen, Denmark"
description: "One day of talks, co-located with Scala Days."

poster_hero: "/img/media/copenhagen.jpg"
poster_thumb: "/img/media/copenhagen-thumb.jpg"

sponsors_section: true
location_section: true

featured: true

schedule:
  - time: "--:--"
    speakers: ["roundcrisis"]
    title: "Keynote (TBD)"
    summary: "TBD"
  - time: "--:--"
    speakers: ["marina"]
    title: "Herding types with Scala macros"
    summary: "In Scala we use the term “type safety”, but what it really means? In short, most applications model data types in a form suitable for storage, change, transmission, and use. During the life cycle of the data, we expect to always use the declared type. But reality is a bit more complicated. One of the main practical problems with the use of types occurs when our application interacts with outside world – in requests to external services, different databases or simply with getting data from file. In most cases, an attempt to support type safety leads to writing a lot of code that we always try to avoid. Fortunately we have macros to do all routine job for us! In this talk we will discuss how to use compile-time reflection in library for schemaless key-value database and the benefits of use of macros in production systems."
  - time: "--:--"
    speakers: ["julienrf"]
    title: "Do it with (free?) arrows!"
    summary: "DSLs with a monad-based algebra (such as free monads) are becoming popular. Recently, DSLs with an applicative-based algebra (e.g. free applicatives) also aroused interest. It is not new that there exists another notion of computation that sits in between applicative functors and monads: arrows. The goal of this talk is to revisit the relationship between these notions of computation in the context of DSL algebras. Through examples of DSLs based on real world use cases, I will highlight the differences in expressive power between these three notions of computation (and some of their friends) and present the consequences for both interpreters and DSL users. At the end of the talk, you will have a better intuition of what it means that “arrows are more powerful than applicative functors but yet support more interpreters than monads”. You will get a precise understanding of “how much” expressive power you give to your users according to your DSL algebra, and, conversely, “how much” you reduce at the same time the space of the possible DSL interpreters. Finally, you will note that arrows provide an interesting trade off. Notably, they support sequencing, they can be invertible, and their computation graph can be statically analyzed."
  - time: "--:--"
    speakers: ["kenbot"]
    title: "Lenses for the masses – introducing Goggles"
    summary: "Lenses, or more generally optics, are a technique that is indispensable to modern functional programming. However, implementations have veered between two extremes: incredible abstractive power with a steep learning curve; and limited domain-specific uses that can be picked up in minutes. Why can't we have our cake and eat it too?  Goggles is a new Scala macro built over the powerful & popular Monocle optics library. It uses Scala's macros and scandalously flexible syntax to create a compiler-checked mini-language to concisely construct, compose and apply optics, with a gentle, familiar interface, and extravagantly informative compiler errors.  In this talk I'll introduce the motivation for lenses and why usability is a problem that so badly needs solving, and how the Goggles library, with Monocle, helps address this in an important way.  There'll be some juicy discussion of Scala macro sorcery too!"
  - time: "--:--"
    speakers: ["raulraja"]
    title: "Freestyle: A framework for purely functional FP Apps & Libs"
    summary: "Freestyle is a newcomer friendly library encouraging pure FP apps & libs in Scala on top of free monads. In this talk we will discuss design choices and main features including modules, algebras, interpreter composition and what is being planned for future releases."
  - time: "--:--"
    speakers: ["zainabali"]
    title: "Libra: Reaching for the stars with dependent types"
    summary: "When we code, we code in numerics - doubles, floats and ints. Those numerics always represent real world quantities. Each problem domain has it’s own kinds of quantities, with its own dimensions. Adding quantities of different dimensions is nonsensical, and can have disastrous consequences.  In this talk, we’ll tackle the field of dimensional analysis. We’ll explore dependent types, singleton types, and dive into generic programming along the way. We’ll find that dimensional analysis can be brought much closer to home - in the compilation stage itself! And finally, we’ll end up deriving Libra - a library which brings dimensional analysis to the compile stage for any problem domain."
  - time: "--:--"
    speakers: ["aaronmblevin"]
    title: "Mastering Typeclass Induction"
    summary: "Typeclasses are a powerful feature of the Scala. Using typeclasses to perform type-level induction is a mysterious, yet surprisingly simple, technique used in shapeless, cats, and circe to do generic programming. We will use basic data types to walk you through how this is done and why it’s useful."

---

## About the Summit

The fourth Typelevel Summit will be co-located with **the** Scala conference: <a href="http://event.scaladays.org/scaladays-cph-2017">Scala Days</a>!

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](http://typelevel.org/conduct.html).

## Tickets

Tickets are on sale now, starting at 60 €.

<a class="btn large" href="https://ti.to/typelevel-summit/typelevel-summit-copenhagen">Buy tickets</a>

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Venue

This event will take place at the Comwell Conference Center Copenhagen.

Arriving early for the Typelevel Summit?
<a href="http://event.scaladays.org/scaladays-cph-2017">Scala Days</a> will take place at the same location from May 30 to May 31 (trainings) and from May 31 to June 2 (conference).
Use code **typelevel** to receive <a href="https://secure.trifork.com/scaladays-cph-2017/registration/registration.jsp?promotionCode=typelevel">10% discount</a>.

{% include venue_map.html %}

## Sponsors

If you would like to talk to us about sponsorship options, please get in touch with us:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>
