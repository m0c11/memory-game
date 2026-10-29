extends SceneTree
## Offline authoring: collapses repeated chair draw calls into a saved MultiMesh.
func _initialize():call_deferred("run")
func relative(node: Node3D, ancestor: Node) -> Transform3D:
 var result=node.transform;var parent=node.get_parent()
 while parent != ancestor:
  if parent is Node3D:result=parent.transform*result
  parent=parent.get_parent()
 return result
func run():
 var chair=load("res://scenes/props/auditorium_chair.tscn").instantiate()
 var groups={}
 for n in chair.find_children("*","MeshInstance3D",true,false):
  for i in range(n.mesh.get_surface_count()):
   var mat=n.get_active_material(i)
   if not groups.has(mat):groups[mat]=[]
   groups[mat].append([n.mesh,i,relative(n,chair)])
 var combined=ArrayMesh.new()
 for mat in groups:
  var st=SurfaceTool.new();st.begin(Mesh.PRIMITIVE_TRIANGLES);st.set_material(mat)
  for part in groups[mat]:st.append_from(part[0],part[1],part[2])
  st.commit(combined)
 ResourceSaver.save(combined,"res://assets/optimized/auditorium_chair.res")
 var seating=load("res://scenes/architecture/seating.tscn").instantiate()
 var chairs=[]
 for n in seating.find_children("*","Node3D",true,false):
  if n.scene_file_path in ["res://scenes/props/auditorium_chair.tscn", "res://scenes/props/chair_collision.tscn"]:chairs.append(n)
 var buffer=PackedFloat32Array()
 for chair_node in chairs:
  var t=relative(chair_node,seating)
  buffer.append_array([t.basis.x.x,t.basis.y.x,t.basis.z.x,t.origin.x,t.basis.x.y,t.basis.y.y,t.basis.z.y,t.origin.y,t.basis.x.z,t.basis.y.z,t.basis.z.z,t.origin.z])
 # Dummy/headless rendering does not retain MultiMesh buffers for ResourceSaver.
 # Author the native serialized buffer explicitly so GPU rendering gets all transforms.
 var file=FileAccess.open("res://scenes/architecture/chairs_batched.tscn",FileAccess.WRITE)
 file.store_string('[gd_scene load_steps=3 format=3]\n[ext_resource type="ArrayMesh" path="res://assets/optimized/auditorium_chair.res" id="Mesh"]\n[sub_resource type="MultiMesh" id="Seats"]\ntransform_format = 1\ninstance_count = %d\nmesh = ExtResource("Mesh")\nbuffer = %s\n[node name="ChairsBatched" type="MultiMeshInstance3D"]\nmultimesh = SubResource("Seats")\n'%[chairs.size(),var_to_str(buffer)])
 file.close()
 print("Authored ",chairs.size()," chairs in ",combined.get_surface_count()," surfaces")
 chair.free();seating.free();quit()
