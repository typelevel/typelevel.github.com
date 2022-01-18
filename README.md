typelevel website
=================

This is the website of typelevel.scala. It is built on Jekyll and served at [typelevel.org](https://typelevel.org).

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

You can also use `tut` in posts. See `posts/2016-09-30-subtype-typeclasses.md` for an example.

### Previewing your changes

#### Bundler

To preview your changes, you have to install [Bundler](https://bundler.io/) first.
To download and set up all necessary dependencies, run

```console
$ bundle install
... lots of text ...
Bundle complete! 1 Gemfile dependency, 81 gems now installed.
Bundled gems are installed into `./vendor/bundle`
```

Then, you can generate the site by running

```console
$ bundle exec jekyll serve -wl --baseurl ''
```

The generated site will end up in the `_site` directory.

#### Nix

A fully configured Jekyll is available as a Nix app.  Flakes required:

```console
$ nix run github:typelevel/typelevel.github.com#jekyll build
warning: Git tree '/Users/ross.baker/src/typelevel.github.com' is dirty
Configuration file: /Users/ross.baker/src/typelevel.github.com/_config.yml
            Source: /Users/ross.baker/src/typelevel.github.com
       Destination: /Users/ross.baker/src/typelevel.github.com/_site
 Incremental build: disabled. Enable with --incremental
      Generating...
                    done in 3.635 seconds.
 Auto-regeneration: disabled. Use --watch to enable.
```

There is also a devshell for direct invocation, and a convenient alias:

```console
$ nix develop github:typelevel/typelevel.github.com
🔨 Welcome to typelevel-org-shell

[general commands]

  jekyll     - a jekyll bundled with this site's dependencies
  menu       - prints this menu
  tl-preview - preview the Jekyll site

$ tl-preview
Configuration file: /home/you/src/typelevel.github.com/_config.yml
            Source: /home/you/src/typelevel.github.com
       Destination: /home/you/src/typelevel.github.com/_site
 Incremental build: disabled. Enable with --incremental
      Generating...
                    done in 3.336 seconds.
 Auto-regeneration: enabled for '/home/you/src/typelevel.github.com'
LiveReload address: http://127.0.0.1:35729
    Server address: http://127.0.0.1:4000/
  Server running... press ctrl-c to stop.
```



## License

Unless otherwise noted, all website content is licensed under a [Creative Commons Attribution 3.0 Unported License](https://creativecommons.org/licenses/by/3.0/deed.en_US).

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

There are four types of projects: core/featured projects, regular projects, incubator projects, and macros.

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
  - title: "Alleycats"
    description: "Lawless classes & illegal instances"
    github: "https://github.com/non/alleycats"
```

Incubator projects and macros are created a little differently. They are located in `_data/incubator.yml` and `_data/macros.yml` respectively, and look like this:

```yml
- title: "imp"
  description: "Summoning implicit values"
  github: "https://github.com/non/imp"
```

### Adding a page

To add a page, create a HTML or Markdown file in the root of the project. The site navigation is not fully dynamic for simplification purposes. It can be changed in the default layout (`_layouts/default.html`).

Sample front matter for a page:

```yml
layout: page
title: "Code of Conduct"
```
