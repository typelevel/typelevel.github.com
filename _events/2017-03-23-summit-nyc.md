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

schedule:
  - time: "8:15"
    title: "Registration & Breakfast"
    break: true
  - time: "9:00"
    title: "Opening Remarks"
    break: true
  - time: "9:15"
    speakers: ["nikivazou"]
    title: "LiquidHaskell: Liquid Types for Haskell (Keynote)"
    summary: "Code deficiencies and bugs constitute an unavoidable part of software systems. In safety-critical systems, like aircrafts or medical equipment, even a single bug can lead to catastrophic impacts such as injuries or death. Formal verification can be used to statically track code deficiencies by proving or disproving correctness properties of a system.  However, at its current state formal verification is a cumbersome process that is rarely used by mainstream developers.  This talk presents LiquidHaskell, a usable formal verifier for Haskell programs. LiquidHaskell naturally integrates the specification of correctness properties in the development process. Moreover, verification is automatic, requiring no explicit proofs or complicated annotations.  At the same time, the specification language is expressive and modular, allowing the user to specify correctness properties ranging from totality and termination to memory safety and safe resource (e.g., file) manipulation.  Finally, LiquidHaskell has been used to verify more than 10,000 lines of real-world Haskell programs.  LiquidHaskell serves as a prototype verifier in a future where formal techniques will be used to facilitate, instead of hinder, software development. For instance, by automatically providing instant feedback, a verifier will allow a web security developer to immediately identify potential code vulnerabilities."
  - time: "10:15"
    title: "Break"
    break: true
  - time: "10:30"
    speakers: ["ratan"]
    title: "Introduction to Recursion Schemes"
    summary: "Recursion is one of the most fundamental tools in the functional programmer’s kit. As with most fundamental tools, it’s quite powerful, and likely, too powerful for most applications. Abstracting away the explicit recursion from algorithms can make them easier to reason about, understand and maintain. Separating description of the program from interpretation, is a pattern we often see in functional programming. This talk is about applying that idea to recursive algorithms. This talk will attempt to be as self-contained as possible and will hopefully make {cata|ana|para|apo}morphisms less intimidating by showing the internals of how they could be implemented with as few parts of Scala as possible."
  - time: "11:10"
    speakers: ["oweinreese"]
    title: "A Tale of Two Tails: The Second Act"
    summary: "TwoTails is a compiler plugin written to add support to Scala for mutual tail recursion. While Trampolines or trampolined style recursion solve the direct need, they require explicit construction by a developer and add overhead in the form of additional data structures. Unfortunately, building a “native” solution directly into Scalac without using trampolines is not a straight forward task, even with basic tail recursion. In the latest version, a second compilation scheme has been introduced solving an issue peculiar to the JVM which the first scheme was not able to properly address. I’ll discuss both the motivation behind this new scheme and the trade-offs entailed by using it, highlighting which is more appropriate given your circumstances."
  - time: "11:30"
    title: "Break"
    break: true
  - time: "11:45"
    speakers: ["dreadedsoftware"]
    title: "Scalable data pipelines with shapeless and cats"
    summary: "The data pipeline is the backbone of most modern platforms. Not only is it important to make sure your pipeline is fast and reliable but, a team also needs to be able to deploy new endpoints quickly. This talk uses inductive implicits and typeclasses to make onboarding painless. With only a limited knowledge of shapeless and cats, a developer can create scalable and maintainable data pipeline architectures that are assembled at compile time. With inductive types, pipelines can be combined to create compound pipelines simply and easily. And cats provides ready-made typeclasses which can help cut down on development time."
  - time: "12:25"
    speakers: ["longcao"]
    title: "Frameless: A More Well-Typed Interface for Spark"
    summary: "With Spark 2.0, Spark users were introduced to the Dataset API, which sought to combine the static guarantees of types (much like in RDDs) with enhancements from Spark SQL’s Catalyst optimizer, which were previously only available to more a weakly typed DataFrame API. In this introductory level talk, we’ll take a brief look at some of the rough edges encountered when working with Datasets and how Frameless, a Typelevel library attempting to add a more well-typed veneer over Spark, can help."
  - time: "12:45"
    title: "Lunch Break"
    break: true
  - time: "14:00"
    speakers: ["danielasfregola"]
    title: "Easy and Efficient Data Validation with Cats"
    summary: "Often when we create a client/server application, we need to validate the requests: can the user associated to the request perform this operation? Can they access or modify the data? Is the input well-formed? When the data validation component in our application is not well designed, the code can quickly become not expressive enough and probably difficult to maintain. Business rules don’t help, adding more and more requirements to add in our validation, making it more and more complex to clearly represent and maintain. At the same time when the validation fails, it should be fairly straight forward to understand why the request was rejected, so that actions can be taken accordingly. This talk introduces Cats, a Scala library based on category theory, and some of its most interesting components for data validation. In particular, we’ll discuss some options to achieve efficient and expressive data validation. We will also argue that, compared to other options in the language, Cats is particularly suited for the task thanks to its easy-to-use data types and more approachable syntax. Throughout the talk, you will see numerous examples on how data validation can be achieved in a clean and robust way, and how we can easily integrate it in our code, without any specific knowledge of category theory."
  - time: "14:40"
    speakers: ["dscleaver"]
    title: "Finding the Free Way"
    summary: "Free Monads are quickly being adopted as the best technique for developing in a pure functional style. Unfortunately, the details for how to best apply them is often left as “an exercise for the reader.” Recently my team began using Free Monads to build Web Services within the Play Framework. We wanted to use Free Monads in an easy to follow way with minimum boilerplate, while still slotting naturally into the Play Framework. In this talk I’ll outline how we took some wrong turns, hit a few potholes, but ultimately found a way to use Free that works for us."
  - time: "15:00"
    title: "Break"
    break: true
  - time: "15:15"
    speakers: ["igstan"]
    title: "A Type Inferencer for ML in 200 Lines of Scala"
    summary: "Scala is both acclaimed and criticized for its type inference capabilities. But most of this criticism stems from Scala’s object-functional nature, so how does type inference look like and work in functional languages without objects, such as Standard ML or Haskell? This talk aims to show one way to achieving that. We will present Wand’s type inference algorithm, a lesser known, but easier to understand and extend alternative to the classic Damas-Hindley-Milner algorithm. We’ll use a small subset of Standard ML as a vehicle language and Scala as the implementation language."
  - time: "15:55"
    speakers: ["edmundnoble"]
    title: "Extensible Effects: A Leaner Cake for Purely Functional Code"
    summary: "Purely functional algorithms and data structures are one thing, but purely functional program architectures are a completely different beast. Constructors and dependency injection frameworks compete in the object oriented landscape; in Scala, we have the Cake Pattern as well. Regardless, we aren’t doing purely functional programming just to pass around mutable objects with state, and the Cake Pattern has a similar problem with hiding effects from the user. Extensible effects provide not only a uniform interface to monadic effects, but a dependency injection mechanism that is aware of them. Finally tagless encodings provide an object-oriented view of the problem, which compared to the initial ADT encoding can be not only easier to understand for newcomers but more efficient."
  - time: "16:30"
    title: "Break"
    break: true
  - time: "16:45"
    speakers: ["adelbertc"]
    title: "Let the Scala compiler work for you"
    summary: "Programming in some languages can feel like you’re working for the compiler - the type checker is naggy, the type system limiting, and much of your code is extraneous. This is backwards. The compiler should be working for you, helping you check your code, allowing you to express the abstractions you want, and enabling you to write clean, beautiful code. In Scala we are lucky to have such a compiler. In this talk we will explore a variety of techniques, libraries, and compiler plugins for Scala that demonstrate the utility of having a compiler that works for you."
  - time: "17:25"
    speakers: ["sofiacole"]
    title: "Adopting Scala: The Next Steps" 
    summary: "Six months into learning Scala, I summarised my experience and delivered a talk to help others going through the same process. This covered effective learning methods, an initial list of topics, and some tips so that others could be effective quickly whilst avoiding some common mistakes. Over a year later, I will reflect on those methods and their result, talk about how I extended my knowledge of functional programming, and explore how to introduce key concepts without feeling overwhelmed. My aim is to present the insights and challenges encountered when learning functional programming to make the experience as approachable as possible."
  - time: "17:45"
    title: "Closing Remarks"
    break: true
  - time: "18:00"
    title: "After party at the venue hosted by Tapad"
    break: true

sponsors:
  - name: "Rally Health"
    logo: "/img/media/sponsors/rally.png"
    link: "https://www.rallyhealth.com/"
    type: "gold"
    height: 40
  - name: "Weight Watchers"
    logo: "/img/media/sponsors/weight_watchers.png"
    link: "http://www.weightwatchers.com/"
    type: "platinum"
    height: 30
  - name: "Cake Solutions"
    logo: "/img/media/sponsors/cake.jpg"
    link: "http://www.cakesolutions.net/"
    type: "platinum"
    height: 100
  - name: "Lightbend"
    logo: "/img/media/sponsors/lightbend.png"
    link: "https://www.lightbend.com/"
    type: "platinum"
    height: 60
  - name: "Tapad"
    logo: "/img/media/sponsors/tapad.png"
    link: "https://www.tapad.com/"
    type: "platinum"
    height: 70
  - name: "Underscore"
    logo: "/img/media/sponsors/underscore.png"
    link: "http://underscore.io/"
    type: "silver"
    height: 50
  - name: "iHeartRadio"
    logo: "/img/media/sponsors/iheartradio.png"
    link: "https://www.iheart.com/"
    type: "silver"
    height: 80
  - name: "Giphy"
    logo: "/img/media/sponsors/giphy.png"
    link: "https://giphy.com/"
    type: "gold"
    height: 60
  - name: "Driver"
    logo: "/img/media/sponsors/driver.png"
    link: "https://www.driver.xyz/"
    type: "gold"
    height: 60
  - name: "Comcast"
    logo: "/img/media/sponsors/comcast.png"
    link: "http://www.comcast.com/"
    type: "gold"
    height: 70
  - name: "Data Monsters"
    logo: "/img/media/sponsors/data-monsters.png"
    link: "https://datamonsters.co/"
    type: "gold"
    height: 90
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

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}

<div class="sponsors">
  <div class="sponsors__sponsor sponsors__sponsor--silver">
    <h3>After Party Sponsor</h3>
    <a href="https://www.meetup.com/">
      <img src="/img/media/sponsors/meetup.png" alt="Meetup" title="Meetup" style="height:50px" />
    </a>
  </div>
</div>
