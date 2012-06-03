package actors

import akka.actor._
import akka.util.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import scala.math._

case class MoveByCommand(dx: Double, dy: Double, drot: Double)

class Asteroid(world: ActorRef, var pos: Pos) extends Actor {

  def receive = {
    case MoveByCommand(dx, dy, drot) =>
      pos = pos.copy(x = pos.x + dx, y = pos.y + dy, rot = pos.rot + drot)
      world ! ObjectMoved(self, pos)
  }

  override def preStart = {
    world ! ObjectCreated(self, ObjectState("Asteroid", null, pos, 60))

    /*context.system.scheduler.schedule(0.1 second, 0.1 second) {
      self ! MoveByCommand(random * 4 - 2, random * 4 - 2, random * 2 - 1)
    }*/
  }

  override def postStop = {
    world ! ObjectDestroyed(self)
  }

}
