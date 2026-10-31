"""One-time migration of authored map stops and addition of the underline scene."""
from pathlib import Path
import re,math
R=Path(__file__).resolve().parents[2];p=R/'scenes/thread_map/thread_map.tscn';s=p.read_text()
if 'id="underline_route"' in s:raise SystemExit('Finale is already authored; edit the native scene directly.')
s=re.sub(r'\[node name="Stop3".*?(?=\[node name="Stop4")','',s,flags=re.S)
s=s.replace('Stop4','Stop3').replace('Stop5','Stop4').replace('День рождения','Последний сеанс декабря').replace('metadata/fraction = 0.87','metadata/fraction = 0.80')
nums=[float(v) for v in re.search(r'points = PackedVector2Array\((.*?)\)',s).group(1).split(',')]
pts=list(zip(nums[::2],nums[1::2]));lengths=[math.dist(a,b) for a,b in zip(pts,pts[1:])];remaining=sum(lengths)*.80
for a,b,l in zip(pts,pts[1:],lengths):
 if remaining<=l:
  pos=(a[0]+(b[0]-a[0])*remaining/l,a[1]+(b[1]-a[1])*remaining/l);break
 remaining-=l
s=s.replace('position = Vector2(3834.433,1213.255)',f'position = Vector2({pos[0]:.3f},{pos[1]:.3f})')
# A short right turn, followed by a flowing underline back to the left.
def cubic(a,b,c,d,n):
 return [tuple((1-t)**3*a[j]+3*(1-t)**2*t*b[j]+3*(1-t)*t*t*c[j]+t**3*d[j] for j in range(2)) for t in [i/n for i in range(n+1)]]
u=cubic((4530,1560),(4760,1600),(4920,1980),(4580,2050),28)+cubic((4580,2050),(3650,2260),(1650,2030),(460,2180),70)[1:]
flat=', '.join(f'{v:.3f}' for xy in u for v in xy)
data=', '.join(f'0, 0, 0, 0, {x:.3f}, {y:.3f}' for x,y in u)
sub=f'''[sub_resource type="Curve2D" id="underline_route"]
bake_interval = 3.0
_data = {{"points": PackedVector2Array({data})}}
point_count = {len(u)}
[sub_resource type="ShaderMaterial" id="underline_material"]
resource_local_to_scene = true
shader = ExtResource("thread")
shader_parameter/revealed = 0.0
'''
s=s.replace('[node name="ThreadMap"',sub+'\n[node name="ThreadMap"')
s=s.replace('load_steps=12','load_steps=14')
s+='''
[node name="Underline" type="Path2D" parent="."]
visible = false
curve = SubResource("underline_route")
[node name="Line" type="Line2D" parent="Underline"]
'''+f'points = PackedVector2Array({flat})\n'+'''width = 18.0
default_color = Color(1,0.77,0.4,1)
texture = SubResource("texture")
texture_mode = 2
material = SubResource("underline_material")
antialiased = true
joint_mode = 2
begin_cap_mode = 2
end_cap_mode = 2
[node name="Ball" type="PathFollow2D" parent="Underline"]
rotates = false
loop = false
[node name="Yarn" type="Sprite2D" parent="Underline/Ball"]
texture = ExtResource("ball")
scale = Vector2(0.38,0.38)
[node name="Look" type="Label" parent="HUD/UI/Page/Ending"]
visible = false
offset_left = 140.0
offset_top = 642.0
offset_right = 1140.0
offset_bottom = 680.0
text = "А теперь посмотри на меня."
theme_override_font_sizes/font_size = 24
horizontal_alignment = 1
mouse_filter = 2
[node name="Continue" type="Button" parent="HUD/UI/Page/Ending"]
visible = false
offset_left = 1040.0
offset_top = 28.0
offset_right = 1232.0
offset_bottom = 76.0
text = "Продолжить"
[node name="Replay" type="Button" parent="HUD/UI/Page/Ending"]
visible = false
offset_left = 720.0
offset_top = 28.0
offset_right = 1015.0
offset_bottom = 76.0
text = "Ещё раз в кино"
'''
s=s.replace('Пусть всё, что нас согревает, останется рядом.','Даша, спасибо тебе за этот год. Я люблю тебя.').replace('С Новым годом. С любовью.','Продолжение — вместе.').replace('offset_top = 568.0','offset_top = 546.0').replace('offset_bottom = 628.0','offset_bottom = 600.0').replace('offset_top = 626.0','offset_top = 600.0').replace('offset_bottom = 666.0','offset_bottom = 635.0')
p.write_text(s)
