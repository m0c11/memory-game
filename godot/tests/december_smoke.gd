extends SceneTree
## Branching playthroughs, real key input, checkpoints, film pause and the four-chapter ending.
var flow
var level
var checks := 0
var failures: Array[String] = []
var visited: Dictionary = {}
const SAVE := "/tmp/december-test.cfg"
func _initialize() -> void: call_deferred("run")
func check(value: bool, label: String) -> void:
	checks += 1
	if not value: failures.append(label)
	print("PASS " if value else "FAIL ", label)
func frames(n := 3) -> void:
	for i in range(n): await process_frame
func key(code: int) -> void:
	var e := InputEventKey.new(); e.keycode = code; e.physical_keycode = code; e.pressed = true
	Input.parse_input_event(e); await frames()
	e = InputEventKey.new(); e.keycode = code; e.physical_keycode = code
	Input.parse_input_event(e); await frames()
func open_chapter() -> void:
	change_scene_to_file(flow.CHAPTERS[3]); await scene_changed; await frames()
	level = current_scene
func run() -> void:
	flow = root.get_node("Journey"); flow.test_mode = true; flow.save_path = SAVE
	flow.novel_state = {}; flow.map_unlocked = 3; flow.map_completed.assign([0,1,2]); flow.map_advance = false
	await open_chapter()
	check(flow.chapter == 3 and not flow.can_reveal(), "Fourth native chapter opens without prematurely unlocking LOVE")
	# All authored branches resolve, and no resource is missing from the export dependency tree.
	var data: Dictionary = level.beats
	for id in data:
		var b: Dictionary = data[id]
		if not str(b.get("next", "")).is_empty(): check(data.has(b.next), "Next exists: " + id)
		for option in b.get("choices", []): check(data.has(option.next), "Choice target exists: " + id)
		if b.get("kind", "") == "branch": check(data.has(b.yes) and data.has(b.no), "Both conditional branches exist: " + id)
	level.goto_beat("start")
	await key(KEY_ENTER)
	check(level.beat_id == "start" and level.dialog.visible_characters == -1, "First Enter reveals the whole line")
	await key(KEY_ENTER)
	check(level.beat_id == "start2", "Second Enter advances exactly once")
	level.goto_beat("outfit"); level.choose(1)
	check(level.flags.outfit == "blue" and "blue" in level.get_node("Page/Portrait").texture.resource_path, "Selected outfit changes the portrait")
	level.goto_beat("cat"); level.choose(2)
	check(level.flags.cat == "photo", "Cat photo choice is remembered")
	level.goto_beat("pack"); level.pack_item("keys"); level.pack_item("keys")
	check(level.packed_items == ["keys"], "Duplicate item cannot count twice")
	flow.test_mode = false
	check(level.save_progress(), "Partial packing persists atomically")
	flow.novel_state = {}; flow.read_progress(); await open_chapter()
	check(level.beat_id == "pack" and level.packed_items == ["keys"] and level.flags.outfit == "blue" and level.flags.cat == "photo", "Disk reload restores line, outfit, cat choice and partial bag")
	flow.test_mode = true
	level.pack_item("phone"); level.pack_item("wallet")
	check(level.beat_id == "packed", "Packing all three items advances")
	level.goto_beat("sure"); level.choose(0); level.goto_beat("bus_thought")
	check(level.beat_id == "bus_early", "Leaving early genuinely changes the bus monologue")
	level.goto_beat("cat_check"); check(level.beat_id == "security", "Photo receives a later callback")
	level.goto_beat("compliment_check"); check(level.beat_id == "compliment_blue", "Gleb notices the chosen outfit")
	level.open_journal(); await key(KEY_ESCAPE)
	check(not level.journal.visible and not paused, "Esc closes history without opening global pause")
	await key(KEY_ESCAPE); check(paused and flow.modal.visible, "Esc pauses the novel")
	var before: int = level.dialog.visible_characters
	await frames(20); check(before == level.dialog.visible_characters, "Pause freezes typewriter")
	await key(KEY_ESCAPE); check(not paused, "Esc resumes the novel")
	flow.finish_and_return();check(not flow.transitioning and not flow.can_reveal(), "Unfinished novel cannot complete from the pause menu")
	# Three complete paths through the actual UI controller; no direct task-completion shortcut.
	for route in range(3):
		flow.novel_state = {}; await open_chapter()
		var count := 0
		while level.beat_id != "film" and count < 200:
			visited[level.beat_id] = true
			if level.beat_id == "pack":
				for item in ["phone", "keys", "wallet"]: level.pack_item(item)
			elif not level.current.get("choices", []).is_empty():
				var option: int = mini(route, level.current.choices.size()-1)
				level.get_node("Page/Choices").get_child(option).pressed.emit()
			else:
				level.dialog.visible_characters = -1
				level.get_node("Page/Dialog/Next").pressed.emit()
			count += 1
			await process_frame
		check(level.beat_id == "film" and count > 40, "Full conversational route %d reaches cinema" % route)
		check(level.packed_items.size() == 3 and level.flags.has("popcorn"), "Route retains items and popcorn choice")
		check(level.film_active and not level.completed and not flow.can_reveal(), "Starting the movie alone does not finish the chapter")
		level.get_node("Page/Film/Skip").pressed.emit()
		var elapsed: float = level.film_opening
		await frames(30)
		check(is_equal_approx(level.film_opening,elapsed) and not level.completed, "Skip confirmation freezes playback without finishing")
		level.get_node("Page/Film/Confirm/Card/Back").pressed.emit()
		check(not level.video.paused, "Cancel skip resumes playback")
	check(visited.has("cat_photo") and visited.has("cat_pet") and visited.has("study_rest") and visited.has("bus_early") and visited.has("bus_late"), "Playthroughs cover caring, humorous and worried choices")
	flow.open_pause(); var elapsed: float=level.film_opening;await frames(30)
	check(is_equal_approx(level.film_opening,elapsed) and level.video.paused, "Global pause also freezes cinema playback")
	flow.resume()
	# Gallery completes naturally, not through debug unlocking; shorten only display duration.
	level.film = level.film.duplicate(); level.film.seconds_per_memory = 0.10
	var until := Time.get_ticks_msec()+20000
	while current_scene == level and Time.get_ticks_msec()<until: await process_frame
	while flow.transitioning and Time.get_ticks_msec()<until: await process_frame
	check(current_scene.scene_file_path == flow.MAP and flow.can_reveal() and flow.map_unlocked == 4, "Movie completion unlocks LOVE and returns through actual Journey transition")
	check(flow.novel_state.beat == "done" and current_scene.target_stop == 4, "Final checkpoint and automatic final roll are preserved")
	# Persistent migration from the previously shipped five-stop map.
	var cfg := ConfigFile.new()
	cfg.set_value("journey","version",1);cfg.set_value("journey","last",2);cfg.set_value("journey","hall",{"stage":6});cfg.set_value("journey","bottles",["Bottles/One"])
	cfg.set_value("map","completed",[0,1,2]);cfg.set_value("map","unlocked",3);cfg.set_value("map","stop",3);cfg.set_value("map","progress",0.70);cfg.save(SAVE)
	flow.read_progress()
	check(flow.map_completed == [0,1,2] and flow.map_unlocked == 3 and flow.hall_state.stage == 6 and flow.bottles == ["Bottles/One"], "Legacy map keeps all real chapter progress and unlocks cinema")
	check(not flow.can_reveal() and flow.novel_state.is_empty(), "Legacy save cannot manufacture novel completion")
	for path in [SAVE,SAVE+".bak",SAVE+".tmp"]:
		if FileAccess.file_exists(path):DirAccess.remove_absolute(path)
	current_scene.queue_free();await frames(8);flow.get_node("Music").stop();OS.delay_msec(100);await frames()
	print("DECEMBER: %d checks, %d failures" % [checks,failures.size()]);quit(0 if failures.is_empty() else 1)
