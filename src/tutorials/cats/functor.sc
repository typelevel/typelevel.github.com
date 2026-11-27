//> using dep org.typelevel::cats-core:2.13.0

import cats.Functor
import cats.syntax.all.*

// `map` for any functor at all, chosen by the compiler from the type
def plusOne[F[_]: Functor](fa: F[Int]): F[Int] =
  fa.map(_ + 1)

println(plusOne(List(1, 2, 3)))
println(plusOne(Option(41)))
println(plusOne(Option.empty[Int]))

// the type class instance is also available directly
println(Functor[List].map(List("cats", "effect"))(_.toUpperCase))
