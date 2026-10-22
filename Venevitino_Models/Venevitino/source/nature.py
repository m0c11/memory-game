from common import *
class Leaves:
 def __init__(self):self.v=[];self.f=[];self.uv=[];self.mi=[]
 def add(self,center,length,width,angle,tilt=0,roll=0,mi=0):
  p=Vector(center);q=Matrix.Rotation(angle,4,'Z')@Matrix.Rotation(tilt,4,'Y')@Matrix.Rotation(roll,4,'X');s=[(0,-.5,0),(-.32,-.26,.015),(-.5,.04,0),(-.29,.31,.008),(0,.5,0),(.29,.31,.008),(.5,.04,0),(.32,-.26,.015),(0,.02,.065)];start=len(self.v);self.v.extend([tuple(p+q@Vector((x*width,y*length,z*length))) for x,y,z in s])
  for i in range(8):
   self.f.append((start+i,start+(i+1)%8,start+8));self.mi.append(mi)
   for k in (i,(i+1)%8,8):self.uv.append((s[k][0]+.5,s[k][1]+.5))
 def finish(self,name,mats=('leaf','leaf_light')):
  o=mesh(name,self.v,self.f,list(mats),uv=False);uv=o.data.uv_layers.new(name='UVMap')
  for d,v in zip(uv.data,self.uv):d.uv=v
  for p,m in zip(o.data.polygons,self.mi):p.material_index=m
  return o
def bush(name,spread,height,count,seed):
 random.seed(seed);asset(name,'Куст '+('широкий' if spread>1.5 else 'компактный'),'nature');leaves=Leaves()
 for i in range(17):
  a=i*2.4;r=random.uniform(.2,.5)*spread;tip=Vector((cos(a)*r,sin(a)*r,random.uniform(.62,1)*height));base=Vector((cos(a)*.07,sin(a)*.07,0));mid=(tip+base)*.5+Vector((0,0,.15));tube('Stem',[base,mid,tip],.009,'bark',res=1)
  for j in range(count//17):
   c=mid.lerp(tip,random.uniform(.36,1));phi=random.uniform(0,2*pi);rr=random.uniform(.04,.27)*spread;p=c+Vector((rr*cos(phi),rr*sin(phi),random.uniform(-.18,.16)))
   if j%4==0:rod('Twig',c,p,.003,'bark',6)
   leaves.add(p,random.uniform(.15,.24),random.uniform(.10,.16),phi,random.uniform(-.8,.8),random.uniform(-.6,.6),j%4==0)
 leaves.finish('Veined_leaves');consolidate('Bush')
def tree(name,kind,height,seed):
 random.seed(seed);asset(name,('Берёза ' if kind=='birch' else 'Лиственное дерево ')+name[-1],'nature');bark='birch' if kind=='birch' else 'bark';lathe('Trunk',[(.22,0),(.17,.3),(.14,height*.42),(.10,height*.65),(.025,height*.96)],bark,14);leaves=Leaves()
 for i in range(14):
  a=i*2.399;z=height*(.43+.034*i);r=(1.4 if kind=='birch' else 2)*(1-i/18*.4);start=Vector((0,0,z));end=Vector((cos(a)*r,sin(a)*r,z+.6));mid=start.lerp(end,.55)+Vector((0,0,.25));tube('Branch',[start,mid,end],.035 if i<6 else .024,bark,res=1)
  for j in range(5):
   phi=a+(j-2)*.4;tip=end+Vector((cos(phi)*.65,sin(phi)*.65,random.uniform(-.35,.50)));b=mid.lerp(end,.35+.12*j);rod('Twig',b,tip,.009,bark,6)
   for k in range(32):
    p=b.lerp(tip,random.uniform(.25,1.15))+Vector((random.uniform(-.32,.32),random.uniform(-.32,.32),random.uniform(-.25,.25)));leaves.add(p,random.uniform(.18,.30),random.uniform(.11,.19),random.uniform(0,2*pi),random.uniform(-1,1),random.uniform(-.7,.7),k%4==0)
 leaves.finish('Canopy')
 for i in range(5):a=i*2*pi/5;beam('Root',(0,0,.16),(.44*cos(a),.44*sin(a),.02),.09,mat=bark)
 consolidate('Tree')
def pine():
 asset('pine_tree','Сосна','nature');lathe('Trunk',[(.21,0),(.15,3),(.09,6),(.015,8.4)],'bark',14);leaves=Leaves()
 for j in range(7):
  z=3.8+j*.63;r=1.6*(1-j/8)
  for k in range(6):
   a=k*pi/3+j*.65;tip=Vector((r*cos(a),r*sin(a),z+.25));rod('Branch',(0,0,z),tip,.022,'bark',8)
   for i in range(100):
    t=random.uniform(.25,1.15);p=tip*t;p.z=z+.18*t+random.uniform(-.15,.19);p.x+=random.uniform(-.32,.32);p.y+=random.uniform(-.32,.32);leaves.add(p,random.uniform(.23,.38),.09,a+random.uniform(-2,2),random.uniform(-.6,.6),0,0)
 leaves.finish('Needles',('pine',));consolidate('Pine')
def grass(name='grass_clump',radius=.38,flowers=False):
 asset(name,'Трава с ромашками' if flowers else 'Пучок травы','nature');vs=[];fs=[]
 for i in range(110):
  a=random.random()*2*pi;r=sqrt(random.random())*radius;x=r*cos(a);y=r*sin(a);h=random.uniform(.12,.36);w=random.uniform(.008,.020);phi=random.random()*2*pi;lean=random.uniform(.03,.13);dx=cos(phi);dy=sin(phi);s=len(vs)
  vs.extend([(x-w*dy,y+w*dx,0),(x+w*dy,y-w*dx,0),(x+w*.35*dy+lean*dx*.3,y-w*.35*dx+lean*dy*.3,h*.55),(x-w*.35*dy+lean*dx*.3,y+w*.35*dx+lean*dy*.3,h*.55),(x+lean*dx,y+lean*dy,h)]);fs.extend([(s,s+1,s+2,s+3),(s+3,s+2,s+4)])
 o=mesh('Blades',vs,fs,['grass','drygrass'])
 for p in o.data.polygons:p.material_index=random.random()<.25
 if flowers:
  petals=Leaves()
  for i in range(12):
   a=random.random()*2*pi;r=random.random()*radius;x=r*cos(a);y=r*sin(a);h=random.uniform(.25,.48);rod('Stem',(x,y,0),(x,y,h),.0025,'grass',5);sphere('Centre',(.025,.025,.014),(x,y,h),'yellow',10,5)
   for k in range(8):phi=k*pi/4;petals.add((x+.02*cos(phi),y+.02*sin(phi),h),.035,.012,phi+pi/2,0,0,0)
  petals.finish('Petals',('white',))
 consolidate()
def reeds():
 asset('reeds_cattails','Камыши','nature');leaves=Leaves()
 for i in range(27):
  x=random.uniform(-.5,.5);y=random.uniform(-.38,.38);h=random.uniform(.8,1.6);rod('Stalk',(x,y,0),(x+.05,y,h),.006,'drygrass',6)
  if i%3==0:lathe('Cattail',[(.016,0),(.027,.03),(.027,.19),(.014,.23)],'darkwood',10,(x+.05,y,h-.05))
  for j in range(3):a=random.random()*2*pi;leaves.add((x+.12*cos(a),y+.12*sin(a),h*(.35+j*.17)),.52,.035,a,.7,.3,0)
 leaves.finish('Leaves',('grass',));consolidate()
def rocks():
 asset('rock_cluster','Камни и галька','nature')
 for i in range(7):
  x=random.uniform(-.55,.55);y=random.uniform(-.4,.4);s=random.uniform(.12,.38);o=sphere('Stone',(s*1.4,s,s*.7),(x,y,s*.24),'stone',10,6)
  for v in o.data.vertices:v.co*=random.uniform(.87,1.1)
 consolidate();asset('fallen_log','Упавшее бревно','nature');rod('Log',(-.95,0,.16),(.95,0,.16),.16,'bark',16)
 for x in [-.956,.956]:
  o=cyl('Cut_end',.14,.004,(x,0,.16),'floor',20);o.rotation_euler.y=pi/2
  for r in [.045,.085,.12]:tube('Growth_ring',[(x-.003,r*cos(a*pi/24),.16+r*sin(a*pi/24)) for a in range(49)],.002,'darkwood',res=1)
 rod('Branch',(.25,0,.2),(.55,.3,.49),.045,'bark',8);consolidate()
def terrain():
 asset('ground_tile_8m','Грунт с травой — 8 м','terrain');box('Ground',(8,8,.12),(0,0,-.08),'grass',0);consolidate();asset('path_straight_4m','Прямая дорожка — 4 м','terrain');vs=[];fs=[]
 for j in range(17):y=-2+j*.25;vs.extend([(-1+random.uniform(-.08,.08),y,.003),(1+random.uniform(-.08,.08),y,.003)])
 for j in range(16):fs.append((j*2,j*2+1,j*2+3,j*2+2))
 mesh('Dirt',vs,fs,'soil')
 for i in range(35):x=random.uniform(-.92,.92);y=random.uniform(-1.95,1.95);r=random.uniform(.018,.05);sphere('Pebble',(r*2,r*1.3,r*.8),(x,y,r*.2),'stone',8,4)
 consolidate();asset('path_curve_90','Поворот дорожки — 90°','terrain');vs=[];fs=[]
 for j in range(25):
  a=j*pi/48
  for r in [2,4]:vs.append((r*cos(a),r*sin(a),.003))
 for j in range(24):fs.append((j*2,j*2+1,j*2+3,j*2+2))
 mesh('Dirt',vs,fs,'soil');consolidate();asset('river_water_12m','Вода — 12 × 12 м','terrain');vs=[];fs=[]
 for j in range(33):
  for i in range(33):x=-6+i*12/32;y=-6+j*12/32;vs.append((x,y,.012*sin(x*1.3+y*2)+.005*cos(y*4)))
 for j in range(32):
  for i in range(32):a=j*33+i;fs.append((a,a+1,a+34,a+33))
 mesh('Water',vs,fs,'water',smooth=True);consolidate();asset('river_bank_6m','Берег — секция 6 м','terrain');mesh('Bank',[(-3,-1,0),(3,-1,0),(-3,0,-.12),(3,0,-.12),(-3,1.8,-.48),(3,1.8,-.48)],[(0,1,3,2),(2,3,5,4)],'sand');consolidate()
def build():
 bush('bush_large',1.8,1.3,850,71);bush('bush_small',1.15,.85,510,72);tree('birch_a','birch',7.1,19);tree('birch_b','birch',6.1,22);tree('broadleaf_tree','oak',7.2,25);pine();grass();grass('wildflowers',.42,True);reeds();rocks();terrain()
