package actors

import akka.actor._
import scala.math._

case class ObjectCreated(obj: ActorRef, state: ObjectState)
case class ObjectMoved(obj: ActorRef, pos: Pos)
case class ObjectDestroyed(obj: ActorRef)

case class EffectCreated(name: String, pos: Pos)

case class PlayerJoined(name: String, player: ActorRef)
case class PlayerSaid(name: String, text: String)
case class PlayerQuited(name: String)

case class MoveByCommand(dist: Double, rot: Double)
case object FireCommand

case class Pos(x: Double, y: Double, rot: Double) {

  def distance(other: Pos) =
    sqrt((x - other.x)*(x - other.x) + (y - other.y)*(y - other.y))

  def lerp(other: Pos, value: Double) =
    Pos(x + (other.x - x) * value, y + (other.y - y) * value, rot + (other.rot - rot) * value)

  def move(dist: Double, drot: Double) = {
    val nrot = rot + drot
    val nx = x + dist * sin(nrot / 180 * Pi)
    val ny = y - dist * cos(nrot / 180 * Pi)

    Pos(nx, ny, nrot)
  }

}

case class ObjectState(typeName: String, owner: String, pos: Pos, radius: Double) {

  def intersects(other: ObjectState) =
    distance(other) < radius + other.radius

  def distance(other: ObjectState) =
    pos.distance(other.pos)

}
