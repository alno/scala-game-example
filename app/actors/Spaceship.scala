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

case object FireCommand

class Spaceship(world: ActorRef, owner: String, var pos: Pos) extends Actor {

  def receive = {
    case MoveByCommand(dist, rot) =>
      pos = pos.move(dist, rot)
      world ! ObjectMoved(self, pos)
    case FireCommand =>
      context.actorOf(Props(new Rocket(world, pos.move(60, 0), 20)))
  }

  override def preStart = {
    world ! ObjectCreated(self, ObjectState("Spaceship", owner, pos, 30))
  }

  override def postStop = {
    world ! ObjectDestroyed(self)
  }

}
