---
layout: post
title: Typelevel Weaver released
category: technical

meta:
  nav: blog
  author: zainabali
---

We are delighted to announce the release of [weaver-test](https://typelevel.org/weaver-test/) under Typelevel.

# What is weaver?

Weaver is a test framework for integration and end-to-end testing. It makes tests faster and easier to debug by using `cats`, `cats-effect` and `fs2`.

Weaver provides a high quality experience when writing and running tests:

- Tests within a suite are run in parallel for the quickest results possible. This is especially suited to IO heavy tests, such as those making API calls or reading files.
- Expectations (ie assertions) are composable values. This enables
  developers to separate the scenario of the test from the checks they perform,
  generally keeping tests cleaner and clearer.
- Failures are aggregated and reported at the end of the run. This prevents the developer from having to "scroll up" forever when trying to understand what failed.
- A lazy logger is provided for each test, and log statements are only displayed in case of a test failure. This lets the developer enrich their tests with clues and works perfectly well with parallel runs. Even though all tests are run in parallel, the developer can browse a sequential log of the test failure.
- “beforeAll” and “afterAll” logic is represented using a `cats.effect.Resource`. This ensures that shared resources, such as HTTP clients, connection pools and file handles, are cleaned up correctly and predictably.

# Why is weaver moving under the Typelevel umbrella?

Weaver makes heavy use of the `cats-effect` and `fs2` Typelevel projects. These enable weaver to run tests concurrently, provide safe resource handling, composable assertions and much more. By becoming part of the Typelevel umbrella, weaver can be maintained more easily alongside its core dependencies.

# Migrating to the `0.9.0` release 

If you use [Scala Steward](https://github.com/scala-steward-org/scala-steward), you will migrate automatically. If not, read the [`0.9.0` migration guide](https://github.com/typelevel/weaver-test/releases/tag/v0.9.0).
