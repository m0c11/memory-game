import common as C
from common import *
def bed(name,blanket):
 asset(name,'Кровать — '+('зелёный текстиль' if blanket.endswith('green') else 'синий текстиль'),'furniture')
 for x in [-.45,.45]:
  for y in [-.98,.98]:box('Post',(.085,.085,.85 if y>.5 else .55),(x,y,.425 if y>.5 else .275),'darkwood',.012)
  box('Side_rail',(.075,1.96,.16),(x,0,.33),'timber',.012)
 for y,h in [(1,.55),(-1,.25)]:
  box('End_rail',(.94,.055,.12),(0,y,h),'timber',.008)
  if y>0:
   for z in [.58,.76]:box('Headboard',(.9,.05,.12),(0,y,z),'timber',.008)
 for j in range(13):box('Slat',(.88,.09,.025),(0,-.88+j*.145,.365),'timber',.003)
 box('Mattress',(.88,1.95,.18),(0,0,.465),'linen',.055);vs=[];fs=[];nx=24;ny=30
 for j in range(ny+1):
  y=-1.02+j*1.46/ny
  for i in range(nx+1):
   x=-.54+i*1.08/nx;z=.57-max(0,abs(x)-.40)*1.7+.008*sin(i*1.3+j*.6)+.006*sin(y*32)
   if y<-.90:z-=(-.90-y)*.8
   vs.append((x,y,z))
 for j in range(ny):
  for i in range(nx):a=j*(nx+1)+i;fs.append((a,a+1,a+nx+2,a+nx+1))
 o=mesh('Draped_blanket',vs,fs,blanket,smooth=True);solidify_z(o,.008);box('Folded_sheet',(.86,.17,.045),(0,.43,.578),'linen',.016);sphere('Pillow',(.65,.39,.17),(0,.73,.61),'linen',32,16)
 for x in [-.31,.31]:tube('Seam',[(x,.56,.61),(x,.73,.64),(x,.90,.61)],.002,'trim')
 consolidate('Bed_frame_and_textile')
def nightstand():
 asset('nightstand','Прикроватная тумбочка','furniture')
 for x in [-.22,.22]:
  for y in [-.18,.18]:box('Leg',(.045,.045,.18),(x,y,.09),'darkwood',.004)
 box('Cabinet',(.48,.41,.40),(0,0,.35),'timber',.01);box('Top',(.54,.47,.045),(0,0,.572),'floor',.008)
 for z in [.26,.45]:box('Drawer',(.425,.03,.17),(0,-.221,z),'siding',.008);rod('Pull',(-.06,-.255,z),(.06,-.255,z),.009,'metal')
 consolidate()
def wardrobe():
 asset('wardrobe','Двухстворчатый шкаф','furniture');box('Back',(1.15,.025,1.86),(0,.275,1.03),'darkwood',.004)
 for x in [-.55,.55]:box('Side',(.05,.58,1.92),(x,0,1.04),'timber',.009)
 for z in [.11,.38,1.98]:box('Shelf',(1.1,.56,.045),(0,0,z),'timber',.006)
 for x in [-.48,.48]:
  for y in [-.20,.20]:box('Foot',(.08,.08,.12),(x,y,.06),'darkwood',.008)
 box('Cornice',(1.22,.63,.07),(0,0,2.04),'floor',.01)
 for side in [-1,1]:
  hinge=empty('Wardrobe_Hinge_L' if side==-1 else 'Wardrobe_Hinge_R',(side*.525,-.3,.15));x=-side*.26;made=[box('Door',(.52,.045,1.8),(x,0,.9),'siding',.008,parent=hinge)]
  for z in [.50,1.35]:made.append(box('Panel',(.40,.02,.65),(x,-.03,z),'timber',.01,parent=hinge))
  made.append(rod('Handle',(x-side*.16,-.075,.85),(x-side*.16,-.075,1.02),.011,'metal',parent=hinge));merge('Wardrobe_door_leaf',made,hinge)
 consolidate('Wardrobe_body')
def table_chair():
 asset('room_table','Комнатный стол','furniture')
 for x in [-.54,.54]:
  for y in [-.28,.28]:box('Leg',(.065,.065,.72),(x,y,.36),'timber',.008)
 for y in [-.29,.29]:box('Apron',(1.16,.045,.12),(0,y,.64),'darkwood',.008)
 for i in range(4):box('Tabletop',(1.3,.173,.045),(0,-.27+i*.18,.755),'floor',.006)
 consolidate();asset('chair','Деревянный стул','furniture')
 for x in [-.19,.19]:
  for y in [-.18,.18]:h=.86 if y>0 else .44;box('Leg',(.045,.045,h),(x,y,h/2),'timber',.006)
 box('Seat',(.45,.44,.04),(0,0,.46),'floor',.012)
 for z in [.68,.81]:box('Back_slat',(.42,.035,.09),(0,.18,z),'timber',.006)
 for x in [-.19,.19]:box('Stretcher',(.032,.37,.032),(x,0,.19),'darkwood',.004)
 consolidate()
def bags():
 asset('backpack','Рюкзак с лямками и карманом','personal');box('Body',(.32,.19,.44),(0,0,.24),'canvas',.065);box('Pocket',(.26,.075,.20),(0,-.118,.16),'canvas',.035)
 for x in [-.105,.105]:tube('Strap',[(x,.08,.42),(x,.16,.34),(x,.17,.13),(x,.09,.05)],.021,'navy');box('Buckle',(.04,.012,.04),(x,.17,.14),'metal',.005)
 tube('Grab_loop',[(-.055,0,.44),(-.045,0,.51),(.045,0,.51),(.055,0,.44)],.013,'navy');tube('Zip',[(-.15,-.04,.27),(-.15,-.04,.4),(-.1,-.04,.455),(.1,-.04,.455),(.15,-.04,.4),(.15,-.04,.27)],.004,'metal');tube('Pocket_zip',[(-.1,-.16,.255),(.1,-.16,.255)],.004,'steel');box('Label',(.055,.004,.03),(0,-.159,.12),'label',.001);consolidate()
 asset('duffel_bag','Дорожная сумка','personal');box('Body',(.58,.28,.27),(0,0,.155),'canvas_red',.075)
 for x in [-.16,.16]:tube('Webbing',[(x,-.14,.07),(x,-.14,.23),(x,-.07,.31),(x,.07,.31),(x,.14,.23),(x,.14,.07)],.014,'navy');tube('Handle',[(x,-.06,.30),(x,-.035,.45),(x,.035,.45),(x,.06,.30)],.012,'leather')
 tube('Zip',[(-.24,0,.295),(.24,0,.295)],.004,'steel')
 for x in [-.29,.29]:box('End_pocket',(.024,.2,.19),(x,0,.17),'canvas_red',.025)
 consolidate();asset('suitcase','Чемодан','personal');box('Case',(.40,.22,.58),(0,0,.34),'navy',.05)
 for x in [-.16,.16]:sphere('Wheel',(.07,.07,.07),(x,0,.045),'rubber')
 tube('Handle',[(-.07,0,.63),(-.07,0,.82),(.07,0,.82),(.07,0,.63)],.012,'metal')
 for x in [-.13,-.045,.045,.13]:box('Rib',(.018,.017,.42),(x,-.117,.34),'rubber',.007)
 consolidate()
def personal():
 asset('books_notebook','Книги и тетрадь','personal')
 for i,(x,y,a,m) in enumerate([(0,0,0,'canvas_red'),(.01,.01,.09,'navy'),(-.02,0,-.05,'canvas')]):
  p=empty('Book',(x,y,i*.045));p.rotation_euler.z=a;box('Pages',(.18,.255,.032),(0,0,.02),'paper',.002,parent=p)
  for z in [.002,.04]:box('Cover',(.19,.27,.006),(0,0,z),m,.002,parent=p)
  box('Spine',(.009,.27,.04),(-.093,0,.02),m,.002,parent=p)
 rod('Pencil',(-.08,-.07,.135),(.11,.03,.135),.004,'yellow',8);consolidate()
 asset('enamel_mug','Эмалированная кружка','personal');lathe('Cup',[(.035,0),(.042,.01),(.043,.095),(.039,.095),(.037,.013)],'ceramic',32);tube('Rim',[(.042*cos(t*2*pi/40),.042*sin(t*2*pi/40),.095) for t in range(41)],.003,'metal');tube('Handle',[(.041+.031*sin(t*pi/20),0,.052+.031*cos(t*pi/20)) for t in range(21)],.007,'ceramic');consolidate()
 asset('glass_bottle','Стеклянная бутылка','personal');lathe('Glass',[(.025,.005),(.035,.018),(.035,.18),(.03,.205),(.014,.25),(.013,.31),(.015,.315),(.015,.326),(.010,.326),(.009,.265)],'bottle',32);lathe('Label',[(.0355,.063),(.0355,.155)],'label',32);lathe('Neck_band',[(.0145,.283),(.0145,.306)],'canvas_red',32);lathe('Cap',[(.015,.326),(.015,.336)],'metal',24);lathe('Stripe',[(.0357,.085),(.0357,.097)],'canvas_red',32);consolidate()
 asset('hand_flashlight','Ручной фонарик','personal');p=empty('Torch_body');p.rotation_euler.x=pi/2;lathe('Body',[(.026,0),(.03,.018),(.03,.17),(.045,.205),(.054,.23),(.054,.27),(.048,.278)],'metal',32,parent=p)
 for z in [.045,.065,.085,.105,.125,.145]:lathe('Grip',[(.031,z),(.031,z+.005)],'rubber',32,parent=p)
 lathe('Bezel',[(.054,.264),(.054,.282),(.045,.282)],'rubber',32,parent=p);lathe('Reflector',[(.044,.277),(.008,.244)],'steel',32,parent=p);cyl('Lens',.043,.003,(0,0,.281),'glass',32,parent=p);box('Switch',(.02,.008,.04),(0,-.033,.17),'rubber',.006,parent=p);empty('SpotLight_Socket',(0,-.29,0));consolidate()
 asset('folded_towel','Сложенное полотенце','personal')
 for z in [.018,.046,.071]:box('Fold',(.38,.24,.026),(0,0,z),'linen',.012)
 for x in [-.14,-.12]:box('Stripe',(.008,.23,.001),(x,0,.085),'blanket_blue',0)
 consolidate();asset('sneakers_pair','Пара кроссовок','personal')
 for x in [-.075,.075]:
  sphere('Sole',(.10,.27,.035),(x,-.015,.018),'white');sphere('Upper',(.095,.24,.095),(x,0,.065),'navy')
  for y in [-.015,.014,.043]:rod('Lace',(x-.028,y,.11),(x+.028,y,.11),.003,'linen',6)
  sphere('Collar',(.068,.068,.025),(x,.065,.115),'rubber')
 consolidate();asset('phone','Телефон','personal');box('Case',(.073,.147,.009),(0,0,.006),'rubber',.009);box('Screen',(.066,.135,.001),(0,-.002,.011),'glass',.003);sphere('Camera',(.008,.008,.002),(.02,.053,.012),'metal');consolidate()
 asset('coat_hook','Куртка на крючке','personal');rod('Hook',(0,0,1.42),(0,-.08,1.42),.012,'metal');box('Jacket',(.42,.14,.55),(0,-.08,1.1),'navy',.08)
 for x in [-1,1]:o=box('Sleeve',(.14,.13,.49),(x*.25,-.07,1.07),'navy',.05);o.rotation_euler.y=x*.17
 tube('Zip',[(0,-.16,.84),(0,-.16,1.33)],.005,'steel');sphere('Collar',(.20,.15,.14),(0,-.08,1.37),'navy');consolidate()
 asset('potted_plant','Комнатное растение','personal');lathe('Pot',[(.07,0),(.10,.16),(.105,.17),(.093,.17),(.09,.148)],'canvas_red',24);cyl('Soil',.09,.01,(0,0,.15),'soil')
 for i in range(9):
  a=i*2.4;h=.27+random.random()*.18;rod('Stem',(0,0,.16),(.1*cos(a),.1*sin(a),h),.004,'grass',6)
  for k in range(2):o=sphere('Leaf',(.13,.045,.013),(.11*cos(a),.11*sin(a),h-.055*k),'leaf',12,6);o.rotation_euler=(.2,.3,a)
 consolidate()
def build():
 bed('bed_green','blanket_green');bed('bed_blue','blanket_blue');nightstand();wardrobe();table_chair();bags();personal();asset('cabin_furnished','Домик с мебелью','assembly');clone('cabin_shell')
 for x,key in [(-1.7,'bed_green'),(.05,'bed_blue')]:clone(key,(x,.1,.5))
 for x in [-1,.58]:clone('nightstand',(x,.8,.5))
 for key,loc,angle in [('wardrobe',(-.1,3.04,.5),0),('room_table',(-1.45,-2.20,.5),0),('chair',(-1.44,-1.4,.5),pi),('backpack',(-.75,.9,.5),-.2),('duffel_bag',(.15,-1.35,.5),.15),('suitcase',(1.5,2.7,.5),0),('books_notebook',(-1.7,-2.2,1.28),0),('enamel_mug',(-1.12,-2.38,1.28),0),('potted_plant',(-1.52,-3.28,1.35),0),('folded_towel',(1,2.75,.5),0),('sneakers_pair',(.88,-2.55,.5),-.1),('coat_hook',(1,-3.28,.5),pi),('phone',(-.98,.78,1.1),0)]:clone(key,loc,angle)
