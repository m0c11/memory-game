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
 var mm=MultiMesh.new();mm.transform_format=MultiMesh.TRANSFORM_3D;mm.mesh=combined;mm.instance_count=chairs.size()
 for i in range(chairs.size()):mm.set_instance_transform(i,relative(chairs[i],seating))
 var visual=MultiMeshInstance3D.new();visual.name="ChairsBatched";visual.multimesh=mm
 var packed=PackedScene.new();packed.pack(visual);ResourceSaver.save(packed,"res://scenes/architecture/chairs_batched.tscn")
 print("Authored ",chairs.size()," chairs in ",combined.get_surface_count()," surfaces")
 chair.free();seating.free();visual.free();quit()
