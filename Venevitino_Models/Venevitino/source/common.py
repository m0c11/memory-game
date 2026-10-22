import bpy,bmesh,math,random,json
from pathlib import Path
from mathutils import Vector,Matrix
from math import sin,cos,pi,sqrt
BASE=Path(__file__).resolve().parents[1];ASSETS={};MATS={};CURRENT=None;ROOT=None
random.seed(1920)
def material(key):
 if key in MATS:return MATS[key]
 m=bpy.data.materials.new(key);m.use_nodes=True;nt=m.node_tree;p=nt.nodes.get('Principled BSDF')
 for suffix in ('basecolor','normal','orm'):
  t=nt.nodes.new('ShaderNodeTexImage');t.image=bpy.data.images.load(str(BASE/'textures'/f'{key}_{suffix}.png'),check_existing=True);t.label=suffix
  if suffix=='basecolor':nt.links.new(t.outputs['Color'],p.inputs['Base Color'])
  elif suffix=='normal':
   t.image.colorspace_settings.name='Non-Color';n=nt.nodes.new('ShaderNodeNormalMap');n.inputs['Strength'].default_value=.3;nt.links.new(t.outputs['Color'],n.inputs['Color']);nt.links.new(n.outputs['Normal'],p.inputs['Normal'])
  else:
   t.image.colorspace_settings.name='Non-Color';s=nt.nodes.new('ShaderNodeSeparateColor');nt.links.new(t.outputs['Color'],s.inputs[0]);nt.links.new(s.outputs['Green'],p.inputs['Roughness']);nt.links.new(s.outputs['Blue'],p.inputs['Metallic'])
 if key in ('glass','bottle'):p.inputs['Alpha'].default_value=.22 if key=='glass' else .72;m.surface_render_method='DITHERED';p.inputs['IOR'].default_value=1.45
 m.use_backface_culling=key not in ('leaf','leaf_light','pine','grass','drygrass','rope','glass','water','roof','linen','blanket_blue','blanket_green');MATS[key]=m;return m
def asset(name,label,category):
 global CURRENT,ROOT
 c=bpy.data.collections.new(name);bpy.context.scene.collection.children.link(c);r=bpy.data.objects.new(name,None);c.objects.link(r);r['asset_name']=label;r['units']='meters';r['category']=category
 ASSETS[name]={'collection':c,'root':r,'label':label,'category':category};CURRENT=c;ROOT=r;return r
def use(name):
 global CURRENT,ROOT
 CURRENT=ASSETS[name]['collection'];ROOT=ASSETS[name]['root']
def empty(name,loc=(0,0,0),parent=None):
 o=bpy.data.objects.new(name,None);CURRENT.objects.link(o);o.parent=parent or ROOT;o.location=loc;return o
def uv_project(me):
 uv=me.uv_layers.new(name='UVMap')
 for p in me.polygons:
  vs=[me.vertices[i].co for i in p.vertices];spans=[max(v[a] for v in vs)-min(v[a] for v in vs) for a in range(3)];axes=sorted(range(3),key=lambda a:spans[a],reverse=True)[:2]
  for li in p.loop_indices:
   v=me.vertices[me.loops[li].vertex_index].co;uv.data[li].uv=(v[axes[0]],v[axes[1]])
def mesh(name,vs,fs,mat='timber',parent=None,uv=True,smooth=False):
 me=bpy.data.meshes.new(name);me.from_pydata(vs,[],fs);me.update()
 if uv:uv_project(me)
 o=bpy.data.objects.new(name,me);CURRENT.objects.link(o);o.parent=parent or ROOT
 for key in ([mat] if isinstance(mat,str) else mat or []):me.materials.append(material(key))
 for p in me.polygons:p.use_smooth=smooth
 return o
def box(name,size,loc=(0,0,0),mat='timber',bevel=.012,rot=None,parent=None):
 x,y,z=[s/2 for s in size];o=mesh(name,[(-x,-y,-z),(x,-y,-z),(x,y,-z),(-x,y,-z),(-x,-y,z),(x,-y,z),(x,y,z),(-x,y,z)],[(3,2,1,0),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7)],mat,parent);o.location=loc
 if rot:o.rotation_euler=rot
 if bevel:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.bevel(bm,geom=list(bm.edges),offset=min(bevel,min(size)*.24),segments=2 if bevel>=.025 else 1,affect='EDGES',profile=.5);bm.to_mesh(o.data);bm.free();o.data.update()
  for l in list(o.data.uv_layers):o.data.uv_layers.remove(l)
  uv_project(o.data)
 return o
def lathe(name,profile,mat='metal',segments=24,loc=(0,0,0),parent=None):
 vs=[];fs=[]
 for r,z in profile:vs.extend([(r*cos(i*2*pi/segments),r*sin(i*2*pi/segments),z) for i in range(segments)])
 for j in range(len(profile)-1):
  for i in range(segments):a=j*segments+i;b=j*segments+(i+1)%segments;fs.append((a,b,b+segments,a+segments))
 fs.append(tuple(reversed(range(segments))));fs.append(tuple((len(profile)-1)*segments+i for i in range(segments)))
 o=mesh(name,vs,fs,mat,parent,smooth=True);o.location=loc;uv=o.data.uv_layers.active
 for p in o.data.polygons:
  for li in p.loop_indices:idx=o.data.loops[li].vertex_index;uv.data[li].uv=((idx%segments)/segments,o.data.vertices[idx].co.z)
 return o
def cyl(name,radius,depth,loc=(0,0,0),mat='metal',vertices=16,parent=None):return lathe(name,[(radius,0),(radius,depth)],mat,vertices,loc,parent)
def rod(name,a,b,radius=.025,mat='timber',vertices=12,parent=None):
 a=Vector(a);b=Vector(b);o=cyl(name,radius,(b-a).length,a,mat,vertices,parent);o.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler();return o
def beam(name,a,b,width=.12,depth=None,mat='timber',parent=None):
 a=Vector(a);b=Vector(b);o=box(name,(width,depth or width,(b-a).length),(a+b)/2,mat,.01,parent=parent);o.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler();return o
def sphere(name,size,loc=(0,0,0),mat='linen',seg=20,rings=12,parent=None):
 vs=[];fs=[]
 for j in range(rings+1):
  v=pi*j/rings
  for i in range(seg):u=2*pi*i/seg;vs.append((size[0]*.5*sin(v)*cos(u),size[1]*.5*sin(v)*sin(u),size[2]*.5*cos(v)))
 for j in range(rings):
  for i in range(seg):a=j*seg+i;b=j*seg+(i+1)%seg;fs.append((a,b,b+seg,a+seg))
 o=mesh(name,vs,fs,mat,parent,smooth=True);o.location=loc;return o
def tube(name,points,radius=.015,mat='metal',parent=None,res=2):
 vs=[];fs=[];n=8 if res==1 else 12
 for j,p in enumerate(points):
  p=Vector(p);t=Vector(points[min(j+1,len(points)-1)])-Vector(points[max(j-1,0)]);q=t.to_track_quat('Z','Y')
  for i in range(n):vs.append(tuple(p+q@Vector((radius*cos(i*2*pi/n),radius*sin(i*2*pi/n),0))))
 for j in range(len(points)-1):
  for i in range(n):a=j*n+i;b=j*n+(i+1)%n;fs.append((a,b,b+n,a+n))
 return mesh(name,vs,fs,mat,parent,smooth=True)
def world_matrix(o):
 m=Matrix.LocRotScale(o.location,o.rotation_euler.to_quaternion(),o.scale);return world_matrix(o.parent)@m if o.parent else m
def solidify_z(o,t):
 old=o.data;vs=[tuple(v.co) for v in old.vertices];n=len(vs);fs=[tuple(p.vertices) for p in old.polygons];edges={}
 for f in fs:
  for a,b in zip(f,f[1:]+f[:1]):k=tuple(sorted((a,b)));edges[k]=None if k in edges else (a,b)
 vs +=[(x,y,z-t) for x,y,z in vs];fs +=[tuple(i+n for i in reversed(f)) for f in list(fs)]
 for p in edges.values():
  if p:a,b=p;fs.append((b,a,a+n,b+n))
 me=bpy.data.meshes.new(old.name+'_solid');me.from_pydata(vs,[],fs);me.update()
 for m in old.materials:me.materials.append(m)
 uv_project(me);o.data=me
def merge(name,obs,parent=None):
 obs=[o for o in obs if o.type=='MESH']
 if not obs:return
 vs=[];fs=[];uvs=[];mats=[];mi=[];sm=[]
 for o in obs:
  me=o.data;off=len(vs);tr=world_matrix(parent or ROOT).inverted()@world_matrix(o);vs.extend([tuple(tr@v.co) for v in me.vertices]);slots=[]
  for m in me.materials:
   if m not in mats:mats.append(m)
   slots.append(mats.index(m))
  layer=me.uv_layers.active
  for p in me.polygons:
   fs.append(tuple(off+i for i in p.vertices));mi.append(slots[p.material_index]);sm.append(p.use_smooth);uvs.extend([tuple(layer.data[li].uv) for li in p.loop_indices])
 o=mesh(name,vs,fs,None,parent,uv=False)
 for m in mats:o.data.materials.append(m)
 layer=o.data.uv_layers.new(name='UVMap')
 for d,u in zip(layer.data,uvs):d.uv=u
 for p,m,s in zip(o.data.polygons,mi,sm):p.material_index=m;p.use_smooth=s
 for old in obs:bpy.data.objects.remove(old,do_unlink=True)
 return o
def consolidate(name=None):return merge(name or ROOT.name+'_Mesh',[o for o in CURRENT.objects if o.type=='MESH' and o.parent==ROOT])
def clone(source,loc=(0,0,0),rot=0,parent=None,name=None):
 src=ASSETS[source];mp={}
 for o in src['collection'].objects:n=o.copy();CURRENT.objects.link(n);mp[o]=n
 for o,n in mp.items():n.parent=mp.get(o.parent,parent or ROOT)
 r=mp[src['root']];r.name=name or source;r.location=loc;r.rotation_euler[2]=rot;return r
def bounds(obs):
 pts=[world_matrix(o)@Vector(v) for o in obs if o.type=='MESH' for v in o.bound_box]
 return [min(p[i] for p in pts) for i in range(3)],[max(p[i] for p in pts) for i in range(3)]
