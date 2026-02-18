{%
  author: ${S11001001}
  date: "2017-09-05"
  tags: [technical]
%}

# There are at least three types of strings

[Newtype mechanisms](https://contributors.scala-lang.org/t/pre-sip-unboxed-wrapper-types/987)
are a great way to introduce wrapper-free, global distinctions of
“different” instances of the same type. But we can do that on a local
level, too,
[by using type parameters](https://gist.github.com/jbgi/d6b677d084fafc641fe01f7ffd00591c/70842ca600e53e8c237c681773fe4e16bd679628#file-label-java-L32).

Consider these two signatures.

```scala
def mungeIDs(uids: List[String], gids: List[String],
             oids: List[String]): Magic[String, String, String]

def mungeIDsSafely[UID <: String, GID <: String, OID <: String]
            (uids: List[UID], gids: List[GID],
             oids: List[OID]): Magic[UID, GID, OID] 
```

The second function is a strictly more general interface; the first,
concrete signature can be implemented by calling the second function,
passing `[String, String, String]` as the type arguments. There is no
need to even have the first signature; anywhere in your program where
you pass three `List[String]`s as arguments to `mungeIDsSafely`, the
proper type arguments will be inferred.

Yet, assuming you don’t wish `mungeIDs` to be oracular (i.e. a source
of UIDs, GIDs, and OIDs), the second signature is probably much more
reliable, because type parameters are quite as
[mysterious](more-types-than-classes.md#it-must-not-necessarily-be-anything)
as the opaque abstract type members of the newtype mechanism.

1. `mungeIDsSafely` can’t invent new IDs, not even with `null`.
2. It can’t combine them to produce new IDs.
3. It *can* treat the three list arguments as `List[String]`. However,
   it cannot convert any `String` back into an ID; any UIDs, GIDs, or
   OIDs that appear in the result `Magic[UID, GID, OID]` must have
   come from one of the argument lists, directly. (That’s not to say
   that `mungeIDsSafely` can’t *use* the string-nature to make that
   decision; for example, it could always choose the
   smallest-as-string UID to put into the resulting `Magic`. But, that
   UID is *still* enforced to be a proper element of the `uids`
   argument, and cannot be gotten from anywhere else.
4. Perhaps most importantly, it cannot mix up UIDs, GIDs, and
   OIDs. Even though, “really”, they’re all strings!

It is entirely irrelevant that you cannot subclass `String` in Scala,
Java, or whatever.
[There are more types than classes](more-types-than-classes.md).

Given the advantages, it’s very unfortunate that the signature of
`mungeIDsSafely` is so much noisier than that of `mungeIDs`.  At least
you have the small consolation of eliminating more useless unit tests.

This is a good first approximation at moving away from the dangers of
concreteness in Scala, and has the advantage of working in Java, too
(sort of; the `null` prohibition is sadly relaxed).

## Non-supertype constraints

In Scala, you can also use implicits to devise arbitrary constraints,
similar to typeclasses in Haskell, and sign your functions using
implicits instead, for much finer-grained control, improved safety,
and types-as-documentation.

```scala
// a typeclass for "IDish types" (imagine instances)
sealed trait IDish[A]

def mungeIDsTCey[UID: IDish, GID: IDish, OID: IDish]
            (uids: List[UID], gids: List[GID],
             oids: List[OID]): Magic[UID, GID, OID] 
```

Though all three types have the same constraint, `IDish`, they are
still distinct types.  And now, the coupling with `String` is broken;
as the program author, you get to decide whether you want that or not.

## Pitfalls avoided for you

Luckily, Java doesn’t make the mistake of “reified generics”. If it
did, you could ask whether `UID = GID = OID = String`, and all your
safety guarantees would be gone. Forcing all generics to be reified
does not grant you any new expressive power; all it does is
permanently close off large swaths of the spectrum of mystery to you,
forbidding you from using the full scope of the design space to
improve the reliability of your well-typed programs.

The same goes for claiming that `null` ought to be a default member of
*every* type, even the abstract ones that ought to be a little more
mysterious; it’s easy to add new capabilities (e.g. Scala’s `>: Null`
constraint, if you really *must* use `null`), but taking them away is
much, much harder.

Furthering this spirit of making good programs easier to write and bad
programs harder to write, a useful area of research in Scala might be
making signatures such as that of `mungeIDsSafely` nicer, or
signatures such as that of `mungeIDs` uglier.
