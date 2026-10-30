extends Node
## Tracks only the authored bottles under this level. No runtime item placement.

signal hunt_completed

@export_range(1.0, 4.0) var pickup_distance: float = 2.5
@export_range(0.9, 0.999) var aim_dot_threshold: float = 0.975

@onready var player = get_parent().get_node("Player")
@onready var counter: Label = $HUD/Objective/Counter
@onready var prompt: Label = $HUD/Prompt
@onready var notice: Label = $HUD/Notice
@onready var completion: PanelContainer = $HUD/Completion
@onready var crosshair: Label = $HUD/Crosshair
@onready var pickup_sound: AudioStreamPlayer = $PickupSound

var bottles: Array[BottlePickup] = []
var focused: BottlePickup
var collected_count: int = 0
var completed: bool = false
var notice_time: float = 0.0
var hint_enabled := false
var hint_target: BottlePickup
var hint_spot: Marker3D
var inspection_spots: Array[Node] = []


func _ready() -> void:
	for item in get_tree().get_nodes_in_group("collectible_bottles"):
		if get_parent().is_ancestor_of(item):
			bottles.append(item)
			item.collected.connect(_on_bottle_collected)
	inspection_spots = get_parent().get_node("Bottles/InspectionSpots").get_children()
	_update_counter()


func _physics_process(delta: float) -> void:
	notice_time = maxf(0.0, notice_time - delta)
	notice.visible = notice_time > 0.0
	_update_hint()
	crosshair.visible = player.first_person
	_set_focus(find_target())
	if focused and Input.is_action_just_pressed("interact"):
		focused.collect()
		_set_focus(null)


func find_target() -> BottlePickup:
	if completed or not player.first_person or not player.interaction_enabled:
		return null
	var camera: Camera3D = player.first_person_camera
	var origin: Vector3 = camera.global_position
	var forward: Vector3 = -camera.global_basis.z
	var best: BottlePickup = null
	var best_dot := aim_dot_threshold
	for bottle in bottles:
		if bottle.is_collected or not bottle.is_visible_in_tree():
			continue
		var target := bottle.aim_point.global_position
		var offset := target - origin
		if offset.length() > pickup_distance or offset.length_squared() < 0.0001:
			continue
		var alignment := forward.dot(offset.normalized())
		if alignment < best_dot:
			continue
		var query := PhysicsRayQueryParameters3D.create(origin, target, 1, [player.get_rid()])
		# Decorative leaves have no solid collider; walls and furniture block a pickup.
		if not player.get_world_3d().direct_space_state.intersect_ray(query).is_empty():
			continue
		best = bottle
		best_dot = alignment
	return best


func _set_focus(value: BottlePickup) -> void:
	if is_instance_valid(focused) and focused != value:
		focused.set_focused(false)
	focused = value
	prompt.visible = focused != null
	if focused:
		focused.set_focused(true)
		prompt.text = focused.pickup_prompt


func _on_bottle_collected(bottle: BottlePickup) -> void:
	collected_count += 1
	_update_counter()
	notice.text = "+1 · " + bottle.discovery_text
	notice_time = 2.8
	pickup_sound.play()
	if collected_count == bottles.size() and not completed:
		completed = true
		$HUD/Objective/Task.text = "Все %d бутылок найдены.\nПоиск завершён — можно гулять дальше." % collected_count
		completion.show()
		$CompletionTimer.start()
		hunt_completed.emit()


func _update_counter() -> void:
	counter.text = "Бутылки: %d / %d" % [collected_count, bottles.size()]


func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("request_hint") and not event.is_echo():
		hint_enabled = not hint_enabled and not completed
		if hint_enabled: _choose_hint()
		_update_hint()
		get_viewport().set_input_as_handled()

func _choose_hint() -> void:
	hint_target = null
	hint_spot = null
	var closest := INF
	var placements := get_parent().get_node("Bottles")
	for spot: Marker3D in inspection_spots:
		var candidate := placements.get_node(spot.get_meta("bottle_path")) as BottlePickup
		if candidate.is_collected: continue
		var distance: float = player.global_position.distance_squared_to(spot.global_position)
		if distance < closest:
			closest = distance
			hint_target = candidate
			hint_spot = spot

func _update_hint() -> void:
	$HUD/Objective.size = Vector2(384,114) if player.first_person else Vector2(240,64)
	$HUD/Objective/Counter.size.x = 360 if player.first_person else 208
	$HUD/Objective/Task.visible = player.first_person
	$HUD/SearchHint.size = Vector2(556,110) if player.first_person else Vector2(240,220)
	$HUD/SearchHint/Text.size = Vector2(520,90) if player.first_person else Vector2(204,198)
	$HUD/SearchHint.visible = hint_enabled and not completed
	$HintMarker.visible = hint_enabled and not completed and not player.first_person
	if not hint_enabled or completed: return
	if not is_instance_valid(hint_target) or hint_target.is_collected: _choose_hint()
	if hint_spot == null:
		$HUD/SearchHint.hide()
		$HintMarker.hide()
		return
	$HintMarker.global_position = hint_spot.global_position + Vector3.UP * 4.0
	var offset: Vector3 = hint_spot.global_position - player.global_position
	offset.y = 0.0
	var distance := ceili(offset.length())
	var direction := "рядом"
	if distance > 3:
		var camera: Camera3D = player.first_person_camera
		var forward := -camera.global_basis.z
		var right := camera.global_basis.x
		var ahead := forward.dot(offset.normalized())
		var side := right.dot(offset.normalized())
		direction = "прямо" if ahead > 0.7 else "позади" if ahead < -0.7 else "справа" if side > 0.0 else "слева"
	var clue := str(hint_spot.get_meta("hint_text", hint_target.discovery_text))
	var footer := "Золотая отметка — место поиска. Tab — вернуться." if not player.first_person else "%s · около %d м · Tab — посмотреть на плане" % [direction.capitalize(), distance]
	$HUD/SearchHint/Text.text = "Подсказка · H — скрыть\n%s\n%s" % [clue, footer]
