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

case class ObjectCreated(obj: ActorRef, state: ObjectState)
case class ObjectMoved(obj: ActorRef, pos: Pos)
case class ObjectDestroyed(obj: ActorRef)

case class EffectCreated(name: String, pos: Pos)

case class PlayerJoined(name: String, player: ActorRef)
case class PlayerSaid(name: String, text: String)
case class PlayerQuited(name: String)

case object CreateAsteroid

class World extends Actor {

  var players = Map[String,ActorRef]()
  var objects = Map[ActorRef,ObjectState]()

  override def preStart = {
    (1 to 5).foreach { i =>
      context.actorOf(Props(new Asteroid(self, Pos(World.minx + World.sizex * random, World.miny + World.sizey * random, 360 * random), 5*random)))
    }

    context.system.scheduler.schedule(3 second, 3 second) {
      self ! CreateAsteroid
    }
  }

  def receive = {

    case CreateAsteroid =>
      val dir = 360 * random

      if ( random > 0.5 ) {
        val y = World.miny + World.sizey * random

        val x = if (sin(dir / 180 * Pi) > 0)
          World.minx - 60
        else
          World.maxx + 60

        context.actorOf(Props(new Asteroid(self, Pos(x, y, dir), 5*random)))
      } else {
        val x = World.minx + World.sizex * random

        val y = if (cos(dir / 180 * Pi) < 0)
          World.miny - 60
        else
          World.maxy + 60

        context.actorOf(Props(new Asteroid(self, Pos(x, y, dir), 5*random)))
      }

    // Player events

    case msg @ PlayerJoined(name, player) =>
      players += name -> player

      sendToPlayers(msg)

      players.withFilter(_._1 != name).foreach { t => player ! PlayerJoined(t._1, t._2) }
      objects.foreach { t => player ! ObjectCreated(t._1, t._2) }

    case msg @ PlayerQuited(name) =>
      players -= name

      sendToPlayers(msg)

    case msg @ PlayerSaid(name, text) =>
      sendToPlayers(msg)

    // Object events

    case msg @ ObjectCreated(obj, state) =>
      objects += obj -> state

      sendToPlayers(msg)

    case msg @ ObjectDestroyed(obj) =>
      objects -= obj

      sendToPlayers(msg)

    case msg @ ObjectMoved(obj, pos) =>
      val objState = objects(obj).copy(pos = pos)

      objects += obj -> objState

      sendToPlayers(msg)

      checkCollisions(obj, objState)
  }

  def checkCollisions(obj: ActorRef, state: ObjectState) {
    for ((otherObj, otherState) <- objects)
        if (obj != otherObj && state.intersects(otherState)) {
          obj ! PoisonPill
          otherObj ! PoisonPill

          sendToPlayers( EffectCreated("Explosion", state.pos.lerp(otherState.pos, state.radius / otherState.radius)) )

          return
        }
  }

  def sendToPlayers(msg : Any) =
    players.values.foreach { _ ! msg }

}

object World {

  lazy val world = Akka.system.actorOf(Props[World])

  def maxx = 1000
  def minx = 0

  def maxy = 1000
  def miny = 0

  def sizex = maxx - minx
  def sizey = maxy - miny
}
