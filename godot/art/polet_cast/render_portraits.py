"""Render dialogue busts from the same saved Blender cast, no painted substitutes."""
from pathlib import Path
import bpy,math
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'art/polet_cast/polet_cast.blend'))
scene=bpy.context.scene;camera=scene.camera
scene.render.resolution_x=128;scene.render.resolution_y=128
camera.data.ortho_scale=.72
roles=['heroine','child','director','counselor','counselor_male']
for role in roles:
 for name in roles:bpy.data.collections[name].hide_render=name!=role
 root=bpy.data.objects[role+'_root'];root.rotation_euler.z=math.pi
 height=1.10 if role=='child' else 1.43
 camera.location=(.35,5,height+.6)
 camera.rotation_euler=(Vector((0,0,height))-camera.location).to_track_quat('-Z','Y').to_euler()
 scene.render.filepath=str(ROOT/'assets/polet/characters'/(role+'_portrait.png'))
 bpy.ops.render.render(write_still=True)
