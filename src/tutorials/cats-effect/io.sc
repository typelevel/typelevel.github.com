//> using dep org.typelevel::cats-effect:3.6.3

import cats.effect.IO

// building an IO performs no effect: this line prints nothing
val hello: IO[Unit] = IO(println("hello"))

// bigger programs are built out of smaller ones
val program: IO[Unit] =
  for
    _ <- hello
    _ <- IO(println("and again"))
    n <- IO(21 * 2)
    _ <- IO(println(s"the answer is $n"))
  yield ()

// still nothing has happened; `program` is only a description
println("built the program, and nothing has run yet")
println(program)
