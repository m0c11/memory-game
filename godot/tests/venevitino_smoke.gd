extends SceneTree
var scene: Node3D
var player: CharacterBody3D
var failures=[]
var total=0
func _initialize(): call_deferred("run")
func frames(n):
 for i in range(n):await physics_frame
func check(label,condition,detail=""):
 total+=1
 print("PASS " if condition else "FAIL ",label," ",detail)
 if not condition:failures.append(label)
func key(code):
 var e=InputEventKey.new();e.physical_keycode=code;e.pressed=true
 Input.parse_input_event(e);await frames(2)
 e=InputEventKey.new();e.physical_keycode=code;e.pressed=false
 Input.parse_input_event(e);await frames(2)
func place(pos,yaw=0.0):
 player.global_position=pos;player.rotation=Vector3(0,yaw,0);player.velocity=Vector3.ZERO
 await frames(15)
func walk(action,n):
 Input.action_press(action);await frames(n);Input.action_release(action);await frames(8)
func run():
 scene=load("res://scenes/levels/venevitino.tscn").instantiate();root.add_child(scene);current_scene=scene;player=scene.get_node("Player")
 await frames(20)
 check("spawn_on_ground",player.is_on_floor() and abs(player.position.y)<.08,str(player.position))
 check("first_person_default",player.first_person and root.get_camera_3d()==player.first_person_camera)
 check("41_cabins",scene.get_node("WestCabins").get_child_count()+scene.get_node("EastCabins").get_child_count()==41)
 var start=player.global_position
 await walk("move_forward",60)
 check("entrance_walk",player.global_position.distance_to(start)>3.3,str(player.position))
 await key(KEY_F);check("flashlight_off",not player.flashlight.visible)
 await key(KEY_F);check("flashlight_on",player.flashlight.visible)
 await key(KEY_TAB);check("overview",not player.first_person and root.get_camera_3d()==scene.get_node("MapCamera"))
 var geometries=get_nodes_in_group("distance_culled")
 check("overview_full_distance",geometries[0].visibility_range_end==0)
 await key(KEY_TAB);check("first_person_restore",player.first_person and geometries[0].visibility_range_end>0)
 var roompaths=[]
 for group in ["WestCabins", "EastCabins"]:
  for cabin in scene.get_node(group).get_children():roompaths.append(group+"/"+str(cabin.name))
 for roompath in roompaths:
  var cabin=scene.get_node(roompath)
  await place(cabin.to_global(Vector3(0,.03,6.5)),cabin.rotation.y)
  await walk("move_forward",70)
  var local=cabin.to_local(player.global_position)
  check(roompath+"_entry",local.z<3.2 and local.z>-.5 and abs(local.y-.5)<.09,str(local))
  await walk("move_back",85)
  local=cabin.to_local(player.global_position)
  check(roompath+"_exit",local.z>6 and abs(local.y)<.09,str(local))
 var c=scene.get_node("WestCabins/Cabin01")
 await place(c.to_global(Vector3(0,.55,-.4)))
 await walk("move_left",65)
 var local=c.to_local(player.global_position)
 check("cabin_furniture_collision",local.x> -2.13,str(local))
 await place(Vector3(1.5,.04,30));await walk("move_left",80)
 check("river_boundary",player.global_position.x>-.2 and player.is_on_floor(),str(player.position))
 await place(Vector3(186,.04,180));await walk("move_right",80)
 check("east_boundary",player.global_position.x<187.7,str(player.position))
 await place(Vector3(4,.05,89.1));await walk("move_left",55)
 check("pier_access",player.global_position.x<1.5 and player.position.y>.27 and player.is_on_floor(),str(player.position))
 await key(KEY_R);await frames(20)
 check("reset_entrance",player.global_position.distance_to(start)<.1,str(player.position))
 Input.action_press("jump");await frames(2);Input.action_release("jump");await frames(16)
 check("jump",player.position.y>.5)
 await frames(60);check("land",player.is_on_floor())
 print("VENEVITINO RESULT ",total," checks; failures=",failures)
 quit(0 if failures.is_empty() else 1)
