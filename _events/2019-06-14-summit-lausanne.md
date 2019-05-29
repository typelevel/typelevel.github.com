---
layout: event

title: "Typelevel Summit Lausanne"
short_title: "Lausanne Summit"
date_string: "June 14, 2019"
location: "École Polytechnique Fédérale de Lausanne"
description: "One day of talks, co-located with the anniversary Scala Days."

poster_hero: "/img/media/lausanne.jpg"
poster_thumb: "/img/media/lausanne-thumb.jpg"

featured: true

sponsors_section: true

schedule:
  - time: "8:15"
    title: "Registration"
    break: true
  - time: "9:00"
    title: "Opening Remarks"
    break: true
  - time: "9:05"
    title: "Keynote"
    speakers: ["martinodersky"]
    summary: TBD
  - time: "TBD"
    speakers: ["denisrosset"]
    title: "Lord of the rings: the Spire numerical towers"
    summary: |
      <p>Spire defines around 80 typeclasses, including 30 coming from algebra and cats-kernel. We’ll see how much of that structure is dictated by mathematical laws, and which parts are the result of design decisions that balance different tradeoffs. In particular, we’ll discuss the different roles played by typeclasses in the Scala ecosystem:
      <ul>
        <li>as encoding operations obeying well-defined laws,</li>
        <li>as enabling the use of a particular syntax for those operations, if possible close to the mathematical notation of a domain (and subfields often disagree on the notation!),</li>
        <li>defining a context in which a combination of typeclasses implicitly imposes additional laws (for example, the ordering of numbers and addition),</li>
        <li>enabling the user to change the variant of a relation being used (Order),</li>
        <li>singling out one variant of a structure as canonical (cats: the additive Group for integers),</li>
        <li>as selecting a particular algorithm for an operation (integer factorization: deterministic or Monte-Carlo).</li>
      </ul></p>
      <p>It quickly becomes apparent that these roles conflict. With this in mind, we’ll have a look at some design choices made in Spire. We’ll discuss success stories, such as the clarification of the laws of the % operator, the commutative ring tower that formalizes integer factorization and Euclidean division. We’ll also discuss parts where trade offs have been made, such as the triplication of group structures (Group, AdditiveGroup, MultiplicativeGroup), the problem of coherent instances, especially when various typeclasses are combined. Time permitting, we’ll also discuss issues with law-based property checks (precision, range, time and memory complexity).</p>
  - time: "TBD"
    speakers: ["buggymcbugfix"]
    title: "Taking Resources to the Type Level"
    summary: "With the Granule project, we are working towards making statically typed functional languages more resource-aware, hence providing a way to enforce stateful protocols regarding memory, file handles, network interaction, etc. Static enforcement of security policies and first-class support for multi-stage programming are further examples of what is possible in a type system based on Linear Logic and Graded Modalities. We present Granule, a functional programming language which combines parametric polymorphism and indexed types with such a type system. Granule programs will probably look very familiar to you, especially if you know some Haskell/ML, but in Granule’s type system we can reason about much more. Hillel Wayne’s Great Theorem Prover Showdown has made a point of the fact that there are many things we can’t easily reason about with functional (programming | proving)—up until now! We will implement leftPad in Granule and prove it correct with little more effort that writing the type signatures. We will then breeze through how Granule’s type system very naturally supports session-typed channels and safe mutable arrays."
  - time: "TBD"
    speakers: ["oronport"]
    title: "TwoFace values: a bridge between terms and types"
    summary: "Scala 2.13 introduces literal types, and with great types comes great thirst for power to control them. In this talk we get acquainted with the singleton-ops library, a typelevel programming library that enables constraining and performing operations on literal types. We learn about the library’s TwoFace value feature, and how it can be used to bridge the gap between types and terms by converting a type expression to term expression and vice-versa."
  - time: "TBD"
    speakers: ["romac"]
    title: "Formal verification of Scala programs with Stainless"
    summary: "Everyone knows that writing bug-free code is fundamentally difficult, and that bugs will sometimes sneak in even in the presence of unit- or property-based tests. One solution to this problem is formal software verification. Formal verification allows users to statically verify that software systems will never crash nor diverge, and will in addition satisfy given functional correctness properties. In this talk, I will present Stainless, a verification system for an expressive subset of Scala. I will start by explaining what formal verification is, what are some of the challenges people encounter when putting it into practice, and how it can be made more practical. Then I will give a high-level overview of Stainless, and finally present a few verified programs, such as a small actor system, a parallel map-reduce implementation, as well as a little surprise! I’ll also demonstrate the tooling we have developed around Stainless which lets users easily integrate Stainless in their SBT-based Scala projects."
  - time: "TBD"
    speakers: ["diesalbla"]
    title: "Actors Design Patterns and Arrowised FRP"
    summary: "Object-oriented design patterns combine basic language features to solve coding problems in an extensible way. In functional Scala, we solve those coding problems with functions, combinators, and type-classes, so design patterns are less relevant. Actor design patterns combine basic features of the actors to solve coding problems in an extensible way. Arrowised functional reactive programming (based on languages like Scala and Haskell also offers a way to solve those coding problem using functions, combinators, and type-classes. This talk describes a prototype implementation of AFRP and its primitive types and functions, discusses its similarities to actors, and then describes how some actor design patterns in the existing literature corresponds to constructions of AFRP."
  - time: "TBD"
    speakers: ["jefersonossa"]
    title: "Exploring Scala Tooling ecosystem"
    summary: "We are going to explore and compare some build tools with special focus on LSP/BSP implementations, IDEs and text editor support. To help the audience’s judgement about the tools that are suitable for their particular needs this talk aims to get attendees familiar with terms like SemanticDB, Metals, Bloop, SBT, Pants, Bazel, Ensime, IntelliJ IDE, Scala IDE, Dotty IDE and other honorific mentions."
  - time: "TBD"
    speakers: ["yifanxing"]
    title: "Want to Diversify the Scala Community? Here is How You Can Help!"
    summary: "The Scala community has grown significantly over the past 15 years. As a community, we wrote millions of lines of code and developed hundreds of projects. While the language is thriving, there is still room to contribute to the community. Different from other tech talks, this talk focuses on contributing to the diversity aspect of the community. It explains the significance and benefits of diversity, and it proposes solutions to diversify and improve the community. One of the best ways to grow the community and to bring diversity into the community is to organize ScalaBridge workshops, which are intended to provide resources for people from underrepresented populations to learn Scala. (Diversity comes in many forms: race, gender, age, religion, culture, sexual orientation, socioeconomic background, etc.) While the workshops have positive and lasting impacts, it cannot be done by one individual or by a single organization. In order for the Scala community to become more diverse, we need your help to scale up! Attend this talk to learn about how to contribute to our community!"
  - time: "TBD"
    speakers: ["aleksander"]
    title: "GADTs in Dotty"
    summary: "GADTs (Generalized Algebraic Data Types) are a special case of ADTs (or Dotty enums) that, when we match on them, let us know more about type parameters to enclosing functions. In practice, they are mostly used to associate types with data constructors (case classes and objects in Scala’s case), and to ensure that incorrectly assembling data structures will not typecheck. Two good examples are a database query type that cannot be malformed (no integers as if conditions!) or a red-black tree data type that will only compile if it is balanced. So far Scala’s support for GADTs has been lacking and rife with runtime type errors compared to Haskell. Fortunately, I’ve been working on making it far better in Dotty! During the talk first we’ll walk through examples of GADTs, see what makes them useful and how they can be applied to solve real problems. Next, I’ll explain how GADTs in Scala naturally follow from subtyping and inheritance, completely unlike Haskell or any other language with GADTs. Finally, I’ll talk about how the support for GADTs in Dotty is tightly related to other features such as match types and (the possible) nullable types."
  - time: "TBD"
    speakers: ["felixmulder"]
    title: "Brave New World - tales of PureScript and Haskell in production"
    summary: "The rumours are true. Writing code in purely functional languages tends to produce code that is much easier to read, modify and reason about. This talk examines how an experienced Scala team transitioned into writing production code using PureScript in AWS lambda, and services using Haskell."

sponsors:
  - name: "Triplequote"
    logo: "/img/media/sponsors/triplequote.png"
    link: "https://www.triplequote.com/"
    type: "gold"
    height: 60

---

## About the Summit

The eight Typelevel Summit will once again be co-located with Scala Days.
Read more about all events in the <a href="https://www.scala-lang.org/blog/2019/01/17/scala-days-2019-celebrating-collaborative-success.html">blog post</a> from the Scala Center.

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

<a class="btn large" href="https://ti.to/typelevel-summit/typelevel-summit-lausanne-2019">Buy tickets</a>

## Speakers and Schedule

Stay tuned while we announce the full programme!

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Sponsors

If you would like to talk to us about sponsorship options, please get in touch:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>

{% include sponsors.html %}
