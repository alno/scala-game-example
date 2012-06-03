package actors

import akka.actor._
import akka.util.duration._

abstract class GameObject(world: ActorRef) extends Actor {

  def pos: Pos

  def radius: Double

  def objectType: String = getClass.getSimpleName

  def owner: String

  override def preStart = {
    world ! ObjectCreated(self, ObjectState(objectType, owner, pos, radius))
  }

  override def postStop = {
    world ! ObjectDestroyed(self)
  }

}

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

class Spaceship(world: ActorRef, val owner: String, var pos: Pos) extends GameObject(world) {

  def radius = 30

  def receive = {
    case MoveByCommand(dist, rot) =>
      pos = pos.move(dist, rot)
      world ! ObjectMoved(self, pos)
    case FireCommand =>
      context.actorOf(Props(new Rocket(world, pos.move(60, 0), 20)))
  }

}

class Rocket(world: ActorRef, initialPos: Pos, speed: Double) extends StaticObject(world, initialPos, speed) {

  def radius = 5

}

class Asteroid(world: ActorRef, initialPos: Pos, speed: Double) extends StaticObject(world, initialPos, speed) {

  def radius = 60

}
