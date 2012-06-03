Crafty.c "PlayerControls",

  init: ->
    @requires 'Keyboard'

  enableControl: ()->
    @bind("EnterFrame", @onEnterFrame)
    @bind("KeyDown", @onKeyDown)
    @speed = 3
    @

  onEnterFrame: (e) ->
    rot = 0
    dist = 0

    if @isDown('LEFT_ARROW')
      rot = -2

    if @isDown('RIGHT_ARROW')
      rot = 2

    if @isDown('UP_ARROW')
      dist = @speed

    if dist != 0 or rot != 0
      @rotation += rot
      @x += dist * Math.sin(@rotation / 180 * Math.PI)
      @y -= dist * Math.cos(@rotation / 180 * Math.PI)
      @trigger('MoveRequest', { dist: dist, rot: rot })

  onKeyDown: (e) ->
    if e.key == 32
      @trigger('FireRequest')

class @Game

  WS = if window['MozWebSocket']
    MozWebSocket
  else
    WebSocket

  constructor: (@name, @url)->
    @players = {}
    @objects = {}

    Crafty.init(1000, 800)
    Crafty.canvas.init()

    Crafty.scene "loading", @defineLoadingScene
    Crafty.scene "main", @defineMainScene

    Crafty.scene "loading"

  send: (data) ->
    @ws.send(JSON.stringify(data))

  receive: (data) ->
    if data.type =='join'
      console.log("Player #{data.player} joined")
    else if data.type == 'quit'
      console.log("Player #{data.player} quited")
    else if data.type == 'create'
      console.log("Object #{data.object} of type #{data.objectType} created")

      @objects[data.object]?.destroy()

      if data.owner == @name
        @objects[data.object] = Crafty.e("2D, DOM, #{data.objectType}, Text, PlayerControls")
          .enableControl()
          .bind('MoveRequest', (m) => @send $.extend({type: 'move'}, m) )
          .bind('FireRequest', (m) => @send $.extend({type: 'fire'}, m) )
      else
        @objects[data.object] = Crafty.e("2D, DOM, #{data.objectType}, Text")

      obj = @objects[data.object]
      obj.origin('center').attr(x: data.x - obj.w / 2, y: data.y - obj.h / 2, z:1, rotation: data.rot ).text(data.owner)

    else if data.type == 'destroy'
      console.log("Object #{data.object} destroyed")

      @objects[data.object]?.destroy()
      @objects[data.object] = null
    else if data.type == 'move'
      obj = @objects[data.object]
      obj?.attr(x: data.x - obj.w / 2, y: data.y - obj.h / 2, rotation: data.rot)
    else if data.type == 'effect'
      Crafty.e("2D, DOM, #{data.effectType}, SpriteAnimation").origin('center').attr(x: data.x, y: data.y, z:1, rotation: data.rot ).animate('Effect', [[0,0],[1,0],[2,0],[3,0],[0,1],[1,1],[2,1],[3,1],[0,2],[1,2],[2,2],[3,2],[0,3],[1,3],[2,3],[3,3]]).animate('Effect', 16)
      console.log("Effect created")

  defineMainScene: =>
    Crafty.sprite "assets/images/spaceship.png",
      Spaceship: [0, 0, 54, 80]

    Crafty.sprite "assets/images/asteroid.png",
      Asteroid: [0, 0, 130, 130]

    Crafty.sprite 128, "assets/images/explosion.png",
      Explosion: [0, 0]

    Crafty.sprite "assets/images/rocket.png",
      Rocket: [0, 0, 10, 22]

    @ws = new WS(@url)
    @ws.onmessage = (e) =>
      # console.log(["Received", JSON.parse(e.data)])
      @receive JSON.parse(e.data)
    @ws.onopen = (e) =>
      @send(type: 'connect')
      @send(type: 'start')

  defineLoadingScene: =>
    Crafty.load ["assets/images/spaceship.png", "assets/images/asteroid.png", "assets/images/explosion.png"], =>
      Crafty.scene("main")

    Crafty.background("#000")
    Crafty.e("2D, DOM, Text").attr(w: 100, h: 20, x: 150, y: 120)
              .text("Loading")
              .css({ "text-align": "center" })
