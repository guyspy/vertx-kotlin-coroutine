package example

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.TemplateHandler
import io.vertx.ext.web.impl.Utils
import io.vertx.ext.web.templ.HandlebarsTemplateEngine
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory


class App : CoroutineVerticle() {

  private val logger = LoggerFactory.getLogger(App::class.java)

  private lateinit var engine: HandlebarsTemplateEngine


  override suspend fun start() {

    engine = HandlebarsTemplateEngine.create()

    val router = Router.router(vertx)
    router.get("/").coroutineHandler { ctx -> home(ctx) }

    router.route("/static/*").handler(StaticHandler.create())

    // Start the server
    awaitResult<HttpServer> {
      vertx.createHttpServer()
          .requestHandler(router::accept)
          .listen(config.getInteger("http.port", 9000), it)
    }
  }

  suspend fun home(ctx: RoutingContext) {
    ctx.put("tmplName", "home.hbs")
    templateHandler(ctx)
  }

  suspend fun templateHandler(ctx: RoutingContext) {
    // put template path
    val file = ctx.get<String>("tmplName")
    val buffer = awaitResult<Buffer> {
      engine.render(ctx, TemplateHandler.DEFAULT_TEMPLATE_DIRECTORY, Utils.normalizePath(file), it)
    }
    ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, TemplateHandler.DEFAULT_CONTENT_TYPE).end(buffer)
  }

}

/**
 * An extension method for simplifying coroutines usage with Vert.x Web routers
 */
fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
  handler { ctx ->
    launch(ctx.vertx().dispatcher()) {
      try {
        fn(ctx)
      } catch (e: Exception) {
        ctx.fail(e)
      }
    }
  }
}
