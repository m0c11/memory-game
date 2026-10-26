extends Node2D
## Transitions between already-authored locations. No scene or geometry generation.
signal location_changed(location_id: String)

@onready var player: CharacterBody2D = $World/Player
@onready var player_camera: Camera2D = $World/Player/Camera2D
@onready var overview_camera: Camera2D = $OverviewCamera
@onready var location_label: Label = $Interface/Location
@onready var prompt_label: Label = $Interface/Prompt
@onready var overview_label: Label = $Interface/OverviewLegend
var locations: Dictionary = {}
var current_location: Node2D
var overview: bool = false
var transition_cooldown: float = 0.0
var message_remaining: float = 0.0

func _ready() -> void:
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	for location in get_tree().get_nodes_in_group("polet_location"):
		if is_ancestor_of(location):
			locations[str(location.get_meta("location_id"))] = location
	enter_location("camp", "Entrance")

func _process(delta: float) -> void:
	transition_cooldown = maxf(0.0, transition_cooldown - delta)
	message_remaining = maxf(0.0, message_remaining - delta)
	if message_remaining > 0.0:
		return
	var portal := nearest_portal()
	prompt_label.text = "E · " + str(portal.get_meta("prompt")) if portal != null and not overview else ""

func nearest_portal() -> Area2D:
	if not is_instance_valid(current_location):
		return null
	var nearest: Area2D
	var distance := INF
	for candidate in get_tree().get_nodes_in_group("polet_portal"):
		var portal := candidate as Area2D
		if not current_location.is_ancestor_of(portal) or not portal.overlaps_body(player):
			continue
		var next_distance := player.global_position.distance_squared_to(portal.global_position)
		if next_distance < distance:
			distance = next_distance
			nearest = portal
	return nearest

func try_interact() -> void:
	if overview or transition_cooldown > 0.0:
		return
	var portal := nearest_portal()
	if portal == null:
		return
	var link := str(portal.get_meta("link", ""))
	var connections: Dictionary = current_location.get_meta("connections", {})
	if not connections.has(link):
		prompt_label.text = str(portal.get_meta("blocked_message", "Закрыто"))
		message_remaining = 2.5
		return
	var destination: Array = connections[link]
	enter_location(str(destination[0]), str(destination[1]))

func enter_location(location_id: String, marker_name: String) -> bool:
	if not locations.has(location_id):
		push_error("Unknown Polet location: " + location_id)
		return false
	var destination := locations[location_id] as Node2D
	var marker := destination.get_node_or_null("Markers/" + marker_name) as Marker2D
	if marker == null:
		push_error("Missing Polet arrival: " + location_id + "/" + marker_name)
		return false
	if overview:
		set_overview(false)
	for location: Node2D in locations.values():
		location.visible = location == destination
	current_location = destination
	player.global_position = marker.global_position
	player.velocity = Vector2.ZERO
	var bounds: Rect2 = destination.get_meta("camera_bounds")
	bounds.position += destination.global_position
	player_camera.limit_left = roundi(bounds.position.x)
	player_camera.limit_top = roundi(bounds.position.y)
	player_camera.limit_right = roundi(bounds.end.x)
	player_camera.limit_bottom = roundi(bounds.end.y)
	player_camera.reset_smoothing()
	player_camera.make_current()
	player_camera.force_update_scroll()
	location_label.text = "ПОЛЁТ · " + str(destination.get_meta("display_name"))
	prompt_label.text = ""
	message_remaining = 0.0
	transition_cooldown = 0.3
	location_changed.emit(location_id)
	return true

func set_overview(enabled: bool) -> void:
	overview = enabled and current_location == locations.get("camp")
	player.movement_enabled = not overview
	player.velocity = Vector2.ZERO
	overview_label.visible = overview
	$Interface/HeaderBackground.visible = not overview
	location_label.visible = not overview
	$Interface/Controls.visible = not overview
	if overview:
		overview_camera.make_current()
	else:
		player_camera.make_current()
		player_camera.force_update_scroll()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.echo:
		return
	if event.is_action_pressed("interact"):
		try_interact()
	elif event.is_action_pressed("reset_position"):
		enter_location("camp", "Entrance")
	elif event.is_action_pressed("toggle_camera"):
		set_overview(not overview)
	elif event.is_action_pressed("ui_cancel") and overview:
		set_overview(false)
	else:
		return
	get_viewport().set_input_as_handled()
