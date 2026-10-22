import bpy, json, math, sys
from pathlib import Path
from mathutils import Vector
BASE=Path(__file__).resolve().parents[1]
OUT=BASE/'renders';OUT.mkdir(exist_ok=True)
(OUT/'catalog').mkdir(exist_ok=True)
scene=bpy.data.scenes.new('STUDIO_RENDER')
bpy.context.window.scene=scene
scene.render.engine='CYCLES';scene.cycles.device='CPU'
scene.cycles.use_denoising=True;scene.cycles.max_bounces=5
scene.cycles.transparent_max_bounces=8
scene.render.threads_mode='FIXED';scene.render.threads=6
scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
scene.render.resolution_percentage=100
scene.view_settings.view_transform='AgX'
scene.view_settings.look='AgX - Medium High Contrast'
scene.view_settings.exposure=0
world=bpy.data.worlds.new('Studio_world');world.use_nodes=True
world.node_tree.nodes['Background'].inputs[0].default_value=(.72,.76,.80,1)
world.node_tree.nodes['Background'].inputs[1].default_value=.35;scene.world=world
studio=bpy.data.collections.new('Studio');scene.collection.children.link(studio)
camdata=bpy.data.cameras.new('Review_camera');cam=bpy.data.objects.new('Review_camera',camdata);studio.objects.link(cam)
camdata.type='ORTHO';camdata.clip_end=2000;scene.camera=cam
me=bpy.data.meshes.new('Ground');me.from_pydata([(-150,-150,0),(150,-150,0),(150,150,0),(-150,150,0)],[],[(0,1,2,3)])
floor=bpy.data.objects.new('Studio_ground',me);studio.objects.link(floor)
mat=bpy.data.materials.new('Studio_ground');mat.use_nodes=True
mat.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(.64,.61,.55,1)
mat.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value=.86;me.materials.append(mat)
lights=[]
for name,offset,power,size in [('Key',(.6,-.8,1.5),25,1),('Fill',(-.9,-.2,.8),10,1),('Rim',(.4,.9,1.3),16,.8)]:
    data=bpy.data.lights.new(name,'AREA');data.shape='DISK'
    obj=bpy.data.objects.new(name,data);studio.objects.link(obj)
    lights.append((obj,Vector(offset),power,size))

def hidden_tree(obj):
    obj.hide_render=True
    for child in obj.children: hidden_tree(child)

def render(key,path,width=640,height=480,samples=10,view=(9,-12,8),cutaway=False,force=False):
    if not force and path.exists() and path.stat().st_size>1000:
        print('SKIP',path.name,flush=True);return
    c=bpy.data.collections[key];scene.collection.children.link(c)
    saved={o:o.hide_render for o in c.all_objects}
    if cutaway:
        for o in list(c.all_objects):
            if o.name.startswith(('cabin_roof','cabin_ceiling','cabin_wall_front','cabin_wall_right','cabin_gutters')):hidden_tree(o)
            if o.type=='EMPTY' and o.name.startswith(('window_120','door_hinged','wall_lamp')) and (o.location.y < -3.2 or o.location.x>2.3):hidden_tree(o)
    bpy.context.view_layer.update()
    points=[o.matrix_world@Vector(v) for o in c.all_objects if o.type=='MESH' and not o.hide_render for v in o.bound_box]
    lo=Vector([min(p[i] for p in points) for i in range(3)]);hi=Vector([max(p[i] for p in points) for i in range(3)])
    center=(lo+hi)*.5;extent=max(hi-lo);extent=max(extent,.3)
    scene.render.resolution_x=width;scene.render.resolution_y=height
    cam.location=center+Vector(view).normalized()*extent*4
    cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler()
    bpy.context.view_layer.update()
    local=[cam.matrix_world.inverted()@p for p in points]
    ww=max(p.x for p in local)-min(p.x for p in local);hh=max(p.y for p in local)-min(p.y for p in local)
    camdata.ortho_scale=1
    frame=camdata.view_frame(scene=scene)
    fw=max(p.x for p in frame)-min(p.x for p in frame);fh=max(p.y for p in frame)-min(p.y for p in frame)
    camdata.ortho_scale=max(ww/fw,hh/fh)*1.14
    cam.location+=cam.rotation_euler.to_matrix()@Vector((0,-camdata.ortho_scale*.015,0))
    floor.location.z=lo.z-.018
    for obj,offset,power,size in lights:
        obj.location=center+offset*extent
        obj.rotation_euler=(center-obj.location).to_track_quat('-Z','Y').to_euler()
        obj.data.energy=power*extent*extent;obj.data.size=size*extent
    scene.cycles.samples=samples;scene.render.filepath=str(path)
    print('RENDER',key,path.name,flush=True)
    bpy.ops.render.render(write_still=True)
    scene.collection.children.unlink(c)
    for o,h in saved.items():o.hide_render=h

mode=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
if 'gables' in mode:
    render('cabin_furnished',OUT/'cabin_exterior.png',1600,1250,32,(10,-14,9),force=True)
    render('cabin_shell',OUT/'cabin_front.png',1100,850,20,(0,-1,0),force=True)
    render('cabin_shell',OUT/'cabin_back.png',1100,850,20,(0,1,0),force=True)
    for key in ('cabin_roof','cabin_shell','cabin_furnished'):
        render(key,OUT/'catalog'/f'{key}.png',640,480,12,force=True)
if not mode or 'heroes' in mode:
    render('cabin_furnished',OUT/'cabin_exterior.png',1600,1250,32,(10,-14,9))
    render('cabin_furnished',OUT/'cabin_interior.png',1600,1250,32,(11,-13,17),True)
    render('cabin_shell',OUT/'cabin_front.png',1100,850,20,(0,-1,0))
    render('cabin_shell',OUT/'cabin_side.png',1300,850,20,(1,0,0))
    render('cabin_shell',OUT/'cabin_back.png',1100,850,20,(0,1,0))
    render('cabin_shell',OUT/'cabin_top.png',1100,1000,20,(0,0,1))
if not mode or 'catalog' in mode or 'nature' in mode:
    manifest=json.loads((BASE/'docs/asset_manifest.json').read_text())
    for item in manifest:
        if 'nature' in mode and item['category'] not in ('nature','terrain'):continue
        key=Path(item['file']).stem
        view=(9,-12,8)
        if key.startswith('cabin_wall'):view=(8,-12,7) if key!='cabin_wall_back' else (8,12,7)
        if key=='cabin_wall_left':view=(-12,-8,7)
        if key in ('volleyball_court','river_water_12m','ground_tile_8m','path_curve_90','river_bank_6m'):view=(8,-10,15)
        if key in ('birch_a','birch_b','pine_tree','broadleaf_tree'):view=(10,-14,6)
        render(key,OUT/'catalog'/f'{key}.png',640,480,12,view,force='nature' in mode)
print('RENDERS_COMPLETE',flush=True)
