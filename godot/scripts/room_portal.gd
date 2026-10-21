extends Area3D
## A doorway moves the existing player between authored room scenes.

@export var destination_path: NodePath
@export var camera_path: NodePath
@export var location_name: String = "Актовый зал"
@export var prompt_text: String = "E — вернуться в зал"

var visitor: CharacterBody3D
var travel_pending: bool = false

@onready var prompt: Control = $Prompt/Panel


func _ready() -> void:
	$Prompt/Panel/Text.text = prompt_text
	body_entered.connect(_on_body_entered)
	body_exited.connect(_on_body_exited)
	prompt.hide()


func _on_body_entered(body: Node3D) -> void:
	if body is CharacterBody3D and body.is_in_group("player"):
		visitor = body
		prompt.show()


func _on_body_exited(body: Node3D) -> void:
	if body == visitor:
		visitor = null
		prompt.hide()


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("interact") and not event.is_echo():
		if is_instance_valid(visitor) and not travel_pending:
			travel_pending = true
			get_viewport().set_input_as_handled()
			call_deferred("_travel", visitor)


func _travel(player: CharacterBody3D) -> void:
	var destination := get_node_or_null(destination_path) as Marker3D
	var camera := get_node_or_null(camera_path) as Camera3D
	if is_instance_valid(player) and destination != null and camera != null:
		player.call("enter_location", destination, camera, location_name)
		visitor = null
		prompt.hide()
	else:
		push_error("Doorway is missing its destination or camera: " + str(get_path()))
	travel_pending = false
