package arnold

import zio._

object AppConfig {

  type AppConfig = Has[Config]

  case class Config(port: Int, username: String, password: String)

  val hardDefault: ULayer[AppConfig] =
    ZLayer.succeed(
      Config(
        48080,
        "test",
        "test"
      )
    )
}
