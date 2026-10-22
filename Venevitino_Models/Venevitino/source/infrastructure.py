from common import *
from architecture import roof_mesh
def build():
 asset('park_bench','Скамья со спинкой','environment')
 for x in [-.7,.7]:
  for y in [-.2,.2]:box('Foot',(.09,.09,.44),(x,y,.22),'timber',.01)
  box('Support',(.10,.55,.09),(x,0,.4),'darkwood',.01);box('Back_support',(.08,.08,.86),(x,.22,.43),'timber',.01)
 for y in [-.18,0,.18]:box('Seat',(1.8,.16,.055),(0,y,.48),'floor',.008)
 for z in [.68,.83]:box('Back',(1.8,.05,.13),(0,.22,z),'timber',.008)
 consolidate();asset('picnic_table','Стол со скамьями','environment')
 for x in [-.72,.72]:beam('A_frame',(x,-.53,.06),(x,.30,.73),.09,mat='timber');beam('A_frame',(x,.53,.06),(x,-.30,.73),.09,mat='timber');box('Crossmember',(.10,1.55,.08),(x,0,.39),'darkwood',.008)
 for i in range(5):box('Top',(2.1,.145,.045),(0,-.3+i*.15,.765),'floor',.008)
 for y in [-.66,.66]:
  for yy in [-.075,.075]:box('Seat',(2.12,.145,.05),(0,y+yy,.455),'timber',.008)
 beam('Brace',(-.7,0,.5),(.7,0,.68),.07,mat='darkwood');consolidate();asset('canopy','Большой навес 6 × 8 м','environment');box('Slab',(6.4,8.4,.14),(0,0,.07),'concrete',.02)
 for x in [-2.85,2.85]:
  for y in [-3.85,0,3.85]:
   box('Post',(.18,.18,2.9),(x,y,1.59),'timber',.014);box('Shoe',(.22,.22,.16),(x,y,.22),'metal',.008)
   for s in [-1,1]:beam('Brace',(x,y,2.3),(x,y+s*.60,2.98),.10,mat='timber')
  box('Eave',(.19,8.15,.24),(x,0,3.05),'timber',.01)
 for y in [-3.9,0,3.9]:
  beam('Tie',(-2.9,y,3.05),(2.9,y,3.05),.16,mat='timber')
  for x in [-1,1]:beam('Rafter',(0,y,4.3),(x*3.2,y,3.07),.15,mat='timber');beam('Strut',(0,y,3.05),(x*1.6,y,3.67),.1,mat='timber')
  beam('King_post',(0,y,3.05),(0,y,4.3),.11,mat='timber')
 roof_mesh('Canopy_roof',3.28,-4.25,4.25,3.05,4.3)
 for y in [-4.24,4.24]:
  for x in [-1,1]:beam('Fascia',(0,y,4.3),(x*3.28,y,3.05),.13,mat='timber')
 for y in [-3.75,3.75]:
  tube('Festoon',[(-2.85+i*5.7/30,y,2.98-.23*sin(i*pi/30)) for i in range(31)],.009,'rubber')
  for i in range(9):x=-2.65+i*.66;z=2.98-.23*sin((x+2.85)/5.7*pi);rod('Wire',(x,y,z),(x,y,z-.13),.007,'rubber');sphere('Bulb',(.075,.075,.11),(x,y,z-.18),'bulb',12,8);empty('Light_Socket',(x,y,z-.18))
 consolidate('Canopy_structure')
 for y in [-2.3,0,2.3]:clone('picnic_table',(0,y,.14))
 asset('street_lamp','Парковый фонарь','environment');lathe('Pole',[(.14,0),(.14,.06),(.09,.12),(.065,.28),(.052,3.35)],'metal',20);box('Base',(.3,.3,.055),(0,0,3.35),'metal',.01)
 for x in [-.14,.14]:
  for y in [-.14,.14]:beam('Corner',(x,y,3.36),(x*.82,y*.82,3.76),.018,mat='metal')
 for a in [0,1]:
  for s in [-1,1]:box('Glass',(.25,.01,.35) if a==0 else (.01,.25,.35),(0,s*.125,3.56) if a==0 else (s*.125,0,3.56),'glass',0)
 sphere('Bulb',(.10,.10,.18),(0,0,3.55),'bulb');lathe('Roof',[(.24,3.76),(.22,3.79),(.03,3.94),(0,3.98)],'metal',4);consolidate('Lamp_geometry');empty('Light_Socket',(0,0,3.55))
 asset('volleyball_net','Волейбольная сетка','environment')
 for x in [-4.6,4.6]:cyl('Pole',.045,2.65,(x,0,0),'timber',16);box('Foot',(.28,.28,.12),(x,0,.06),'concrete',.015);tube('Guy_rope',[(x,0,2.4),(x*1.12,.9,.02)],.007,'rope',res=1)
 for z in [1.48,2.43]:box('Tape',(9,.014,.045),(0,0,z),'linen',.003)
 for x in [-4.5,4.5]:box('Side_tape',(.045,.014,.95),(x,0,1.955),'linen',.003)
 for i in range(91):rod('Net',(-4.5+i*.1,0,1.5),(-4.5+i*.1,0,2.41),.0025,'rope',5)
 for i in range(10):rod('Net',(-4.5,0,1.5+i*.1),(4.5,0,1.5+i*.1),.0025,'rope',5)
 consolidate();asset('volleyball_court','Площадка 9 × 18 м','environment');box('Sand',(11,20,.08),(0,0,-.05),'sand',0)
 for x in [-4.5,4.5]:box('Line',(.045,18,.002),(x,0,.001),'linen',0)
 for y in [-9,-3,0,3,9]:box('Line',(9,.045,.002),(0,y,.001),'linen',0)
 consolidate();asset('river_dock','Деревянный пирс','environment')
 for x in [-.8,.8]:
  for y in [-2.2,0,2.2]:cyl('Pile',.09,.94,(x,y,-.65),'darkwood',14)
  box('Bearer',(.12,5,.16),(x,0,.17),'timber',.01)
 for i in range(29):box('Deck',(2,.165,.05),(0,-2.44+i*.174,.275),'floor',.006)
 consolidate()
