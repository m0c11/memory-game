"""Offline authoring. All resulting worlds, actors, UI and timelines are .tscn."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[2];S=R/'scenes/studvesna'
def q(s):return json.dumps(s,ensure_ascii=False)
def node(n,t,p='.',v=''):return f'\n[node name="{n}" type="{t}"'+('' if p is None else f' parent="{p}"')+']\n'+v+'\n'
def inst(n,r,p='.',v=''):return f'\n[node name="{n}" parent="{p}" instance=ExtResource("{r}")]\n{v}\n'
def track(i,p,t,v):return f'tracks/{i}/type = "value"\ntracks/{i}/path = NodePath("{p}")\ntracks/{i}/interp = 1\ntracks/{i}/enabled = true\ntracks/{i}/keys = {{"times": PackedFloat32Array({", ".join(map(str,t))}), "transitions": PackedFloat32Array({", ".join("1" for _ in t)}), "update": 0, "values": [{", ".join(map(str,v))}]}}\n'
refs=''.join(f'[ext_resource type="PackedScene" path="res://scenes/studvesna/{n}.tscn" id="{n}"]\n' for n in ['dasha','marina','gleb','ensemble_a','ensemble_b','ensemble_c'])
s='[gd_scene format=3]\n'+refs+'\n[sub_resource type="Animation" id="show"]\nresource_name = "show"\nlength = 131.75\n'
times=[0,14,21,37,45,60,70,84,94,112,123,131.75]
for i in range(20):
 c=i%5;r=i//5
 compact=f'Vector3({-3.6+c*.55+r*.18:.3f}, 0.734, {-3.15-r*.65:.3f})'
 diagonal=f'Vector3({-3.5+c*1.45+r*.16:.3f}, 0.734, {-3.05-r*.72:.3f})'
 wide=f'Vector3({-3.2+c*1.6+(.25 if r%2 else -.25):.3f}, 0.734, {-3-r*.78:.3f})'
 low=f'Vector3({-3.2+c*1.6+(.25 if r%2 else -.25):.3f}, 0.734, {-3-r*.75:.3f})'
 s+=track(i,f'Dancers/Dancer{i:02}:position',times,[compact,compact,diagonal,diagonal,wide,wide,low,low,wide,wide,compact,compact])
s+=track(20,'ShowCamera:position',[0,35,65,96,120,131.75],['Vector3(0,4.1,7.6)','Vector3(-0.5,4,7)','Vector3(0.5,4.3,8.3)','Vector3(0,4.1,7)','Vector3(-0.4,3.9,7)','Vector3(-0.4,3.9,7)'])
s+='''
[sub_resource type="AnimationLibrary" id="library"]
_data = {"show": SubResource("show")}
[sub_resource type="StandardMaterial3D" id="panel_material"]
albedo_color = Color(0.36, 0.07, 0.13, 1)
roughness = 0.9
[sub_resource type="BoxMesh" id="panel"]
size = Vector3(1.1, 2.1, 0.08)
material = SubResource("panel_material")
'''
s+=node('Performance','Node3D',None)+node('Dancers','Node3D')
for i in range(20):
 ref='dasha' if i==2 else ['ensemble_a','ensemble_b','ensemble_c'][i%3]
 s+=inst(f'Dancer{i:02}',ref,'Dancers',f'position = Vector3({-3.2+(i%5)*1.6}, 0.734, {-3-(i//5)*.75})\ncostume = true\nphase = {(i%4)*.12}\nmetadata/heroine = {str(i==2).lower()}')
s+=node('Name','Label3D','Dancers/Dancer02','position = Vector3(0,2.05,0)\nbillboard = 1\npixel_size = 0.003\nfont_size = 22\noutline_size = 6\nmodulate = Color(1,0.83,0.48,1)\ntext = "ДАША"')
s+=node('ShowCamera','Camera3D',v='position = Vector3(0,4.1,7.6)\nrotation = Vector3(-0.23,0,0)\nfov = 50.0\ncurrent = false')+node('AnimationPlayer','AnimationPlayer',v='libraries = {&"": SubResource("library")}')+node('Scenery','Node3D')
for i in range(4):s+=node(f'Panel{i}','MeshInstance3D','Scenery',f'position = Vector3({-2+i*1.3},1.79,-6.12)\nmesh = SubResource("panel")')
for n,x,color in [('Red',-3,'Color(1,0.05,0.12,1)'),('Violet',3,'Color(0.6,0.18,1,1)')]:s+=node(n,'OmniLight3D',v=f'position = Vector3({x},3.2,-4.1)\nlight_color = {color}\nlight_energy = 1.1\nomni_range = 5.0')
(S/'performance.tscn').write_text(s)
s='''[gd_scene format=3]
[sub_resource type="StandardMaterial3D" id="material"]
shading_mode = 0
albedo_color = Color(1,0.76,0.34,1)
[sub_resource type="TorusMesh" id="ring"]
inner_radius = 0.30
outer_radius = 0.33
rings = 20
ring_segments = 6
material = SubResource("material")
'''+node('Point','Node3D',None)+node('Ring','MeshInstance3D',v='position = Vector3(0,0.02,0)\nmesh = SubResource("ring")')+node('Caption','Label3D',v='position = Vector3(0,2.1,0)\nbillboard = 1\npixel_size = 0.003\nfont_size = 28\noutline_size = 6\nmodulate = Color(1,0.86,0.58,1)\ntext = "E"')
(S/'point.tscn').write_text(s)
s='[gd_scene format=3]\n'+refs
for typ,n,path in [('Script','script','scripts/studvesna/story.gd'),('PackedScene','point','scenes/studvesna/point.tscn'),('PackedScene','performance','scenes/studvesna/performance.tscn'),('AudioStream','music','assets/studvesna/audio/dance.ogg'),('AudioStream','choice','assets/polet/audio/choice.wav'),('AudioStream','cloth','assets/polet/audio/cloth.wav'),('AudioStream','success','assets/polet/audio/success.wav'),('AudioStream','step','assets/polet/audio/step.wav'),('AudioStream','applause','assets/studvesna/audio/applause.wav'),('Theme','theme','resources/studvesna/theme.tres')]:s+=f'[ext_resource type="{typ}" path="res://{path}" id="{n}"]\n'
s+='''
[sub_resource type="StyleBoxFlat" id="panel"]
bg_color = Color(0.035,0.047,0.078,0.95)
border_width_left = 3
border_color = Color(0.85,0.65,0.36,1)
corner_radius_top_left = 10
corner_radius_top_right = 10
corner_radius_bottom_left = 10
corner_radius_bottom_right = 10
[sub_resource type="StyleBoxFlat" id="bar"]
bg_color = Color(0.93,0.67,0.32,1)
corner_radius_top_left = 4
corner_radius_top_right = 4
corner_radius_bottom_left = 4
corner_radius_bottom_right = 4
'''
s+=node('Story','Node3D',None,'script = ExtResource("script")')+node('Cast','Node3D')
s+=inst('Marina','marina','Cast','position = Vector3(-20.2,0,-4.5)')+inst('Gleb','gleb','Cast','position = Vector3(20.6,0,-4.7)\ncostume = true')
for i,(x,z) in enumerate([(-22,-4),(-19,-5),(-3.5,-.9),(3.5,-.6),(18.4,-3.9),(1.8,-1.2)]):s+=inst('Participant'+str(i),['ensemble_a','ensemble_b','ensemble_c'][i%3],'Cast',f'position = Vector3({x},0,{z})\ncostume = true')
s+=node('RehearsalCast','Node3D',v='visible = false')
for i in range(19):s+=inst('Participant'+str(i),['ensemble_a','ensemble_b','ensemble_c'][i%3],'RehearsalCast',f'position = Vector3({(i%10-4.5)*.7},0.734,{-5.05-(i//10)*.65})\ncostume = true')
s+=node('Points','Node3D')
for name,pos,label in [('Bag',(-21.1,0,-2.45),'Оставить вещи'),('Rehearsal0',(-2,.734,-4.3),'Репетиция · 1'),('Rehearsal1',(0,.734,-4),'Репетиция · 2'),('Rehearsal2',(2,.734,-4.3),'Репетиция · 3'),('Costume',(-18.9,0,-2.6),'Сценический костюм'),('Makeup',(-18.9,0,-5.65),'Подготовить грим'),('Marina',(-20.2,0,-4.5),'Марина'),('Gleb',(20.6,0,-4.7),'Глеб'),('Ready',(-4.7,.734,-3.25),'Готовность к выходу')]:s+=inst(name,'point','Points',f'position = Vector3{pos}\nmetadata/label = {q(label)}')
s+=inst('Performance','performance',v='visible = false')+node('Celebration','Node3D',v='visible = false')
for i in range(22):
 ref='dasha' if i==2 else 'gleb' if i==8 else 'marina' if i==9 else ['ensemble_a','ensemble_b','ensemble_c'][i%3]
 s+=inst('Person'+str(i),ref,'Celebration',f'position = Vector3({(i%11-5)*.7},{0 if i<11 else .734},{-.65 if i<11 else -2.9})\ncostume = {str(ref!="marina").lower()}')
s+=node('FinalCamera','Camera3D',v='position = Vector3(0,3.3,8)\nrotation = Vector3(-0.18,0,0)\nfov = 53.0\ncurrent = false')
for n,ref,vol in [('DanceAudio','music',-4),('Choice','choice',-14),('Cloth','cloth',-12),('Success','success',-14),('Applause','applause',-15),('Footstep','step',-24)]:s+=node(n,'AudioStreamPlayer',v=f'stream = ExtResource("{ref}")\nvolume_db = {vol}')
s+=node('HUD','CanvasLayer',v='layer = 6')+node('Page','Control','HUD','layout_mode = 0\noffset_right = 1280.0\noffset_bottom = 720.0\nmouse_filter = 2\ntheme = ExtResource("theme")')
def ctrl(n,t,p,rect,props=''):
 x,y,w,h=rect
 return node(n,t,p,f'layout_mode = 0\noffset_left = {float(x)}\noffset_top = {float(y)}\noffset_right = {float(x+w)}\noffset_bottom = {float(y+h)}\n'+props)
def lab(n,p,rect,text,size=20,extra=''):return ctrl(n,'Label',p,rect,f'text = {q(text)}\ntheme_override_font_sizes/font_size = {size}\nmouse_filter = 2\nautowrap_mode = 2\n'+extra)
def btn(n,p,rect,text):return ctrl(n,'Button',p,rect,f'text = {q(text)}\nmouse_default_cursor_shape = 2')
s+=ctrl('Objective','Panel','HUD/Page',(24,20,650,112),'mouse_filter = 2\ntheme_override_styles/panel = SubResource("panel")')
s+=lab('Chapter','HUD/Page/Objective',(20,9,610,28),'СТУДВЕСНА · РАННЕЕ УТРО',17,'theme_override_colors/font_color = Color(0.94,0.74,0.45,1)')+lab('Task','HUD/Page/Objective',(20,40,610,62),'Оставить вещи в левой кулисе',22)
s+=lab('Prompt','HUD/Page',(220,625,840,38),'',22,'horizontal_alignment = 1')+lab('Controls','HUD/Page',(100,682,1080,30),'WASD — идти · E — действие · Tab — камера · Esc — пауза',15,'horizontal_alignment = 1')+lab('Notice','HUD/Page',(290,145,700,55),'',19,'horizontal_alignment = 1')
s+=ctrl('Dialog','Panel','HUD/Page',(240,420,800,252),'visible = false\ntheme_override_styles/panel = SubResource("panel")')+lab('Speaker','HUD/Page/Dialog',(28,12,744,28),'',22,'theme_override_colors/font_color = Color(0.94,0.74,0.45,1)')+lab('Text','HUD/Page/Dialog',(28,46,744,73),'',20)
for i in range(3):s+=btn('Option'+str(i),'HUD/Page/Dialog',(28,125+i*40,744,36),'Продолжить')
s+=ctrl('Rhythm','Panel','HUD/Page',(260,510,760,158),'visible = false\ntheme_override_styles/panel = SubResource("panel")')+lab('Title','HUD/Page/Rhythm',(20,10,720,28),'Вместе в ритме',20,'horizontal_alignment = 1')+lab('Cue','HUD/Page/Rhythm',(25,42,710,42),'←    →',30,'horizontal_alignment = 1')
s+=ctrl('Timing','ProgressBar','HUD/Page/Rhythm',(110,92,540,8),'show_percentage = false\nmax_value = 1.0\ntheme_override_styles/fill = SubResource("bar")')+lab('Feedback','HUD/Page/Rhythm',(20,110,450,35),'Ошибки не прерывают танец',16)+btn('Watch','HUD/Page/Rhythm',(490,109,250,36),'Просто смотреть')+lab('Subtitle','HUD/Page',(220,445,840,54),'',25,'horizontal_alignment = 1')
s+=ctrl('Curtain','ColorRect','HUD/Page',(0,0,1280,720),'visible = false\nmouse_filter = 0\ncolor = Color(0.015,0.02,0.035,1)')+lab('Title','HUD/Page/Curtain',(160,280,960,90),'',34,'horizontal_alignment = 1')+lab('Small','HUD/Page/Curtain',(160,380,960,70),'',20,'horizontal_alignment = 1')
s+=btn('FinalReturn','HUD/Page',(460,615,360,50),'Дальше по нити воспоминаний')+'visible = false\n'
(S/'story.tscn').write_text(s)
p=R/'scenes/levels/auditorium.tscn';s=p.read_text().replace('Vector3(-3.5, 0.734, -2.94)','Vector3(-2.7, 0.734, -2.94)').replace('Vector3(3.5, 0.734, -2.94)','Vector3(2.7, 0.734, -2.94)')
if 'id="story"' not in s:
 s=s.replace('load_steps=32','load_steps=34');at=s.index('[sub_resource')
 s=s[:at]+'[ext_resource type="PackedScene" path="res://scenes/studvesna/story.tscn" id="story"]\n[ext_resource type="PackedScene" path="res://scenes/studvesna/dasha.tscn" id="heroine"]\n\n'+s[at:]
 at=s.index('[node name="Backstage"');s=s[:at]+'''[node name="Body" parent="Player/Visual" index="0"]
visible = false
[node name="FrontMarker" parent="Player/Visual" index="1"]
visible = false
[node name="Heroine" parent="Player/Visual" instance=ExtResource("heroine")]
follow_player = true
[node name="Story" parent="." instance=ExtResource("story")]

'''+s[at:];s+='\n[editable path="Player"]\n'
p.write_text(s)
