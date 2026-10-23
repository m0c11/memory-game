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


func _ready() -> void:
	for item in get_tree().get_nodes_in_group("collectible_bottles"):
		if get_parent().is_ancestor_of(item):
			bottles.append(item)
			item.collected.connect(_on_bottle_collected)
	_update_counter()


func _physics_process(delta: float) -> void:
	notice_time = maxf(0.0, notice_time - delta)
	notice.visible = notice_time > 0.0
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
