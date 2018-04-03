---
layout: event

title: "Typelevel Summit Berlin"
short_title: "Berlin Summit"
date_string: "May 18, 2018"
location: "Zalando, Zeughofstraße 1, Berlin"
description: "One day of recorded talks after Scala Days."

poster_hero: "/img/media/berlin.jpg"
poster_thumb: "/img/media/berlin-thumb.jpg"

featured: true

sponsors_section: true

sponsors:
  - name: "Zalando"
    logo: "/img/media/sponsors/zalando.png"
    link: "https://jobs.zalando.com/tech/?utm_source=typelevel&utm_medium=event-page-organic-b&utm_campaign=2018-css&utm_content=01-typelevel-summit"
    type: "platinum"
    height: 50
  - name: "Criteo"
    logo: "/img/media/sponsors/crite_o_labs.png"
    link: "https://www.criteo.com/"
    type: "gold"
    height: 100
  - name: "Commercetools"
    logo: "/img/media/sponsors/commercetools.png"
    link: "https://www.commercetools.com/"
    type: "silver"
    height: 60
  - name: "Lightbend"
    logo: "/img/media/sponsors/lightbend.png"
    link: "https://www.lightbend.com/"
    type: "silver"
    height: 50
  - name: "Signify"
    logo: "/img/media/sponsors/signify.png"
    link: "https://www.signifytechnology.com/"
    type: "silver"
    height: 80

schedule:
  - time: "TBD"
    speakers: ["keikonakata"]
    title: "Introducing namespaces into SQL result sets using nested structural types"
    summary: |
      <p>Many modern programming languages support decent namespaces. Namespaces are commonly structured hierarchies.  We bring this power to a database query language, using nested structural types.</p>

      <p>For this purpose, we hijack table aliases: given a table T containing two columns C of type String and D of type Int, a table &quot;T as S&quot; is a new table containing two columns S.C of type String and S.D of type Int. In Scala, this is neatly expressed as
      <pre>
        T : AnyRef { def C : String, def D: Int }

        T as S : AnyRef { def S: { def C: String, def D: Int } }
      </pre></p>

      <p>We implement the above as operation using the whitebox macro. We rely on Scala's type system's ability to compute Greatest Lower Bounds (GLBs) and Least Upper Bounds (LUBs) of structural types, to enable polymorphic and compositional query creation. To enable GLB and LUB computation for nested structured types, we have patched the Scala compiler.</p>
  - time: "TBD"
    speakers: ["pheymann"]
    title: "Typedapi: Define your API on the type level"
    summary: |
      <p>Have you ever thought “I really like Haskell’s Servant. Why don’t we have something like that in Scala?” or “Why can't I just define my APIs as types and Scala does the heavy lifting?”? If so, this talk is made for you.</p>

      <p>I will tell you a short story about excitement, pain and hate peaking in a climax of type-driven enlightenment. I will tell you my journey of developing Typedapi, a library for building typesafe APIs which moves as many computations to the type level as possible.</p>

      <p>We will fight many a beast on our way from Scala’s desugarisation to folds working just on types. But eventually, we will arrive at our destination, exhausted, with scars but also able to make our code a bit safer again.</p>

---

## About the Summit

The sixth Typelevel Summit will once again be co-located with **the** Scala conference: <a href="https://eu.scaladays.org/">Scala Days</a>!

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](/conduct.html).

Special thanks go to [Zalando](https://jobs.zalando.com/tech/?utm_source=typelevel&utm_medium=event-page-organic-b&utm_campaign=2018-css&utm_content=01-typelevel-summit) who kindly provide the venue.

<a class="btn large" href="https://ti.to/typelevel-summit/typelevel-summit-berlin/">Buy a ticket</a>


## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}


## Bursaries

Do you want to attend the Summit, but require funding? We award bursaries for travel, accommodation, and tickets. Everyone is welcome to apply!

If you also want to submit a talk: you can also leave us a note abound funding in the Call for Speakers form (below), and we'll contact you.

<a class="btn large" href="https://docs.google.com/forms/d/e/1FAIpQLSdhS66p9RgW1KQDsTREE8kQRkWge45WKHZXcMLruyl_2lexYg/viewform">Apply for a bursary</a>


## Co-located Event

The [Scala Center](https://scala.epfl.ch/) will organize a co-located event with roundtables of project maintainers.
Note that because the space is limited, tickets are not on sale for this event.
To register interest, please get in touch [via email](mailto:info@typelevel.org).


## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

{% include sponsors.html %}

If you would like to talk to us about sponsorship options, please get in touch:

<a class="btn large" href="mailto:info@typelevel.org">Become a sponsor</a>
