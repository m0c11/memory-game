import common as C
from common import *
def wall(name,label,L,openings,loc,angle):
 asset(name,label,'architecture');xs=sorted(set([-L/2,L/2]+[v for c,w,b,h in openings for v in (c-w/2,c+w/2)]));zs=sorted(set([.5,3.2]+[v for c,w,b,h in openings for v in (b,b+h)]))
 for a,d in zip(xs,xs[1:]):
  for b,e in zip(zs,zs[1:]):
   x=(a+d)/2;z=(b+e)/2
   if not any(abs(x-c)<w/2-1e-5 and bot<z<bot+h for c,w,bot,h in openings):box('Solid_wall_return',(d-a,.18,e-b),(x,0,z),'interior',.003)
 for row in range(16):
  z=.5+(row+.5)*2.7/16;segments=[(-L/2,L/2)]
  for c,w,b,h in openings:
   if b-.01<z<b+h+.01:
    new=[]
    for a,d in segments:
     if a<c-w/2:new.append((a,min(d,c-w/2)))
     if d>c+w/2:new.append((max(a,c+w/2),d))
    segments=[s for s in new if s[1]-s[0]>.001]
  for a,d in segments:box('Lap_board',(d-a,.035,2.7/16-.009),((a+d)/2,-.106,z),'siding',.006)
 for i in range(int(L/.18)+1):
  x=-L/2+.09+i*.18
  for a,b in zip(zs,zs[1:]):
   if not any(abs(x-c)<w/2+.015 and bot<(a+b)/2<bot+h for c,w,bot,h in openings):box('Interior_joint',(.006,.009,b-a),(x,.095,(a+b)/2),'trim',.001)
 for z in [.56,3.14]:
  seg=[(-L/2,L/2)]
  if z<.7:
   for c,w,b,h in openings:
    if b<.7:seg=[(-L/2,c-w/2),(c+w/2,L/2)]
  for a,b in seg:box('Skirting',(b-a,.035,.12),((a+b)/2,.115,z),'trim',.005)
 consolidate('Wall_with_openings');r=C.ROOT;r.location=loc;r.rotation_euler.z=angle
 for o in list(r.children):o.data.transform(world_matrix(r))
 r.location=(0,0,0);r.rotation_euler=(0,0,0)
def window():
 asset('window_120x140','Окно 1,2 × 1,4 м','architecture')
 for x in [-.57,.57]:box('Jamb',(.06,.2,1.4),(x,0,.7),'trim',.008)
 for z in [.035,1.365]:box('Jamb',(1.2,.2,.07),(0,0,z),'trim',.008)
 for x in [-.65,.65]:box('Architrave',(.12,.04,1.52),(x,-.125,.7),'trim',.007)
 for z in [-.045,1.45]:box('Architrave',(1.42,.05,.12),(0,-.135,z),'trim',.007)
 box('Sill',(1.45,.34,.075),(0,-.07,-.02),'trim',.008)
 for x in [-.28,.28]:
  for xx in [x-.255,x+.255]:box('Sash_stile',(.036,.055,1.25),(xx,-.035,.7),'trim',.004)
  for z in [.095,1.305]:box('Sash_rail',(.54,.055,.04),(x,-.035,z),'trim',.004)
  box('Glass',(.48,.009,1.15),(x,-.028,.7),'glass',0);rod('Handle',(x+.16,-.095,.65),(x+.16,-.095,.76),.012,'metal')
 box('Mullion',(.055,.07,1.3),(0,-.055,.7),'trim',.004);consolidate('Window_frame_glazing')
 vs=[];fs=[]
 for side in [-1,1]:
  start=len(vs)
  for j in range(9):
   for i in range(17):vs.append((side*(.32+i*.014),.16+.027*sin(i*pi/2),.06+j*.16+.006*cos(i*pi/2)))
  for j in range(8):
   for i in range(16):a=start+j*17+i;fs.append((a,a+1,a+18,a+17))
 mesh('Curtains',vs,fs,'linen');rod('Curtain_rod',(-.65,.16,1.43),(.65,.16,1.43),.012,'metal')
def door():
 asset('door_hinged_090x210','Дверь с петлями 0,9 × 2,1 м','architecture')
 for x in [-.035,.935]:box('Jamb',(.07,.21,2.16),(x,0,1.08),'darkwood',.008)
 box('Header',(1.04,.21,.08),(.45,0,2.14),'darkwood',.008)
 for x in [-.09,.99]:box('Casing',(.12,.04,2.26),(x,-.13,1.1),'timber',.008)
 box('Casing_header',(1.2,.04,.12),(.45,-.13,2.21),'timber',.007);consolidate('Door_frame');hinge=empty('Door_Hinge');hinge['pivot_axis']='Godot Y';hinge['open_degrees']=100;made=[]
 def b(n,s,l,m='darkwood',be=.008):o=box(n,s,l,m,be,parent=hinge);made.append(o);return o
 b('Leaf',(.88,.055,2.075),(.45,0,1.047))
 for x in [.075,.825]:b('Stile',(.12,.085,2.07),(x,-.018,1.045),'timber')
 for z,h in [(.09,.16),(.75,.14),(2,.15)]:b('Rail',(.66,.085,h),(.45,-.018,z),'timber')
 for z,h in [(.4,.43),(1.37,1.05)]:
  b('Raised_panel',(.58,.075,h),(.45,-.03,z),'siding')
  for x in [.145,.755]:b('Moulding',(.025,.10,h+.02),(x,-.018,z),'timber',.004)
  for zz in [z-h/2,z+h/2]:b('Moulding',(.62,.10,.026),(.45,-.018,zz),'timber',.004)
 for yy in [-.063,.063]:
  b('Lock_plate',(.04,.012,.22),(.765,yy,1.01),'metal');made.append(rod('Lever',(.765,yy*1.5,1.055),(.66,yy*1.5,1.055),.012,'metal',parent=hinge));made.append(rod('Spindle',(.765,yy,1.055),(.765,yy*1.55,1.055),.014,'metal',parent=hinge))
 for z in [.25,1.05,1.85]:made.append(cyl('Hinge_barrel',.018,.12,(0,0,z),'metal',16,parent=hinge));b('Hinge_leaf',(.055,.01,.10),(.025,-.035,z+.06),'metal',.001)
 merge('Door_Leaf',made,hinge)
def floor_ceiling():
 asset('cabin_floor','Пол и цоколь 5 × 7 м','architecture');box('Foundation',(5,7,.44),(0,0,.22),'concrete',.025);box('Subfloor',(4.82,6.82,.07),(0,0,.445),'darkwood',.005)
 for i in range(28):
  for j in range(4):box('Floorboard',(.169,1.696,.035),(-2.4+(i+.5)*4.8/28,-3.4+(j+.5)*1.7,.4825),'floor',.003)
 consolidate('Floor');asset('cabin_ceiling','Потолок отдельный','architecture')
 for i in range(28):box('Ceiling_board',(.178,7,.055),(-2.5+(i+.5)*5/28,0,3.175),'interior',.003)
 for y in [-3.3,0,3.3]:box('Ceiling_beam',(4.8,.12,.16),(0,y,3.075),'trim',.01)
 consolidate('Ceiling')
def roof_mesh(name,half,front,back,eave,ridge):
 vs=[];fs=[];ny=max(20,int((back-front)/.22)*4);nx=28
 for side in [-1,1]:
  start=len(vs)
  for i in range(nx+1):
   t=i/nx;x=side*t*half;z=ridge+(eave-ridge)*t;step=.03*((t*7)%1)
   for j in range(ny+1):y=front+(back-front)*j/ny;vs.append((x,y,z+.019*(.5+.5*cos((y-front)*2*pi/.22))+step))
  for i in range(nx):
   for j in range(ny):a=start+i*(ny+1)+j;f=(a,a+1,a+ny+2,a+ny+1);fs.append(f if side==-1 else tuple(reversed(f)))
 o=mesh(name,vs,fs,'roof');solidify_z(o,.025);return o
def roof():
 asset('cabin_roof','Двускатная кровля с фронтонами','architecture');roof_mesh('Metal_roof',2.78,-5.14,3.78,3.16,4.48)
 for y in [-5.12,3.76]:
  for side in [-1,1]:beam('Bargeboard',(0,y,4.46),(side*2.78,y,3.14),.14,.12,'timber');beam('Metal_edge',(0,y-.015,4.51),(side*2.78,y-.015,3.19),.045,.14,'roof')
 for x in [-2.74,2.74]:box('Fascia',(.12,8.94,.18),(x,-.68,3.12),'timber',.01)
 mesh('Ridge_cap',[(-.12,-5.18,4.44),(0,-5.18,4.55),(.12,-5.18,4.44),(-.12,3.82,4.44),(0,3.82,4.55),(.12,3.82,4.44)],[(0,3,4,1),(1,4,5,2)],'roof')
 for y in [-4.86,-3.5,3.5]:
  mesh('Gable_backing',[(-2.5,y-.04,3.2),(2.5,y-.04,3.2),(0,y-.04,4.43),(-2.5,y+.04,3.2),(2.5,y+.04,3.2),(0,y+.04,4.43)],[(0,1,2),(3,5,4),(0,3,4,1),(1,4,5,2),(2,5,3,0)],'darkwood')
  for row in range(7):
   z=3.2+(row+.5)*.17;w=5*(4.43-z)/1.23
   if w>0:box('Gable_siding',(w,.14,.161),(0,y,z),'siding',.004)
 y=-4.98;beam('Truss_tie',(-2.48,y,3.15),(2.48,y,3.15),.14,mat='timber');beam('King_post',(0,y,3.2),(0,y,4.4),.12,mat='timber')
 for x in [-1.3,1.3]:beam('Truss_brace',(0,y,3.2),(x,y,3.79),.10,mat='timber')
 consolidate('Roof_and_gables')
def porch():
 asset('cabin_porch','Крыльцо, перила и ступени','architecture')
 for x in [-2.35,-.75,.75,2.35]:
  for y in [-4.85,-3.65]:box('Pier',(.20,.20,.44),(x,y,.22),'concrete',.012)
 for y in [-4.92,-3.60]:box('Rim',(5,.16,.24),(0,y,.34),'timber',.008)
 for i in range(9):box('Deck_board',(5,.16,.055),(0,-4.94+i*.17,.4725),'floor',.006)
 for x in [-2.36,-.69,.69,2.36]:box('Post',(.15,.15,2.7),(x,-4.92,1.85),'timber',.012);box('Base',(.20,.20,.08),(x,-4.92,.55),'darkwood',.008)
 for a,b in [(-2.36,-.69),(.69,2.36)]:
  for z in [.68,1.39]:box('Front_rail',(b-a,.09,.085),((a+b)/2,-4.92,z),'timber',.008)
  for i in range(8):box('Baluster',(.05,.05,.65),(a+(i+1)*(b-a)/9,-4.92,1.03),'timber',.004)
 for x in [-2.36,2.36]:
  for z in [.68,1.39]:box('Side_rail',(.09,1.42,.085),(x,-4.21,z),'timber',.008)
  for i in range(7):box('Baluster',(.05,.05,.65),(x,-4.82+i*.18,1.03),'timber',.003)
 for j in range(3):
  z=(j+1)*.5/3;y=-5.72+j*.29
  for x in [-.66,.66]:box('Step_support',(.12,.32,z-.05),(x,y,(z-.05)/2),'darkwood',.006)
  box('Step_tread',(1.52,.34,.055),(0,y,z-.0275),'floor',.008)
 consolidate('Porch')
def gutters():
 asset('cabin_gutters','Водостоки и трубы','architecture')
 for x in [-2.83,2.83]:
  vs=[];fs=[]
  for y in [-5.12,3.8]:
   for j in range(13):a=pi+j*pi/12;vs.append((x+.075*cos(a),y,3.1+.075*sin(a)))
  for i in range(12):fs.append((i,i+1,i+14,i+13))
  o=mesh('Gutter',vs,fs,'metal');solidify_z(o,.003)
  for y in [-4.9,3.45]:
   xx=2.54 if x>0 else -2.54;tube('Downpipe',[(x,y,3.05),(x,y,2.87),(xx,y,2.67),(xx,y,.18),(xx*1.03,y-.12,.10)],.043,'metal')
   for z in [.55,1.65,2.65]:box('Pipe_clip',(.12,.11,.025),(xx,y,z),'metal',.002)
 consolidate('Gutters')
def walllamp():
 asset('wall_lamp','Настенный светильник','architecture');box('Mount',(.14,.035,.24),(0,.015,.13),'metal',.015);rod('Bracket',(0,0,.2),(0,-.16,.2),.025,'metal');sphere('Globe',(.16,.16,.23),(0,-.17,.12),'bulb');cyl('Cap',.1,.025,(0,-.17,.23),'metal',24)
 for x in [-.06,.06]:rod('Cage',(x,-.22,.03),(x,-.22,.23),.006,'metal')
 consolidate('Lamp_no_light');empty('Light_Socket',(0,-.18,.12))
def build():
 front=[(-1.52,1.2,1.35,1.4),(0,.94,.5,2.14),(1.52,1.2,1.35,1.4)];back=[(-1.3,1.2,1.35,1.4),(1.3,1.2,1.35,1.4)];sides=[(-1.9,1.2,1.35,1.4),(1.15,1.2,1.35,1.4)]
 wall('cabin_wall_front','Передняя стена',5,front,(0,-3.41,0),0);wall('cabin_wall_back','Задняя стена',5,back,(0,3.41,0),pi);wall('cabin_wall_left','Левая стена',6.64,sides,(-2.41,0,0),-pi/2);wall('cabin_wall_right','Правая стена',6.64,sides,(2.41,0,0),pi/2)
 window();door();floor_ceiling();roof();porch();gutters();walllamp()
 asset('wall_section','Стеновая секция 1 × 2,7 м','architecture');box('Core',(1,.18,2.7),(0,0,1.35),'interior',.005)
 for i in range(16):box('Lap_board',(1,.03,.16),(0,-.105,(i+.5)*2.7/16),'siding',.005)
 consolidate();asset('cabin_shell','Домик без мебели','assembly')
 for key in ['cabin_wall_front','cabin_wall_back','cabin_wall_left','cabin_wall_right','cabin_floor','cabin_ceiling','cabin_roof','cabin_porch','cabin_gutters']:clone(key)
 for x in [-2.49,2.49]:
  for y in [-3.49,3.49]:box('Corner_trim',(.14,.14,2.7),(x,y,1.85),'timber',.008)
 for x in [-1.52,1.52]:clone('window_120x140',(x,-3.41,1.35))
 for x in [-1.3,1.3]:clone('window_120x140',(x,3.41,1.35),pi)
 for x,angle in [(-2.41,-pi/2),(2.41,pi/2)]:
  for lx in [-1.9,1.15]:clone('window_120x140',(x,lx if x>0 else -lx,1.35),angle)
 clone('door_hinged_090x210',(-.45,-3.41,.5));clone('wall_lamp',(.51,-3.56,2.79))
 for a,b in [(.7,.95),(1.85,2.32)]:box('Partition',(b-a,.10,2.15),((a+b)/2,1.5,1.575),'interior',.006)
 box('Partition_header',(1.62,.10,.55),(1.51,1.5,2.925),'interior',.006);box('Partition_side',(.10,1.82,2.64),(.70,2.41,1.82),'interior',.006);clone('door_hinged_090x210',(.95,1.5,.5));empty('Interior_Light_Socket',(0,0,2.9))
 C.ROOT['footprint_m']='5 x 7; porch +1.5';C.ROOT['wall_height_m']=2.7
