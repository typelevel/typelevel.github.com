---
layout: event

title: "Typelevel Summit Philadelphia"
short_title: "Philadelphia Summit"
date_string: "April 1, 2019"
location: "Science History Institute, Philadelphia"
description: "One day of recorded talks and one day of unconference, co-located with NE Scala."

poster_hero: "/img/media/philly.jpg"
poster_thumb: "/img/media/philly-thumb.jpg"

featured: true

sponsors_section: true

schedule:
  - time: "8:15"
    break: yes
    title: "Registration & Breakfast sponsored by Coatue"
  - time: "8:55"
    break: yes
    title: "Opening Remarks"
  - time: "9:00"
    speakers: ["stephaniebalzer"]
    title: "Keynote: Shared Session Types for Safe, Practical Concurrency"
    summary: |
      <p>Message-passing concurrency abstracts over the details of how programs are compiled to machine
      instructions and has been adopted by various practical languages, such as Erlang, Go, and Rust.
      For example, Mozilla's Servo, a next-generation browser engine being written in Rust, exploits
      message-passing concurrency to parallelize loading and rendering of webpage elements, done
      sequentially in existing web browsers.  Messages are exchanged along channels, which are typed
      with enumeration types.  Whereas typing ensures in this setting that only messages of the
      appropriate type are communicated along channels, it fails to guarantee adherence to the
      intended protocol of message exchange.</p>
      <p>In this talk I show how session types can be used to type communication channels to check
      protocol adherence at compile-time.  Session types were conceived in the context of process
      calculi, but made their ways into various practical languages using libraries.  A key
      restriction of prior session type work is linearity.  Whereas linear session types enjoy strong
      properties such as race freedom, protocol adherence, and deadlock-freedom, their insistance on
      a single client rules out common programing scenarios, such as multi-producer-consumer queues
      or shared databases or output devices.  I report on my work on shared session types, which
      accommodates those programing scenarios, while upholding the guarantees of linear session
      types.  First, I introduce manifest sharing, a discipline in which linear and shared sessions
      coexist, but the type system ensures that clients of shared sessions run in mutual exclusion
      from each other.  Manifest sharing guarantees race freedom and protocol adherence, but permits
      deadlocks.  Next, I introduce manifest deadlock freedom, which makes shared and linear sessions
      deadlock-free by construction.  Finally, I give an overview of my current and future research
      plans.</p>
  - time: "9:55"
    break: yes
    title: "Break"
  - time: "10:10"
    speakers: ["adamrosien"]
    title: "Systematic Software with Scala"
    summary: "Scala is a very flexible language, and this flexibility can make it difficult to know how to effectively design Scala code. In the nearly ten years I've been using Scala, my approach to using the language has coalesced around a ten or so strategies, which are similar to OO design patterns but broader in scope and borrow many ideas from functional programming. Using these strategies I can create code in a systematic and repeatable way. In this talk I will present the majority of my strategies, and illustrate their use by live coding a simple graphics system where the majority of the code is systematically derived by applying strategies. The strategies allow me to work at a higher-level of abstraction, and the coding itself becomes formulaic. This means I can get more work done and my code is simpler to read and use. I hope that my strategies will also enable you to design better code in Scala."
  - time: "10:45"
    speakers: ["justin"]
    title: "Journey to an FP Test Harness"
    summary: |
      <p>The hardest part of the pure-FP journey for many people is taking that first real step. Even after you’ve read all the books and done all the exercises, you need to start committing real code to truly grok the FP mindset.</p>

      <p>This little case study will trace my journey over that line, in building a new test harness to an existing Play application. In the course of it, we’ll explore how my assumptions evolved:
      <ul>
        <li>From stateful members to consistent use of StateT;</li>
        <li>From Play’s native Future-centricity to IO;</li>
        <li>Becoming a little more nuanced about test state using IndexedStateT;</li>
        <li>Moving away from an ever-growing cake to focus on imports instead;</li>
        <li>And the payoff, being able to refactor the test code to be modular, readable and robust.</li>
      </ul></p>

      <p>The goal here is to show that, while there are a bunch of parts, none of this is rocket science. In the end, the resulting code is delightfully elegant, and the general approach should work for many Play applications.</p>
  - time: "11:20"
    break: yes
    title: "Break"
  - time: "11:35"
    speakers: ["sellout"]
    title: "The Monoiad: an epic poem on monoids"
    summary: "Monoids provide a vast landscape of concepts that we rely on in FP. Applicatives, monads, categories – all of them are monoids, as is much else. The epic takes us on a journey with this fundamental structure. We’ll move between everyday Scala, some niche areas of the language, and category theory."
  - time: "12:10"
    break: yes
    title: "Lunch sponsored by Simple"
  - time: "13:45"
    speakers: ["kristinasojakova"]
    title: "Keynote: Higher Inductive Types in Homotopy Type Theory"
    summary: "Homotopy type theory is a new field of mathematics based on the recently-discovered correspondence between constructive type theory and abstract homotopy theory. Higher inductive types, which form a crucial part of this new system, generalize ordinary inductive types such as the natural numbers to higher dimensions. We will look at a few different examples of higher inductive types such as the integers, circles, and the torus, and indicate how we can use their associated induction principles to reason about them, e.g., to prove that the torus is equivalent to the product of two circles."
  - time: "14:40"
    break: yes
    title: "Break"
  - time: "14:55"
    speakers: ["davenpcm"]
    title: "Telling the Truth with Types"
    summary: |
      <p>There are many problems one faces when building effective solutions.

      <ol>
        <li>Outlining proper behavior, such that desired outcomes are achieved.</li>
        <li>Simplifying the problem space, such that solutions are extensible and maintainable.</li>
        <li>Interfacing with existing code.</li>
      </ol></p>

      <p>Together we will walk through typical problems, and apply a set of processes to more effectively meet these criteria. We will identify what information we need to make available and how we can consume that information to build out systems which behave as we expect. We will use the type system as our guide, to lift our reasoning directly into our codebases.</p>

      <p>Whether you are just starting out, or an experienced functional programmer this talk will deliver a set of tools to approach the next set of challenges.</p>
  - time: "15:30"
    speakers: ["fabio"]
    title: "Composable concurrency with Ref + Deferred"
    summary: |
      <p>fs2 offers a very powerful and composable set of concurrent combinators and data structures, which are all built out of two deceptively simple primitives: Ref and Deferred.</p>

      <p>This talk will explain what they are, the design principles behind them, and how to use them to build your own business logic abstractions. In the process, we will discover a general pattern in the form of concurrent state machines, and see how it integrates with final tagless on one hand, and streaming control flow on the other.</p>

      <p>If you have ever wondered how to translate that complicated piece of actor logic in pure FP, or how fs2’s Queues, Topics and Signals work under the hood, this is the talk for you.</p>
  - time: "16:05"
    break: yes
    title: "Break"
  - time: "16:20"
    speakers: ["rossabaker"]
    title: "Extending your HTTP library with monad transformers"
    summary: "A tour of monad transformers and how stacking various effects onto IO can extend our HTTP library in new and interesting ways. We’ll review OptionT from last year’s talk, derive something akka-http like with EitherT, and demonstrating tracing with TraceT."
  - time: "16:55"
    speakers: ["ryanwilliams"]
    title: "Portable, type-fancy multidimensional arrays"
    summary: "Zarr is a multidimensional-array container format that's gaining momentum in several scientific domains. It hails from the Python world, and primarily caters to numpy- and xarray-wielding scientists. It shines as a more remote- and parallel-processing-friendly HDF5 replacement. I implemented the Zarr spec in portable Scala, leveraging dependent- and higher-kinded-types. The resulting arrays have a unique type-safety profile. In this talk I'll: contextualize Zarr's use in the single-cell-sequencing domain, examine the freewheeling DSLs that scientific-Python exposes for array processing (including remote and distributed), discuss possibilities for Scala (and types!) to make inroads in these ecosystems, and show what worked well and poorly about my attempt."
  - time: "17:30"
    break: yes
    title: "Closing"

sponsors:
  - name: "Comcast"
    logo: "/img/media/sponsors/comcast.png"
    link: "http://www.comcast.com/"
    type: "gold"
    height: 70
  - name: "Azavea"
    logo: "/img/media/sponsors/azavea.png"
    link: "http://www.azavea.com/"
    type: "gold"
    height: 70
  - name: "Chariot Solutions"
    logo: "/img/media/sponsors/chariot.png"
    link: "http://www.chariotsolutions.com/"
    type: "gold"
    height: 70
  - name: "Simple"
    logo: "/img/media/sponsors/simple.png"
    link: "http://www.simple.com/"
    type: "gold"
    height: 70
  - name: "Coatue"
    logo: "/img/media/sponsors/coatue.png"
    link: "http://www.coatue.com/"
    type: "gold"
    height: 70

---

## About the Summit

The seventh Typelevel Summit will once again be co-located with the <a href="https://nescala.io">Northeast Scala Symposium</a> in Philadelphia, with one day of recorded talks and one day of unconference.
The schedule for this year is as follows:

* April 1st: Typelevel Summit
* April 2nd: Northeast Scala Symposium
* April 3rd: Unconference

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Venue

The Science History Institute collects and shares the stories of innovators and of discoveries that shape our lives, preserving and interpreting the history of chemistry, chemical engineering, and the life sciences.
Headquartered in Philadelphia, with offices in California and Europe, the Institute houses an archive and a library for historians and researchers, a fellowship program for visiting scholars from around the globe, a community of researchers who examine historical and contemporary issues, an acclaimed museum that is free and open to the public, and a state-of-the-art conference center.

{% include venue_map.html %}

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}
