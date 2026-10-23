extends SceneTree
## Real scene, real physics rays and input: every authored hiding place must be reachable.

var scene: Node3D
var player: CharacterBody3D
var hunt: Node
var failures: Array[String] = []
var checks := 0
var completed_signals := 0


func _initialize() -> void:
	call_deferred("run")


func frames(count: int) -> void:
	for i in range(count):
		await physics_frame


func check(label: String, condition: bool) -> void:
	checks += 1
	print("PASS " if condition else "FAIL ", label)
	if not condition:
		failures.append(label)


func key(code: int, pressed: bool = true) -> void:
	var event := InputEventKey.new()
	event.physical_keycode = code
	event.keycode = code
	event.pressed = pressed
	Input.parse_input_event(event)
	await frames(3)


func tap(code: int) -> void:
	await key(code)
	await key(code, false)


func inspect(spot: Marker3D) -> BottlePickup:
	var bottle: BottlePickup = scene.get_node("Bottles").get_node(spot.get_meta("bottle_path"))
	player.global_position = spot.global_position
	player.velocity = Vector3.ZERO
	player.rotation = Vector3.ZERO
	player.head.rotation = Vector3.ZERO
	await frames(15)
	player.first_person_camera.look_at(bottle.aim_point.global_position, Vector3.UP)
	await frames(3)
	return bottle


func _on_hunt_completed() -> void:
	completed_signals += 1


func run() -> void:
	scene = load("res://scenes/levels/venevitino.tscn").instantiate()
	root.add_child(scene)
	current_scene = scene
	# Patrol behavior is covered separately; keep this test focused on every hiding place.
	var patrols = scene.get_node("Organizers")
	patrols.process_mode = Node.PROCESS_MODE_DISABLED
	for guard in patrols.guards:
		guard.collision_layer = 0
	player = scene.get_node("Player")
	hunt = scene.get_node("BottleHunt")
	hunt.hunt_completed.connect(_on_hunt_completed)
	await frames(20)
	check("20 bottles, empty counter", hunt.bottles.size() == 20 and hunt.collected_count == 0)
	check("completion hidden at start", not hunt.completed and not hunt.completion.visible)
	var spots = scene.get_node("Bottles/InspectionSpots").get_children()
	var first := await inspect(spots[0])
	check("aim shows prompt and ring", hunt.focused == first and hunt.prompt.visible and first.focus_ring.visible)
	player.first_person_camera.rotate_y(PI)
	await frames(3)
	await tap(KEY_E)
	check("cannot pick up behind the player", hunt.collected_count == 0 and not hunt.prompt.visible)
	await inspect(spots[0])
	var origin: Vector3 = player.first_person_camera.global_position
	var target := first.aim_point.global_position
	player.global_position += (origin - target).normalized() * 5.0
	await frames(3)
	await tap(KEY_E)
	check("cannot pick up from a distance", hunt.collected_count == 0)
	await inspect(spots[0])
	var wall := StaticBody3D.new()
	var shape := CollisionShape3D.new()
	shape.shape = BoxShape3D.new()
	shape.shape.size = Vector3(0.4, 0.6, 0.4)
	wall.add_child(shape)
	scene.add_child(wall)
	wall.global_position = player.first_person_camera.global_position.lerp(first.aim_point.global_position, 0.55)
	await frames(4)
	await tap(KEY_E)
	check("solid wall blocks pickup and prompt", hunt.collected_count == 0 and hunt.focused == null)
	wall.queue_free()
	await frames(4)
	await inspect(spots[0])
	await tap(KEY_ESCAPE)
	await tap(KEY_E)
	check("unlocked cursor blocks pickup", hunt.collected_count == 0 and hunt.focused == null)
	var click := InputEventMouseButton.new()
	click.button_index = MOUSE_BUTTON_LEFT
	click.pressed = true
	Input.parse_input_event(click)
	await frames(3)
	click = InputEventMouseButton.new()
	click.button_index = MOUSE_BUTTON_LEFT
	click.pressed = false
	Input.parse_input_event(click)
	await tap(KEY_TAB)
	await tap(KEY_E)
	check("map blocks pickup", not player.first_person and hunt.collected_count == 0 and not hunt.crosshair.visible)
	await tap(KEY_TAB)
	await inspect(spots[0])
	await key(KEY_E)
	check("E collects, hides bottle and updates counter", first.is_collected and not first.visible and hunt.collected_count == 1 and hunt.counter.text == "Бутылки: 1 / 20")
	check("pickup feedback", hunt.notice.visible and hunt.notice.text.begins_with("+1"))
	await inspect(spots[1])
	await frames(10)
	check("holding E does not collect the next bottle", hunt.collected_count == 1)
	await key(KEY_E, false)
	check("duplicate pickup rejected", not first.collect() and hunt.collected_count == 1)
	await tap(KEY_R)
	check("return to entrance preserves progress", hunt.collected_count == 1 and first.is_collected)
	for spot in spots:
		var bottle := await inspect(spot)
		if bottle.is_collected:
			continue
		check(str(spot.name) + " standing on a reachable floor", player.is_on_floor() and player.global_position.distance_to(spot.global_position) < 0.12)
		check(str(spot.name) + " visible to interaction ray", hunt.focused == bottle)
		var before: int = hunt.collected_count
		await tap(KEY_E)
		check(str(spot.name) + " collected once", bottle.is_collected and hunt.collected_count == before + 1)
	check("all 20 collected", hunt.collected_count == 20 and hunt.counter.text == "Бутылки: 20 / 20")
	check("hunt completes exactly once", hunt.completed and hunt.completion.visible and completed_signals == 1)
	await tap(KEY_E)
	await tap(KEY_R)
	await frames(190)
	check("completed state survives further input", hunt.collected_count == 20 and completed_signals == 1 and not hunt.prompt.visible)
	check("transient feedback expires", not hunt.notice.visible)
	check("backpacks remain after bottle removal", scene.get_node("Bottles/Backpacks/Porch/Backpack").is_visible_in_tree())
	await frames(400)
	check("completion banner clears but objective stays complete", not hunt.completion.visible and hunt.get_node("HUD/Objective/Task").text.begins_with("Все 20 бутылок найдены"))
	print("BOTTLE HUNT RESULT ", checks, " checks; failures=", failures)
	hunt.pickup_sound.stop()
	scene.queue_free()
	await process_frame
	await process_frame
	quit(0 if failures.is_empty() else 1)
