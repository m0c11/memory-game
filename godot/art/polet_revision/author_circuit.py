from pathlib import Path
import json
R=Path(__file__).resolve().parents[2]
s='''[gd_scene format=3]
[ext_resource type="Script" path="res://scripts/polet/story/circuit.gd" id="script"]
[ext_resource type="Theme" path="res://resources/polet/ui.tres" id="theme"]
[node name="Circuit" type="Control"]
visible = false
layout_mode = 3
anchors_preset = 15
anchor_right = 1.0
anchor_bottom = 1.0
theme = ExtResource("theme")
script = ExtResource("script")
[node name="Shade" type="ColorRect" parent="."]
anchors_preset = 15
anchor_right = 1.0
anchor_bottom = 1.0
color = Color(0.01,0.025,0.045,0.8)
[node name="Card" type="Panel" parent="."]
offset_left = 160.0
offset_top = 65.0
offset_right = 1120.0
offset_bottom = 675.0
'''
def control(n,t,x,y,w,h,text,size=20):return f'\n[node name="{n}" type="{t}" parent="Card"]\noffset_left = {float(x)}\noffset_top = {float(y)}\noffset_right = {float(x+w)}\noffset_bottom = {float(y+h)}\ntext = {json.dumps(text,ensure_ascii=False)}\ntheme_override_font_sizes/font_size = {size}\n'+('autowrap_mode = 2\nmouse_filter = 2\n' if t=='Label' else '')
s+=control('Title','Label',30,18,900,40,'СТЕКЛЯШКА · Собираем вечерний свет',27)+control('Description','Label',30,65,900,65,'Поворачивай дорожки. Соедини источник слева с тремя секциями гирлянды справа. Золотым отмечено то, куда уже дошёл свет.')
s+=control('Source','Label',35,252,170,52,'24 V →',26)
for i in range(12):s+=control('Tile'+str(i),'Button',220+(i%4)*96,148+(i//4)*88,86,78,'─',42)
for i,text in enumerate(['Левый край','Середина','Правый край']):s+=control('Output'+str(i),'Label',625,170+i*88,280,55,'→ '+text,22)
s+=control('Hint','Label',30,420,900,70,'У каждой дорожки должны совпасть вход и выход. Можно начать с любой секции.',19)
s+=control('Check','Button',30,515,290,54,'Проверить свет')+control('Help','Button',335,515,290,54,'Подсказать участок')+control('Close','Button',640,515,290,54,'Вернуться в зал')
(R/'scenes/polet/story/circuit.tscn').write_text(s)
