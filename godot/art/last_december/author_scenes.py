"""Offline scene authoring: every node is saved and editable in Godot."""
from pathlib import Path
from PIL import Image
import json
R=Path(__file__).resolve().parents[2]
def q(v):return json.dumps(v,ensure_ascii=False)
def node(name,type,parent=None,props=''):
 return '\n[node name='+q(name)+' type='+q(type)+((' parent='+q(parent)) if parent is not None else '')+']\n'+props+'\n'
def box(x,y,w,h):return f'offset_left = {float(x)}\noffset_top = {float(y)}\noffset_right = {float(x+w)}\noffset_bottom = {float(y+h)}\n'
def label(name,parent,text,x,y,w,h,size=22):return node(name,'Label',parent,box(x,y,w,h)+f'text = {q(text)}\ntheme_override_font_sizes/font_size = {size}\nautowrap_mode = 3\nmouse_filter = 2')
def button(name,parent,text,x,y,w,h):return node(name,'Button',parent,box(x,y,w,h)+f'text = {q(text)}')
for bg in ['room','kitchen','stop','bus','foyer','cinema']:
 s='[gd_scene load_steps=2 format=3]\n[ext_resource type="Texture2D" path="res://assets/last_december/'+bg+'.svg" id="art"]\n'
 s+=node(bg,'Control',props=box(0,0,1280,720)+'mouse_filter = 2')
 s+=node('Illustration','TextureRect','.',box(0,0,1280,720)+'texture = ExtResource("art")\nmouse_filter = 2\nexpand_mode = 1\nstretch_mode = 6')
 (R/'scenes/last_december'/f'{bg}.tscn').write_text(s)
# Small JPEGs for a clearly documented game-memory gallery, not substitute personal footage.
for source,target in [('auditorium-optimized.png','memory_hall.jpg'),('polet-final-evening.png','memory_polet.jpg'),('venevitino-canopy.png','memory_venevitino.jpg')]:
 src=R/'docs'/source
 if not src.exists():src=R/'docs'/'polet-story-evening.png'
 if not (R/'assets/last_december'/target).exists():
  im=Image.open(src).convert('RGB');im.thumbnail((1280,720));im.save(R/'assets/last_december'/target,quality=88)
s='[gd_resource type="Resource" script_class="DecemberFilm" load_steps=5 format=3]\n[ext_resource type="Script" path="res://scripts/last_december/film.gd" id="script"]\n'
for i,name in enumerate(['hall','polet','venevitino']):s+=f'[ext_resource type="Texture2D" path="res://assets/last_december/memory_{name}.jpg" id="m{i}"]\n'
s+='[resource]\nscript = ExtResource("script")\nmemories = Array[Texture2D]([ExtResource("m0"), ExtResource("m1"), ExtResource("m2")])\ncaptions = PackedStringArray("Утро, которое стало общим воспоминанием.", "Лето заканчивается. Тепло остаётся.", "Столько всего было. И столько ещё будет.")\nseconds_per_memory = 7.0\n'
(R/'resources/last_december/film.tres').write_text(s)
# Native scene: static backgrounds, portrait area, options, packing, history, film and confirmation.
s='[gd_scene load_steps=13 format=3]\n'
for typ,path,id in [('Script','scripts/last_december/novel.gd','script'),('Theme','resources/menu/theme.tres','theme'),('Resource','resources/last_december/film.tres','film'),('AudioStream','assets/polet/audio/last_evening.wav','music'),('AudioStream','assets/polet/audio/choice.wav','click')]:
 s+=f'[ext_resource type="{typ}" path="res://{path}" id="{id}"]\n'
for bg in ['room','kitchen','stop','bus','foyer','cinema']:s+=f'[ext_resource type="PackedScene" path="res://scenes/last_december/{bg}.tscn" id="{bg}"]\n'
s+='''[sub_resource type="StyleBoxFlat" id="dialog"]
bg_color = Color(0.045, 0.065, 0.10, 0.98)
border_width_top = 2
border_color = Color(0.77, 0.60, 0.37, 0.85)
corner_radius_top_left = 18
corner_radius_top_right = 18
corner_radius_bottom_left = 18
corner_radius_bottom_right = 18
'''
s+=node('LastDecember','Control',props='anchors_preset = 15\nanchor_right = 1.0\nanchor_bottom = 1.0\nscript = ExtResource("script")\ntheme = ExtResource("theme")\nfilm = ExtResource("film")')
s+=node('Letterbox','ColorRect','.', 'anchors_preset = 15\nanchor_right = 1.0\nanchor_bottom = 1.0\ncolor = Color(0.025,0.035,0.055,1)\nmouse_filter = 2')
s+=node('Page','Control','.',box(0,0,1280,720)+'mouse_filter = 2')
s+=node('Backgrounds','Control','Page',box(0,0,1280,720)+'mouse_filter = 2')
for bg in ['room','kitchen','stop','bus','foyer','cinema']:
 s+=f'\n[node name="{bg}" parent="Page/Backgrounds" instance=ExtResource("{bg}")]\nvisible = '+('true' if bg=='room' else 'false')+'\n'
s+=node('Shade','ColorRect','Page',box(0,0,1280,720)+'color = Color(0.025,0.04,0.08,0.14)\nmouse_filter = 2')
s+=node('Portrait','TextureRect','Page',box(70,80,390,635)+'expand_mode = 1\nstretch_mode = 5\nmouse_filter = 2')
s+=node('SceneFade','ColorRect','Page',box(0,0,1280,720)+'color = Color(0.03,0.05,0.08,1)\nmodulate = Color(1,1,1,0)\nmouse_filter = 2')
s+=node('Header','ColorRect','Page',box(0,0,1280,86)+'color = Color(0.03,0.05,0.08,0.8)\nmouse_filter = 2')
s+=label('Chapter','Page','01 / ДОМА',40,19,800,26,17)
s+=label('Location','Page','КВАРТИРА · ВЕЧЕР',40,48,800,26,13)
s+=button('History','Page','История',915,22,130,44)
s+=button('Pause','Page','Пауза · Esc',1060,22,180,44)
s+=node('Phone','Panel','Page',box(820,108,390,344)+'visible = false')
s+=label('Header','Page/Phone','СООБЩЕНИЯ',25,20,340,30,14)
s+=label('Sender','Page/Phone','Глеб',25,65,340,50,20)
s+=label('Message','Page/Phone','',25,127,340,180,22)
s+=node('Clip','Panel','Page',box(815,105,395,350)+'visible = false')
s+=label('Header','Page/Clip','ОДНО ВИДЕО',24,15,340,30,15)
s+=node('Image','TextureRect','Page/Clip',box(60,40,275,235)+'expand_mode = 1\nstretch_mode = 5\nmouse_filter = 2')
s+=label('Caption','Page/Clip','',24,285,345,58,18)
s+=node('Choices','VBoxContainer','Page',box(605,270,605,206)+'theme_override_constants/separation = 12\nvisible = false')
for i in range(3):s+=node('Option'+str(i),'Button','Page/Choices','layout_mode = 2\ncustom_minimum_size = Vector2(0,58)\nalignment = 0\nautowrap_mode = 3\ntheme_override_font_sizes/font_size = 20\ntext = "Ответ"')
s+=node('Bag','VBoxContainer','Page',box(690,220,500,230)+'theme_override_constants/separation = 18\nvisible = false')
for i,t in enumerate(['Телефон','Ключи','Кошелёк']):s+=node('Item'+str(i),'Button','Page/Bag','layout_mode = 2\ncustom_minimum_size = Vector2(0,62)\ntext = '+q(t))
s+=node('Dialog','Panel','Page',box(40,500,1200,192)+'theme_override_styles/panel = SubResource("dialog")')
s+=label('Speaker','Page/Dialog','ДАША',28,16,1000,32,21)
s+=label('Text','Page/Dialog','',28,57,1135,90,24)
s+=button('Next','Page/Dialog','Дальше  →',970,140,206,40)
s+=label('Help','Page/Dialog','Пробел / Enter · читать дальше',28,152,850,24,13)
s+=node('Journal','ColorRect','Page',box(0,0,1280,720)+'visible = false\ncolor = Color(0.02,0.035,0.06,0.92)')
s+=node('Card','Panel','Page/Journal',box(160,70,960,580))
s+=label('Title','Page/Journal/Card','Наш разговор',30,20,650,45,27)
s+=node('Scroll','ScrollContainer','Page/Journal/Card',box(30,85,900,410)+'horizontal_scroll_mode = 0')
s+=node('Entries','Label','Page/Journal/Card/Scroll','layout_mode = 2\nsize_flags_horizontal = 3\nautowrap_mode = 3\ntheme_override_font_sizes/font_size = 21\ntext = ""')
s+=button('Close','Page/Journal/Card','Вернуться к разговору',580,515,350,44)
s+=node('Film','ColorRect','Page',box(0,0,1280,720)+'visible = false\ncolor = Color(0,0,0,1)')
s+=node('Memory','TextureRect','Page/Film',box(0,0,1280,720)+'expand_mode = 1\nstretch_mode = 5\nmouse_filter = 2')
s+=node('MemoryShade','ColorRect','Page/Film',box(0,575,1280,145)+'visible = false\ncolor = Color(0.015,0.025,0.04,0.8)\nmouse_filter = 2')
s+=node('Video','VideoStreamPlayer','Page/Film',box(0,0,1280,720)+'expand = true\nmouse_filter = 2')
s+=label('Title','Page/Film','Последний сеанс декабря',200,315,880,90,36)+'\n'
s+=''
s+=label('Caption','Page/Film','',70,600,1140,90,24)
s+=button('Skip','Page/Film','Пропустить…',1040,28,200,44)
s+=node('Confirm','ColorRect','Page/Film',box(0,0,1280,720)+'visible = false\ncolor = Color(0.02,0.035,0.06,0.92)')
s+=node('Card','Panel','Page/Film/Confirm',box(330,220,620,280))
s+=label('Title','Page/Film/Confirm/Card','Перейти к последней нити?',30,25,560,55,26)
s+=label('Text','Page/Film/Confirm/Card','Можно досмотреть воспоминания или продолжить к финалу.',30,90,560,70,21)
s+=button('Back','Page/Film/Confirm/Card','Смотреть дальше',30,195,270,55)
s+=button('Continue','Page/Film/Confirm/Card','Перейти к финалу',320,195,270,55)
s+=node('Blackout','ColorRect','Page',box(0,0,1280,720)+'visible = false\ncolor = Color(0,0,0,1)')
s+=button('Retry','Page/Blackout','Дальше по нити',460,330,360,60)
s+=node('Music','AudioStreamPlayer','.', 'stream = ExtResource("music")\nvolume_db = -24.0')
s+=node('Foley','AudioStreamPlayer','.', 'stream = ExtResource("click")\nvolume_db = -20.0')
# Center only cinematic text; normal narrative remains left aligned.
s=s.replace('[node name="Caption" type="Label" parent="Page/Film"]', '[node name="Caption" type="Label" parent="Page/Film"]\nhorizontal_alignment = 1')
s=s.replace('text = "Последний сеанс декабря"\n','horizontal_alignment = 1\ntext = "Последний сеанс декабря"\n')
for name,parent,next_path,previous_path in [
 ('Close','Page/Journal/Card','.', '.'),('Skip','Page/Film','.', '.'),
 ('Back','Page/Film/Confirm/Card','../Continue','../Continue'),
 ('Continue','Page/Film/Confirm/Card','../Back','../Back')]:
 marker=f'[node name="{name}" type="Button" parent="{parent}"]'
 s=s.replace(marker,marker+f'\nfocus_next = NodePath("{next_path}")\nfocus_previous = NodePath("{previous_path}")')
(R/'scenes/levels/last_december.tscn').write_text(s)
(R/'art/last_december/.gdignore').touch()
