---
layout: post
title: Machinist vs. value classes

meta:
  nav: blog
  author: non
  pygments: true
---

This article is about [machinist](https://github.com/typelevel/machinist), a stand-alone project which started out as part of the [spire](https://github.com/non/spire) project and has been originally published in [October 2014](https://gist.github.com/non/a6ff3c0796e566db20d1).
The original description can be found on [this blog]({% post_url 2013-10-13-spires-ops-macros %}).
You should read that linked post first if you are not familiar with how Machinist works.


Introduction
------------


[Machinist Issue #2](https://github.com/typelevel/machinist/issues/2) asks:

> Is it correct, that this stuff is completely obsolete now due to
> value classes or are there still some use cases? An example of using
> value class for zero-cost implicit enrichment: [...]

The short answer is that value classes existed before the Machinist macros were implemented, and they do not solve the same problem Machinist solves.

This article is the long answer.


The base case
-------------

The Machinist's goal is to remove *any* overhead that would distinguish
using a type class "directly" from using it indirectly via an implicit
operator.

Imagine we have the following "toy" type class:

```scala
trait Div[A] {
  def div(lhs: A, rhs: A): A
}

object Div {
  implicit val DivString = new Div[String] {
    def div(lhs: String, rhs: String): String = lhs + "/" + rhs
  }
}
```

This allows us to write generic code such as:

```scala
class Test1 {
  def gen[A](x: A, y: A)(implicit ev: Div[A]): A = ev.div(x, y)
  def test: String = gen("foo", "bar")
}
```

We have a generic method `gen` that works with any type we have a `Div[A]` instance for, and we verify that it works using a `test` method that operates on some strings. So far, so good. But obviously, calling `ev.div` is a bit ugly.

Implicit conversion with a value class
--------------------------------------

We can make the `gen` method look a bit nicer by using an implicit conversion. Here's the code:

```scala
object Test3 {
  implicit class DivOps[A](val lhs: A) extends AnyVal {
    def /(rhs: A)(implicit ev: Div[A]): A = ev.div(lhs, rhs)
  }
}

class Test3 {
  import Test3.DivOps
  def gen[A: Div](x: A, y: A): A = x / y
  def test: String = gen("foo", "bar")
}
```

Now, we can just say `x / y` and have that call `Div#div` automatically. We also don't need a reference to `ev: Div[A]` so we can use the nicer `[A: Div]` syntax. 

With a normal implicit conversion, every call to `gen` would construct an instance of `Test3.DivOps`. However, since we have defined `Test3.DivOps` as a value class (by extending `AnyVal`), the object instantiation is ellided. Instead, the method call is dispatched to `Test3.DivOps.$div$extension` which calls `ev.div`.

We often talk about value classes as not having a *cost*. Since no class is instantiated, we are not required to pay a cost in allocations, but we do still pay a cost in indirection (instead of calling `ev.div` directly as in `Test1` we have an intermediate extension method).

You can see the difference in the output from `javap`.

In the case of `Test1.gen`, the call to `ev.div` and return are all handled with 5 instructions (8 bytes of bytecode):

```
// cost.Test1.gen(A, A, Div[A]): A
0: aload_3
1: aload_1
2: aload_2
3: invokeinterface #16,  3           // InterfaceMethod cost/Div.div:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
8: areturn
```

In the case of `Test3.gen`, there is extra ceremony setting up the companion objects, and a call to the extension method (`$div$extension`), which is defined in `Test3.DivOps`:

```
// cost.Test3.gen(A, A, Div[A]): A
0: getstatic     #25                 // Field cost/Test3$DivOps$.MODULE$:Lcost/Test3$DivOps$;
3: getstatic     #16                 // Field cost/Test3$.MODULE$:Lcost/Test3$;
6: aload_1
7: invokevirtual #18                 // Method cost/Test3$.DivOps:(Ljava/lang/Object;)Ljava/lang/Object;
10: aload_2
11: aload_3
12: invokevirtual #28                 // Method cost/Test3$DivOps$.$div$extension:(Ljava/lang/Object;Ljava/lang/Object;Lcost/Div;)Ljava/lang/Object;
15: areturn

// cost.Test3.DivOps.$div$extension(A, A, Div[A]): A
0: aload_3
1: aload_1
2: aload_2
3: invokeinterface #20,  3           // InterfaceMethod cost/Div.div:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
8: areturn
```

In fact the bytecode for the extension method is uncannily similar to that of `Test1.gen`, but in this case `Test3.gen` involves 8 more instructions (15 bytes).

In some cases these bytecode differences might not be significant (for example if the running time of `Div[A].div` is expected to dwarf the cost of method dispatch). However, when type classes are used to support primitive operations (such as addition or comparisons) it's likely that this overhead might be significant.

Enter machinist
---------------

Machinist is based on a set of macros that were introduced in [Spire](https://github.com/non/spire) to remove the performance penalties associated with generic math implementations. These macros were based on an even earlier approach which used a compiler plugin.

The basic approach has not changed: at compile-time we can detect situations where we build an object just to assemble a method call with the arguments to its constructor. In these cases we rewrite the tree, removing the object allocation and making the method call directly. Machinist's documentation goes to some trouble to explain it, but basically, we want to be able to write code like `Test3.gen` but have it interpreted as `Test1.gen`. That is literally the entire purpose of machinist.

Here's a construction that works for this example:

```scala
object Test2 {
  implicit class DivOps[A](lhs: A)(implicit ev: Div[A]) {
    def /(rhs: A): A = macro machinist.DefaultOps.binop[A, A]
  }
}

class Test2 {
  import Test2.DivOps
  def gen[A: Div](x: A, y: A): A = x / y
  def test: String = gen("foo", "bar")
}
```

We use the `machinist.DefaultOps` object to provide an instance of the `binop` macros, which will rewrite `DivOps(x)(ev).$div(y)` into `ev.div(x, y)`.

Here's what we end up with in bytecode:

```
// cost.Test2.gen(A, A, Div[A]): A
0: aload_3
1: aload_1
2: aload_2
3: invokeinterface #26,  3           // InterfaceMethod cost/Div.div:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
8: areturn
```

As you can see, the sourcecode for `Test2.gen` is identical to `Test3.gen`, and the bytecode for `Test2.gen` is identical to that of `Test1.gen`. Success!

Caveats
-------

There are a few caveats that are worth mentioning:

### Managing compilation units

The issue that sparked this article used the operator `+/+`. Machinist claims to be able to support any symbolic operator. Why didn't we use that operator here?

The answer has to do with how Scala macros work. Scala requires that macros be defined in a separate "compilation unit" from the one they are invoked in. This makes it very awkward to create a code snippet that both defines and uses a macro. In this case, it means that we can't extend `machinist.Ops` to define new symbolic operators in the same file that demonstrates their use. This is why we used `/` (which maps to `div` and is a "default operator").

You can arrange your "real" projects so that they are not affected by this limitation.

### Use outside of generic methods

Now that we've demonstrated the cost that implicit conversions to value classes impose, you might imagine wanting to perform this transformation on *all* your implicit conversions.

Unfortunately, Machinist is not sufficiently general to support this. Right now its macros support a number of different "shapes" but assume generic method which dispatches to an implicit evidence parameter. It might be possible to write macros which inline the method body of a concrete implicit class, but that's outside the scope of the project. 


Postscript: messy details
-------------------------

This article throws around a lot of source code and bytecode.  Below are included the files needed to build the demo (`cost.scala` and `build.sbt`) as well as the `javap` output from the three test classes, and the value class.

### cost.scala

```scala
package cost

import language.implicitConversions
import scala.language.experimental.macros

trait Div[A] {
  def div(lhs: A, rhs: A): A
}

object Div {
  implicit val DivString = new Div[String] {
    def div(lhs: String, rhs: String): String = lhs + "/" + rhs
  }
}

class Test1 {
  def gen[A](x: A, y: A)(implicit ev: Div[A]): A = ev.div(x, y)
  def test: String = gen("foo", "bar")
}

object Test2 {
  implicit class DivOps[A](lhs: A)(implicit ev: Div[A]) {
    def /(rhs: A): A = macro machinist.DefaultOps.binop[A, A]
  }
}

class Test2 {
  import Test2.DivOps
  def gen[A: Div](x: A, y: A): A = x / y
  def test: String = gen("foo", "bar")
}

object Test3 {
  implicit class DivOps[A](val lhs: A) extends AnyVal {
    def /(rhs: A)(implicit ev: Div[A]): A = ev.div(lhs, rhs)
  }
}

class Test3 {
  import Test3.DivOps
  def gen[A: Div](x: A, y: A): A = x / y
  def test: String = gen("foo", "bar")
}
```

### build.sbt

```scala
name := "cost"

scalaVersion := "2.11.2"

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

libraryDependencies += "org.typelevel" %% "machinist" % "0.2.2"
```

### Test1.out

```
Compiled from "cost.scala"
public class cost.Test1 {
  public <A extends java/lang/Object> A gen(A, A, cost.Div<A>);
    Code:
       0: aload_3       
       1: aload_1       
       2: aload_2       
       3: invokeinterface #16,  3           // InterfaceMethod cost/Div.div:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
       8: areturn       

  public java.lang.String test();
    Code:
       0: aload_0       
       1: ldc           #27                 // String foo
       3: ldc           #29                 // String bar
       5: getstatic     #35                 // Field cost/Div$.MODULE$:Lcost/Div$;
       8: invokevirtual #39                 // Method cost/Div$.DivString:()Lcost/Div;
      11: invokevirtual #41                 // Method gen:(Ljava/lang/Object;Ljava/lang/Object;Lcost/Div;)Ljava/lang/Object;
      14: checkcast     #43                 // class java/lang/String
      17: areturn       

  public cost.Test1();
    Code:
       0: aload_0       
       1: invokespecial #47                 // Method java/lang/Object."<init>":()V
       4: return        
}
```

### Test2.out

```
Compiled from "cost.scala"
public class cost.Test2 {
  public static <A extends java/lang/Object> cost.Test2$DivOps<A> DivOps(A, cost.Div<A>);
    Code:
       0: getstatic     #16                 // Field cost/Test2$.MODULE$:Lcost/Test2$;
       3: aload_0       
       4: aload_1       
       5: invokevirtual #18                 // Method cost/Test2$.DivOps:(Ljava/lang/Object;Lcost/Div;)Lcost/Test2$DivOps;
       8: areturn       

  public <A extends java/lang/Object> A gen(A, A, cost.Div<A>);
    Code:
       0: aload_3       
       1: aload_1       
       2: aload_2       
       3: invokeinterface #26,  3           // InterfaceMethod cost/Div.div:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
       8: areturn       

  public java.lang.String test();
    Code:
       0: aload_0       
       1: ldc           #37                 // String foo
       3: ldc           #39                 // String bar
       5: getstatic     #44                 // Field cost/Div$.MODULE$:Lcost/Div$;
       8: invokevirtual #48                 // Method cost/Div$.DivString:()Lcost/Div;
      11: invokevirtual #50                 // Method gen:(Ljava/lang/Object;Ljava/lang/Object;Lcost/Div;)Ljava/lang/Object;
      14: checkcast     #52                 // class java/lang/String
      17: areturn       

  public cost.Test2();
    Code:
       0: aload_0       
       1: invokespecial #56                 // Method java/lang/Object."<init>":()V
       4: return        
}
```

### Test3.out

```
Compiled from "cost.scala"
public class cost.Test3 {
  public static java.lang.Object DivOps(java.lang.Object);
    Code:
       0: getstatic     #16                 // Field cost/Test3$.MODULE$:Lcost/Test3$;
       3: aload_0       
       4: invokevirtual #18                 // Method cost/Test3$.DivOps:(Ljava/lang/Object;)Ljava/lang/Object;
       7: areturn       

  public <A extends java/lang/Object> A gen(A, A, cost.Div<A>);
    Code:
       0: getstatic     #25                 // Field cost/Test3$DivOps$.MODULE$:Lcost/Test3$DivOps$;
       3: getstatic     #16                 // Field cost/Test3$.MODULE$:Lcost/Test3$;
       6: aload_1       
       7: invokevirtual #18                 // Method cost/Test3$.DivOps:(Ljava/lang/Object;)Ljava/lang/Object;
      10: aload_2       
      11: aload_3       
      12: invokevirtual #28                 // Method cost/Test3$DivOps$.$div$extension:(Ljava/lang/Object;Ljava/lang/Object;Lcost/Div;)Ljava/lang/Object;
      15: areturn       

  public java.lang.String test();
    Code:
       0: aload_0       
       1: ldc           #39                 // String foo
       3: ldc           #41                 // String bar
       5: getstatic     #46                 // Field cost/Div$.MODULE$:Lcost/Div$;
       8: invokevirtual #50                 // Method cost/Div$.DivString:()Lcost/Div;
      11: invokevirtual #52                 // Method gen:(Ljava/lang/Object;Ljava/lang/Object;Lcost/Div;)Ljava/lang/Object;
      14: checkcast     #54                 // class java/lang/String
      17: areturn       

  public cost.Test3();
    Code:
       0: aload_0       
       1: invokespecial #58                 // Method java/lang/Object."<init>":()V
       4: return        
}
```

### Test3.DivOps.out

```
Compiled from "cost.scala"
public class cost.Test3$DivOps$ {
  public static final cost.Test3$DivOps$ MODULE$;

  public static {};
    Code:
       0: new           #2                  // class cost/Test3$DivOps$
       3: invokespecial #12                 // Method "<init>":()V
       6: return        

  public final <A extends java/lang/Object> A $div$extension(A, A, cost.Div<A>);
    Code:
       0: aload_3       
       1: aload_1       
       2: aload_2       
       3: invokeinterface #20,  3           // InterfaceMethod cost/Div.div:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
       8: areturn       

  public final <A extends java/lang/Object> int hashCode$extension(A);
    Code:
       0: aload_1       
       1: invokevirtual #32                 // Method java/lang/Object.hashCode:()I
       4: ireturn       

  public final <A extends java/lang/Object> boolean equals$extension(A, java.lang.Object);
    Code:
       0: aload_2       
       1: astore_3      
       2: aload_3       
       3: instanceof    #36                 // class cost/Test3$DivOps
       6: ifeq          15
       9: iconst_1      
      10: istore        4
      12: goto          18
      15: iconst_0      
      16: istore        4
      18: iload         4
      20: ifeq          61
      23: aload_2       
      24: ifnonnull     31
      27: aconst_null   
      28: goto          38
      31: aload_2       
      32: checkcast     #36                 // class cost/Test3$DivOps
      35: invokevirtual #40                 // Method cost/Test3$DivOps.lhs:()Ljava/lang/Object;
      38: astore        5
      40: aload_1       
      41: aload         5
      43: invokestatic  #45                 // Method scala/runtime/BoxesRunTime.equals:(Ljava/lang/Object;Ljava/lang/Object;)Z
      46: ifeq          53
      49: iconst_1      
      50: goto          54
      53: iconst_0      
      54: ifeq          61
      57: iconst_1      
      58: goto          62
      61: iconst_0      
      62: ireturn       

  public cost.Test3$DivOps$();
    Code:
       0: aload_0       
       1: invokespecial #47                 // Method java/lang/Object."<init>":()V
       4: aload_0       
       5: putstatic     #49                 // Field MODULE$:Lcost/Test3$DivOps$;
       8: return        
}
```
