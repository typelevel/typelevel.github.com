//> using dep org.http4s::http4s-ember-server::0.23.33
//> using dep org.typelevel::laika-preview::1.3.2
//> using dep com.monovore::decline-effect::2.6.0
//> using dep org.graalvm.js:js:25.0.2
//> using dep org.webjars.npm:katex:0.16.28
//> using dep pink.cozydev::protosearch-laika:0.0-fdae301-SNAPSHOT
//> using repository https://central.sonatype.com/repository/maven-snapshots
//> using option -deprecation

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

// Welcome to the typelevel.org build script!
// This script builds the site and can serve it locally for previewing.
//
// Main -- Entry point
// LaikaBuild -- Laika build, markdown in html out
// LaikaCustomizations -- Custom directives

object Main extends CommandIOApp("build", "builds the site") {
  import com.comcast.ip4s.*
  import fs2.io.file.{Files, Path}
  import laika.io.model.FilePath
  import laika.preview.{ServerBuilder, ServerConfig}
  import org.http4s.server.Server

  enum Subcommand {
    case Serve(port: Port)
    case Build(output: Path)
  }

  val portOpt = Opts
    .option[Int]("port", "bind to this port")
    .mapValidated(Port.fromInt(_).toValidNel("Invalid port"))
    .withDefault(port"8000")

  val destinationOpt = Opts
    .option[String]("out", "site output directory")
    .map(Path(_))
    .withDefault(Path("target"))

  val opts = Opts
    .subcommand("serve", "serve the site")(portOpt.map(Subcommand.Serve(_)))
    .orElse(destinationOpt.map(Subcommand.Build(_)))

  def main = opts.map {
    case Subcommand.Build(destination) =>
      Files[IO].deleteRecursively(destination).voidError *>
        LaikaBuild.build(FilePath.fromFS2Path(destination)).as(ExitCode.Success)

    case Subcommand.Serve(port) =>
      val serverConfig = ServerConfig.defaults
        .withPort(port)
        .withBinaryRenderers(LaikaBuild.binaryRenderers)
      val server = ServerBuilder[IO](LaikaBuild.parser, LaikaBuild.input)
        .withConfig(serverConfig)
        .build
      server.evalTap(logServer(_)).useForever
  }

  def logServer(server: Server) =
    IO.println(s"Serving site at ${server.baseUri}")
}

object LaikaBuild {
  import java.net.{URI, URL}
  import laika.api.*
  import laika.api.format.*
  import laika.ast.*
  import laika.config.*
  import laika.format.*
  import laika.io.config.*
  import laika.io.model.*
  import laika.io.syntax.*
  import laika.parse.code.languages.ScalaSyntax
  import laika.theme.*
  import pink.cozydev.protosearch.analysis.{IndexFormat, IndexRendererConfig}
  import pink.cozydev.protosearch.ui.SearchUI

  def input = {
    val securityPolicy = new URI(
      "https://raw.githubusercontent.com/typelevel/.github/refs/heads/main/SECURITY.md"
    ).toURL()

    InputTree[IO]
      .addDirectory("src")
      .addInputStream(
        IO.blocking(securityPolicy.openStream()),
        Path.Root / "security.md"
      )
      .addClassResource[this.type](
        "laika/helium/css/code.css",
        Path.Root / "css" / "code.css"
      )
  }

  def theme = {
    val provider = new ThemeProvider {
      def build[F[_]: Async] =
        ThemeBuilder[F]("typelevel.org")
          .addRenderOverrides(LaikaCustomizations.overrides)
          .build
    }

    provider.extendWith(SearchUI.standalone)
  }

  def parser = MarkupParser
    .of(Markdown)
    .using(
      Markdown.GitHubFlavor,
      SyntaxHighlighting.withSyntaxBinding("scala", ScalaSyntax.Scala3),
      LaikaCustomizations.Directives,
      LaikaCustomizations.RssExtensions
    )
    .withConfigValue(LinkValidation.Global(excluded = Seq(Path.Root / "blog" / "feed.rss")))
    .withConfigValue(LaikaKeys.siteBaseURL, "https://typelevel.org/")
    .parallel[IO]
    .withTheme(theme)
    .build

  val binaryRenderers = List(
    IndexRendererConfig(true),
    BinaryRendererConfig(
      "rss",
      LaikaCustomizations.Rss,
      artifact = Artifact(
        basePath = Path.Root / "blog" / "feed",
        suffix = "rss"
      ),
      false,
      false
    )
  )

  def build(destination: FilePath) = parser.use { parser =>
    val html = Renderer
      .of(HTML)
      .withConfig(parser.config)
      .parallel[IO]
      .withTheme(theme)
      .build
    val rss = Renderer
      .of(LaikaCustomizations.Rss)
      .withConfig(parser.config)
      .parallel[IO]
      .build
    val index =
      Renderer.of(IndexFormat).withConfig(parser.config).parallel[IO].build

    (html, rss, index).tupled.use { (html, rss, index) =>
      parser.fromInput(input).parse.flatMap { tree =>
        html.from(tree).toDirectory(destination).render *>
          rss.from(tree).toFile(destination / "blog" / "feed.rss").render *>
          index
            .from(tree)
            .toFile(destination / "search" / "searchIndex.idx")
            .render
      }
    }
  }
}

object LaikaCustomizations {
  import cats.data.NonEmptySet
  import java.time.OffsetDateTime
  import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
  import laika.config.*
  import laika.api.bundle.*
  import laika.api.config.*
  import laika.api.format.*
  import laika.ast.*
  import laika.format.*
  import laika.theme.*

  def addAnchorLinks(fmt: TagFormatter, h: Header) = {
    val link = h.options.id.map { id =>
      SpanLink
        .internal(RelativePath.CurrentDocument(id))(
          Literal("", Styles("fas", "fa-link", "fa-sm"))
        )
        .withOptions(
          Styles("anchor-link")
        )
    }
    val linkedContent = link.toList ++ h.content
    fmt.newLine + fmt.element(
      "h" + h.level.toString,
      h.withContent(linkedContent)
    )
  }

  val overrides = HTML.Overrides { case (fmt, h: Header) =>
    addAnchorLinks(fmt, h)
  }

  object RssExtensions extends ExtensionBundle {
    def description = "RSS-specific extensions"

    override def extendPathTranslator =
      ctx => ExtendedTranslator(ctx.baseTranslator)

    private final class ExtendedTranslator(delegate: PathTranslator)
        extends PathTranslator {
      export delegate.{translate, getAttributes}

      def forReferencePath(path: Path) =
        ExtendedTranslator(delegate.forReferencePath(path))

      override def translate(target: Target) = target match {
        case InternalTarget.Resolved(absolutePath, relativePath, formats) =>
          delegate.translate(
            InternalTarget.Resolved(
              absolutePath,
              relativePath,
              TargetFormats.Selected("html") // force HTML links in RSS feed
            )
          )
        case target =>
          delegate.translate(target)
      }
    }
  }

  object Directives extends DirectiveRegistry {

    val templateDirectives = Seq(
      // custom Laika template directive for listing blog posts
      TemplateDirectives.eval("forBlogPosts") {
        import TemplateDirectives.dsl.*

        (cursor, parsedBody, source).mapN { (c, b, s) =>
          def contentScope(value: ConfigValue) =
            TemplateScope(TemplateSpanSequence(b), value, s)

          val posts = c.parent.allDocuments.flatMap { d =>
            d.config.get[OffsetDateTime]("date").toList.tupleLeft(d)
          }

          posts
            .sortBy(_._2)(using summon[Ordering[OffsetDateTime]].reverse)
            .traverse { (d, _) =>
              d.config.get[ConfigValue]("").map(contentScope(_))
            }
            .leftMap(_.message)
            .map(TemplateSpanSequence(_))
        }
      }
    )

    val linkDirectives = Seq.empty
    val spanDirectives = Seq(
      SpanDirectives.create("math") {
        import SpanDirectives.dsl.*
        rawBody.map { body =>
          RawContent(
            NonEmptySet.of("html", "rss"),
            KaTeX(body, false)
          )
        }
      }
    )
    val blockDirectives = Seq(
      BlockDirectives.create("math") {
        import BlockDirectives.dsl.*
        rawBody.map { body =>
          RawContent(
            NonEmptySet.of("html", "rss"),
            KaTeX(body, true),
            Styles("bulma-has-text-centered")
          )
        }
      }
    )
  }

  object Rss
      extends TwoPhaseRenderFormat[TagFormatter, BinaryPostProcessor.Builder] {

    def interimFormat = new {
      def fileSuffix = "rss"

      val defaultRenderer = {
        case (fmt, Title(_, _)) =>
          "" // don't render title b/c it is in the RSS metadata
        case (fmt, elem) => HTML.defaultRenderer(fmt, elem)
      }

      export HTML.formatterFactory
    }

    def prepareTree(tree: DocumentTreeRoot) = Right(tree)

    def postProcessor: BinaryPostProcessor.Builder = new {
      def build[F[_]: Async](config: Config, theme: Theme[F]) =
        Resource.pure { (result, output, config) =>
          val posts = result.allDocuments
            .flatMap { d =>
              d.config.get[OffsetDateTime]("date").toList.tupleLeft(d)
            }
            .sortBy(_._2)(using summon[Ordering[OffsetDateTime]].reverse)

          output.resource.use { os =>
            Async[F].blocking {
              val pw = new java.io.PrintWriter(os)
              pw.print("""|<?xml version="1.0" encoding="UTF-8" ?>
                          |<rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
                          |<channel>
                          |<title>Typelevel Blog</title>
                          |<link>https://typelevel.org/blog/</link>
                          |<description>The Typelevel Blog RSS Feed</description>
                          |""".stripMargin)

              posts
                .takeWhile(_._2.isAfter(OffsetDateTime.now().minusYears(1)))
                .foreach { (doc, _) =>
                  pw.print(doc.content)
                }

              pw.print("""|</channel>
                          |</rss>
                          |""".stripMargin)
              pw.flush()
            }
          }
        }
    }
  }
}

object KaTeX {
  import org.graalvm.polyglot.*
  import scala.jdk.CollectionConverters.*

  private def loadKaTeX(): String = {
    val resourcePath = "/META-INF/resources/webjars/katex/0.16.28/dist/katex.js"
    val inputStream = getClass.getResourceAsStream(resourcePath)
    new String(inputStream.readAllBytes())
  }

  private lazy val katex = {
    val ctx = Context
      .newBuilder("js")
      .allowAllAccess(true)
      .build()
    ctx.eval("js", loadKaTeX())
    ctx.getBindings("js").getMember("katex")
  }

  def apply(latex: String, displayMode: Boolean = false): String =
    synchronized {
      val options = Map("throwOnError" -> true, "strict" -> true, "displayMode" -> displayMode)
      katex.invokeMember("renderToString", latex, options.asJava).asString
    }

}
