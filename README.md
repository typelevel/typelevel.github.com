typelevel website
=================

This is the website of typelevel.scala. It is built on Jekyll and served at [typelevel.org](https://typelevel.org).

## Important notice

:loudspeaker: The blog post workflow has changed:
- The default branch is now `development`. `master` is the target for automated commits from Travis. Please `git checkout development` for doing anything.
- Posts go into `posts` instead of `_posts`. Run `sbt run` to have them copied over.
- You can use `tut` in posts. See `posts/2016-09-30-subtype-typeclasses.md` for an example.

## Getting Started (the short version)

If you just want to add a blog post or fix a typo in the content, here's how to get started.

### Creating a blog post

1. Create a new file in the `posts` directory or copy an existing post. Its name should have the format `YYYY-MM-DD-short_title.md`.
2. Set the `title` (short title of the post, appears as the HTML `<title>`) and `author` (your GitHub user name) in the front matter. MathJax is available via `mathjax: true` inside the front matter.
3. If this is your first blog post, please indicate if you want your name and a profile picture to appear on the post. If not, you can remove the `author` field from the front matter. Add your details in `_data/authors.yml`.
4. Write your content using Markdown. For code highlighting, use the usual GitHub syntax:

```scala
def yourCode: Here
```

If you haven't written a post before, please add yourself to `_data/authors.yml`.

That's it, we'll take care of the rest. If you wish, you can also submit just a plain Markdown file and we'll be happy to integrate it.

### Previewing your changes

To preview your changes, you have to install Ruby, Bundler and SBT first.
Once you've done that, you can start the Jekyll server like this:

```bash
sbt run
bundle install
bundle exec jekyll serve --baseurl ''
```

This will first preprocess the blog posts and then start a local web server on port 4000 where you can browse the site.
For automatic site rebuild, run the following two commands in different terminals:

```
sbt ~run
bundle exec jekyll serve --incremental --baseurl ''
```

This will trigger a recompile and rebuild if you change the source files.

## License

Two different licenses apply:
* The Scala code, including the SBT build definition, is licensed under the [MIT License](https://opensource.org/licenses/MIT).
* Unless otherwise noted, all website content is licensed under a [Creative Commons Attribution 3.0 Unported License](https://creativecommons.org/licenses/by/3.0/deed.en_US).

## Development

### CSS

The stylesheets are written in SASS, and can be found in the `css` and `_scss` directories.
It is being processed/compiled into regular CSS by Jekyll.

```
├── css/
│   ├── main.scss # Custom CSS, brings all stylesheets together
├── _scss/
│   ├── _fonts.scss # @font-face embedding.
│   ├── _mixins.scss # SASS mixins
│   ├── _reset.scss # Normalize stylesheet
│   ├── _syntax.scss # Syntax highlighting by Pygments
│   ├── _variables.scss # SASS variables (colors, fonts, etc.)
```

### Javascript

Javascript can be found in the `js/` folder, which also includes its dependencies.

### Templates

All templates/layouts can be found in the `_layouts` folder, except the blog layout, which is located inside its own subfolder `blog/`.

### Images

Images for styling purposes are located inside `img/`, photos inside `img/media/`.

### Adding a project

There are three types of projects: core/featured projects, regular projecs, and macros.

To add a regular project, create a new markdown file in the `_projects` folder with the following front matter:

```yml
layout: post
title: "Cats"
category: "Functional Programming"
description: "An experimental library intended to provide abstractions for functional programming in Scala, leveraging its unique features. Design goals are approachability, modularity, documentation and efficiency."
permalink: "https://non.github.io/cats/"
github: "https://github.com/non/cats"
```

Right now nothing more than the correct front matter is required.

Do the same for a **core/featured** project, but also add `core: true`.
To add companions or extensions to these projects, use the front matter, too:

```yml
extensions:
  - title: "Dogs"
    description: "Functional data structures"
    github: "https://github.com/stew/dogs"
  - title: "Alleycats"
    description: "Lawless classes & illegal instances"
    github: "https://github.com/non/alleycats"
```

Macros are created a little differently. They are located in `_data/macros.yml` and look like this:

```yml
- title: "imp"
  description: "Summoning implicit values"
  github: "https://github.com/non/imp"
```

### Adding a page

To add a page, create a HTML or Markdown file in the root of the project. The site navgation is not fully dynamic for simplification purposes. It can be changed in the default layout (`_layouts/default.html`).

Sample front matter for a page:

```yml
layout: page
title: "Code of Conduct"
```
