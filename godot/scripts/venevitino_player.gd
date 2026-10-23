extends "res://scripts/capsule_player.gd"
## Only player controls. All terrain, furniture, lights and cameras are saved scenes.

@onready var flashlight: SpotLight3D = $Head/FirstPersonCamera/Flashlight
var night_environment: Environment
var night_ambient_color: Color
var night_ambient_energy: float
# Logical input state also works without a window in physics tests.
var interaction_enabled: bool = true

func _ready() -> void:
	super._ready()
	first_person = true
	visual.hide()
	first_person_camera.make_current()
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	var world := get_parent().get_node_or_null("Environment") as WorldEnvironment
	if world:
		night_environment = world.environment
		night_ambient_color = night_environment.ambient_light_color
		night_ambient_energy = night_environment.ambient_light_energy

func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("toggle_flashlight") and not event.is_echo():
		flashlight.visible = not flashlight.visible
		get_viewport().set_input_as_handled()
	else:
		if event.is_action_pressed("ui_cancel"):
			interaction_enabled = false
		elif first_person and event is InputEventMouseButton:
			if event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
				interaction_enabled = true
		super._unhandled_input(event)

func _toggle_camera() -> void:
	super._toggle_camera()
	interaction_enabled = first_person
	# The overview shows the full authored plan; walking uses scene-defined distances.
	for geometry in get_tree().get_nodes_in_group("distance_culled"):
		geometry.visibility_range_end = float(geometry.get_meta("render_distance")) if first_person else 0.0
	if night_environment:
		night_environment.fog_enabled = first_person
		night_environment.ambient_light_energy = night_ambient_energy if first_person else 0.85
		night_environment.ambient_light_color = night_ambient_color if first_person else Color(0.8, 0.8, 0.8)
