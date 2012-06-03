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

    Crafty.init(1000, 800)
    Crafty.canvas.init()

    Crafty.scene "loading", @defineLoadingScene
    Crafty.scene "main", @defineMainScene

    Crafty.scene "loading"

  send: (data) ->
    @ws.send(JSON.stringify(data))

  receive: (data) ->
    if data.type =='join'
      @players[data.player]?.destroy()
      @players[data.player] = Crafty.e("2D, DOM, Spaceship, Text, PlayerControls").attr(x: data.x, y: data.y, z:1, rotation: data.rot ).origin('center').text(data.player)

      if data.player == @name
        @players[data.player].enableControl().bind('Moved', (m) => @send $.extend({type: 'move'}, m) )

    else if data.type == 'quit'
      @players[data.player]?.destroy()
      @players[data.player] = null
    else if data.type == 'move'
      @players[data.player]?.attr(x: data.x, y: data.y, rotation: data.rot)

  defineMainScene: =>
    Crafty.sprite "assets/images/spaceship.png",
      Spaceship: [0, 0, 54, 80]

    @ws = new WS(@url)
    @ws.onmessage = (e) =>
      # console.log(["Received", JSON.parse(e.data)])
      @receive JSON.parse(e.data)
    @ws.onopen = (e) =>
      @send(type: 'connect')

  defineLoadingScene: =>
    Crafty.load ["assets/images/spaceship.png"], =>
      Crafty.scene("main")

    Crafty.background("#000")
    Crafty.e("2D, DOM, Text").attr(w: 100, h: 20, x: 150, y: 120)
              .text("Loading")
              .css({ "text-align": "center" })
