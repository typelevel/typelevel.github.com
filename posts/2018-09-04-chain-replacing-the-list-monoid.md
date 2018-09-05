---
layout: post
title: Chain – Replacing the List Monoid

meta:
  nav: blog
  author: lukajcb
  pygments: true

---

`List` is a great data type, it is very simple and easy to understand.
It has very low overhead for the most important functions such as `fold` and `map` and also supports prepending a single element in constant time.

Traversing a data structure with something like `Writer[List[Log], A]` or `ValidatedNel[Error, A]` is  powerful and allows us to precisely specify what kind of iteration we want to do while remaining succint.
However, in terms of efficiency it's a whole different story unfortunately.
That is because both of these traversals make use of the `List` monoid (or the `NonEmptyList` semigroup), which by the nature of `List` is very inefficient.
If you use `traverse` with a data structure with `n` elements and `Writer` or `Validated` as the `Applicative` type, you will end up with a runtime of `O(n^2)`.
This is because, with `List`, appending a single element requires iterating over the entire data structure and therefore takes linear time.

So `List` isn't all that great for this use case, so let's use `Vector` or `NonEmptyVector` instead, right?

Well, `Vector` has its own problems and in this case it's unfortunately not that much faster than `List` at all. You can check [this blog post](http://www.lihaoyi.com/post/BenchmarkingScalaCollections.html#vectors-are-ok) by Li Haoyi for some deeper insight into `Vector`s issues.

Because of this, it's now time to welcome a new data structure to Cats.
Meet `Chain` and it's non-empty counterpart, `NonEmptyChain`.

Available in the newest Cats 1.3.0 release, `Chain` evolved from what used to be `fs2.Catenable` and Erik Osheim's [Chain](https://github.com/non/chain ) library.
Similar to `List`, it is also a very simple data structure, but unlike `List` it supports both constant O(1) time `append` and `prepend`.
This makes its `Monoid` instance super performant and a much better fit for usage with `Validated`,`Writer`, `Ior` or `Const`.

To utilize this, we've added a bunch of `NonEmptyChain` shorthands in Cats 1.3 that mirror those that used `NonEmptyList` in earlier versions. These include type aliases like `ValidatedNec` or `IorNec` as well as helper functions like `groupByNec` or `Validated.invalidNec`.
We hope that these make it easy for you to upgrade to the more efficient data structure and enjoy those benefits as soon as possible.

To get a good idea of the performance improvements, here are some benchmarks that test monoidal append (higher score is better):

```
[info] Benchmark                                  Mode  Cnt   Score   Error  Units
[info] CollectionMonoidBench.accumulateChain     thrpt   20  51.911 ± 7.453  ops/s
[info] CollectionMonoidBench.accumulateList      thrpt   20   6.973 ± 0.781  ops/s
[info] CollectionMonoidBench.accumulateVector    thrpt   20   6.304 ± 0.129  ops/s
```

As you can see accumulating things with `Chain` is more than 7 times faster than `List` and over 8 times faster than `Vector`.
So appending is a lot more performant than the standard library collections, but what about operations like `map` or `fold`?
Fortunately we've also benchmarked these (again, higher score is better):

```
[info] Benchmark                           Mode  Cnt          Score         Error  Units
[info] ChainBench.foldLeftLargeChain      thrpt   20        117.267 ±       1.815  ops/s
[info] ChainBench.foldLeftLargeList       thrpt   20        135.954 ±       3.340  ops/s
[info] ChainBench.foldLeftLargeVector     thrpt   20         61.613 ±       1.326  ops/s
[info]
[info] ChainBench.mapLargeChain           thrpt   20         59.379 ±       0.866  ops/s
[info] ChainBench.mapLargeList            thrpt   20         66.729 ±       7.165  ops/s
[info] ChainBench.mapLargeVector          thrpt   20         61.374 ±       2.004  ops/s
```

While not as dominant, `Chain` holds its ground fairly well.
It won't have the random access performance of something like `Vector`, but in a lot of other cases, `Chain` seems to outperform it quite handily.
So if you don't perform a lot of random access on your data structure, then you should be fine using `Chain` extensively instead.

So next time you write any code that uses `List` or `Vector` as a `Monoid`, be sure to use `Chain` instead!

The whole code for `Chain` and `NonEmptyChain` can be found [here](https://github.com/typelevel/cats/blob/v1.3.0/core/src/main/scala/cats/data/Chain.scala) and [here](https://github.com/typelevel/cats/blob/v1.3.0/core/src/main/scala/cats/data/NonEmptyChain.scala).
You can also check out the benchmarks [here](https://github.com/typelevel/cats/blob/v1.3.0/bench/src/main/scala/cats/bench).
