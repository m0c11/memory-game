class_name OrganizerPatrol
extends CharacterBody3D

signal player_caught(organizer: OrganizerPatrol)

enum State { PATROL, CHASE, SEARCH, RETURN }

@export var route_path: NodePath
@export var patrol_speed: float = 1.8
@export var chase_speed: float = 3.2
@export var vision_distance: float = 12.0
@export var vision_angle: float = 100.0
@export var recognition_seconds: float = 1.5
@export var search_seconds: float = 5.0
@export var catch_distance: float = 1.15

@onready var agent: NavigationAgent3D = $NavigationAgent3D
@onready var animation: AnimationPlayer = $AnimationPlayer
@onready var player: CharacterBody3D = get_tree().get_first_node_in_group("player")
@onready var route: Node3D = get_node(route_path)

var state: State = State.PATROL
var suspicion: float = 0.0
var can_detect: bool = false
var waypoint: int = 0
var visited_waypoints: int = 0
var last_seen: Vector3
var search_time: float = 0.0
var pause_time: float = 0.0
var repath_time: float = 0.0
var blocked_time: float = 0.0
var gravity: float = float(ProjectSettings.get_setting("physics/3d/default_gravity", 9.8))


func sees_player() -> bool:
	if not is_instance_valid(player):
		return false
	var eye: Vector3 = $Eyes.global_position
	var chest: Vector3 = player.global_position + Vector3(0, 1.0, 0)
	var offset := chest - eye
	if offset.length() > vision_distance or offset.length() < 0.01:
		return false
	if (-global_basis.z).dot(offset.normalized()) < cos(deg_to_rad(vision_angle * 0.5)):
		return false
	var query := PhysicsRayQueryParameters3D.create(eye, chest, 1, [get_rid()])
	var hit := get_world_3d().direct_space_state.intersect_ray(query)
	return hit.is_empty() or hit.collider == player


func _physics_process(delta: float) -> void:
	if NavigationServer3D.map_get_iteration_id(agent.get_navigation_map()) == 0:
		return
	var visible_player := can_detect and sees_player()
	if visible_player:
		last_seen = player.global_position
		suspicion = minf(1.0, suspicion + delta / recognition_seconds)
		if suspicion >= 1.0:
			state = State.CHASE
			search_time = search_seconds
	else:
		suspicion = maxf(0.0, suspicion - delta * 0.65)
		if state == State.CHASE:
			state = State.SEARCH
			search_time = search_seconds
	if state == State.SEARCH:
		search_time -= delta
		if search_time <= 0.0:
			return_to_route()
	if state == State.CHASE and visible_player:
		if global_position.distance_to(player.global_position) < catch_distance:
			player_caught.emit(self)
			return

	var direction := Vector3.ZERO
	var speed := patrol_speed
	if visible_player and suspicion < 1.0 and state in [State.PATROL, State.RETURN]:
		face(last_seen - global_position, delta)
	else:
		var destination := last_seen
		if state in [State.PATROL, State.RETURN]:
			destination = route.get_child(waypoint).global_position
			if horizontal_distance(global_position, destination) < 0.65:
				state = State.PATROL
				waypoint = (waypoint + 1) % route.get_child_count()
				visited_waypoints += 1
				pause_time = 1.3
				destination = route.get_child(waypoint).global_position
		elif state == State.CHASE:
			speed = chase_speed
		pause_time = maxf(0.0, pause_time - delta)
		repath_time -= delta
		if repath_time <= 0.0 or agent.target_position.distance_to(destination) > 1.0:
			agent.target_position = destination
			repath_time = 0.35
		if pause_time <= 0.0 and not agent.is_navigation_finished():
			direction = agent.get_next_path_position() - global_position
			direction.y = 0.0
			direction = direction.normalized()
			face(direction, delta)
		elif state == State.SEARCH:
			rotation.y += delta * 0.6

	velocity.x = move_toward(velocity.x, direction.x * speed, delta * 10.0)
	velocity.z = move_toward(velocity.z, direction.z * speed, delta * 10.0)
	if not is_on_floor():
		velocity.y -= gravity * delta
	else:
		velocity.y = 0.0
	var before := global_position
	move_and_slide()
	# Dynamic collisions can temporarily block a route. Retry without teleporting.
	if direction.length_squared() > 0.5 and global_position.distance_to(before) < delta * 0.15:
		blocked_time += delta
		if blocked_time > 3.0:
			repath_time = 0.0
			if state == State.PATROL:
				waypoint = (waypoint + 1) % route.get_child_count()
			blocked_time = 0.0
	else:
		blocked_time = 0.0
	if Vector2(velocity.x, velocity.z).length() > 0.1:
		if not animation.is_playing():
			animation.play("walk")
	else:
		animation.stop()
		$Model.position = Vector3.ZERO


func face(direction: Vector3, delta: float) -> void:
	if Vector2(direction.x, direction.z).length_squared() > 0.001:
		rotation.y = lerp_angle(rotation.y, atan2(-direction.x, -direction.z), minf(5.0 * delta, 1.0))


func return_to_route() -> void:
	suspicion = 0.0
	state = State.RETURN
	pause_time = 0.0
	repath_time = 0.0
	velocity = Vector3.ZERO
	var nearest := INF
	for i in range(route.get_child_count()):
		var distance := global_position.distance_to(route.get_child(i).global_position)
		if distance < nearest:
			nearest = distance
			waypoint = i


func horizontal_distance(a: Vector3, b: Vector3) -> float:
	return Vector2(a.x - b.x, a.z - b.z).length()
