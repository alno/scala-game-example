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

case class Join(name: String, player: ActorRef, pos: Pos)
case class Say(name: String, text: String)
case class Quit(name: String)
case class Move(name: String, pos: Pos)

class World extends Actor {

  var players = Map[String,ActorRef]()
  var positions = Map[String,Pos]()

  def receive = {
    case msg @ Join(joinedName, joinedActor, joinedPos) =>
      players += joinedName -> joinedActor
      positions += joinedName -> joinedPos

      joinedActor ! msg

      for ( (name, actor) <- players if name != joinedName) {
        actor ! msg
        joinedActor ! Join(name, actor, positions(name))
      }

    case msg @ Quit(quitedName) =>
      players -= quitedName
      positions -= quitedName

      players.values.foreach { actor =>
        actor ! msg
      }

    case msg : Move =>
      positions += msg.name -> msg.pos

      players.values.foreach { actor =>
        actor ! msg
      }

    case msg : Say =>
      players.values.foreach { actor =>
        actor ! msg
      }
  }

}

object World {

  lazy val world = Akka.system.actorOf(Props[World])

}
