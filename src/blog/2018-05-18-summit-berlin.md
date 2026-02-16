{%
  laika.html.template: event.template.html
  date: "2018-05-18"
  event-date: "May 18, 2018"
  event-location: "Zalando, Zeughofstraße 1, Berlin"
  tags: [summits, events]
%}

# Typelevel Summit Berlin

![Typelevel Summit Berlin](/img/media/berlin.jpg)

## About the Summit

The sixth Typelevel Summit will once again happen after **the** Scala conference: [Scala Days](https://eu.scaladays.org/), in the same city!

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](/code-of-conduct/README.md).

Special thanks go to [Zalando](https://jobs.zalando.com/tech/?utm_source=typelevel&utm_medium=event-page-organic-b&utm_campaign=2018-css&utm_content=01-typelevel-summit) who kindly provide the venue.


## Speakers and Schedule

| Time | Talk |
|------|------|
| 8:15 | Registration |
 9:00 | Opening Remarks |
 9:05 | @:style(schedule-title)Keynote: Just the right kind of Consistency!@:@ @:style(schedule-byline)Annette Bieniusa@:@ You need a data store that allows for high throughput and availability, while supporting consistency patterns referential integrity, numerical invariants, or atomic updates? Current designs for data storage forces application developers to decide early in the design cycle, and once and for all, what type of consistency the database should provide. At one extreme, strong consistency requires frequent global coordination; restricting concurrency in this way greatly simplifies application development, but it reduces availability and increases latency. At the opposite extreme, there are systems that provide eventual consistency only: they never sacrifice availability, but application developers must write code to deal with all sorts of concurrency anomalies in order to prevent violation of application invariants. But your system just needs to be consistent enough for the application to be correct! In the talk, I will discuss insights and techniques for analysing the consistency requirements of an application, and show techniques how you can establish them in your system. |
 10:00 | Break |
 10:20 | @:style(schedule-title)A Fistful of Functors@:@ @:style(schedule-byline)Itamar Ravid@:@ Functors show up everywhere in our day-to-day programming. They're so common, we take them for granted - especially in typed functional programming. Beside being common, they're incredibly useful for code reuse. However, functors have several relatively unknown variants: profunctors, bifunctors, contravariant functors, and so on. And guess what - they're amazingly useful, especially combined with other abstractions in the functional programming toolkit! In this talk, we'll cover the many species of functors and see how they can help us with tasks such as serialization, stream processing, and more. |
 10:55 | @:style(schedule-title)Cancelable IO@:@ @:style(schedule-byline)Alexandru Nedelcu@:@ Task / IO data types have been ported in Scala, inspired by Haskell's monadic IO and are surging in popularity due to the need in functional programming for referential transparency, but also because controlling side effects by means of lawful, FP abstractions makes reasoning about asynchrony and concurrency so much easier. But concurrency brings with it race conditions, i.e. the ability to execute multiple tasks at the same time, possibly interrupting the losers and cleaning up resources afterwards and thus we end up reasoning about preemption. This talk describes the design of Monix's Task for cancelability and preemption, a design that has slowly transpired in cats-effect, first bringing serious performance benefits and now a sane design for cancelation. Topics include how cancelable tasks can be described, along with examples of race conditions that people can relate to, highlighting the challenges faced when people use an IO/Task data type that cannot be interrupted. |
 11:30 | Break |
 11:50 | @:style(schedule-title)Legacy Engineering: Making Criteo Functional@:@ @:style(schedule-byline)Guillaume Bort@:@ Criteo uses a lot of Scala in its code-base. Historically for big data stuff using the usual suspects Spark & Scalding, but more and more for application development. A few Typelevel projects started to appear in our code base as developers started to embrase more sophisticated FP practices in their Scala code. Today most of our Scala projects are built around cats, fs2, doobie, algebra, shapeless, etc. In this presentation we will discuss the challenges of introducing more functional code in a large software company as Criteo and how typelevel projects have helped. We'll talk about what's worked well as well as where the dragons lie. |
 12:10 | @:style(schedule-title)Introducing namespaces into SQL result sets using nested structural types@:@ @:style(schedule-byline)Keiko Nakata@:@ Many modern programming languages support decent namespaces. Namespaces are commonly structured hierarchies.  We bring this power to a database query language, using nested structural types. For this purpose, we hijack table aliases: given a table T containing two columns C of type String and D of type Int, a table `T as S` is a new table containing two columns S.C of type String and S.D of type Int. In Scala, this is neatly expressed as `T : AnyRef { def C : String, def D: Int }` `T as S : AnyRef { def S: { def C: String, def D: Int } }`. We implement the above as operation using the whitebox macro. We rely on Scala's type system's ability to compute Greatest Lower Bounds (GLBs) and Least Upper Bounds (LUBs) of structural types, to enable polymorphic and compositional query creation. To enable GLB and LUB computation for nested structured types, we have patched the Scala compiler.
 12:45 | Lunch Break |
 14:15 | @:style(schedule-title)Healthy Minds in a Healthy Community@:@ @:style(schedule-byline)Sasha Romijn@:@ Open source communities attract and boast passionate, idealistic people, and many of us invest copious amounts of time and effort to contribute to our projects and support our communities. This underlying emotional attachment can make us more vulnerable to elevated stress, burnout and conflicts. And then there are those of us who also manage mental illness. More often than not, we suffer these struggles in silence, feeling (and fearing) that we're alone in our trouble. Here, our communities can make a huge difference, by building a positive and safe environment where we can blossom and support ourselves and our peers, and feel included. This talk will take a look at open-source communities through the eyes of various mental well-being issues and struggles, and show various things that some communities already do. With this, we hope to support and inspire more communities to help foster healthy minds in a healthy environment. |
 14:50 | @:style(schedule-title)Typedapi: Define your API on the type level@:@ @:style(schedule-byline)Paul Heymann@:@ Have you ever thought “I really like Haskell’s Servant. Why don’t we have something like that in Scala?” or “Why can't I just define my APIs as types and Scala does the heavy lifting?”? If so, this talk is made for you. I will tell you a short story about excitement, pain and hate peaking in a climax of type-driven enlightenment. I will tell you my journey of developing Typedapi, a library for building typesafe APIs which moves as many computations to the type level as possible. We will fight many a beast on our way from Scala’s desugaring to folds working just on types. But eventually, we will arrive at our destination, exhausted, with scars but also able to make our code a bit safer again.
 15:10 | Break |
 15:30 | @:style(schedule-title)An Intuitive Guide to Combining Free Monad and Free Applicative@:@ @:style(schedule-byline)Cameron Joannidis@:@ The usage of Free Monads is becoming more well understood, however the lesser known Free Applicative is still somewhat of a mystery to the average Scala developer. In this talk I will explain how you can combine the power of both these constructs in an intuitive and visual manner. You will learn the motivations for using Free Structures in the first place, how we can build up a complex domain, how we can introduce parallelism into our domain and a bunch of other practical tips for designing programs with these structures. This will also give you a deeper understanding of what libraries like Freestyle are doing under the hood and why it is so powerful. |
 16:05 | @:style(schedule-title)Laws for Free@:@ @:style(schedule-byline)Alistair Johnson@:@ Everyone that uses a functional programming library like cats is aware of the methods that each type class adds and also the properties that the methods need to abide by. But in practice, the properties are not always proved, rather testing that the methods behave as expected. This is a problem waiting to happen, as the algebraic properties are not “optional extras” – if your semigroup's combine is not associative ... then it ain't a semigroup, sorry! So in this talk we will quickly review what we mean by a property and a law and show how to use the cats laws that are available. We'll see that they are simple to use and add literally hundreds of scalacheck tests for free. And impress your boss as well, the tests can be “seen” on the screen! |
 16:25 | Break |
 16:45 | @:style(schedule-title)Lifting Data Structures to the Type-level@:@ @:style(schedule-byline)Jon Pretty@:@ In this talk, I will give a fast-paced tour of how various features of the Scala type system, many of them under-explored, can be harnessed to construct type-level representations of a number of different datatypes in Scala. The type system offers a limited number of “tools”, such as subtyping, least-upper-bound inference, type unification, singleton types and dependent types and (of course) implicit search, which we can compose in interesting ways to implement type-level operations on these type-level data structures. Value-level operations follow naturally from the types, but this is much less interesting. |
 17:20 | @:style(schedule-title)Non-academic functional Workflows@:@ @:style(schedule-byline)Stefan Schneider@:@ In this talk I want to report about how we used cats to build a domain specific language that enables us to compile workflows into later executable programs. We started with the idea of having a possibility to combine the multiple unconnected tools that are typically used to analyze an image acquired by our microscopes. The Free Monad in cats looked to us as the perfect fit to write a domain specific language that provides a lot of the advantages of an a modern functional compiler plus enforcing stack safety of the program, which would ultimately provided by third party users. We started developing with a team that had only very little experience in Scala and none with cats. Thanks to the good documentation, Scala Exercises and the straightforward mapping to functional principles, known to us from the university, we were able to get a prototype running for a trade show in 6 weeks. |
 17:40 | Closing Remarks |


## Venue

This event will take place at Zalando.




## Co-located Event

The [Scala Center](https://scala.epfl.ch/) will organize a co-located event with roundtables of project maintainers.
Note that because the space is limited, tickets are not on sale for this event.
To register interest, please get in touch [via email](mailto:info@typelevel.org).


## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

### Platinum
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/zalando.png) { alt: Zalando, title: Zalando, style: legacy-event-sponsor }](https://jobs.zalando.com/tech/?utm_source=typelevel&utm_medium=event-page-organic-b&utm_campaign=2018-css&utm_content=01-typelevel-summit)@:@
@:@

### Gold
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/crite_o_labs.png) { alt: Criteo, title: Criteo, style: legacy-event-sponsor }](https://www.criteo.com/)@:@
@:@

### Silver
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/commercetools.png) { alt: Commercetools, title: Commercetools, style: legacy-event-sponsor }](https://www.commercetools.com/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/lightbend.png) { alt: Lightbend, title: Lightbend, style: legacy-event-sponsor }](https://www.lightbend.com/)@:@
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/signify.png) { alt: Signify, title: Signify, style: legacy-event-sponsor }](https://www.signifytechnology.com/)@:@
@:@
