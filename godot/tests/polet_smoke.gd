extends SceneTree
## Loads the actual authored level; checks physical access, transitions and movement.
var level: Node2D
var player: CharacterBody2D
var failures: Array[String] = []
var checks: int = 0
var footprint := CircleShape2D.new()

func _initialize() -> void:
	call_deferred("run")

func check(condition: bool, message: String) -> void:
	checks += 1
	if not condition:
		failures.append(message)
		push_error(message)

func frames(count: int) -> void:
	for i in range(count):
		await physics_frame

func clear_at(point: Vector2) -> bool:
	var query := PhysicsShapeQueryParameters2D.new()
	query.shape = footprint
	query.transform = Transform2D(0, point)
	query.collision_mask = 1
	return player.get_world_2d().direct_space_state.intersect_shape(query, 1).is_empty()

func walk(action: String, count: int) -> void:
	Input.action_press(action)
	await frames(count)
	Input.action_release(action)
	await frames(2)

func interact() -> void:
	var event := InputEventAction.new()
	event.action = "interact"
	event.pressed = true
	Input.parse_input_event(event)
	await process_frame
	event = InputEventAction.new()
	event.action = "interact"
	event.pressed = false
	Input.parse_input_event(event)
	await frames(3)

func run() -> void:
	footprint.radius = 8.0
	level = load("res://scenes/levels/polet.tscn").instantiate()
	root.add_child(level)
	player = level.player
	await frames(4)
	check(level.locations.size() == 11, "All 11 authored locations exist")
	check(level.current_location == level.locations.camp, "Starts at camp")
	check(player.global_position.is_equal_approx(Vector2(896, 1576)), "Starts at entrance")
	for location: Node2D in level.locations.values():
		for marker: Marker2D in location.get_node("Markers").get_children():
			check(clear_at(marker.global_position), "Unblocked arrival: " + str(location.get_meta("location_id")) + "/" + marker.name)
		var connections: Dictionary = location.get_meta("connections", {})
		for destination: Array in connections.values():
			check(level.locations.has(destination[0]) and level.locations[destination[0]].has_node("Markers/"+destination[1]), "Valid authored destination " + str(destination))

	# Test walk/run speed and diagonal normalization on a clear straight path.
	player.global_position = Vector2(896, 1440)
	var start := player.global_position
	await walk("move_forward", 30)
	var distance := start.distance_to(player.global_position)
	check(distance > 54 and distance < 66, "Walking speed matches 120 px/sec")
	player.global_position = start
	Input.action_press("sprint")
	await walk("move_forward", 30)
	Input.action_release("sprint")
	check(start.distance_to(player.global_position) > distance * 1.4, "Shift increases movement speed")
	player.global_position = Vector2(896, 1200)
	start = player.global_position
	Input.action_press("move_right")
	await walk("move_forward", 20)
	Input.action_release("move_right")
	await frames(2)
	check(start.distance_to(player.global_position) < 49, "Diagonal input is normalized")

	# Traverse the real portals using input and trigger overlap, not direct room loading.
	for i in range(1, 5):
		level.enter_location("camp", "Cabin"+str(i))
		await frames(22)
		await walk("move_forward", 17)
		check(level.nearest_portal() != null, "Cabin doorway reachable " + str(i))
		await interact()
		check(str(level.current_location.get_meta("location_id")) == "corridor_"+str(i), "E enters corridor " + str(i))
		var destination: Node2D = level.current_location
		level.try_interact()
		check(level.current_location == destination, "Transition cooldown prevents bounce " + str(i))
		await walk("move_forward", 49)
		await interact()
		check(str(level.current_location.get_meta("location_id")) == "room_"+str(i), "E enters bedroom " + str(i))
		await frames(22)
		await interact()
		check(str(level.current_location.get_meta("location_id")) == "corridor_"+str(i), "Bedroom returns to corridor " + str(i))
		await walk("move_back", 52)
		await interact()
		check(level.current_location == level.locations.camp, "Corridor returns outside " + str(i))
		check(player.global_position.distance_to(level.locations.camp.get_node("Markers/Cabin"+str(i)).global_position) < 1, "Returns to correct building " + str(i))

	for pair in [["Dorm", "dorm"], ["Glass", "glass"]]:
		level.enter_location("camp", pair[0])
		await frames(22)
		await walk("move_forward", 17)
		await interact()
		check(str(level.current_location.get_meta("location_id")) == pair[1], "Enter " + pair[1])
		await frames(22)
		await walk("move_back", 5)
		await interact()
		check(level.current_location == level.locations.camp, "Exit " + pair[1])

	# Doorways use E and have a threshold: walking past the trigger must not escape into void.
	for id: String in level.locations:
		if id == "camp":continue
		level.enter_location(id, "Entrance")
		await walk("move_back", 90)
		var local_y: float = player.global_position.y - level.current_location.global_position.y
		var limit := 345.0 if id.begins_with("room_") else (257.0 if id.begins_with("corridor_") else (281.0 if id == "dorm" else 265.0))
		check(local_y <= limit, "Door threshold prevents leaving room bounds: " + id)

	# A full camp path grid includes all saved collision bodies (trees, buildings, furniture).
	var grid := AStarGrid2D.new()
	grid.region = Rect2i(0, 0, 145, 113)
	grid.cell_size = Vector2(16, 16)
	grid.diagonal_mode = AStarGrid2D.DIAGONAL_MODE_NEVER
	grid.update()
	for y in range(113):
		for x in range(145):
			if not clear_at(Vector2(x, y)*16):grid.set_point_solid(Vector2i(x, y))
	var origin := Vector2i(56, 98)
	for marker: Marker2D in level.locations.camp.get_node("Markers").get_children():
		var target := Vector2i((marker.global_position / 16).round())
		check(not grid.get_id_path(origin, target).is_empty(), "Walkable camp route: " + marker.name)

	# Bed, building shells, gates and tree trunks are genuine physical obstacles.
	level.enter_location("room_1", "Entrance")
	var room: Node2D = level.current_location
	player.global_position = room.global_position + Vector2(96, 204)
	await walk("move_forward", 50)
	check((player.global_position-room.global_position).y > 178, "Bed blocks player feet")
	level.enter_location("camp", "Cabin1")
	await walk("move_forward", 80)
	check(player.global_position.y >= 455, "Building shell blocks passage")
	check(not clear_at(Vector2(896, 1660)), "Closed gate is solid")
	var tree := get_first_node_in_group("polet_tree") as Node2D
	check(not clear_at(tree.global_position-Vector2(0, 5)), "Tree trunk is solid")
	check(clear_at(tree.global_position-Vector2(0, 70)), "Tree crown has no solid rectangle")

	level.enter_location("camp", "Entrance")
	player.global_position = Vector2(1888,1558)
	await frames(22)
	await interact()
	check(level.current_location == level.locations.camp, "Administration remains closed")
	check(level.prompt_label.text.contains("Админка закрыта"), "Closed administration explains the door")
	player.global_position = Vector2(896,1630)
	await frames(22)
	await interact()
	check(level.current_location == level.locations.camp, "Gate does not start unfinished bicycle gameplay")
	check(level.prompt_label.text.contains("велосипеде"), "Gate explains future bicycle trip")
	check(not clear_at(Vector2(54,800)) and not clear_at(Vector2(2248,800)), "Both side boundaries are solid")

	level.enter_location("camp", "Entrance")
	start = player.global_position
	level.set_overview(true)
	await walk("move_forward", 20)
	check(player.global_position.is_equal_approx(start), "Overview freezes player")
	check(level.overview_camera.is_current(), "Overview uses map camera")
	level.set_overview(false)
	check(player.movement_enabled and level.player_camera.is_current(), "Returns to player camera")
	level.enter_location("dorm", "Entrance")
	level.set_overview(true)
	check(not level.overview, "Map cannot activate inside a room")
	var reset := InputEventAction.new()
	reset.action = "reset_position";reset.pressed = true
	Input.parse_input_event(reset)
	await frames(3)
	reset = InputEventAction.new();reset.action = "reset_position";reset.pressed = false
	Input.parse_input_event(reset)
	check(level.current_location == level.locations.camp and player.global_position.is_equal_approx(Vector2(896,1576)), "R returns to camp entrance")

	for action in ["move_left","move_right","move_forward","move_back","sprint"]:Input.action_release(action)
	level.queue_free()
	await frames(3)
	print("POLET SMOKE: ", checks, " checks, ", failures.size(), " failures: ", failures)
	quit(0 if failures.is_empty() else 1)
