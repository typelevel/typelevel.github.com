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
    speakers: ["annettebieniusa"]
    title: "Keynote: Just the right kind of Consistency!"
    summary: "You need a data store that allows for high throughput and availability, while supporting consistency patterns referential integrity, numerical invariants, or atomic updates? Current designs for data storage forces application developers to decide early in the design cycle, and once and for all, what type of consistency the database should provide. At one extreme, strong consistency requires frequent global coordination; restricting concurrency in this way greatly simplifies application development, but it reduces availability and increases latency. At the opposite extreme, there are systems that provide eventual consistency only: they never sacrifice availability, but application developers must write code to deal with all sorts of concurrency anomalies in order to prevent violation of application invariants. But your system just needs to be consistent enough for the application to be correct! In the talk, I will discuss insights and techniques for analysing the consistency requirements of an application, and show techniques how you can establish them in your system."
  - time: "TBD"
    speakers: ["itrvd"]
    title: "A Fistful of Functors"
    summary: "Functors show up everywhere in our day-to-day programming. They're so common, we take them for granted - especially in typed functional programming. Beside being common, they're incredibly useful for code reuse. However, functors have several relatively unknown variants: profunctors, bifunctors, contravariant functors, and so on. And guess what - they're amazingly useful, especially combined with other abstractions in the functional programming toolkit! In this talk, we'll cover the many species of functors and see how they can help us with tasks such as serialization, stream processing, and more."
  - time: "TBD"
    speakers: ["cameronjoannidis"]
    title: "An Intuitive Guide to Combining Free Monad and Free Applicative"
    summary: "The usage of Free Monads is becoming more well understood, however the lesser known Free Applicative is still somewhat of a mystery to the average Scala developer. In this talk I will explain how you can combine the power of both these constructs in an intuitive and visual manner. You will learn the motivations for using Free Structures in the first place, how we can build up a complex domain, how we can introduce parallelism into our domain and a bunch of other practical tips for designing programs with these structures. This will also give you a deeper understanding of what libraries like Freestyle are doing under the hood and why it is so powerful."
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
    speakers: ["alexandru"]
    title: "Cancelable IO"
    summary: "Task / IO data types have been ported in Scala, inspired by Haskell's monadic IO and are surging in popularity due to the need in functional programming for referential transparency, but also because controlling side effects by means of lawful, FP abstractions makes reasoning about asynchrony and concurrency so much easier. But concurrency brings with it race conditions, i.e. the ability to execute multiple tasks at the same time, possibly interrupting the losers and cleaning up resources afterwards and thus we end up reasoning about preemption. This talk describes the design of Monix's Task for cancelability and preemption, a design that has slowly transpired in cats-effect, first bringing serious performance benefits and now a sane design for cancelation. Topics include how cancelable tasks can be described, along with examples of race conditions that people can relate to, highlighting the challenges faced when people use an IO/Task data type that cannot be interrupted."
  - time: "TBD"
    speakers: ["pheymann"]
    title: "Typedapi: Define your API on the type level"
    summary: |
      <p>Have you ever thought “I really like Haskell’s Servant. Why don’t we have something like that in Scala?” or “Why can't I just define my APIs as types and Scala does the heavy lifting?”? If so, this talk is made for you.</p>

      <p>I will tell you a short story about excitement, pain and hate peaking in a climax of type-driven enlightenment. I will tell you my journey of developing Typedapi, a library for building typesafe APIs which moves as many computations to the type level as possible.</p>

      <p>We will fight many a beast on our way from Scala’s desugaring to folds working just on types. But eventually, we will arrive at our destination, exhausted, with scars but also able to make our code a bit safer again.</p>
  - time: "TBD"
    speakers: ["InTheNow"]
    title: "Laws for Free"
    summary: "Everyone that uses a functional programming library like cats is aware of the methods that each type class adds and also the properties that the methods need to abide by. But in practice, the properties are not always proved, rather testing that the methods behave as expected. This is a problem waiting to happen, as the algebraic properties are not “optional extras” – if your semigroup's combine is not associative ... then it ain't a semigroup, sorry! So in this talk we will quickly review what we mean by a property and a law and show how to use the cats laws that are available. We'll see that they are simple to use and add literally hundreds of scalacheck tests for free. And impress your boss as well, the tests can be “seen” on the screen!"
  - time: "TBD"
    speakers: ["propensive"]
    title: "Lifting Data Structures to the Type-level"
    summary: "In this talk, I will give a fast-paced tour of how various features of the Scala type system, many of them under-explored, can be harnessed to construct type-level representations of a number of different datatypes in Scala. The type system offers a limited number of “tools”, such as subtyping, least-upper-bound inference, type unification, singleton types and dependent types and (of course) implicit search, which we can compose in interesting ways to implement type-level operations on these type-level data structures. Value-level operations follow naturally from the types, but this is much less interesting."
  - time: "TBD"
    speakers: ["stefanschneider"]
    title: "Non-academic functional Workflows"
    summary: "In this talk I want to report about how we used cats to build a domain specific language that enables us to compile workflows into later executable programs. We started with the idea of having a possibility to combine the multiple unconnected tools that are typically used to analyze an image acquired by our microscopes. The Free Monad in cats looked to us as the perfect fit to write a domain specific language that provides a lot of the advantages of an a modern functional compiler plus enforcing stack safety of the program, which would ultimately provided by third party users. We started developing with a team that had only very little experience in Scala and none with cats. Thanks to the good documentation, Scala Exercises and the straightforward mapping to functional principles, known to us from the university, we were able to get a prototype running for a trade show in 6 weeks."
  - time: "TBD"
    speakers: ["guillaumebort"]
    title: "Legacy Engineering: Making Criteo Functional"
    summary: "Criteo uses a lot of Scala in its code-base. Historically for big data stuff using the usual suspects Spark & Scalding, but more and more for application development. A few Typelevel projects started to appear in our code base as developers started to embrase more sophisticated FP practices in their Scala code. Today most of our Scala projects are built around cats, fs2, doobie, algebra, shapeless, etc. In this presentation we will discuss the challenges of introducing more functional code in a large software company as Criteo and how typelevel projects have helped. We'll talk about what's worked well as well as where the dragons lie."
  - time: "TBD"
    speakers: ["sasharomijn"]
    title: "Healthy Minds in a Healthy Community"
    summary: "Open source communities attract and boast passionate, idealistic people, and many of us invest copious amounts of time and effort to contribute to our projects and support our communities. This underlying emotional attachment can make us more vulnerable to elevated stress, burnout and conflicts. And then there are those of us who also manage mental illness. More often than not, we suffer these struggles in silence, feeling (and fearing) that we're alone in our trouble. Here, our communities can make a huge difference, by building a positive and safe environment where we can blossom and support ourselves and our peers, and feel included. This talk will take a look at open-source communities through the eyes of various mental well-being issues and struggles, and show various things that some communities already do. With this, we hope to support and inspire more communities to help foster healthy minds in a healthy environment."

---

## About the Summit

The sixth Typelevel Summit will once again happen after **the** Scala conference: <a href="https://eu.scaladays.org/">Scala Days</a>, in the same city!

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](/conduct.html).

Special thanks go to [Zalando](https://jobs.zalando.com/tech/?utm_source=typelevel&utm_medium=event-page-organic-b&utm_campaign=2018-css&utm_content=01-typelevel-summit) who kindly provide the venue.


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
