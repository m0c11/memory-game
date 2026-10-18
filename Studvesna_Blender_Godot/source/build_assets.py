"""Build real, textured Blender meshes; export each asset as self-contained glTF 2.0.
Run with Blender 4.5 or its bpy Python module. Metres; Z up in Blender; -Y front.
"""
import bpy, math, json, os, sys
from pathlib import Path
from mathutils import Vector, Matrix
from math import sin,cos,pi

ROOT=Path(__file__).resolve().parents[1]
if str(Path(__file__).resolve().parent) not in sys.path:sys.path.insert(0,str(Path(__file__).resolve().parent))
TEX=ROOT/'textures'; MODELS=ROOT/'models'; RENDERS=ROOT/'renders'
for d in (MODELS,RENDERS): d.mkdir(exist_ok=True)
M={}; ASSETS=[]; CURRENT=None; PARTS=[]; IMAGES={}

def init():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc=bpy.context.scene;sc.name='Asset_Library';sc.unit_settings.system='METRIC';sc.unit_settings.scale_length=1
    for name in sorted({p.name.replace('_base.png','') for p in TEX.glob('*_base.png')}): material(name)

def image(name,suffix):
    key=(name,suffix)
    if key not in IMAGES:
        im=bpy.data.images.load(str(TEX/f'{name}_{suffix}.png'),check_existing=True)
        if suffix!='base': im.colorspace_settings.name='Non-Color'
        IMAGES[key]=im
    return IMAGES[key]

def material(name):
    mat=bpy.data.materials.new(name);mat.use_nodes=True;mat.use_backface_culling=True
    n=mat.node_tree.nodes;l=mat.node_tree.links; bs=n.get('Principled BSDF')
    tx=n.new('ShaderNodeTexImage');tx.image=image(name,'base');tx.location=(-600,180)
    l.new(tx.outputs['Color'],bs.inputs['Base Color'])
    orm=n.new('ShaderNodeTexImage');orm.image=image(name,'orm');orm.location=(-600,-70)
    sep=n.new('ShaderNodeSeparateColor');sep.mode='RGB';sep.location=(-350,-70)
    l.new(orm.outputs['Color'],sep.inputs['Color']);l.new(sep.outputs['Green'],bs.inputs['Roughness']);l.new(sep.outputs['Blue'],bs.inputs['Metallic'])
    nt=n.new('ShaderNodeTexImage');nt.image=image(name,'normal');nt.location=(-600,-300)
    normal=n.new('ShaderNodeNormalMap');normal.inputs['Strength'].default_value=.3;normal.location=(-350,-300)
    l.new(nt.outputs['Color'],normal.inputs['Color']);l.new(normal.outputs['Normal'],bs.inputs['Normal'])
    if name in ['led_green','led_red','led_blue','ivory_light','amber']:
        l.new(tx.outputs['Color'],bs.inputs['Emission Color']);bs.inputs['Emission Strength'].default_value=.28 if name=='amber' else .4
    if name=='audio_display':
        l.new(tx.outputs['Color'],bs.inputs['Emission Color']);bs.inputs['Emission Strength'].default_value=.23
    if name in ('plastic_clear','water'):
        bs.inputs['Alpha'].default_value=.24 if name=='plastic_clear' else .52
        bs.inputs['IOR'].default_value=1.46 if name=='plastic_clear' else 1.333
        mat.surface_render_method='DITHERED';mat.use_backface_culling=False
    M[name]=mat
    return mat

def asset(key,title,category,notes=''):
    global CURRENT,PARTS
    col=bpy.data.collections.new(key);bpy.context.scene.collection.children.link(col)
    root=bpy.data.objects.new(key,None);col.objects.link(root);root.empty_display_size=.15
    root['asset_id']=key;root['title_ru']=title;root['units']='metres';root['reference']='references/'
    CURRENT={'id':key,'title':title,'category':category,'notes':notes,'collection':col,'root':root,'objects':[]};PARTS=[]
    ASSETS.append(CURRENT)
    return root

def register(o,name,mat=None):
    o.name=CURRENT['id']+'__'+name
    for col in list(o.users_collection):col.objects.unlink(o)
    CURRENT['collection'].objects.link(o);o.parent=CURRENT['root']
    if mat:o.data.materials.append(M[mat])
    CURRENT['objects'].append(o);PARTS.append(o)
    return o

def uvbox(o,scale=1):
    if o.type!='MESH':return
    uv=o.data.uv_layers.active or o.data.uv_layers.new(name='UVMap')
    for p in o.data.polygons:
        axis=max(range(3),key=lambda k:abs(p.normal[k]))
        for li in p.loop_indices:
            v=o.data.vertices[o.data.loops[li].vertex_index].co
            a,b=((v.y,v.z) if axis==0 else (v.x,v.z) if axis==1 else (v.x,v.y))
            uv.data[li].uv=(a/scale+.5,b/scale+.5)

def bevel(o,width=.01,segments=3):
    if width<=0:return o
    mod=o.modifiers.new('Soft manufactured edges','BEVEL');mod.width=width;mod.segments=segments
    mod.affect='EDGES';mod.limit_method='ANGLE';mod.harden_normals=True
    mod=o.modifiers.new('Weighted corner normals','WEIGHTED_NORMAL');mod.keep_sharp=True;mod.weight=30
    return o

def cube(name,loc,dims,mat,bev=.005,rot=None):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc)
    o=register(bpy.context.object,name,mat);o.dimensions=dims
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);uvbox(o)
    if rot:o.rotation_euler=rot
    for p in o.data.polygons:p.use_smooth=True
    bevel(o,bev,6 if bev>=.02 else 3);return o

def cyl(name,loc,r,depth,mat,axis=(0,0,1),vertices=24,bev=.001):
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=r,depth=depth,location=loc)
    o=register(bpy.context.object,name,mat)
    o.rotation_euler=Vector(axis).to_track_quat('Z','Y').to_euler()
    for p in o.data.polygons:p.use_smooth=len(p.vertices)==4
    bevel(o,bev,2);return o

def rod(name,a,b,r,mat,vertices=16):
    av,bv=Vector(a),Vector(b);return cyl(name,(av+bv)/2,r,(bv-av).length,mat,bv-av,vertices)

def sphere(name,loc,scale,mat,segments=24,rings=12):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=rings,radius=1,location=loc)
    o=register(bpy.context.object,name,mat);o.scale=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    for p in o.data.polygons:p.use_smooth=True
    return o

def curve(name,pts,r,mat,cyclic=False,res=3):
    cu=bpy.data.curves.new(name,'CURVE');cu.dimensions='3D';cu.resolution_u=8;cu.bevel_depth=r;cu.bevel_resolution=res
    spl=cu.splines.new('POLY');spl.points.add(len(pts)-1)
    for p,co in zip(spl.points,pts):p.co=(*co,1)
    spl.use_cyclic_u=cyclic
    o=bpy.data.objects.new(name,cu);return register(o,name,mat)

def ribbon(name,pts,width,thick,mat,width_axis=(0,1,0)):
    vv=[];faces=[];uv=[]
    for i,p in enumerate(pts):
        p=Vector(p);t=(Vector(pts[min(i+1,len(pts)-1)])-Vector(pts[max(0,i-1)])).normalized()
        w=Vector(width_axis);w=(w-t*w.dot(t)).normalized()
        for side in [-1,1]:vv.append(tuple(p+w*side*width/2));uv.append(((side+1)/2,i/(len(pts)-1)))
    for i in range(len(pts)-1):a=i*2;faces.append((a,a+1,a+3,a+2))
    o=mesh(name,vv,faces,mat,uv,smooth=True)
    so=o.modifiers.new('Woven webbing thickness','SOLIDIFY');so.thickness=thick
    bevel(o,.001,2);return o

def mesh(name,verts,faces,mat,uv=None,bev=0,smooth=False):
    me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update()
    o=bpy.data.objects.new(name,me);register(o,name,mat)
    if uv is not None:
        layer=me.uv_layers.new(name='UVMap')
        for p in me.polygons:
            for li in p.loop_indices:layer.data[li].uv=uv[me.loops[li].vertex_index]
    else:uvbox(o)
    for p in me.polygons:p.use_smooth=smooth
    bevel(o,bev,3);return o

def quad(name,verts,mat):return mesh(name,verts,[(0,1,2,3)],mat,[(0,0),(1,0),(1,1),(0,1)])

def torus(name,loc,major,minor,mat,rot=(0,0,0),scale=(1,1,1)):
    bpy.ops.mesh.primitive_torus_add(major_radius=major,minor_radius=minor,major_segments=32,minor_segments=8,location=loc,rotation=rot)
    o=register(bpy.context.object,name,mat);o.scale=scale
    for p in o.data.polygons:p.use_smooth=True
    return o

def screw(name,loc,axis=(0,-1,0),r=.004):
    o=cyl(name,loc,r,.002,'silver',axis,vertices=12,bev=.0004)
    return o

def transform_objects(objects,matrix):
    for o in objects:o.matrix_basis=matrix@o.matrix_basis

def roundrect(w,h,r,n=8):
    pts=[]
    for cx,cy,a in [(w/2-r,h/2-r,0),(-w/2+r,h/2-r,pi/2),(-w/2+r,-h/2+r,pi),(w/2-r,-h/2+r,3*pi/2)]:
        for j in range(n+1):
            q=a+j*pi/(2*n);pts.append((cx+r*cos(q),cy+r*sin(q)))
    return pts

def lathe(name,profile,mat,segments=48,closed=True,uvscale=1):
    vv=[];uv=[]
    for i,(r,z) in enumerate(profile):
        for j in range(segments+1):
            t=j*2*pi/segments;vv.append((r*cos(t),r*sin(t),z));uv.append((j/segments,i/(len(profile)-1)*uvscale))
    faces=[];n=segments+1
    for i in range(len(profile)-1):
        for j in range(segments):a=i*n+j;faces.append((a,a+1,a+n+1,a+n))
    if closed:faces.extend([tuple(reversed(range(segments))),tuple((len(profile)-1)*n+j for j in range(segments))])
    return mesh(name,vv,faces,mat,uv,smooth=True)

def chair():
    asset('01_auditorium_chair','Кресло зрительного зала','props')
    cube('seat_shell',(0,.0,.445),(.49,.48,.075),'charcoal',.035)
    cube('blue_seat',(0,-.012,.49),(.48,.45,.13),'blue_upholstery',.055)
    back=cube('back_shell',(0,.195,.78),(.475,.065,.57),'charcoal',.05,(-.075,0,0))
    cube('blue_backrest',(0,.15,.79),(.455,.11,.55),'blue_upholstery',.055,(-.075,0,0))
    pts=[(x,.088+z*.075,.79+z) for x,z in roundrect(.421,.514,.057)]
    curve('backrest_seam',pts,.0014,'navy_fabric',True,1)
    curve('seat_piping',[(x,y-.012,.503) for x,y in roundrect(.46,.435,.055)],.0016,'navy_fabric',True,1)
    for side in [-1,1]:
        x=side*.285
        cube('arm_side',(x,.015,.50),(.09,.42,.32),'charcoal',.013)
        cube('wood_arm_cap',(x,-.005,.674),(.092,.455,.042),'wood_armrest',.014)
        cube('steel_pedestal',(x,.07,.235),(.055,.10,.39),'dark_metal',.006)
        cube('mount_foot',(x,.06,.017),(.135,.17,.034),'charcoal',.008)
        for y in [-.003,.125]:screw('floor_bolt',(x,y,.036),(0,0,1),.008)
    rod('underseat_crossbar',(-.27,.11,.365),(.27,.11,.365),.024,'dark_metal')

def folding_chair(folded=False):
    asset('03_folding_chair_folded' if folded else '02_folding_chair_open','Складной стул — сложен' if folded else 'Складной стул — раскрыт','props')
    if not folded:
        for x in [-.205,.205]:
            pts=[(x,-.255,.025),(x,-.125,.37),(x,.12,.81),(x,.10,.855)]
            curve('main_tube',pts,.013,'dark_metal')
            rod('rear_leg',(x,.25,.027),(x,-.015,.47),.012,'dark_metal')
            cyl('hinge',(x,.009,.454),.024,.032,'charcoal',(1,0,0));screw('hinge_bolt',(x+(.018 if x>0 else -.018),.009,.454),(1,0,0),.006)
            for y in [-.255,.25]:cube('rubber_foot',(x,y,.021),(.038,.048,.035),'rubber',.01)
        rod('back_top',(-.205,.10,.85),(.205,.10,.85),.014,'dark_metal')
        cube('back_pad',(0,.105,.718),(.39,.045,.225),'black_fabric',.035,(-.10,0,0))
        cube('seat_frame',(0,-.007,.44),(.435,.425,.038),'dark_metal',.035)
        cube('seat_pad',(0,-.015,.465),(.405,.399,.049),'black_fabric',.035)
        rod('leg_crossbrace',(-.205,.19,.16),(.205,.19,.16),.009,'dark_metal')
        for x in [-.18,.18]:rod('fold_brace',(x,-.15,.42),(x,.16,.25),.007,'silver')
    else:
        for x in [-.205,.205]:
            curve('main_tube',[(x,0,.025),(x,0,.87),(x*.93,0,.9)],.013,'dark_metal')
            rod('folded_leg',(x,.07,.025),(x,.035,.71),.012,'dark_metal')
            cube('foot',(x,.025,.02),(.038,.11,.035),'rubber',.009)
            cyl('hinge',(x,.02,.61),.022,.031,'charcoal',(1,0,0))
        rod('back_top',(-.19,0,.898),(.19,0,.898),.014,'dark_metal')
        cube('back_pad',(0,-.015,.773),(.39,.045,.225),'black_fabric',.035)
        cube('folded_seat_rim',(0,-.031,.44),(.435,.047,.405),'dark_metal',.035)
        cube('folded_seat_pad',(0,-.060,.44),(.405,.046,.375),'black_fabric',.035)
        rod('crossbar',(-.205,.066,.2),(.205,.066,.2),.009,'dark_metal')

def tripod(z=.12,r=.32):
    cyl('tripod_hub',(0,0,z),.027,.1,'charcoal')
    for a in [pi/2,pi/2+2*pi/3,pi/2+4*pi/3]:
        p=(r*cos(a),r*sin(a),.025);rod('leg',(0,0,z),p,.011,'dark_metal')
        sphere('rubber_foot',p,(.023,.023,.013),'rubber',16,8)

def musicstand():
    asset('04_music_stand','Пюпитр','props');tripod(.13,.29)
    cyl('lower_tube',(0,0,.45),.015,.65,'dark_metal');cyl('height_collar',(0,0,.76),.022,.06,'charcoal')
    cyl('upper_tube',(0,0,.95),.010,.4,'dark_metal')
    rod('tightening_screw',(.0,0,.76),(.047,0,.76),.006,'dark_metal');sphere('knob',(.05,0,.76),(.013,.016,.016),'rubber')
    start=len(PARTS)
    cube('tray',(0,0,.16),(.49,.016,.32),'charcoal',.008)
    cube('tray_lip',(0,-.024,.009),(.49,.063,.018),'charcoal',.005)
    for x,z in [(-.055,.09),(.055,.09),(0,.21)]:screw('tray_rivet',(x,-.009,z),r=.004)
    cyl('tilt_hinge',(0,.04,.08),.025,.09,'dark_metal',(1,0,0))
    transform_objects(PARTS[start:],Matrix.Translation((0,0,1.02))@Matrix.Rotation(math.radians(-13),4,'X'))

def micstand():
    asset('05_microphone_stand','Микрофон на стойке','props');tripod(.13,.31)
    cyl('lower_shaft',(0,0,.57),.014,.9,'dark_metal');cyl('adjustment_collar',(0,0,1.01),.024,.09,'rubber')
    cyl('upper_shaft',(0,0,1.26),.010,.44,'dark_metal')
    cyl('tilt_knuckle',(0,0,1.49),.028,.046,'charcoal',(1,0,0))
    rod('short_angled_neck',(0,0,1.49),(.0,-.09,1.62),.011,'dark_metal')
    a=Vector((0,-.1,1.62));v=Vector((0,-.92,.4)).normalized()
    rod('mic_body',a,a+v*.16,.019,'charcoal',24)
    cyl('mic_grille_ring',a+v*.17,.025,.017,'silver',v)
    sphere('microphone_grille',a+v*.205,(.027,.027,.04),'speaker_grille')
    PARTS[-1].rotation_euler=v.to_track_quat('Z','Y').to_euler()
    curve('mic_short_cable',[tuple(a),(.04,-.04,1.5),(.042,.02,1.35),(0,.025,1.15)],.003,'rubber')

def desk():
    asset('06_sound_desk','Стол звукорежиссёра','props')
    cube('desktop',(0,0,.745),(1.5,.74,.045),'charcoal',.012)
    cube('bottom_shelf',(0,.02,.13),(1.43,.64,.038),'charcoal',.006)
    cube('back_panel',(0,.337,.4),(1.42,.032,.55),'charcoal',.004)
    for side in [-1,1]:
        x=side*.735;cube('side_panel',(x,0,.425),(.045,.76,.85),'charcoal',.008)
        cube('rubber_foot',(x,-.26,.012),(.07,.12,.025),'rubber',.005)
        cube('rubber_foot',(x,.26,.012),(.07,.12,.025),'rubber',.005)
        for z in [.19,.65,.79]:screw('side_fastener',(x+side*.025,-.24,z),(side,0,0),.005)
    cube('rear_raised_rail',(0,.355,.81),(1.44,.035,.12),'charcoal',.004)
    for x in [-.58,.58]:cyl('cable_grommet',(x,.20,.772),.031,.004,'rubber')

def mixer():
    asset('07_mixing_console','Микшерный пульт','props')
    w,d=.95,.57
    v=[(-w/2,-d/2,0),(w/2,-d/2,0),(w/2,d/2,0),(-w/2,d/2,0),(-w/2,-d/2,.06),(w/2,-d/2,.06),(w/2,d/2,.16),(-w/2,d/2,.16)]
    mesh('sloping_case',v,[(0,3,2,1),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7)],'charcoal',bev=.009)
    def z(y):return .111+y*(.1/d)
    face=[(-.444,-.258,z(-.258)+.003),(.444,-.258,z(-.258)+.003),(.444,.257,z(.257)+.003),(-.444,.257,z(.257)+.003)]
    quad('printed_control_face',face,'mixer_panel')
    tilt=math.atan(.1/d);normal=(0,-sin(tilt),cos(tilt))
    for i in range(16):
        x=-.405+i*.054
        for j in range(4):
            y=.17-j*.06
            cyl('rotary_pot',(x,y,z(y)+.015),.013,.025,'dark_metal',normal,20)
            cyl('knob_cap',(x,y-sin(tilt)*.014,z(y)+.028),.0095,.002,'led_blue' if j==2 else 'charcoal',normal,16)
            cube('knob_index',(x,y-.004,z(y)+.030),(.0015,.009,.001),'silver',.0003,(tilt,0,0))
        y=-.14
        cube('fader_slot',(x,y,z(y)+.004),(.006,.16,.002),'rubber',.002,(tilt,0,0))
        yy=-.175+(i%5)*.016
        cube('fader_cap',(x,yy,z(yy)+.012),(.028,.018,.012),'cream_trim',.003,(tilt,0,0))
        cube('channel_button',(x,-.032,z(-.032)+.008),(.019,.013,.008),'led_red' if i%4==0 else 'cream_trim',.003,(tilt,0,0))
        for j in range(4):
            yy=.219-j*.008
            cube('level_led',(x,yy,z(yy)+.005),(.004,.004,.003),'led_green' if j>0 else 'amber',.0005)
    for x in [-.46,.46]:
        cube('side_grip',(x,0,.085),(.035,.54,.12),'dark_metal',.007)
        for y in [-.21,.20]:screw('case_screw',(x,y,z(y)+.006),normal,.004)
    for i in range(8):
        x=-.34+i*.096;cyl('rear_jack',(x,.286,.105),.013,.005,'silver',(0,1,0));cyl('jack_hole',(x,.29,.105),.007,.006,'rubber',(0,1,0))

def laptop():
    asset('08_audio_laptop','Ноутбук со звуковыми дорожками','props')
    cube('base',(0,0,.012),(.36,.245,.024),'charcoal',.009)
    cube('keyboard_recess',(0,.032,.025),(.324,.128,.003),'rubber',.005)
    for row in range(5):
        for col in range(14):
            x=-.147+col*.0226;y=-.018+row*.023
            cube('key',(x,y,.029),(.019,.018,.004),'charcoal',.002)
    cube('spacebar',(0,-.035,.029),(.12,.013,.004),'charcoal',.002)
    cube('trackpad',(0,-.081,.025),(.112,.058,.002),'dark_metal',.004)
    for x in [-.13,.13]:cyl('hinge',(x,.112,.025),.009,.065,'dark_metal',(1,0,0))
    start=len(PARTS)
    cube('lid',(0,0,.106),(.36,.013,.212),'charcoal',.008)
    quad('screen',[(-.163,-.007,.018),(.163,-.007,.018),(.163,-.007,.197),(-.163,-.007,.197)],'audio_display')
    cyl('webcam',(0,-.008,.204),.0025,.001,'screen_black',(0,-1,0),12)
    transform_objects(PARTS[start:],Matrix.Translation((0,.113,.025))@Matrix.Rotation(math.radians(-13),4,'X'))
    for x in [-.14,.14]:
        for y in [-.08,.07]:cube('rubber_pad',(x,y,-.001),(.022,.022,.004),'rubber',.004)

def wall_display():
    asset('09_wall_display','Настенный экран','props')
    cube('display_body',(0,0,.32),(1.02,.057,.60),'charcoal',.012)
    quad('unlit_glass',[(-.486,-.029,.044),(.486,-.029,.044),(.486,-.029,.596),(-.486,-.029,.596)],'screen_black')
    cube('wall_plate',(0,.22,.32),(.13,.025,.25),'dark_metal',.008)
    rod('articulated_arm',(-.09,.045,.32),(.10,.20,.32),.025,'dark_metal')
    for x in [-.16,.16]:cube('vesa_rail',(x,.039,.32),(.035,.024,.28),'dark_metal',.004)
    cube('tiny_power_led',(.40,-.031,.038),(.005,.002,.003),'led_blue',.0005)

def floor_monitor():
    asset('10_stage_wedge_monitor','Напольный сценический монитор','props')
    w=.48
    v=[(-w/2,-.19,0),(w/2,-.19,0),(w/2,.16,0),(-w/2,.16,0),(-w/2,-.19,.065),(w/2,-.19,.065),(w/2,.075,.29),(-w/2,.075,.29),(-w/2,.16,.20),(w/2,.16,.20)]
    mesh('wedge_cabinet',v,[(0,3,2,1),(0,1,5,4),(4,5,6,7),(7,6,9,8),(8,9,2,3),(0,4,7,8,3),(1,2,9,6,5)],'charcoal',bev=.012)
    quad('perforated_grille',[(-.22,-.178,.077),(.22,-.178,.077),(.22,.063,.282),(-.22,.063,.282)],'speaker_grille')
    for side in [-1,1]:
        x=side*.246
        cube('recessed_handle',(x,.025,.115),(.007,.115,.054),'rubber',.017)
        cube('handle_bar',(x+side*.004,.025,.118),(.009,.083,.012),'dark_metal',.004)
        for y in [-.13,.1]:cube('foot',(side*.17,y,.001),(.04,.04,.012),'rubber',.005)
    n=Vector((0,-.225,.265)).normalized()
    for x in [-.21,.21]:
        for y in [-.16,.047]:screw('grille_screw',(x,y,.065+(y+.19)*.225/.265+.006),n,.004)
    cyl('input_socket',(0,.166,.10),.017,.006,'silver',(0,1,0))

def wall_speaker():
    asset('11_column_speaker','Высокая настенная колонка','props')
    cube('cabinet',(0,0,.56),(.245,.19,1.10),'charcoal',.012)
    for k in range(3):
        z=.205+k*.355
        cube('grille_frame',(0,-.099,z),(.223,.009,.342),'dark_metal',.005)
        quad('grille',[(-.105,-.105,z-.163),(.105,-.105,z-.163),(.105,-.105,z+.163),(-.105,-.105,z+.163)],'speaker_grille')
        for x in [-.098,.098]:
            for zz in [-.153,.153]:screw('grille_bolt',(x,-.107,z+zz),r=.003)
    cube('wall_bracket',(0,.155,.57),(.12,.06,.22),'dark_metal',.006)
    torus('hanging_eye',(0,0,1.135),.026,.006,'dark_metal',(pi/2,0,0))

def stage_light():
    asset('12_stage_spotlight','Сценический прожектор','props')
    cyl('housing',(0,0,.22),.125,.25,'charcoal',(0,1,0),40,.005)
    cyl('rear_cover',(0,.138,.22),.107,.03,'dark_metal',(0,1,0),32,.004)
    for y in [-.105,.08,.11]:cyl('housing_ring',(0,y,.22),.13,.014,'dark_metal',(0,1,0),40,.002)
    cyl('front_bezel',(0,-.137,.22),.134,.04,'dark_metal',(0,1,0),40,.004)
    cyl('amber_fresnel_lens',(0,-.16,.22),.108,.008,'amber',(0,1,0),48,.001)
    for r in [.033,.053,.073,.092]:torus('fresnel_ring',(0,-.165,.22),r,.0014,'amber',(pi/2,0,0))
    for a in range(8):
        t=a*2*pi/8;cube('cooling_rib',(.126*cos(t),.04,.22+.126*sin(t)),(.014,.14,.014),'dark_metal',.002,(0,-t,0))
    for x in [-.165,.165]:
        cube('yoke_side',(x,.015,.32),(.025,.045,.28),'dark_metal',.008)
        cyl('tilt_knob',(x*1.085,0,.22),.03,.03,'rubber',(1,0,0),20)
    cube('yoke_top',(0,.015,.455),(.355,.045,.026),'dark_metal',.006)
    cube('mounting_clamp',(0,.015,.50),(.055,.075,.07),'dark_metal',.008)
    curve('power_cord',[(.04,.14,.23),(.08,.23,.21),(-.08,.24,.14),(-.14,.14,.18)],.004,'rubber')

def cable():
    asset('13_audio_cable_coil','Аудиокабель с разъёмами XLR','props')
    pts=[]
    for i in range(501):
        t=i/500*2*pi*5;r=.12+.06*i/500;pts.append((r*cos(t),r*sin(t),.012))
    curve('five_coils',pts,.005,'rubber',False,2)
    curve('lead_a',[(.12,0,.012),(.205,-.045,.015),(.215,-.13,.014),(.18,-.205,.014)],.005,'rubber')
    curve('lead_b',[(.18,0,.012),(.23,.01,.014),(.27,-.09,.014),(.30,-.15,.014)],.005,'rubber')
    def connector(pos,direction,male):
        pos=Vector(pos);v=Vector(direction).normalized()
        for z,r,l,mat in [(0,.009,.034,'rubber'),(.034,.012,.043,'silver'),(.059,.0125,.01,'charcoal')]:
            cyl('XLR_shell',pos+v*z,r,l,mat,v,24)
        for i in range(4):cyl('strain_relief_ridge',pos+v*(-.01+i*.006),.010,.002,'rubber',v,20)
        end=pos+v*.065;cyl('socket_face',end,.0095,.002,'rubber',v,24)
        tangent=v.cross(Vector((0,0,1))).normalized();up=v.cross(tangent).normalized()
        for a in [pi/2,pi/2+2*pi/3,pi/2+4*pi/3]:
            q=end+tangent*cos(a)*.004+up*sin(a)*.004
            cyl('pin' if male else 'pin_socket',q+v*.002,.0012,.004 if male else .001,'silver' if male else 'screen_black',v,10)
    connector((.18,-.205,.018),(-.7,-.7,0),True);connector((.30,-.15,.018),(.25,-.97,0),False)
    cube('cable_tie',(-.15,0,.014),(.069,.024,.027),'charcoal',.005)

def duffel():
    asset('14_red_sports_bag','Красная спортивная сумка','props')
    cube('soft_canvas_body',(0,0,.157),(.55,.29,.29),'red_canvas',.065)
    cube('reinforced_bottom',(0,0,.035),(.54,.282,.065),'rubber',.027)
    for side in [-1,1]:
        x=side*.27
        cube('end_pocket',(x,0,.155),(.044,.252,.238),'red_canvas',.035)
        pts=[(x+side*.023,y,z+.155) for y,z in roundrect(.237,.223,.035)]
        curve('pocket_piping',pts,.0032,'rubber',True,2)
        cube('handle_anchor',(side*.18,-.151,.238),(.028,.009,.045),'rubber',.004)
        torus('strap_ring',(side*.30,0,.24),.014,.003,'silver',(pi/2,0,0))
    for x in [-.163,.163]:
        # Front/back woven webbing down to the protected base.
        for y in [-.144,.144]:
            cube('webbing_band',(x,y,.152),(.033,.009,.205),'black_fabric',.004)
            for z in [.075,.225]:screw('strap_rivet',(x,y+(-.006 if y<0 else .006),z),(0,-1 if y<0 else 1,0),.0035)
        for y in [-.085,.085]:
            pts=[]
            for i in range(25):
                t=i/24*pi;pts.append((-.163+.326*i/24,y,.273+.15*sin(t)))
            # Two parallel flat-ish tube runs describe a fabric handle with a grip.
            if x<0:ribbon('carry_handle',pts,.028,.004,'black_fabric')
    cube('red_handle_grip',(0,-.085,.417),(.12,.031,.026),'red_canvas',.012)
    curve('top_zipper',[(x,0,.305) for x in [-.23,-.16,-.08,0,.08,.16,.23]],.004,'rubber')
    for i in range(46):
        x=-.22+i*.0095;cube('zipper_teeth',(x,0,.307),(.003,.008,.002),'silver',.0004)
    cube('zipper_pull',(.18,-.009,.313),(.026,.012,.004),'dark_metal',.004)
    ribbon('loose_shoulder_strap',[(-.295,.03,.24),(-.36,-.03,.11),(-.36,-.11,.06),(-.30,-.18,.035),(0,-.225,.029),(.32,-.18,.04),(.36,-.04,.12),(.295,.03,.24)],.025,.004,'black_fabric',(0,0,1))
    cube('shoulder_pad',(.1,-.22,.033),(.14,.03,.025),'rubber',.012)

def kneepads():
    asset('15_kneepads_pair','Парные танцевальные наколенники','props')
    for idx,xx in enumerate([-.10,.10]):
        start=len(PARTS)
        # Hollow neoprene sleeve with a sewn rim; no dummy leg inside.
        prof=[(.062,.015),(.070,.04),(.066,.12),(.061,.19),(.057,.195),(.057,.185),(.061,.12),(.065,.04),(.057,.017)]
        sleeve=lathe('neoprene_sleeve',prof,'black_fabric',40,False);sleeve.scale.y=.65
        pad=sphere('oval_front_padding',(0,-.046,.105),(.061,.024,.081),'black_fabric',32,16)
        pts=[(.056*cos(t),-.052-.017*sin(t)**2,.105+.076*sin(t)) for t in [i*2*pi/80 for i in range(80)]]
        curve('sewn_pad_outline',pts,.0014,'charcoal',True,1)
        for z in [.04,.164]:
            for side in [-1,1]:
                cube('velcro_wing',(side*.061,-.004,z),(.035,.07,.027),'black_fabric',.006)
                cube('velcro_tab',(side*.074,-.044,z),(.026,.012,.025),'rubber',.004)
        transform_objects(PARTS[start:],Matrix.Translation((xx,0,0))@Matrix.Rotation((-.1 if idx==0 else .1),4,'Z'))

def bottle():
    asset('16_water_bottle','Бутылка воды','props')
    profile=[(.006,.003),(.025,.004),(.034,.012),(.035,.02),(.034,.036),(.032,.04),(.035,.046),(.035,.062),(.032,.067),(.035,.073),(.035,.12),(.034,.14),(.031,.16),(.026,.177),(.018,.189),(.014,.195),(.014,.213)]
    shell=lathe('clear_bottle_shell',profile,'plastic_clear',64,True)
    sol=shell.modifiers.new('Thin PET wall','SOLIDIFY');sol.thickness=.00065
    water=lathe('water_volume',[(.027,.009),(.032,.02),(.032,.148)],'water',48,True)
    label=lathe('printed_blue_label',[(.0354,.075),(.0354,.125)],'bottle_label',64,False)
    cyl('cap',(0,0,.215),.016,.018,'blue_plastic',vertices=48,bev=.0015)
    cyl('seal_ring',(0,0,.202),.0158,.003,'blue_plastic',vertices=48)
    for j in range(32):
        a=j*2*pi/32;rod('cap_grip',(cos(a)*.016,sin(a)*.016,.208),(cos(a)*.016,sin(a)*.016,.222),.0007,'blue_plastic',6)
    for j in range(5):
        a=j*2*pi/5;sphere('base_lobe',(.022*cos(a),.022*sin(a),.008),(.012,.012,.008),'plastic_clear',16,8)

def programme():
    asset('17_performance_programme','Программа выступления','props')
    nx,ny=24,18;vv=[];uv=[];faces=[]
    for j in range(ny+1):
        for i in range(nx+1):
            u=i/nx;v=j/ny
            z=.003+.016*(u**8)*((1-v)**6)+.0015*sin(v*pi)
            vv.append(((u-.5)*.24,(v-.5)*.165,z));uv.append((u,v))
    for j in range(ny):
        for i in range(nx):a=j*(nx+1)+i;faces.append((a,a+1,a+nx+2,a+nx+1))
    o=mesh('printed_top_sheet',vv,faces,'programme_print',uv,smooth=True)
    sol=o.modifiers.new('Paper thickness','SOLIDIFY');sol.thickness=.00035
    cube('folded_lower_pages',(0,0,.001),(.238,.163,.0015),'paper',.0003)

def stage_platform(module=False):
    asset('19_stage_deck_module' if module else '18_stage_platform','Соединяемый модуль сцены' if module else 'Сцена целиком','architecture')
    w=2.0 if module else 11.0;d=4.0;h=.72
    cube('navy_solid_fascia',(0,0,h/2),(w,d,h),'navy_paint',.012)
    deck=cube('maple_deck',(0,0,h-.011),(w+.012,d+.012,.044),'wood_maple',.008)
    cube('wood_front_nosing',(0,-d/2-.008,h-.024),(w+.012,.025,.045),'wood_maple',.007)
    for x in [-w/2+.025,w/2-.025]:
        for y in [-d/2+.04,d/2-.04]:screw('deck_fastener',(x,y,h+.013),(0,0,1),.002)

def stairs(right=False):
    asset('21_stage_stairs_right' if right else '20_stage_stairs_left','Правая лестница' if right else 'Левая лестница','architecture')
    w=1.05;dep=.30;rise=.18
    # Four joined rectangular risers, with no gaps under the treads.
    for i in range(4):
        h=(i+1)*rise;y=-.6+dep*(i+.5)
        cube('solid_riser',(0,y,h/2),(w,dep+.001,h),'navy_paint',.003)
        cube('carpet_tread',(0,y,h+.001),(w-.01,dep-.006,.008),'carpet',.002)
        cube('step_nosing',(0,y-dep/2+.005,h+.005),(w,.014,.012),'cream_trim',.002)
    side=1 if right else -1
    for i in range(4):
        h=(i+1)*rise
        cube('outer_cheek',(side*(w/2+.006),-.6+dep*(i+.5),h/2),(.014,dep,h),'navy_paint',.002)

def carpet_tile():
    asset('22_carpet_floor_tile','Модуль коврового пола 2 × 2 м','architecture','Origin on walkable upper surface.')
    cube('thin_floor_slab',(0,0,-.029),(2,2,.05),'navy_paint',.002)
    cube('carpet_top',(0,0,-.002),(2,2,.004),'carpet',.001)

def seating_terrace():
    asset('23_seating_terrace','Три ступенчатых ряда основания','architecture')
    for i in range(3):
        h=(i+1)*.16;y=-1.35+(i+.5)*.9
        cube('solid_terrace',(0,y,h/2),(4,.9+.001,h),'navy_paint',.002)
        cube('carpet_surface',(0,y,h+.002),(4,.9,.008),'carpet',.002)
        cube('edge_strip',(0,y-.447,h+.004),(4,.011,.012),'cream_trim',.002)

def acoustic_wall():
    asset('24_acoustic_wall','Кремовая акустическая стена','architecture')
    cube('wall_slab',(0,.055,1.4),(2.4,.16,2.8),'cream_wall',.004)
    for row in range(4):
        for col in range(4):
            x=-.87+col*.58;z=.50+row*.56
            cube('inset_acoustic_panel',(x,-.031,z),(.563,.012,.543),'cream_trim',.004)
    cube('base_trim',(0,-.038,.055),(2.4,.035,.11),'cream_trim',.003)
    cube('top_trim',(0,-.032,2.75),(2.4,.025,.075),'cream_trim',.003)

def door_module():
    asset('25_recessed_door_wall','Стена с закрытым дверным проёмом','architecture')
    for x in [-.925,.925]:cube('wall_pier',(x,.06,1.4),(.55,.23,2.8),'cream_wall',.006)
    cube('wall_header',(0,.06,2.49),(1.32,.23,.62),'cream_wall',.005)
    cube('closed_door_back',(0,.126,1.075),(1.28,.045,2.15),'cream_trim',.006)
    for x in [-.315,.315]:
        cube('flush_door_leaf',(x,.096,1.075),(.619,.026,2.12),'cream_wall',.005)
        cube('subtle_leaf_inset',(x,.080,1.12),(.55,.008,1.91),'cream_trim',.003)
    for x in [-.666,.666]:cube('door_jamb',(x,-.062,1.105),(.048,.045,2.21),'cream_trim',.006)
    cube('door_head',(0,-.062,2.2),(1.38,.045,.052),'cream_trim',.006)
    cube('threshold',(0,.035,.012),(1.3,.25,.025),'cream_trim',.003)
    for x in [-.042,.042]:
        rod('door_pull',(x,.068,1.0),(x,.068,1.15),.004,'silver')

def drape(key,title,width,height,folds,amplitude,mat,mirror=False):
    asset(key,title,'architecture')
    nx=max(48,int(folds*16));nz=16;vv=[];uv=[];faces=[]
    for j in range(nz+1):
        v=j/nz
        for i in range(nx+1):
            u=i/nx;t=(1-u if mirror else u)*2*pi*folds
            x=(u-.5)*width
            y=amplitude*(sin(t)+.16*sin(2*t+.5))*(.94+.06*(1-v))
            z=v*height+.009*(1-v)*cos(t)
            vv.append((x,y,z));uv.append((u*width,v*height))
    for j in range(nz):
        for i in range(nx):a=j*(nx+1)+i;faces.append((a,a+nx+1,a+nx+2,a+1))
    o=mesh('folded_fabric',vv,faces,mat,uv,smooth=True)
    so=o.modifiers.new('Real cloth thickness','SOLIDIFY');so.thickness=.006
    curve('bottom_hem',[tuple(vv[i]) for i in range(nx+1)],.007,mat,False,2)
    cube('top_hanging_rail',(0,.035,height+.025),(width+.03,.045,.04),'dark_metal',.003)
    for i in range(int(folds)+1):
        x=-width/2+i*width/folds
        torus('hanging_ring',(x,.02,height+.019),.012,.003,'dark_metal',(pi/2,0,0))

def projection_screen():
    asset('30_projection_screen','Проекционный экран','architecture')
    w,h=5.0,2.8
    cube('frame',(0,0,h/2),(w,.06,h),'charcoal',.012)
    quad('blank_projection_surface',[(-w/2+.04,-.032,.04),(w/2-.04,-.032,.04),(w/2-.04,-.032,h-.04),(-w/2+.04,-.032,h-.04)],'screen_blank')
    for x in [-1.8,1.8]:
        cube('mount_tab',(x,.025,h+.035),(.06,.05,.12),'silver',.006)
        cyl('mount_bolt',(x,.025,h+.06),.012,.06,'dark_metal',(0,1,0))

def ceiling_panel():
    asset('31_ceiling_panel','Белый панельный потолок','architecture')
    cube('ceiling_support',(0,0,.055),(3,2.4,.09),'cream_trim',.003)
    for ix in range(5):
        for iy in range(4):
            x=-1.2+ix*.6;y=-.9+iy*.6
            cube('underside_tile',(x,y,.002),(.587,.587,.012),'white_panel',.002)
            cube('upper_tile',(x,y,.104),(.587,.587,.008),'white_panel',.002)

def strip_light():
    asset('32_ceiling_strip_light','Линейный потолочный светильник','architecture')
    cube('light_housing',(0,0,.065),(1.5,.19,.13),'cream_trim',.008)
    cube('recessed_channel',(0,0,-.004),(1.43,.149,.015),'dark_metal',.004)
    cube('light_diffuser',(0,0,-.013),(1.37,.121,.008),'ivory_light',.005)
    for x in [-.71,.71]:
        cube('end_cap',(x,0,.02),(.055,.19,.045),'charcoal',.003)
        screw('housing_screw',(x,0,-.004),(0,0,-1),.004)

def ceiling_soffit():
    asset('33_ceiling_soffit','Потолочная балка с точечным светом','architecture')
    cube('cream_soffit',(0,0,.15),(3.5,.45,.3),'cream_trim',.008)
    for x in [-1.4,-.7,0,.7,1.4]:
        cyl('downlight_recess',(x,0,-.003),.042,.012,'dark_metal',(0,0,1),32)
        cyl('downlight_trim',(x,0,-.01),.037,.009,'silver',(0,0,1),32)
        cyl('downlight_diffuser',(x,0,-.016),.029,.006,'ivory_light',(0,0,1),32)

def build():
    init()
    for f in [chair,folding_chair,lambda:folding_chair(True),musicstand,micstand,desk,mixer,laptop,wall_display,floor_monitor,wall_speaker,stage_light,cable,duffel,kneepads,bottle,programme,stage_platform,lambda:stage_platform(True),stairs,lambda:stairs(True),carpet_tile,seating_terrace,acoustic_wall,door_module]:
        f();print('BUILT',ASSETS[-1]['id'],len(ASSETS[-1]['objects']),flush=True)
    drape('26_curtain_left','Левая синяя кулиса',1.6,3.8,6,.105,'navy_fabric')
    drape('27_curtain_right','Правая синяя кулиса',1.6,3.8,6,.105,'navy_fabric',True)
    drape('28_stage_valance','Верхняя синяя падуга',11,.42,32,.022,'navy_fabric')
    drape('29_black_backdrop','Чёрный задник',10,3.6,25,.075,'black_fabric')
    for f in [projection_screen,ceiling_panel,strip_light,ceiling_soffit]:f()
    bpy.context.view_layer.update()
    print('COMPLETE_GEOMETRY',len(ASSETS),'assets',sum(len(a['objects']) for a in ASSETS),'parts',flush=True)

def bounds(a):
    coords=[]
    for o in a['objects']:
        if o.type in ('MESH','CURVE'):
            coords.extend([o.matrix_world@Vector(v) for v in o.bound_box])
    return Vector([min(v[i] for v in coords) for i in range(3)]),Vector([max(v[i] for v in coords) for i in range(3)])

def export_assets():
    manifest=[]
    for a in ASSETS:
        for o in a['objects']:
            if o.type=='MESH' and 'Export triangulation' not in o.modifiers:
                mod=o.modifiers.new('Export triangulation','TRIANGULATE')
                if hasattr(mod,'keep_custom_normals'):mod.keep_custom_normals=True
        bpy.ops.object.select_all(action='DESELECT')
        for o in a['objects']+[a['root']]:o.select_set(True)
        bpy.context.view_layer.objects.active=a['root']
        fp=MODELS/(a['id']+'.glb')
        bpy.ops.export_scene.gltf(filepath=str(fp),export_format='GLB',use_selection=True,export_apply=True,export_yup=True,export_materials='EXPORT',export_extras=True,export_cameras=False,export_lights=False,export_animations=False,export_image_format='AUTO',export_tangents=True)
        from fix_glb_tangents import fix
        fix(fp)
        lo,hi=bounds(a)
        manifest.append({'id':a['id'],'title_ru':a['title'],'category':a['category'],'file':'models/'+fp.name,'dimensions_m':[round(v,4) for v in hi-lo],'origin':'base centre; floor tile origin on its upper walkable face','notes':a['notes'],'part_count':len(a['objects'])})
        print('EXPORTED',fp.name,fp.stat().st_size,flush=True)
    (ROOT/'asset_manifest.json').write_text(json.dumps({'blender_version':bpy.app.version_string,'units':'metres','asset_count':len(manifest),'assets':manifest},ensure_ascii=False,indent=2),encoding='utf-8')

def setup_render():
    sc=bpy.context.scene;sc.render.engine='CYCLES';sc.cycles.device='CPU';sc.cycles.samples=40
    sc.cycles.use_denoising=True;sc.cycles.max_bounces=6;sc.cycles.transparent_max_bounces=8
    sc.render.resolution_x=768;sc.render.resolution_y=640;sc.render.resolution_percentage=100
    sc.render.image_settings.file_format='PNG';sc.render.image_settings.color_mode='RGBA';sc.render.film_transparent=True
    sc.render.threads_mode='FIXED';sc.render.threads=8
    sc.view_settings.view_transform='AgX';sc.view_settings.look='AgX - Medium High Contrast';sc.view_settings.exposure=0
    world=bpy.data.worlds.new('Neutral studio');world.use_nodes=True;world.node_tree.nodes['Background'].inputs[0].default_value=(.72,.76,.82,1);world.node_tree.nodes['Background'].inputs[1].default_value=.35;sc.world=world
    studio=bpy.data.collections.new('STUDIO_render_only');sc.collection.children.link(studio)
    def light(name,loc,power,size):
        d=bpy.data.lights.new(name,'AREA');d.energy=power;d.shape='DISK';d.size=size;o=bpy.data.objects.new(name,d);studio.objects.link(o);o.location=loc;o.rotation_euler=(-o.location).to_track_quat('-Z','Y').to_euler();return o
    lights=[light('Key',(-3,-4,6),850,5),light('Fill',(4,-2,3.5),420,4),light('Rim',(1,4,5),650,4)]
    cd=bpy.data.cameras.new('Catalog_camera');cam=bpy.data.objects.new('Catalog_camera',cd);studio.objects.link(cam);cd.type='ORTHO';sc.camera=cam
    return cam,lights,studio

def render_asset(a,cam,lights):
    sc=bpy.context.scene
    for b in ASSETS:b['collection'].hide_render=b is not a
    lo,hi=bounds(a);center=(lo+hi)/2;dims=hi-lo
    direction=Vector((4,-6,3.8)).normalized()
    if a['id'] in ('31_ceiling_panel','32_ceiling_strip_light','33_ceiling_soffit'):
        direction=Vector((3,-5,-3.2)).normalized()
    if a['id']=='17_performance_programme':direction=Vector((2,-3,4)).normalized()
    cam.location=center+direction*20;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler()
    rot=cam.rotation_euler.to_matrix().transposed()
    pts=[rot@(Vector((x,y,z))-center) for x in [lo.x,hi.x] for y in [lo.y,hi.y] for z in [lo.z,hi.z]]
    dx=max(v.x for v in pts)-min(v.x for v in pts);dy=max(v.y for v in pts)-min(v.y for v in pts)
    aspect=sc.render.resolution_x/sc.render.resolution_y
    # Blender ortho_scale is image width for landscape sensors.
    cam.data.ortho_scale=max(dx,dy*aspect)*1.18
    scale=max(dims.length*.65,.3)
    for light,rel,power in zip(lights,[(-3,-4,6),(4,-2,3.5),(1,4,5)],[500,240,400]):
        light.location=center+Vector(rel)*scale;light.rotation_euler=(center-light.location).to_track_quat('-Z','Y').to_euler()
        light.data.energy=power*scale*scale;light.data.size=4*scale
    sc.render.filepath=str(RENDERS/(a['id']+'.png'))
    bpy.ops.render.render(write_still=True)
    print('RENDERED',a['id'],flush=True)

def save_master(cam,studio):
    for i,a in enumerate(ASSETS):
        a['collection'].hide_render=False
        a['root'].location=((i%6)*13,-(i//6)*7,0)
    for im in bpy.data.images:
        if im.source=='FILE':im.filepath=bpy.path.relpath(im.filepath,start=str(ROOT));im.pack()
    studio.hide_render=True
    bpy.context.scene['README']='33 individual assets. All dimensions in metres. External GLB models in models/. Packed PNG PBR textures. Collections contain editable named parts.'
    bpy.context.scene.camera=cam
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'Studvesna_Assets.blend'),compress=True)

def main():
    build();export_assets();cam,lights,studio=setup_render()
    # A resumable source scene is saved before the long rendering stage.
    for im in bpy.data.images:
        if im.source=='FILE':im.pack()
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'Studvesna_Working.blend'),compress=True)
    picks=os.getenv('ASSET_RENDER_ONLY','')
    for a in ASSETS:
        if not picks or a['id'].split('_')[0] in picks.split(','):render_asset(a,cam,lights)
    save_master(cam,studio)
    print('ASSET_BUILD_FINISHED',flush=True)

if __name__=='__main__':main()
