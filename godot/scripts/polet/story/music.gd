extends Control
signal completed
signal cancelled
var elapsed: float = 0.0
var hits: int = 0
var running: bool = false
var tick_played: bool = false

func _ready() -> void:
	$Card/Hit.pressed.connect(hit)
	$Card/Close.pressed.connect(func(): cancelled.emit())

func begin() -> void:
	elapsed = 0.0
	running = true
	$Card/Hint.text = "Поймай зелёную зону три раза. Ошибка не сбрасывает удачные попадания."
	$Card/Count.text = "Проверка звука · %d / 3" % hits
	show()
	$Card/Hit.grab_focus()

func _process(delta: float) -> void:
	if visible and running:
		elapsed += delta
		if phase() >= 0.36 and not tick_played:
			get_parent().get_parent().sound("Tick")
			tick_played = true
		if phase() < 0.1: tick_played = false
		$Card/Cursor.position.x = 85.0 + phase() * 590.0

func phase() -> float:
	return fposmod(elapsed, 1.8) / 1.8

func hit() -> void:
	if not running:
		return
	if phase() >= 0.36 and phase() <= 0.64:
		hits += 1
		get_parent().get_parent().sound("Success")
		$Card/Hint.text = "Отлично! Ещё один такт." if hits < 3 else "Звук готов!"
	else:
		$Card/Hint.text = "Чуть мимо. Дождись зелёной полосы и попробуй снова."
	elapsed = 0.0
	$Card/Count.text = "Проверка звука · %d / 3" % hits
	if hits == 3:
		running = false
		completed.emit()
