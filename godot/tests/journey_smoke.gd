extends SceneTree
## Actual menu → automatic map → levels → map; isolate all player files.
var flow
var checks := 0
var failures: Array[String] = []
func _initialize() -> void: call_deferred("run")
func check(value: bool, label: String) -> void:
	checks += 1
	print("PASS " if value else "FAIL ",label)
	if not value: failures.append(label)
func frames(count: int = 3) -> void:
	for i in range(count): await process_frame
func wait_transition() -> void:
	var until := Time.get_ticks_msec()+40000
	while flow.transitioning and Time.get_ticks_msec()<until: await process_frame
	check(not flow.transitioning,"Scene transition completes")
	await frames(4)
func travel(index: int) -> void:
	flow.travel(index)
	await wait_transition()
func arrive() -> void:
	var until := Time.get_ticks_msec()+10000
	while current_scene.rolling and Time.get_ticks_msec()<until: await process_frame
	check(not current_scene.rolling,"Yarn arrives automatically")
	await frames(8)
func key(code: int) -> void:
	var event := InputEventKey.new();event.keycode=code;event.physical_keycode=code;event.pressed=true
	Input.parse_input_event(event);await frames()
	event = InputEventKey.new();event.keycode=code;event.physical_keycode=code
	Input.parse_input_event(event);await frames()
func run() -> void:
	if not OS.get_environment("XDG_DATA_HOME").contains("journey-smoke"):
		push_error("Run with XDG_DATA_HOME=/tmp/journey-smoke/data");quit(2);return
	flow=root.get_node("Journey")
	flow.save_path="user://thread_journey_test.cfg"
	flow.map_started=false
	change_scene_to_file(flow.MENU);await scene_changed;await frames()
	check(current_scene.has_node("Page/Start") and not current_scene.has_node("Page/Chapters"),"Simple start menu replaces chapter album")
	check(current_scene.get_node("Page/Continue").disabled,"No Continue before first run")
	flow.start_new_game();await wait_transition();await arrive()
	check(current_scene.scene_file_path==flow.MAP and flow.map_unlocked==0,"Start opens first map stop only")
	check(is_equal_approx(flow.map_progress,0.055),"Arrival stores exact first stop")
	check(float(current_scene.get_node("Thread").material.get_shader_parameter("revealed"))<0.06,"Future thread is concealed")
	check(not current_scene.get_node("Stops/Stop1").visible,"Future chapter labels stay hidden")
	check(not flow.can_reveal(),"Final word is locked")
	flow.travel(2);await frames()
	check(current_scene.scene_file_path==flow.MAP and not flow.transitioning,"Cannot bypass chapter gate")
	current_scene.enter_chapter();flow.travel(1);await wait_transition()
	check(flow.chapter==0,"Double activation cannot skip the hall")
	if current_scene.get_node("Story").dialog.visible: current_scene.get_node("Story").choose(0)
	flow.open_pause()
	var pos: Vector3=current_scene.get_node("Player").global_position
	Input.action_press("move_forward");await frames(12);Input.action_release("move_forward")
	check(pos.is_equal_approx(current_scene.get_node("Player").global_position),"Pause freezes hall movement")
	await key(KEY_ESCAPE)
	check(not paused,"Esc resumes 3D level")
	await travel(-2);await arrive()
	check(flow.map_unlocked==0 and current_scene.target_stop==0,"Leaving unfinished hall does not complete it")
	await travel(0)
	var hall=current_scene.get_node("Story")
	if hall.dialog.visible:hall.choose(0)
	flow.finish_and_return()
	check(not flow.transitioning and flow.map_unlocked==0,"Unfinished hall cannot complete")
	hall.complete_action("bag")
	for i in range(3):hall.complete_action("rehearsal")
	hall.complete_action("costume");hall.complete_action("makeup")
	await travel(-2);await arrive();await travel(0)
	hall=current_scene.get_node("Story")
	check(hall.stage==3 and hall.costume_ready and hall.makeup_ready,"Hall preparation survives actual reload")
	hall.start_dance(true);hall.finish_dance()
	await create_timer(2.7).timeout
	hall.choose(0);await create_timer(5.8).timeout
	check(hall.stage==6,"Hall finale completes before transition")
	flow.finish_and_return();await wait_transition()
	check(current_scene.rolling and current_scene.target_stop==1,"Hall finish starts automatic roll toward Polet")
	check(current_scene.get_node("HUD/UI/Page/Card/Enter").disabled,"Cannot enter chapter while rolling")
	await frames(30)
	var middle: float=flow.map_progress
	check(middle>0.055 and middle<0.26,"Yarn travels along the saved curve")
	await travel(-1)
	check(not current_scene.get_node("Page/Continue").disabled,"Continue becomes available")
	current_scene.start_game();await frames()
	check(current_scene.get_node("NewGame").visible,"New run requires confirmation")
	current_scene.get_node("NewGame").hide()
	var epoch: int=flow.run_id
	check(flow.run_id==epoch and flow.map_unlocked==1,"Dismissing confirmation preserves progress")
	flow.open_settings();flow.settings.get_node("Volume").value=0.35;await key(KEY_ESCAPE)
	check(not paused and is_equal_approx(flow.volume,0.35),"Settings save and return to main menu")
	flow.read_progress()
	check(is_equal_approx(flow.map_progress,middle),"Mid-roll position survives disk round trip")
	await travel(-2)
	check(flow.map_progress>=middle,"Continue resumes the unfinished segment")
	await arrive()
	check(current_scene.target_stop==1 and is_equal_approx(flow.map_progress,0.26),"Continue arrives at Polet")
	await travel(1)
	var story=current_scene.get_node("Story")
	check(story.finished.is_empty(),"New run ignores older Polet checkpoints")
	if story.busy:story.close_modal()
	story.finish_task("linen")
	await travel(-2);await arrive()
	check(current_scene.target_stop==1 and flow.map_unlocked==1,"Incomplete Polet stays at its own stop")
	await travel(1);story=current_scene.get_node("Story")
	check(story.finished.has("linen"),"Same run restores Polet task")
	if story.busy:story.close_modal()
	for id in PoletSave.TASKS:story.finish_task(id)
	story.stage=story.Stage.DONE
	story.open_pause(true)
	story.pause_panel.get_node("Card/NextChapter").pressed.emit()
	await wait_transition();await arrive()
	check(current_scene.target_stop==2 and 1 in flow.map_completed,"Polet finale returns through map to Venevitino")
	await travel(2)
	check(DisplayServer.get_name()=="headless" or Input.mouse_mode==Input.MOUSE_MODE_CAPTURED,"FPS captures cursor after transition")
	var hunt=current_scene.get_node("BottleHunt")
	hunt.bottles[0].collect();await frames()
	await key(KEY_ESCAPE)
	check(paused and flow.modal.visible,"FPS pause freezes world")
	await key(KEY_ESCAPE);check(not paused,"FPS resumes")
	await travel(-2);await arrive();await travel(2)
	hunt=current_scene.get_node("BottleHunt")
	check(hunt.collected_count==1,"Bottles survive map excursion")
	for bottle in hunt.bottles:bottle.collect()
	await frames(5)
	check(flow.modal.visible and flow.modal_kind=="ending","Last bottle opens ending")
	flow.finish_and_return();await wait_transition();await arrive()
	check(current_scene.target_stop==3 and flow.map_unlocked==3,"Third chapter reaches reserved corporate stop")
	check(current_scene.get_node("HUD/UI/Page/Card/Enter").disabled,"Unbuilt chapter cannot open missing resource")
	check(not flow.can_reveal() and not current_scene.revealing,"Three levels do not expose final LOVE")
	current_scene.go_to(2);await arrive()
	check(flow.map_revealed>=0.70 and is_equal_approx(flow.map_progress,0.48),"Replay moves back without erasing thread")
	await travel(2)
	check(not current_scene.get_node("Organizers").enabled,"Completed hunt restores peaceful exploration")
	await travel(-1)
	flow.start_new_game();await wait_transition();await arrive()
	check(flow.run_id==epoch+1 and flow.bottles.is_empty() and flow.map_completed.is_empty() and flow.hall_state.is_empty(),"New run resets route and bottles using a new save epoch")
	flow.mark_finished(0);await travel(1)
	check(current_scene.get_node("Story").finished.is_empty(),"New run cannot resurrect old Polet save or backup")
	await travel(-1)
	flow.save_path="res://missing-directory/never.cfg"
	var saved_epoch: int=flow.run_id
	flow.start_new_game()
	check(flow.run_id==saved_epoch and flow.modal.visible and not flow.transitioning,"Failed new-run write rolls back all progress")
	flow.resume();flow.test_mode=true
	current_scene.queue_free();await frames(6)
	flow.get_node("Music").stop();OS.delay_msec(100);await frames()
	print("JOURNEY MAP: %d checks, %d failures" % [checks,failures.size()])
	quit(0 if failures.is_empty() else 1)
