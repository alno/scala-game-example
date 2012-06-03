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

abstract class StaticObject(world: ActorRef, initialPos: Pos, speed: Double) extends GameObject(world) {

  var pos: Pos = initialPos

  def owner = null

  def receive = {
    case MoveByCommand(dist, rot) =>
      pos = pos.move(dist, rot)

      if (pos.x < World.minx - radius || pos.y < World.miny - radius || pos.x > World.maxx + radius || pos.y > World.maxy + radius)
        self ! PoisonPill
      else
        world ! ObjectMoved(self, pos)
  }

  override def preStart = {
    super.preStart

    context.system.scheduler.schedule(0.05 second, 0.05 second) {
      self ! MoveByCommand(speed, 0)
    }
  }

}
