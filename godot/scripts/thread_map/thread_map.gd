extends Node2D
## Follows an authored Curve2D; geometry, stops, yarn and UI live in the saved scene.
@export var preview_finale := false
@export var roll_speed := 650.0
var rolling := false
var revealing := false
var target_stop := 0
var reveal_tween: Tween
@onready var journey = get_node("/root/Journey")
@onready var ball: PathFollow2D = $Route/Ball
@onready var camera: Camera2D = $Camera
@onready var ui: Control = $HUD/UI/Page
@onready var stops: Array[Node] = $Stops.get_children()

func _ready() -> void:
	get_viewport().size_changed.connect(fit_ui)
	fit_ui()
	ui.get_node("Menu").pressed.connect(func(): journey.travel(-1))
	ui.get_node("Card/Enter").pressed.connect(enter_chapter)
	ui.get_node("Card/Previous").pressed.connect(func(): go_to(target_stop - 1))
	ui.get_node("Card/Next").pressed.connect(func(): go_to(target_stop + 1))
	ball.progress_ratio = journey.map_progress if not preview_finale else 0.87
	target_stop = journey.map_stop if not preview_finale else 5
	camera.position = ball.position + Vector2(0,90)
	if preview_finale:
		rolling = true
	else:
		# Only advance after a genuine completion. Leaving an unfinished chapter stays here.
		if journey.map_advance:
			target_stop = mini(target_stop + 1, journey.map_unlocked)
			journey.map_stop = target_stop
			journey.map_advance = false
		rolling = absf(ball.progress_ratio - stop_fraction(target_stop)) > 0.0001
	refresh()
	if target_stop == 5 and not rolling and journey.can_reveal(): call_deferred("reveal_word")

func fit_ui() -> void:
	var size := get_viewport().get_visible_rect().size
	var factor := minf(size.x/1280.0,size.y/720.0)
	ui.scale = Vector2.ONE * factor
	ui.position = (size - Vector2(1280,720)*factor)*0.5

func stop_fraction(index: int) -> float:
	return float(stops[index].get_meta("fraction"))

func _process(delta: float) -> void:
	if journey.transitioning or revealing: return
	if rolling:
		var before := ball.progress
		ball.progress = move_toward(before, stop_fraction(target_stop)*$Route.curve.get_baked_length(), roll_speed*delta)
		$Route/Ball/Yarn.rotation += (ball.progress-before)/34.0
		if not preview_finale:
			journey.map_progress = ball.progress_ratio
			journey.map_revealed = maxf(journey.map_revealed, ball.progress_ratio)
		if absf(ball.progress_ratio-stop_fraction(target_stop)) < 0.0001:
			rolling = false
			if not preview_finale:
				journey.map_stop = target_stop
				if not journey.persist(): journey.show_error("Не удалось сохранить путь. Клубок останется здесь; можно попробовать ещё раз.")
			if target_stop == 5 and (preview_finale or journey.can_reveal()):
				reveal_word()
				return
			else:
				refresh()
				if ui.get_node("Card/Enter").disabled: ui.get_node("Card/Previous").grab_focus()
				else: ui.get_node("Card/Enter").grab_focus()
	camera.position = camera.position.lerp(ball.position + Vector2(0,90), 1.0-exp(-delta*5.0))
	var visible_fraction: float = ball.progress_ratio if preview_finale else journey.map_revealed
	$Thread.material.set_shader_parameter("revealed", visible_fraction)
	for i in range(stops.size()):
		stops[i].visible = i <= target_stop and absf(stops[i].position.x-ball.position.x)<750 and stops[i].position.distance_to(ball.position)<600

func refresh() -> void:
	ui.get_node("Card/Title").text = str(stops[target_stop].get_meta("title"))
	ui.get_node("Card/Description").text = "Нить ведёт к следующему воспоминанию…" if rolling else "Клубок на месте. Можно войти и прожить этот момент."
	var enter: Button = ui.get_node("Card/Enter")
	enter.disabled = rolling or target_stop >= journey.CHAPTERS.size()
	enter.text = "Клубок катится…" if rolling else "Войти в воспоминание"
	if not rolling and target_stop < journey.CHAPTERS.size():
		var descriptions := ["Утро, репетиция, волнение — и ваш общий выход.", "Последний день смены. Забота, прощания и вечер вместе.", "Ночная база, фонарик и маленькие тайники на знакомых тропах."]
		ui.get_node("Card/Description").text = descriptions[target_stop]
		if target_stop in journey.map_completed:
			enter.text = "Вернуться в воспоминание"
			ui.get_node("Card/Description").text += "\nУже пройдено. Можно ещё побыть здесь."
		elif has_checkpoint():
			enter.text = "Продолжить воспоминание"
	if not rolling and target_stop >= journey.CHAPTERS.size():
		ui.get_node("Card/Description").text = "Эта глава ещё создаётся. Можно вернуться к уже открытым воспоминаниям."
		enter.text = "Продолжение впереди"
	ui.get_node("Card/Previous").disabled = rolling or target_stop == 0
	ui.get_node("Card/Next").disabled = rolling or target_stop >= journey.map_unlocked

func has_checkpoint() -> bool:
	if target_stop == 0: return int(journey.hall_state.get("stage", 0)) > 0
	if target_stop == 2: return not journey.bottles.is_empty()
	if target_stop == 1:
		var data := PoletSave.read_data()
		return not data.is_empty() and data.get("run_id", 0) == journey.run_id and not data.finished.is_empty()
	return false

func go_to(index: int) -> void:
	if rolling or revealing or index < 0 or index > journey.map_unlocked or index >= stops.size(): return
	target_stop = index
	journey.map_stop = index
	rolling = true
	refresh()

func enter_chapter() -> void:
	if rolling or revealing or target_stop >= journey.CHAPTERS.size(): return
	journey.travel(target_stop)

func reveal_word() -> void:
	revealing = true
	for stop in stops: stop.hide()
	ui.get_node("Card").hide()
	ui.get_node("Heading").hide()
	ui.get_node("Hint").hide()
	$Thread.material.set_shader_parameter("revealed",1.0)
	var final_zoom := minf(get_viewport_rect().size.x/5200.0,get_viewport_rect().size.y/2900.0)
	reveal_tween = create_tween().set_parallel(true)
	reveal_tween.tween_property($Thread,"width",18.0,5.0)
	reveal_tween.tween_property($Glow,"width",70.0,5.0)
	reveal_tween.tween_property(camera,"position",Vector2(2265,1050),5.0).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_IN_OUT)
	reveal_tween.tween_property(camera,"zoom",Vector2.ONE*final_zoom,5.0).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_IN_OUT)
	reveal_tween.tween_property($Atmosphere/Mist.material,"shader_parameter/reveal",1.0,4.0)
	await reveal_tween.finished
	ui.get_node("Ending").modulate.a = 0.0
	ui.get_node("Ending").show()
	create_tween().tween_property(ui.get_node("Ending"),"modulate:a",1.0,1.5)

func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("ui_cancel") and not event.is_echo():
		journey.travel(-1)
		get_viewport().set_input_as_handled()
