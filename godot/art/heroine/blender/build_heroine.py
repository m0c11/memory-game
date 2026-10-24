"""Offline Blender authoring. Generates saved meshes/assets, never runs in the game.
Run: blender -b -t 4 --python build_heroine.py
Input textures: user-supplied heroine_beautiful_godot.zip.
"""
import bpy, math, json, os
from pathlib import Path
from mathutils import Vector
from math import sin, cos, pi, exp, sqrt
ROOT=Path(__file__).resolve().parents[1]
PROJECT=ROOT.parents[1]
OUT=PROJECT/'assets/characters/heroine'
TEX=ROOT/'textures'
PREVIEW=ROOT/'previews'
for p in [OUT,PREVIEW]:p.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
scene=bpy.context.scene
scene.unit_settings.system='METRIC'
scene.unit_settings.scale_length=1
hero=bpy.data.collections.new('HEROINE — export');scene.collection.children.link(hero)
studio=bpy.data.collections.new('STUDIO — preview only');scene.collection.children.link(studio)
objects=[]
def move_coll(obj,coll=hero):
 for c in list(obj.users_collection):c.objects.unlink(obj)
 coll.objects.link(obj)
def active(o):
 bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o

def mat(name,base=None,rough=.6,metal=0,normal=.12):
 m=bpy.data.materials.new(name);m.use_nodes=True
 n=m.node_tree.nodes;l=m.node_tree.links;p=n.get('Principled BSDF')
 p.inputs['Roughness'].default_value=rough;p.inputs['Metallic'].default_value=metal
 if base:
  p.inputs['Base Color'].default_value=(*base,1)
 else:
  for suffix in ['basecolor','normal','metallicRoughness']:
   f=TEX/(name+'_'+suffix+'.png')
   if not f.exists():continue
   t=n.new('ShaderNodeTexImage');t.image=bpy.data.images.load(str(f),check_existing=True);t.label=suffix
   if suffix=='basecolor':l.new(t.outputs['Color'],p.inputs['Base Color'])
   elif suffix=='normal':
    t.image.colorspace_settings.name='Non-Color';nm=n.new('ShaderNodeNormalMap');nm.inputs['Strength'].default_value=normal;l.new(t.outputs['Color'],nm.inputs['Color']);l.new(nm.outputs['Normal'],p.inputs['Normal'])
   else:
    t.image.colorspace_settings.name='Non-Color';sep=n.new('ShaderNodeSeparateColor');l.new(t.outputs['Color'],sep.inputs['Color']);l.new(sep.outputs['Green'],p.inputs['Roughness']);l.new(sep.outputs['Blue'],p.inputs['Metallic'])
 return m
mats={n:mat(n,normal=.09 if n in ['jacket','pants','cream'] else .06) for n in ['skin','hair','jacket','pants','cream','shoe','headband','eye_white','iris','black','mouth','metal_dark','metal_ring','lens']}
mats['seam']=mat('Tonal stitching',(.16,.10,.065),.85)
mats['lip']=mat('Soft rose lips',(.47,.18,.14),.53)
mats['lash']=mat('Soft brown lashes',(.035,.016,.009),.7)
mats['iris_dark']=mat('Iris limbal ring',(.055,.021,.009),.38)
mats['iris_gold']=mat('Iris warm ring',(.24,.105,.028),.34)
mats['hair_light']=mat('Chestnut strand highlight',(.13,.055,.025),.52)
mats['ear_inner']=mat('Ear warm interior',(.60,.29,.20),.7)
# Vertex colour tint multiplies original supplied skin texture: gentle cheeks, not floating blush discs.
skin=mats['skin'];n=skin.node_tree.nodes;l=skin.node_tree.links;p=n.get('Principled BSDF')
tex=next(x for x in n if x.type=='TEX_IMAGE' and 'basecolor' in x.image.name)
vcol=n.new('ShaderNodeVertexColor');vcol.layer_name='Complexion'
mix=n.new('ShaderNodeMixRGB');mix.blend_type='MULTIPLY';mix.inputs[0].default_value=1
l.new(tex.outputs['Color'],mix.inputs[1]);l.new(vcol.outputs['Color'],mix.inputs[2]);l.new(mix.outputs[0],p.inputs['Base Color'])
p.inputs['Subsurface Weight'].default_value=.035

def mesh(name,verts,faces,material,uv=None):
 me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update()
 o=bpy.data.objects.new(name,me);hero.objects.link(o);objects.append(o)
 o.data.materials.append(mats[material] if isinstance(material,str) else material)
 for p in me.polygons:p.use_smooth=True
 if uv:
  layer=me.uv_layers.new(name='UVMap')
  for poly in me.polygons:
   for li in poly.loop_indices:layer.data[li].uv=uv[me.loops[li].vertex_index]
 return o

def finish(o,sub=0):
 active(o)
 if sub:
  mod=o.modifiers.new('Surface refinement','SUBSURF');mod.levels=sub;bpy.ops.object.modifier_apply(modifier=mod.name)
 bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT');bpy.ops.mesh.normals_make_consistent(inside=False);bpy.ops.object.mode_set(mode='OBJECT')
 return o

def ellipsoid(name,loc,scale,material,seg=32,rings=20):
 bpy.ops.mesh.primitive_uv_sphere_add(segments=seg,ring_count=rings,location=loc)
 o=bpy.context.object;o.name=name;o.scale=scale;move_coll(o);objects.append(o)
 bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
 o.data.materials.append(mats[material]);
 for p in o.data.polygons:p.use_smooth=True
 return o

def interpolate(seq,t):
 # Catmull-Rom smooth profile; endpoints repeated.
 f=t*(len(seq)-1);i=min(int(f),len(seq)-2);u=f-i
 a=Vector(seq[max(i-1,0)]);b=Vector(seq[i]);c=Vector(seq[i+1]);d=Vector(seq[min(i+2,len(seq)-1)])
 return .5*((2*b)+(-a+c)*u+(2*a-5*b+4*c-d)*u*u+(-a+3*b-3*c+d)*u*u*u)

def sweep(name,points,widths,depths,material,steps=36,sides=16):
 vs=[];uv=[]
 for i in range(steps+1):
  t=i/steps;pos=interpolate(points,t);tangent=interpolate(points,min(1,t+.001))-interpolate(points,max(0,t-.001));tangent.normalize()
  a=tangent.cross(Vector((0,1,0))).normalized();b=tangent.cross(a).normalized()
  w=interpolate([(x,0,0) for x in widths],t).x;d=interpolate([(x,0,0) for x in depths],t).x
  for j in range(sides+1):
   angle=2*pi*j/sides;vs.append(pos+a*(w*cos(angle))+b*(d*sin(angle)));uv.append((j/sides,t))
 faces=[]
 for i in range(steps):
  for j in range(sides):
   a=i*(sides+1)+j;faces.append((a,a+1,a+sides+2,a+sides+1))
 faces.extend([tuple(range(sides,-1,-1)),tuple(steps*(sides+1)+j for j in range(sides+1))])
 return finish(mesh(name,vs,faces,material,uv))

def curve(name,points,radius,material):
 cu=bpy.data.curves.new(name,'CURVE');cu.dimensions='3D';cu.resolution_u=3;cu.bevel_depth=radius;cu.bevel_resolution=1
 sp=cu.splines.new('BEZIER');sp.bezier_points.add(len(points)-1)
 for p,co in zip(sp.bezier_points,points):p.co=co;p.handle_left_type='AUTO';p.handle_right_type='AUTO'
 o=bpy.data.objects.new(name,cu);hero.objects.link(o);o.data.materials.append(mats[material]);active(o);bpy.ops.object.convert(target='MESH');objects.append(o);return o

def join_remesh(name,parts,voxel=.003,smooth=4,ratio=.45):
 active(parts[0])
 for p in parts:p.select_set(True)
 bpy.ops.object.join();o=bpy.context.object;o.name=name
 r=o.modifiers.new('Weld garment surface','REMESH');r.mode='VOXEL';r.voxel_size=voxel;r.use_smooth_shade=True;bpy.ops.object.modifier_apply(modifier=r.name)
 s=o.modifiers.new('Relax joins','SMOOTH');s.factor=.75;s.iterations=smooth;bpy.ops.object.modifier_apply(modifier=s.name)
 d=o.modifiers.new('Game mesh budget','DECIMATE');d.ratio=ratio;bpy.ops.object.modifier_apply(modifier=d.name)
 active(o);bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT');bpy.ops.uv.smart_project(angle_limit=1.15,island_margin=.018);bpy.ops.object.mode_set(mode='OBJECT')
 for p in o.data.polygons:p.use_smooth=True
 return finish(o)

def torso(name,levels,material,opening=False):
 vs=[];uv=[];steps=32;cols=64
 for i in range(steps+1):
  z,rx,ry,gap=interpolate(levels,i/steps)
  for j in range(cols+1):
   a=gap+(2*pi-2*gap)*j/cols if opening else 2*pi*j/cols
   x=rx*sin(a);y=-ry*cos(a)
   # Slight fabric curvature, not a cylinder.
   y-=.003*cos(6*a)*(sin(pi*i/steps)**2)
   vs.append((x,y,z));uv.append((j/cols,i/steps))
 faces=[]
 for i in range(steps):
  for j in range(cols):
   a=i*(cols+1)+j;faces.append((a,a+1,a+cols+2,a+cols+1))
 o=mesh(name,vs,faces,material,uv)
 mod=o.modifiers.new('Fabric thickness','SOLIDIFY');mod.thickness=.008;mod.offset=-1;active(o);bpy.ops.object.modifier_apply(modifier=mod.name)
 return finish(o)

# Continuous tailored jacket; the open front is actual geometry.
jacket=torso('Blazer — continuous shell',[(.84,.158,.096,.13),(.88,.155,.095,.15),(.99,.123,.079,.12),(1.105,.14,.087,.23),(1.22,.169,.09,.48),(1.28,.177,.079,.65),(1.315,.13,.064,.59),(1.335,.068,.049,.48)],'jacket',True)
arm_points={}
sleeves=[]
for side,s in [('L',1),('R',-1)]:
 pts=[(s*.153,0,1.282),(s*.19,0,1.265),(s*.225,-.003,1.17),(s*.25,-.013,1.08),(s*.276,-.02,.99),(s*.298,-.025,.905)]
 arm_points[side]=pts
 sleeves.append(sweep('Sleeve.'+side,pts,[.067,.063,.054,.046,.043,.047],[.065,.059,.052,.044,.04,.041],'jacket',38,24))
jacket=join_remesh('Blazer', [jacket]+sleeves,.003,5,.34)
# Turtleneck fitted under real blazer opening.
torso('Knit sweater',[(.855,.126,.073,0),(.98,.117,.074,0),(1.11,.132,.081,0),(1.235,.143,.08,0),(1.29,.12,.065,0),(1.33,.055,.043,0)],'cream')
torso('Ribbed roll collar',[(1.313,.058,.045,0),(1.324,.06,.047,0),(1.36,.057,.043,0),(1.369,.052,.04,0)],'cream')
# Lapels lie on the chest, no projecting billboard panels.
for side,s in [('L',1),('R',-1)]:
 poly=[(s*.058,-.049,1.329),(s*.126,-.067,1.291),(s*.112,-.086,1.246),(s*.141,-.076,1.232),(s*.047,-.093,1.108),(s*.073,-.092,1.246)]
 o=mesh('Notched lapel.'+side,poly,[tuple(range(6))],'jacket',[(p[0]*4,p[2]*4) for p in poly]);active(o)
 mod=o.modifiers.new('Lapel thickness','SOLIDIFY');mod.thickness=.004;bpy.ops.object.modifier_apply(modifier=mod.name)
 mod=o.modifiers.new('Soft tailored edge','BEVEL');mod.width=.002;mod.segments=3;bpy.ops.object.modifier_apply(modifier=mod.name);finish(o)
 curve('Lapel seam.'+side,[(x,y-.0015,z) for x,y,z in poly[:5]],.00065,'seam')
 # Patch pocket fitted to each front side.
 pts=[(s*.064,-.084,.938),(s*.126,-.059,.938),(s*.132,-.065,.876),(s*.068,-.091,.876)]
 o=mesh('Pocket.'+side,pts,[(0,1,2,3)],'jacket',[(0,1),(1,1),(1,0),(0,0)]);active(o)
 mod=o.modifiers.new('Pocket thickness','SOLIDIFY');mod.thickness=.003;bpy.ops.object.modifier_apply(modifier=mod.name);finish(o)
 curve('Pocket edge.'+side,[(x,y-.002,z) for x,y,z in pts+[pts[0]]],.0006,'seam')
 # Continuous sleeve cuff piping.
 wrist=Vector(arm_points[side][-1]);tangent=(wrist-Vector(arm_points[side][-2])).normalized();a=tangent.cross(Vector((0,1,0))).normalized();b=tangent.cross(a).normalized()
 curve('Cuff seam.'+side,[wrist+a*.046*cos(2*pi*j/32)+b*.040*sin(2*pi*j/32)-tangent*.009 for j in range(33)],.0007,'seam')
for z in [1.086,1.015,.951]:
 ellipsoid('Horn button',(.024,-.090,z),(.0065,.003,.0065),'shoe',16,10)
# Joined trousers: pelvis and legs are one surface with a natural crotch.
pelvis=ellipsoid('Trouser seat',(0,.007,.84),(.139,.069,.115),'pants')
legs=[]
for side,s in [('L',1),('R',-1)]:
 legs.append(sweep('Trouser leg.'+side,[(s*.076,.005,.875),(s*.084,.002,.775),(s*.089,0,.62),(s*.091,-.012,.48),(s*.094,0,.32),(s*.095,.003,.12)],[.075,.073,.062,.053,.052,.055],[.075,.07,.062,.05,.048,.053],'pants',40,24))
trousers=join_remesh('Trousers',[pelvis]+legs,.003,5,.35)
for side,s in [('L',1),('R',-1)]:
 curve('Trouser pressed crease.'+side,[(s*.094,-.05,.15),(s*.09,-.054,.35),(s*.09,-.063,.63),(s*.085,-.066,.81)],.00055,'pants')
 # Loafers with sculpted instep rather than floating spheres.
 levels=[(.014,.042,.087,-.032),(.027,.045,.089,-.032),(.042,.043,.088,-.032),(.062,.042,.084,-.032),(.08,.039,.073,-.027),(.10,.036,.053,-.009),(.121,.031,.038,.008)]
 vs=[];uv=[];faces=[];cols=40
 for i in range(21):
  z,rx,ry,yoff=interpolate(levels,i/20)
  for j in range(cols+1):
   a=2*pi*j/cols;vs.append((s*.095+rx*sin(a),yoff-ry*cos(a),z));uv.append((j/cols,i/20))
 for i in range(20):
  for j in range(cols):
   a=i*(cols+1)+j;faces.append((a,a+1,a+cols+2,a+cols+1))
 faces+=[tuple(range(cols,-1,-1)),tuple(20*(cols+1)+j for j in range(cols+1))]
 finish(mesh('Loafer.'+side,vs,faces,'shoe',uv))
 sweep('Sole.'+side,[(s*.095,-.03,.006),(s*.095,-.03,.019)],[.045,.045],[.090,.090],'black',2,40)
 curve('Loafer welt.'+side,[(s*.095+.043*sin(2*pi*j/40),-.032-.086*cos(2*pi*j/40),.035) for j in range(41)],.0012,'seam')
 curve('Loafer apron.'+side,[(s*.095-.031,-.035,.09),(s*.095-.027,-.085,.077),(s*.095,-.102,.07),(s*.095+.027,-.085,.077),(s*.095+.031,-.035,.09)],.0011,'shoe')
 # Exposed ankle subtly joins trouser cuff to shoe.
 sweep('Ankle.'+side,[(s*.095,.005,.09),(s*.095,0,.15)],[.027,.03],[.03,.032],'skin',8,20)

# Anatomical head surface: nose, cheeks, chin and mouth pad are integrated in one mesh.
profile=[(1.355,.025,.037,.007),(1.37,.047,.053,.003),(1.39,.066,.066,.001),(1.415,.083,.077,.004),(1.445,.102,.083,.008),(1.48,.112,.087,.01),(1.52,.111,.089,.012),(1.56,.105,.09,.015),(1.60,.079,.075,.015),(1.623,.04,.045,.016),(1.631,.001,.001,.016)]
def head_point(t,a):
 z,rx,ry,cy=interpolate(profile,t);x=rx*sin(a);y=cy-ry*cos(a)
 front=max(0,cos(a))**12
 nose=.038*exp(-(x/.018)**2-((z-1.461)/.022)**2)+.009*exp(-(x/.013)**2-((z-1.489)/.037)**2)
 cheeks=.006*(exp(-((x-.064)/.032)**2)+exp(-((x+.064)/.032)**2))*exp(-((z-1.446)/.025)**2)
 mouth=.009*exp(-(x/.039)**2-((z-1.415)/.022)**2)
 eyes=.0035*(exp(-((x-.045)/.028)**2)+exp(-((x+.045)/.028)**2))*exp(-((z-1.489)/.023)**2)
 y-=front*(nose+cheeks+mouth-eyes)
 return x,y,z
vs=[];uv=[];faces=[];rows=80;cols=96
for i in range(rows+1):
 for j in range(cols+1):
  a=-pi+2*pi*j/cols;vs.append(head_point(i/rows,a));uv.append((j/cols,i/rows))
for i in range(rows):
 for j in range(cols):
  a=i*(cols+1)+j;faces.append((a,a+1,a+cols+2,a+cols+1))
head=finish(mesh('Face and cranium',vs,faces,'skin',uv))
# The neck terminates inside collar and head, hidden continuous silhouette.
sweep('Neck',[(0,.008,1.32),(0,.009,1.372),(0,.013,1.40)],[.038,.039,.041],[.034,.036,.039],'skin',14,28)
for side,s in [('L',1),('R',-1)]:
 ear=ellipsoid('Ear.'+side,(s*.109,.004,1.463),(.021,.019,.035),'skin',28,18)
 ellipsoid('Ear concha.'+side,(s*.119,-.009,1.464),(.010,.005,.020),'ear_inner',20,14)
 curve('Ear helix.'+side,[(s*.111,-.011,1.436),(s*.123,-.014,1.447),(s*.127,-.012,1.473),(s*.119,-.009,1.49),(s*.107,-.006,1.477)],.0032,'skin')
 # Almond eyes set into face; lids follow the curved facial surface.
 cx=s*.045;cz=1.488;w=.025;up=.014;down=.010;cy=-.075
 e_v=[(cx,cy-.009,cz)];e_uv=[(.5,.5)];e_f=[];rings=7;sides=40
 for r in range(1,rings+1):
  rr=r/rings
  for j in range(sides):
   a=2*pi*j/sides;dx=w*cos(a);dz=(up if sin(a)>0 else down)*sin(a)
   # Outer corners recede along face; subtle upward outer corner.
   e_v.append((cx+dx*rr,cy-.009*(1-rr*rr)+s*dx*.20,cz+dz*rr+s*dx*.035*rr));e_uv.append((.5+.5*rr*cos(a),.5+.5*rr*sin(a)))
 for j in range(sides):e_f.append((0,1+j,1+(j+1)%sides))
 for r in range(rings-1):
  for j in range(sides):
   a=1+r*sides+j;b=1+r*sides+(j+1)%sides;e_f.append((a,b,b+sides,a+sides))
 finish(mesh('Eye white.'+side,e_v,e_f,'eye_white',e_uv))
 # Iris is subtly domed and physically inset into almond opening.
 ellipsoid('Iris rim.'+side,(cx,cy-.0105,cz+.001),(.0128,.0027,.0124),'iris_dark',32,16)
 ellipsoid('Brown iris.'+side,(cx,cy-.013,cz+.001),(.0116,.002,.0118),'iris',32,16)
 ellipsoid('Pupil.'+side,(cx,cy-.0152,cz+.001),(.0062,.0013,.0076),'black',24,14)
 ellipsoid('Eye catchlight.'+side,(cx-.0035,cy-.017,cz+.006),(.0023,.001,.0023),'eye_white',16,10)
 upper=[];lower=[]
 for j in range(17):
  a=pi*j/16;dx=w*cos(a);upper.append((cx+dx,cy+s*dx*.20-.001,cz+up*sin(a)+s*dx*.035))
  a=pi+pi*j/16;dx=w*cos(a);lower.append((cx+dx,cy+s*dx*.20-.0004,cz+down*sin(a)+s*dx*.035))
 curve('Upper eyelid.'+side,upper,.0025,'skin');curve('Lower eyelid.'+side,lower,.0018,'skin')
 curve('Eyelash line.'+side,[(x,y-.0018,z-.0006) for x,y,z in upper],.0012,'lash')
 curve('Brow.'+side,[(s*.021,-.077,1.524),(s*.039,-.080,1.53),(s*.058,-.073,1.529),(s*.071,-.063,1.522)],.0028,'hair')
 # Small nostril tucked under nose, not a protruding bead.
 ellipsoid('Nostril.'+side,(s*.009,-.108,1.448),(.003,.001,.0017),'lip',16,10)
# Integrated-looking lips follow muzzle with a subtle smile.
curve('Upper lip',[(-.027,-.078,1.418),(-.014,-.085,1.416),(-.005,-.088,1.417),(0,-.089,1.415),(.005,-.088,1.417),(.014,-.085,1.416),(.027,-.078,1.418)],.0017,'lip')
curve('Lower lip',[(-.024,-.079,1.417),(-.011,-.087,1.412),(0,-.089,1.411),(.011,-.087,1.412),(.024,-.079,1.417)],.0022,'lip')
curve('Smile',[(-.027,-.0795,1.419),(-.013,-.088,1.415),(0,-.090,1.414),(.013,-.088,1.415),(.027,-.0795,1.419)],.00065,'mouth')
# Hands: palms, thumbs and fingers welded and relaxed; no disconnected beads.
for side,s in [('L',1),('R',-1)]:
 wx=s*.30;wy=-.025;wz=.893
 palm=ellipsoid('Palm.'+side,(wx+s*.009,wy,wz-.038),(.029,.018,.046),'skin',28,18)
 parts=[palm,sweep('Wrist.'+side,[(wx,wy,wz+.02),(wx+s*.008,wy,wz-.035)],[.025,.026],[.018,.018],'skin',10,20)]
 for k,(dx,length) in enumerate([(-.020,.049),(-.006,.057),(.008,.053),(.020,.042)]):
  x=wx+s*(.012+dx);z=wz-.067
  parts.append(sweep(['Index','Middle','Ring','Pinky'][k]+'.'+side,[(x,wy,z+.012),(x+s*.004,wy-.003,z-length*.43),(x+s*.006,wy-.010,z-length*.85),(x+s*.005,wy-.016,z-length)],[.008,.0075,.0065,.0035],[.008,.007,.006,.0035],'skin',16,12))
 parts.append(sweep('Thumb.'+side,[(wx-s*.013,wy-.001,wz-.018),(wx-s*.034,wy-.006,wz-.039),(wx-s*.038,wy-.02,wz-.062)],[.013,.011,.006],[.012,.010,.006],'skin',20,14))
 join_remesh('Hand.'+side,parts,.0016,4,.45)

# Hair cap follows the skull instead of a sphere perched on top.
vs=[];uv=[];faces=[];rows=28;cols=80
for i in range(rows+1):
 t=i/rows
 for j in range(cols+1):
  a=-pi+2*pi*j/cols
  front=max(0,cos(a));theta_max=1.78-.86*front
  theta=.015+(theta_max-.015)*t
  vs.append((.121*sin(theta)*sin(a),.019-.109*sin(theta)*cos(a),1.498+.143*cos(theta)))
  uv.append((j/cols,t))
for i in range(rows):
 for j in range(cols):
  a=i*(cols+1)+j;faces.append((a,a+1,a+cols+2,a+cols+1))
cap=finish(mesh('Hair crown',vs,faces,'hair',uv))
active(cap);mod=cap.modifiers.new('Closed scalp shell','SOLIDIFY');mod.thickness=.008;mod.offset=-.5;bpy.ops.object.modifier_apply(modifier=mod.name)
# Broad overlapping ribbons shape the long hair; flattened cross-sections avoid sausage locks.
for k in range(13):
 a=pi*.40+(pi*1.20)*k/12
 sx=sin(a);cy=-cos(a)
 pts=[(.114*sx,.018+.098*cy,1.55),(.128*sx,.035+.104*cy,1.443),(.139*sx,.048+.112*cy,1.31),(.152*sx,.05+.107*cy,1.19),(.139*sx,.052+.095*cy,1.085),(.115*sx,.05+.07*cy,1.052+.02*sin(k))]
 sweep('Hair back ribbon.%02d'%k,pts,[.004,.031,.035,.034,.025,.0018],[.004,.018,.02,.019,.015,.0018],'hair',42,12)
 # Secondary restrained highlight tracing flow, mostly tucked into main strand.
 # Actual textured hair mesh carries fine detail; keep silhouette clean.
for side,s in [('L',1),('R',-1)]:
 pts=[(s*.102,-.018,1.575),(s*.127,-.028,1.509),(s*.132,-.015,1.408),(s*.143,-.017,1.31),(s*.158,-.012,1.226),(s*.148,.008,1.164)]
 sweep('Face framing wave.'+side,pts,[.003,.026,.024,.025,.02,.0015],[.003,.016,.016,.017,.013,.0015],'hair',44,14)
# Asymmetric swept fringe, rooted at part; follows forehead instead of hiding the face.
for k in range(5):
 pts=[(.043-k*.004,-.047,1.626-k*.002),(.008-k*.006,-.083,1.604-k*.004),(-.046-k*.006,-.090,1.57-k*.006),(-.089-k*.004,-.068,1.546-k*.007),(-.119-k*.002,-.029,1.493-k*.009)]
 sweep('Swept fringe.%02d'%k,pts,[.013,.022,.023,.018,.0015],[.008,.010,.012,.009,.0012],'hair',36,12)
for k in range(3):
 pts=[(.047+k*.006,-.043,1.623-k*.002),(.083+k*.006,-.068,1.59-k*.005),(.112+k*.004,-.05,1.552-k*.008),(.127+k*.003,-.021,1.493-k*.012)]
 sweep('Part fringe.%02d'%k,pts,[.015,.022,.018,.0015],[.008,.012,.01,.0015],'hair',32,12)
# Unite overlapping hair roots and crown, then relax seams into one coherent coiffure.
hair_parts=[o for o in list(hero.objects) if o.type=='MESH' and o.name.startswith(('Hair crown','Hair back ribbon','Face framing','Swept fringe','Part fringe'))]
hair=join_remesh('Hair sculpt',hair_parts,.0020,6,.22)
# Cylindrical UVs align supplied hair texture with vertical flow (seam at the back).
layer=hair.data.uv_layers.active
for poly in hair.data.polygons:
 us=[]
 for li in poly.loop_indices:
  v=hair.data.vertices[hair.data.loops[li].vertex_index].co
  u=(math.atan2(v.x,-v.y+.025)/(2*pi)+.5)%1
  us.append((li,u,(v.z-1.04)/.61))
 crossing=max(a[1] for a in us)-min(a[1] for a in us)>.5
 for li,u,v in us:layer.data[li].uv=(u+1 if crossing and u<.5 else u,v)

# Ivory cloth headband: flat broad ribbon hugging crown, not floating wire.
vs=[];uv=[];faces=[]
for i in range(65):
 a=-1.34+2.68*i/64
 for j in range(5):
  y=-.012+(j/4-.5)*.032
  vs.append((.126*sin(a),y,1.498+.140*cos(a)));uv.append((i/64,j/4))
for i in range(64):
 for j in range(4):
  a=i*5+j;faces.append((a,a+1,a+6,a+5))
o=mesh('Ivory fabric headband',vs,faces,'headband',uv);active(o)
mod=o.modifiers.new('Padded band','SOLIDIFY');mod.thickness=.004;bpy.ops.object.modifier_apply(modifier=mod.name);finish(o)
# Assign complexion attribute to every skin mesh. All parts use supplied skin base colour.
for o in list(hero.objects):
 if o.type!='MESH' or not any(m==mats['skin'] for m in o.data.materials):continue
 attr=o.data.color_attributes.new(name='Complexion',type='FLOAT_COLOR',domain='POINT')
 for v in o.data.vertices:
  x,y,z=o.matrix_world@v.co
  blush=(exp(-((abs(x)-.068)/.029)**2-((z-1.445)/.021)**2)*max(0,min(1,(-y-.025)/.045))) if z>1.35 else 0
  attr.data[v.index].color=(1,1-.14*blush,1-.12*blush,1)

# Decimate dense authoring surfaces before skinning; keep the silhouette, UVs and colours.
for o in list(hero.objects):
 if o.type!='MESH':continue
 triangles=sum(len(p.vertices)-2 for p in o.data.polygons)
 if o.name=='Blazer':budget=9000
 elif o.name=='Trousers':budget=6500
 elif o.name.startswith('Hand.'):budget=2400
 elif o.name=='Face and cranium':budget=6500
 elif o.name=='Knit sweater':budget=1800
 elif o.name=='Ribbed roll collar':budget=1200
 elif o.name=='Hair sculpt':budget=12000
 elif o.name.startswith(('Loafer.','Hair back ribbon','Face framing','Swept fringe','Part fringe')):budget=900
 else:budget=700
 if triangles>budget:
  active(o);d=o.modifiers.new('Export triangle budget','DECIMATE');d.ratio=budget/triangles;bpy.ops.object.modifier_apply(modifier=d.name)

# Build a coherent deforming humanoid rig with soft blended joints.
armdata=bpy.data.armatures.new('HeroineSkeleton');rig=bpy.data.objects.new('HeroineRig',armdata);hero.objects.link(rig);active(rig);bpy.ops.object.mode_set(mode='EDIT')
bones={
 'Root':((0,0,0),(0,0,.12),None),
 'Hips':((0,0,.83),(0,0,.96),'Root'),
 'Spine':((0,0,.96),(0,0,1.11),'Hips'),
 'Chest':((0,0,1.11),(0,0,1.29),'Spine'),
 'Neck':((0,0,1.29),(0,.008,1.39),'Chest'),
 'Head':((0,.008,1.39),(0,.01,1.60),'Neck')}
for side,s in [('L',1),('R',-1)]:
 bones.update({
 'Thigh.'+side:((s*.083,0,.85),(s*.091,-.012,.48),'Hips'),
 'Shin.'+side:((s*.091,-.012,.48),(s*.095,.003,.12),'Thigh.'+side),
 'Foot.'+side:((s*.095,.003,.12),(s*.095,-.1,.045),'Shin.'+side),
 'UpperArm.'+side:((s*.163,0,1.28),(s*.25,-.013,1.08),'Chest'),
 'LowerArm.'+side:((s*.25,-.013,1.08),(s*.30,-.025,.893),'UpperArm.'+side),
 'Hand.'+side:((s*.30,-.025,.893),(s*.312,-.03,.823),'LowerArm.'+side)})
 for fn,dx,ll in [('Index',-.020,.049),('Middle',-.006,.057),('Ring',.008,.053),('Pinky',.020,.042)]:
  x=s*(.312+dx);bones[fn+'1.'+side]=((x,-.025,.826),(x+s*.005,-.04,.826-ll),'Hand.'+side)
 bones['Thumb1.'+side]=((s*.287,-.026,.875),(s*.262,-.045,.831),'Hand.'+side)
for name,(h,t,parent) in bones.items():
 b=armdata.edit_bones.new(name);b.head=h;b.tail=t
 if parent:b.parent=armdata.edit_bones[parent]
bpy.ops.object.mode_set(mode='OBJECT');rig.show_in_front=True;armdata.display_type='OCTAHEDRAL'
def blend_height(z,pairs):
 if z<=pairs[0][0]:return {pairs[0][1]:1.}
 if z>=pairs[-1][0]:return {pairs[-1][1]:1.}
 for (a,ba),(b,bb) in zip(pairs,pairs[1:]):
  if a<=z<=b:
   if ba==bb:return {ba:1.}
   t=(z-a)/(b-a);t=t*t*(3-2*t);return {ba:1-t,bb:t}
def segdistance(p,a,b):
 a,b=Vector(a),Vector(b);d=b-a;f=max(0,min(1,(p-a).dot(d)/d.length_squared));return (p-a-f*d).length
for o in list(hero.objects):
 if o.type!='MESH':continue
 name=o.name
 active(o);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
 groups={n:o.vertex_groups.new(name=n) for n in bones}
 if name=='Trousers':
  for v in o.data.vertices:
   t=max(0,min(1,(v.co.z-.79)/.07));factor=1-.16*t*t*(3-2*t)
   v.co.x*=factor;v.co.y*=factor
 for v in o.data.vertices:
  x,y,z=v.co;side='L' if x>=0 else 'R'
  if name.startswith(('Hair','Face framing','Swept fringe','Part fringe','Ivory','Face and','Ear','Eye','Brow','Iris','Brown iris','Pupil','Nostril','Upper lip','Lower lip','Smile','Lower eyelid','Upper eyelid')):w={'Head':1.}
  elif name=='Neck' or name=='Ribbed roll collar':w=blend_height(z,[(1.30,'Chest'),(1.35,'Neck'),(1.40,'Head')])
  elif name.startswith('Hand.'):
   if z<.836:
    choices=['Hand.'+side]+[f+'1.'+side for f in ['Index','Middle','Ring','Pinky','Thumb']]
    ds=sorted([(segdistance(v.co,*bones[b][:2]),b) for b in choices])[:2]
    ws=[1/max(.002,d)**5 for d,b in ds];w={b:a/sum(ws) for (d,b),a in zip(ds,ws)}
   else:w=blend_height(z,[(.865,'Hand.'+side),(.915,'LowerArm.'+side)])
  elif name.startswith(('Loafer','Sole','Ankle')):w={'Foot.'+side:1.}
  elif name.startswith(('Trousers','Trouser')):w=blend_height(z,[(.16,'Shin.'+side),(.43,'Shin.'+side),(.53,'Thigh.'+side),(.78,'Thigh.'+side),(.88,'Hips')])
  elif name=='Blazer' and abs(x)>.15 and z>.89:
   w=blend_height(z,[(.91,'LowerArm.'+side),(1.035,'LowerArm.'+side),(1.125,'UpperArm.'+side),(1.30,'UpperArm.'+side)])
   t=max(0,min(1,(abs(x)-.15)/.06));w={b:a*t for b,a in w.items()};w['Chest']=1-t
  elif name.startswith('Cuff'):w={'LowerArm.'+side:1.}
  else:w=blend_height(z,[(.86,'Hips'),(1.0,'Spine'),(1.17,'Chest'),(1.34,'Chest')])
  total=sum(w.values())
  assert total>0, (name,v.index,tuple(v.co))
  w={b:a/total for b,a in w.items()}
  for b,a in w.items():
   if a>1e-6:groups[b].add([v.index],a,'REPLACE')
 # Remove unused vertex groups.
 for g in list(o.vertex_groups):
  if not any(any(vg.group==g.index for vg in v.groups) for v in o.data.vertices):o.vertex_groups.remove(g)
 mod=o.modifiers.new('Heroine deformation','ARMATURE');mod.object=rig;o.parent=rig
# Refined idle, Root stationary. Saved rest frame remains easy to edit.
scene.render.fps=30;scene.frame_start=0;scene.frame_end=120
for b in rig.pose.bones:b.rotation_mode='XYZ'
for f in [0,30,60,90,120]:
 t=f/120;wave=sin(2*pi*t);breath=.5-.5*cos(4*pi*t)
 for name in ['Hips','Spine','Chest','Neck','Head','UpperArm.L','UpperArm.R']:
  b=rig.pose.bones[name];b.rotation_euler=(0,0,0);b.location=(0,0,0)
  if name=='Hips':b.location.x=.0015*wave
  if name=='Chest':b.rotation_euler.x=.006*breath;b.rotation_euler.z=.004*wave
  if name=='Head':b.rotation_euler.y=.012*wave
  if name.startswith('UpperArm'):b.rotation_euler.x=.008*breath
  b.keyframe_insert(data_path='rotation_euler',frame=f,group=name);b.keyframe_insert(data_path='location',frame=f,group=name)
rig.animation_data.action.name='idle'
for fc in rig.animation_data.action.fcurves:
 for k in fc.keyframe_points:k.interpolation='BEZIER'
scene.frame_set(0)
# Godot convention: face -Z; Blender exporter converts +Y to -Z.
root=bpy.data.objects.new('Heroine',None);hero.objects.link(root);rig.parent=root;root.rotation_euler.z=pi
root['asset_height_m']=1.65;root['forward']='Godot -Z';root['notes']='Offline authored asset. Idle only; locomotion not yet authored.'
# Scale to exactly 1.65 m, keep soles on ground. Applied at root for skeleton consistency.
bpy.context.view_layer.update()
minz=min((o.matrix_world@v.co).z for o in hero.objects if o.type=='MESH' for v in o.data.vertices)
maxz=max((o.matrix_world@v.co).z for o in hero.objects if o.type=='MESH' for v in o.data.vertices)
root.scale=(1.65/(maxz-minz),)*3;root.location.z=-minz*root.scale.z
# Export meshes merged by material to keep draw calls modest. Authoring meshes stay separate.
def export_game_model():
 groups={}
 for o in list(hero.objects):
  if o.type!='MESH':continue
  key=tuple(m.name for m in o.data.materials)
  duplicate=o.copy();duplicate.data=o.data.copy();hero.objects.link(duplicate)
  groups.setdefault(key,[]).append(duplicate)
 export_meshes=[]
 for key,parts in groups.items():
  active(parts[0])
  for p in parts:p.select_set(True)
  bpy.ops.object.join();merged=bpy.context.object;merged.name='Heroine_'+key[0].replace(' ','_');export_meshes.append(merged)
 bpy.ops.object.select_all(action='DESELECT')
 for o in [root,rig]+export_meshes:o.select_set(True)
 bpy.context.view_layer.objects.active=rig
 bpy.ops.export_scene.gltf(filepath=str(OUT/'heroine.glb'),export_format='GLB',use_selection=True,export_yup=True,export_animations=True,export_skins=True,export_materials='EXPORT',export_extras=True,export_force_sampling=True)
 for o in export_meshes:bpy.data.objects.remove(o,do_unlink=True)
export_game_model()
# Neutral studio renders of the actual mesh, not generated concept art.
def studio_mat(name,color,rough=.8):return mat(name,color,rough)
ground=studio_mat('Studio warm stone',(.23,.21,.19))
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.002));floor=bpy.context.object;floor.name='Studio floor';move_coll(floor,studio);floor.data.materials.append(ground)
def area(name,pos,power,color,size,target):
 d=bpy.data.lights.new(name,'AREA');d.energy=power;d.color=color;d.shape='DISK';d.size=size
 o=bpy.data.objects.new(name,d);studio.objects.link(o);o.location=pos;o.rotation_euler=(Vector(target)-o.location).to_track_quat('-Z','Y').to_euler()
area('Key softbox',(2.0,3.5,4),300,(1,.84,.70),3,(0,0,1))
area('Cool fill',(-2,1.5,2.4),180,(.72,.83,1),2.8,(0,0,1))
area('Hair rim',(.6,-2,2.5),350,(1,.77,.51),2,(0,0,1.2))
scene.world=bpy.data.worlds.new('Soft grey studio');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.32,.36,.43,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.35
camdata=bpy.data.cameras.new('Portrait camera');cam=bpy.data.objects.new('Portrait camera',camdata);studio.objects.link(cam);scene.camera=cam
scene.render.engine='CYCLES';scene.cycles.samples=64;scene.cycles.use_denoising=False;scene.cycles.device='CPU'
scene.render.resolution_x=1100;scene.render.resolution_y=1400;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.view_settings.view_transform='AgX'
def camera(pos,target,ortho):
 cam.location=pos;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=ortho
camera((-2.5,5,2.0),(0,0,.85),1.97)
# Pack textures so .blend is independently portable. External PNGs remain available as sources.
for im in bpy.data.images:
 if im.source=='FILE':im.pack()
# Save clean viewport focused on character, studio hidden only in viewport.
for o in studio.objects:o.hide_set(True)
active(rig)
for area_ui in bpy.context.screen.areas:
 if area_ui.type=='VIEW_3D':
  area_ui.spaces.active.region_3d.view_distance=2.6;area_ui.spaces.active.region_3d.view_location=(0,0,.86);area_ui.spaces.active.clip_end=100
bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'blender/heroine.blend'),compress=True)
report={'height_m':1.65,'triangles':sum(sum(len(p.vertices)-2 for p in o.data.polygons) for o in hero.objects if o.type=='MESH'),'mesh_objects':sum(o.type=='MESH' for o in hero.objects),'bones':len(bones),'animations':['idle'],'source_textures':'User supplied heroine_beautiful_godot.zip','forward':'-Z in Godot','notes':['No runtime generation','Not yet locomotion animated','Studio excluded from GLB','Original textures embedded; complexion vertex colour multiplies skin texture']}
(ROOT/'asset_report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print('ASSET_REPORT',report,flush=True)
scene.render.filepath=str(PREVIEW/'heroine_three_quarter.png');bpy.ops.render.render(write_still=True)
camera((0,5,1.6),(0,0,1.44),.55);scene.render.resolution_x=1100;scene.render.resolution_y=1100;scene.render.filepath=str(PREVIEW/'heroine_face.png');bpy.ops.render.render(write_still=True)
camera((2.5,-5,1.8),(0,0,.85),1.97);scene.render.resolution_x=1100;scene.render.resolution_y=1400;scene.render.filepath=str(PREVIEW/'heroine_back.png');bpy.ops.render.render(write_still=True)
print('DONE',flush=True)
