{%
  laika.html.template: event.template.html
  date: "2018-03-20"
  event-date: "March 20, 2018"
  event-location: "Broad Institute, Cambridge, Massachusetts"
  tags: [summits, events]
%}

# Typelevel Summit Boston

![Typelevel Summit Boston](/img/media/cambridge.jpg)

## About the Summit

The fifth Typelevel Summit will once again be co-located with the [Northeast Scala Symposium](http://www.nescala.org/) in Cambridge, Massachusetts, with one day of recorded talks and one day of (shared) unconference.
The unconference will happen on March 18, NE Scala on March 19, and finally, the Summit on March 20.
For tickets and other information about attending, please visit the website of the [Northeast Scala Symposium](http://www.nescala.org/).

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](/code-of-conduct/README.md).

## Speakers and Schedule

| Time | Talk |
|------|------|
| 8:15 | Registration & Breakfast by Clover Food Labs |
 9:00 | Opening Remarks |
 9:05 | @:style(schedule-title)http4s: pure, typeful, functional HTTP in Scala@:@ @:style(schedule-byline)Ross Baker@:@ http4s embraces cats.data.Kleisli for service definitions and fs2.Stream for payload processing. With its foundation on these simple types, we’ll take a whirlwind tour of how http4s can: plug into your functional business logic; snap together with other functional libraries; relate servers to clients; test with a simple function call; run on multiple backends; and support multiple higher level DSLs. This talk will not make you an expert in http4s, but show that it’s a relatively short path to become one.
 9:40 | Break |
 9:55 | @:style(schedule-title)Opaque types: understanding SIP-35@:@ @:style(schedule-byline)Erik Osheim@:@ Proposed in SIP-35, opaque types introduce a way to define types which only exist at compile-time. Despite some superficial similarities to value classes, opaque types are significantly more flexible and introduce a number of exciting new possibilities in the Scala design space. Opaque types are motivated by a number of different concerns: desire for a non-class type that exists only at compile-time; efficiency concerns with value classes; limitations of existing type aliases; and need to better support phantom types, type tags, etc. This talk will introduce opaque types, compare them to type aliases and value classes (their two nearest cousins) and then walk through some examples of using opaque types. The focus will be on advantages of using opaque types versus other encodings, including looking at how various types are represented by the JVM at runtime. The talk does not assume in-depth knowledge of the Scala compiler and will motivate the code using plausible real world examples. Attendees will come away from this talk with a better understanding of what SIP-35 means, why it was proposed, and how it could change how we write Scala code for the better.
 10:30 | @:style(schedule-title)Big Data at the Intersection of Typed FP and Category Theory@:@ @:style(schedule-byline)Long Cao@:@ Big data, functional programming, and category theory aren’t just three trendy topics smashed into a talk title as bait! Foundational ideas from typed functional programming and category theory have real and practical applications for working with big data and can also be utilized to write more principled pipelines at scale. Whether it’s aggregating with monoids or writing more typesafe Spark jobs, we’ll try and bridge these topics together in a way that can be immediately useful. Some knowledge of Scala and a big data framework like Apache Hadoop, Spark, or Beam is suggested but not necessary. |
 11:05 | Break |
 11:20 | @:style(schedule-title)Tracking with Writer Monad@:@ @:style(schedule-byline)Eugene Platonov@:@ This talk will tell the story of one team at eBay which used to do data tracking in a healthy side-effecting manner. Until the team realized that it’s not that healthy. The solution was found in a Writer Monad (residing in the cats library) as well as in the fact that the writer monad can stay in shades. Some people, especially when they are new to typed FP, don’t like/feel comfortable to see words like Semigroup, Traversable, Writer and such in their domain code. The talk will show how those “scary” parts can be “hidden” by domain specific extension methods. |
 11:40 | @:style(schedule-title)Duality and How to Delete Half (minus ε) of Your Code@:@ @:style(schedule-byline)Greg Pfeil@:@ In functional programming, we often refer to category theory to explain various concepts. We’ll go over where these concepts do and don’t map well to Scala, as well as what duality is, how we can take advantage of it in Scala, and how to distinguish other concepts that are often confused with it. |
 12:15 | Lunch on your own out in Kendall Square |
 14:00 | @:style(schedule-title)Keynote: Planning for Rainfall@:@ @:style(schedule-byline)Kathi Fisler@:@ Soloway's Rainfall problem, a classic benchmark in computing education research, has proven difficult for many CS1 students.  Rainfall tests students' abilities at plan composition, the task of integrating code fragments that implement subparts of a problem into a single program.  Nearly all early studies of Rainfall involved students who were learning imperative programming with arrays.  Over the last few years, we've conducted studies with students who were learning functional programming instead.  These students have produced atypical profiles of compositions and errors on Rainfall (and similar problems).  What do these results suggest about the role of programming languages in novice programming education?  This talk raises various questions about the relationships between programming languages, program design, curricula, and how students perceive code structure. The talk assumes no experience with having been rained upon. |
 15:05 | Break |
 15:20 | @:style(schedule-title)Why Monads?@:@ @:style(schedule-byline)Luca Belli@:@ Monads remain a somewhat mysterious concept in Functional Programming, even though the number of tutorials and blog posts trying to “monadsplain” is at an all-time high. Rather than answering the classical question “What is a Monad?”, we are going to dig more into “Why Monads?”. Building intuition on why monads are useful will help better understand what they are as well. We’ll start with a simple function in a monadless world and we’ll see how annoying it would be to use it in different contexts (List, Maybe, Either). As soon as we are sufficiently frustrated we’ll invoke our friendly Monad and see how much easier our life becomes. |
 15:55 | @:style(schedule-title)Pants and Monorepos@:@ @:style(schedule-byline)Dorothy Ordogh@:@ Large or quickly growing projects that consist of many interdependent sub-projects with complex dependencies on third-party libraries can be difficult to handle with standard language build tools. Add on to that code generators and the use of multiple languages and suddenly a lot of your coding life is spent figuring out the right commands to run for the right language, and waiting for all of your code to build. This is where Pants can help! Pants is an open source build tool developed and used by Twitter, Square, Foursquare, Medium, and others. This talk will begin with a brief overview of what Pants is and how it can help, and then discuss new features we have been adding to make the tool faster. In particular, I will discuss the work we have done to restrict what is going on the JVM compile classpaths to make building Scala and Java projects faster, and the work we are doing to implement a remotely executing build system. |
 16:15 | Break |
 16:30 | @:style(schedule-title)Declarative Control Flow with fs2 Stream@:@ @:style(schedule-byline)Fabio Labella@:@ fs2 is a purely functional streaming library, with support for concurrent and nondeterministic merging of arbitrary streams. Concurrency support means that we can use Stream not only to process data in constant memory, but also as a very general abstraction for control flow: whilst IO gives us an excellent model for a single effectful action, assembling behaviour with it often has a very imperative flavour (pure, but still imperative). This talk will introduce fs2 combinators by example, and will hopefully show how we can model control flow in a declarative, high level, composable fashion. In particular, we will focus on concurrent combinators. |
 17:05 | @:style(schedule-title)Scalafix @ Twitter scale@:@ @:style(schedule-byline)Uma Srinivasan@:@ Scalafix is a fairly popular OSS tool that is useful for performing syntactic and semantic rewrites of Scala code. At Twitter we use it for migration to new library interfaces and maintenance of code health by removal of deprecated code. In this talk we walk through examples of simple and complex Scalafix custom rule specifications for rewrites. We describe the core infrastructure we have set up to support rewrites across our entire monorepo, several orders of magnitude faster than if we were to apply them manually. A simple demo will be included to provide a glimpse of our developer workflow and the user experience with our code base. We envision leveraging this tool for more purposes such as improving performance, upgrading compiler revisions, and assisting developers to automatically recognize and prevent commits of disallowed code patterns. |
 17:25 | Closing Remarks |

## Sponsors

We'd like to thank all our sponsors who help to make the Summit happen:

### Gold
@:style(bulma-grid bulma-is-col-min-12)
@:style(bulma-cell bulma-has-text-centered)[@:image(/img/media/sponsors/comcast.png) { alt: Comcast, title: Comcast, style: legacy-event-sponsor }](https://www.comcast.com/)@:@
@:@
