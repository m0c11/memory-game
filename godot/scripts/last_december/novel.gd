extends Control
## Native authored UI/background scenes; this script only controls story and playback.
@export var film: DecemberFilm
const STORY = preload("res://resources/last_december/story.tres")
const PORTRAITS := {
	"dasha_blue_smile": preload("res://assets/last_december/dasha_blue_smile.svg"),
	"dasha_blue_soft": preload("res://assets/last_december/dasha_blue_soft.svg"),
	"dasha_blue_worried": preload("res://assets/last_december/dasha_blue_worried.svg"),
	"dasha_cream_smile": preload("res://assets/last_december/dasha_cream_smile.svg"),
	"dasha_cream_soft": preload("res://assets/last_december/dasha_cream_soft.svg"),
	"dasha_cream_worried": preload("res://assets/last_december/dasha_cream_worried.svg"),
	"gleb_default_smile": preload("res://assets/last_december/gleb_default_smile.svg"),
	"gleb_default_soft": preload("res://assets/last_december/gleb_default_soft.svg"),
	"gleb_default_worried": preload("res://assets/last_december/gleb_default_worried.svg"),
	"mother_default_smile": preload("res://assets/last_december/mother_default_smile.svg"),
	"mother_default_soft": preload("res://assets/last_december/mother_default_soft.svg"),
	"mother_default_worried": preload("res://assets/last_december/mother_default_worried.svg"),
}
var beats: Dictionary
var beat_id := "start"
var flags: Dictionary = {"outfit": "cream"}
var packed_items: Array[String] = []
var history: Array[String] = []
var current: Dictionary
var completed := false
var film_active := false
var film_elapsed := 0.0
var film_index := 0
var film_opening := 0.0
var characters_shown := 0.0
var background_name := ""
var fade_tween: Tween
var finishing := false
@onready var journey = get_node("/root/Journey")
@onready var page: Control = $Page
@onready var dialog: Label = $Page/Dialog/Text
@onready var choices: VBoxContainer = $Page/Choices
@onready var video: VideoStreamPlayer = $Page/Film/Video
@onready var journal: Control = $Page/Journal

func _ready() -> void:
	beats = STORY.beats
	get_viewport().size_changed.connect(fit_page)
	fit_page()
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	$Page/Dialog/Next.pressed.connect(advance)
	$Page/Dialog.gui_input.connect(func(event):
		if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
			advance(); accept_event())
	for i in range(3):
		choices.get_child(i).pressed.connect(choose.bind(i))
	for i in range(3):
		$Page/Bag.get_child(i).pressed.connect(pack_item.bind(["phone", "keys", "wallet"][i]))
	$Page/History.pressed.connect(open_journal)
	$Page/Journal/Card/Close.pressed.connect(close_journal)
	$Page/Pause.pressed.connect(journey.open_pause)
	$Page/Film/Skip.pressed.connect(func():
		set_story_paused(true); $Page/Film/Confirm.show(); $Page/Film/Confirm/Card/Back.grab_focus())
	$Page/Film/Confirm/Card/Back.pressed.connect(func():
		$Page/Film/Confirm.hide(); set_story_paused(false); $Page/Film/Skip.grab_focus())
	$Page/Film/Confirm/Card/Continue.pressed.connect(finish_film)
	$Page/Blackout/Retry.pressed.connect(return_to_thread)
	$Page/Blackout/Retry.hide()
	video.finished.connect(finish_film)
	var saved: Dictionary = journey.novel_state
	if saved.get("version", 0) == 1 and beats.has(str(saved.get("beat", ""))):
		beat_id = str(saved.beat)
		var saved_flags = saved.get("flags", {})
		if saved_flags is Dictionary: flags.merge(saved_flags, true)
		for item in saved.get("packed", []):
			if item in ["phone", "keys", "wallet"] and item not in packed_items: packed_items.append(item)
		for entry in saved.get("history", []):
			if entry is String: history.append(entry)
		# Replaying a completed chapter is a fresh read, without erasing global completion.
		if beat_id == "done":
			beat_id = "start"; flags = {"outfit": "cream"}; packed_items.clear(); history.clear()
	$Music.finished.connect(func():
		if not film_active or film.video == null: $Music.play())
	$Music.play()
	goto_beat(beat_id, true)

func fit_page() -> void:
	var size := get_viewport_rect().size
	var factor := minf(size.x / 1280.0, size.y / 720.0)
	page.scale = Vector2.ONE * factor
	page.position = (size - Vector2(1280,720)*factor)*0.5

func goto_beat(id: String, restoring := false) -> void:
	if finishing or not beats.has(id): return
	var candidate: Dictionary = beats[id]
	if candidate.get("kind", "") == "branch":
		goto_beat(candidate.yes if flags.get(candidate.flag) == candidate.equals else candidate.no, restoring)
		return
	beat_id = id
	current = candidate
	if current.get("kind", "") == "done":
		completed = true
		return
	var bg: String = current.background
	if bg != background_name:
		background_name = bg
		for child in $Page/Backgrounds.get_children(): child.visible = child.name == bg
		if fade_tween: fade_tween.kill()
		$Page/SceneFade.modulate.a = 0.65
		fade_tween = create_tween()
		fade_tween.tween_property($Page/SceneFade, "modulate:a", 0.0, 0.5 if journey.motion else 0.0)
	$Page/Chapter.text = current.get("chapter", chapter_for(bg))
	$Page/Location.text = {"room":"КВАРТИРА · ВЕЧЕР", "kitchen":"КУХНЯ · ТЁПЛЫЙ СВЕТ", "stop":"ОСТАНОВКА · ДЕКАБРЬ", "bus":"ГОРОД ЗА ОКНОМ", "foyer":"КИНОТЕАТР · УСПЕЛИ", "cinema":"РЯДОМ · ПЕРЕД СЕАНСОМ"}[bg]
	var who: String = current.get("portrait", "")
	$Page/Portrait.visible = not who.is_empty()
	if who == "cat":
		$Page/Portrait.texture = preload("res://assets/last_december/irzhik.svg")
	else:
		if not who.is_empty():
			var outfit: String = str(flags.get("outfit", "cream")) if who == "dasha" else "default"
			if outfit not in ["cream", "blue", "default"]: outfit = "cream"
			$Page/Portrait.texture = PORTRAITS["%s_%s_%s" % [who, outfit, current.get("mood", "soft")]]
	$Page/Dialog/Speaker.text = current.speaker if not str(current.speaker).is_empty() else "ПОСЛЕДНИЙ СЕАНС ДЕКАБРЯ"
	dialog.text = current.text
	dialog.visible_characters = 0
	characters_shown = 0.0
	var options: Array = current.get("choices", [])
	choices.visible = not options.is_empty()
	for i in range(3):
		var button: Button = choices.get_child(i)
		button.visible = i < options.size()
		if button.visible: button.text = options[i].text
	$Page/Bag.visible = current.get("kind", "") == "pack"
	update_bag()
	$Page/Dialog/Next.visible = options.is_empty() and not $Page/Bag.visible
	$Page/Dialog/Next.text = "Дальше  →"
	$Page/Phone.visible = current.get("phone", false) and options.is_empty()
	if $Page/Phone.visible:
		$Page/Phone/Sender.text = current.speaker
		$Page/Phone/Message.text = current.text
		$Foley.stream = preload("res://assets/polet/audio/bell.wav"); $Foley.play()
	$Page/Clip.visible = current.get("kind", "") == "clip"
	if $Page/Clip.visible:
		$Page/Clip/Image.texture = preload("res://assets/last_december/irzhik.svg") if id == "clip1" else PORTRAITS["dasha_%s_worried" % flags.get("outfit", "cream")]
		$Page/Clip/Caption.text = "Ещё пять минут…" if id == "clip1" else "Нет, всё-таки первый свитер."
	if current.get("sound", "") == "keys":
		$Foley.stream = preload("res://assets/polet/audio/bell.wav"); $Foley.play()
	if not restoring and not str(current.text).is_empty():
		history.append((str(current.speaker) + ": " if not str(current.speaker).is_empty() else "") + str(current.text))
		while history.size() > 120: history.pop_front()
	if not save_progress(): journey.show_error("Не удалось сохранить разговор. Можно продолжить и попробовать сохранить позже.")
	if current.get("kind", "") == "film":
		start_film()
	elif choices.visible:
		choices.get_child(0).grab_focus()
	elif $Page/Bag.visible:
		for child in $Page/Bag.get_children():
			if not child.disabled: child.grab_focus(); break
	else:
		$Page/Dialog/Next.grab_focus()

func chapter_for(bg: String) -> String:
	return {"room":"ДОМА", "kitchen":"НА КУХНЕ", "stop":"В ДОРОГЕ", "bus":"В ДОРОГЕ", "foyer":"ВСТРЕЧА", "cinema":"ПОСЛЕДНИЙ СЕАНС ДЕКАБРЯ"}[bg]

func advance() -> void:
	if film_active or journal.visible or finishing or journey.transitioning: return
	if dialog.visible_characters >= 0 and dialog.visible_characters < dialog.text.length():
		dialog.visible_characters = -1
		characters_shown = dialog.text.length()
		return
	if choices.visible or $Page/Bag.visible: return
	var next: String = current.get("next", "")
	if not next.is_empty(): goto_beat(next)

func choose(index: int) -> void:
	if film_active or journal.visible or finishing or journey.transitioning: return
	var options: Array = current.get("choices", [])
	if index < 0 or index >= options.size(): return
	var option: Dictionary = options[index]
	flags.merge(option.get("flags", {}), true)
	history.append("Даша → " + str(option.text))
	$Foley.stream = preload("res://assets/polet/audio/choice.wav"); $Foley.play()
	goto_beat(option.next)

func pack_item(id: String) -> void:
	if beat_id != "pack" or id in packed_items or id not in ["phone", "keys", "wallet"]: return
	packed_items.append(id)
	$Foley.stream = preload("res://assets/polet/audio/paper.wav"); $Foley.play()
	update_bag()
	if packed_items.size() == 3:
		goto_beat("packed")
	else:
		if not save_progress(): journey.show_error("Не удалось сохранить сборы. Выбранные вещи пока остаются в сумке.")
		for child in $Page/Bag.get_children():
			if not child.disabled: child.grab_focus(); break

func update_bag() -> void:
	for i in range(3):
		var taken: bool = ["phone", "keys", "wallet"][i] in packed_items
		$Page/Bag.get_child(i).disabled = taken
		$Page/Bag.get_child(i).text = ["Телефон", "Ключи", "Кошелёк"][i] + ("  ✓" if taken else "  → в сумку")

func save_progress() -> bool:
	journey.novel_state = {"version": 1, "beat": beat_id, "flags": flags.duplicate(true), "packed": packed_items.duplicate(), "history": history.duplicate()}
	return journey.persist()

func open_journal() -> void:
	if film_active: return
	$Page/Journal/Card/Scroll/Entries.text = "\n\n".join(history)
	journal.show()
	$Page/Journal/Card/Close.grab_focus()

func close_journal() -> void:
	journal.hide()
	if choices.visible: choices.get_child(0).grab_focus()
	else: $Page/Dialog/Next.grab_focus()

func set_story_paused(value: bool) -> void:
	video.paused = value or $Page/Film/Confirm.visible
	$Music.stream_paused = value

func start_film() -> void:
	film_active = true
	film_elapsed = 0.0; film_index = 0; film_opening = 0.0
	$Page/Film.show()
	$Page/Film/Memory.hide()
	$Page/Film/Caption.hide()
	$Page/Film/Title.show()
	$Page/Film/MemoryShade.hide()
	$Page/Film/Skip.grab_focus()
	$Page/Film/Video.visible = film.video != null
	if film.video:
		video.stream = film.video
		$Music.stop()
	else:
		show_memory(0)

func show_memory(index: int) -> void:
	if film.memories.is_empty(): return
	$Page/Film/Memory.texture = film.memories[index]
	$Page/Film/Caption.text = film.captions[index] if index < film.captions.size() else ""
	$Page/Film/Memory.modulate.a = 0.0

func _process(delta: float) -> void:
	if journal.visible or journey.transitioning: return
	if not film_active:
		if dialog.visible_characters >= 0:
			characters_shown += delta * 48.0
			dialog.visible_characters = int(characters_shown)
		return
	if $Page/Film/Confirm.visible: return
	if film_opening < 2.5:
		film_opening += delta
		if film_opening >= 2.5:
			$Page/Film/Title.hide()
			if film.video: video.play()
			else: $Page/Film/Memory.show(); $Page/Film/Caption.show(); $Page/Film/MemoryShade.show()
		return
	if film.video: return
	film_elapsed += delta
	if film.memories.is_empty():
		finish_film(); return
	var seconds: float = film.seconds_per_memory
	$Page/Film/Memory.modulate.a = clampf(minf(film_elapsed, seconds - film_elapsed), 0.0, 1.0)
	if film_elapsed >= seconds:
		film_elapsed = 0.0
		film_index += 1
		if film_index >= film.memories.size(): finish_film()
		else: show_memory(film_index)

func finish_film() -> void:
	if not film_active or finishing: return
	video.stop()
	$Page/Film/Confirm.hide()
	$Page/Film.hide()
	$Page/Blackout.show()
	film_active = false
	completed = true
	beat_id = "done"
	return_to_thread()

func return_to_thread() -> void:
	if not completed or finishing: return
	if not save_progress():
		$Page/Blackout/Retry.show()
		$Page/Blackout/Retry.grab_focus()
		journey.show_error("Не удалось сохранить финал. После закрытия паузы можно попробовать ещё раз кнопкой «Дальше по нити».")
		return
	journey.finish_and_return()
	finishing = journey.transitioning
	$Page/Blackout/Retry.visible = not finishing

func _exit_tree() -> void:
	$Music.stop()
	video.stop()
