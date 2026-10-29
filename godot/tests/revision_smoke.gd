extends SceneTree
## Migration of partial progress and structural rendering budgets after the playtest.
var checks := 0
var failures := 0
func check(value: bool, label: String) -> void:
 checks += 1
 if not value: failures += 1; push_error(label)
func _initialize(): call_deferred("run")
func run():
 var partial={"version":2,"finished":["linen","rubbish","mop"],"routing":[0,2,1,0,1,0,0,1,0,2,1,1]}
 var saved=PoletSave.sanitize(JSON.parse_string(JSON.stringify(partial)))
 check(saved.finished.size()==3,"New saves preserve incomplete building")
 check(saved.routing[1]==2,"Partial routing survives JSON roundtrip")
 var old=partial.duplicate(true);old.version=1
 check(PoletSave.sanitize(old).finished.size()==9,"Old completed room migrates to completed building")
 var flags: Array=PoletSave.CLEANING.duplicate();flags.append_array(["child","radio","garland_box","garland_left"])
 partial.finished=flags
 saved=PoletSave.sanitize(partial)
 check(saved.finished.has("garland_left") and not saved.finished.has("garland"),"Partial mounted garland preserved")
 flags=PoletSave.TASKS.slice(0,PoletSave.TASKS.find("bicycle"))
 partial.finished=flags
 saved=PoletSave.sanitize(partial)
 check(saved.finished.has("ride_shop") and not saved.finished.has("bicycle"),"Shop checkpoint doesn't award return trip")
 # All seat transforms must be explicitly serialized; Dummy rendering cannot roundtrip GPU buffers.
 var text=FileAccess.get_file_as_string("res://scenes/architecture/chairs_batched.tscn")
 var serialized=text.get_slice("buffer = PackedFloat32Array(",1).get_slice(")",0).split(",")
 check(serialized.size()==107*12,"107 visible seats have all 12 transform components saved")
 var identities=0
 for i in range(0,serialized.size(),12):
  if float(serialized[i])==1.0 and float(serialized[i+5])==1.0 and float(serialized[i+10])==1.0:identities+=1
 check(identities==107,"Seat transforms have nonzero scale")
 for path in ["res://scenes/levels/auditorium.tscn","res://scenes/levels/venevitino.tscn"]:
  var scene=load(path).instantiate()
  var surfaces=0;var triangles=0;var shadows=0
  for node in scene.find_children("*","Node",true,false):
   if node is Light3D and node.shadow_enabled:shadows+=1
   var mesh: Mesh
   var count=1
   if node is MeshInstance3D:mesh=node.mesh
   elif node is MultiMeshInstance3D:mesh=node.multimesh.mesh;count=node.multimesh.instance_count
   if mesh:
    surfaces+=mesh.get_surface_count()
    for i in range(mesh.get_surface_count()):
     var arrays=mesh.surface_get_arrays(i)
     triangles+=(arrays[Mesh.ARRAY_INDEX].size() if arrays[Mesh.ARRAY_INDEX]!=null else arrays[Mesh.ARRAY_VERTEX].size())/3*count
  if path.contains("auditorium"):
   check(surfaces<1800,"Hall draw surface budget")
   check(scene.get_node("Passages/LeftEntrance").position.z< -4,"Backstage entrance is behind curtain")
   check(scene.get_node("Architecture/Seating/Chairs").multimesh.instance_count==107,"Seat count unchanged")
   var data=scene.get_node("BakedLighting").light_data
   for i in range(data.get_user_count()):check(scene.get_node("BakedLighting").has_node(data.get_user_path(i)),"Every baked mesh exists")
  else:
   check(triangles<15000000,"Venevitino authored geometry budget")
   check(shadows==1,"Only player flashlight casts dynamic shadows")
   check(scene.get_node("Player/Head/FirstPersonCamera/Flashlight").shadow_enabled,"Flashlight shadows retained")
  print(path," surfaces=",surfaces," triangles=",triangles," shadows=",shadows)
  scene.free()
 print("REVISION: ",checks," checks, ",failures," failures")
 quit(0 if failures==0 else 1)
