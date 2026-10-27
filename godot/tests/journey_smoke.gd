extends SceneTree
## Real scene transitions, paused input, checkpoint preservation and album navigation.
## Run with an isolated XDG_DATA_HOME: this intentionally exercises actual Polet saves.
var checks := 0
var failures: Array[String] = []
var flow
func _initialize() -> void: call_deferred("run")
func check(value: bool, label: String) -> void:
	checks += 1
	print("PASS " if value else "FAIL ", label)
	if not value: failures.append(label)
func frames(count: int = 3) -> void:
	for i in range(count): await process_frame
func key(code: int) -> void:
	var event := InputEventKey.new()
	event.physical_keycode = code; event.keycode = code; event.pressed = true
	Input.parse_input_event(event)
	await frames()
	event = InputEventKey.new(); event.physical_keycode = code; event.keycode = code
	Input.parse_input_event(event)
	await frames()
func travel(index: int) -> void:
	flow.travel(index)
	var start := Time.get_ticks_msec()
	while flow.transitioning and Time.get_ticks_msec() - start < 45000:
		await process_frame
	check(not flow.transitioning, "Transition finishes to " + str(index))
	await frames(5)
func run() -> void:
	flow = root.get_node("Journey")
	# Do not risk touching a developer's or player's save when invoked incorrectly.
	if not OS.get_environment("XDG_DATA_HOME").contains("journey-smoke"):
		push_error("Use XDG_DATA_HOME=/tmp/journey-smoke/data to isolate player saves")
		quit(2); return
	PoletSave.write_data({"version":1,"finished":[]})
	flow.last_chapter = -1
	flow.bottles.clear()
	flow.save_path = "user://journey_smoke.cfg"
	change_scene_to_file(flow.MENU)
	await scene_changed
	await frames()
	check(current_scene.has_node("Page/Chapters/Chapter2"), "All three chapters authored in album")
	check(current_scene.get_node("Page/Continue").text == "Начать путешествие", "Fresh start opens first chapter")
	current_scene.select_chapter(2)
	check(current_scene.selected == 2 and current_scene.get_node("Page/Chapters/Chapter2").button_pressed, "Chapter selection updates without switching scene")
	flow.open_settings()
	check(paused and flow.settings.visible, "Settings freeze game and accept UI input")
	flow.settings.get_node("Volume").value = 0.35
	flow.settings.get_node("Motion").button_pressed = false
	await key(KEY_ESCAPE)
	check(not paused and not flow.modal.visible, "Esc closes settings and returns to album")
	check(is_equal_approx(flow.volume,0.35) and not flow.motion, "Settings kept")
	await travel(0)
	check(current_scene.scene_file_path == flow.CHAPTERS[0], "Album opens concert hall")
	flow.open_pause()
	var player = current_scene.get_node("Player")
	var before: Vector3 = player.global_position
	Input.action_press("move_forward"); await frames(20); Input.action_release("move_forward")
	check(player.global_position.is_equal_approx(before), "Pause freezes 3D movement")
	check(flow.card.get_node("Next").visible, "Hall has manual next-chapter action")
	await key(KEY_ESCAPE)
	check(not paused and not flow.modal.visible, "Esc resumes hall")
	# Double activation cannot enqueue a competing scene load.
	flow.travel(1); flow.travel(2)
	while flow.transitioning: await process_frame
	await frames(5)
	check(flow.chapter == 1 and current_scene.scene_file_path == flow.CHAPTERS[1], "Double activation guarded")
	check(is_equal_approx(current_scene.get_node("Presentation").volume, 0.35), "Album volume carries into Polet")
	var story = current_scene.get_node("Story")
	if story.busy: story.close_modal()
	story.finish_task("linen")
	story.open_pause()
	check(not story.pause_panel.get_node("Card/NextChapter").visible, "Polet next chapter waits for finale")
	check(story.pause_panel.get_node("Card/Album").visible, "Polet pause offers album")
	await travel(-1)
	check(PoletSave.read_data().finished.has("linen"), "Returning to album saves Polet checkpoint")
	check(flow.last_chapter == 1, "Album preserves last chapter for Continue")
	await travel(1)
	story = current_scene.get_node("Story")
	check(story.finished.has("linen"), "Continue restores Polet completed task")
	if story.busy: story.close_modal()
	for id in PoletSave.TASKS: story.finish_task(id)
	story.stage = story.Stage.DONE
	story.open_pause(true)
	check(story.pause_panel.get_node("Card/NextChapter").visible, "Polet ending offers Venevitino")
	await travel(2)
	check(current_scene.scene_file_path == flow.CHAPTERS[2], "Polet transitions to Venevitino")
	check(current_scene.get_node("Player").first_person and (DisplayServer.get_name() == "headless" or Input.mouse_mode == Input.MOUSE_MODE_CAPTURED), "FPS active after curtain (mouse capture checked with display)")
	var hunt = current_scene.get_node("BottleHunt")
	hunt.bottles[0].collect()
	await frames()
	check(flow.bottles.size() == 1, "Bottle stored immediately")
	await key(KEY_ESCAPE)
	check(paused and flow.modal.visible and Input.mouse_mode == Input.MOUSE_MODE_VISIBLE, "FPS pause frees mouse")
	await key(KEY_ESCAPE)
	check(not paused and (DisplayServer.get_name() == "headless" or Input.mouse_mode == Input.MOUSE_MODE_CAPTURED), "FPS resume (mouse capture checked with display)")
	await travel(-1)
	await travel(2)
	hunt = current_scene.get_node("BottleHunt")
	check(hunt.collected_count == 1 and hunt.bottles[0].is_collected and not hunt.bottles[0].visible, "Returning to Venevitino restores bottle and counter")
	for bottle in hunt.bottles: bottle.collect()
	await frames(5)
	check(flow.modal.visible and paused and flow.modal_kind == "ending", "All bottles open authored chapter ending")
	flow.resume()
	check(not paused, "Ending allows more exploration")
	await travel(-1)
	var count: int = flow.bottles.size()
	flow.bottles.clear(); flow.last_chapter = -1; flow.read_progress()
	check(flow.bottles.size() == count and flow.last_chapter == 2, "Journey save survives reload")
	check(current_scene.get_node("Page/Continue").text == "Продолжить путешествие", "Album offers Continue")
	await travel(2)
	check(current_scene.get_node("BottleHunt").collected_count == count and not current_scene.get_node("Organizers").enabled, "Completed hunt restores peaceful exploration")
	await travel(-1)
	flow.open_settings(); flow.close_settings()
	check(not paused, "Menu settings still usable after round trip")
	# Read-only path failure keeps the loaded scene and gives a recoverable modal.
	flow.save_path = "res://missing-parent/never.cfg"
	var original = current_scene
	flow.travel(0)
	check(current_scene == original and not flow.transitioning and flow.modal.visible, "Save failure cancels travel without losing current scene")
	flow.resume()
	flow.test_mode = true
	flow.get_node("Music").stop()
	current_scene.queue_free()
	await frames(8)
	flow.get_node("Music").stop()
	OS.delay_msec(100)
	await frames(3)
	print("JOURNEY: %d checks, %d failures" % [checks, failures.size()])
	quit(0 if failures.is_empty() else 1)
