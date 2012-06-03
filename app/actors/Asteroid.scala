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

class Asteroid(world: ActorRef, var pos: Pos) extends Actor {

  def receive = {
    case MoveCommand(newPos) =>
      pos = newPos
      world ! ObjectMoved(self, pos)
  }

  override def preStart = {
    world ! ObjectCreated(self, ObjectState("Asteroid", null, pos, 60))
  }

  override def postStop = {
    world ! ObjectDestroyed(self)
  }

}
