//> using dep org.typelevel::cats-core:2.13.0

import cats.Monoid
import cats.syntax.all.*

// `combine` (aliased to |+|) and `empty` come from the Monoid instance
println(1 |+| 2)
println("type" |+| "level")
println(Monoid[List[Int]].empty)

// folding a collection with its monoid
println(List(1, 2, 3, 4).combineAll)
println(List("a", "b", "c").combineAll)

// tuples and maps are monoids when their contents are
println(List(("cats", 1), ("cats", 2)).combineAll)
println(List(Map("a" -> 1), Map("a" -> 2, "b" -> 3)).combineAll)
