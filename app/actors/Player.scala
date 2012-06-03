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

case class In(event: String, data: JsValue)
case object Connect
case object Disconnect

class Player(world: ActorRef, name: String, out: PushEnumerator[JsValue]) extends Actor {

  def receive = {
    case Join(playerName, _, Pos(x, y, rot)) =>
      out.push(JsObject(List("type" -> JsString("join"), "player" -> JsString(playerName), "x" -> JsNumber(x), "y" -> JsNumber(y), "rot" -> JsNumber(rot))))

    case Quit(playerName) =>
      out.push(JsObject(List("type" -> JsString("quit"), "player" -> JsString(playerName))))

    case Say(playerName, message) =>
      out.push(JsObject(List("type" -> JsString("chat"), "player" -> JsString(playerName), "message" -> JsString(message))))

    case Move(playerName, Pos(x, y, rot)) =>
      out.push(JsObject(List("type" -> JsString("move"), "player" -> JsString(playerName), "x" -> JsNumber(x), "y" -> JsNumber(y), "rot" -> JsNumber(rot))))

    case In("connect", data) =>
      world ! Join(name, self, Pos(400, 400, -30))

    case In("disconnect", data) =>
      world ! Quit(name)
      context.stop(self)

    case In("chat", data) =>
      world ! Say(name, (data \ "message").as[String])

    case In("move", data) =>
      world ! Move(name, Pos((data \ "x").as[Double], (data \ "y").as[Double], (data \ "rot").as[Double]))

  }

}

object Player {

  implicit val timeout = Timeout(1 second)

  def join(name: String) = {
    val out = Enumerator.imperative[JsValue]()
    val player = Akka.system.actorOf(Props(new Player(World.world, name, out)))
    val in = Iteratee.foreach[JsValue] { data =>
      player ! In((data \ "type").as[String], data)
    }.mapDone { _ =>
      player ! In("disconnect", JsNull)
    }

    (in, out)
  }

}
