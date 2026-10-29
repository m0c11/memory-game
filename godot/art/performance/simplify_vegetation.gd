extends SceneTree
## Offline mesh LOD selection. Keeps original vertices/UVs/materials and scene paths.
var serial:=0
func _initialize():call_deferred("run")
func simplify(mesh: Mesh) -> ArrayMesh:
 var importer=ImporterMesh.new()
 for i in range(mesh.get_surface_count()):importer.add_surface(Mesh.PRIMITIVE_TRIANGLES,mesh.surface_get_arrays(i),[],{},mesh.surface_get_material(i))
 importer.generate_lods(60.0,60.0,[])
 var result=ArrayMesh.new()
 for i in range(mesh.get_surface_count()):
  var arrays=mesh.surface_get_arrays(i)
  var original=arrays[Mesh.ARRAY_INDEX].size()
  var chosen=arrays[Mesh.ARRAY_INDEX]
  for j in range(importer.get_surface_lod_count(i)):
   var candidate=importer.get_surface_lod_indices(i,j)
   if candidate.size()>=original*.25 and candidate.size()<chosen.size():chosen=candidate
  arrays[Mesh.ARRAY_INDEX]=chosen
  result.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
  result.surface_set_material(i,mesh.surface_get_material(i))
 return result
func run():
 for name in ["grass_clump","wildflowers","bush_small","bush_large","pine_tree","birch_a","birch_b","broadleaf_tree","reeds_cattails"]:
  var src="res://assets/venevitino/foliage/"+name+".res"
  ResourceSaver.save(simplify(load(src)),"res://assets/optimized/"+name+".res")
 for name in ["pine_tree","birch_a","birch_b","broadleaf_tree","bush_large","bush_small","cabin_roof","path_straight_4m"]:
  var scene=load("res://assets/venevitino/models/"+name+".glb").instantiate()
  scene.scene_file_path=""
  for n in scene.find_children("*","Node",true,false):
   n.owner=scene
   if n is MeshInstance3D:
    var mesh=simplify(n.mesh)
    var path="res://assets/optimized/"+name+"_"+str(serial)+".res";serial+=1
    ResourceSaver.save(mesh,path);n.mesh=load(path)
  var packed=PackedScene.new();packed.pack(scene)
  ResourceSaver.save(packed,"res://assets/optimized/"+name+".tscn")
  scene.free()
 print("Saved optimized geometry; no runtime generation.");quit()
