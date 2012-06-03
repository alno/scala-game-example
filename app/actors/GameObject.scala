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
