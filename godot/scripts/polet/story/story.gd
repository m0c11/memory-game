extends Node
## Quest state and interaction only. Every actor, prop and minigame is saved in a scene.
enum Stage { CLEAN, SUPPORT, RADIO, PREPARE, RIDE, FINALE, DONE }
var stage: Stage = Stage.CLEAN
var finished: Dictionary = {}
var busy: bool = false
var modal: String = ""
var dialogue_kind: String = ""
var dialogue_step: int = 0
var hold_target: Node2D
var hold_time: float = 0.0
var outfit: int = 0
@onready var level: Node2D = get_parent()
@onready var ui: CanvasLayer = $Interface
@onready var dialogue: Control = $Interface/Dialogue
@onready var circuit: Control = $Interface/Circuit
@onready var music: Control = $Interface/Music
@onready var bicycle: Control = $Interface/Bicycle

func _ready() -> void:
	dialogue.chosen.connect(choose)
	circuit.completed.connect(func(): finish_task("circuit"); close_modal())
	circuit.cancelled.connect(close_modal)
	music.completed.connect(func(): finish_task("music"); $PartyMusic.play(); close_modal())
	music.cancelled.connect(close_modal)
	bicycle.completed.connect(ride_complete)
	bicycle.cancelled.connect(close_modal)
	$PartyMusic.finished.connect(func(): $PartyMusic.play())
	call_deferred("refresh")

func _exit_tree() -> void:
	$PartyMusic.stop()

func _process(delta: float) -> void:
	if not is_instance_valid(level.current_location):
		return
	ui.get_node("Quest").visible = not busy and not level.overview
	ui.get_node("Hold").visible = false
	$PartyMusic.volume_db = -18.0 if str(level.current_location.get_meta("location_id")) == "glass" else -45.0
	if busy:
		return
	var target := nearest_target()
	if hold_target != null:
		if target != hold_target or not Input.is_action_pressed("interact") or level.overview:
			hold_target = null
			hold_time = 0.0
		else:
			hold_time += delta
			ui.get_node("Hold").visible = true
			ui.get_node("Hold").value = hold_time / 1.2 * 100.0
			if hold_time >= 1.2:
				var id := str(hold_target.get_meta("action_id"))
				hold_target = null
				hold_time = 0.0
				finish_task(id)

func enabled(id: String) -> bool:
	if finished.has(id):
		return false
	match id:
		"linen", "rubbish", "mop": return stage == Stage.CLEAN
		"child": return stage <= Stage.SUPPORT
		"radio": return stage <= Stage.RADIO
		"garland", "circuit", "music": return stage == Stage.PREPARE
		"bicycle": return stage == Stage.RIDE
		"finale": return stage == Stage.FINALE
		"outfit", "colleague": return stage != Stage.DONE
	return false

func nearest_target() -> Node2D:
	if busy or level.overview or not is_instance_valid(level.current_location):
		return null
	var nearest: Node2D
	var closest: float = 38.0 * 38.0
	for target: Node2D in get_tree().get_nodes_in_group("polet_action"):
		if not level.current_location.is_ancestor_of(target) or not enabled(str(target.get_meta("action_id"))):
			continue
		var distance := target.global_position.distance_squared_to(level.player.global_position)
		if distance > closest:
			continue
		# Interactions cannot reach through a wall or furniture.
		var query := PhysicsRayQueryParameters2D.create(level.player.global_position, target.global_position, 1)
		if not level.player.get_world_2d().direct_space_state.intersect_ray(query).is_empty():
			continue
		closest = distance
		nearest = target
	return nearest

func current_prompt() -> String:
	var target := nearest_target()
	if target == null:
		return ""
	var id := str(target.get_meta("action_id"))
	var prefix := "Удерживай E · " if id in ["linen", "rubbish", "mop", "garland"] else "E · "
	return prefix + str(target.get_meta("title"))

func try_interact() -> bool:
	var target := nearest_target()
	if target == null:
		return false
	var id := str(target.get_meta("action_id"))
	match id:
		"linen", "rubbish", "mop", "garland":
			hold_target = target
			hold_time = 0.0
		"child":
			if stage == Stage.SUPPORT:
				start_dialogue("child", "Последний отъезд", "Все уже уехали… А я не хочу, чтобы смена заканчивалась.", ["Мне тоже грустно расставаться.", "Какой момент смены ты запомнишь?", "Давай немного посидим вместе."])
			else:
				start_dialogue("chat", "Ребёнок", "Я подожду здесь. Ты закончишь уборку и придёшь?", ["Конечно, скоро вернусь."])
		"radio":
			if stage == Stage.RADIO:
				start_dialogue("radio", "Директриса", "Корпус принят, последний ребёнок уехал с родными. Спасибо! Осталось сдать рацию и ключи — и можно готовить наш вечер.", ["Передать рацию и ключи."])
			else:
				start_dialogue("chat", "Директриса", "Загляни ко мне после уборки и проводов последнего ребёнка. Я буду у админки.", ["Хорошо."])
		"circuit":
			open_modal("circuit")
			circuit.show()
			circuit.get_node("Card/Switch1").grab_focus()
		"music":
			if not finished.has("circuit") or not finished.has("garland"):
				start_dialogue("chat", "Подготовка музыки", "Сначала развесим гирлянду и включим все три линии на щитке. Потом проверим звук.", ["Хорошо."])
			else:
				open_modal("music")
				music.begin()
		"bicycle":
			start_dialogue("ride_intro", "Старый велосипед", "Всё готово, а угощений на всех мало. Прокатимся до магазина? Велосипед поскрипывает, но ещё повозит нас.", ["Поехали!", "Сначала ещё погуляю."])
		"finale":
			start_dialogue("finale", "Директриса", "Вот и закончилась смена. Сегодня вы убирали, провожали, поддерживали друг друга — и всё равно нашли силы собраться вместе.", ["Слушать дальше."])
		"outfit":
			start_dialogue("outfit", "Что надеть на вечер?", "Вещи уже собраны. Осталось выбрать, в чём провести последний вечер смены.", ["Любимая бирюзовая кофта.", "Тёплый терракотовый свитер.", "Светлая праздничная рубашка."])
		"colleague":
			start_dialogue("chat", "Вожатая", "Я соберу ребят к вечеру. Если ищешь стекляшку — она справа от центральной дорожки. А велосипед ждёт у ворот.", ["Встретимся вечером!"])
	return true

func finish_task(id: String) -> void:
	if finished.has(id):
		return
	finished[id] = true
	var room: Node2D = level.get_node("World/Room1")
	match id:
		"linen":
			room.get_node("Item128").hide()
			room.get_node("Item132").hide()
			room.get_node("CleanBedNorth").show()
			room.get_node("CleanBedSouth").show()
			room.get_node("FloorDetails/Item152").hide()
			room.get_node("FloorDetails/Item157").hide()
		"rubbish":
			for name in ["Item153", "Item154", "Item155"]:
				room.get_node("FloorDetails/" + name).hide()
		"mop": room.get_node("FloorDetails/Item156").hide()
		"garland": level.get_node("World/Glass/GarlandOff").show()
		"child":
			level.get_node("World/Camp/Child").hide()
			stage = Stage.RADIO
		"radio": stage = Stage.PREPARE
	if stage == Stage.CLEAN and finished.has_all(["linen", "rubbish", "mop"]):
		stage = Stage.SUPPORT
	if finished.has_all(["garland", "circuit"]):
		level.get_node("World/Glass/GarlandOff").hide()
		level.get_node("World/Glass/GarlandOn").show()
	if stage == Stage.PREPARE and finished.has_all(["garland", "circuit", "music"]):
		stage = Stage.RIDE
	refresh()

func refresh() -> void:
	var count := 0
	for id in ["linen", "rubbish", "mop"]:
		if finished.has(id): count += 1
	var objectives := [
		"01 / 06   Сдать корпус\nКорпус 1, сверху слева → комната\nБельё · мусор · пол: %d / 3" % count,
		"02 / 06   Не оставлять одного\nПоговори с ребёнком на центральной площадке.",
		"03 / 06   Смена сдана\nОтнеси рацию директрисе у админки справа внизу.",
		"04 / 06   Наш последний вечер\nВ стекляшке: гирлянда %s · щиток %s · музыка %s" % [mark("garland"), mark("circuit"), mark("music")],
		"05 / 06   За угощениями\nВелосипед у южных ворот.\nМожно заглянуть за нарядом в общагу.",
		"06 / 06   Все уже собрались\nВернись в стекляшку и поговори с директрисой.",
		"ПОЛЁТ · Последний день\nСмена закончилась. Воспоминания остаются.\nМожно ещё погулять по лагерю."
	]
	ui.get_node("Quest/Text").text = objectives[int(stage)]
	for target: Node2D in get_tree().get_nodes_in_group("polet_action"):
		if level.is_ancestor_of(target):
			target.visible = enabled(str(target.get_meta("action_id")))
	level.get_node("World/Glass/EveningGuests").visible = stage >= Stage.FINALE
	level.get_node("World/Camp/Director").visible = stage < Stage.FINALE
	level.get_node("World/Camp/Colleague").visible = stage < Stage.FINALE
	level.get_node("World/Daylight").color = Color(0.55, 0.62, 0.78) if stage >= Stage.FINALE else Color.WHITE
	level.get_node("World/Glass/EveningLight").enabled = stage >= Stage.FINALE
	level.get_node("World/Camp/Child/CollisionShape2D").set_deferred("disabled", stage > Stage.SUPPORT)
	for name in ["Director", "Colleague"]:
		level.get_node("World/Camp/" + name + "/CollisionShape2D").set_deferred("disabled", stage >= Stage.FINALE)
	for guest in level.get_node("World/Glass/EveningGuests").get_children():
		guest.get_node("CollisionShape2D").set_deferred("disabled", stage < Stage.FINALE)

func mark(id: String) -> String:
	return "✓" if finished.has(id) else "—"

func open_modal(kind: String) -> void:
	modal = kind
	busy = true
	hold_target = null
	hold_time = 0.0
	level.player.movement_enabled = false
	level.player.velocity = Vector2.ZERO
	level.prompt_label.text = ""
	ui.get_node("Hold").hide()

func close_modal() -> void:
	for panel in [dialogue, circuit, music, bicycle]:
		panel.hide()
	bicycle.running = false
	music.running = false
	modal = ""
	busy = false
	level.player.movement_enabled = not level.overview
	level.transition_cooldown = 0.25
	refresh()

func start_dialogue(kind: String, speaker: String, message: String, choices: Array) -> void:
	open_modal("dialogue")
	dialogue_kind = kind
	dialogue_step = 0
	dialogue.show_page(speaker, message, choices)

func choose(index: int) -> void:
	match dialogue_kind:
		"child":
			if dialogue_step == 0:
				var replies := ["Правда? Я думал, только мне так грустно…", "Наш вечерний огонёк. И как мы все вместе смеялись.", "Спасибо. Когда кто-то рядом, уже не так одиноко."]
				dialogue_step = 1
				dialogue.show_page("Ребёнок", replies[index], ["Давай сохраним это воспоминание.", "Можно скучать и ждать новых встреч."])
			elif dialogue_step == 1:
				dialogue_step = 2
				var reply := "Я нарисую наш отряд дома!" if index == 0 else "Я попробую написать ребятам, когда приеду домой."
				dialogue.show_page("Ребёнок", reply + "\nА вот и мои родные. Спасибо, что побыла со мной. До встречи!", ["До встречи. Береги себя!"])
			else:
				finish_task("child")
				close_modal()
		"radio":
			finish_task("radio")
			close_modal()
		"outfit":
			outfit = index
			var colors := [Color(0.22, 0.52, 0.58), Color(0.73, 0.34, 0.23), Color(0.93, 0.86, 0.66)]
			level.player.get_node("Jacket").color = colors[index]
			bicycle.get_node("Rider/Jacket").color = colors[index]
			close_modal()
		"ride_intro":
			close_modal()
			if index == 0:
				open_modal("bicycle")
				bicycle.begin()
		"ride_return":
			finished["bicycle"] = true
			stage = Stage.FINALE
			close_modal()
			level.enter_location("camp", "Glass")
		"finale":
			if dialogue_step == 0:
				dialogue_step = 1
				dialogue.show_page("Вожатые", "Спасибо за эту смену. За помощь, за шутки, за то, что были рядом.\nБез тебя этот лагерь был бы совсем другим.", ["Я тоже буду по вам скучать."])
			elif dialogue_step == 1:
				dialogue_step = 2
				dialogue.show_page("Последний огонёк", "За стеклом темнеет. Музыка играет тихо, и сегодня уже никуда не нужно спешить.\nЭтот день закончится. А всё хорошее ты заберёшь с собой.", ["Сохранить этот момент в памяти."])
			else:
				stage = Stage.DONE
				finished["finale"] = true
				close_modal()
		_: close_modal()

func ride_complete() -> void:
	bicycle.hide()
	start_dialogue("ride_return", "У магазина", "Доехали! Угощения и напитки в корзине.\nОбратно катимся по знакомой дороге — в стекляшке уже ждут друзья.", ["Вернуться в лагерь."])

func _unhandled_input(event: InputEvent) -> void:
	if not busy or (event is InputEventKey and event.echo):
		return
	if event.is_action_pressed("ui_cancel"):
		# Arrival and ending pages are checkpoints: finish the page before leaving.
		if dialogue_kind not in ["ride_return", "finale"] or modal != "dialogue":
			close_modal()
		get_viewport().set_input_as_handled()
	elif modal == "dialogue" and event is InputEventKey and event.pressed:
		var index := int(event.physical_keycode) - KEY_1
		if index >= 0 and index < 3 and dialogue.get_node("Card/Choice" + str(index + 1)).visible:
			choose(index)
			get_viewport().set_input_as_handled()
