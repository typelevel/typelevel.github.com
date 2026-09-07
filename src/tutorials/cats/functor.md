{%
  laika.title = Functor
  scastie.code = functor.sc
%}

A `Functor` is anything you can `map` over: a container or a context whose
contents can be transformed without changing the shape of the context itself.
`List`, `Option` and `Either` are all functors, and so are many types that don't
look like collections at all.

Cats captures this as a type class with a single abstract method:

```scala
trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B]
```

Because the type class is abstract in `F`, you can write code once and run it
against every functor. The `plusOne` method in the editor never mentions `List`
or `Option`, yet it works with both.

**Try it:** call `plusOne` with a `Vector`, or map an `Option` that is `None`
and see what comes back.
