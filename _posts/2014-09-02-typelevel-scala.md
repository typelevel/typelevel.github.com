---
layout: post
title: Typelevel Scala and the future of the Scala ecosystem

meta:
  nav: blog
  author: milessabin
  pygments: true
---

**tl;dr** Typelevel is forking Scala; we call on all stakeholders in the Scala ecosystem to collaborate on the creation of an independent, non-profit, open source foundation to safeguard the interests of the entire Scala community.

Last week I tweeted the following question:

<div>
  <blockquote><p>How much interest would there be in a community sponsored fork of the <a href="https://twitter.com/hashtag/Scala?src=hash">#Scala</a> toolchain? RTs and fav's please.</p>&mdash; Miles Sabin (@milessabin) <a href="https://twitter.com/milessabin/status/503929023635161088">August 25, 2014</a></blockquote>
</div>


It generated a lively response, both on Twitter and privately. The responses were sometimes perplexed, but typically excited and invariably positive. What I want to do here is provide some background to the question and sketch out the directions that positive answers lead.

As the Scala community has matured, a number of different strands of development of the language have emerged,

* The Typesafe Scala compiler is focused on stability and compatibility with Java.
* The LAMP/EPFL Dotty compiler is focused on providing a practical implementation of the DOT calculus.
* The Scala.js compiler is focused on targeting JavaScript as a backend platform.
* The Scala Virtualized compiler is focused on language virtualization and staging.
* The Scala.Meta system aims to provide portable metaprogramming facilities across a variety of Scala compilers.
* There are several research and private variants of the Scala compiler in development and use by a variety of academic and commercial organizations.
* The IDEs provide their own Scala variants which more or less accurately approximate Typesafe Scala.

Of these, the only compiler targeting the JVM which is generally suitable for use by the Scala community at large in the near term is the Typesafe Scala compiler. The current roadmap for the 2.12 release of this compiler, due in early 2016, has very modest goals, consistent with Typesafe's focus. Beyond that, the Next Steps roadmap mentions many things of interest, however it is clear that these are all a long way out – it will be 2017 at the earliest before any of this sees the light of day.

Typesafe's motivation for focusing on stability and Java 8 compatibility is very easy to understand. Typesafe is a commercial entity with products to sell in the JVM-based enterprise middleware space. Its primary software offerings, Akka and Play, are probably the most Java-friendly Scala projects of any significance, and simple arithmetic should tell you that a very large proportion (probably a large majority) of the potential customers for these (and the associated consultancy, training and support) are mainly Java enterprises with wholly or largely Java codebases and development teams. In this context it should be easy to see that for them an emphasis on Scala as a complement to Java, rather as its successor, is paramount.

Whilst this is entirely reasonable, and meets the needs of many, there is nevertheless a significant constituency at the core of the Scala community whose needs are not being fully met. This constituency is the segment of the Scala community which puts greater emphasis on typeful functional programming styles and which has a strong interest in current developments in functional programming in the wider world beyond Scala. The projects gathered here under the Typelevel umbrella are prime examples of that constituency.

As the producers and consumers of these and other projects we continually find ourselves running up against limitations of current Scala. Sometimes these limitations are minor and amenable to simple workarounds, many of which have passed into Scala folklore. Other limitations are more serious and can only be worked around with cumbersome encodings or otherwise elaborate and confusing hacks. These have the unfortunate consequence that elegant solutions to important problems are obscured by layers of cruft which exist only to sidestep quirks of the current compiler.

What makes this all the more frustrating is that many of these limitations are comparatively easy to remove. Fixes for some of them are purely syntactic – for instance type constructor partial application, of huge importance to Scalaz and its users, has a clunky encoding ("type lambdas") which could be given first class syntactic support without any impact on Scala's semantics and in a completely binary compatible way. Similarly, syntactic support for singleton types for literal values (see SIP-23) would be of enormous value to shapeless and Spire and their users. And the addition of literals for Bytes and Shorts would be welcomed by Spire, Scodec and many others. Other fixes, whilst affecting semantics, would do so only in a conservative way – programs which are valid when compiled with the Typesafe Scala compiler would have the same meaning and binary representation when compiled with the fixes in place.

With this in mind, we intend to create a new Scala distribution, as a conservative fork of the Typesafe Scala compiler, with a focus on meeting the needs of the part of the Scala community which has coalesced around the Typelevel stack. As a conservative fork, our aim is to be merge compatible from the Typesafe Scala compiler, and a key objective is to feed pull requests (many of which will resolve long standing Scala bugs or feature requests) back to the Typesafe Scala compiler. Our goal is to have a language which continues to evolve at the pace we saw until a couple of years ago, but with the difference that this will now be an opt-in process and the priorities will be set by the community.

Of course the devil is in the details. Forking a compiler is only a small part of the story – in many ways more important is the surrounding ecosystem of libraries. As part of this initiative we intend to publish compatible builds of at least the Typelevel libraries – taking our lead from the Typesafe community build (which attempts to track ecosystem coherence over time by building a selection of community libraries against the development Scala compiler as it evolves) and Scala.js (which has ported a selection of important community libraries to its compiler).

We welcome the participation of all other parties, individuals or organizations, who share our general goals – both those who want to contribute to the development of the compiler and those who would like their libraries and frameworks to be part of a Typelevel community build. It's early days, but we hope that with enough enthusiastic participation we will be able to produce useful binaries well before the end of the year. 

We anticipate a number of objections to this initiative,

* That it will split the community and fragment the language.
 
     As I observed earlier, there are already several variants of the language in existence and it has been clear for a long time that different sections of the community have different interests. We shouldn't be afraid of acknowledging this fact – attempting to ignore it will be (arguably is already) counterproductive. Instead we should embrace diversity as a sign of a healthy and vigorous platform and community.
     
* That we don't have the resources or the expertise to pull this off.
 
    We disagree – the community around the Typelevel projects contains many of the most able Scala programmers on the planet. Between us we have a deep understanding of Scala's type system and other semantics (both as specified and as implemented), of compiler construction in general and of Typesafe Scala compiler internals in particular. We are intimately familiar with the Scala toolchain, which many of us have been using at scale for years in our day jobs. We are also intimately familiar with the issues that we seek to address – they are ones we face daily. 
    
    We also have the existence proof of the other Scala compiler variants. The number of full-time-equivalent people working on these projects is really very small – we believe that in practice this can be matched or exceeded by an open, inclusive and enthusiastic open source project.
 
* That we underestimate the difficulty of maintaining binary and/or merge compatibility.

    No, we really don't. We fully expect this to be the most challenging part of the whole exercise. That said, we have the benefit of years of experience of Scala binary compatibility issues, and we know now that a combination of a community-build style model along with effective use of the Migration Manager (already a component of the Typelevel SBT plugin) is enormously helpful in keeping on top of the issue.

    There is a real risk here, and care will be needed. One thing is for sure though – if we don't try, we'll never know if it's possible.
  
* That the fork is too conservative.
 
     It's certainly true that restricting ourselves to only changes which are merge compatible with the Typesafe Scala compiler puts fairly strict limits on what we can do. Many highly desirable changes fall well beyond, and some people want to explore those possibilities.

    We think that this is completely reasonable, and we don't think the two are mutually exclusive – a merge compatible Typelevel compiler meets many of our immediate needs, but we want to enable people to push further just as is being done by Scala.Meta, Scala Virtualized and Dotty.

    We believe that the same infrastructure (community builds, MiMa) that will help the merge-compatible Typelevel compiler stay close to the Typesafe compiler will also be of great assistance to people who want to experiment with more radical changes. At a minimum, community build infrastructure will enable people to work with not just a bare compiler with but a core set of compatible libraries as well. We believe that such infrastructure would also benefit Scala Virtualized, Scala.Meta and Dotty.
 
This brings me to the final part of this message. It has become clear to us that there are many distinct stakeholders in the Scala ecosystem with a mixture of shared and divergent interests. This is a good thing and is something we should jointly strive to support. To that end, we believe that it is time for the formation of an independent, non-profit, open source foundation to safeguard the interests of the entire Scala community – we call on all organizations and individuals who want to see a flourishing Scala ecosystem to join with us in that project.
