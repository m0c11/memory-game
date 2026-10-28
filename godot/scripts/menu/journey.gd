extends CanvasLayer
## Persistent navigation only. All UI and worlds are authored PackedScenes.
const MAP := "res://scenes/thread_map/thread_map.tscn"
const MENU := "res://scenes/menu/main_menu.tscn"
const CHAPTERS := ["res://scenes/levels/auditorium.tscn", "res://scenes/levels/polet.tscn", "res://scenes/levels/venevitino.tscn"]
const TITLES := ["Студвесна", "Полёт · Последний день", "Веневитино · После заката"]
const QUOTES := ["Вдохнуть. Улыбнуться. Сделать шаг к свету.", "Лето заканчивается. Забота остаётся.", "За знакомой тропинкой начинается маленькое приключение."]
var save_path := "user://journey_v1.cfg"
var run_id := 0
var map_started := false
var map_advance := false
var map_completed: Array[int] = []
var map_unlocked := 0
var map_stop := 0
var map_progress := 0.0
var map_revealed := 0.0
var last_chapter := -1
var chapter := -1
var bottles: Array[String] = []
var volume := 0.7
var motion := true
var fullscreen := false
var transitioning := false
var test_mode := false
var previous_mouse := Input.MOUSE_MODE_VISIBLE
var transition_mouse := Input.MOUSE_MODE_VISIBLE
var settings_from_menu := false
var previous_focus: Control
var activated_scene: Node
var modal_kind := "pause"
var music_delay := 0.3
@onready var modal: Control = $UI/Modal
@onready var card: Control = $UI/Modal/Page/Card
@onready var settings: Control = $UI/Modal/Page/SettingsPanel
@onready var curtain: Control = $UI/Curtain

func _ready() -> void:
	get_window().title = "Наши воспоминания"
	$Music.finished.connect(func(): $Music.play())
	read_progress()
	get_viewport().size_changed.connect(fit_pages)
	fit_pages()
	get_tree().scene_changed.connect(on_scene_changed)
	$UI/Shortcut.pressed.connect(open_pause)
	card.get_node("Resume").pressed.connect(resume)
	card.get_node("Next").pressed.connect(finish_and_return)
	card.get_node("Home").pressed.connect(func(): travel(-2) if chapter >= 0 else travel(-1))
	card.get_node("Settings").pressed.connect(open_settings)
	card.get_node("Exit").pressed.connect(request_quit)
	settings.get_node("Back").pressed.connect(close_settings)
	settings.get_node("Volume").value_changed.connect(func(value): volume = value; apply_volume())
	settings.get_node("Motion").toggled.connect(func(value): motion = value)
	settings.get_node("Fullscreen").toggled.connect(func(value):
		fullscreen = value
		DisplayServer.window_set_mode(DisplayServer.WINDOW_MODE_FULLSCREEN if value else DisplayServer.WINDOW_MODE_WINDOWED))
	apply_volume()
	if fullscreen: DisplayServer.window_set_mode(DisplayServer.WINDOW_MODE_FULLSCREEN)
	call_deferred("on_scene_changed")

func _process(delta: float) -> void:
	music_delay = maxf(0.0, music_delay - delta)
	var scene := get_tree().current_scene
	var in_menu_or_map := is_instance_valid(scene) and scene.scene_file_path in [MENU,MAP] and not transitioning
	if in_menu_or_map and music_delay <= 0.0 and not $Music.playing: $Music.play()
	$Music.volume_db = move_toward($Music.volume_db, -24.0 if in_menu_or_map else -60.0, delta * 60.0)
	if not in_menu_or_map and $Music.volume_db <= -59.0: $Music.stop()

func _exit_tree() -> void:
	$Music.stop()

func fit_pages() -> void:
	var size := get_viewport().get_visible_rect().size
	var factor := minf(size.x / 1280.0, size.y / 720.0)
	for page in [$UI/Modal/Page, $UI/Curtain/Page]:
		page.scale = Vector2.ONE * factor
		page.position = (size - Vector2(1280,720) * factor) * 0.5

func read_progress() -> void:
	for path in [save_path, save_path + ".bak"]:
		var cfg := ConfigFile.new()
		if cfg.load(path) != OK or cfg.get_value("journey", "version", 0) != 1: continue
		var last = cfg.get_value("journey", "last", -1)
		if last is int: last_chapter = clampi(last, -1, 2)
		bottles.clear()
		var found = cfg.get_value("journey", "bottles", [])
		if found is Array:
			for id in found:
				if id is String and id not in bottles: bottles.append(id)
		var loudness = cfg.get_value("settings", "volume", 0.7)
		if loudness is float or loudness is int: volume = clampf(loudness,0.0,1.0)
		motion = cfg.get_value("settings", "motion", true) == true
		fullscreen = cfg.get_value("settings", "fullscreen", false) == true
		run_id = maxi(0,int(cfg.get_value("journey","run_id",0)))
		map_advance = cfg.get_value("map","advance",false) == true
		map_started = cfg.get_value("map","started",last_chapter>=0) == true
		map_unlocked = clampi(int(cfg.get_value("map","unlocked",maxi(0,last_chapter))),0,5)
		map_stop = clampi(int(cfg.get_value("map","stop",maxi(0,last_chapter))),0,map_unlocked)
		map_progress = clampf(float(cfg.get_value("map","progress",0.0)),0.0,1.0)
		map_revealed = clampf(float(cfg.get_value("map","revealed",map_progress)),map_progress,1.0)
		map_completed.clear()
		var done = cfg.get_value("map","completed",[])
		if done is Array:
			for id in done:
				if id is int and id >= 0 and id < 5 and id not in map_completed: map_completed.append(id)
		if not cfg.has_section("map"): migrate_completion()
		# A partial/edited config must not expose the final word ahead of the story.
		if not can_reveal():
			map_unlocked = mini(map_unlocked,4)
			map_stop = mini(map_stop,map_unlocked)
			map_progress = minf(map_progress,0.87)
			map_revealed = minf(map_revealed,0.87)
		return
	# Existing Polet saves remain in place when upgrading an older project.
	var polet := PoletSave.read_data()
	if not polet.is_empty():
		last_chapter = 1
		volume = polet.volume
		map_started = true
		map_stop = 1
		map_unlocked = 1
		migrate_completion()

func persist() -> bool:
	if test_mode: return true
	var cfg := ConfigFile.new()
	cfg.set_value("journey", "version", 1)
	cfg.set_value("journey","run_id",run_id)
	cfg.set_value("map","started",map_started)
	cfg.set_value("map","advance",map_advance)
	cfg.set_value("map","completed",map_completed)
	cfg.set_value("map","unlocked",map_unlocked)
	cfg.set_value("map","stop",map_stop)
	cfg.set_value("map","progress",map_progress)
	cfg.set_value("map","revealed",map_revealed)
	cfg.set_value("journey", "last", last_chapter)
	cfg.set_value("journey", "bottles", bottles)
	cfg.set_value("settings", "volume", volume)
	cfg.set_value("settings", "motion", motion)
	cfg.set_value("settings", "fullscreen", fullscreen)
	if cfg.save(save_path + ".tmp") != OK: return false
	var previous := ConfigFile.new()
	if previous.load(save_path) == OK and previous.get_value("journey","version",0) == 1:
		if DirAccess.copy_absolute(save_path, save_path + ".bak") != OK: return false
	return DirAccess.rename_absolute(save_path + ".tmp", save_path) == OK

func apply_volume() -> void:
	AudioServer.set_bus_volume_db(0, linear_to_db(maxf(volume,0.0001)))
	var scene := get_tree().current_scene
	if is_instance_valid(scene) and scene.has_node("Presentation") and chapter == 1:
		scene.get_node("Presentation").volume = volume
		scene.get_node("Story/Interface/Pause/Card/Volume").set_value_no_signal(volume)

func on_scene_changed() -> void:
	var scene := get_tree().current_scene
	if not is_instance_valid(scene) or scene == activated_scene or scene.get_meta("test_mode", false): return
	activated_scene = scene
	chapter = CHAPTERS.find(scene.scene_file_path)
	$UI/Shortcut.visible = chapter in [0,2] and not transitioning
	get_tree().auto_accept_quit = false
	if chapter < 0: return
	last_chapter = chapter
	map_started = true
	map_stop = chapter
	map_unlocked = maxi(map_unlocked,chapter)
	if chapter == 2:
		var hunt = scene.get_node("BottleHunt")
		for bottle in hunt.bottles:
			var id := str(scene.get_path_to(bottle))
			if id in bottles:
				bottle.is_collected = true
				bottle.hide()
				hunt.collected_count += 1
			bottle.collected.connect(on_bottle_collected)
		hunt.completed = hunt.collected_count == hunt.bottles.size()
		hunt._update_counter()
		if hunt.completed:
			hunt.get_node("HUD/Objective/Task").text = "Все бутылки найдены · можно гулять дальше."
			scene.get_node("Organizers")._on_hunt_completed()
		hunt.hunt_completed.connect(func(): call_deferred("open_ending"))
	if not persist(): show_error("Не удалось сохранить выбор главы. Проверь свободное место и доступ к папке сохранений.")

func on_bottle_collected(bottle: Node) -> void:
	var id := str(get_tree().current_scene.get_path_to(bottle))
	if id not in bottles: bottles.append(id)
	if not persist(): show_error("Бутылка найдена, но сохранение недоступно. Попробуй вернуться в меню позже.")

func migrate_completion() -> void:
	var polet := PoletSave.read_data()
	if not polet.is_empty() and polet.get("run_id",0) == run_id and "finale" in polet.finished:
		mark_finished(1)
	if bottles.size() >= 20: mark_finished(2)

func can_reveal() -> bool:
	for i in range(5):
		if i not in map_completed: return false
	return true

func mark_finished(index: int) -> void:
	if index < 0 or index > 4: return
	if index not in map_completed: map_completed.append(index)
	if index == map_stop: map_advance = true
	map_unlocked = maxi(map_unlocked,mini(index+1,4))
	if can_reveal(): map_unlocked = 5

func finish_and_return() -> void:
	if chapter == 0:
		mark_finished(0) # Temporary explicit end of the hall walkthrough, not a quest win.
	elif chapter == 1 and get_tree().current_scene.get_node("Story").stage == 6:
		mark_finished(1)
	elif chapter == 2 and get_tree().current_scene.get_node("BottleHunt").completed:
		mark_finished(2)
	travel(-2)

func start_new_game() -> void:
	if transitioning or get_tree().current_scene.scene_file_path != MENU: return
	var previous := [run_id,map_started,map_completed.duplicate(),map_unlocked,map_stop,map_progress,map_revealed,last_chapter,bottles.duplicate(),map_advance]
	run_id += 1
	map_started = true
	map_completed.clear()
	map_advance = false
	map_unlocked = 0
	map_stop = 0
	map_progress = 0.0
	map_revealed = 0.0
	last_chapter = -1
	bottles.clear()
	if not persist():
		run_id=previous[0];map_started=previous[1];map_completed.assign(previous[2]);map_unlocked=previous[3];map_stop=previous[4];map_progress=previous[5];map_revealed=previous[6];last_chapter=previous[7];bottles.assign(previous[8]);map_advance=previous[9]
		show_error("Не удалось начать новую историю. Прежнее прохождение сохранено.")
		return
	travel(-2)

func save_current() -> bool:
	var scene := get_tree().current_scene
	if is_instance_valid(scene) and chapter == 1:
		volume = scene.get_node("Presentation").volume
		if not scene.get_node("Story").save_progress(): return false
		if scene.get_node("Story").stage == 6: mark_finished(1)
	if is_instance_valid(scene) and chapter == 2 and scene.get_node("BottleHunt").completed:
		mark_finished(2)
	return persist()

func _input(event: InputEvent) -> void:
	if transitioning:
		get_viewport().set_input_as_handled()
		return
	if event.is_action_pressed("ui_cancel") and not event.is_echo():
		if modal.visible:
			if settings.visible: close_settings()
			else: resume()
		elif chapter in [0,2]: open_pause()
		else: return # Polet keeps its own puzzle/dialogue/pause semantics.
		get_viewport().set_input_as_handled()

func open_pause() -> void:
	if transitioning: return
	begin_modal()
	modal_kind = "pause"
	card.get_node("Title").text = TITLES[chapter] if chapter >= 0 else "Наши воспоминания"
	card.get_node("Text").text = "Можно ещё побыть здесь или вернуться к нити воспоминаний." if chapter < 2 else "Можно остаться на ночную прогулку или вернуться к другим воспоминаниям."
	card.get_node("Status").text = "WASD — ходить · Shift — бег · Tab — камера\nE — действие · Esc — вернуться" if chapter != 1 else "Прогресс последнего дня сохранится при переходе."
	card.get_node("Next").visible = chapter == 0 or (chapter == 2 and get_tree().current_scene.get_node("BottleHunt").completed)
	card.get_node("Next").text = "Закончить прогулку · к нити" if chapter == 0 else "Дальше по нити"
	card.get_node("Home").text = "Вернуться на карту" if chapter >= 0 else "Главное меню"
	card.get_node("Resume").grab_focus()

func open_ending() -> void:
	if chapter != 2 or transitioning: return
	mark_finished(2)
	if not persist():
		show_error("Не удалось сохранить завершение поиска. Можно остаться здесь и попробовать позже.")
		return
	open_pause()
	modal_kind = "ending"
	card.get_node("Title").text = "Ночь, которую запомним"
	card.get_node("Text").text = "Все бутылки найдены. В окнах ещё горит свет — можно остаться у реки или вернуться к другим воспоминаниям."
	card.get_node("Status").text = "Три места. Столько историй.\nИ ещё столько всего впереди."

func begin_modal() -> void:
	if not modal.visible:
		previous_mouse = Input.mouse_mode
		previous_focus = get_viewport().gui_get_focus_owner()
	get_tree().paused = true
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	modal.show()
	card.show()
	settings.hide()
	$UI/Shortcut.hide()

func resume() -> void:
	modal.hide()
	get_tree().paused = false
	Input.mouse_mode = previous_mouse
	$UI/Shortcut.visible = chapter in [0,2]
	if is_instance_valid(previous_focus) and previous_focus.is_visible_in_tree(): previous_focus.grab_focus()

func open_settings() -> void:
	settings_from_menu = not modal.visible
	begin_modal()
	card.hide()
	settings.show()
	settings.get_node("Volume").set_value_no_signal(volume)
	settings.get_node("Motion").set_pressed_no_signal(motion)
	settings.get_node("Fullscreen").set_pressed_no_signal(DisplayServer.window_get_mode() == DisplayServer.WINDOW_MODE_FULLSCREEN)
	settings.get_node("Back").grab_focus()

func close_settings() -> void:
	if not save_current():
		show_error("Не удалось записать настройки. Попробуй ещё раз после проверки папки сохранений.")
		return
	var scene := get_tree().current_scene
	if is_instance_valid(scene) and scene.scene_file_path == MENU:
		scene.get_node("Background").material.set_shader_parameter("motion", motion)
	if settings_from_menu: resume()
	else: open_pause()

func show_error(message: String) -> void:
	begin_modal()
	card.get_node("Title").text = "Не получилось открыть страницу"
	card.get_node("Text").text = message
	card.get_node("Next").hide()
	card.get_node("Status").text = "Текущий уровень остаётся доступен."
	card.get_node("Resume").grab_focus()

func request_quit() -> void:
	if transitioning: return
	if not save_current():
		show_error("Не удалось сохранить прогресс. Игра остаётся открытой, чтобы можно было попробовать ещё раз.")
		return
	get_tree().quit()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST and chapter != 1: request_quit()

func travel(target: int) -> void:
	if transitioning or target < -2 or target >= CHAPTERS.size(): return
	if target >= 0 and target > map_unlocked: return
	if not save_current():
		show_error("Не удалось сохранить прогресс. Переход отменён; можно продолжить игру и попробовать позже.")
		return
	transition_mouse = previous_mouse if modal.visible else Input.mouse_mode
	transitioning = true
	get_tree().paused = true
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	$UI/Shortcut.hide()
	curtain.show()
	curtain.modulate.a = 0.0
	$UI/Curtain/Page/Number.text = "НИТЬ ВОСПОМИНАНИЙ" if target < 0 else "ГЛАВА %02d" % (target + 1)
	$UI/Curtain/Page/Title.text = ("Нить воспоминаний" if target == -2 else "Наши воспоминания") if target < 0 else TITLES[target]
	$UI/Curtain/Page/Quote.text = "Некоторые моменты хочется прожить ещё раз." if target < 0 else QUOTES[target]
	$UI/Curtain/Page/Loading.text = "Открываем страницу…"
	var fade := create_tween().set_pause_mode(Tween.TWEEN_PAUSE_PROCESS)
	fade.tween_property(curtain, "modulate:a", 1.0, 0.4)
	await fade.finished
	modal.hide()
	var path: String = (MAP if target == -2 else MENU) if target < 0 else CHAPTERS[target]
	var error := ResourceLoader.load_threaded_request(path)
	if error != OK:
		transition_failed("Не удалось начать загрузку главы.")
		return
	var started := Time.get_ticks_msec()
	while true:
		var progress: Array = []
		var status := ResourceLoader.load_threaded_get_status(path, progress)
		if status == ResourceLoader.THREAD_LOAD_FAILED or status == ResourceLoader.THREAD_LOAD_INVALID_RESOURCE:
			transition_failed("Файлы главы не удалось загрузить. Текущий уровень сохранён.")
			return
		if status == ResourceLoader.THREAD_LOAD_LOADED: break
		$UI/Curtain/Page/Loading.text = "Открываем воспоминание · %d%%" % int(float(progress[0]) * 100) if not progress.is_empty() else "Открываем воспоминание…"
		await get_tree().process_frame
	var packed := ResourceLoader.load_threaded_get(path) as PackedScene
	# Let the chapter title breathe, including on a fast SSD.
	var remaining := maxf(0.0, 0.75 - float(Time.get_ticks_msec() - started) / 1000.0)
	await get_tree().create_timer(remaining, true).timeout
	error = get_tree().change_scene_to_packed(packed)
	if error != OK:
		transition_failed("Не удалось открыть сцену главы.")
		return
	await get_tree().scene_changed
	apply_volume()
	$UI/Curtain/Page/Loading.text = ""
	# New scene must remain frozen until it is fully visible.
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	fade = create_tween().set_pause_mode(Tween.TWEEN_PAUSE_PROCESS)
	fade.tween_property(curtain, "modulate:a", 0.0, 0.55)
	await fade.finished
	curtain.hide()
	transitioning = false
	apply_volume()
	if not modal.visible: get_tree().paused = false
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED if chapter == 2 and not modal.visible else Input.MOUSE_MODE_VISIBLE
	$UI/Shortcut.visible = chapter in [0,2] and not modal.visible

func transition_failed(message: String) -> void:
	curtain.hide()
	transitioning = false
	show_error(message)
	previous_mouse = transition_mouse
