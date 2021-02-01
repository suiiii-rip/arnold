package arnold

import zio._
import zio.logging._
import zio.logging.slf4j._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

import AuthenticationService._
import CommandService._
import TwitchClient._
import TwitchChat._
import zio.clock.Clock
import arnold.AppConfig._

object MainApp extends zio.App {

  type AppEnv =
    CommandService
      with Chat
      with AuthenticationService
      with AppConfig
      with Clock
      with Logging

  type AppTask[A] = RIO[AppEnv, A]

  val appEnv = {
    import zio.ZLayer._

    val loggingEnv = Slf4jLogger.make((ctx, msg) => msg)

    val baseEnv = requires[Clock] ++ loggingEnv ++ AppConfig.fromEnv

    val chatEnv = requires[AppConfig] ++ requires[CommandService] ++
      requires[Logging] >+>
      TwitchClient.client >+> TwitchChat.default

    baseEnv >+>
      CommandService.inMemory >+>
      chatEnv >+>
      AuthenticationService.default
  }

  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {
    val ex = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4));

    (
      for {
        _ <- log.info("Statup")
        httpApp = Router[AppTask](
          "/" -> new ApiRoutes[AppEnv] {}.routes
        ).orNotFound
        server <- ZIO.runtime[AppEnv].flatMap { implicit rts =>
          val config = rts.environment.get[Config]
          BlazeServerBuilder[AppTask](ex)
            .bindHttp(config.server.port, "0.0.0.0")
            .withHttpApp(httpApp)
            .serve
            .compile
            .drain
        }
      } yield server
    )
      .provideCustomLayer(appEnv)
      .exitCode

  }

}
