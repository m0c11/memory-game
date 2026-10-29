extends SceneTree
var checks := 0
var failures: Array[String] = []
var level
var story
var flow
func _initialize() -> void: call_deferred("run")
func check(value: bool,label: String) -> void:
 checks += 1
 print("PASS " if value else "FAIL ",label)
 if not value: failures.append(label)
func frames(n: int = 4) -> void:
 for i in range(n): await physics_frame
func key(code: int) -> void:
 var event := InputEventKey.new();event.physical_keycode=code;event.keycode=code;event.pressed=true
 Input.parse_input_event(event);await frames()
 event=InputEventKey.new();event.physical_keycode=code;event.keycode=code
 Input.parse_input_event(event);await frames()
func walk_to(target: Vector3,label: String) -> void:
 var player=level.get_node("Player")
 for i in range(360):
  var diff: Vector3=target-player.global_position;diff.y=0
  if diff.length()<0.18:break
  var camera=player.overview_camera
  var right: Vector3=camera.global_basis.x;right.y=0;right=right.normalized()
  var forward: Vector3=-camera.global_basis.z;forward.y=0;forward=forward.normalized()
  var x: float=diff.normalized().dot(right);var z: float=diff.normalized().dot(forward)
  for action in ["move_left","move_right","move_forward","move_back"]:Input.action_release(action)
  Input.action_press("move_right" if x>0 else "move_left",absf(x))
  Input.action_press("move_forward" if z>0 else "move_back",absf(z))
  await physics_frame
 for action in ["move_left","move_right","move_forward","move_back"]:Input.action_release(action)
 await frames(8)
 var d: Vector3=target-player.global_position;d.y=0
 if d.length()>=0.5: print("BLOCKED ",label," at ",player.global_position)
 check(d.length()<0.5,label+" physically reachable")
func run() -> void:
 flow=root.get_node("Journey");flow.test_mode=true;flow.hall_state={}
 flow.map_completed.clear();flow.map_unlocked=0
 change_scene_to_file("res://scenes/levels/auditorium.tscn");await scene_changed;await frames(8)
 level=current_scene;story=level.get_node("Story")
 check(story.dialog.visible and story.busy,"Arrival intro opens")
 check(not level.get_node("Player").is_physics_processing(),"Dialogue locks movement")
 story.choose(0)
 check(level.get_node("Player").is_physics_processing(),"Choice restores movement")
 check(story.stage==0 and flow.map_unlocked==0,"Start has no completion")
 flow.finish_and_return();await frames()
 check(current_scene==level and not flow.transitioning,"Old finish handler cannot skip unfinished chapter")
 flow.open_pause();check(not flow.card.get_node("Next").visible,"Pause hides premature completion");flow.resume()
 check(story.get_node("Performance/Dancers").get_child_count()==20,"Twenty authored dancers")
 var heroes:=0
 for dancer in story.get_node("Performance/Dancers").get_children():
  if dancer.get_meta("heroine",false):heroes+=1
 check(heroes==1,"Exactly one Dasha in ensemble")
 check(story.cues.size()>20,"Recording has sparse audio-derived cues")
 var player=level.get_node("Player")
 player.global_position=Vector3(-4.5,.76,-4.5);await frames(8);await key(KEY_E)
 check(player.global_position.x< -15,"E enters left backstage")
 await walk_to(Vector3(-19.6,0,-2.65),"Path to belongings")
 await walk_to(Vector3(-20.65,0,-2.9),"Bag interaction area")
 story._process(.01);check(story.nearest=="Bag","Bag is available nearby")
 await key(KEY_E);await create_timer(2.0,false).timeout
 check(story.stage==1,"Actual E advances arrival")
 check(level.get_node("Backstage/Left/SportsBag").visible,"Placed bag remains in room")
 var saved: Dictionary=story.snapshot();story.restore(saved);story.refresh()
 check(story.stage==1,"Arrival checkpoint restores")
 player.enter_location(level.get_node("ReturnPoints/Left"),level.get_node("Cameras/ReferenceCamera"),"Зал")
 await walk_to(Vector3(-3.3,.734,-2.95),"Leave backstage along curtain")
 await walk_to(Vector3(-4.7,.734,-3.05),"Reach stair landing")
 await walk_to(Vector3(-6.02,.734,-3),"Cross stair landing")
 await walk_to(Vector3(-6.02,0,-.95),"Left stair base")
 await walk_to(Vector3(-6.02,.734,-3),"Stage stairs")
 await walk_to(Vector3(-4.7,.734,-3.05),"Stage landing")
 await walk_to(Vector3(-3.5,.734,-2.95),"Gap between curtain and monitor")
 await walk_to(Vector3(-3.5,.734,-4.25),"Route behind monitor")
 await walk_to(Vector3(-2.4,.734,-4.25),"Clear rehearsal approach")
 for i in range(3):
  await walk_to(story.get_node("Points/Rehearsal%d"%i).global_position,"Rehearsal %d"%i)
  story._process(.01)
  check(story.nearest=="Rehearsal%d"%i,"Correct rehearsal point active")
  await key(KEY_E);await create_timer(2.0,false).timeout
 check(story.stage==2 and story.rehearsal==3,"Three rehearsals unlock preparation")
 player.enter_location(level.get_node("Backstage/Left/Arrival"),level.get_node("Backstage/Left/OverviewCamera"),"Кулиса")
 await walk_to(Vector3(-18.9,0,-3),"Costume area")
 story._process(.01);await key(KEY_E);await create_timer(2.0,false).timeout
 check(story.costume_ready and story.stage==2,"Costume alone cannot start show")
 check(story.heroine.get_node("Facing/Rig/Scarf").visible,"Dasha wears black hip scarf")
 await walk_to(Vector3(-18.9,0,-4.9),"Mirror area")
 story._process(.01);await key(KEY_E);await create_timer(2.0,false).timeout
 check(story.makeup_ready and story.stage==3,"Mirror completes preparation")
 story.interact("Marina");story.choose(0);check(story.dialog.visible,"Marina reply branches")
 story.choose(0);check(story.spoke_marina and not story.busy,"Marina dialogue closes safely")
 story.interact("Gleb");story.choose(1);check(story.spoke_gleb,"Gleb optional conversation persists")
 story.interact("Ready");await frames()
 var option0: Control=story.dialog.get_node("Option0")
 var option1: Control=story.dialog.get_node("Option1")
 var option2: Control=story.dialog.get_node("Option2")
 check(option0.get_rect().end.y<=option1.position.y and option1.get_rect().end.y<=option2.position.y,"Three choices do not overlap")
 check(option2.get_rect().end.y<=story.dialog.size.y,"Last choice fits inside card")
 story.choose(2)
 saved=story.snapshot();story.restore(saved)
 check(story.stage==3 and story.spoke_gleb and story.spoke_marina,"Ready checkpoint round trip")
 story.start_dance(false);await frames(5)
 check(story.stage==4 and not player.visible and story.rhythm.visible,"Performance locks exploration and opens rhythm")
 check(story.get_node("Performance").visible,"Authored ensemble visible")
 story.dance_time=float(story.cues[0]);story.next_cue=0;story.cue_hit=false
 check(not story.hit_cue(1),"Wrong direction has no success")
 check(story.hit_cue(0),"Correct timed direction succeeds")
 check(not story.hit_cue(0),"Repeated input cannot farm cue")
 var before: float=story.get_node("DanceAudio").get_playback_position()
 flow.open_pause();await create_timer(.5,true).timeout
 check(absf(story.get_node("DanceAudio").get_playback_position()-before)<.1,"Pause freezes dance audio")
 flow.resume();story.rhythm.get_node("Watch").pressed.emit()
 check(story.watch_only,"Switch to watch mode during show")
 saved=story.snapshot();story.get_node("DanceAudio").stop();story.restore(saved)
 check(story.stage==3,"Interrupted dance resumes before show")
 story.start_dance(true);await frames()
 check(story.watch_only and not story.hit_cue(0),"Watch mode requires no inputs")
 story.finish_dance();await create_timer(2.7,false).timeout
 check(story.stage==5 and story.dialog.visible,"After-concert scene follows dance")
 check(flow.map_unlocked==0,"Dance alone cannot skip group finale")
 story.choose(0);await frames()
 check(story.ui.get_node("Subtitle").text.contains("ФКН"),"Corrected FKN chant")
 await create_timer(5.8,false).timeout
 check(story.stage==6 and 0 in flow.map_completed and flow.map_unlocked==1,"Finale unlocks Polet")
 check(story.snapshot().stage==6,"Completion in snapshot")
 story.choose(1)
 check(story.ui.get_node("FinalReturn").visible and not player.is_physics_processing(),"Final view keeps return button and locks hidden player")
 story.restore({"version":1,"stage":6,"rehearsal":0});check(story.stage==1,"Corrupt completion cannot skip rehearsal")
 story.restore({"version":1,"stage":6,"rehearsal":3,"costume":true});check(story.stage==2,"Corrupt completion cannot skip makeup")
 story.restore({"version":1,"stage":"bad","rehearsal":"bad"});check(story.stage==0,"Malformed fields fall back safely")
 flow.test_mode=false;flow.save_path="/tmp/studvesna-state-smoke.cfg";flow.hall_state=saved
 check(flow.persist(),"Hall shares atomic Journey save")
 flow.hall_state={};flow.read_progress();check(flow.hall_state==saved,"Hall survives disk round trip")
 for suffix in ["",".bak",".tmp"]:DirAccess.remove_absolute(flow.save_path+suffix)
 flow.test_mode=true;level.queue_free();flow.get_node("Music").stop();await frames(8);OS.delay_msec(100)
 print("STUDVESNA: %d checks, %d failures"%[checks,failures.size()]);quit(0 if failures.is_empty() else 1)
