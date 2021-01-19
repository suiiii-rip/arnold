package arnold

import zio._
import zio.config.magnolia.describe
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config._

object AppConfig {

  type AppConfig = Has[Config]

  private val automaticConfig = descriptor[Config]

  case class Config(server: ServerConfig)

  case class ServerConfig(port: Int, username: String, password: String)

  val hardDefault =
    ZConfig.fromMap(Map(
        "server.port" -> "48080",
        "server.username" -> "test",
        "server.password" -> "test"
      ), automaticConfig, keyDelimiter = Some('.'))

  val fromEnv = ZConfig.fromSystemEnv(configDescriptor = automaticConfig,
    keyDelimiter = Some('_'))
}
