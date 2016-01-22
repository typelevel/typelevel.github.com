typelevel website
=================

This is the website of typelevel.scala. It is built on Jekyll and served at [typelevel.org](//typelevel.org).

Initial development setup comes from [shakyShane/jekyll-gulp-sass-browser-sync](https://github.com/shakyShane/jekyll-gulp-sass-browser-sync).

## Getting Started (the short version)

If you just want to add a blog post or fix a typo in the content, here's how to get started.

### Creating a blog post

1. Create a new file in the `_posts` directory or copy an existing post. Its name should have the format `YYYY-MM-DD-short_title.md`.
2. Set the `title` (short title of the post, appears as the HTML `<title>`) and `author` (your GitHub user name) in the front matter. MathJax is available via `mathjax: true` inside the front matter.
3. If this is your first blog post, please indicate if you want your name and a profile picture to appear on the post. If not, you can remove the `author` field from the front matter. Add your details in `_data/authors.yml`.
4. Write your content using Markdown. For code highlighting, use the usual GitHub syntax:

```scala
def yourCode: Here
```

If you haven't written a post before, please add yourself to `_data/authors.yml`.

That's it, we'll take care of the rest. If you wish, you can also submit just a plain Markdown file and we'll be happy to integrate it.

### Previewing your changes

To preview your changes, you have to install the following things first:

* Ruby
* node.js
* Pygments

Once you've done that, you need to install `github-pages`.
This will give you a local setup mirroring GitHub's setup.
You can install this package via `gem`:

```bash
# don't forget to add the directory where Gem binaries are installed to your `$PATH`
# on my machine, that's `$HOME/.gem/ruby/2.2.0/bin`
gem install github-pages
```

Everything set up? Great. Now you just have to tell Jekyll to generate the site (the empty quotation at the end is important):

```bash
jekyll serve --watch --baseurl ''
```

This will start a local web server on port 4000 where you can browse the site and which re-generates the site if you change the source files.

## Full Setup

The whole building process, including Jekyll, happens trough [gulp](http://gulpjs.com/).
To start development install the dependencies and start `gulp`.

1. `npm install`
2. `npm install gulp -g` (globally install gulp)
3. You might have to install some gems, use `gem install bundler && bundle install` for that (also installs Jekyll).
4. `gulp`
5. Navigate to [127.0.0.1:3000](http://127.0.0.1:3000).

Keep in mind that you need Ruby and Node.js installed on your machine (see above).

## Development

### CSS

The stylesheets are written in SASS, and can be found in the `_scss` directory. It is being processed/compiled into regular CSS by gulp. The processed and minified CSS is moved to `css/`.

```
├── _scss/
│   ├── _fonts.scss # @font-face embedding.
│   ├── _mixins.scss # SASS mixins
│   ├── _reset.scss # Normalize stylesheet
│   ├── _syntax.scss # Syntax highlighting by Pygments
│   ├── _variables.scss # SASS variables (colors, fonts, etc.)
│   ├── main.scss # Custom CSS, brings all stylesheets together
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
