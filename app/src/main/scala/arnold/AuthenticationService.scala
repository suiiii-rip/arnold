package arnold

import zio._
import AppConfig._

object AuthenticationService {

  type AuthenticationService = Has[AuthenticationService.Service]

  trait Service {
    def authenticate(name: String, password: String): Task[Option[User]]
  }

  val default: URLayer[AppConfig, AuthenticationService] =
    ZLayer.fromService[Config, Service] { config =>
      new Service {
        val c = config.server
        val user = User(c.username, c.password)

        override def authenticate(
            name: String,
            password: String
        ): Task[Option[User]] =
          Task {
            Option(User(name, password))
              .filter(user.==)
          }
      }
    }

  def authentcate(
      name: String,
      password: String
  ): RIO[AuthenticationService, Option[User]] =
    RIO.accessM(_.get.authenticate(name, password))

}
