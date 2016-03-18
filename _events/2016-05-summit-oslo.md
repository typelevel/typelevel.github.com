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
papers_section: true
sponsors_section: true

featured: true

schedule:
  - time: "TBD"
    title: How to bake "How to Bake Pi"
    speakers: ["cheng"]
    summary: "Mathematics is a very misunderstood subject.  Many people associate it only with painful experiences with childhood, or think it's all about numbers and calculations, or that it's a cold subject with clear rights and wrongs. My mission is to bring my love of mathematics to more people, and as part of this journey I need to show the beauty and the power of abstract thinking. In this talk I will present my experiences of this, starting with the book I wrote for a very general audience, and the Category Theory course I teach to art students at the School of the Art Institute of Chicago. Using a variety of surprising examples, I will show that it is possible to convince maths phobics and maths sceptics that abstract mathematics can be relevant and useful for everyone."
  - time: "TBD"
    title: "Decorate your types with refined"
    speakers: ["fthomas"]
    summary: "Scala has a powerful type system that allows to create very expressive types. But sometimes we need guarantees about our values beyond what the type system can usually check, for example integers in the range from zero to fifty-nine, or chars that are either a letter or a digit. One way to realize these constraints is known as smart constructors, where the construction mechanism validates at runtime that our values satisfy the constraint. Unfortunately this technique requires some boilerplate and always incur runtime checks even if the values are kown at compile-time. This talk will introduce a library for refining types with type-level predicates, which abstracts over smart constructors. We'll go from the idea of refinement types to examples of the library using the rich set of predicates it provides, and show how it can be used at compile- and runtime alike. On that way we'll see how we can make good use of literal-based singleton types that are proposed in SIP-23. I'll also demonstrate how refined integrates with other libraries like circe, Monocle, or scodec."
  - time: "TBD"
    title: "Monitoring and controlling power plants with Monix"
    speakers: ["alexandru"]
    summary: "This talk is about my experience in dealing with modeling behavior by processing asynchronous soft-real time signals from different source using Monix, the library for building asynchronous and event-based logic.  It's an experience report from my work at E.On, in monitoring and controlling power plants. We do this by gathering signals in real time and modeling state machines that give us the state in which an asset is in. The component, for lack of inspiration named Asset-Engine, is the one component in the project that definitely adheres to FP principles, the business logic being described with pure functions and data-structures and the communication being handled by actors and by Observable streams.  I want to show how I pushed side effects at the edges, in a very pragmatic setup."

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

Stay tuned while we prepare the full programme!

{% assign schedule=page.schedule %}
{% include schedule.html %}

## Call for Speakers

The Summit will feature both 30 minute and 15 minute-long talks.
We will announce a first batch of talks starting March 8th, considering proposals submitted until March 1st.
But don't worry: If you missed that date, you've still got time to submit a talk.
The final deadline for proposals is April 3rd (anywhere on Earth).

We are looking for a variety of topics; including, but not limited to:

- Experience reports:  "How we migrated our code base to shapeless"
- Typelevel project updates: "What's new in Typelevel project X"
- Your project updates: "What's new in X", where X uses Typelevel heavily
- Big picture: "Where is Scala headed for typed FP?"
- Tutorial-style: "Error-handling with cats", "Idiomatic ScalaCheck"
- Tooling: "We can now use Ensime in Notepad"
- Related work: "How language X does typed FP and what we could learn"
- Non-tech issues: "Diversity in the Scala community", "Governance in Typelevel"

Proposals should be related to the Typelevel family in some way or follow the Typelevel spirit.

<a class="btn large" href="http://goo.gl/forms/SX3plxsOKb">Submit your talk here</a>

## Assistance and Bursaries

We are providing bursaries and assistance for speakers and attendees. You can read more about this [on our blog]({% post_url 2016-01-14-summit_assistance %}) and apply using the button below:

<a class="btn large" href="https://docs.google.com/a/underscoreconsulting.com/forms/d/1hhia7etHm_UT4WnQS7JTyGE03z-2-T1xJGujOkvacjs/viewform">Apply for bursaries / speaker assistance</a>

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}

If you would like to talk to us about sponsorship options, please get in touch with us:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>

## Venue

This event will take place at Teknologihuset.

Arriving early for the Typelevel Summit?
<a href="http://2016.flatmap.no/">flatMap(Oslo)</a> will be held at the same location on May 2nd and 3rd.
CfP and ticket sales are now open.

{% include venue_map.html %}
