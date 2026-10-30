extends SceneTree
## End-to-end story using authored targets and physical interaction, plus minigame edge cases.
var level
var story
var failures: Array[String] = []
var checks := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, message: String) -> void:
	checks += 1
	if not ok:
		failures.append(message)
		push_error(message)

func frames(count: int = 3) -> void:
	for i in range(count):
		await physics_frame

func key(action: String, pressed: bool) -> void:
	var event := InputEventAction.new()
	event.action = action
	event.pressed = pressed
	Input.parse_input_event(event)

func route_exists(destination: Vector2) -> bool:
	var location: Node2D = level.current_location
	var bounds: Rect2 = location.get_meta("camera_bounds")
	var origin := location.global_position + bounds.position
	var grid := AStarGrid2D.new()
	grid.region = Rect2i(Vector2i.ZERO, Vector2i(ceilf(bounds.size.x / 16.0), ceilf(bounds.size.y / 16.0)))
	grid.cell_size = Vector2(16, 16)
	grid.diagonal_mode = AStarGrid2D.DIAGONAL_MODE_NEVER
	grid.update()
	var query := PhysicsShapeQueryParameters2D.new()
	var footprint := CircleShape2D.new()
	footprint.radius = 8.0
	query.shape = footprint
	query.collision_mask = 1
	var space: PhysicsDirectSpaceState2D = level.player.get_world_2d().direct_space_state
	for y in range(grid.region.size.y):
		for x in range(grid.region.size.x):
			query.transform = Transform2D(0, origin + Vector2(x, y) * 16.0)
			grid.set_point_solid(Vector2i(x, y), not space.intersect_shape(query, 1).is_empty())
	var start := Vector2i(((location.get_node("Markers/Entrance").global_position - origin) / 16.0).round())
	var end := Vector2i(((destination - origin) / 16.0).round())
	if not grid.is_in_boundsv(start) or not grid.is_in_boundsv(end):
		return false
	return not grid.get_id_path(start, end).is_empty()

func target(path: String) -> Node2D:
	var node: Node2D = level.get_node(path)
	level.player.global_position = node.global_position + Vector2(0, 12)
	level.player.velocity = Vector2.ZERO
	await frames(22)
	var query := PhysicsShapeQueryParameters2D.new()
	var shape := CircleShape2D.new()
	shape.radius = 8.0
	query.shape = shape
	query.transform = Transform2D(0, level.player.global_position)
	query.collision_mask = 1
	check(level.player.get_world_2d().direct_space_state.intersect_shape(query, 1).is_empty(), "Physical approach free: " + path)
	check(story.nearest_target() == node, "Target reachable by E: " + path)
	check(route_exists(level.player.global_position), "Walkable route from arrival to task: " + path)
	return node

func press_e() -> void:
	key("interact", true)
	await frames(3)
	key("interact", false)
	await frames(3)

func tap_keyboard(code: Key) -> void:
	var event := InputEventKey.new()
	event.keycode = code
	event.physical_keycode = code
	event.pressed = true
	Input.parse_input_event(event)
	await frames(2)
	event = InputEventKey.new()
	event.keycode = code
	event.physical_keycode = code
	event.pressed = false
	Input.parse_input_event(event)
	await frames(2)

func clean(path: String, id: String) -> void:
	await target(path)
	key("interact", true)
	await frames(200)
	key("interact", false)
	await frames(3)
	check(story.finished.has(id), "Hold E completes " + id)

func run() -> void:
	level = load("res://scenes/levels/polet.tscn").instantiate()
	level.set_meta("test_mode", true)
	root.add_child(level)
	story = level.story
	await frames(4)
	check(story.stage == story.Stage.CLEAN, "Story starts with cleanup")
	check(not story.enabled("bicycle"), "Cannot skip to shopping")
	check(not story.enabled("finale"), "Cannot skip to finale")
	check(level.get_node("World/Glass/EveningGuests/Director/CollisionShape2D").disabled, "Absent guests do not block room")
	await target("World/Camp/TalkChild")
	await press_e()
	check(story.busy and story.dialogue_kind == "chat", "Early child visit gives contextual dialogue")
	var start: Vector2 = level.player.global_position
	key("move_left", true)
	key("reset_position", true)
	key("toggle_camera", true)
	await frames(20)
	key("move_left", false)
	key("reset_position", false)
	key("toggle_camera", false)
	check(level.player.global_position.is_equal_approx(start) and not level.overview, "Dialogue blocks movement, reset and overview")
	story.dialogue.get_node("Card/Choice1").pressed.emit()
	check(story.stage == story.Stage.CLEAN and not story.busy, "Early talk does not advance quest")
	level.enter_location("room_1", "Entrance")
	await target("World/Room1/Linen")
	key("interact", true)
	await frames(20)
	key("interact", false)
	await frames(3)
	check(not story.finished.has("linen") and story.hold_time == 0.0, "Releasing E cancels incomplete cleaning")
	await clean("World/Room1/Linen", "linen")
	check(not level.get_node("World/Room1/Item128").visible and level.get_node("World/Room1/CleanBedNorth").visible, "Linen task replaces messy bed with authored clean bed")
	await clean("World/Room1/Rubbish", "rubbish")
	check(not level.get_node("World/Room1/FloorDetails/Item153").visible, "Collected paper disappears")
	await clean("World/Room1/Mop", "mop")
	check(story.stage == story.Stage.CLEAN, "One room cannot complete a three-room building")
	for suffix in ["b","c"]:
		level.enter_location("room_1"+suffix,"Entrance")
		check(not story.enabled("mop_"+suffix),"Sweep floor only after collecting things")
		for task in [["Linen","linen"],["Rubbish","rubbish"],["Mop","mop"]]:
			await clean("World/Room1"+suffix+"/"+task[0],task[1]+"_"+suffix)
	check(story.stage == story.Stage.SUPPORT, "All nine tasks unlock support dialogue")
	check(not level.get_node("World/Room1/FloorDetails/Item156").visible, "Mopping removes footprints")
	level.enter_location("camp", "Entrance")
	await target("World/Camp/TalkChild")
	await press_e()
	check(story.dialogue_kind == "child", "Support dialogue starts from actual target")
	await tap_keyboard(KEY_2)
	check(story.dialogue.get_node("Card/Message").text.contains("огонёк"), "Choice changes child response")
	story.dialogue.get_node("Card/Choice2").pressed.emit()
	check(story.dialogue.get_node("Card/Message").text.contains("написать"), "Second choice changes farewell")
	story.dialogue.get_node("Card/Choice1").pressed.emit()
	await frames()
	check(story.stage == story.Stage.RADIO, "Child farewell unlocks equipment return")
	check(level.get_node("World/Camp/Child/CollisionShape2D").disabled, "Departing child no longer blocks the path")
	await target("World/Camp/TalkDirector")
	await press_e()
	story.dialogue.get_node("Card/Choice1").pressed.emit()
	check(story.stage == story.Stage.PREPARE, "Radio handover unlocks preparation")
	level.enter_location("glass", "Entrance")
	check(not story.enabled("circuit"),"Routing waits for hanging the garland")
	await clean("World/Glass/Garland", "garland_box")
	check(not story.enabled("garland_middle"),"Secure ends before adjusting sag")
	for part in ["left","right","middle"]:
		await clean("World/Glass/Garland"+part,"garland_"+part)
		check(level.get_node("World/Glass/Section"+part).visible,"Hung section visible: "+part)
	check(story.finished.has("garland"),"Three mounted sections finish garland")
	await target("World/Glass/Circuit")
	await press_e()
	check(story.modal == "circuit", "Routing puzzle opens")
	story.circuit.get_node("Card/Check").pressed.emit()
	check(not story.finished.has("circuit"), "Broken route is not accepted")
	story.circuit.get_node("Card/Tile1").pressed.emit()
	var rotation: int = story.circuit.rotations[1]
	story.circuit.get_node("Card/Close").pressed.emit()
	await frames(20)
	await press_e()
	check(story.circuit.rotations[1] == rotation, "Partial routing survives closing")
	for i in range(12):
		while story.circuit.rotations[i] != 0: story.circuit.turn(i)
	check(story.circuit.outputs() == [true,true,true], "One source reaches all three outputs")
	story.circuit.get_node("Card/Check").pressed.emit()
	check(story.finished.has("circuit") and not story.busy, "Solving circuit completes it")
	check(level.get_node("World/Glass/GarlandOn").visible and not level.get_node("World/Glass/GarlandOff").visible, "Solved circuit lights garland")
	check(story.stage == story.Stage.RIDE, "Preparation unlocks bicycle without music minigame")
	check(story.get_node("PartyMusic").playing, "Music actually plays")
	level.enter_location("dorm", "Entrance")
	await target("World/Dorm/Outfit")
	await press_e()
	story.choose(1)
	check(story.outfit == 1 and level.player.outfit == 1, "Optional outfit changes character costume")
	level.enter_location("camp", "Entrance")
	await target("World/Camp/Ride")
	await press_e()
	story.choose(0)
	check(story.modal == "bicycle" and story.bicycle.running, "Bicycle starts through world interaction")
	story.bicycle.get_node("Exit").pressed.emit()
	story.bicycle.get_node("Pause/Card/Leave").pressed.emit()
	check(story.stage == story.Stage.RIDE and not story.finished.has("bicycle"), "Aborting trip awards no completion")
	await frames(20)
	await press_e()
	story.choose(0)
	story.bicycle.set_process(false)
	story.bicycle.distance = 400.0
	story.bicycle.speed = 185.0
	story.bicycle.rider_y = 470.0
	story.bicycle._process(0.15)
	check(story.bicycle.bumps == 1 and story.bicycle.slow_time > 0.0, "Authored obstacle triggers a slowdown")
	var distance_before: float = story.bicycle.distance
	story.bicycle._process(0.1)
	check(story.bicycle.bumps == 1 and story.bicycle.distance > distance_before, "Collision does not repeat or reset trip")
	Input.action_press("move_forward")
	story.bicycle._process(0.2)
	Input.action_release("move_forward")
	check(story.bicycle.rider_y < 470.0, "W steers rider away from obstacles")
	# Advance the real scrolling ride with normal input, without bypassing finish logic.
	Input.action_press("sprint")
	for i in range(800):
		if not story.bicycle.running: break
		story.bicycle._process(0.05)
	Input.action_release("sprint")
	check(story.dialogue_kind == "ride_shop" and story.busy, "Reaching shop starts purchase dialogue")
	check(not story.finished.has("bicycle"),"Outward trip cannot finish the ride")
	story.choose(0)
	check(story.bicycle.returning and story.finished.has("ride_shop"),"Purchased cargo starts return trip")
	story.bicycle.balance = 10.0
	story.bicycle.advance(1.0,0.0,false,true)
	check(story.bicycle.balance > 10.0 and story.bicycle.speed <= 100.0,"Braking steadies the basket")
	for i in range(1600):
		if not story.bicycle.running: break
		story.bicycle.advance(0.05,0.0,true,false)
	check(story.dialogue_kind == "ride_return", "Real return journey reaches the camp")
	story.choose(0)
	await frames()
	check(story.stage == story.Stage.FINALE and level.current_location == level.locations.camp, "Shopping returns player to camp")
	check(level.get_node("World/Glass/EveningGuests").visible, "Friends appear for final evening")
	level.enter_location("glass", "Entrance")
	await target("World/Glass/Finale")
	await press_e()
	check(story.dialogue_kind == "finale", "Final dialogue reachable among guests")
	for i in range(3):story.dialogue.get_node("Card/Choice1").pressed.emit()
	check(story.stage == story.Stage.DONE and story.modal == "pause", "Full story reaches closing card")
	story.pause_panel.get_node("Card/Resume").pressed.emit()
	check(level.player.movement_enabled and not story.enabled("finale"), "Free exploration after ending, finale not repeated")
	level.enter_location("camp", "Entrance")
	check(story.stage == story.Stage.DONE and story.finished.has("linen"), "Returning to entrance preserves quest state")
	print("Polet story: %d checks, %d failures" % [checks, failures.size()])
	level.queue_free()
	await frames(4)
	# Fixed-fps tests can finish before the audio mixing thread releases its last buffer.
	OS.delay_msec(50)
	await process_frame
	await process_frame
	quit(0 if failures.is_empty() else 1)
