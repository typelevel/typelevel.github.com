---
layout: index
title: "Powerful libraries for Scala"

meta:
  nav: index
  canonical: ""
---

<div class="jumbotron">
  <h1>{% include typelevel.html %}</h1>
  <p class="lead">Let the Scala compiler work for you. We provide type classes, instances, conversions, testing, supplements to the standard library, and much more.</p>
  <a class="btn btn-large btn-success" href="{{ site.baseurl }}/projects">Get started</a>
</div>

<hr>

<div class="row-fluid">
  <div class="span4">
    <h2>discipline</h2>
    <p><span class="tagline">Flexible law checking.</span> Originally intended for internal use in spire, this library helps libraries declaring type classes to precisely state the laws which instances need to satisfy, and takes care of not checking derived laws multiple times.</p>
    <p><a class="btn" href="https://github.com/typelevel/discipline">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>Monocle</h2>
    <p><span class="tagline">Lenses for Scala.</span> Strongly inspired by Haskell's <code>lens</code> library, Monocle is an Optics library where Optics gather the concepts of <code>Lens</code>, <code>Traversal</code>, <code>Optional</code>, <code>Prism</code> and <code>Iso</code>.</p>
    <p><a class="btn" href="https://github.com/julien-truffaut/Monocle">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>ScalaCheck</h2>
    <p><span class="tagline">Property checking.</span> ScalaCheck is a library for automated property-based testing. It contains generators for randomized test data and combinators for properties.</p>
    <p><a class="btn" href="http://scalacheck.org">Learn more &raquo;</a></p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>shapeless</h2>
    <p><span class="tagline">Generic programming.</span> Shapeless is an generic programming library. Starting with implementations of <em>Scrap your boilerplate</em> and higher rank polymorphism in Scala, it quickly grew to provide advanced abstract tools like heterogenous lists and automatic instance derivation for type classes.</p>
    <p><a class="btn" href="https://github.com/milessabin/shapeless">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>specs2</h2>
    <p><span class="tagline">Expressive specifications.</span> specs2 is a library for writing executable software specifications, aiming for conciseness, readability and extensibility.</p>
    <p><a class="btn" href="http://specs2.org">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>spire</h2>
    <p><span class="tagline">Numeric abstractions.</span> Spire is a numeric library for Scala which is intended to be generic, fast, and precise. Using features such as specialization, macros, type classes, and implicits, Spire works hard to allow developers to write efficient numeric code without having to »bake in« particular numeric representations.</p>
    <p><a class="btn" href="https://github.com/non/spire">Learn more &raquo;</a></p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>*-contrib</h2>
    <p><span class="tagline">Little helpers.</span> To make your life even easier, we tried to keep the dependencies between the projects to a minimum. If you choose to combine some libraries, we provide convenient helpers to reduce interoperability boilerplate in your code.</p>
    <p><a class="btn" href="{{ site.baseurl }}/projects/#helpers">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>Scala</h2>
    <p><span class="tagline">Our fork of the Scala compiler.</span> We wish to work with all stakeholders in the Scala ecosystem to safeguard the interests of the entire Scala community.</p>
    <p><a class="btn" href="{{ site.baseurl }}/blog/2014/09/02/typelevel-scala.html">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>Plugins for scalac</h2>
    <p><span class="tagline">Extending the compiler.</span> We provide some experimental compiler plugins to scrap some more boilerplate and help you adhere to certain coding standards.</p>
    <p><a class="btn" href="{{ site.baseurl }}/projects/#helpers">Learn more &raquo;</a></p>
  </div>
</div>
