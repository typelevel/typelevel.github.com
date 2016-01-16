typelevel website
=================

This is the website of typelevel.scala. It is built on Jekyll and served at [typelevel.org](//typelevel.org).

Initial development setup comes from [shakyShane/jekyll-gulp-sass-browser-sync](https://github.com/shakyShane/jekyll-gulp-sass-browser-sync).

## Setup
The whole building process, including Jekyll, happens trough [gulp](http://gulpjs.com/). To start development, clone this repo, install the dependencies and start gulp.

1. `npm install`
2. `gulp`
3. Navigate to [127.0.0.1:3000](http://127.0.0.1:3000).

Keep in mind that you need Ruby, Node.js and Jekyll installed on your machine. To install Jekyll you can use `gem install jekyll`, find installation instructions for Ruby and Node on their respective websites. Gulp is installed through npm.

## Development
### CSS
The stylesheets are written in SASS, and can be found in the `_scss` directory. It is being processed/compiled into regular CSS by gulp. The processed and minified CSS is moved to `css/`.

````
├── _scss/
│   ├── _fonts.scss # @font-face embedding.
│   ├── _mixins.scss # SASS mixins
│   ├── _reset.scss # Normalize stylesheet
│   ├── _syntax.scss # Syntax highlighting by Pygments
│   ├── _variables.scss # SASS variables (colors, fonts, etc.)
│   ├── main.scss # Custom CSS, brings all stylesheets together
````

### Javascript
Javascript can be found in the `js/` folder, which also includes its dependencies.

### Templates
All templates/layouts can be found in the `_layouts` folder, except the blog layout, which is located inside its own subfolder `blog/`.

### Images
Images for styling purposes are located inside `img/`, photos inside `img/media/`.

## Working with content
### Adding a blog post
1. Create a new file in the `_posts` directory or copy an existing post. Its name should have the format `YYYY-MM-DD-short_title.md`.
2. Set the `title` (short title of the post, appears as the HTML `<title>`) and `author` (your GitHub user name) in the front matter. MathJax is available via `mathjax: true` inside the front matter.
3. If this is your first blog post, please indicate if you want your name and a profile picture to appear on the post. If not, you can remove the `author` field from the front matter. Add your details in `_data/authors.yml`.
4. Write your content using Markdown. For code highlighting, use the usual GitHub syntax:

```scala
def yourCode: Here
```

That's it, we'll take care of the rest. If you wish, you can also submit just a plain Markdown file and we'll be happy to integrate it.

### Adding an event
Adding an event is very similar to adding a post. Create a new markdown file inside `_events/` and name it as you would like to have it in the URL. Example `philladelphia-meetup.md` would become *typelevel.org/event/philladelphia-meetup*. You want to include some of the front matter:

````yml
title: "Typelevel Summit Philadelphia"
date: 02-03-16
date_string: "March 2-3, 2016"
location: "Hub's Cira Centre"
description: "An experimental library intended to provide abstractions for functional programming in Scala, leveraging its unique features."

poster_hero: "/img/media/philly.jpg"
poster_thumb: "/img/media/philly-thumb.jpg"

location_section: true
papers_section: true
sponsors_section: true
````

The last three lines can hide/show the according sections in the header of the event page. They link to the specific headings on the page (anchors for this are automatically added to the headings by Redcarpet's TOC).

After that, just write regular markdown. Refer to the already existing events for reference.

### Adding a project
There are three types of projects: core/featured projects, regular projecs, and macros.

To add a regular project, create a new markdown file in the `_projects` folder with the following front matter:

````yml
layout: post
title: "Cats"
category: "Functional Programming"
description: "An experimental library intended to provide abstractions for functional programming in Scala, leveraging its unique features. Design goals are approachability, modularity, documentation and efficiency."
permalink: "https://non.github.io/cats/"
github: "https://github.com/non/cats"
````

Right now nothing more than the correct front matter is required.

Do the same for a **core/featured** project, but also add `core: true`. To add companions or extensions to these projects, use the front matter, too:

````yml
extensions:
  - title: "Dogs"
    description: "Functional data structures"
    github: "https://github.com/stew/dogs"
  - title: "Alleycats"
    description: "Lawless classes & illegal instances"
    github: "https://github.com/non/alleycats"
````

Macros are created a little differently. They are located in `_data/macros.yml` and look like this:

````yml
- title: "imp"
  description: "Summoning implicit values"
  github: "https://github.com/non/imp"
````

### Adding a page
To add a page, create a HTML or Markdown file in the root of the project. The site navgation is not fully dynamic for simplification purposes. It can be changed in the default layout (`_layouts/default.html`).

Sample front matter for a page:

````yml
layout: page
title: "Code of Conduct"
````

### Global variables
Some variables that are used throughout the website are stored inside the `_config.yml` file, such as social links or usernames. It also includes some configurations. 