{%
  author: ${larsrh}
  date: "2016-01-28"
  tags: [summits]
%}

# First batch of talks at the Philadelphia Summit

The work on the programme for the [Philadelphia Summit][philadelphia] is in full swing!
As announced earlier, we're happy to share with you the first batch of accepted talks.
Don't worry though, there's still time until the end of the week to [submit a proposal][cfp].

### Becoming a cat(s) person

Want to contribute to Cats?
Let's head over to the Cats Issues list and do some live coding!
Along the way we will see how the codebase is organized, the various bits of automation provided, and how you can use our various channels to get feedback on your work.

@:style(bulma-media)
@:style(bulma-media-left) @:style(bulma-figure bulma-image bulma-is-64x64) @:image(/img/media/speakers/adelbertchang.jpeg){ style: bulma-is-rounded } @:@ @:@
@:style(bulma-media-content) _Adelbert Chang is a Software Engineer at Box and a recent graduate from UC Santa Barbara where he studied Computer Science and researched graph querying and modeling. He enjoys helping with functional programming education and learning more about programming._@:@
@:@

### Direct syntax for monad comprehensions

Easy, intuitive, direct-style syntax for monad comprehensions!
Like Scala `async` or SBT `.value`, but generalized to any monad.
Implemented, ready to be used and requiring only vanilla Scala 2.10/2.11 and blackbox macros.
Future extensions could include automatic use of Applicative where possible, support for more embedded control-flow operations, comprehensions over multiple compatible monads at once for user-defined notions of compatible and compiler tweaks for syntactic improvements.

@:style(bulma-media)
@:style(bulma-media-left) @:style(bulma-figure bulma-image bulma-is-64x64) @:image(/img/media/speakers/chrisvogt.jpg){ style: bulma-is-rounded } @:@ @:@
@:style(bulma-media-content) _Chris Vogt. Slick co-author, Compossible records author, frequent Scala conference/user group speaker, former member of Martin's team at LAMP/EPFL, based in NYC, Senior Software Engineer at x.ai_@:@
@:@

@:style(bulma-media)
@:style(bulma-media-left) @:style(bulma-figure bulma-image bulma-is-64x64) @:image(/img/media/speakers/chrishodapp.jpg){ style: bulma-is-rounded } @:@ @:@
@:style(bulma-media-content) _Chris Hodapp. Several-time Scala GSOC student and eventually mentor, author of the ill-fated Comprehensive Comprehensions project. He's hoping to see tooling and techniques from the FP/Typelevel community improve the leverage of the average developer. Based in the SF Bay Area._@:@
@:@


### Scala Exercises

Scala Exercises is a web based community tool open sourced by 47 Degrees.
It contains multiple koan and free form style exercises maintained by library authors and maintainers to help you master some of the most important tools in the Scala Ecosystem.
Version 2 comes with a brand new backend and exercise tracking where you can login simply using your Github account and track your progress throughout exercises and libraries.
Version 2 will launch with exercises for the stdlib, Cats, Shapeless and other well known libraries and frameworks part of the Scala ecosystem.

@:style(bulma-media)
@:style(bulma-media-left) @:style(bulma-figure bulma-image bulma-is-64x64) @:image(/img/media/speakers/raulraja.jpg){ style: bulma-is-rounded } @:@ @:@
@:style(bulma-media-content) _Raul Raja. Functional programming enthusiast, CTO and Co-founder at 47 Degrees, a functional programming consultancy specialized in Scala._@:@
@:@

### Probabilistic Programming: What It Is and How It Works

Probabilistic programming is the other Big Thing to happen in machine learning alongside deep learning.
It is also closely tied to functional programming. In this talk I will explain the goals of probabilistic programming and how we can implement a probabilistic programming language in Scala.
Probabilistic models are one of the main approaches in machine learning.
Probabilistic programming aims to make expressive probabilistic models cheaper to develop.
This is achieved by expressing the model within an embedded DSL, and then compiling learning (inference) algorithms from the model description.
This automates one of the main tasks in building a probabilistic model, and provides the same benefits as a compiler for a traditional high-level language.
With the close tie of functional programming to mathematics, and the use of techniques like the free monad, functional programming languages are an ideal platform for embedding probabilistic programming.

@:style(bulma-media)
@:style(bulma-media-left) @:style(bulma-figure bulma-image bulma-is-64x64) @:image(/img/media/speakers/noelwelsh.png){ style: bulma-is-rounded } @:@ @:@
@:style(bulma-media-content) _Noel Welsh is a partner at Underscore, a consultancy that specializes in Scala. He's been using Scala for 6 years in all sorts of applications. He's the author of Advanced Scala, which is in the process of being rewritten to use Cats._@:@
@:@

[philadelphia]: summit-philadelphia-2016-03-02.md
[cfp]: http://goo.gl/forms/SX3plxsOKb
