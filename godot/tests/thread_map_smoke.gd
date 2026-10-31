extends SceneTree
## Isolated map final reveal, legacy migration, route resume and save recovery.
var flow
var checks := 0
var failed: Array[String] = []
const FILE := "/tmp/thread-map-save-test.cfg"
func _initialize() -> void: call_deferred("run")
func check(value: bool, label: String) -> void:
	checks+=1
	print("PASS " if value else "FAIL ",label)
	if not value:failed.append(label)
func frames(count: int = 3) -> void:
	for i in range(count):await process_frame
func run() -> void:
	flow=root.get_node("Journey")
	flow.save_path=FILE
	flow.test_mode=true
	flow.map_started=true
	flow.map_completed.assign([0,1,2])
	flow.map_unlocked=3;flow.map_stop=3;flow.map_progress=0.80;flow.map_revealed=0.80;flow.map_advance=false
	change_scene_to_file(flow.MAP);await scene_changed;await frames()
	var map=current_scene
	check(map.get_node("Stops").get_child_count()==5,"Four authored chapter stops plus final knot")
	check(map.get_node("Route").curve.get_point_count()>100,"Route is saved Curve2D geometry")
	check(map.get_node("Thread").points.size()==map.get_node("Route").curve.get_point_count(),"Visible thread and movement share authored points")
	check(not map.revealing and not map.get_node("HUD/UI/Page/Ending").visible,"Unfinished chapters keep final camera closed")
	check(not map.get_node("Stops/Stop4").visible,"Final knot label hidden")
	map.go_to(4);check(map.target_stop==3,"Cannot jump past locked stops")
	map.go_to(2)
	var before: float=map.ball.progress
	await frames(20)
	check(map.ball.progress<before,"Replays roll backward along same curve")
	check(is_equal_approx(flow.map_revealed,0.80),"Replays keep all revealed thread")
	flow.open_settings()
	before=map.ball.progress;await frames(20)
	check(is_equal_approx(map.ball.progress,before),"Global settings pause the automatic movement")
	flow.close_settings()
	# Only the complete planned story enables the final segment.
	check(not flow.can_reveal(),"Three chapters still do not expose the surprise")
	flow.mark_finished(3);check(flow.can_reveal() and flow.map_unlocked==4,"Four chapters unlock final knot")
	flow.map_stop=3;flow.map_progress=0.80;flow.map_revealed=0.80;flow.map_advance=true
	change_scene_to_file(flow.MAP);await scene_changed;await frames()
	map=current_scene
	check(map.target_stop==4 and map.rolling,"Final segment starts automatically after last chapter")
	var until:=Time.get_ticks_msec()+20000
	while (map.rolling or not map.get_node("HUD/UI/Page/Ending").visible) and Time.get_ticks_msec()<until: await process_frame
	check(map.revealing and map.get_node("HUD/UI/Page/Ending").visible,"Final pan reveals dedication")
	check(map.get_node("Underline/Ball").progress_ratio == 1.0, "Ball completes authored underline before dedication")
	check(not map.get_node("HUD/UI/Page/Menu").visible, "Gift reveal hides menu until explicit Continue")
	check(map.camera.zoom.x<0.3,"Camera pulls back to the whole word")
	check(map.get_node("Stops").get_children().all(func(n):return not n.visible),"Chapter labels stay hidden during reveal")
	check(not map.get_node("HUD/UI/Page/Card").visible,"Gameplay card clears the final view")
	check(is_equal_approx(float(map.get_node("Thread").material.get_shader_parameter("revealed")),1.0),"Finale lights the complete strand")
	check(is_equal_approx(flow.map_progress,1.0),"Final arrival saved at end of route")
	change_scene_to_file(flow.MAP);await scene_changed;await frames()
	check(current_scene.revealing,"Reopening a completed journey replays final reveal")
	# Legacy album files remain usable; route does not require deleting old saves.
	var cfg:=ConfigFile.new()
	cfg.set_value("journey","version",1);cfg.set_value("journey","last",2);cfg.set_value("journey","bottles",["Bottles/One"])
	cfg.save(FILE)
	flow.map_completed.clear();flow.bottles.clear();flow.read_progress()
	check(flow.map_started and flow.map_unlocked>=2 and flow.map_stop==2,"Legacy album keeps its last available chapter")
	check(flow.bottles==["Bottles/One"],"Legacy bottle IDs preserved")
	check(not flow.can_reveal(),"Legacy visits cannot unlock final surprise")
	flow.test_mode=false
	check(flow.persist(),"Map saves atomically")
	flow.map_progress=0.20;check(flow.persist(),"Second map save creates backup")
	cfg=ConfigFile.new();cfg.set_value("journey","version",999);cfg.save(FILE)
	flow.read_progress()
	check(not is_equal_approx(flow.map_progress,0.20),"Unknown main schema recovers previous checkpoint")
	# Hostile/incomplete route flags cannot zoom out prematurely.
	cfg=ConfigFile.new();cfg.set_value("journey","version",1);cfg.set_value("map","unlocked",5);cfg.set_value("map","stop",5);cfg.set_value("map","progress",1.0);cfg.set_value("map","revealed",1.0);cfg.save(FILE)
	flow.read_progress()
	check(flow.map_unlocked<=3 and flow.map_progress<=0.80 and flow.map_revealed<=0.80,"Incomplete save clamps hidden final stretch")
	flow.test_mode=true
	for file in [FILE,FILE+".bak",FILE+".tmp"]:
		if FileAccess.file_exists(file):DirAccess.remove_absolute(file)
	current_scene.queue_free();await frames(8);flow.get_node("Music").stop();OS.delay_msec(100);await frames()
	print("THREAD MAP: %d checks, %d failures" % [checks,failed.size()])
	quit(0 if failed.is_empty() else 1)
