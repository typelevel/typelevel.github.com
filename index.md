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
    <h2>cats</h2>
    <p><span class="tagline">Functional programming.</span> An experimental library intended to provide abstractions for functional programming in Scala, leveraging its unique features. Design goals are approachability, modularity, documentation and efficiency.</p>
    <p><a class="btn" href="https://github.com/non/cats">{% include octocat.html %}</a></p>
  </div>
  <div class="span8">
    <h2>cats ecosystem</h2>
    <p><span class="tagline">Extensions &amp; companions for cats.</span></p> 
    <p>
      <table>
        <tr>
          <td><a class="btn" href="https://github.com/stew/dogs">{% include octocat.html %} dogs</a></td>
          <td>Functional data structures</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/non/alleycats">{% include octocat.html %} alleycats</a></td>
          <td>Lawless classes &amp; illegal instances</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/milessabin/kittens">{% include octocat.html %} kittens</a></td>
          <td>Automatic type class derivation</td>
        </tr>
      </table>
    </p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>shapeless</h2>
    <p><span class="tagline">Generic programming.</span> Shapeless is an generic programming library. Starting with implementations of <em>Scrap your boilerplate</em> and higher rank polymorphism in Scala, it quickly grew to provide advanced abstract tools like heterogenous lists and automatic instance derivation for type classes.</p>
    <p><a class="btn" href="https://github.com/milessabin/shapeless">Learn more &raquo;</a></p>
  </div>
  <div class="span8">
    <h2>shapeless ecosystem</h2>
    <p><span class="tagline">Extensions &amp; companions for shapeless.</span></p> 
    <p>
      <table>
        <tr>
          <td><a class="btn" href="https://github.com/alexarchambault/argonaut-shapeless">{% include octocat.html %} argonaut-shapeless</a></td>
          <td>Automatic derivation for argonaut</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/alexarchambault/scalacheck-shapeless">{% include octocat.html %} scalacheck-shapeless</a></td>
          <td>Automatic derivation for ScalaCheck</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/fommil/spray-json-shapeless">{% include octocat.html %} spary-json-shapeless</a></td>
          <td>Automatic derivation for spray-json</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/typelevel/shapeless-contrib">{% include octocat.html %} shapeless-contrib</a></td>
          <td>Various bindings for third-party libraries</td>
        </tr>
      </table>
    </p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>algebra</h2>
    <p><span class="tagline">Basic type classes.</span> A solid foundation of basic algebra type classes such as groups and rings aiming to serve as a consistent foundation for multiple libraries.</p>
    <p><a class="btn" href="https://github.com/non/algebra">{% include octocat.html %}</a></p>
  </div>
  <div class="span4">
    <h2>spire</h2>
    <p><span class="tagline">Numeric abstractions.</span> Spire is a numeric library for Scala which is intended to be generic, fast, and precise. Using features such as specialization, macros, type classes, and implicits, Spire works hard to allow developers to write efficient numeric code without having to »bake in« particular numeric representations.</p>
    <p><a class="btn" href="https://github.com/non/spire">{% include octocat.html %}</a></p>
  </div>
  <div class="span4">
    <h2>refined</h2>
    <p><span class="tagline">Constraints on types.</span> Tools for refining types with type-level predicates which constrain the set of values described by the refined type, for example restricting to positive or negative numbers.</p>
    <p><a class="btn" href="https://github.com/fthomas/refined">{% include octocat.html %}</a></p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>Monocle</h2>
    <p><span class="tagline">Lenses for Scala.</span> Strongly inspired by Haskell's <code>lens</code> library, Monocle is an Optics library where Optics gather the concepts of <code>Lens</code>, <code>Traversal</code>, <code>Optional</code>, <code>Prism</code> and <code>Iso</code>.</p>
    <p><a class="btn" href="https://github.com/julien-truffaut/Monocle">{% include octocat.html %}</a></p>
  </div>
  <div class="span4">
    <h2>circe</h2>
    <p><span class="tagline">Principled JSON processing.</span> Yet another JSON library for Scala which integrates with cats, shapeless and Monocle.</p>
    <p><a class="btn" href="https://github.com/travisbrown/circe">{% include octocat.html %}</a></p>
  </div>
   <div class="span4">
    <h2>scodec</h2>
    <p><span class="tagline">Binary serialization.</span> scodec is a combinator library for working with binary data. It focuses on contract-first and pure functional encoding and decoding of binary data and provides integration into scalaz and shapeless.</p>
    <p><a class="btn" href="https://github.com/scodec/scodec">{% include octocat.html %}</a></p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>ScalaCheck</h2>
    <p><span class="tagline">Property checking.</span> ScalaCheck is a library for automated property-based testing. It contains generators for randomized test data and combinators for properties.</p>
    <p><a class="btn" href="https://github.com/rickynils/scalacheck">{% include octocat.html %}</a> <a class="btn" href="http://scalacheck.org">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>specs2</h2>
    <p><span class="tagline">Expressive specifications.</span> specs2 is a library for writing executable software specifications, aiming for conciseness, readability and extensibility.</p>
    <p><a class="btn" href="https://github.com/etorreborre/specs2">{% include octocat.html %}</a> <a class="btn" href="http://specs2.org">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>discipline</h2>
    <p><span class="tagline">Flexible law checking.</span> Originally intended for internal use in spire, this library helps libraries declaring type classes to precisely state the laws which instances need to satisfy, and takes care of not checking derived laws multiple times.</p>
    <p><a class="btn" href="https://github.com/typelevel/discipline">{% include octocat.html %}</a></p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>Ensime</h2>
    <p><span class="tagline">Scala for text editors.</span> A cross editor platform for Emacs, Sublime, Atom and vim offering semantic editing for Scala, including contextual completion, classpath search and refactorings.</p>
    <p><a class="btn" href="https://github.com/ensime">{% include octocat.html %}</a></p>
  </div>
  <div class="span4">
    <h2>doobie</h2>
    <p><span class="tagline">Principled database access.</span> A pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it just provides a principled way to construct programs (and higher-level libraries) that use JDBC.</p>
    <p><a class="btn" href="https://github.com/tpolecat/doobie">{% include octocat.html %}</a></p>
  </div>
  <div class="span4">
    <h2>tut</h2>
    <p><span class="tagline">Checked documentation.</span> A very simple documentation tool for Scala that reads Markdown files and interprets Scala code in <code>tut</code> sheds, allowing you to write documentation that is typechecked and run as part of your build.</p>
    <p><a class="btn" href="https://github.com/tpolecat/tut">{% include octocat.html %}</a></p>
  </div>
</div>
<div class="row-fluid">
  <div class="span4">
    <h2>Scala</h2>
    <p><span class="tagline">Our fork of the Scala compiler.</span> We wish to work with all stakeholders in the Scala ecosystem to safeguard the interests of the entire Scala community.</p>
    <p><a class="btn" href="https://github.com/typelevel/scala">{% include octocat.html %}</a> <a class="btn" href="{% post_url 2014-09-02-typelevel-scala %}">Learn more &raquo;</a></p>
  </div>
  <div class="span4">
    <h2>catalysts</h2>
    <p><span class="tagline">Project building blocks.</span> A small library of building blocks to help build platform independent Scala projects with SBT.</p>
    <p><a class="btn" href="https://github.com/InTheNow/catalysts">{% include octocat.html %}</a></p>
  </div>
  <div class="span4">
    <h2>structures</h2>
    <p><span class="tagline">Functional type classes.</span> Zero-dependency Scala library that defines commonly used type classes for functional programming.</p>
    <p><a class="btn" href="https://github.com/mpilquist/structures">{% include octocat.html %}</a></p>
  </div>
</div>

<div class="row-fluid">
  <div class="span12">
    <h2>Macros &amp; compiler plugins</h2>
    <p><span class="tagline">Extending the language.</span></p>
    <p>
      <table>
        <tr>
          <td><a class="btn" href="https://github.com/milessabin/export-hook">{% include octocat.html %} export-hook</a></td>
          <td>Support for expanding implicit scope</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/non/imp">{% include octocat.html %} imp</a></td>
          <td>Summoning implicit values</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/non/kind-projector">{% include octocat.html %} kind-projector</a></td>
          <td>Plugin for nicer type-lambda syntax</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/mpilquist/local-implicits">{% include octocat.html %} local-implicits</a></td>
          <td>Plugin for locally-scoped implicit values</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/typelevel/machinist">{% include octocat.html %} machinist</a></td>
          <td>Zero-cost operator enrichment</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/milessabin/macro-compat">{% include octocat.html %} macro-compat</a></td>
          <td>Cross-version macro support</td>
        </tr>
        <tr>
          <td><a class="btn" href="https://github.com/mpilquist/simulacrum">{% include octocat.html %} simulacrum</a></td>
          <td>First-class syntax for type classeo</td>
        </tr>
      </table>
    </p>
  </div>
</div>
