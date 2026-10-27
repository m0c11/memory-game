extends SceneTree

var scene: Node3D
var watch: Node3D
var player: CharacterBody3D
var hunt: Node
var failures: Array[String] = []
var checks := 0

func _initialize():call_deferred("run")
func frames(count):
	for i in range(count):await physics_frame
func check(label, condition):
	checks += 1
	print("PASS " if condition else "FAIL ",label)
	if not condition:failures.append(label)
func place_guard(guard, pos, yaw=0.0):
	guard.global_position=pos
	guard.rotation=Vector3(0,yaw,0)
	guard.velocity=Vector3.ZERO
	guard.suspicion=0.0
	guard.state=OrganizerPatrol.State.PATROL
	guard.pause_time=0.0
func place_player(pos):
	player.global_position=pos
	player.velocity=Vector3.ZERO
	await frames(4)
func run():
	scene=load("res://scenes/levels/venevitino.tscn").instantiate()
	scene.set_meta("test_mode", true)
	root.add_child(scene);current_scene=scene
	watch=scene.get_node("Organizers");player=scene.get_node("Player");hunt=scene.get_node("BottleHunt")
	var map_ready_deadline=Time.get_ticks_msec()+10000
	var navigation_map=watch.guards[0].agent.get_navigation_map()
	while NavigationServer3D.map_get_iteration_id(navigation_map)==0 and Time.get_ticks_msec()<map_ready_deadline:
		await physics_frame
	# An iteration can precede asynchronous region synchronization on a busy CPU.
	var route=watch.guards[0].route
	while NavigationServer3D.map_get_path(navigation_map,route.get_child(0).global_position,route.get_child(1).global_position,true).size()<2 and Time.get_ticks_msec()<map_ready_deadline:
		OS.delay_msec(1)
		await physics_frame
	check("saved navigation map synchronized",NavigationServer3D.map_get_iteration_id(navigation_map)>0)
	await frames(20)
	check("three authored organizers",watch.guards.size()==3)
	var starts=[]
	for guard in watch.guards:starts.append(guard.global_position)
	await frames(180)
	for i in range(watch.guards.size()):
		var guard=watch.guards[i]
		check(str(guard.name)+" walks on ground",guard.global_position.distance_to(starts[i])>2 and guard.is_on_floor())
		var map=guard.agent.get_navigation_map()
		for k in range(guard.route.get_child_count()):
			var a=guard.route.get_child(k).global_position
			var b=guard.route.get_child((k+1)%guard.route.get_child_count()).global_position
			var path=NavigationServer3D.map_get_path(map,a,b,true)
			check(str(guard.name)+" route segment "+str(k),path.size()>1 and path[-1].distance_to(b)<.8)
	await frames(5100)
	for guard in watch.guards:
		print("PATROL ",guard.name," pos=",guard.global_position," visited=",guard.visited_waypoints)
		check(str(guard.name)+" advances past corners",guard.visited_waypoints>=3 and guard.is_on_floor())
	# Freeze other patrols to isolate sight and pursuit using the real world geometry.
	watch.set_physics_process(false)
	for guard in watch.guards:
		guard.set_physics_process(false)
		guard.can_detect=false
	var guard: OrganizerPatrol=watch.guards[0]
	place_guard(guard,Vector3(69.3,.03,190))
	await place_player(Vector3(69.3,.03,184))
	check("sees player in front",guard.sees_player())
	await place_player(Vector3(69.3,.03,196))
	check("does not see behind",not guard.sees_player())
	await place_player(Vector3(69.3,.03,172))
	check("vision has a distance limit",not guard.sees_player())
	# The west wall of Cabin10 separates the guard from the player inside.
	place_guard(guard,Vector3(25.5,.03,122.54),-PI/2)
	await place_player(Vector3(29.26,.53,122.54))
	check("real cabin wall blocks vision",not guard.sees_player())
	place_guard(guard,Vector3(69.3,.03,190))
	await place_player(Vector3(69.3,.03,184))
	guard.can_detect=true
	guard.set_physics_process(true)
	await frames(35)
	check("recognition is gradual",guard.suspicion>0 and guard.suspicion<1 and guard.state==OrganizerPatrol.State.PATROL)
	await frames(65)
	check("recognition starts pursuit",guard.state==OrganizerPatrol.State.CHASE)
	var distance=guard.global_position.distance_to(player.global_position)
	await frames(35)
	check("pursuit closes the distance",guard.global_position.distance_to(player.global_position)<distance-.8)
	var last=guard.last_seen
	await place_player(Vector3(150,.03,220))
	await frames(5)
	check("loss of sight searches last seen location",guard.state==OrganizerPatrol.State.SEARCH and guard.last_seen.distance_to(last)<.2)
	await frames(320)
	check("search times out and returns to patrol",guard.state in [OrganizerPatrol.State.RETURN,OrganizerPatrol.State.PATROL])
	# A genuine close encounter must preserve collected bottles, reset once and give grace.
	guard.set_physics_process(false)
	place_guard(guard,Vector3(69.3,.03,190))
	await place_player(Vector3(69.3,.03,189.15))
	hunt.bottles[0].collect()
	watch.grace_time=0.0
	watch.set_physics_process(true)
	guard.suspicion=1.0
	guard.state=OrganizerPatrol.State.CHASE
	guard.set_physics_process(true)
	await frames(8)
	check("encounter returns player to entrance",watch.encounters==1 and player.global_position.distance_to(player.spawn_transform.origin)<.15)
	check("encounter preserves bottle progress",hunt.collected_count==1 and hunt.bottles[0].is_collected)
	check("encounter explains what happened",watch.warning.visible)
	check("encounter gives a grace period",watch.grace_time>8.0 and not guard.can_detect)
	watch._on_player_caught(guard)
	check("simultaneous encounters cannot repeat",watch.encounters==1)
	for bottle in hunt.bottles:bottle.collect()
	await frames(5)
	check("completed hunt stops pursuit",not watch.enabled and watch.guards.all(func(g):return not g.can_detect and g.suspicion==0.0))
	watch.grace_time=0.0
	watch._on_player_caught(guard)
	check("completed hunt cannot send player back",watch.encounters==1)
	print("ORGANIZER RESULT ",checks," checks; failures=",failures)
	hunt.pickup_sound.stop()
	scene.queue_free()
	await process_frame
	OS.delay_msec(100)
	await process_frame
	await process_frame
	quit(0 if failures.is_empty() else 1)
