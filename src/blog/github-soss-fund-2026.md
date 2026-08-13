{%
  author: [${armanbilge}, ${security}]
  date: "2026-08-13"
  tags: [security]
%}

# GitHub Secure Open Source Fund

We are proud to announce that Typelevel is a graduate of Session 4 of the [GitHub Secure Open Source Fund][soss]. In addition to a $10,000 grant to the [Typelevel Foundation], the program provided three intense weeks of instruction and workshops covering secure development practices and vulnerability handling in open source. Antonio Jimenez and I participated together and, with support from the Typelevel Security Team, began applying what we learned to harden Typelevel's security posture. Some of the changes we made include:

* Requiring members of our GitHub org to enable **secure two-factor authentication methods**. We also invited all maintainers of organization projects to become full members of the org (instead of external collaborators).
* Creating an advanced security configuration that enabled **private vulnerability reporting** and applying it to all of our public repositories. We also updated our [Security Policy] with more detailed reporting instructions.
* Prototyping some [enhancements to sbt-typelevel][pr882] that integrate **vulnerability detection** into the existing dependency submission workflows. We plan to roll these out in an upcoming release.

Thank you to our maintainers for your cooperation and patience during these changes.

Following his completion of the program, the Board appointed Antonio to the Security Team in May. He will present a talk about our experience ["Securing Typelevel: Lessons from the GitHub SOS Fund"][Securing Typelevel] at Scala Days this October in Berlin. For Antonio and me, the value of participating went beyond the instruction and access to security experts: it created an important opportunity for knowledge transfer as we reviewed together prior security incidents in Typelevel and how they were handled.

[Applications are open][application] for the next round of the GitHub Secure Open Source Fund and we strongly encourage you to apply. If you are interested in participating, please [reach out] so that we can support your application.

[soss]: https://github.com/open-source/github-secure-open-source-fund
[Typelevel Foundation]: /foundation/README.md
[Security Policy]: /security.md
[pr882]: https://github.com/typelevel/sbt-typelevel/pull/882
[Securing Typelevel]: https://scaladays.org/session/securing-typelevel-lessons-from-the-github-sos-fund/
[application]: https://forms.office.com/r/YN3MWEKQ5m
[reach out]: mailto:security@typelevel.org
