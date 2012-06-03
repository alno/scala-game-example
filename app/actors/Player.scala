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

sealed trait PlayerState

class Player(world: ActorRef, name: String, out: PushEnumerator[JsValue]) extends Actor with FSM[PlayerState, Option[ActorRef]] {

  case object Disconnected extends PlayerState
  case object Connected extends PlayerState

  import FSM._

  startWith(Disconnected, None)

  when(Disconnected) {

    case Event(In("connect", data), _) =>
      world ! PlayerJoined(name, self)
      goto(Connected) using None

    case Event(In("disconnect", data), _) =>
      stop

  }

  when(Connected) {

    case Event(In("chat", data), _) =>
      world ! PlayerSaid(name, (data \ "message").as[String])
      stay

    case Event(In("disconnect", data), _) =>
      world ! PlayerQuited(name)
      stop

    case Event(In("start", data), None) =>
      val ship = context.actorOf(Props(new Ship(world, name, Pos(400, 400, -30))))
      stay using Some( ship )

    case Event(In("finish", data), Some(ship)) =>
      ship ! PoisonPill
      stay using None

    case Event(In("move", data), Some(ship)) =>
      ship ! MoveCommand(Pos((data \ "x").as[Double], (data \ "y").as[Double], (data \ "rot").as[Double]))
      stay

    case Event(PlayerJoined(playerName, _), _) =>
      out.push(JsObject(List("type" -> JsString("join"), "player" -> JsString(playerName))))
      stay

    case Event(PlayerQuited(playerName), _) =>
      out.push(JsObject(List("type" -> JsString("quit"), "player" -> JsString(playerName))))
      stay

    case Event(PlayerSaid(playerName, message), _) =>
      out.push(JsObject(List("type" -> JsString("chat"), "player" -> JsString(playerName), "message" -> JsString(message))))
      stay

    case Event(ObjectCreated(obj, owner, Pos(x,y,rot)), _) =>
      out.push(JsObject(List("type" -> JsString("create"), "object" -> JsString(obj.toString), "owner" -> JsString(owner), "x" -> JsNumber(x), "y" -> JsNumber(y), "rot" -> JsNumber(rot))))
      stay

    case Event(ObjectDestroyed(obj), _) =>
      out.push(JsObject(List("type" -> JsString("destroy"), "object" -> JsString(obj.toString))))
      stay

    case Event(ObjectMoved(obj, Pos(x,y,rot)), _) =>
      out.push(JsObject(List("type" -> JsString("move"), "object" -> JsString(obj.toString), "x" -> JsNumber(x), "y" -> JsNumber(y), "rot" -> JsNumber(rot))))
      stay
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
