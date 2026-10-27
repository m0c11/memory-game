"""Offline Blender model / animation authoring and transparent sprite rendering.
Run blender -b -t 4 --python godot/art/polet_cast/build_cast.py
No runtime asset generation. Source heroine is kept unchanged.
"""
import bpy, math, os
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'assets/polet/characters'; OUT.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'art/heroine/blender/heroine.blend'))
scene=bpy.context.scene
source=bpy.data.collections['HEROINE — export']
originals=list(source.objects)
for c in list(bpy.data.collections):
 if c!=source:
  for obj in list(c.objects):bpy.data.objects.remove(obj,do_unlink=True)
  bpy.data.collections.remove(c)
palette={'skin':(.73,.46,.30,1),'hair':(.105,.042,.020,1),'jacket':(.10,.30,.29,1),'pants':(.075,.12,.18,1),'cream':(.83,.78,.62,1),'shoe':(.16,.11,.07,1),'headband':(.93,.87,.65,1),'eye_white':(.9,.88,.8,1),'iris':(.13,.065,.025,1),'black':(.01,.008,.006,1)}
roles=[('heroine',(.10,.30,.29,1),'long'),('child',(.76,.47,.07,1),'short'),('director',(.30,.19,.40,1),'bun'),('counselor',(.58,.21,.13,1),'tail'),('counselor_male',(.16,.29,.47,1),'short')]
casts=[]
for role,coat,hair in roles:
 coll=bpy.data.collections.new(role);scene.collection.children.link(coll);mapping={}
 for o in originals:
  n=o.copy()
  if o.type=='ARMATURE':n.data=o.data.copy();n.animation_data_clear()
  coll.objects.link(n);mapping[o]=n
 for o,n in mapping.items():
  if o.parent in mapping:n.parent=mapping[o.parent]
  for mod in n.modifiers:
   if mod.type=='ARMATURE':mod.object=mapping.get(mod.object,mod.object)
  if n.type=='MESH':
   for slot in n.material_slots:
    mat=slot.material.copy();slot.link='OBJECT';slot.material=mat
    name=mat.name.split('.')[0]; color=palette.get(name,(.2,.10,.055,1))
    if name=='jacket':color=coat
    if role=='director' and name=='hair':color=(.19,.17,.16,1)
    if role=='counselor' and name=='hair':color=(.24,.08,.025,1)
    mat.diffuse_color=color
    bs=mat.node_tree.nodes.get('Principled BSDF')
    if bs:
     for l in list(mat.node_tree.links):
      if l.to_node==bs and l.to_socket.name=='Base Color':mat.node_tree.links.remove(l)
     bs.inputs['Base Color'].default_value=color
  if o.name=='Ivory fabric headband' and role!='heroine':n.hide_render=True;n.hide_viewport=True
  if hair!='long' and o.name=='Hair sculpt':n.hide_render=True;n.hide_viewport=True
  if role in ['child','counselor_male'] and any(x in o.name for x in ['Eyelash','lip','Lapel','Pocket','Horn button']):n.hide_render=True
 rig=mapping[next(o for o in originals if o.type=='ARMATURE')]
 root=mapping[bpy.data.objects['Heroine']]
 root.name=role+'_root'; rig.name=role+'_rig'
 # Readable head/body ratio in the 64px silhouette, without changing the photo's features.
 for b in rig.pose.bones:b.rotation_mode='XYZ';b.rotation_euler=(0,0,0);b.location=(0,0,0)
 if hair!='long':
  def blob(name,loc,scale,color):
   bpy.ops.mesh.primitive_uv_sphere_add(segments=20,ring_count=12,location=loc)
   ob=bpy.context.object;ob.name=role+'_'+name
   for c in list(ob.users_collection):c.objects.unlink(ob)
   coll.objects.link(ob);ob.scale=scale;ob.parent=root
   mat=bpy.data.materials.new(ob.name);mat.diffuse_color=(*color,1);ob.data.materials.append(mat)
   for poly in ob.data.polygons:poly.use_smooth=True
   # Vertex group follows the same Head bone as the face.
   bpy.context.view_layer.objects.active=ob;ob.select_set(True);bpy.ops.object.transform_apply(location=True,rotation=False,scale=True)
   if name=='hair_cap':
    for v in ob.data.vertices:
     if v.co.y < .01 and v.co.z < 1.535:v.co.z=1.535
   group=ob.vertex_groups.new(name='Head');group.add(list(range(len(ob.data.vertices))),1,'REPLACE')
   mod=ob.modifiers.new('Head binding','ARMATURE');mod.object=rig
  col=(.12,.06,.025) if hair=='short' else ((.21,.08,.03) if hair=='tail' else (.25,.23,.20))
  blob('hair_cap',(0,.018,1.53),(.145,.125,.143),col)
  if hair=='bun':blob('hair_bun',(0,.13,1.53),(.08,.08,.09),col)
  if hair=='tail':blob('ponytail',(0,.11,1.39),(.085,.07,.21),col)
 if role=='child':root.scale*=.78
 if role=='counselor_male':root.scale.x*=1.12
 coll.hide_render=True;casts.append((role,coll,root,rig))
# Remove original meshes only after sharing/copying the model data.
for o in originals:bpy.data.objects.remove(o,do_unlink=True)
bpy.data.collections.remove(source)
bpy.ops.object.camera_add(location=(0,6,4.2));camera=bpy.context.object;camera.name='SpriteCamera';scene.camera=camera
camera.data.type='ORTHO';camera.data.ortho_scale=2.15
camera.rotation_euler=(Vector((0,0,.83))-camera.location).to_track_quat('-Z','Y').to_euler()
scene.render.engine='BLENDER_WORKBENCH'
scene.display.shading.light='STUDIO';scene.display.shading.studio_light='paint.sl'
scene.display.render_aa='OFF'
scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True
scene.display.shading.show_cavity=True;scene.display.shading.cavity_type='BOTH';scene.display.shading.curvature_ridge_factor=1.25;scene.display.shading.curvature_valley_factor=1.0
scene.display.shading.show_object_outline=False
scene.render.resolution_x=64;scene.render.resolution_y=64;scene.render.resolution_percentage=100
scene.render.film_transparent=True;scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
scene.view_settings.view_transform='Standard';scene.render.fps=8
# Five authored actions, compatible with all cast rigs.
def pose(rig,phase,kind):
 for b in rig.pose.bones:b.rotation_euler=(0,0,0);b.location=(0,0,0)
 rig.pose.bones['Head'].scale=(1.15,1.15,1.15)
 wave=math.sin(phase)
 rig.pose.bones['Chest'].rotation_euler.x=.018*wave
 rig.pose.bones['Head'].rotation_euler.z=.025*wave
 if kind=='walk':
  for side,sign in [('L',1),('R',-1)]:
   rig.pose.bones['Thigh.'+side].rotation_euler.x=.40*wave*sign
   rig.pose.bones['Shin.'+side].rotation_euler.x=max(0,-wave*sign)*.55
   rig.pose.bones['UpperArm.'+side].rotation_euler.x=-.28*wave*sign
  rig.pose.bones['Hips'].location.y=.012*abs(wave)
 if kind=='work':
  rig.pose.bones['Chest'].rotation_euler.x=.22+.08*wave
  for side in ['L','R']:
   rig.pose.bones['UpperArm.'+side].rotation_euler.x=-.6
   rig.pose.bones['LowerArm.'+side].rotation_euler.x=-.6+.12*wave
 if kind=='ride':
  rig.pose.bones['Chest'].rotation_euler.x=.2
  for side,sign in [('L',1),('R',-1)]:
   rig.pose.bones['Thigh.'+side].rotation_euler.x=-1.1+.3*wave*sign
   rig.pose.bones['Shin.'+side].rotation_euler.x=1.35+.4*wave*sign
   rig.pose.bones['UpperArm.'+side].rotation_euler.x=-.8
   rig.pose.bones['LowerArm.'+side].rotation_euler.x=-.7
for role,coll,root,rig in casts:
 for kind in ['idle','walk','work','ride']:
  rig.animation_data_clear()
  for f in range(9):
   pose(rig,f/8*math.tau,kind)
   for b in rig.pose.bones:
    b.keyframe_insert('rotation_euler',frame=f+1);b.keyframe_insert('location',frame=f+1)
  rig.animation_data.action.name=role+'_'+kind;rig.animation_data.action.use_fake_user=True
 rig.animation_data_clear();pose(rig,0,'idle')
scene.frame_start=1;scene.frame_end=8
casts[0][1].hide_render=False
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/polet_cast/polet_cast.blend'),compress=True)
# Each file is a real camera render, packed into an atlas by pack_cast.py afterwards.
for role,coll,root,rig in casts:
 for other in casts:other[1].hide_render=True
 coll.hide_render=False
 folder=ROOT/'art/polet_cast/frames'/role;folder.mkdir(parents=True,exist_ok=True)
 for direction,angle in enumerate([0,math.pi/2,math.pi,3*math.pi/2]):
  root.rotation_euler.z=math.pi+angle
  for kind in (['idle','walk','work'] if role=='heroine' else ['idle','walk']):
   for f in range(8 if kind!='idle' else 4):
    pose(rig, f/(4 if kind=='idle' else 8)*math.tau,kind)
    bpy.context.view_layer.update();scene.render.filepath=str(folder/f'{kind}_{direction}_{f}.png');bpy.ops.render.render(write_still=True)
 if role=='heroine':
  root.rotation_euler.z=3*math.pi/2
  for f in range(8):
   pose(rig,f/8*math.tau,'ride');bpy.context.view_layer.update();scene.render.filepath=str(folder/f'ride_0_{f}.png');bpy.ops.render.render(write_still=True)
 print('CAST COMPLETE',role,flush=True)
