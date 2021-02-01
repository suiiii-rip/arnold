package arnold

import zio._
import zio.logging._
import TwitchClient._
import CommandService._
import AppConfig._

import java.util.function.Consumer
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import zio.Exit.Success
import zio.Exit.Failure

import scala.language.reflectiveCalls

object TwitchChat {

  type Tc = com.github.twitch4j.chat.TwitchChat

  type Chat = Has[TwitchChat.Service]

  trait Service {}

  val default: ZLayer[
    Twitch with CommandService with Logging with AppConfig,
    Nothing,
    Chat
  ] =
    ZLayer
      .fromServicesManaged[
        TwitchClient,
        CommandService.Service,
        Config,
        Logger[String],
        Any,
        Nothing,
        TwitchChat.Service
      ]((client, commandService, config, log) => {
        Managed.make(
          for {
            rts <- Task.runtime
            _ <- log.info(s"Starting chat")
            channel = config.twitch.channel
            // errors occuring here are considered fatal
            chat = client.getChat()
            service <- UIO {
              new Service {

                def disconnect(): UIO[Unit] = {
                  UIO(chat.disconnect)
                }

                def handler(): UIO[Consumer[ChannelMessageEvent]] = UIO {

                  new Consumer[ChannelMessageEvent] {
                    override def accept(e: ChannelMessageEvent): Unit = {
                      val message = Option(e.getMessage())
                      val handle = for {
                        _ <- log.info(s"Handling chat msg ${message}")
                        m = for {
                          msg <- message
                          if msg.headOption == Some('!') && msg.size > 1
                          mm = msg.substring(1).trim()
                          if mm.nonEmpty
                        } yield mm
                        _ <- log.debug(s"Handling command $m")
                        cmd <- m
                          .map(commandService.get(_))
                          .getOrElse(Task(None))
                        _ <- cmd match {
                          case None => UIO.unit
                          case Some(c) =>
                            for {
                              _ <- log.debug(
                                s"Received cmd $c, sending response ${c.message} to $channel"
                              )
                              _ <- Task(chat.sendMessage(channel, c.message))
                              _ <- log.debug(
                                s"Sent msg ${c.message} to $channel"
                              )
                            } yield (())
                        }
                      } yield (())

                      rts.unsafeRunAsync(handle)(exit =>
                        exit match {
                          case Failure(ex) =>
                            rts.unsafeRun(
                              log.error(
                                s"Failed to handle message ${e.getMessage()}",
                                ex
                              )
                            )
                            throw ex.squash
                          case _ =>
                            ()
                        }
                      )
                    }
                  }
                }
              }
            }
            _ <- UIO(chat.connect())
            _ <- UIO(chat.joinChannel(channel))
            handler <- service.handler()
            _ <- UIO {
              chat
                .getEventManager()
                .onEvent(
                  classOf[ChannelMessageEvent],
                  handler
                )
            }
          } yield service
        )(_.disconnect())
      })

}
