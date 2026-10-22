import bpy,sys,json,time
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from common import *
import architecture,furniture,nature,infrastructure,characters
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
for c in list(bpy.data.collections):
 if c.name=='Collection':bpy.data.collections.remove(c)
bpy.context.scene.name='ASSET_LIBRARY';bpy.context.scene.unit_settings.system='METRIC';bpy.context.scene.unit_settings.scale_length=1;started=time.time()
for mod in [architecture,furniture,nature,infrastructure,characters]:print('BUILD',mod.__name__,flush=True);mod.build()
for im in bpy.data.images:
 if im.source=='FILE':im.pack()
bpy.context.view_layer.update();manifest=[]
for key,a in ASSETS.items():
 obs=list(a['collection'].objects);lo,hi=bounds(obs);bpy.ops.object.select_all(action='DESELECT')
 for o in obs:o.select_set(True)
 bpy.context.view_layer.objects.active=a['root'];print('EXPORT',key,flush=True)
 bpy.ops.export_scene.gltf(filepath=str(BASE/'models'/f'{key}.glb'),export_format='GLB',use_selection=True,export_apply=True,export_yup=True,export_texcoords=True,export_normals=True,export_materials='EXPORT',export_cameras=False,export_lights=False,export_animations=False,export_extras=True)
 tris=0
 for o in obs:
  if o.type=='MESH':o.data.calc_loop_triangles();tris+=len(o.data.loop_triangles)
 manifest.append({'file':f'models/{key}.glb','name_ru':a['label'],'category':a['category'],'size_m':[round(hi[i]-lo[i],3) for i in range(3)],'bounds_blender_min':lo,'bounds_blender_max':hi,'triangles':tris,'objects':len(obs)})
(BASE/'docs/asset_manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
scene=bpy.data.scenes.new('CABIN_REVIEW');scene.unit_settings.system='METRIC';scene.unit_settings.scale_length=1;scene.collection.children.link(ASSETS['cabin_furnished']['collection']);bpy.context.window.scene=scene
scene.world=bpy.data.worlds.new('Neutral_world');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.35
for scr in bpy.data.screens:
 for area in scr.areas:
  if area.type=='VIEW_3D':
   area.spaces.active.region_3d.view_distance=15;area.spaces.active.region_3d.view_location=(0,-.4,1.7);area.spaces.active.shading.type='MATERIAL'
bpy.ops.wm.save_as_mainfile(filepath=str(BASE/'Venevitino_Assets.blend'),compress=True);print('DONE',len(ASSETS),round(time.time()-started),flush=True)
