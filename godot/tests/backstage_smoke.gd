extends SceneTree
## Run: godot --headless --path godot --fixed-fps 60 --script res://tests/backstage_smoke.gd

var hall: Node3D
var player: CharacterBody3D
var failures: Array[String] = []
var checks: int = 0


func _initialize() -> void:
	call_deferred("run_checks")


func frames(count: int) -> void:
	for frame in range(count):
		await physics_frame


func report(name: String, passed: bool) -> void:
	checks += 1
	print(("PASS " if passed else "FAIL ") + name + " position=" + str(player.global_position))
	if not passed:
		failures.append(name)


func release_movement() -> void:
	for action in ["move_left", "move_right", "move_forward", "move_back"]:
		Input.action_release(action)


func walk_to(target: Vector3, max_frames: int = 260) -> bool:
	for frame in range(max_frames):
		var offset := target - player.global_position
		offset.y = 0.0
		if offset.length() < 0.14:
			release_movement()
			await frames(12)
			return true
		var direction := offset.normalized() * minf(offset.length() * 2.0, 1.0)
		if player.first_person:
			direction = player.global_basis.inverse() * direction
		else:
			var camera: Camera3D = player.overview_camera
			direction = Basis(Vector3.UP, camera.global_rotation.y).inverse() * direction
		Input.action_press("move_left", maxf(-direction.x, 0.0))
		Input.action_press("move_right", maxf(direction.x, 0.0))
		Input.action_press("move_forward", maxf(-direction.z, 0.0))
		Input.action_press("move_back", maxf(direction.z, 0.0))
		await physics_frame
	release_movement()
	await frames(12)
	return false


func place(location: Vector3) -> void:
	release_movement()
	player.global_position = location
	player.velocity = Vector3.ZERO
	await frames(12)


func key(code: int, hold: int = 3) -> void:
	var down := InputEventKey.new()
	down.physical_keycode = code
	down.keycode = code
	down.pressed = true
	Input.parse_input_event(down)
	await frames(hold)
	var up := InputEventKey.new()
	up.physical_keycode = code
	up.keycode = code
	Input.parse_input_event(up)
	await frames(12)


func run_checks() -> void:
	hall = load("res://scenes/levels/auditorium.tscn").instantiate()
	root.add_child(hall)
	current_scene = hall
	player = hall.get_node("Player")
	var left: Node3D = hall.get_node("Backstage/Left")
	var right: Node3D = hall.get_node("Backstage/Right")
	var main_camera: Camera3D = hall.get_node("Cameras/ReferenceCamera")
	await frames(20)
	report("initial_hall_camera_and_floor", root.get_camera_3d() == main_camera and player.is_on_floor())
	var initial := player.global_position
	await key(KEY_E)
	report("E_away_from_door_does_nothing", player.global_position.distance_to(initial) < 0.05)

	await walk_to(Vector3(0.2, 0, -0.2))
	report("walk_to_left_entrance", await walk_to(Vector3(-7.25, 0, -0.2)))
	report("left_prompt_is_visible", hall.get_node("Passages/LeftEntrance/Prompt/Panel").visible)
	await key(KEY_E, 45)
	report("left_entry_and_no_held_key_bounce", player.global_position.distance_to(left.get_node("Arrival").global_position) < 0.1)
	report("left_camera_and_title", root.get_camera_3d() == left.get_node("OverviewCamera") and "Левая" in hall.get_node("Interface/Title/Text/Subtitle").text)
	report("left_floor", player.is_on_floor())
	report("old_prompt_hidden", not hall.get_node("Passages/LeftEntrance/Prompt/Panel").visible)
	Input.action_press("move_forward")
	await frames(28)
	release_movement()
	await frames(12)
	report("W_follows_left_overview", left.to_local(player.global_position).x < 0.3)
	await key(KEY_R)

	await place(left.global_position + Vector3(-0.5, 0.03, -0.5))
	await walk_to(left.global_position + Vector3(-0.5, 0, -2.2), 75)
	var local_position := left.to_local(player.global_position)
	report("vanity_blocks_player", local_position.z > -1.5 and local_position.z < -1.2)
	await place(left.global_position + Vector3(-2.3, 0.03, 0.2))
	await walk_to(left.global_position + Vector3(-3.5, 0, 0.2), 60)
	local_position = left.to_local(player.global_position)
	report("left_room_wall_blocks", local_position.x > -2.76 and local_position.x < -2.6)
	await key(KEY_R)
	report("R_returns_to_current_room_entrance", player.global_position.distance_to(left.get_node("Arrival").global_position) < 0.1)
	report("walk_to_left_exit", await walk_to(left.global_position + Vector3(2.25, 0, 0)))
	await key(KEY_E)
	report("left_return_to_hall", player.global_position.distance_to(hall.get_node("ReturnPoints/Left").global_position) < 0.1 and root.get_camera_3d() == main_camera)

	await place(Vector3(0.2, 0.03, -0.2))
	report("walk_to_right_entrance", await walk_to(Vector3(7.25, 0, -0.2)))
	await key(KEY_TAB)
	report("first_person_in_hall", player.first_person and root.get_camera_3d() == player.get_node("Head/FirstPersonCamera"))
	await key(KEY_E)
	report("right_entry_preserves_first_person", player.first_person and player.global_position.distance_to(right.get_node("Arrival").global_position) < 0.1)
	report("right_floor_and_prompt", player.is_on_floor() and not hall.get_node("Passages/RightEntrance/Prompt/Panel").visible)
	await key(KEY_TAB)
	report("Tab_uses_right_room_camera", not player.first_person and root.get_camera_3d() == right.get_node("OverviewCamera"))

	await place(right.global_position + Vector3(2.65, 0.03, 1.2))
	await walk_to(right.global_position + Vector3(1.4, 0, 1.2), 60)
	local_position = right.to_local(player.global_position)
	report("piano_blocks_player", local_position.x > 2.4 and local_position.x < 2.55)
	await key(KEY_R)
	report("R_stays_in_right_room", player.global_position.distance_to(right.get_node("Arrival").global_position) < 0.1)
	report("walk_to_right_exit", await walk_to(right.global_position + Vector3(-0.7, 0, 1.75)))
	await key(KEY_TAB)
	await key(KEY_E)
	report("right_return_preserves_first_person", player.first_person and player.global_position.distance_to(hall.get_node("ReturnPoints/Right").global_position) < 0.1)
	await key(KEY_TAB)
	report("Tab_restores_hall_overview", root.get_camera_3d() == main_camera and not player.first_person)
	await key(KEY_R)
	report("R_after_return_uses_hall_entrance", absf(player.global_position.x) < 8 and player.is_on_floor())

	release_movement()
	print("BACKSTAGE RESULT: ", checks, " checks, ", failures.size(), " failures: ", failures)
	quit(0 if failures.is_empty() else 1)
