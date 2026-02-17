//> using dep org.http4s::http4s-ember-server::0.23.33
//> using dep org.typelevel::laika-preview::1.3.2
//> using dep com.monovore::decline-effect::2.6.0
//> using dep org.graalvm.js:js:25.0.2
//> using dep org.webjars.npm:katex:0.16.28
//> using dep org.webjars.npm:fortawesome__fontawesome-free:7.1.0
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
      .merge(Redirects.inputTree)
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
    .withConfigValue(
      LinkValidation.Global(excluded = Seq(Path.Root / "blog" / "feed.rss"))
    )
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
          RawContent(NonEmptySet.of("html"), Icons("fa-link"))
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
      },
      TemplateDirectives.create("svg") {
        import TemplateDirectives.dsl.*
        attribute(0).as[String].map { icon =>
          TemplateElement(
            RawContent(NonEmptySet.of("html", "rss"), Icons(icon))
          )
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

  val Icons = {
    def loadFaIcon(prefix: String, name: String) = {
      val resourcePath =
        "/META-INF/resources/webjars/fortawesome__fontawesome-free/7.1.0"
      val inputStream =
        getClass.getResourceAsStream(s"$resourcePath/svgs/$prefix/$name.svg")
      String(inputStream.readAllBytes())
    }

    Map(
      // brands
      "fa-bluesky" -> loadFaIcon("brands", "bluesky"),
      "fa-discord" -> loadFaIcon("brands", "discord"),
      "fa-github" -> loadFaIcon("brands", "github"),
      "fa-linkedin" -> loadFaIcon("brands", "linkedin"),
      "fa-mastodon" -> loadFaIcon("brands", "mastodon"),
      "fa-youtube" -> loadFaIcon("brands", "youtube"),
      // solids
      "fa-book" -> loadFaIcon("solid", "book"),
      "fa-envelope" -> loadFaIcon("solid", "envelope"),
      "fa-globe" -> loadFaIcon("solid", "globe"),
      "fa-hand-holding-heart" -> loadFaIcon("solid", "hand-holding-heart"),
      "fa-link" -> loadFaIcon("solid", "link"),
      "fa-magnifying-glass" -> loadFaIcon("solid", "magnifying-glass"),
      "fa-person-chalkboard" -> loadFaIcon("solid", "person-chalkboard"),
      "fa-puzzle-piece" -> loadFaIcon("solid", "puzzle-piece"),
      "fa-square-rss" -> loadFaIcon("solid", "square-rss")
    )
  }
}

object KaTeX {
  import org.graalvm.polyglot.*
  import scala.jdk.CollectionConverters.*

  private def loadKaTeX(): String = {
    val resourcePath = "/META-INF/resources/webjars/katex/0.16.28/dist/katex.js"
    val inputStream = getClass.getResourceAsStream(resourcePath)
    String(inputStream.readAllBytes())
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
      val options = Map(
        "throwOnError" -> true,
        "strict" -> true,
        "displayMode" -> displayMode
      )
      katex.invokeMember("renderToString", latex, options.asJava).asString
    }

}

object Redirects {
  import laika.io.model.InputTree
  import laika.ast.Path.Root

  def inputTree = map.foldLeft(InputTree[IO]) { case (tree, (from, to)) =>
    tree.addString(mkRedirect(to), Root / (from.stripSuffix(".html") + ".md"))
  }

  private def mkRedirect(to: String) =
    s"""{% laika.html.template = "/templates/redirect.template.html", laika.targetFormats: [html], target = "$to" %}"""

  private val map = Map(
    "/about/index.html" -> "/foundation/README.md",
    "/about/contributing/index.html" -> "/foundation/README.md",
    "/blog/2013/04/04/inauguration.html" -> "/blog/inauguration.md",
    "/blog/2013/06/24/deriving-instances-1.html" -> "/blog/deriving-instances-1.md",
    "/blog/2013/07/07/generic-numeric-programming.html" -> "/blog/generic-numeric-programming.md",
    "/blog/2013/09/11/using-scalaz-Unapply.html" -> "/blog/using-scalaz-Unapply.md",
    "/blog/2013/10/13/spires-ops-macros.html" -> "/blog/spires-ops-macros.md",
    "/blog/2013/10/13/towards-scalaz-1.html" -> "/blog/towards-scalaz-1.md",
    "/blog/2013/10/18/treelog.html" -> "/blog/treelog.md",
    "/blog/2013/11/17/discipline.html" -> "/blog/discipline.md",
    "/blog/2013/12/15/towards-scalaz-2.html" -> "/blog/towards-scalaz-2.md",
    "/blog/2014/01/18/implicitly_existential.html" -> "/blog/implicitly-existential.md",
    "/blog/2014/02/21/error-handling.html" -> "/blog/error-handling.md",
    "/blog/2014/03/09/liskov_lifting.html" -> "/blog/liskov-lifting.md",
    "/blog/2014/04/14/fix.html" -> "/blog/fix.md",
    "/blog/2014/06/22/mapping-sets.html" -> "/blog/mapping-sets.md",
    "/blog/2014/07/02/type_equality_to_leibniz.html" -> "/blog/type-equality-to-leibniz.md",
    "/blog/2014/07/06/singleton_instance_trick_unsafe.html" -> "/blog/singleton-instance-trick-unsafe.md",
    "/blog/2014/09/02/typelevel-scala.html" -> "/blog/typelevel-scala.md",
    "/blog/2014/09/20/higher_leibniz.html" -> "/blog/higher-leibniz.md",
    "/blog/2014/11/10/why_is_adt_pattern_matching_allowed.html" -> "/blog/why-is-adt-pattern-matching-allowed.md",
    "/blog/2015/02/26/rawtypes.html" -> "/blog/rawtypes.md",
    "/blog/2015/07/13/type-members-parameters.html" -> "/blog/type-members-parameters.md",
    "/blog/2015/07/16/method-equiv.html" -> "/blog/method-equiv.md",
    "/blog/2015/07/19/forget-refinement-aux.html" -> "/blog/forget-refinement-aux.md",
    "/blog/2015/07/23/type-projection.html" -> "/blog/type-projection.md",
    "/blog/2015/07/27/nested-existentials.html" -> "/blog/nested-existentials.md",
    "/blog/2015/07/30/values-never-change-types.html" -> "/blog/values-never-change-types.md",
    "/blog/2015/08/06/machinist.html" -> "/blog/machinist.md",
    "/blog/2015/08/07/symbolic-operators.html" -> "/blog/symbolic-operators.md",
    "/blog/2015/09/21/change-values.html" -> "/blog/change-values.md",
    "/blog/2015/12/11/announcement_summit.html" -> "/blog/announcement-summit.md",
    "/blog/2016/01/14/summit_assistance.html" -> "/blog/summit-assistance.md",
    "/blog/2016/01/20/summit_keynote.html" -> "/blog/summit-keynote.md",
    "/blog/2016/01/28/existential-inside.html" -> "/blog/existential-inside.md",
    "/blog/2016/01/28/summit_programme.html" -> "/blog/summit-programme.md",
    "/blog/2016/02/04/variance-and-functors.html" -> "/blog/variance-and-functors.md",
    "/blog/2016/03/13/information-hiding.html" -> "/blog/information-hiding.md",
    "/blog/2016/03/24/typelevel-boulder.html" -> "/blog/typelevel-boulder.md",
    "/blog/2016/05/10/internal-state.html" -> "/blog/internal-state.md",
    "/blog/2016/08/21/hkts-moving-forward.html" -> "/blog/hkts-moving-forward.md",
    "/blog/2016/09/19/variance-phantom.html" -> "/blog/variance-phantom.md",
    "/blog/2016/09/21/edsls-part-1.html" -> "/blog/edsls-part-1.md",
    "/blog/2016/09/30/subtype-typeclasses.html" -> "/blog/subtype-typeclasses.md",
    "/blog/2016/10/17/minicheck.html" -> "/blog/minicheck.md",
    "/blog/2016/10/18/scala-center.html" -> "/blog/scala-center.md",
    "/blog/2016/10/26/edsls-part-2.html" -> "/blog/edsls-part-2.md",
    "/blog/2016/11/17/heaps.html" -> "/blog/heaps.md",
    "/blog/2016/12/17/scala-coc.html" -> "/blog/scala-coc.md",
    "/blog/2017/02/13/more-types-than-classes.html" -> "/blog/more-types-than-classes.md",
    "/blog/2017/03/01/four-ways-to-escape-a-cake.html" -> "/blog/four-ways-to-escape-a-cake.md",
    "/blog/2017/04/02/equivalence-vs-equality.html" -> "/blog/equivalence-vs-equality.md",
    "/blog/2017/05/02/io-monad-for-cats.html" -> "/blog/io-monad-for-cats.md",
    "/blog/2017/06/13/libra.html" -> "/blog/libra.md",
    "/blog/2017/06/21/ciris.html" -> "/blog/ciris.md",
    "/blog/2017/08/04/cats-1.0-mf.html" -> "/blog/cats-1.0-mf.md",
    "/blog/2017/09/05/three-types-of-strings.html" -> "/blog/three-types-of-strings.md",
    "/blog/2017/12/20/who-implements-typeclass.html" -> "/blog/who-implements-typeclass.md",
    "/blog/2017/12/25/cats-1.0.0.html" -> "/blog/cats-1.0.0.md",
    "/blog/2017/12/27/optimizing-final-tagless.html" -> "/blog/optimizing-final-tagless.md",
    "/blog/2018/04/13/rethinking-monaderror.html" -> "/blog/rethinking-monaderror.md",
    "/blog/2018/05/09/product-with-serializable.html" -> "/blog/product-with-serializable.md",
    "/blog/2018/05/09/tagless-final-streaming.html" -> "/blog/tagless-final-streaming.md",
    "/blog/2018/06/07/shared-state-in-fp.html" -> "/blog/shared-state-in-fp.md",
    "/blog/2018/06/15/typedapi.html" -> "/blog/typedapi.md",
    "/blog/2018/06/27/optimizing-tagless-final-2.html" -> "/blog/optimizing-tagless-final-2.md",
    "/blog/2018/07/12/testing-in-the-wild.html" -> "/blog/testing-in-the-wild.md",
    "/blog/2018/08/07/refactoring-monads.html" -> "/blog/refactoring-monads.md",
    "/blog/2018/08/25/http4s-error-handling-mtl.html" -> "/blog/http4s-error-handling-mtl.md",
    "/blog/2018/09/04/chain-replacing-the-list-monoid.html" -> "/blog/chain-replacing-the-list-monoid.md",
    "/blog/2018/09/29/monad-transformer-variance.html" -> "/blog/monad-transformer-variance.md",
    "/blog/2018/10/06/intro-to-mtl.html" -> "/blog/intro-to-mtl.md",
    "/blog/2018/11/02/semirings.html" -> "/blog/semirings.md",
    "/blog/2018/11/28/http4s-error-handling-mtl-2.html" -> "/blog/http4s-error-handling-mtl-2.md",
    "/blog/2019/01/30/cats-ecosystem-community-survey-results.html" -> "/blog/cats-ecosystem-community-survey-results.md",
    "/blog/2019/02/06/algebraic-api-design.html" -> "/blog/algebraic-api-design.md",
    "/blog/2019/04/24/typelevel-sustainability-program-announcement.html" -> "/blog/typelevel-sustainability-program-announcement.md",
    "/blog/2019/05/01/typelevel-switches-to-scala-code-of-conduct.html" -> "/blog/typelevel-switches-to-scala-code-of-conduct.md",
    "/blog/2019/05/29/support-typelevel-thanks-to-triplequote-hydra.html" -> "/blog/support-typelevel-thanks-to-triplequote-hydra.md",
    "/blog/2019/09/05/jdg.html" -> "/blog/jdg.md",
    "/blog/2019/11/13/Update-about-sustainability-program.html" -> "/blog/Update-about-sustainability-program.md",
    "/blog/2020/06/17/confronting-racism.html" -> "/blog/confronting-racism.md",
    "/blog/2020/10/30/concurrency-in-ce3.html" -> "/blog/concurrency-in-ce3.md",
    "/blog/2021/02/21/fibers-fast-mkay.html" -> "/blog/fibers-fast-mkay.md",
    "/blog/2021/04/27/community-safety.html" -> "/blog/community-safety.md",
    "/blog/2021/05/05/discord-migration.html" -> "/blog/discord-migration.md",
    "/blog/2021/11/15/on-recent-events.html" -> "/blog/on-recent-events.md",
    "/blog/2022/01/19/governing-documents.html" -> "/blog/governing-documents.md",
    "/blog/2022/04/01/call-for-steering-committee-members.html" -> "/blog/call-for-steering-committee-members.md",
    "/blog/2022/07/25/welcoming-new-steering-committee-members.html" -> "/blog/welcoming-new-steering-committee-members.md",
    "/blog/2022/09/06/new-website-layout.html" -> "/blog/new-website-layout.md",
    "/blog/2022/09/12/tuple-announcement.html" -> "/blog/tuple-announcement.md",
    "/blog/2022/09/19/typelevel-native.html" -> "/blog/typelevel-native.md",
    "/blog/2022/11/10/fabric.html" -> "/blog/fabric.md",
    "/blog/2023/02/23/gsoc.html" -> "/blog/gsoc-2023.md",
    "/blog/2023/04/03/typelevel_toolkit.html" -> "/blog/typelevel-toolkit.md",
    "/blog/2023/11/03/charter-changes.html" -> "/blog/charter-changes.md",
    "/blog/2024/03/02/gsoc.html" -> "/blog/gsoc-2024.md",
    "/blog/2024/03/10/github-seats.html" -> "/blog/github-seats.md",
    "/blog/2024/03/11/code-of-conduct.html" -> "/blog/code-of-conduct.md",
    "/blog/2024/08/24/call-for-code-of-conduct-committee-members.html" -> "/blog/call-for-code-of-conduct-committee-members.md",
    "/blog/2024/11/21/new-code-of-conduct-committee-members.html" -> "/blog/new-code-of-conduct-committee-members.md",
    "/blog/2024/12/22/gsoc24-going-feral-on-the-cloud.html" -> "/blog/gsoc24-going-feral-on-the-cloud.md",
    "/blog/2025/02/21/spotify-foss-fund.html" -> "/blog/spotify-foss-fund.md",
    "/blog/2025/02/27/gsoc.html" -> "/blog/gsoc-2025.md",
    "/blog/2025/06/10/weaver-test-release.html" -> "/blog/weaver-test-release.md",
    "/blog/2025/08/19/evolving-typelevel.html" -> "/blog/evolving-typelevel.md",
    "/blog/2025/09/02/custom-error-types.html" -> "/blog/custom-error-types.md",
    "/blog/governance/index.html" -> "/blog/README.md",
    "/blog/social/index.html" -> "/blog/README.md",
    "/blog/technical/index.html" -> "/blog/README.md",
    "/blog/summits/index.html" -> "/blog/README.md",
    "/code-of-conduct.html" -> "/code-of-conduct/README.md",
    "/event/2016-03-summit-philadelphia/index.html" -> "/blog/summit-philadelphia-2016-03-02.md",
    "/event/2016-05-summit-oslo/index.html" -> "/blog/summit-oslo-2016-05-04.md",
    "/event/2016-06-hackday/index.html" -> "/blog/hackday-2016-06-11.md",
    "/event/2016-07-hackday/index.html" -> "/blog/hackday-2016-07-16.md",
    "/event/2016-08-hackday/index.html" -> "/blog/hackday-2016-08-13.md",
    "/event/2016-09-conf-cadiz/index.html" -> "/blog/conf-cadiz-2016-09-30.md",
    "/event/2016-09-hackday/index.html" -> "/blog/hackday-2016-09-17.md",
    "/event/2016-09-lake-district-workshop/index.html" -> "/blog/lake-district-workshop-2016-09-14.md",
    "/event/2016-10-hackday/index.html" -> "/blog/hackday-2016-10-15.md",
    "/event/2016-10-scala-io/index.html" -> "/blog/scala-io-2016-10-27.md",
    "/event/2016-11-hackday/index.html" -> "/blog/hackday-2016-11-12.md",
    "/event/2016-12-scalaxhack/index.html" -> "/blog/scalaxhack-2016-12-10.md",
    "/event/2017-01-hackday/index.html" -> "/blog/hackday-2017-01-21.md",
    "/event/2017-03-summit-nyc/index.html" -> "/blog/summit-nyc-2017-03-23.md",
    "/event/2017-06-summit-copenhagen/index.html" -> "/blog/summit-copenhagen-2017-06-03.md",
    "/event/2017-10-conf-cadiz/index.html" -> "/blog/conf-cadiz-2017-10-26.md",
    "/event/2018-03-summit-boston/index.html" -> "/blog/summit-boston-2018-03-20.md",
    "/event/2018-05-summit-berlin/index.html" -> "/blog/summit-berlin-2018-05-18.md",
    "/event/2019-04-summit-philadelphia/index.html" -> "/blog/summit-philadelphia-2019-04-01.md",
    "/event/2019-06-summit-lausanne/index.html" -> "/blog/summit-lausanne-2019-06-14.md",
    "/event/2020-03-summit-nyc/index.html" -> "/blog/summit-nyc-2020-03-12.md",
    "/event/2023-10-summit-nescala/index.html" -> "/blog/summit-nescala-2023-10-26.md",
    "/event/2025-08-meetup-lausanne/index.html" -> "/blog/meetup-lausanne-2025-08-22.md",
    "/events/index.html" -> "/blog/README.md",
    "/gsoc/ideas/index.html" -> "/gsoc/ideas.md",
    "/gsoc/projects/index.html" -> "/gsoc/README.md",
    "/license/index.html" -> "/colophon.md",
    "/platforms/index.html" -> "/projects/README.md",
    "/platforms/js/index.html" -> "/projects/README.md",
    "/platforms/jvm/index.html" -> "/projects/README.md",
    "/platforms/native/index.html" -> "/projects/README.md",
    "/projects/organization/index.html" -> "/projects/README.md",
    "/projects/affiliate/index.html" -> "/projects/README.md",
    "/steering-committee/index.html" -> "/foundation/people.md",
    "/steering-committee.html" -> "/foundation/people.md"
  )
}
