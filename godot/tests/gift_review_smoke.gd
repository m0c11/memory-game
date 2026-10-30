extends SceneTree
## UX regressions reproduced during the gift review: pausing, orientation and optional help.
var failures: Array[String] = []
var checks := 0
var flow
func _initialize(): call_deferred("run")
func check(value: bool, message: String) -> void:
 checks+=1
 print("PASS " if value else "FAIL ",message)
 if not value: failures.append(message)
func frames(count: int=3) -> void:
 for i in range(count):await physics_frame
func key(code: int) -> void:
 var e=InputEventKey.new();e.keycode=code;e.physical_keycode=code;e.pressed=true
 Input.parse_input_event(e);await frames()
 e=InputEventKey.new();e.keycode=code;e.physical_keycode=code
 Input.parse_input_event(e);await frames()
func dispose(scene):
 scene.queue_free();await frames(4);current_scene=null
func run():
 flow=root.get_node("Journey");flow.test_mode=true
 var scene=load("res://scenes/levels/polet.tscn").instantiate()
 scene.set_meta("test_mode",true);root.add_child(scene);current_scene=scene;flow.chapter=1
 await frames(4)
 var story=scene.story
 for id in ["linen","rubbish","mop"]:story.finish_task(id)
 check(story.get_node("Interface/Quest/Text").text.contains("1: сдана"),"Quest distinguishes completed room")
 check(str(scene.get_node("World/Corridor1/RoomDoor").get_meta("prompt")).contains("сдана"),"Door remembers room completion")
 check(str(scene.get_node("World/Corridor1/LeftDoor").get_meta("prompt")).contains("0/3"),"Next room has its own counter")
 story.open_modal("bicycle");story.bicycle.begin();story.bicycle.distance=800.0
 await key(KEY_ESCAPE)
 var bike=story.bicycle
 check(story.modal=="bicycle" and bike.trip_paused and not bike.running,"Escape pauses ride instead of leaving")
 var before: float=bike.distance
 Input.action_press("sprint");await frames(60);Input.action_release("sprint")
 check(is_equal_approx(bike.distance,before),"Paused ride never advances")
 await key(KEY_ESCAPE)
 check(bike.running and not bike.trip_paused and bike.distance>=before,"Escape resumes same ride")
 bike.pause_trip();bike.get_node("Pause/Card/Leave").pressed.emit()
 check(not story.busy and not story.finished.has("bicycle"),"Explicit leave returns without awarding completion")
 story.open_modal("bicycle");bike.begin(true);bike.set_process(false)
 bike.distance=1800.0;bike.stamina=44.0;bike.balance=62.0
 bike.pause_trip();bike.resume_trip()
 check(bike.returning and bike.distance==1800.0 and bike.stamina==44.0 and bike.balance==62.0,"Return trip retains distance, energy and basket across pause")
 bike.stamina=5.0;bike.advance(.1,0.0,true,false)
 check(bike.exhausted,"Low energy locks sprint instead of flickering every frame")
 bike.advance(.1,0.0,false,false)
 check(not bike.exhausted,"Releasing sprint allows recovery")
 await dispose(scene)
 flow.chapter=-1
 scene=load("res://scenes/levels/venevitino.tscn").instantiate()
 scene.set_meta("test_mode",true);root.add_child(scene);current_scene=scene
 await frames(20)
 var p=scene.get_node("Player");var watch=scene.get_node("Organizers");var hunt=scene.get_node("BottleHunt")
 check(not hunt.hint_enabled and not hunt.get_node("HintMarker").visible,"Hint is opt-in")
 await key(KEY_H)
 check(hunt.hint_enabled and hunt.hint_target!=null and hunt.hint_spot!=null,"H selects an uncollected hiding place")
 check(hunt.get_node("HUD/SearchHint/Text").text.contains("м"),"Hint explains direction and distance")
 check(not hunt.get_node("HintMarker").visible,"No through-wall marker during walking")
 await key(KEY_TAB)
 var pos: Vector3=p.position;var grace: float=watch.grace_time
 var guards: Array=[]
 for g in watch.guards:guards.append([g.position,g.suspicion,g.waypoint])
 check(hunt.get_node("HintMarker").visible,"Optional marker is visible on plan")
 check(p.get_node("PlanMarker").visible,"Player position is readable on map")
 check(not scene.get_node("LushVegetation").visible,"Fine foliage does not obscure map")
 Input.action_press("move_forward");await frames(120);Input.action_release("move_forward")
 check(p.position.is_equal_approx(pos),"Walking keys cannot move an unseen player on map")
 check(is_equal_approx(watch.grace_time,grace),"Map inspection does not consume encounter grace")
 for i in range(watch.guards.size()):
  var guard=watch.guards[i]
  check(guard.position.is_equal_approx(guards[i][0]) and guard.suspicion==guards[i][1] and guard.waypoint==guards[i][2],"Map suspends patrol and suspicion: "+guard.name)
 var count: int=watch.encounters
 watch.grace_time=0;watch._on_player_caught(watch.guards[0])
 check(watch.encounters==count,"Catch cannot occur while reading map")
 await key(KEY_TAB)
 check(p.first_person and not hunt.get_node("HintMarker").visible,"Leaving map restores walking and hides map marker")
 var original=hunt.hint_target
 original.collect();await frames()
 check(hunt.hint_target!=original and not hunt.hint_target.is_collected,"Hint switches away from a collected bottle")
 await key(KEY_H)
 check(not hunt.hint_enabled and not hunt.get_node("HUD/SearchHint").visible,"Help can be dismissed")
 for spot in hunt.inspection_spots:
  check(spot.has_meta("hint_text") and str(spot.get_meta("hint_text")).length()>10,"Authored clue: "+spot.name)
 for bottle in hunt.bottles:bottle.collect()
 await key(KEY_H)
 check(not hunt.hint_enabled,"Completed hunt cannot direct to collected items")
 await dispose(scene)
 flow.hall_state={"stage":2};flow.map_completed.clear();flow.map_stop=0;flow.map_progress=.055;flow.map_unlocked=2
 scene=load(flow.MAP).instantiate();root.add_child(scene);current_scene=scene;await frames()
 scene.rolling=false;scene.refresh()
 check(scene.ui.get_node("Card/Enter").text.contains("Продолжить"),"Map distinguishes a saved chapter")
 flow.map_completed.append(0);scene.refresh()
 check(scene.ui.get_node("Card/Enter").text.contains("Вернуться"),"Map distinguishes a completed memory")
 scene.target_stop=3;scene.refresh()
 check(scene.ui.get_node("Card/Enter").disabled,"Unwritten chapter remains honestly unavailable")
 await dispose(scene)
 flow.get_node("Music").stop()
 OS.delay_msec(80)
 await process_frame
 print("GIFT REVIEW: ",checks," checks, ",failures.size()," failures")
 quit(0 if failures.is_empty() else 1)
