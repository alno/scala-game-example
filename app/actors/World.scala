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

case class Pos(x: Double, y: Double, rot: Double)

case class ObjectState(typeName: String, owner: String, pos: Pos)

case class ObjectCreated(obj: ActorRef, state: ObjectState)
case class ObjectMoved(obj: ActorRef, pos: Pos)
case class ObjectDestroyed(obj: ActorRef)

case class PlayerJoined(name: String, player: ActorRef)
case class PlayerSaid(name: String, text: String)
case class PlayerQuited(name: String)

class World extends Actor {

  var players = Map[String,ActorRef]()
  var objects = Map[ActorRef,ObjectState]()

  def receive = {

    // Player events

    case msg @ PlayerJoined(name, player) =>
      players += name -> player

      players.values.foreach { _ ! msg }

      players.withFilter(_._1 != name).foreach { t => player ! PlayerJoined(t._1, t._2) }
      objects.foreach { t => player ! ObjectCreated(t._1, t._2) }

    case msg @ PlayerQuited(name) =>
      players -= name

      players.values.foreach { _ ! msg }

    case msg @ PlayerSaid(name, text) =>
      players.values.foreach { _ ! msg }

    // Object events

    case msg @ ObjectCreated(obj, state) =>
      objects += obj -> state

      players.values.foreach { _ ! msg }

    case msg @ ObjectDestroyed(obj) =>
      objects -= obj

      players.values.foreach { _ ! msg }

    case msg @ ObjectMoved(obj, pos) =>
      objects += obj -> objects(obj).copy(pos = pos)

      players.values.foreach { _ ! msg }

  }

}

object World {

  lazy val world = Akka.system.actorOf(Props[World])

}
