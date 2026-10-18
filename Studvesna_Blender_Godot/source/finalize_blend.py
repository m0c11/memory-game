"""Make the editable library easy to browse without changing exported meshes."""
from pathlib import Path
import bpy
from mathutils import Vector
root=Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(root/'Studvesna_Assets.blend'))
scene=bpy.context.scene
for o in bpy.data.objects:
    if o.type=='EMPTY' and o.get('asset_id'):
        col=bpy.data.collections.get(o['asset_id'])
        if col:
            col.instance_offset=o.location
            col.asset_mark()
            col.asset_data.description=o.get('title_ru','')+' — textured, metres, separate GLB in models/'
            col.asset_data.tags.new('Studvesna')
            col.asset_data.tags.new('Architecture' if int(o.name[:2])>=18 else 'Prop')
center=Vector((32.5,-17.5,1.8))
cam=scene.camera
cam.location=center+Vector((0,-52,55));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=84
studio=bpy.data.collections.get('STUDIO_render_only')
if studio:studio.hide_render=False
for name,offset,power in [('Key',(-30,-40,60),50000),('Fill',(40,-20,35),24000),('Rim',(10,40,50),40000)]:
    o=bpy.data.objects.get(name)
    if o:
        o.location=center+Vector(offset);o.rotation_euler=(center-o.location).to_track_quat('-Z','Y').to_euler();o.data.energy=power;o.data.size=40
for screen in bpy.data.screens:
    for area in screen.areas:
        if area.type=='VIEW_3D':
            space=area.spaces.active
            space.region_3d.view_location=center
            space.region_3d.view_distance=75
            space.region_3d.view_rotation=cam.rotation_euler.to_quaternion()
            space.clip_end=1000
scene.render.filepath='//Studvesna_Library_PhysicalScale.png'
scene.render.resolution_x=2400;scene.render.resolution_y=1800
bpy.ops.wm.save_as_mainfile(filepath=str(root/'Studvesna_Assets.blend'),compress=True)
print('FINAL_BLEND_READY',flush=True)
