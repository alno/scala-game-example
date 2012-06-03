package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._

import actors.Player

object Application extends Controller {

  def index(username: Option[String]) = Action { implicit request =>
    Ok(views.html.index(username))
  }

  def connect(username: String) = WebSocket.using[JsValue] { request =>
    Player.join(username)
  }

}
