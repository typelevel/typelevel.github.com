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

schedule:
  - time: "8:30"
    title: "Registration"
    break: true
  - time: "9:00"
    title: "Opening Remarks"
    break: true
  - time: "9:05"
    speakers: ["roundcrisis"]
    title: "Keynote: Inviting everyone to the party"
    summary: "Most of today's popular general-purpose programming languages incorporate various aspects of the imperative, object and functional programming paradigms. In some cases, these languages provide clear guidelines as to what style is preferred, and why. As programmers, we have a choice to make about which paradigm(s) to use and to what extent, even if the language provides clear guidelines. How should we think about those choices? Where are the sweet spots to make trade-offs, and what do they depend on? Let's wear the hats of history and science, thinking about the past and looking to the future, examining these apparent conflicts. Paradigm change is not a new thing - perhaps we can learn something from the history books? Wear Some(hat) and party like it's a hat party. With hats."
  - time: "10:05"
    title: "Break"
    break: true
  - time: "10:30"
    speakers: ["vlovgr"]
    title: "Refined types for validated configurations"
    summary: "Are you tired of writing boilerplate code to load configurations? Have you ever had errors because of bad configuration values? Then this talk is for you! In a live-coding session we’ll see how to encode validation rules on the type-level and load validated settings without any boilerplate code.<br/><br/>In the first part of this talk we’ll look at the challenges associated with loading configurations. We’ll see how typesafe config is typically used, and see how we can eliminate most boilerplate code with Typelevel incubator project PureConfig. We’ll however see that it’s still very much possible to load invalid settings.<br/><br/>In the second part we’ll continue by exploring options to encode type invariants, for enforcing validation, looking at how we can get PureConfig to only load validated settings. We’ll ultimately end up with type-level predicates using Typelevel project refined, and see how we can get PureConfig and refined to work together seamlessly.<br/><br/>The end result is more precise types, with static validation guarantees, and a way of loading validated configurations without boilerplate – finally you can stop worrying about your configurations!"
  - time: "11:10"
    speakers: ["marina"]
    title: "Herding types with Scala macros"
    summary: "In Scala we use the term “type safety”, but what it really means? In short, most applications model data types in a form suitable for storage, change, transmission, and use. During the life cycle of the data, we expect to always use the declared type. But reality is a bit more complicated. One of the main practical problems with the use of types occurs when our application interacts with outside world – in requests to external services, different databases or simply with getting data from file. In most cases, an attempt to support type safety leads to writing a lot of code that we always try to avoid. Fortunately we have macros to do all routine job for us! In this talk we will discuss how to use compile-time reflection in library for schemaless key-value database and the benefits of use of macros in production systems."
  - time: "11:25"
    title: "Break"
    break: true
  - time: "11:45"
    speakers: ["harrylaou"]
    title: "Monad Stacks or: How I Learned to Stop Worrying and Love the Free Monad"
    summary: "In this talk, I will demonstrate various techniques, such as: Monad Transformers, Effects libraries, and Free monads. These techniques can be used to transform scala “spaghetti” code (that is embedded maps, flatmaps and pattern matching) to cleaner code that almost looks like imperative code."
  - time: "12:25"
    speakers: ["raulraja"]
    title: "Freestyle: A framework for purely functional FP Apps & Libs"
    summary: "Freestyle is a newcomer friendly library encouraging pure FP apps & libs in Scala on top of free monads. In this talk we will discuss design choices and main features including modules, algebras, interpreter composition and what is being planned for future releases."
  - time: "12:45"
    title: "Lunch Break"
    break: true
  - time: "14:00"
    speakers: ["kenbot"]
    title: "Lenses for the masses – introducing Goggles"
    summary: "Lenses, or more generally optics, are a technique that is indispensable to modern functional programming. However, implementations have veered between two extremes: incredible abstractive power with a steep learning curve; and limited domain-specific uses that can be picked up in minutes. Why can't we have our cake and eat it too?  Goggles is a new Scala macro built over the powerful & popular Monocle optics library. It uses Scala's macros and scandalously flexible syntax to create a compiler-checked mini-language to concisely construct, compose and apply optics, with a gentle, familiar interface, and extravagantly informative compiler errors.  In this talk I'll introduce the motivation for lenses and why usability is a problem that so badly needs solving, and how the Goggles library, with Monocle, helps address this in an important way.  There'll be some juicy discussion of Scala macro sorcery too!"
  - time: "14:40"
    speakers: ["data_fly"]
    title: "The power of type classes in big data ETL: a real world use case of combining Spark and Shapeless"
    summary: "In this talk, we will explore a type driven approach of big data ETL in Spark. Through code snippets, we will see how to express data processing logic with type classes and singleton types using Shapeless, and how to build a higher level DSL over Spark to make the logic easy to read from the code."
  - time: "14:55"
    title: "Break"
    break: true
  - time: "15:15"
    speakers: ["aaronmblevin"]
    title: "Mastering Typeclass Induction"
    summary: "Typeclasses are a powerful feature of the Scala. Using typeclasses to perform type-level induction is a mysterious, yet surprisingly simple, technique used in shapeless, cats, and circe to do generic programming. We will use basic data types to walk you through how this is done and why it’s useful."
  - time: "15:55"
    speakers: ["julienrf"]
    title: "Do it with (free?) arrows!"
    summary: "DSLs with a monad-based algebra (such as free monads) are becoming popular. Recently, DSLs with an applicative-based algebra (e.g. free applicatives) also aroused interest. It is not new that there exists another notion of computation that sits in between applicative functors and monads: arrows. The goal of this talk is to revisit the relationship between these notions of computation in the context of DSL algebras. Through examples of DSLs based on real world use cases, I will highlight the differences in expressive power between these three notions of computation (and some of their friends) and present the consequences for both interpreters and DSL users. At the end of the talk, you will have a better intuition of what it means that “arrows are more powerful than applicative functors but yet support more interpreters than monads”. You will get a precise understanding of “how much” expressive power you give to your users according to your DSL algebra, and, conversely, “how much” you reduce at the same time the space of the possible DSL interpreters. Finally, you will note that arrows provide an interesting trade off. Notably, they support sequencing, they can be invertible, and their computation graph can be statically analyzed."
  - time: "16:25"
    title: "Break"
    break: true
  - time: "16:45"
    speakers: ["zainabali"]
    title: "Libra: Reaching for the stars with dependent types"
    summary: "When we code, we code in numerics - doubles, floats and ints. Those numerics always represent real world quantities. Each problem domain has it’s own kinds of quantities, with its own dimensions. Adding quantities of different dimensions is nonsensical, and can have disastrous consequences.  In this talk, we’ll tackle the field of dimensional analysis. We’ll explore dependent types, singleton types, and dive into generic programming along the way. We’ll find that dimensional analysis can be brought much closer to home - in the compilation stage itself! And finally, we’ll end up deriving Libra - a library which brings dimensional analysis to the compile stage for any problem domain."
  - time: "17:30"
    break: true
    title: "Reception hosted by 47 Degrees"

sponsors:
  - name: "47 Degrees"
    logo: "/img/media/sponsors/47_degrees.png"
    link: "http://www.47deg.com/"
    type: "silver"
    height: 80
  - name: "Soundcloud"
    logo: "/img/media/sponsors/soundcloud.png"
    link: "http://www.soundcloud.com/"
    type: "silver"
    height: 50
  - name: "Signify"
    logo: "/img/media/sponsors/signify.png"
    link: "https://www.signifytechnology.com/"
    type: "silver"
    height: 80
  - name: "scalac"
    logo: "/img/media/sponsors/scalac.png"
    link: "https://scalac.io/"
    type: "silver"
    height: 50
  - name: "Underscore"
    logo: "/img/media/sponsors/underscore.png"
    link: "http://underscore.io/"
    type: "gold"
    height: 60
  - name: "Lightbend"
    logo: "/img/media/sponsors/lightbend.png"
    link: "https://www.lightbend.com/"
    type: "gold"
    height: 60

---

## About the Summit

The fourth Typelevel Summit will be co-located with **the** Scala conference: <a href="http://event.scaladays.org/scaladays-cph-2017">Scala Days</a>!

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](http://typelevel.org/conduct.html).

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}
