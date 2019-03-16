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
  - time: "TBD"
    speakers: ["kristinasojakova"]
    title: "Keynote: Higher Inductive Types in Homotopy Type Theory"
    summary: "Homotopy type theory is a new field of mathematics based on the recently-discovered correspondence between constructive type theory and abstract homotopy theory. Higher inductive types, which form a crucial part of this new system, generalize ordinary inductive types such as the natural numbers to higher dimensions. We will look at a few different examples of higher inductive types such as the integers, circles, and the torus, and indicate how we can use their associated induction principles to reason about them, e.g., to prove that the torus is equivalent to the product of two circles."
  - time: "TBD"
    speakers: ["stephaniebalzer"]
    title: "Keynote: Session Types"
  - time: "TBD"
    speakers: ["fabio"]
    title: "Composable concurrency with Ref + Deferred"
    summary: |
      <p>fs2 offers a very powerful and composable set of concurrent combinators and data structures, which are all built out of two deceptively simple primitives: Ref and Deferred.</p>

      <p>This talk will explain what they are, the design principles behind them, and how to use them to build your own business logic abstractions. In the process, we will discover a general pattern in the form of concurrent state machines, and see how it integrates with final tagless on one hand, and streaming control flow on the other.</p>

      <p>If you have ever wondered how to translate that complicated piece of actor logic in pure FP, or how fs2’s Queues, Topics and Signals work under the hood, this is the talk for you.</p>
  - time: "TBD"
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
  - time: "TBD"
    speakers: ["adamrosien"]
    title: "Systematic Software with Scala"
    summary: "Scala is a very flexible language, and this flexibility can make it difficult to know how to effectively design Scala code. In the nearly ten years I've been using Scala, my approach to using the language has coalesced around a ten or so strategies, which are similar to OO design patterns but broader in scope and borrow many ideas from functional programming. Using these strategies I can create code in a systematic and repeatable way. In this talk I will present the majority of my strategies, and illustrate their use by live coding a simple graphics system where the majority of the code is systematically derived by applying strategies. The strategies allow me to work at a higher-level of abstraction, and the coding itself becomes formulaic. This means I can get more work done and my code is simpler to read and use. I hope that my strategies will also enable you to design better code in Scala."
  - time: "TBD"
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
  - time: "TBD"
    speakers: ["sellout"]
    title: "The Monoiad: an epic poem on monoids"
    summary: "Monoids provide a vast landscape of concepts that we rely on in FP. Applicatives, monads, categories – all of them are monoids, as is much else. The epic takes us on a journey with this fundamental structure. We’ll move between everyday Scala, some niche areas of the language, and category theory."
  - time: "TBD"
    speakers: ["ryanwilliams"]
    title: "Portable, type-fancy multidimensional arrays"
    summary: "Zarr is a multidimensional-array container format that's gaining momentum in several scientific domains. It hails from the Python world, and primarily caters to numpy- and xarray-wielding scientists. It shines as a more remote- and parallel-processing-friendly HDF5 replacement. I implemented the Zarr spec in portable Scala, leveraging dependent- and higher-kinded-types. The resulting arrays have a unique type-safety profile. In this talk I'll: contextualize Zarr's use in the single-cell-sequencing domain, examine the freewheeling DSLs that scientific-Python exposes for array processing (including remote and distributed), discuss possibilities for Scala (and types!) to make inroads in these ecosystems, and show what worked well and poorly about my attempt."

---

## About the Summit

The seventh Typelevel Summit will once again be co-located with the <a href="https://nescala.io">Northeast Scala Symposium</a> in Philadelphia, with one day of recorded talks and one day of unconference.
The schedule for this year is as follows:

* April 1st: Typelevel Summit
* April 2nd: Northeast Scala Symposium
* April 3rd: Unconference

<a class="btn large" href="https://ti.to/northeast-scala-symposium-2019/nescala-2019">Buy tickets</a>

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Venue

The Science History Institute collects and shares the stories of innovators and of discoveries that shape our lives, preserving and interpreting the history of chemistry, chemical engineering, and the life sciences.
Headquartered in Philadelphia, with offices in California and Europe, the Institute houses an archive and a library for historians and researchers, a fellowship program for visiting scholars from around the globe, a community of researchers who examine historical and contemporary issues, an acclaimed museum that is free and open to the public, and a state-of-the-art conference center.

{% include venue_map.html %}

## Sponsors

If you would like to talk to us about sponsorship options, please get in touch:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>
