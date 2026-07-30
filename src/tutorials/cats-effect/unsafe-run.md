{%
  laika.title = Running an IO
  scastie.code = unsafe-run.sc
%}

Descriptions have to be executed eventually. In a real application that happens
once, at the edge of the world: you extend `IOApp`, hand it your program, and
Cats Effect runs it on its own runtime.

The `unsafeRun*` methods let you do it by hand. They are called *unsafe* not
because they are broken, but because they break the property that makes `IO`
worth having: they actually perform effects, and they can block or throw. Every
call is a place where functional reasoning stops.

- `unsafeRunSync()` blocks the current thread until the program finishes.
- `unsafeRunAndForget()` starts it and returns immediately.
- `unsafeToFuture()` hands you a `Future` of the result.

Running the same description twice performs the effects twice: an `IO` is a
recipe, not a cached result.

**Try it:** run `program` a second time, or comment out the `unsafeRunSync()`
call and watch the output disappear.
