Crafty.c "PlayerControls",

  init: ->
    @requires 'Keyboard'

  enableControl: ()->
    @bind("EnterFrame", @onEnterFrame)
    @speed = 3
    @

  onEnterFrame: (e) ->
    moved = false

    if @isDown('LEFT_ARROW')
      @rotation -= 1
      moved = true

    if @isDown('RIGHT_ARROW')
      @rotation += 1
      moved = true

    if @isDown('UP_ARROW')
      @x += @speed * Math.sin(@rotation * (Math.PI / 180))
      @y -= @speed * Math.cos(@rotation * (Math.PI / 180))
      moved = true

    @trigger('Moved', { x: @x, y: @y, rot: @rotation }) if moved

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
      console.log("Object #{data.object} created")

      @objects[data.object]?.destroy()

      if data.owner == @name
        @objects[data.object] = Crafty.e("2D, DOM, Spaceship, Text, PlayerControls").enableControl().bind('Moved', (m) => @send $.extend({type: 'move'}, m) )
      else
        @objects[data.object] = Crafty.e("2D, DOM, Spaceship, Text")

      @objects[data.object].attr(x: data.x, y: data.y, z:1, rotation: data.rot ).origin('center').text(data.owner)

    else if data.type == 'destroy'
      console.log("Object #{data.object} destroyed")

      @objects[data.object]?.destroy()
      @objects[data.object] = null
    else if data.type == 'move'
      @objects[data.object]?.attr(x: data.x, y: data.y, rotation: data.rot)

  defineMainScene: =>
    Crafty.sprite "assets/images/spaceship.png",
      Spaceship: [0, 0, 54, 80]

    @ws = new WS(@url)
    @ws.onmessage = (e) =>
      # console.log(["Received", JSON.parse(e.data)])
      @receive JSON.parse(e.data)
    @ws.onopen = (e) =>
      @send(type: 'connect')
      @send(type: 'start')

  defineLoadingScene: =>
    Crafty.load ["assets/images/spaceship.png"], =>
      Crafty.scene("main")

    Crafty.background("#000")
    Crafty.e("2D, DOM, Text").attr(w: 100, h: 20, x: 150, y: 120)
              .text("Loading")
              .css({ "text-align": "center" })
