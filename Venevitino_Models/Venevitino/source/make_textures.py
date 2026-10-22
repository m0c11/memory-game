from pathlib import Path
import numpy as np
from PIL import Image
OUT=Path(__file__).resolve().parents[1]/'textures';OUT.mkdir(exist_ok=True)
rng=np.random.default_rng(19820)
def noise(n,k):return np.asarray(Image.fromarray(np.uint8(rng.random((k,k))*255)).resize((n,n),Image.Resampling.BICUBIC)).astype(float)/255-.5
def make(name,col,mode='plain',rough=.8,metal=0,n=512):
 y,x=np.mgrid[0:n,0:n]/n;g=noise(n,32)*.055+noise(n,128)*.025;h=noise(n,64)*.05
 if mode=='wood':
  w=.009*np.sin(x*19)+.004*np.sin(x*47)+noise(n,8)*.012;g+=.045*np.sin((y+w)*850)+.024*np.sin((y+w)*1700)+noise(n,16)*.055;h=.10*np.sin((y+w)*850)+.03*np.sin((y+w)*1700)
 elif mode=='cloth':g+=.035*np.sin(x*n*np.pi)+.035*np.sin(y*n*np.pi);h=.06*np.cos(x*n*np.pi)+.06*np.cos(y*n*np.pi)
 elif mode in ('stone','soil','grass'):
  g+=noise(n,10)*.16+noise(n,128)*.15;h=noise(n,128)*.2+noise(n,16)*.12
 elif mode=='bark':g+=.1*np.sin(x*110+noise(n,12)*6)+noise(n,64)*.12;h=.15*np.sin(x*110+noise(n,12)*6)
 elif mode=='birch':
  for _ in range(160):
   xx,yy=rng.random(2);w=rng.uniform(.015,.10);hh=rng.uniform(.003,.014);mask=((x-xx)/w)**2+((y-yy)/hh)**2<1;g[mask]-=rng.uniform(.55,.9);h[mask]-=.08
 elif mode=='leaf':
  mid=np.exp(-((x-.5)/.012)**2);vein=np.exp(-(np.sin((y-np.abs(x-.5)*.65)*np.pi*11)/.13)**2);g+=mid*.08+vein*.02;h=mid*.09+vein*.025
 elif mode=='water':h=.06*np.sin(y*100+np.sin(x*16)*2)+.03*np.sin(y*250+x*40)
 rgb=np.uint8(np.clip(np.array(col)[None,None,:]*(1+g[:,:,None]),0,255))
 gy,gx=np.gradient(h);v=np.stack((-gx*8,-gy*8,np.ones_like(h)),axis=-1);v/=np.linalg.norm(v,axis=-1)[:,:,None]
 normal=np.uint8(np.clip((v*.5+.5)*255,0,255));orm=np.zeros((n,n,3),dtype='uint8');orm[:,:,0]=255;orm[:,:,1]=np.uint8(np.clip((rough+g*.1)*255,0,255));orm[:,:,2]=int(metal*255)
 for suffix,data in [('basecolor',rgb),('normal',normal),('orm',orm)]:
  target=OUT/f'{name}_{suffix}.png';temp=target.with_suffix('.tmp');Image.fromarray(data).save(temp,format='PNG',optimize=True);temp.replace(target)
spec=[('siding',(104,75,54),'wood',.78,0,1024),('timber',(133,98,65),'wood',.77,0,1024),('darkwood',(76,54,37),'wood',.77,0,512),('trim',(190,176,151),'wood',.75,0,512),('interior',(194,184,162),'wood',.82,0,1024),('floor',(160,122,78),'wood',.7,0,1024),('roof',(37,40,45),'plain',.68,.18,512),('concrete',(119,119,110),'stone',.92,0,512),('metal',(44,49,48),'plain',.42,.65,256),('steel',(145,150,148),'plain',.3,.85,256),('glass',(140,165,160),'plain',.16,0,128),('bulb',(238,217,168),'plain',.38,0,128),('linen',(206,199,179),'cloth',.95,0,512),('blanket_green',(76,99,82),'cloth',.97,0,512),('blanket_blue',(71,91,113),'cloth',.97,0,512),('canvas',(79,86,66),'cloth',.94,0,512),('canvas_red',(124,67,49),'cloth',.94,0,512),('navy',(48,62,78),'cloth',.94,0,512),('denim',(58,72,87),'cloth',.88,0,512),('ceramic',(190,185,157),'plain',.3,0,256),('paper',(219,205,171),'plain',.9,0,256),('label',(185,166,126),'plain',.88,0,256),('bottle',(77,110,73),'plain',.2,0,256),('leather',(72,52,37),'plain',.7,0,256),('rubber',(35,36,33),'plain',.9,0,256),('bark',(86,68,46),'bark',.98,0,1024),('birch',(194,188,168),'birch',.96,0,1024),('leaf',(68,92,51),'leaf',.95,0,256),('leaf_light',(95,114,64),'leaf',.95,0,256),('pine',(48,74,53),'leaf',.97,0,256),('grass',(93,111,63),'grass',.98,0,512),('drygrass',(139,129,77),'grass',.98,0,512),('soil',(112,92,67),'soil',.97,0,1024),('sand',(153,139,109),'soil',.96,0,512),('stone',(123,127,117),'stone',.96,0,512),('water',(54,94,99),'water',.18,.12,512),('rope',(171,165,139),'cloth',.98,0,256),('skin',(187,136,103),'plain',.85,0,256),('hair',(50,38,29),'plain',.95,0,256),('white',(218,214,194),'plain',.78,0,256),('yellow',(201,164,77),'plain',.85,0,128)]
for s in spec:make(*s)
print('TEXTURES_READY',len(spec),flush=True)
