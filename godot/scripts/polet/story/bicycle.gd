extends Control
## Two trips along the same authored road; no scenery is generated at runtime.
signal completed
signal shop_reached
signal cancelled
const LENGTH := 5300.0
var distance: float = 0.0
var rider_y: float = 470.0
var bumps: int = 0
var immunity: float = 0.0
var slow_time: float = 0.0
var running: bool = false
var returning: bool = false
var speed: float = 0.0
var stamina: float = 100.0
var balance: float = 100.0
var pedal_time: float = 0.0

func _ready() -> void:
	$Exit.pressed.connect(func(): running = false; cancelled.emit())

func begin(return_trip: bool = false) -> void:
	returning = return_trip
	distance = 0.0
	rider_y = 470.0
	bumps = 0
	immunity = 0.0
	slow_time = 0.0
	speed = 0.0
	stamina = 100.0
	balance = 100.0
	running = true
	$Title.text = "ОБРАТНО В ЛАГЕРЬ · Держим корзину!" if returning else "ЗА ДОБАВКОЙ · Иначе «Архангельской» не хватит"
	$Hint.text = "W/S, ↑/↓ — объезд · Shift — разгон · Пробел — тормоз и устойчивость"
	$Result.hide()
	$Rider.scale.x = -1.0 if returning else 1.0
	$Rider/Cargo.visible = returning
	for obstacle in $Road/Obstacles.get_children(): obstacle.set_meta("passed", false)
	show()
	$Exit.release_focus()
	update_view()
	get_parent().get_parent().sound("Bell")

func _process(delta: float) -> void:
	if not visible or not running: return
	advance(delta, Input.get_axis("move_forward", "move_back"), Input.is_action_pressed("sprint"), Input.is_physical_key_pressed(KEY_SPACE))

func advance(delta: float, steering: float, sprinting: bool, braking: bool) -> void:
	if not running: return
	var accelerating := sprinting and stamina > 5.0 and not braking
	var target_speed := 300.0 if accelerating else 185.0
	if braking: target_speed = 65.0
	if slow_time > 0.0: target_speed = 60.0
	if returning and balance < 20.0: target_speed = minf(target_speed, 90.0)
	speed = move_toward(speed, target_speed, 220.0 * delta)
	stamina = clampf(stamina + (-25.0 if accelerating else 18.0) * delta, 0.0, 100.0)
	rider_y = clampf(rider_y + steering * 190.0 * delta, 410.0, 590.0)
	if returning:
		balance = clampf(balance + (24.0 if braking else 5.0 if absf(steering)<0.1 else -speed/28.0)*delta,0.0,100.0)
	immunity = maxf(0.0, immunity - delta)
	slow_time = maxf(0.0, slow_time - delta)
	distance = minf(LENGTH, distance + speed * delta)
	update_view()
	for obstacle in $Road/Obstacles.get_children():
		var relative: Vector2 = obstacle.global_position - $Rider.global_position
		if not obstacle.get_meta("passed", false) and absf(relative.x) < 36.0 and absf(relative.y) < 28.0 and immunity <= 0.0:
			obstacle.set_meta("passed", true)
			if speed < 100.0:
				$Hint.text = "Мягко проехали кочку. Можно снова разгоняться!"
			else:
				bumps += 1
				balance = maxf(0.0,balance-30.0)
				slow_time = 1.0
				immunity = 1.3
				get_parent().get_parent().sound("Bump")
				$Hint.text = "Корзину качнуло! Пробел — притормозить и поправить груз." if returning else "Кочка! Лучше объехать или заранее притормозить."
	pedal_time -= delta
	if pedal_time <= 0.0:
		get_parent().get_parent().sound("Click")
		pedal_time = 0.55
	if distance >= LENGTH:
		running = false
		$Result.show()
		if returning: completed.emit()
		else: shop_reached.emit()

func update_view() -> void:
	$Road.position.x = -LENGTH + distance if returning else -distance
	$Rider.position = Vector2(1040 if returning else 240, rider_y)
	$Rider/Sprite.speed_scale = speed / 185.0
	$Rider.modulate.a = 0.55 if immunity > 0.0 else 1.0
	$Progress.value = distance / LENGTH * 100.0
	$Distance.text = "%s: %d м · Кочки: %d" % ["До лагеря" if returning else "До магазина", ceili((LENGTH-distance)/10.0), bumps]
	$Condition.text = "Силы: %d%%   %s" % [int(stamina), "Устойчивость корзины: %d%%" % int(balance) if returning else "Береги силы для разгона между ямами"]
