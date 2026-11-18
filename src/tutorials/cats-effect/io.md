{%
  laika.title = IO
  scastie.code = io.sc
%}

An `IO[A]` is a *description* of a computation that may perform side effects and
eventually produce an `A`. Building one runs nothing at all: `IO(println("hi"))`
is a value you can pass around, store in a list, or throw away.

That is the whole idea. Once effects are values, the ordinary tools work on
them: you can `map` and `flatMap` them into bigger programs, combine them in a
`for` comprehension, and repeat one simply by using it twice.

Notice in the editor that nothing is printed while the `IO`s are being built.
The description is inert until somebody runs it — which is the subject of the
next lesson.

**Try it:** reorder the steps in the `for` comprehension, or build an `IO` and
never use it.
