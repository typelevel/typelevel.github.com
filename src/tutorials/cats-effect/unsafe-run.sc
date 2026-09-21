//> using dep org.typelevel::cats-effect:3.6.3

import cats.effect.IO
import cats.effect.unsafe.implicits.global

val program: IO[Int] =
  for
    _ <- IO(println("starting"))
    n <- IO(21 * 2)
    _ <- IO(println(s"finished with $n"))
  yield n

// nothing above this line has printed anything
println("about to run")

val result = program.unsafeRunSync()
println(s"got back $result")
