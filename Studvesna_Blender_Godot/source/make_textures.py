"""Deterministic, original PBR texture maps for the Studvesna model set."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'textures'; OUT.mkdir(exist_ok=True)
rng=np.random.default_rng(1920)

def maps(name,color,kind='paint',rough=.7,metal=0,size=256,alpha=None):
    n=size; y,x=np.mgrid[:n,:n]; noise=rng.normal(0,1,(n,n))
    h=np.zeros((n,n)); v=noise*.7
    if kind in ('fabric','carpet'):
        period=4 if kind=='fabric' else 3
        h=(np.sin(x*np.pi*2/period)*np.cos(y*np.pi*2/period)+noise*.25)*.4
        v=h*3.0+noise*(1.8 if kind=='carpet' else .8)
    elif kind=='wood':
        # Horizontal, stagger-jointed maple planks with restrained grain.
        band=n//8; row=y//band; grain=np.sin(y*.28+np.sin(x*.018)*1.1)
        grain+=.35*np.sin(y*.95+x*.01)
        variation=rng.normal(0,3,8)[np.minimum(row,7)]
        v=variation+grain*3+noise*.7
        joints=(y%band<2)|((x+(row%3)*n//3)%(n//2)<2)
        v[joints]-=17; h=grain*.12; h[joints]=-.4
    elif kind=='grille':
        spacing=12; xx=(x+(y//spacing%2)*spacing/2)%spacing-spacing/2
        yy=y%spacing-spacing/2; d=np.sqrt(xx*xx+yy*yy)
        h=np.where(d<3.6,-1.,.1); v=np.where(d<3.6,-15.,2.)+noise*.3
    elif kind=='rubber':
        h=noise*.1; v=noise*.5
    elif kind=='paper': v=noise*.2
    arr=np.clip(np.array(color)[None,None,:]+v[:,:,None],0,255).astype('uint8')
    if alpha is not None: arr=np.dstack((arr,np.full((n,n),alpha,dtype='uint8')))
    Image.fromarray(arr).save(OUT/f'{name}_base.png')
    orm=np.zeros((n,n,3),dtype='uint8');orm[:,:,0]=255
    orm[:,:,1]=np.clip(rough*255+noise*.7,0,255);orm[:,:,2]=int(metal*255)
    Image.fromarray(orm).save(OUT/f'{name}_orm.png')
    dy,dx=np.gradient(h); z=np.ones_like(h); vec=np.dstack((-dx*.35,-dy*.35,z))
    vec/=np.linalg.norm(vec,axis=2)[:,:,None]
    Image.fromarray(np.clip((vec*.5+.5)*255,0,255).astype('uint8')).save(OUT/f'{name}_normal.png')

def graphic(name,im,rough=.6):
    im.save(OUT/f'{name}_base.png')
    maps(name+'_temp',(128,128,128),rough=rough,size=64)
    for suffix in ('orm','normal'):
        (OUT/f'{name}_temp_{suffix}.png').rename(OUT/f'{name}_{suffix}.png')
    (OUT/f'{name}_temp_base.png').unlink()

def main():
    specs=[
        ('blue_upholstery',(15,61,180),'fabric',.83,0,512),
        ('navy_fabric',(19,37,78),'fabric',.86,0,512),
        ('black_fabric',(30,31,35),'fabric',.9,0,512),
        ('red_canvas',(155,29,39),'fabric',.85,0,512),
        ('charcoal',(43,44,49),'paint',.57,.15,256),
        ('dark_metal',(29,31,35),'paint',.45,.65,256),
        ('rubber',(19,21,25),'rubber',.87,0,256),
        ('silver',(151,156,161),'paint',.33,.8,128),
        ('cream_wall',(222,209,176),'paint',.9,0,512),
        ('cream_trim',(230,218,192),'paint',.68,0,256),
        ('white_panel',(226,226,219),'paint',.83,0,256),
        ('carpet',(53,65,84),'carpet',.95,0,512),
        ('navy_paint',(29,44,73),'paint',.72,0,256),
        ('wood_maple',(199,158,100),'wood',.61,0,1024),
        ('wood_armrest',(115,83,52),'wood',.59,0,512),
        ('speaker_grille',(36,38,43),'grille',.73,.3,512),
        ('screen_blank',(233,226,209),'paper',.91,0,256),
        ('paper',(241,230,207),'paper',.86,0,128),
        ('screen_black',(14,18,23),'paint',.3,.1,128),
        ('blue_plastic',(16,104,195),'paint',.4,0,128),
        ('amber',(248,166,67),'paint',.32,0,128),
        ('ivory_light',(255,239,205),'paint',.35,0,64),
        ('led_green',(79,192,93),'paint',.4,0,64),
        ('led_red',(221,61,48),'paint',.4,0,64),
        ('led_blue',(65,154,199),'paint',.4,0,64),
        ('plastic_clear',(194,223,235),'paint',.18,0,128),
        ('water',(99,163,196),'paint',.19,0,128),
    ]
    for s in specs: maps(*s)
    # Unbranded audio workstation display.
    im=Image.new('RGB',(1024,640),(24,21,59));d=ImageDraw.Draw(im)
    d.rectangle((0,0,1023,38),fill=(54,39,103))
    for i in range(8):d.rectangle((20+i*70,13,63+i*70,23),fill=(116,105,157))
    d.rectangle((8,52,170,627),fill=(32,32,69))
    for j in range(9):
        yy=68+j*56;d.rectangle((20,yy,140,yy+38),fill=(45+j%2*7,43,86))
        for k in range(3): d.rectangle((28+k*30,yy+10,45+k*30,yy+15),fill=(93,92,136))
    for xx in range(190,1024,52): d.line((xx,50,xx,630),fill=(58,55,96),width=1)
    for yy in range(68,640,55): d.line((184,yy,1023,yy),fill=(58,55,96),width=1)
    for track,c in enumerate([(35,176,161),(83,196,211),(155,103,213),(114,78,174)]):
        yc=176+track*90
        for xx in range(226,995,2):
            amp=(np.sin(xx*.092+track)*.5+.5)*rng.uniform(3,28)*(1 if xx%180<145 else .1)
            d.line((xx,yc-amp,xx,yc+amp),fill=c,width=2)
    d.line((631,49,631,628),fill=(208,151,58),width=2)
    graphic('audio_display',im,.35)
    # Printed programme with the source's pastel blocks.
    im=Image.new('RGB',(768,512),(243,230,207));d=ImageDraw.Draw(im)
    d.rectangle((50,43,715,51),fill=(187,167,145))
    cols=[(212,152,174),(230,174,162),(204,215,164),(226,199,147),(179,171,203)]
    for j in range(5):
        for k in range(2):
            x=55+k*337;y=76+j*73
            d.rectangle((x,y,x+310,y+51),fill=cols[j])
            d.rectangle((x+15,y+12,x+230,y+15),fill=tuple(min(255,c+14) for c in cols[j]))
    graphic('programme_print',im,.89)
    # Manufacturer-free bottle label, evenly wrapping the bottle.
    im=Image.new('RGB',(512,192),(228,237,234));d=ImageDraw.Draw(im)
    d.rectangle((0,40,511,146),fill=(23,118,188))
    d.rectangle((0,151,511,158),fill=(74,172,217))
    graphic('bottle_label',im,.46)
    # Printed scale markings on the mixing console.
    im=Image.new('RGB',(1024,768),(38,40,44));d=ImageDraw.Draw(im)
    for i in range(16):
        x=50+i*58
        d.rectangle((x-20,40,x+20,710),outline=(65,67,71),width=1)
        for j in range(9):
            y=430+j*27;d.line((x-17,y,x-10,y),fill=(158,159,152),width=2)
        d.rectangle((x-14,728,x+13,735),fill=(158,159,152))
    d.rectangle((35,20,984,28),fill=(101,104,104))
    graphic('mixer_panel',im,.59)
    print('Textures:',len(list(OUT.glob('*.png'))),flush=True)

if __name__=='__main__': main()
