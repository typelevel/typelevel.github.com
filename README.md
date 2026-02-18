# typelevel.org

This is the source of typelevel.org. It is built with [Laika] and deployed to GitHub Pages.

## Get Started

To work on the website, you will need:
* Scala 3.5 or later
* Java 21 or later

### Preview Server

For the best experience, serve the website to immediately see your changes in a live preview.

```bash
scala build.scala -- serve
```

Within a few seconds, a preview server will be available at http://localhost:8000/. Press `Ctrl+C` to stop the server. In case you need to use a different port, you may pass it as an option.
```bash
scala run build.scala -- serve --port 8080
```

### Write a blog post

Blog posts (including event announcements) are added to the `src/blog/` directory. Content is written using [GitHub-flavored Markdown][gfm]. Code blocks support syntax highlighting in Scala and [several other languages][syntax]. Rendering of mathematical expressions is enabled for any document by setting `katex: true` in the configuration header and using the `@:math` directive.

```
@:math
\forall a,b,c \in S : (a \cdot b) \cdot c = a \cdot (b \cdot c)
@:@
```

If this is your first blog post, be sure to add your author info to `src/directory.conf`.

```hocon
toolkitty {
  name: Toolkitty
  pronouns: "they/them"
  avatar: "https://github.com/toolkitty.png"
  github: toolkitty
  bluesky: toolkitty.bsky.social
  bio: "I am the mascot of the Scala Toolkit!"
}
```

Note that event announcements use a custom template with additional fields specified in the configuration header.

```
{%
  laika.html.template: event.template.html
  date: "2025-08-15" # the date the post is published
  event-date: "August 22, 2025" # the actual date of the event
  event-location: "École Polytechnique Fédérale de Lausanne"
  tags: [events]
%}
```

## Development

The build machinery is defined in `build.scala`. It implements several customizations, including an RSS feed generator and integrations with Protosearch, KaTeX, and Font Awesome.

To learn more about how you can develop and customize the website please reference the extensive [Laika] documentation.

## Support

We are happy to help you contribute to our website! Please [create a discussion][discussion] or message the [#website][discord] channel on the Typelevel Discord.

[Laika]: https://typelevel.org/Laika
[syntax]: https://typelevel.org/Laika/latest/03-preparing-content/05-syntax-highlighting.html#supported-languages
[gfm]: https://github.github.com/gfm/
[discussion]: https://github.com/typelevel/typelevel.github.com/discussions/new/choose
[discord]: https://discord.gg/krrdNdSDFf
