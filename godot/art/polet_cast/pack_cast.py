"""Pack unmodified Blender renders into sprite atlases and authored SpriteFrames resources."""
from pathlib import Path
from PIL import Image
import shutil,json
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'assets/polet/characters';ART=ROOT/'art/polet_cast'
old=OUT/'frames'
if old.exists():shutil.move(str(old),str(ART/'frames'))
for folder in (ART/'frames').iterdir():
 if not folder.is_dir():continue
 files=sorted(folder.glob('*.png'));atlas=Image.new('RGBA',(64*8,64*((len(files)+7)//8)))
 ext='[ext_resource type="Texture2D" path="res://assets/polet/characters/'+folder.name+'.png" id="1"]\n'
 sub=[];anim={}
 for i,file in enumerate(files):
  atlas.paste(Image.open(file),((i%8)*64,(i//8)*64));kind,d,f=file.stem.split('_');direction=['down','right','up','left'][int(d)];name=kind if kind=='ride' else kind+'_'+direction
  anim.setdefault(name,[]).append((int(f),i))
  sub.append(f'[sub_resource type="AtlasTexture" id="F{i}"]\natlas = ExtResource("1")\nregion = Rect2({i%8*64}, {i//8*64}, 64, 64)')
 atlas.save(OUT/(folder.name+'.png'))
 records=[]
 for name,frames in anim.items():
  entries=', '.join('{"duration": 1.0, "texture": SubResource("F%d")}'%i for _,i in sorted(frames))
  records.append('{"name": &"'+name+'", "speed": '+('2.0' if name.startswith('idle') else '8.0')+', "loop": true, "frames": ['+entries+']}')
 (OUT/(folder.name+'.tres')).write_text(f'[gd_resource type="SpriteFrames" load_steps={len(files)+2} format=3]\n\n'+ext+'\n'+'\n\n'.join(sub)+'\n\n[resource]\nanimations = ['+',\n'.join(records)+']\n')
# A contact sheet of actual renders for review.
preview=Image.new('RGBA',(320,128),(32,44,49,255))
for i,name in enumerate(['heroine','child','director','counselor','counselor_male']):
 preview.alpha_composite(Image.open(ART/'frames'/name/'idle_0_0.png'),(i*64,0))
 preview.alpha_composite(Image.open(ART/'frames'/name/'walk_3_2.png'),(i*64,64))
preview.resize((960,384),Image.Resampling.NEAREST).save(ART/'cast_preview.png')
