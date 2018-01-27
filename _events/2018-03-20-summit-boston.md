---
layout: event

title: "Typelevel Summit Boston"
short_title: "Boston Summit"
date_string: "March 20, 2018"
location: "Broad Institute, Cambridge, Massachusetts"
description: "One day of recorded talks and one day of unconference, co-located with NE Scala."

poster_hero: "/img/media/cambridge.jpg"
poster_thumb: "/img/media/cambridge-thumb.jpg"

featured: true

schedule:
  - time: TBD
    speakers: ["longcao"]
    title: "Big Data at the Intersection of Typed FP and Category Theory"
    summary: "Big data, functional programming, and category theory aren’t just three trendy topics smashed into a talk title as bait! Foundational ideas from typed functional programming and category theory have real and practical applications for working with big data and can also be utilized to write more principled pipelines at scale. Whether it’s aggregating with monoids or writing more typesafe Spark jobs, we’ll try and bridge these topics together in a way that can be immediately useful. Some knowledge of Scala and a big data framework like Apache Hadoop, Spark, or Beam is suggested but not necessary."
  - time: TBD
    speakers: ["rossabaker"]
    title: "http4s: pure, typeful, functional HTTP in Scala"
    summary: |
      <p>http4s embraces cats.data.Kleisli for service definitions and fs2.Stream for payload processing. With its foundation on these simple types, we’ll take a whirlwind tour of how http4s can:</p>

      <ul>
        <li>plug into your functional business logic</li>
        <li>snap together with other functional libraries</li>
        <li>relate servers to clients</li>
        <li>test with a simple function call</li>
        <li>run on multiple backends</li>
        <li>support multiple higher level DSLs</li>
      </ul>

      <p>This talk will not make you an expert in http4s, but show that it’s a relatively short path to become one.</p>
  - time: TBD
    speakers: ["dordogh"]
    title: "Pants and Monorepos"
    summary: "Large or quickly growing projects that consist of many interdependent sub-projects with complex dependencies on third-party libraries can be difficult to handle with standard language build tools. Add on to that code generators and the use of multiple languages and suddenly a lot of your coding life is spent figuring out the right commands to run for the right language, and waiting for all of your code to build. This is where Pants can help! Pants is an open source build tool developed and used by Twitter, Square, Foursquare, Medium, and others. This talk will begin with a brief overview of what Pants is and how it can help, and then discuss new features we have been adding to make the tool faster. In particular, I will discuss the work we have done to restrict what is going on the JVM compile classpaths to make building Scala and Java projects faster, and the work we are doing to implement a remotely executing build system."
---

## About the Summit

The fifth Typelevel Summit will once again be co-located with the [Northeast Scala Symposium](http://www.nescala.org/) in Cambridge, Massachusetts, with one day of recorded talks and one day of (shared) unconference.
The unconference will happen on March 18, NE Scala on March 19, and finally, the Summit on March 20.
For tickets and other information about attending, please visit the website of the [Northeast Scala Symposium](http://www.nescala.org/).

The Summits are open to all, not just current contributors to and users of the Typelevel projects, and we are especially keen to encourage participation from people who are new to them.
Whilst many of the Typelevel projects use somewhat "advanced" Scala, they are a lot more approachable than many people think, and a major part of Typelevel's mission is to make the ideas they embody much more widely accessible.
If you're interested in types and pure functional programming we'd love to see you here!

This is a community conference and we strive to make it an inclusive and fulfilling event for all participants. All attendees, speakers, and organizers must abide by the [Typelevel Code of Conduct](/conduct.html).

## Speakers and Schedule

{% assign schedule=page.schedule %}
{% include schedule.html %}
