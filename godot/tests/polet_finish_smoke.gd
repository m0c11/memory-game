extends SceneTree
## Save-file round trips, recovery, checkpoints, actors, presentation and modal safety.
var checks := 0
var failures: Array[String] = []
var level
const FILE := "/tmp/polet-finish-save-test.json"
func _initialize() -> void: call_deferred("run")
func check(value: bool, label: String) -> void:
	checks += 1
	if not value: failures.append(label); push_error(label)
func frames(count: int = 3) -> void:
	for i in range(count): await physics_frame
func fresh() -> void:
	level = load("res://scenes/levels/polet.tscn").instantiate()
	level.set_meta("test_mode", true)
	root.add_child(level)
	await frames()
func dispose() -> void:
	for audio in level.find_children("*", "AudioStreamPlayer", true, false): audio.stop()
	level.queue_free()
	await frames(4)
	OS.delay_msec(50)
	await process_frame
	await process_frame
func run() -> void:
	for path in [FILE,FILE+".bak",FILE+".tmp"]:
		if FileAccess.file_exists(path): DirAccess.remove_absolute(path)
	check(PoletSave.sanitize(null).is_empty(), "Malformed save is rejected")
	check(PoletSave.sanitize({"version":99,"finished":[]}).is_empty(), "Unknown schema does not load")
	var safe := PoletSave.sanitize({"version":1,"finished":["finale","music","unknown"],"outfit":99,"volume":-5,"switches":["bad"],"music_hits":20})
	check(safe.finished.is_empty(), "Impossible quest skips are discarded")
	check(safe.outfit == 2 and safe.volume == 0 and safe.music_hits == 2, "Untrusted numeric values are clamped")
	var data := {"version":1,"finished":["linen"],"outfit":1,"volume":0.4,"wind":false,"switches":[true,false,false],"music_hits":1}
	check(PoletSave.write_data(data, FILE) == OK, "Atomic first save succeeds")
	check(PoletSave.read_data(FILE).finished == ["linen"], "Saved checkpoint round-trips")
	data.finished.append("rubbish")
	check(PoletSave.write_data(data, FILE) == OK, "Second save succeeds")
	check(FileAccess.file_exists(FILE+".bak"), "Previous valid save is backed up")
	var broken := FileAccess.open(FILE,FileAccess.WRITE);broken.store_string("{incomplete");broken.close()
	check(PoletSave.read_data(FILE).finished == ["linen"], "Corrupt main save falls back to backup")
	check(PoletSave.write_data(data, FILE) == OK, "Save recovers after corrupted file")
	var checkpoints: Array = [[],["linen"],["linen","rubbish","mop"],["linen","rubbish","mop","child"],["linen","rubbish","mop","child","radio"],["linen","rubbish","mop","child","radio","circuit","garland"],["linen","rubbish","mop","child","radio","circuit","garland","music"],["linen","rubbish","mop","child","radio","circuit","garland","music","bicycle"],PoletSave.TASKS]
	var stages := [0,0,1,2,3,4,4,5,6]
	for i in range(checkpoints.size()):
		await fresh()
		data.finished = checkpoints[i]
		level.story.restore(data)
		await frames()
		check(level.story.stage == stages[i], "Restores story stage at checkpoint %d" % i)
		check(level.player.outfit == 1, "Restores costume at checkpoint %d" % i)
		check(level.story.circuit.rotations[1] == 1, "Restores partial puzzle at checkpoint %d" % i)
		check(is_equal_approx(level.get_node("Presentation").volume,0.4), "Restores volume at checkpoint %d" % i)
		check(not level.get_node("Presentation").wind, "Restores motion preference at checkpoint %d" % i)
		check(level.get_node("World/Room1/CleanBedNorth").visible == ("linen" in checkpoints[i]), "Restores clean bed visuals at checkpoint %d" % i)
		check(level.get_node("World/Glass/EveningGuests").visible == (i >= 7), "Restores final cast at checkpoint %d" % i)
		if i >= 3: check(not level.get_node("World/Camp/Child").visible, "Departed child stays departed after reload")
		await dispose()
	await fresh()
	for actor in [level.player,level.get_node("World/Camp/Child"),level.get_node("World/Camp/Director"),level.get_node("World/Camp/Colleague"),level.get_node("World/Glass/EveningGuests/Counselor2")]:
		check(actor.has_node("Sprite") and not actor.has_node("Capsule"), "Real character sprite replaces placeholder: " + actor.name)
		var sprite: AnimatedSprite2D = actor.get_node("Sprite")
		for facing in ["down","left","up","right"]:
			check(sprite.sprite_frames.get_frame_count("walk_"+facing) == 8, "Eight walk frames: " + actor.name + facing)
	level.player.global_position = Vector2(896,1440)
	Input.action_press("move_right")
	await frames(12)
	Input.action_release("move_right")
	check(level.player.facing == "right", "Walking changes facing")
	await frames()
	check(level.player.sprite.animation == "idle_right", "Stopping keeps facing")
	level.story.open_pause()
	var before: Vector2 = level.player.global_position
	Input.action_press("move_back")
	await frames(12)
	Input.action_release("move_back")
	check(level.player.global_position.is_equal_approx(before), "Pause prevents movement")
	level.story.confirm_restart()
	check(level.story.dialogue_kind == "restart", "Restart requires explicit confirmation")
	level.story.choose(1)
	check(not level.story.busy and level.story.stage == 0, "Cancelling restart resumes existing game")
	level.enter_location("room_1","Entrance")
	before = level.player.global_position
	level.set_overview(true)
	await frames()
	check(level.get_node("World/MapLabels").visible, "Map shows readable labels from indoors")
	check(level.get_node("World/MapLabels/Goal").position.is_equal_approx(Vector2(480,448)), "Map points to current objective")
	level.set_overview(false)
	check(level.player.global_position.is_equal_approx(before) and level.current_location.visible, "Closing map restores original room and position")
	check(level.player.sprite.material.get_shader() != null, "Costume shader is assigned")
	check(level.get_node("World/Camp").find_child("Canopy",true,false).material != null, "Canopy wind shader is assigned")
	check(level.get_node("Presentation/Grade").material != null, "Final colour treatment is assigned")
	for sound in ["Air","Success","Paper","Cloth","Sweep","Door","Click","Choice","Bell","Bump","Radio","Tick"]:
		check(level.get_node("Presentation/"+sound).stream.get_length()>0, "Audio loaded: " + sound)
	await dispose()
	for path in [FILE,FILE+".bak",FILE+".tmp"]:
		if FileAccess.file_exists(path):DirAccess.remove_absolute(path)
	print("Polet finish: %d checks, %d failures" % [checks,failures.size()])
	quit(0 if failures.is_empty() else 1)
