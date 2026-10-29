from pathlib import Path
import re
R=Path(__file__).resolve().parents[2]
def blocks(s):return re.split(r'(?=\[node )',s)
def modify(s,name,key,value,parent=None):
 out=[]
 for b in blocks(s):
  if b.startswith(f'[node name="{name}" ') and (parent is None or f'parent="{parent}"' in b.split('\n')[0]):
   if re.search(r'^'+re.escape(key)+r' = ',b,re.M):b=re.sub(r'^'+re.escape(key)+r' = .*$',key+' = '+value,b,flags=re.M)
   else:b=b.split('\n',1)[0]+'\n'+key+' = '+value+'\n'+b.split('\n',1)[1]
  out.append(b)
 return ''.join(out)
base=(R/'scenes/polet/rooms/cabin_room.tscn').read_text()
for suffix,color,title,shift in [('b','Color(0.34,0.48,0.55,0.7)','Комната 2 · Открытки',48),('c','Color(0.65,0.49,0.28,0.7)','Комната 3 · Солнечная',-36)]:
 s=base
 for n,pos in [('Item136',(256+shift,205)),('Item137',(248+shift,175)),('Item138',(264+shift,233)),('Item160',(142+shift,263))]:s=modify(s,n,'position',f'Vector2{pos}')
 if suffix=='b':
  # Two beds and a reading corner instead of the four-bed layout.
  s=''.join(b for b in blocks(s) if not any(b.startswith(f'[node name="{n}" ') for n in ['Item130','Item131','Item134','Item135']))
 s+=f'\n[node name="Rug" type="Polygon2D" parent="FloorDetails"]\npolygon = PackedVector2Array(180,240, 330,240, 330,302, 180,302)\ncolor = {color}\nz_index = 0\n'
 s+=f'\n[node name="RoomName" type="Label" parent="Architecture"]\noffset_left = 115.0\noffset_top = 78.0\noffset_right = 390.0\noffset_bottom = 108.0\ntext = "{title}"\ntheme_override_font_sizes/font_size = 13\nmodulate = Color(0.25,0.3,0.28,1)\n'
 for i in range(4):s+=f'\n[node name="Postcard{i}" type="Polygon2D" parent="Architecture"]\nposition = Vector2({180+i*30},108)\npolygon = PackedVector2Array(0,0, 20,0, 20,16, 0,16)\ncolor = Color({.4+i*.1},0.63,{.5-i*.05},1)\n'
 (R/f'scenes/polet/rooms/cabin_room_{suffix}.tscn').write_text(s)
