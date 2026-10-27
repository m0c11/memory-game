extends Control
## An authored side-view road: move the rider vertically, scenery scrolls as one saved group.
signal completed
signal cancelled
var distance: float = 0.0
var rider_y: float = 470.0
var bumps: int = 0
var immunity: float = 0.0
var slow_time: float = 0.0
var running: bool = false
var pedal_time: float = 0.0
const LENGTH := 5300.0

func _ready() -> void:
	$Exit.pressed.connect(func(): running = false; cancelled.emit())

func begin() -> void:
	distance = 0.0
	rider_y = 470.0
	bumps = 0
	immunity = 0.0
	slow_time = 0.0
	running = true
	$Hint.text = "W / S или ↑ / ↓ — объезжай ямы. Shift — педали быстрее."
	$Result.hide()
	for obstacle in $Road/Obstacles.get_children():
		obstacle.set_meta("passed", false)
	show()
	$Exit.release_focus()
	get_parent().get_parent().sound("Bell")

func _process(delta: float) -> void:
	if not visible or not running:
		return
	rider_y = clampf(rider_y + Input.get_axis("move_forward", "move_back") * 220.0 * delta, 410.0, 590.0)
	slow_time = maxf(0.0, slow_time - delta)
	immunity = maxf(0.0, immunity - delta)
	var speed := 230.0 if Input.is_action_pressed("sprint") else 165.0
	if slow_time > 0.0:
		speed = 70.0
	pedal_time -= delta
	if pedal_time <= 0:
		get_parent().get_parent().sound("Click")
		pedal_time = 0.4
	$Rider/Sprite.speed_scale = speed / 165.0
	distance = minf(LENGTH, distance + speed * delta)
	$Road.position.x = -distance
	$Rider.position = Vector2(240, rider_y)
	$Rider.modulate.a = 0.55 if immunity > 0.0 else 1.0
	for obstacle in $Road/Obstacles.get_children():
		var relative: Vector2 = obstacle.position - Vector2(distance + 240.0, rider_y)
		if not obstacle.get_meta("passed", false) and absf(relative.x) < 36.0 and absf(relative.y) < 28.0 and immunity <= 0.0:
			obstacle.set_meta("passed", true)
			bumps += 1
			get_parent().get_parent().sound("Bump")
			slow_time = 1.0
			immunity = 1.3
			$Hint.text = "Кочка! Велосипед цел — продолжаем."
	$Progress.value = distance / LENGTH * 100.0
	$Distance.text = "До магазина: %d м · Кочки: %d" % [ceili((LENGTH - distance) / 10.0), bumps]
	if distance >= LENGTH:
		running = false
		$Result.show()
		completed.emit()
