"""Lay out genuine Cycles renders; no generated or painted proxy objects."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,math
ROOT=Path(__file__).resolve().parents[1]
manifest=json.loads((ROOT/'asset_manifest.json').read_text())['assets']
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

def make(name,items,cols,title,subtitle):
    cellw,cellh=760,635;pad=60;header=210;footer=95
    rows=math.ceil(len(items)/cols);w=cols*cellw+2*pad;h=rows*cellh+header+footer
    bg=(242,240,235);im=Image.new('RGB',(w,h),bg);d=ImageDraw.Draw(im)
    big=ImageFont.truetype(BOLD,56);small=ImageFont.truetype(FONT,26)
    label=ImageFont.truetype(BOLD,23);meta=ImageFont.truetype(FONT,19)
    d.rectangle((pad,48,pad+52,55),fill=(25,69,145))
    d.text((pad,70),title,font=big,fill=(28,36,48))
    d.text((pad,145),subtitle,font=small,fill=(90,97,108))
    for i,a in enumerate(items):
        x=pad+(i%cols)*cellw;y=header+(i//cols)*cellh
        p=ROOT/'renders'/(a['id']+'.png')
        with Image.open(p) as src:
            src=src.convert('RGBA');bbox=src.getbbox()
            if bbox:src=src.crop(bbox)
            src.thumbnail((cellw-72,cellh-114),Image.Resampling.LANCZOS)
            pos=(x+(cellw-src.width)//2,y+20+(cellh-122-src.height)//2)
            im.paste(src,pos,src)
        d.text((x+22,y+cellh-79),a['id'][:2]+'  '+a['title_ru'],font=label,fill=(31,43,61))
        dim=' × '.join(f'{v:.2f}' for v in a['dimensions_m'])+' м'
        d.text((x+22,y+cellh-42),dim,font=meta,fill=(109,112,115))
    d.line((pad,h-footer+9,w-pad,h-footer+9),fill=(211,213,211),width=2)
    d.text((pad,h-footer+30),'Рендер реальных моделей Blender • Материалы с PNG-текстурами • Масштаб показа различается',font=small,fill=(84,92,101))
    im.save(ROOT/name,optimize=True)
    print(name,im.size,flush=True)

if __name__=='__main__':
    make('Studvesna_All_Models.png',manifest,6,'ГОД 19 → 20 / СТУДВЕСНА','33 модели • Предметы, оборудование и архитектура актового зала')
    make('Studvesna_Props_Render.png',[a for a in manifest if a['category']=='props'],4,'ПРЕДМЕТЫ И ОБОРУДОВАНИЕ','17 моделей • Матовые материалы, ткань, металл и мелкие конструктивные детали')
    make('Studvesna_Architecture_Render.png',[a for a in manifest if a['category']=='architecture'],4,'МОДУЛИ АКТОВОГО ЗАЛА','16 моделей • Сцена, проходы, стены, кулисы и потолок')
