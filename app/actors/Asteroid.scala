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

case class MoveByCommand(dist: Double, rot: Double)

class Asteroid(world: ActorRef, var pos: Pos) extends Actor {

  def receive = {
    case MoveByCommand(dist, rot) =>
      pos = pos.move(dist, rot)
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
