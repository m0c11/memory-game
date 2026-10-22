import bpy,json
from pathlib import Path
from mathutils import Vector
BASE=Path(__file__).resolve().parents[1]
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
bpy.ops.import_scene.gltf(filepath=str(BASE/'models/cabin_wall_front.glb'))
bpy.context.view_layer.update()
cases=[('door_opening',0,1.5,False),('window_opening',1.52,2,False),('solid_wall',2.3,1.5,True),('header_above_door',0,3,True)]
out=[]
for name,x,z,expected in cases:
    hit=False
    for o in bpy.context.scene.objects:
        if o.type!='MESH':continue
        inv=o.matrix_world.inverted();a=inv@Vector((x,-10,z));b=inv@Vector((x,10,z))
        result=o.ray_cast(a,(b-a).normalized(),distance=(b-a).length)
        hit=hit or result[0]
    out.append({'case':name,'ray_hits_wall':hit,'expected':expected,'pass':hit==expected})
report={'passed':all(r['pass'] for r in out),'checks':out}
(BASE/'docs/openings_validation.json').write_text(json.dumps(report,indent=2))
print('OPENINGS',report,flush=True)
if not report['passed']:raise RuntimeError('Opening check failed')
