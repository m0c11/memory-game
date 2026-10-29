extends SceneTree
## Offline batching with existing lightmap UVs remapped into their atlas slices.
## Keep the unbatched hall_shell.tscn as the editable source; regenerate after edits.
func _initialize(): call_deferred("run")
func relative(node: Node3D, ancestor: Node) -> Transform3D:
 var result=node.transform
 var parent=node.get_parent()
 while parent!=ancestor:
  if parent is Node3D: result=parent.transform*result
  parent=parent.get_parent()
 return result
func run():
 var hall=load("res://scenes/architecture/hall_shell.tscn").instantiate()
 hall.scene_file_path=""
 var data=load("res://assets/lighting/auditorium.lmbake").duplicate()
 var users=data._get_user_data()
 var baked={}
 var retained=[]
 for i in range(0,users.size(),4):
  var path=str(users[i])
  if path.begins_with("../Architecture/Hall/"): baked[path]=[users[i+1],users[i+2]]
  elif not path.begins_with("../Architecture/Seating/Rows/") and not path.contains("01_auditorium_chair"):
   retained.append_array(users.slice(i,i+4))
 data._set_user_data(retained)
 var groups={}
 var originals=hall.find_children("*","MeshInstance3D",true,false)
 for n in originals:
  var path="../Architecture/Hall/"+str(hall.get_path_to(n))
  var light=baked.get(path,[Rect2(0,0,1,1),-1])
  var key="%d_%d_%d"%[light[1],n.layers,n.cast_shadow]
  if not groups.has(key):groups[key]={"slice":light[1],"layers":n.layers,"shadow":n.cast_shadow,"materials":{}}
  var mats=groups[key].materials
  for i in range(n.mesh.get_surface_count()):
   var mat=n.get_active_material(i)
   if not mats.has(mat):mats[mat]=[]
   var arrays=n.mesh.surface_get_arrays(i)
   if light[1]>=0 and arrays[Mesh.ARRAY_TEX_UV2]!=null:
    var uv=arrays[Mesh.ARRAY_TEX_UV2].duplicate()
    for j in range(uv.size()):uv[j]=uv[j]*light[0].size+light[0].position
    arrays[Mesh.ARRAY_TEX_UV2]=uv
   var mesh=ArrayMesh.new();mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
   mats[mat].append([mesh,relative(n,hall)])
 for n in originals:n.free()
 var serial=0
 for key in groups:
  var group=groups[key];var mesh=ArrayMesh.new()
  for mat in group.materials:
   var st=SurfaceTool.new();st.begin(Mesh.PRIMITIVE_TRIANGLES);st.set_material(mat)
   for part in group.materials[mat]:st.append_from(part[0],0,part[1])
   st.commit(mesh)
  var path="res://assets/optimized/hall_batch_%d.res"%serial
  ResourceSaver.save(mesh,path)
  var node=MeshInstance3D.new();node.name="Batch%d"%serial;serial+=1
  node.mesh=load(path);node.layers=group.layers;node.cast_shadow=group.shadow
  hall.add_child(node)
  if group.slice>=0:data.add_user(NodePath("../Architecture/Hall/"+node.name),Rect2(0,0,1,1),group.slice,-1)
 for n in hall.find_children("*","Node",true,false):
  n.scene_file_path=""
  n.owner=hall
 var packed=PackedScene.new();packed.pack(hall)
 ResourceSaver.save(packed,"res://scenes/architecture/hall_shell_batched.tscn")
 ResourceSaver.save(data,"res://assets/lighting/auditorium_optimized.lmbake")
 print("Hall: ",originals.size()," meshes -> ",serial," atlas batches; lighting preserved")
 hall.free();quit()
