extends CharacterBody3D
## Геометрия, коллизия и камера сохранены в capsule_player.tscn.
## Скрипт только двигает готового игрока и переключает камеры.

signal location_changed(location_name: String)

@export var walk_speed: float = 3.5
@export var sprint_speed: float = 5.5
@export var acceleration: float = 18.0
@export var jump_velocity: float = 4.5
@export var mouse_sensitivity: float = 0.0025

@onready var visual: Node3D = $Visual
@onready var head: Node3D = $Head
@onready var first_person_camera: Camera3D = $Head/FirstPersonCamera

var first_person: bool = false
var gravity: float = float(ProjectSettings.get_setting("physics/3d/default_gravity", 9.8))
var spawn_transform: Transform3D
var overview_camera: Camera3D


func _ready() -> void:
	spawn_transform = global_transform
	overview_camera = get_tree().get_first_node_in_group("overview_camera") as Camera3D


func enter_location(arrival: Marker3D, camera: Camera3D, location_name: String) -> void:
	# Rooms, cameras and arrival markers already exist in the saved level.
	overview_camera = camera
	spawn_transform = arrival.global_transform
	_reset_position()
	if first_person:
		first_person_camera.make_current()
	else:
		overview_camera.make_current()
	location_changed.emit(location_name)


func _physics_process(delta: float) -> void:
	if global_position.y < -10.0:
		_reset_position()

	if not is_on_floor():
		velocity.y -= gravity * delta
	elif Input.is_action_just_pressed("jump"):
		velocity.y = jump_velocity

	var input_vector := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	var direction := Vector3(input_vector.x, 0.0, input_vector.y)
	# Движение следует взгляду: в зале W направляет к сцене,
	# в кулисах — в глубину текущего обзорного ракурса.
	if first_person:
		direction = global_transform.basis * direction
		direction.y = 0.0
	elif is_instance_valid(overview_camera):
		var forward := -overview_camera.global_basis.z
		forward.y = 0.0
		var right := overview_camera.global_basis.x
		right.y = 0.0
		direction = right.normalized() * input_vector.x - forward.normalized() * input_vector.y

	var speed: float = sprint_speed if Input.is_action_pressed("sprint") else walk_speed
	velocity.x = move_toward(velocity.x, direction.x * speed, acceleration * delta)
	velocity.z = move_toward(velocity.z, direction.z * speed, acceleration * delta)

	if not first_person and direction.length_squared() > 0.001:
		var facing_angle: float = atan2(-direction.x, -direction.z)
		visual.rotation.y = lerp_angle(visual.rotation.y, facing_angle, minf(delta * 12.0, 1.0))

	move_and_slide()


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("toggle_camera"):
		_toggle_camera()
		get_viewport().set_input_as_handled()
	elif event.is_action_pressed("reset_position"):
		_reset_position()
	elif event.is_action_pressed("ui_cancel"):
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	elif first_person and event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
			Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	elif first_person and event is InputEventMouseMotion:
		if Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
			rotation.y -= event.relative.x * mouse_sensitivity
			head.rotation.x = clampf(head.rotation.x - event.relative.y * mouse_sensitivity, deg_to_rad(-75.0), deg_to_rad(75.0))


func _toggle_camera() -> void:
	if first_person:
		if not is_instance_valid(overview_camera):
			return
		first_person = false
		visual.rotation.y = rotation.y
		rotation.y = 0.0
		visual.show()
		overview_camera.make_current()
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	else:
		first_person = true
		rotation.y = visual.rotation.y
		visual.rotation.y = 0.0
		visual.hide()
		first_person_camera.make_current()
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED


func _reset_position() -> void:
	# Возвращаемся в начальную точку, если игрок упал или нажал R.
	global_transform = spawn_transform
	velocity = Vector3.ZERO
	visual.rotation = Vector3.ZERO
	head.rotation = Vector3.ZERO
	if not first_person:
		visual.rotation.y = rotation.y
		rotation.y = 0.0
