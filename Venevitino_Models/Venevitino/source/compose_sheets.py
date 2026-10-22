from PIL import Image,ImageDraw,ImageFont,ImageOps
from pathlib import Path
import json,math
BASE=Path(__file__).resolve().parents[1];OUT=BASE/'renders'
manifest=json.loads((BASE/'docs/asset_manifest.json').read_text())
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
BG='#edeae3';INK='#25342e';MUTED='#65716a';ACCENT='#8b6546';CARD='#f8f6f0'
def font(size,bold=False):return ImageFont.truetype(BOLD if bold else FONT,size)
def fit(im,path,rect):
    x,y,w,h=rect
    with Image.open(path) as p:img=ImageOps.contain(p.convert('RGB'),(w,h),Image.Resampling.LANCZOS)
    im.paste(img,(x+(w-img.width)//2,y+(h-img.height)//2))
def wrapped(draw,text,f,width):
    out=[];line=''
    for word in text.split():
        cand=(line+' '+word).strip()
        if line and draw.textlength(cand,font=f)>width:out.append(line);line=word
        else:line=cand
    return out+[line]
groups=[('АРХИТЕКТУРА И СБОРКИ',('architecture','assembly')),('МЕБЕЛЬ И ЛИЧНЫЕ ВЕЩИ',('furniture','personal')),('ДЕРЕВЬЯ И РАСТИТЕЛЬНОСТЬ',('nature',)),('ЗЕМЛЯ, ДОРОЖКИ И РЕКА',('terrain',)),('ОБЪЕКТЫ БАЗЫ ОТДЫХА',('environment',)),('СТАТИЧЕСКИЕ ПЕРСОНАЖИ',('characters',))]
# Keep every item visible even if a future build adds a new category.
known={c for _,cats in groups for c in cats}
extra=tuple(sorted({i['category'] for i in manifest}-known))
if extra:groups.append(('ДОПОЛНИТЕЛЬНЫЕ МОДЕЛИ',extra))
groups=[(title,[i for i in manifest if i['category'] in cats]) for title,cats in groups]
groups=[g for g in groups if g[1]]
W=4000;PAD=70;GAP=22;CW=(W-2*PAD-5*GAP)//6;CH=475
H=1390+sum(100+math.ceil(len(items)/6)*CH for _,items in groups)+125
im=Image.new('RGB',(W,H),BG);d=ImageDraw.Draw(im)
d.text((PAD,50),'ВЕНЕВИТИНО',font=font(82,True),fill=INK)
d.text((PAD,155),f'{len(manifest)} моделей  /  Blender → Godot  /  рендеры готовой геометрии',font=font(32),fill=MUTED)
hero_w=(W-2*PAD-40)//2
for j,(file,label) in enumerate([('cabin_exterior.png','ДОМИК С ВЕРАНДОЙ'),('cabin_interior.png','ИНТЕРЬЕР • КРЫША И ДВЕ СТЕНЫ СКРЫТЫ')]):
    x=PAD+j*(hero_w+40)
    d.rounded_rectangle((x,235,x+hero_w,1240),radius=20,fill=CARD)
    fit(im,OUT/file,(x+12,245,hero_w-24,915))
    d.text((x+30,1183),label,font=font(28,True),fill=INK)
d.text((PAD,1285),'Масштаб 1:1 • Стены, крыша, пол и дверь разделены • Мебель и вещи можно переставлять',font=font(28),fill=MUTED)
y=1390;counter=1
for title,items in groups:
    d.text((PAD,y),title,font=font(36,True),fill=INK)
    d.line((PAD,y+61,W-PAD,y+61),fill='#b8b3a8',width=2);y+=100
    for n,item in enumerate(items):
        x=PAD+(n%6)*(CW+GAP);yy=y+(n//6)*CH
        d.rounded_rectangle((x,yy,x+CW,yy+CH-22),radius=16,fill=CARD,outline='#d0c9bd',width=1)
        key=Path(item['file']).stem
        fit(im,OUT/'catalog'/f'{key}.png',(x+10,yy+10,CW-20,320))
        d.text((x+18,yy+340),f'{counter:02}',font=font(20,True),fill=ACCENT)
        lines=wrapped(d,item['name_ru'],font(22,True),CW-82)
        for l,line in enumerate(lines[:2]):d.text((x+62,yy+336+29*l),line,font=font(22,True),fill=INK)
        d.text((x+18,yy+415),key+'.glb',font=font(16),fill=MUTED)
        counter+=1
    y+=math.ceil(len(items)/6)*CH
d.text((PAD,H-77),'PBR-текстуры встроены в GLB. Ночное освещение добавляется в Godot. Персонажи — статические модели.',font=font(24),fill=MUTED)
im.save(OUT/'Venevitino_All_Models.png',optimize=True)
preview=im.copy();preview.thumbnail((2000,10000));preview.save(OUT/'Venevitino_All_Models_preview.jpg',quality=92)

W=4200;H=3200;PAD=80;im=Image.new('RGB',(W,H),BG);d=ImageDraw.Draw(im)
d.text((PAD,55),'ЖИЛОЙ ДОМИК / ВЕНЕВИТИНО',font=font(74,True),fill=INK)
d.text((PAD,155),'Корпус 5 × 7 м • стены 2,7 м • веранда 1,5 м • настоящий внутренний объём',font=font(32),fill=MUTED)
pw=1990
for j,(file,label) in enumerate([('cabin_exterior.png','ОБЪЁМНЫЙ ВИД'),('cabin_interior.png','МЕБЛИРОВАННЫЙ ИНТЕРЬЕР В РАЗРЕЗЕ')]):
    x=PAD+j*(pw+60)
    d.rounded_rectangle((x,245,x+pw,1810),radius=20,fill=CARD)
    fit(im,OUT/file,(x+8,260,pw-16,1450))
    d.text((x+25,1743),label,font=font(30,True),fill=INK)
labels=[('cabin_front.png','СПЕРЕДИ'),('cabin_side.png','СПРАВА'),('cabin_back.png','СЗАДИ'),('cabin_top.png','СВЕРХУ')]
cw=985
for j,(file,label) in enumerate(labels):
    x=PAD+j*(cw+30)
    d.rounded_rectangle((x,1895,x+cw,2940),radius=16,fill=CARD)
    fit(im,OUT/file,(x+10,1915,cw-20,915))
    d.text((x+24,2870),label,font=font(29,True),fill=INK)
d.text((PAD,3010),'Все виды сняты с одной 3D-модели. Мебель, окна, дверь и конструктивные части сохранены отдельными объектами.',font=font(27),fill=MUTED)
d.text((PAD,3070),'Вид в разрезе получен скрытием крыши, потолка и двух стен. В GLB экспортирован полный домик.',font=font(26),fill=MUTED)
im.save(OUT/'Venevitino_Cabin_Details.png',optimize=True)
preview=im.copy();preview.thumbnail((2100,1600));preview.save(OUT/'Venevitino_Cabin_Details_preview.jpg',quality=94)
print('SHEETS_COMPLETE',len(manifest),flush=True)
