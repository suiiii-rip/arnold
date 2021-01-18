package arnold

import zio._

object CommandService {

  type CommandService = Has[CommandService.Service]

  trait Service {
    def list(): Task[List[Command]]
    def get(keyword: String): Task[Option[Command]]
    def set(command: Command): Task[Command]
  }

  val inMemory: ULayer[CommandService] = ZLayer.fromEffect {
    Ref
      .make(Map[String, Command]())
      .map(ref =>
        new Service {

          override def list(): Task[List[Command]] =
            ref.get.map(m => m.values.toList)

          override def get(keyword: String): Task[Option[Command]] =
            ref.get.map(m => m.get(keyword))

          // TODO check command
          override def set(command: Command): Task[Command] = ref
            .update(m => m.updated(command.keyword, command))
            .map(_ => command)

        }
      )
  }

  def list(): RIO[CommandService, List[Command]] = RIO.accessM(_.get.list())

  def get(keyword: String): RIO[CommandService, Option[Command]] =
    RIO.accessM(_.get.get(keyword))

  def set(cmd: Command): RIO[CommandService, Command] =
    RIO.accessM(_.get.set(cmd))

}
