---
layout: post
title: Testing in the wild

meta:
  nav: blog
  author: etorreborre
  pygments: true

---

 Writing tests seems like a wonderful idea in theory but real systems can be a real pain to test. Today I want to show a few tips on how to
 use [specs2](http://specs2.org) + [ScalaCheck](http://www.scalacheck.org) to make some real-world testing somewhat bearable.

 I am currently refactoring a big piece of code. Such refactoring is more like a small rewrite and some of our previous tests also have to be
 rewritten from scratch. I will try to introduce you to the problem first.

### Creating articles

 The system-du-jour is called "Panda" (we have animal names for many of our services in my team) and is tasked with the creation of articles
 on our legacy platform. An article is already a complicated beast. We have 3 levels of descriptions:

  - Model: the description for a pair of Adidas shoes, with the brand, the gender it applies to, the size chart it uses and so on
  - Config: the description for a specific combination of colours - black adidas -, images, material,... There can be several configs per model
  - Simple: the description of a specific size - 37, 38, 39 -, price, stock, EAN (European Article Number),... There can be several simples
  per config

 This data can be created in our legacy catalog by calling a bunch of SOAP (yes you read that right) APIs and getting back, at each level,
  some identifiers for the model, the configs, the simples.

 This in itself can already be quite complicated and the creation of an article could fail in many ways. But it gets a lot more complex
  considering that:

   1. we need the service to be idempotent and not try to recreate an existing model/config/simple twice if we receive the same event twice
      (we are using Kafka as our events system)

   2. the articles sent by a merchant can be created incrementally, so some parts of the model/config/simple might have been already created
      in a previous call

   3. we are not the only one creating articles in the system! Indeed, our team creates articles coming from external merchants but there is
      also an internal "wholesale" department buying their own articles and creating them in the catalog. In that case a merchant might add
      a new config to an existing model or some simples to an existing config

   4. any step in the process could break and we have no support for transactions making sure that everything is created at once

 So many things which can go wrong, how would you go about testing it?

### Combinations, the key to testing

 After I started rewriting the tests I realized that our current approach was barely scratching the surface of all the possible combinations.
 In a similar case your first thought should be "ScalaCheck"! But this time I am going to use ScalaCheck with a twist. Instead of only modelling
 input data (model/config/simples) I am also modelling the system state:

  - we have a "mapping table" to store merchant articles that have already been created. In which states can it be?
  - the legacy catalog can also be in many different states: does a particular model/config/simple already exist or not?

 If we translate this into a specs2 + ScalaCheck specification, we get a property like this:

```scala
class ArticleServiceSpec extends Specification with ScalaCheck { def is = s2"""

  <insert long description of the problem and what we want to test>

  run tests for model creation $modelCreation

"""

  def modelCreation = prop { (reviewed: Article, catalog: TestCatalog, mappings: TestMappings) =>

    ok // for now


  }.setGen1(genArticleOneConfig)
}
```

 We are creating a ScalaCheck property, with a specs2 method `prop` which gives us some additional power over row ScalaCheck properties.
 One thing we do here is to restrict the kind of generated `Article` to articles containing only one new configuration because we want to
  focus first on all the possible cases of model creation. So we pass a specific generator to the property, just for the first argument
  with `setGen1`.

 Then, as you can see above, we return `ok` which is a specs2 result. This is because `prop` allows us to return anything that specs2 recognizes
  as a `Result` (with the `org.specs2.execute.AsResult` typeclass) and then we are not limited to booleans in our ScalaCheck properties but
  we can use specs2 matchers as well (we are going use this in the next step).

 Now, for testing we need to do the following:

   1. capture the state before the article creation
   2. execute the article creation
   3. capture the state after the article creation
   4. compare the resulting state to expected values

```scala
  val before = createBeforeState(reviewed, catalog, mappings)
  val result = run(createService(catalog, mappings).createArticles[R](reviewed))
  val after  = createAfterState(reviewed, catalog, mappings, result)
```

 What are `BeforeState` and `AfterState`? They are custom case classes modelling the variables we are interested in:
```scala
 case class BeforeState(
   modelIdProvided:       Boolean,
   modelNeedsToBeCreated: Boolean,
   modelExistsInCatalog:  Boolean,
   mappingExists:         Boolean)

 case class AfterState(
   modelExistsInCatalog: Boolean,
   mappingExists:        Boolean,
   exception:            Option[Throwable])
```

 The first 2 variables of `BeforeState` are a bit curious. The first one gives us a `ModelId` if upstream systems know that a model already
  exist. Then how could `modelNeedsToBeCreated` be true? Well, the events we receive don't rule out this possibility. This is the
  current state of our domain data and arguably we should model things differently and reject malformed events right away. This is where the
  saying ["Listen to your tests"](https://www.obeythetestinggoat.com/book/chapter_purist_unit_tests.html#_listen_to_your_tests_ugly_tests_signal_a_need_to_refactor) comes in :-).

 If we count the number of combinations we end up with 16 possibilities for our "before state" and 8 possible outcomes. How can we represent
   all those combinations in our test?

### DataTables

 Specs2 offers to possibility to create tables of data directly inside the code for better readability of actual and expected values
 when you have lots of different possible combinations. Here is what we can do here

```scala
  val results =
  "#" | "model-id" | "create" | "in catalog" | "mapping" || "in catalog"  | "mapping" | "exception" | "comment" |
  1   !  true      ! true     ! true         ! true      !! true          ! true      ! false       ! "no model is created, because it can be found in the catalog, creation data is ignored" |
  2   !  true      ! true     ! true         ! false     !! true          ! true      ! false       ! "we just updated the mapping" |
  3   !  true      ! true     ! false        ! true      !! false         ! true      ! true        ! "the config creation must fail, no existing model" |
  4   !  true      ! true     ! false        ! false     !! true          ! true      ! false       ! "the given model-id is ignored (a warning is logged)" |
  5   !  true      ! false    ! true         ! true      !! true          ! true      ! false       ! "no model is created, because it can be found in the catalog" |
  6   !  true      ! false    ! true         ! false     !! true          ! false     ! false       ! "the mappings are not updated because we did not create the model" |
  7   !  true      ! false    ! false        ! true      !! false         ! true      ! true        ! "no corresponding model in the catalog" |
  8   !  true      ! false    ! false        ! false     !! false         ! false     ! true        ! "no corresponding model in the catalog" |
  9   !  false     ! true     ! true         ! true      !! true          ! true      ! false       ! "we use the mapping table to retrieve the model id and the catalog for the model" |
  10  !  false     ! true     ! true         ! false     !! true          ! true      ! false       ! "in this case the model already exists in the catalag but we have no way to know" |
  11  !  false     ! true     ! false        ! true      !! false         ! true      ! true        ! "the mapping exists but not the data in the catalog" |
  12  !  false     ! true     ! false        ! false     !! true          ! true      ! false       ! "regular model + config creation case" |
  13  !  false     ! false    ! true         ! true      !! true          ! true      ! true        ! "there is no model id and no creation data" |
  14  !  false     ! false    ! true         ! false     !! true          ! false     ! true        ! "the model exists in the catalog but we have no way to retrieve it" |
  15  !  false     ! false    ! false        ! true      !! false         ! true      ! true        ! "model id found in the mapping but not in the catalog" |
  16  !  false     ! false    ! false        ! false     !! false         ! false     ! true        ! "not enough data to create the model nor the mapping"

 checkState(before, after, parseTable(results))
```
 This looks like a strange piece of code but this is actually all valid Scala syntax! `results` is a specs2 `DataTable` created out of:

  - a header where column names are separated with `|`
  - rows that are also separated with `|`
  - cells on each row, separated with `!`

We can also use `||` and `!!` as separators and we use this possibility here to visually distinguish input columns from expected results
  columns.

### Running the tests

The table above is like a big "truth table" for all our input conditions. Running a test consists in:

  1. using the 'before state' to locate one of the row
  2. getting the expected 'after state' from the expected columns
  3. comparing the actual 'after state' with the expected one

The funny thing is that before executing the test I did not exactly know what the code would actually do! So I just let the test guide me.
I put some expected values, run the test and in case of a failure, inspect the input values, think hard about why the code is not behaving the
way I think it should.

One question comes to mind: since this is a ScalaCheck property, how can we be sure we hit all the cases in the table? The first thing we
can do is to massively increase the number of tests that are going to be executed for this property, like 10000. With specs2 you have many
ways to do this. You can set the `minTestsOk` ScalaCheck property directly in the code:
```scala
def modelCreation = prop { (reviewed: Article, catalog: TestCatalog, mappings: TestMappings) =>
 ...
}.setGen1(genArticleOneConfig).set(minTestsOk = 10000)
```

But you can also do it from sbt:
```
sbt> testOnly *ArticleServiceSpec -- scalacheck.mintestsok 10000
```

This is quite cool because this means that you don't have to recompile the code if you just want to run a ScalaCheck property with more tests.

### Checking the results

As I wrote, when a specific combination would fail I had to inspect the inputs/outputs and think hard, maybe my expectations are wrong and
I needed to change the expected values? To this end I added a "line number" column to the table and reported it in the result:

```
 [error]  > On line 6
 [error]
 [error]  Before
 [error]    model id set:             true
 [error]    model creation data set:  false
 [error]    model exists in catalog : true
 [error]    model id mapping exists:  false
 [error]
 [error]  After
 [error]    model exists in catalog:  true
 [error]      expected:               true
 [error]
 [error]    model id mapping exists:  false
 [error]      expected:               false
 [error]
 [error]    exception thrown:         None
 [error]      expected:               Some
```

 This reporting is all done in the `checkState` method which is:

 - doing the comparison between actual and expected values
 - displaying the before / after states
 - displaying the difference between expected and actual values

 Actually I even enhanced the display of actual/expected values by coloring them in green or red in the console, using one of specs2 helper
 classes `org.specs2.text.AnsiColors`:
```scala
import org.specs2.text.AnsiColors

def withColor[A](actual: A, expected: A, condition: (A, A) => Boolean = (a:A, e:A) => a == e): String =
  // color the expected value in green or red, depending on the test success
  color(expected.toString, if (condition(actual, expected)) green else red)

withColor(after.modelExistsInCatalog, expected.modelExistsInCatalog)
```

Both the line numbering and the coloring really helps in fixing issues fast!

### Replaying tests

A vexing issue with property-based testing is that being random, it will generate random failures every time you re-run a property. So you
can't re-run a property with the exact same input data. But that was before ScalaCheck 1.14! Now we can pass the seed that is used by the random
 generator to faithfully re-run a failing test. Indeed when a property fails, specs2 will display the current seed value:
```
[error]  The seed is 1tRQ5-jdfEABEXz1y62Cs0C4vNJQKyXps9eWvbjJPSI=
```

And you can pass this value on the command line to re-run with exactly the failing input data:
```
sbt> testOnly *ArticleServiceSpec -- scalacheck.seed 1tRQ5-jdfEABEXz1y62Cs0C4vNJQKyXps9eWvbjJPSI=
```

This is super-convenient for debugging!

### Comments

Finally when a given row in the table passes, there is a `comment` column to register the reason for this specific outcomes so that future
generations have a sense of *why* the code is behaving that way. In that sense this whole approach is a bit like having "golden tests" which
are capturing the behaviour of the system as a series of examples

### Conclusion

This post shows how we can leverage features from both specs2 and ScalaCheck to make our tests more exhaustive, more readable, more debuggable.
The reality is still more complicated than this:

 - the total number of combinations would make our table very large. So there are actually several tables (one for model creation, one for
 config creation,...) where we assume that some variables are fixed while others can move

 - specs2 datatables are currently limited to 10 columns. The `DataTable` code is actually code generated and the latest version only has 10
 columns. One easy first step would be to generate more code (and go up to the magic 22 number for example) or to re-implement this functionality
 as some kind of HList

 - the input state is not trivial to generate because the objects are dependent. The `ModelId` of a generated model must be exactly the same
 as the one used in the `Mappings` component to register that a model has already been created. So in reality the 2 generators for `Article` and
 `Mappings` are not totally independent

 - the `Arbitrary` instance for `Article` can give us articles with 5 `Configs` and 10 `Simples` but for this test, one `Config` and one `Simple`
  are enough. Unfortunately we miss a nice language to express those generation condition and easily tweak the default `Arbitrary[Article]` (I
  will explore a solution to this problem during the next Haskell eXchange)

 - why are we even using ScalaCheck to generate all the cases since we already statically know all the possible 16 input conditions? We could
  invert this relation and have a ScalaCheck property generated for each row of the datatable with some arbitrary data for the model (and some
   fixed data given by the current row). This would not necessarily lead to easier code to implement.

Anyway despite those remaining questions and issues I hope this post gives you some new ideas on how to be more effective when writing tests
  with specs2 and ScalaCheck, please comment on your own experiments!
