---
layout: post
title: A comprehensive introduction to Cats-mtl

meta:
  nav: blog
  author: lukajcb
  pygments: true

tut:
  scala: 2.12.7
  binaryScala: "2.12"
  scalacOptions:
    - "-Ypartial-unification"
    - "-language:higherKinds"
  dependencies:
    - org.typelevel::cats-mtl-core:0.4.0
    - org.typelevel::cats-effect:1.0.0
  plugins:
    - org.spire-math::kind-projector:0.9.8
---

## A comprehensive introduction to MTL

MTL is a library for composing monad transformers and making it easier to work with nested monad transformer stacks.
It originates from the land of Haskell, but has made it into Scala a long time ago.
For the longest time however, it was barely usable, because of a bunch of different Scala quirks coming together.
With all this, I feel many have the impression that mtl is something scary, abstract or too complicated.
In this blog post, I'll try my best to disprove this notion and demonstrate the simplicity and elegance of Cats-mtl. After reading this, I hope you'll agree that one should prefer `mtl`  whenever one needs to compose more than one monad transformer nested inside of each other.

## What is mtl?

Mtl is an acronym and stands for *Monad Transformer Library*. Its main purpose it make it easier to work with nested monad transformers. It achieves this by encoding the effects of most common monad transformers as type classes.
To understand what this means we'll first have to look at some of the common monad transformers.

I'll go over some of the lesser known transformers `StateT` and `ReaderT` next, so feel free to skip the next section if you already know about `StateT` and `ReaderT`.


### ReaderT

`ReaderT` allows us to *read* from an environment and create other values that depend on the environment.
This can be especially useful for e.g. reading from some external configuration.
Some like to describe this as the functional programming equivalent of dependency injection.

As an example, let's imagine we want to make a call to a service, but to make that call we need to pass some configuration.

First, some imports and some declarations:

```scala
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

// These are just String for simplicity
type Config = String
type Result = String

```

Now let's say we have these two functions for the service we want to call and the configuration we want to read from.
```scala

def getConfig: IO[Config] = ???
// getConfig: cats.effect.IO[Config]

def serviceCall(c: Config): IO[Result] = ???
// serviceCall: (c: Config)cats.effect.IO[Result]
```

The easiest thing would be to just pass down the configuration from the very top of your application.
However that can be pretty tedious, so what we do instead is use `ReaderT`.
`ReaderT` gives us the `ask` function, which gives us access to a read-only environment value of type `E`:

```scala
def ask[F[_]: Applicative, E]: ReaderT[F, E, E]
```

We can then use `flatMap`, `map` or for-comprehensions to actually use that value and do things with it: 

```scala
def readerProgram: ReaderT[IO, Config, Result] = for {
  config <- ReaderT.ask[IO, Config]
  result <- ReaderT.liftF(serviceCall(config))
} yield result
// readerProgram: cats.data.ReaderT[cats.effect.IO,Config,Result]
```

Now that we have a value of `ReaderT` that gives us back our result, the next step is to actually "inject" the dependency.
For this purpose, `ReaderT[F, E, A]` gives us a `run` function that expects us to give it a value of `E` and will then return an `F[A]`, so in our case an `IO` of `Result`:

```scala
def run(e: E): F[A]
```

Combined with our `getConfig` function we can now write the entry point to our program:

```scala
def main: IO[Result] = getConfig.flatMap(readerProgram.run)
// main: cats.effect.IO[Result]
```

And that is how we can do functional dependency injection in Scala.
However, I believe this pattern isn't used very often, because it forces you to wrap all of your steps in `ReaderT`.
If you continue reading on, we'll go through how this problem can be mitigated using MTL.


### StateT

Like `ReaderT`, `StateT` also allows us to read from an environment.
However, unlike `ReaderT`, it also allows us to write to that environment, making it capable of holding state, hence the name. 
With `StateT` over `IO`, we can deliberately create programs that can access the outside world and also maintain mutable state.
This is very powerful and, when used without care, can give rise to similar problems as can be found in imperative programs that abuse global mutable state and unlimited side effects.
Use `StateT` with care however, and it can be a really great tool for parts of your application that require some notion of mutable state.

An example use case that comes up very often is the ability to send some requests to an external services and after each of those requests, use the resulting value to modify an environment with which you'll create the next request.
This environment could be used for something simple like a cache, or something more complex like dynamically changing the parameters of each request, depening on what state the environment currently holds.
Let's look at an abstract example, that showcases this ability.

First, we'll define a function that calls our external service which will take the environment into account.

```scala
// Again we use String here for simplicity, in real code this would be something else
type Env = String
type Request = String
type Response = String

def initialEnv: Env = ???

def request(r: Request, env: Env): IO[Response] = ???
``` 

Next, we'll also need a function that given a response and an old environment will return a new updated environment.

```scala
def updateEnv(r: Response, env: Env): Env = ???

// We also need some fake requests
def req1: Request = ???
def req2: Request = ???
def req3: Request = ???
def req4: Request = ???
```

Now we can get started with `StateT`.
To do so, we'll create a new request function that will make the request with the current environment and update it after we've received the response:

```scala
def requestWithState(r: Request): StateT[IO, Env, Response] = for {
  env <- StateT.get[IO, Env]
  resp <- StateT.liftF(request(r, env))
  _ <- StateT.modify[IO, Env](updateEnv(resp, _))
} yield resp
```

This demonstrates the power of `StateT`.
We can get the current state by using `StateT.get` (which returns a `StateT[IO, Env, Env]` similar to `ReaderT.ask`) and we can also modify it using `StateT.modify` (which takes a function `Env => Env` and returns a `StateT[IO, Env, Unit]`).

Now, if we wanted to make those different requests, we could just reuse that `requestWithState` function N number of times:

```scala
def stateProgram: StateT[IO, Env, Response] = for {
  resp1 <- requestWithState(req1)
  resp2 <- requestWithState(req2)
  resp3 <- requestWithState(req3)
  resp4 <- requestWithState(req4)
} yield resp4
```

And now we have a fully fledged program exactly as we wanted.
But what can we actually do with the `StateT` value?
To run the full program, we need an `IO`. 
Of course, just like `ReaderT`, we can turn `StateT` into `IO` by using the `run` method and supplying an initial value for our environment. 
Let's try that out!


```scala
def main: IO[(Env, Response)] = stateProgram.run(initialEnv)
// main: cats.effect.IO[(Env, Response)]
```

And that gives us a fully working stateful application. Cool.
Next, we'll look at how we can combine different transformers and what monad transformers actually represent.


## Monad Transformers encode some notion of *effect*

`EitherT` encodes the effect of short-circuiting errors.
`ReaderT` encodes the effect of reading a value from the environment.
`StateT` encodes the effect of pure local mutable state.

All of these monad transformers encode their effects as data structures, but there's another way to achieve the same result: Type classes!

For example we've looked extensively at the `ReaderT.ask` function, what would it look like if we used a type class here instead?
Well, Cats-mtl has an answer and it's called `ApplicativeAsk`.
You can think of it as `ReaderT` encoded as a type class:

```scala
trait ApplicativeAsk[F[_], E] {
  val applicative: Applicative[F]

  def ask: F[E]
}
```

At it's core `ApplicativeAsk` just encodes the fact that we can ask for a value from the environment, exactly like `ReaderT` does.
Exactly like `ReaderT`, it also includes another type parameter `E`, that represents that environment.

If you're wondering why `ApplicativeAsk` has an `Applicative` field instead of just extending from `Applicative`, that is to avoid implicit ambiguities that arise from having multiple subclasses of a given type (here `Applicative`) in scope implicitly.
So in this case we favor composition over inheritance as otherwise, we could not e.g. use `Monad` together with `ApplicativeAsk`.
You can read more about this issue in this excellent [blog post by Adelbert Chang](https://typelevel.org/blog/2016/09/30/subtype-typeclasses.html).

### Effect type classes

`ApplicativeAsk` is an example for what is at the core of Cats-mtl.
Cats-mtl provides type classes for most common effects which let you choose what kind of effects you need without committing to a specific monad transformer stack.

Ideally, you'd write all your code using only an abstract type constructor `F[_]` with different type class constraints and then at the end run that code with a specific data type that is able to fulfill those constraints.

So without further ado, let's try to convert our `Reader` program from earlier into mtl-style.
First, I'll include the original program again:

```scala
def getConfig: IO[Config] = ???
// getConfig: cats.effect.IO[Config]

def serviceCall(c: Config): IO[Result] = ???
// serviceCall: (c: Config)cats.effect.IO[Result]

def readerProgram: ReaderT[IO, Config, Result] = for {
  config <- ReaderT.ask[IO, Config]
  result <- ReaderT.liftF(serviceCall(config))
} yield result
// readerProgram: cats.data.ReaderT[cats.effect.IO,Config,Result]

def main: IO[Result] = getConfig.flatMap(readerProgram.run)
// main: cats.effect.IO[Result]
```

Now we should just replace that `ReaderT` with an `F` and add an `ApplicativeAsk[F, Config]` constraint, right?
We have one small problem though, how can we lift our `serviceCall` which is an `IO` value, into our abstract `F` context?
Fortunately `cats-effect` already defines a typeclass designed to help us out here called `LiftIO`.
It defines a single function `liftIO` that does exactly what you'd expect:

```scala
@typeclass trait LiftIO[F[_]] {
  def liftIO[A](io: IO[A]): F[A]
}
```

If there's an instance for `LiftIO[F]` we can lift any `IO[A]` into an `F[A]`.
Furthermore `IO` defines a method `to` which makes use of this type class to provide some nicer looking syntax.

With this in mind, we can now define our `readerProgram` fully using MTL:

```scala
import cats.mtl._
import cats.mtl.instances.all._

def readerProgram[F[_]: Monad: LiftIO](implicit A: ApplicativeAsk[F, Config]): F[Result] = for {
  config <- A.ask
  result <- serviceCall(config).to[F]
} yield result
```

We replaced our call to `ReaderT.ask` with a call to `ask` provided by `ApplicativeAsk` and instead of using `ReaderT.liftF` to lift an `IO` into `ReaderT`, we can simply use the `to` function on `IO`, pretty neat if you ask me.

Now to run it, all we need to do is specify the target `F` to run in, in our case `ReaderT[IO, Config, Result]` fits perfectly:

```scala
val materializedProgram = readerProgram[ReaderT[IO, Config, ?]]

def main: IO[Result] = getConfig.flatMap(materializedProgram.run)
```

This process of turning a program defined by an abstract type constructor with additional type class constraints into an actual concrete data type is sometimes called *interpreting*  or *materializing*  a program.

Another thing we can do is define a type alias for `ApplicativeAsk[F, Config]` so that we can more easily use it with the context bound syntax:

```scala
type ApplicativeConfig[F[_]] = ApplicativeAsk[F, Config]

def readerProgram[F[_]: Monad: LiftIO: ApplicativeConfig]: F[Result] = ???
```

So far so good, but this doesn't seem to be any better than what we had before.
I've teased at the beginning that MTL really shines once you use more than one monad transformer.
So let's say our program now also needs to be able to handle errors (which I think is a very reasonable requirement).

To do so, we'll use `MonadError`, which can be found in cats-core instead of mtl, but in its essence, it encodes the short circuting effect that's shared with `EitherT`.

To keep things simple for now, we want to raise an error if the configuration we got was invalid somehow.
For this purpose we'll have this simple function that will simply return if a `Config` is valid or not:

```scala
def validConfig(c: Config): Boolean = ???
```

Then we'll also want to define an error ADT for our app:

```scala
sealed trait AppError
case object InvalidConfig extends AppError
```

Now we can go and extend our program from earlier. 
We'll add a `MonadError[F, AppError]` type alias, `MonadAppError` and then add a constraint for it in our program.

```scala
type MonadAppError[F[_]] = MonadError[F, AppError]

def program[F[_]: MonadAppError: ApplicativeConfig: LiftIO]: F[Result] = ???
```

Now we want so ensure somehow that our config is valid and raise an `InvalidConfig` error if it's not.
To do so, we'll simply use the `ensure` function provided by `MonadError`.
It looks like this:

```scala
def ensure(error: => E)(predicate: A => Boolean): F[A]
```

And it fills our need exactly. It will raise the passed `error`, if the `predicate` function returns `false`.
Let's go and try it out:

```scala
def program[F[_]: MonadAppError: ApplicativeConfig: LiftIO]: F[Result] = for {
  config <- ApplicativeAsk[F, Config].ask
              .ensure(InvalidConfig)(validConfig)
  result <- serviceCall(config).to[F]
} yield result
// program: [F[_]](implicit evidence$1: MonadAppError[F], implicit evidence$2: ApplicativeConfig[F], implicit evidence$3: cats.effect.LiftIO[F])F[Result]
```

Pretty simple, now let's materialize it!
To do so, we'll use a monad stack of `ReaderT`, `EitherT` and `IO`.
Unwrapped it should look like this `IO[Either[AppError, Reader[Config, A]]]`.

We'll create some type aliases to get a better overview:


```scala
type EitherApp[A] = EitherT[IO, AppError, A]
type Stack[A] = ReaderT[EitherApp, Config, A]

val materializedProgram: Stack[Result] = program[Stack]

def main: IO[Either[AppError, Result]] =
  EitherT.liftF(getConfig).flatMap(materializedProgram.run).value
```


This is the magic of mtl, it is able to give you type class instances for every single monad transformer in the stack.
This means that when you stack `EitherT`, `ReaderT` and `StateT`, you'll be able to get instances for `MonadError`, `ApplicativeAsk` and `MonadState`, which is really useful!

If you're wondering how this works, well let's just have a quick look at how the `MonadError` instance for `ReaderT`

```scala
def monadErrorForReaderT[F[_], E, R](implicit F: MonadError[F, E]): MonadError[ReaderT[F, R, ?], E] =
  new MonadError[ReaderT[F, R, ?], E] {
    def raiseError[A](e: E): ReaderT[F, R, A] =
      ReaderT.liftF(F.raiseError(e))

    def handleErrorWith[A](fa: ReaderT[F, R, A])(f: E => ReaderT[F, R, A]): ReaderT[F, R, A] =
      ReaderT.ask[F, R].flatMap { r => 
        ReaderT.liftF(fa.run(r).handleErrorWith(e => f(e).run(r)))
      }
  }
```

To get an instance of `MonadError` for `ReaderT[F, R, ?]`, we need to have a `MonadError` for `F`.
Then we can easily use that underlying instance to handle and raise the errors instead.
Again, this means that if some part of transformer stack is capable of raising and handling errors, now your whole stack is.
So if it includes `EitherT` somewhere, you can "lift" that capability. 

There are different strategies for lifting these capabilities throughout your monad stack, but they'd be out of scope for this article.

What this means for us, is that we never have to think about lifting individual monads through transformer stacks.
The implicit search used by the type class mechanic takes care of it.
Pretty neat, I think.
Now contrast this lack of lifting, with the same program written without mtl:

```scala
type EitherApp[A] = EitherT[IO, AppError, A]
// defined type alias EitherApp

type Stack[A] = ReaderT[EitherApp, Config, A]
// defined type alias Stack

def program: Stack[Result] = for {
  config <- ReaderT.ask[EitherApp, Config]
  _ <- if (validConfig(config)) ().pure[Stack]
       else ReaderT.liftF[EitherApp, Config, Unit](EitherT.leftT(InvalidConfig))
  result <- ReaderT.liftF(EitherT.liftF[IO, AppError, Result](serviceCall(config)))
} yield result
// program: Stack[Result]
```

It's the same program, but now we have to add type annotations and `liftF`s everywhere.
If you try to take away one of those type annotations the program will fail to compile, so this is the minimum amount of boilerplate you need.


### Adding State

For the next step, let's imagine we want to send multiple requests and after each, use information we retrieved from the response for the next request, similar to how we did earlier in the `StateT` example.

Instead of using `StateT`, we'll use the `MonadState` type class:

```scala
trait MonadState[F[_], S] {
  val monad: Monad[F]

  def get: F[S]

  def set(s: S): F[Unit]

  def modify(f: S => S): F[Unit] = get.flatMap(s => set(f(s)))
}
```

Let's imagine we have a list of requests, where we want to update the environment after each request, and we also want to use the environment to create the next request.
At the very end we want to return the list of all the responses we got:

```scala
type Result = List[Response]

def updateEnv(r: Response, env: Env): Env = ???

def requests: List[Request] = ???

def newServiceCall(c: Config, req: Request, e: Env): IO[Response] = ???
```

So far, so good, next we'll use `MonadState` to create a new function that will wrap `newServiceCall` with the addition of modifying the environment using `updateEnv`.
To do so, we'll create a new type alias for `MonadState[F, Env]`:

```scala
type MonadStateEnv[F[_]] = MonadState[F, Env]
// defined type alias MonadStateEnv

def requestWithState[F[_]: Monad: MonadStateEnv: LiftIO](c: Config, req: Request): F[Response] = for {
  env <- MonadState[F, Env].get
  response <- newServiceCall(c, req, env).to[F]
  _ <- MonadState[F, Env].modify(updateEnv(response, _))
} yield response
// requestWithState: [F[_]](c: Config, req: Request)(implicit evidence$1: cats.Monad[F], implicit evidence$2: MonadStateEnv[F], implicit evidence$3: cats.effect.LiftIO[F])F[Response]
```

Here, we use `get` to retrieve the current state of the environment, then we use `newServiceCall` and lift it into `F` and use the response to modify the environment with `updateEnv`.

Now, we can use  `requestWithState` on our list of requests and embed this new part into our program.
The best way to do that, is of course `traverse`, as we want to go from a `List[Request]` and a function `Request => F[Response]` to an `F[List[Response]]`.
So without further ado, this is our final program, using all three different mtl type classes we learned about in this article: 

```scala
def program[F[_]: MonadAppError: MonadStateEnv: ApplicativeConfig: LiftIO]: F[Result] = for {
  config <- ApplicativeAsk[F, Config].ask
    .ensure(InvalidConfig)(validConfig)
  responses <- requests.traverse(req => requestWithState[F](config, req))
} yield responses
// program: [F[_]](implicit evidence$1: MonadAppError[F], implicit evidence$2: MonadStateEnv[F], implicit evidence$3: ApplicativeConfig[F], implicit evidence$4: cats.effect.LiftIO[F])F[Result]
```

And that is it! 
Of course, we still have to run it, so let's materialize our `F` into an appropriate data type.
We'll be using a stack of `EitherT`, `StateT` and `ReaderT`, with `IO` as our base to satisfy `LiftIO`:

```scala
def materializedProgram = program[StateT[EitherT[ReaderT[IO, Config, ?], AppError, ?], Env, ?]]
```

And now we have a fully applied transformer stack.

The only thing left is to turn that stack back into an `IO` by running the individual layers.

```scala
def main: IO[Either[AppError, (Env, Result)]] = 
  getConfig.flatMap(conf => 
    materializedProgram.run(initialEnv) //Run the StateT layer
      .value //Run the EitherT layer
      .run(conf) //Run the ReaderT layer
  ) 
```

If we were to get that same value using just transformers and no mtl, the amount of boilerplate would be excruciating. We would need multiple `liftF`s for every monad transformer and dozens of type annotations, leaving the actual code hidden under layers and layers of boilerplate.

With Cats-mtl, dealing with different effects is simple and free of boilerplate.
We can describe our application as functions dealing with an abstract context `F[_]` that must be able to provide certain effect constraints.
These constraints are provided by the different MTL type classes in Cats-mtl and their instances can be lifted up to the highest layer with Cats-mtl's underlying machinery.

In summary Cats-mtl provides two things:
MTL type classes representing effects and a way to lift instances of these classes through transformer stacks.
If you'd like to learn more about Cats-mtl, [check out its new website!](https://typelevel.org/cats-mtl/)

### Other mtl class instances

Now I said that `ApplicativeAsk` is the type class encoding of `ReaderT`, but it's by no means the only one that can form an `ApplicativeAsk` instance.
Monad transformer stacks are known to be quite unperformant, especially so on the JVM, so there are some alternate solutions. For example, one could use [the Arrows library](https://github.com/traneio/arrows), which provides effect types with an input type in addition to its output type `Arrow[A, B]`. If you squint a bit, it's practically equivalent to a function `A => IO[B]` or `ReaderT[IO, A, B]`. At the same time, however, it can be substantially more performant.

Other examples include using something like `cats-effect`' `Ref` for `MonadState` (a working instance [can be found here](https://github.com/oleg-py/meow-mtl)), or using a bifunctor `IO` that includes an extra type parameter for the error type, i.e. `BIO[E, A]` instead of using `EitherT[IO, E, A]` (a WIP for cats-effect [can be found here](https://github.com/LukaJCB/cats-bio)).

In general, we can think up more performant solutions to our effect type class instances by using more specialized data structures. Monad Transformers are extremely general, which makes them very flexible, but that flexibility may come at a price. One of the great things about `mtl` is that we don't have to choose up front, but only at the very end when our program is run.
For example, we might choose to use only monad transformers at the begining when developing our application. Then, when we want to scale up, we can move to more performant instances simply by changing a few lines when materializing our programs.

In the long term, I'd like to provide a submodule of `cats-mtl` that has very specialized and performant data types for every combination of effect type classes.
For this purpose, I've created [the cats-mtl-special library](https://github.com/LukaJCB/cats-mtl-special) some time ago, but it still remains very much a work in progress.
Shoutout also to Jamie Pullar who has been using cats-mtl extensively in production and has also built some more performant instances along with some benchmarks which you can find [as part of his talk here](https://www.slideshare.net/RyanAdams12/jamie-pullar-cats-mtl-in-action/39).
