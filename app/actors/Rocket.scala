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

class Rocket(world: ActorRef, initialPos: Pos, speed: Double) extends StaticObject(world, initialPos, speed) {

  def radius = 5

}
