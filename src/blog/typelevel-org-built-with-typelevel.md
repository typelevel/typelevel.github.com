{%
  author: [${armanbilge}, ${valencik}]
  date: "2026-02-18"
  tags: [community]
%}

# typelevel.org built with Typelevel

We are proud to share that our website is now built with [Laika], a Typelevel Organization project for generating static sites! As cool as it is that we are self-hosting, the intention of this revamp was to **make it easier for our community to develop and contribute to the website**. We [chose technologies](/colophon.md) that we hope balance familiarity and ease-of-use with functionality and stability. Notably, this new website can be generated in its entirety by running a Scala script: `scala build.scala`. Stay tuned for a future blog post that dives into the details, but for now you may peruse the [PR].

Finally, we would like to express gratitude to our friends at 47 Degrees who generously built the [previous version](new-website-layout.md) of the website for us.

## What’s next and how you can help

Truthfully, so far this is a "minimally viable website" and we invite you to [help us iterate on it](https://github.com/typelevel/typelevel.github.com#get-started). Broadly, our goals are to explain:

1. Who we are, and how you can join our community.
2. What we build, and how you can use it.

The next phase of development will largely focus on creating new content to support these goals (and the infrastructure to support that content). Here are a few ideas we have:

* Educational and tutorial content to facilitate onboarding.
    * How to **Get Started** with Typelevel using our [Toolkit].
    * Curated pathways to **Learn** how to use Typelevel in different scenarios: web services, serverless, CLIs, UIs, etc.
    * How to **Get Started Contributing** both to existing projects and also by publishing new libraries with [sbt-typelevel].
* A **Typelevel Project Index** for exploring Organization and Affiliate projects, à la [Scaladex]. We are imagining a webapp built with [Calico], with features for browsing projects, finding version numbers, and scaffolding new applications.
* Content-agnostic enhancements to the website itself.
    * Upstreaming customizations from our build to Laika.
    * Integrating [mdoc], for typechecking code.
    * Improvements to layout, styling, and theme.

We are accepting ideas and help in many forms! Please use our [issue tracker] and join the discussion on the [#website] channel in our Discord server.

## In memoriam

This project would not have been possible without [Jens Halm] and his [vision] for a documentation tool that is native to our ecosystem. Jens raised the bar for open source stewardship: beyond the technical excellence of his work on [Laika], he consistently published feature roadmaps, detailed issue and PR descriptions, and thorough documentation. Indeed, by creating a documentation tool that integrated so well with our tech stack, he has empowered *all* of us to become exemplary maintainers. Moreover, Jens' enthusiasm to support our community (including entertaining our numerous feature requests with in-depth responses full of context and design insights!) was his most generous gift to us.

[Calico]: https://armanbilge.com/calico/
[issue tracker]: https://github.com/typelevel/typelevel.github.com/issues
[Jekyll]: https://jekyllrb.com/
[Jens Halm]: https://github.com/jenshalm
[Laika]: https://typelevel.org/Laika
[mdoc]: https://scalameta.org/mdoc/
[repository]: https://github.com/typelevel/typelevel.github.com#get-started
[PR]: https://github.com/typelevel/typelevel.github.com/pull/576
[sbt-typelevel]: https://typelevel.org/sbt-typelevel/
[Scaladex]: https://index.scala-lang.org/
[Toolkit]: https://typelevel.org/toolkit
[vision]: https://typelevel.org/Laika/latest/01-about-laika/02-design-goals.html
[#website]: https://discord.gg/krrdNdSDFf
