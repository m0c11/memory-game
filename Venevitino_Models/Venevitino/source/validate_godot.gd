extends SceneTree
var issues: Array[String] = []
var meshes := 0
var surfaces := 0

func _initialize() -> void:
	call_deferred("run_checks")

func inspect_node(node: Node, filename: String) -> void:
	if node is MeshInstance3D:
		meshes += 1
		for i in range(node.mesh.get_surface_count()):
			surfaces += 1
			var mat = node.get_active_material(i)
			if not mat is BaseMaterial3D:
				issues.append(filename + ": missing material")
			elif mat.albedo_texture == null or mat.normal_texture == null:
				issues.append(filename + ": missing albedo/normal texture")
	for child in node.get_children():
		inspect_node(child, filename)

func run_checks() -> void:
	var count := 0
	for filename in DirAccess.get_files_at("res://models"):
		if not filename.ends_with(".glb"):
			continue
		count += 1
		var packed = load("res://models/" + filename)
		if not packed is PackedScene:
			issues.append(filename + ": cannot load PackedScene")
			continue
		var instance = packed.instantiate()
		root.add_child(instance)
		inspect_node(instance, filename)
		if filename == "door_hinged_090x210.glb":
			var hinge = instance.find_child("Door_Hinge", true, false)
			if hinge == null:
				issues.append("Door_Hinge not found")
			else:
				var origin = hinge.global_position
				hinge.rotate_y(deg_to_rad(100.0))
				if hinge.global_position.distance_to(origin) > 0.00001:
					issues.append("Door pivot moved")
		instance.free()
	var report = {"engine": Engine.get_version_info().string, "files": count, "mesh_instances": meshes, "surfaces": surfaces, "errors": issues, "passed": count == 59 and issues.is_empty()}
	var f = FileAccess.open("res://godot_validation.json", FileAccess.WRITE)
	f.store_string(JSON.stringify(report, "  "))
	print("GODOT_VALIDATION ", JSON.stringify(report))
	quit(0 if report.passed else 1)
