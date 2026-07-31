{%
  laika.title = Tutorials
%}

# Typelevel Tutorials

Welcome! These tutorials are hands-on: each lesson explains an idea on this side
of the page and gives you a live Scala editor on the other. Edit the code, press
**Run**, and see what happens.

@:style(bulma-notification bulma-is-warning bulma-is-light)
  This section is an early prototype. The lessons below exist to show that the
  machinery works; their content is simply a few vibe-coded example pages. They
  should be replaced by human-curated tracks that say what we want to say.
@:@

## Cats

[Cats](https://typelevel.org/cats/) is the foundation of the Typelevel
ecosystem: a library of type classes and data types for functional programming
in Scala.

@:navigationTree {
  entries = [
    { target = "/tutorials/cats", excludeRoot = true, excludeSections = true }
  ]
}

## Cats Effect

[Cats Effect](https://typelevel.org/cats-effect/) builds on Cats to describe
side effects as values, and gives you a runtime to execute them.

@:navigationTree {
  entries = [
    { target = "/tutorials/cats-effect", excludeRoot = true, excludeSections = true }
  ]
}

These tutorials were inspired by [scalatutorials](https://scalatutorials.com), which teaches
the Scala language itself. If you're just starting out, we recommend check that out first,
then come back here to help dive into Typelevel!
