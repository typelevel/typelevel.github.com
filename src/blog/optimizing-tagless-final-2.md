{%
  author: ${lukajcb}
  date: "2018-06-27"
  tags: [technical]
%}

# Optimizing Tagless Final – Part 2 – Monadic programs

In our previous post on optimizing tagless final programs we learned how we could use the [sphynx library](https://github.com/LukaJCB/sphynx) to derive some optimization schemes for your tagless final code. In case you missed it and want to read up on it, you can find it [right here](https://typelevel.org/blog/2017/12/27/optimizing-final-tagless.html) or you can watch my presentation on the topic [here](https://www.youtube.com/watch?v=E9iRYNuTIYA), but you should be able to follow this blog post without going through it all in detail.

## Optimizing monadic programs

One of the questions I've been getting a lot, is if we can also do something like that for the monadic parts of our program.
The answer is yes, we can, however it will have to be quite a bit different.

I don't think the differences are quite obvious, so we'll go through them step by step.
With applicative programs, we're optimizing a bunch of independent instructions.
That means, we can look at all of them and extract information out of them statically (i.e. without running the interpreter).
They can be seen as a sequence of instructions that we can fold down to a single monoid `M`, that holds the information that we need to optimize.
We then used that monoid to recreate a new interpreter that can take this extra information into account.

With monadic programs, we do not have such luxury.
We can only step through each of our instructions one at a time, because every instruction depends on the results of the prior one.
This means that we cannot extract any information beyond the very first instruction we have.
That might seem like a deal breaker, but there's still a few things we can do.
We could, for example, build up our monoid `M` dynamically, after each monadic instruction.
Then, before invoking the next computation in the monadic sequence, we could take that monoid and recreate that next computation with that extra information.

Now, that might sound super abstract to you, and I wouldn't disagree, so let's look at a quick example.
Say, we're using the `KVStore` algebra again from last time:


```scala
trait KVStore[F[_]] {
  def get(key: String): F[Option[String]]
  def put(key: String, value: String): F[Unit]
}
```

We could optimize programs with this algebra by caching the results of `get` and we could use that same cache to also cache key-value pairs we inserted using `put`.

So given this example program:

```scala
def program[F[_]: Monad](key: String)(F: KVStore[F]): F[List[String]] = for {
  _ <- F.put(key, "cat")
  dog <- F.get(key)
  cat <- F.get(dog.getOrElse("dog"))
  cat2 <- F.get("cat")
} yield List(dog, cat, cat2).flatten
```

The naively interpreted program would be doing the following things:
1. put the value "cat" into the store with the `key` passed by the user
2. get the value "cat" back out of the store
3. access the key-value store and maybe return a value associated with the key "cat"
4. access the store again with the same "cat" key.


Now if accessing the key-value store means going through a network layer this is of course highly inefficient.
Ideally our fully optimized program should do the following things:
1. put the value "cat" into the store with the `key` parameter passed by the user and cache it.
2. access the cache to get the value "cat" associated with `key`
3. access the key-value-store and maybe return a value associated with the key "cat"
4. access the cache to return the previous result for the "cat" key.

Cool, next, let's look at how we might get there.
First the type of our cache, which for our case can just be a `Map[String, String]`, but generically could just be any monoid.

Now what we want to do is transform any interpreter for `KVStore` programs into interpreters that
1. Look in the cache before performing a `get` action with the actual interpreter
2. Write to the cache after performing either a `get` or `put` action.

So how can we get there? It seems like we want to thread a bunch of state through our program, that we want to both read and write to.
If you're familiar with FP folklore you might recognize that that description fits almost exactly to the `State` monad.
Furthermore, because we know that our `F[_]` is a monad, that means the `StateT` monad transformer over `F` will also be a monad.

Okay with that said, let's try to develop function that turns any interpreter `KVStore[F]` into an interpreter into `StateT[F, M, A]`, so an `KVStore[StateT[F, M, ?]]`, where `M` is the monoid we use to accumulate our extracted information.
We'll start with the `put` operation.
For `put`, we'll want to call the interpreter to perform the action and then modify the state by adding the retrieved value into our cache.
To make the code a bit more legible we'll also define a few type aliases.

```scala
type Cache = Map[String, String]
type CachedAction[A] = StateT[F, Cache, A]

def transform(interp: KVStore[F]): KVStore[CachedAction] = new KVStore[CachedAction] {
  def put(key: String, v: String): CachedAction[Unit] =
    StateT.liftF[F, Cache, Unit](interp.put(key, v)) *> StateT.modify(_.updated(key, v))

  def get(key: String): CachedAction[Option[String]] = ???
}
```

So far, so good, now let's have a look at what to do with the `get` function.
It's a bit more complex, because we want to read from the cache, as well as write to it if the cache didn't include our key.
What we have to do is, get our current state, then check if the key is included, if so, just return it, otherwise call the interpreter to perform the `get` action and then write that into the cache.

```scala
def get(key: String): CachedAction[Option[String]] = for {
  cache <- StateT.get[F, Cache]
  result <- cache.get(key) match {
              case s @ Some(_) => s.pure[CachedAction]
              case None => StateT.liftF[F, Cache, Option[String]](interp.get(key))
                             .flatTap(updateCache(key))
            }
} yield result

def updateCache(key: String)(ov: Option[String]): CachedAction[Unit] = ov match {
  case Some(v) => StateT.modify(_.updated(key, v))
  case None => ().pure[CachedAction]
}
```

This is quite something, so let's try to walk through it step by step.
First we get the cache using `StateT.get`, so far so good.
Now, we check if the key is in the cache using `cache.get(key)`.
The result of that is an `Option[String]`, which we can pattern match to see if it did include the key.
If it did, then we can just return that `Option[String]` by lifting it into `CachedAction` using `pure`.
If it wasn't in the cache, things are a bit more tricky.
First, we lift the interpreter action into `CachedAction` using `StateT.liftF`, that gives us a `CachedAction[Option[String]]`, which is already the return type we need and we could return it right there, but we still need to update the cache.
Because we already have the return type we need, we can use the `flatTap` combinator.
Then inside the `updateCache` function, we take the result of our interpreter, which is again an `Option[String]`, and update the cache if the value is present.
If it's empty, we don't want to do anything at all, so we just lift unit into `CachedAction`.

In case you're wondering `flatTap` works just like `flatMap`, but will then `map` the result type back to the original one, making it a bit similar to a monadic version of the left shark (`<*`) operator, making it very useful for these "fire-and-forget" operations.
It's defined like this:

```scala
def flatTap[F[_]: Monad, A, B](fa: F[A])(f: A => F[B]): F[A] =
  fa.flatMap(a => f(a).map(b => a))
```

And with that we now have a working function to turn any interpreter into an optimized interpreter.
We can also generalize this fairly easily into a function that will do all of the wiring for us.
To do so, we'll generalize away from `KVStore` and `Cache` and instead use generic `Alg[_[_]]` and `M` parameters:


```scala
def optimize[Alg[_[_]], F[_]: Monad, M: Monoid, A]
  (program: MonadProgram[Alg, A])
  (withState: Alg[F] => Alg[StateT[F, M, ?]]): Alg[F] => F[A] = interpreter =>
    program(withState(interpreter)).runEmptyA
```

Just like last time, we have to use a `MonadProgram` wrapper around `Alg[F] => F[A]`, because Scala lacks rank-N types which would allow us to define values that work over ALL type constructors `F[_]: Monad` (Fortunately however, this will very probably soon be fixed in dotty, PR [here](https://github.com/lampepfl/dotty/pull/4672)).

Now let's see if we can actually use it, by checking it with a test interpreter that will print whenever we retrieve or insert values into the `KVStore`.


```scala
optimize[KVStore, IO, Cache, List[String]](program("mouse"))(transform)
  .apply(printInterpreter)
  .unsafeRunSync()

// Put key: mouse, value: cat
// Get key: cat
```

It works and does exactly what we want! 
Nice! We could end this blog post right here, but there's still a couple of things I'd like to slightly alter.

### Refining the API

As you were able to tell the implementation of our transformation from the standard interpreter to the optimized interpreter is already quite complex and that is for a very very simple algebra that doesn't do a lot.
Even then, I initially wrote an implementation that packs everything in a single `StateT` constructor to avoid the overhead of multiple calls to `flatMap`, but considered the version I showed here more easily understandable.
For more involved algebras and more complex programs, all of this will become a lot more difficult to manage.
In our last blog post we were able to clearly separate the extraction of our information from the rebuilding of our interpreter with that information.
Let's have a look at if we can do the same thing here.

First we'll want to define an extraction method.
For applicative programs we used `Const[M, ?]`, however that cannot work here, as `Const` doesn't have a `Monad` instance and also, because for extraction with monadic programs, we need to actually take the result of the computation into account. 
That means, that for every operation in our algebra, we want a way to turn it into our monoid `M`.
With that said, it seems we want a function `A => M`, where `A` is the result type of the operations in our algebra.
So what we can do here is define an algebra for `? => M`, in types an `Alg[? => M]`.

Let's try to do define such an interpreter for our `KVStore` along with `Cache`/`Map[String, String`:

```scala
def extract: KVStore[? => Cache] = new KVStore[? => Cache] {
  def get(key: String): Option[String] => Cache = {
    case Some(s) => Map(key -> s)
    case None => Map.empty
  }

  def put(key: String, a: String): Unit => Cache =
    _ => Map(key -> a)
}
```

Just as before we want to extract the cache piece by piece with every monadic step.
Whenever we get an `Option[String]` after using `get`, we can then turn that into a `Cache` if it's non-empty.
The same goes for `put`, where we'll create a Map using the key-value pair.
We now have a way to turn the results of our algebra operations into our information `M`, so far so good!

Next, we'll need a way to rebuild our operations using that extracted information.
For that, let's consider what that actually means.
For applicative programs this meant a function that given a state `M` and an interpreter `Alg[F]`, gave a  reconstructed interpreter inside the `F` context `F[Alg[F]]`.
So a function `(M, Alg[F]) => F[Alg[F]]`.

For monadic programs, there's no need to precompute any values, as we're dealing with fully sequential computations that can potentially update the state after every evaluation.
So we're left with a function `(M, Alg[F]) => Alg[F]`.
Let's try building that for `KVStore`:

```scala
def rebuild(m: M, interp: KVStore[F]): KVStore[F] = new KVStore[F] {
  def get(key: String): F[Option[String]] = m.get(key) match {
    case o @ Some(_) => Monad[F].pure(o)
    case None => interp.get(key)
  }

  def put(key: String, a: String): F[Unit] =
    m => interp.put(key, a)
}
```

Easy enough!
For `get` we look inside our cache and use the value if it's there, otherwise we call the original interpreter to do its job.
For `put`, there's nothing to gain from having access to our extracted information and the only thing we can do is call the interpreter and let it do what needs to be done.

Now we have a way to extract information and then also use that information, next up is finding a way to wire these two things together to get back to the behaviour we got using `StateT`.

And as a matter of fact, we'll wire them back together using exactly `StateT`, as it's monad instance does do exactly what we want.

Using our two functions `extract` and `rebuild` it's fairly easy to get back to `KVStore[StateT[F, Cache, ?]]`:

```scala
def transform(interp: KVStore[F]): KVStore[StateT[F, Cache, ?]] = new KVStore[StateT[F, Cache, ?]] {
  def put(key: String, v: String): StateT[F, Cache, Unit] =
    StateT(cache => rebuild(cache, interp).put(key, v).map(a => 
      (cache |+| extract.put(key, v)) -> a))

  def get(key: String): StateT[F, Cache, Option[String]] =
    StateT(cache => rebuild(cache, interp).get(key).map(a => 
      (cache |+| extract.get(key)) -> a))
}
```

This is fairly straightforward, we use rebuild with our cache and the interpreter to get a new interpreter that will run the operation.
Then, we use the result, which is just an `F[Unit]`/`F[Option[String]]` respectively, and map it 
  using the extractor to get the newest `Cache` and using its `Monoid` instance to update the state and then we tuple it with the result, giving us an `F[(Cache, Unit)]` or `F[(Cache, Option[String])]`, which is exactly what the `StateT` constructor needs. 

This is great, but can we generalize this to any algebra and any monoid?

The answer is yes, but it's not exactly easy.
First let's look at the actual problem.
We have two interpreters `extract` and `rebuild`, but we have no way to combine them, because `Alg`, is completely unconstrained and that means we can't call any functions on a generic `Alg[F]` at all.
So, okay, we need to constrain our `Alg` parameter to be able to combine values of `Alg[F]` with values of `Alg[G]` in some way, but what kind of type class could that be?
Are there even type classes that operate on the kind of `Alg`? 


### Higher kinded things

There are, they're just hidden away in a small library called `Mainecoon`.
That library gives us higher kinded versions of things like functors and contravariant functors, called `FunctorK` and `ContravariantK` respectively.

Let's have a quick look at `FunctorK`:

```scala
@typeclass
trait FunctorK[A[_[_]]] {
  def mapK[F[_], G[_]](af: A[F])(f: F ~> G): A[G]
}
```

Instead of mapping over type constructors `F[_]`, we map over algebras `A[_[_]]` and insteading of using functions `A => B`, we use natural transformations `F ~> G`.
This is nice, but doesn't really get us that far. 

What we really need is the equivalent of the `Applicative`/`Apply` `map2` operation.
`map2` looks like this:

```scala
def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
```

And a higher kinded version would look like this:

```scala
def map2K[F[_], G[_], H[_]](af: A[F], ag: A[G])(f: Tuple2K[F, G, ?] ~> H): A[H]
```

If you haven't guessed yet `Tuple2K` is just a higher kinded version of `Tuple2`:

```scala
type Tuple2K[F[_], G[_], A] = (F[A], G[A])
```

Unfortunately `Mainecoon` doesn't have an `ApplyK` type class that gives us this `map2K` operation, but it gives the next best thing! 
A higher-kinded `Semigroupal`, which when combined with the higher kinded `Functor` gives us that higher kinded `Apply` type class.
It's called `CartesianK` (because cats `Semigroupal` used to be called `Cartesian`, but is renamed to `SemigroupalK` in the next version) and looks like this:

```scala
@typeclass 
trait CartesianK[A[_[_]]] {
  def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, ?]]
}
```

Now just like you can define `map2` using `map` and `product` we can do the same for `map2K`:

```scala
def map2K[F[_], G[_], H[_]](af: A[F], ag: A[G])(f: Tuple2K[F, G, ?] ~> H): A[H] =
  productK(af, ag).mapK(f)
```


### Putting it all together

Okay, after that quick detour, let's have a look at how can make use of these type classes.

If we look at what we have and how we'd like to use the `map2K` function, we can infer the rest that we need quite easily.

We have an `Alg[F]` and a `Alg[? => M]`, and we want an `Alg[StateT[F, M, ?]]`, so given those two as the inputs to `map2K`, all that seems to be missing is the natural transformation `Tuple2K[F, ? => M, ?] ~> StateT[F, M, ?]`.
Nice! As so often, the types guide us and show us the way.

Well let's try to define just that:

```scala
new (Tuple2K[F, ? => M, ?] ~> StateT[F, M, ?]) {
  def apply[A](fa: Tuple2K[F, ? => M, ?]): StateT[F, M, A] =
    StateT(m => F.map(fa.first)(a => M.combine(fa.second(a), m) -> a))
}
```

This looks good, but actually has a problem, to get an `Alg[F]` from `rebuild` we give it an `M` and an interpreter `Alg[F]`. 
The interpreter isn't really a problem, but the `M` can prove problematic as we need to give it to the `rebuild` function after each monadic step to always receive the latest state.
If we look at our natural transformation above, that function will never receive the newest state.
So what can we do about this?
Well, we could be a bit more honest about our types:

```scala
type FunctionM[A] = M => F[A]
def rebuild(interp: Alg[F]): Alg[FunctionM]
```

Hey, now we're getting there. This works, but if we look into some of the data types provided by `Cats` we can acutally see that this is just `Kleisli` or `ReaderT`, so our `rebuild` should actually look like this:

```scala
def rebuild(interp: Alg[F]): Alg[Kleisli[F, M, ?]]
```

And now, we can easily implement a correct version of that natural transformation from earlier:

```scala
new (Tuple2K[Kleisli[F, M, ?], ? => M, ?] ~> StateT[F, M, ?]) {
  def apply[A](fa: Tuple2K[Kleisli[F, M, A], ? => M, ?]): StateT[F, M, A] =
    StateT(m => F.map(fa.first.run(m))(a => (fa.second(a) |+| m) -> a))
}
```

Cool, then let us also adjust the rebuild function we created for `KVStore`:

```scala
def rebuild(interp: KVStore[F]): KVStore[Kleisli[F, M, ?]] = new KVStore[Kleisli[F, M, ?]] {
  def get(key: String): Kleisli[F, Cache, Option[String]] = Kleisli(m => m.get(key) match {
    case o @ Some(_) => Monad[F].pure(o)
    case None => interp.get(key)
  })

  def put(key: String, a: String): Kleisli[F, Cache, Unit] =
    Kleisli(m => interp.put(key, a))
}
```

It's stayed pretty much the same, we just needed to wrap the whole thing in a `Kleisli` and we're good!

Now we can go ahead and define the full function signature:

```scala
def optimize[Alg[_[_]]: FunctorK: CartesianK, F[_]: Monad, M: Monoid, A]
  (program: MonadProgram[Alg, A])
  (extract: Alg[? => M])
  (rebuild: Alg[F] => Alg[Kleisli[F, M, ?]]): Alg[F] => F[A] = { interpreter =>
  
    val tupleToState = new (Tuple2K[Kleisli[F, M, ?], ? => M, ?] ~> StateT[F, M, ?]) {
      def apply[A](fa: Tuple2K[Kleisli[F, M, A], ? => M, A]): StateT[F, M, A] =
        StateT(m => F.map(fa.first.run(m))(a => (fa.second(a) |+| m) -> a))
    }

    val withState: Alg[StateT[F, M, ?]] =
      map2K(extract(interpreter), rebuild))(tupleToState)

    program(withState).runEmptyA
  
  }
```

That is all, we've got a fully polymorphic function that can optimize monadic programs.

Let's use it!

```scala
optimize(program)(extract)(rebuild)
  .apply(printInterpreter)
  .unsafeRunSync()
```


Now, when we run this, it should be exactly the same result as when we ran it earlier using the direct `StateT` interpreter, but the resulting code is much cleaner.
However, it does have the drawback that you'll now need additional constraints for every algebra to use this function.
That said though, one of the cool features of `Mainecoon` is that it comes with auto-derivation.
Meaning we can just add an annotation to any of our algebras and it will automatically derive the `FunctorK` and `CartesianK` instances.

In fact, that is exactly how I defined those two instances for the `KVStore` algebra:

```scala
@autoFunctorK
@autoCartesianK
trait KVStore[F[_]] { ... }
```

This makes it fairly easy to use these extra type classes and helpts mitigate the drawbacks I mentioned.

### Conclusions

Today we've seen a way to make optimizing monadic tagless final programs easier and intuitive, all the code is taken from the sphynx library and can be found [right here](https://github.com/LukaJCB/sphynx), but might still be subject to change, because designing a good API is hard.

What do you think about this optimization scheme? Maybe you just prefer using `StateT` and being done with it, or maybe you like to use a typeclass based approach like the one we used last time?

Would love to hear from you all in the comments!
