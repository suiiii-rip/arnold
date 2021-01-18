package arnold

import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.Request

import zio.interop.catz._
import zio.stream.interop.fs2z._
import zio._
import cats.data.Kleisli
import zio.logging._

import io.circe.generic.auto._
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

import AuthenticationService._
import CommandService._

import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes
import cats.data.OptionT
import org.http4s.BasicCredentials
import org.http4s.server.middleware.authentication.BasicAuth

trait ApiRoutes[R <: CommandService with AuthenticationService with Logging] {

  type ApiTask[A] = RIO[R, A]
  type Err = String

  implicit def circeJsonDecoder[A](implicit
      decoder: Decoder[A]
  ): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]
  implicit def circeJsonEncoder[A](implicit
      decoder: Encoder[A]
  ): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  lazy val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]
  import dsl._

  val authedMiddleware: AuthMiddleware[ApiTask, User] =
    BasicAuth("mediaspyy", b => authentcate(b.username, b.password))

  def routes: HttpRoutes[ApiTask] = authedMiddleware(authedRoutes)

  def authedRoutes = AuthedRoutes.of[User, ApiTask] {
    case GET -> Root / "command" as user =>
      list().foldM(t => InternalServerError(t.getMessage()), l => Ok(l))
    case req @ PUT -> Root / "command" / command as user => {
      val txt = req.req.bodyText.toZStream().fold("")((s, s1) => s + s1)

      val res = for {
        text <- txt
        cmd = Command(command, text)
        _ <- log.info(s"Adding command $cmd")
        _ <- set(cmd)
      } yield (())

      res.foldM(t => InternalServerError(t.getMessage()), _ => Ok())
    }
  }

}
