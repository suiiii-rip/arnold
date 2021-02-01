package arnold

import zio._
import com.github.twitch4j.TwitchClientBuilder
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;

import AppConfig._
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.twitch4j.auth.providers.TwitchIdentityProvider

object TwitchClient {

  import com.github.twitch4j.TwitchClient

  type Twitch = Has[TwitchClient]

  val client: ZLayer[AppConfig, TwitchConnectError, Twitch] =
    ZLayer.fromServiceManaged[Config, Any, TwitchConnectError, TwitchClient](
      c => {
        val t = c.twitch
        Managed
          .make(Task {
            TwitchClientBuilder
              .builder()
              .withEnableChat(true)
              .withChatAccount(new OAuth2Credential("twitch", t.authToken))
              .withCredentialManager({
                val cm = CredentialManagerBuilder.builder().build()
                cm.registerIdentityProvider(
                  new TwitchIdentityProvider(
                    t.clientId,
                    t.clientSecret,
                    "http://localhost"
                  )
                )
                cm
              })
                .build()
          })(client => UIO(client.close()))
          .mapError(t => TwitchConnectError("unable to connect to twitch", t))
      }
    )

  case class TwitchConnectError(msg: String, cause: Throwable = null)
      extends RuntimeException(msg, cause)

}
