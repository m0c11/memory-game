"""Offline SVG and native paper-puppet animation authoring. Not run by Godot."""
from pathlib import Path
R=Path(__file__).resolve().parents[2];A=R/'assets/studvesna/characters';S=R/'scenes/studvesna'
def svg(n,w,h,b):
 (A/(n+'.svg')).write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}"><defs><linearGradient id="shade" x2="1" y2="1"><stop stop-color="#faf3df"/><stop offset="1" stop-color="#bdb9b6"/></linearGradient><linearGradient id="skin" x2="0.8" y2="1"><stop stop-color="#f6d7bb"/><stop offset="1" stop-color="#cf9d89"/></linearGradient></defs><g stroke="#2b2434" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">{b}</g></svg>')
svg('torso',144,176,'<path fill="url(#shade)" d="M47 8 Q72 1 97 8 L130 30 119 93 128 168 Q70 180 16 168 L25 93 14 30Z"/><path fill="none" stroke="#9b9695" d="M32 49 38 145 M109 53 100 148 M46 155 Q72 161 100 153"/><path fill="#f0d1b3" d="M56 4 58 21 Q72 32 86 21 L88 4"/><path fill="#eee8db" d="M48 11 58 7 72 28 60 35Z M96 11 86 7 72 28 85 35Z"/><path stroke="#aaa4a0" d="M72 29 72 66"/>')
svg('arm',62,160,'<path fill="url(#shade)" d="M15 5 Q33 -1 47 12 L49 72 41 130 19 131 12 76Z"/><path fill="url(#skin)" d="M20 128 40 128 44 145 Q39 161 28 153 L20 150Z"/><path fill="none" stroke="#a39d9b" d="M20 52 25 106 M22 122 39 123"/>')
svg('leg',68,174,'<path fill="#30313f" d="M13 5 57 5 53 69 48 147 16 148 14 74Z"/><path fill="none" stroke="#58596a" d="M32 16 32 77 28 138"/><path fill="#21212b" d="M17 142 46 142 57 158 Q64 168 54 171 L8 171 Q3 160 17 142Z"/><path stroke="#79727a" d="M11 168 54 168"/>')
svg('scarf',102,126,'<path fill="#181921" d="M4 9 82 1 99 21 61 29 77 116 41 98 23 39Z"/><path fill="none" stroke="#686475" d="M31 24 53 84 M60 34 67 98 M10 11 78 6"/>')
svg('bow',100,52,'<path fill="#eee9dc" d="M9 7 45 19 91 7 90 46 54 33 10 46Z"/><rect fill="#f9f4ec" x="43" y="16" width="15" height="24" rx="3"/>')
for name,hair,bun,glasses,longhair in [('dasha','#503529',True,False,False),('marina','#8e6242',False,False,True),('gleb','#292535',False,True,False),('ensemble_a','#654331',True,False,False),('ensemble_b','#29232c',False,False,False),('ensemble_c','#b98149',False,False,True)]:
 b=f'<circle fill="{hair}" cx="67" cy="17" r="18"/>' if bun else ''
 b+=f'<path fill="{hair}" d="M24 68 Q10 23 56 17 Q101 10 111 55 L112 130 20 130Z"/>' if longhair else f'<path fill="{hair}" d="M24 72 Q8 34 43 22 Q91 5 110 43 L107 82Z"/>'
 b+='<path fill="url(#skin)" d="M49 105 78 105 80 137 47 137Z"/><ellipse fill="#d4a78d" cx="27" cy="75" rx="8" ry="12"/><ellipse fill="#d4a78d" cx="102" cy="75" rx="8" ry="12"/><path fill="url(#skin)" d="M29 49 Q66 20 100 49 L98 87 Q94 111 65 119 Q34 110 30 87Z"/>'
 b+=f'<path fill="{hair}" d="M24 64 Q18 24 63 21 Q107 16 110 60 Q89 58 73 39 Q54 60 24 64Z"/>'
 if longhair:b+=f'<path fill="{hair}" d="M27 55 Q20 101 12 139 L29 144 Q43 95 34 60 M94 47 Q112 98 121 137 L103 145 Q89 92 94 47Z"/>'
 b+='<path stroke="#694a3d" d="M41 67 52 65 M77 65 89 67"/><ellipse fill="#353139" cx="47" cy="76" rx="3.2" ry="4.8"/><ellipse fill="#353139" cx="83" cy="76" rx="3.2" ry="4.8"/><path stroke="#b78170" fill="none" d="M64 79 61 88 67 89"/><path stroke="#905754" fill="none" d="M55 98 Q65 104 76 98"/><ellipse fill="#d99387" stroke="none" opacity=".4" cx="42" cy="89" rx="8" ry="4"/><ellipse fill="#d99387" stroke="none" opacity=".4" cx="87" cy="89" rx="8" ry="4"/>'
 if glasses:b+='<g fill="none" stroke="#272733" stroke-width="3"><rect x="31" y="66" width="28" height="22" rx="5"/><rect x="71" y="66" width="28" height="22" rx="5"/><path d="M59 71 Q65 68 71 71"/></g>'
 svg(name+'_head',132,148,b)
def track(i,p,t,v):
 return f'tracks/{i}/type = "value"\ntracks/{i}/path = NodePath("{p}")\ntracks/{i}/interp = 1\ntracks/{i}/enabled = true\ntracks/{i}/keys = {{"times": PackedFloat32Array({", ".join(map(str,t))}), "transitions": PackedFloat32Array({", ".join("1" for _ in t)}), "update": 0, "values": [{", ".join(map(str,v))}]}}\n'
animations=''
for name,length,times,angles in [('idle',3,[0,1.5,3],[0,.025,0]),('walk',.65,[0,.1625,.325,.4875,.65],[-.28,0,.28,0,-.28]),('dance',8,[0,1,2,3,4,5,6,7,8],[.1,1.1,.6,-.3,-1.1,-.4,.3,.9,.1]),('cheer',1.4,[0,.35,.7,1.05,1.4],[1.8,2.4,1.8,2.4,1.8])]:
 animations+=f'\n[sub_resource type="Animation" id="{name}"]\nresource_name = "{name}"\nlength = {length}\nloop_mode = 1\n'
 for i,part in enumerate(['LeftArm','RightArm','LeftLeg','RightLeg']):
  factor=(-1 if i%2==0 else 1)*(1 if i<2 else (.15 if name in ['dance','cheer'] else .6))
  animations+=track(i,'Facing/Rig/'+part+':rotation:z',times,[round(a*factor,4) for a in angles])
 animations+=track(4,'Facing/Rig:position:y',times,[round(.025*abs(a),4) for a in angles])
 animations+=track(5,'Facing/Rig:rotation:z',times,[round(a*.09 if name=='dance' else 0,4) for a in angles])
animations+='\n[sub_resource type="AnimationLibrary" id="Library"]\n_data = {"idle": SubResource("idle"), "walk": SubResource("walk"), "dance": SubResource("dance"), "cheer": SubResource("cheer")}\n'
for name in ['dasha','marina','gleb','ensemble_a','ensemble_b','ensemble_c']:
 s='[gd_scene format=3]\n[ext_resource type="Script" path="res://scripts/studvesna/actor.gd" id="script"]\n'
 for part in ['torso','arm','leg','scarf','bow',name+'_head']:s+=f'[ext_resource type="Texture2D" path="res://assets/studvesna/characters/{part}.svg" id="{part}"]\n'
 s+=animations+f'\n[node name="{name.title()}" type="Node3D"]\nscript = ExtResource("script")\nscarf = {str(name=="dasha").lower()}\n'
 s+='\n[node name="Facing" type="Node3D" parent="."]\n\n[node name="Rig" type="Node3D" parent="Facing"]\n'
 def sprite(n,p,tex,pos,offset='Vector2(0, 0)',pix=.0045):return f'\n[node name="{n}" type="Sprite3D" parent="{p}"]\nposition = Vector3{pos}\npixel_size = {pix}\ntexture = ExtResource("{tex}")\noffset = {offset}\nshaded = false\nalpha_cut = 1\ntexture_filter = 4\n'
 for i,part in enumerate(['LeftLeg','RightLeg','LeftArm','RightArm']):
  x=(-1 if i%2==0 else 1)*(.13 if i<2 else .26);y=.73 if i<2 else 1.28
  s+=f'\n[node name="{part}" type="Node3D" parent="Facing/Rig"]\nposition = Vector3({x}, {y}, 0)\n'
  s+=sprite('Sprite','Facing/Rig/'+part,'leg' if i<2 else 'arm','(0, 0, 0.004)',f'Vector2(0, {-75 if i<2 else -65})')
 s+=sprite('Torso','Facing/Rig','torso','(0, 1.05, 0.018)')+sprite('Head','Facing/Rig',name+'_head','(0, 1.56, 0.025)',pix=.0042)+sprite('Scarf','Facing/Rig','scarf','(0.16, 0.69, 0.035)')
 if name=='gleb':s+=sprite('Bow','Facing/Rig','bow','(0, 1.35, 0.04)',pix=.0025)
 s+='\n[node name="AnimationPlayer" type="AnimationPlayer" parent="."]\nlibraries = {&"": SubResource("Library")}\nautoplay = "idle"\n'
 (S/(name+'.tscn')).write_text(s)
