extends Node3D
## Coordinates fair encounters; bottle progress belongs to BottleHunt and is untouched.

@onready var player = get_parent().get_node("Player")
@onready var hunt: Node = get_parent().get_node("BottleHunt")
@onready var panel: Panel = $HUD/Status
@onready var message: Label = $HUD/Status/Message
@onready var meter: ProgressBar = $HUD/Status/Meter
@onready var warning: PanelContainer = $HUD/Warning
var guards: Array[OrganizerPatrol] = []
var grace_time: float = 5.0
var warning_time: float = 0.0
var encounters: int = 0
var enabled: bool = true


func _ready() -> void:
	for child in get_children():
		if child is OrganizerPatrol:
			guards.append(child)
			child.player_caught.connect(_on_player_caught)
	hunt.hunt_completed.connect(_on_hunt_completed)


func _physics_process(delta: float) -> void:
	grace_time = maxf(0.0, grace_time - delta)
	warning_time = maxf(0.0, warning_time - delta)
	warning.visible = warning_time > 0.0
	var awareness := 0.0
	var chasing := false
	var searching := false
	for guard in guards:
		guard.can_detect = enabled and grace_time <= 0.0
		awareness = maxf(awareness, guard.suspicion)
		chasing = chasing or guard.state == OrganizerPatrol.State.CHASE
		searching = searching or guard.state == OrganizerPatrol.State.SEARCH
	panel.visible = enabled and (awareness > 0.01 or chasing or searching)
	meter.value = awareness
	if chasing:
		message.text = "Тебя заметили! Уйди из поля зрения."
		message.modulate = Color(1, 0.5, 0.4)
	elif searching:
		message.text = "Тебя ищут. Обойди организатора."
		message.modulate = Color(1, 0.83, 0.45)
	else:
		message.text = "Кажется, тебя заметили…"
		message.modulate = Color(1, 0.83, 0.45)


func _on_player_caught(_guard: OrganizerPatrol) -> void:
	if not enabled or grace_time > 0.0:
		return
	encounters += 1
	grace_time = 10.0
	warning_time = 4.5
	player._reset_position()
	for guard in guards:
		guard.can_detect = false
		guard.return_to_route()


func _on_hunt_completed() -> void:
	enabled = false
	warning_time = 0.0
	warning.hide()
	for guard in guards:
		guard.can_detect = false
		guard.return_to_route()
