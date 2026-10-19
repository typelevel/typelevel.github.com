{%
  laika.title = Monoid
  scastie.code = monoid.sc
%}

A `Monoid` is a type that knows how to combine two of its values, and what an
"empty" value looks like:

```scala
trait Monoid[A]:
  def empty: A
  def combine(x: A, y: A): A
```

Combining must be associative, and `empty` must not change anything it is
combined with. That is enough structure to fold a whole collection without
writing a fold: Cats gives you `combineAll` for free.

Numbers, strings, lists and maps are all monoids — and so is any tuple or map
whose contents are monoids, which is where it starts to pay off.

**Try it:** combine a `List[Map[String, Int]]` and watch the values under
duplicate keys get combined rather than overwritten.
