import common as C
from common import *
def person(name,seated=False,variant=0):
 asset(name,'Организатор с фонариком' if not seated else 'Сидящий гость '+str(variant),'characters');hip=.57 if seated else .93;cloth='canvas_red' if variant==2 else 'navy'
 box('Jacket',(.42,.24,.53),(0,0,hip+.25),cloth,.085);sphere('Shoulders',(.48,.26,.20),(0,0,hip+.47),cloth);box('Hem',(.39,.26,.07),(0,0,hip+.015),cloth,.02);tube('Zip',[(0,-.132,hip+.04),(0,-.135,hip+.47)],.004,'metal')
 for x in [-.13,.13]:box('Pocket',(.11,.012,.075),(x,-.13,hip+.12),cloth,.006)
 cyl('Neck',.058,.10,(0,0,hip+.52),'skin',16);sphere('Head',(.17,.185,.24),(0,-.015,hip+.72),'skin',24,16);sphere('Jaw',(.135,.13,.10),(0,-.037,hip+.64),'skin',20,10);sphere('Hair',(.18,.18,.145),(0,.004,hip+.795),'hair',20,12);sphere('Nose',(.035,.055,.045),(0,-.108,hip+.71),'skin',12,8)
 for x in [-.09,.09]:sphere('Ear',(.025,.038,.055),(x,0,hip+.72),'skin',12,8)
 for x in [-.036,.036]:sphere('Eye',(.021,.006,.009),(x,-.104,hip+.745),'hair',10,6);rod('Brow',(x-.012,-.103,hip+.763),(x+.012,-.103,hip+.763),.003,'hair',6)
 for s in [-1,1]:
  shoulder=(s*.245,0,hip+.43);elbow=(s*.3,-.055,hip+.18);hand=(s*.23,-.32,hip+.13) if seated else (s*.34,-.20,hip+.02);rod('Sleeve',shoulder,elbow,.075,cloth,16);sphere('Elbow',(.14,.14,.14),elbow,cloth);rod('Sleeve',elbow,hand,.061,cloth,16);sphere('Hand',(.066,.075,.10),hand,'skin')
  knee=(s*.13,-.43,.48) if seated else (s*.12,s*.1,.5);ankle=(s*.13,-.43,.08) if seated else (s*.14,s*.15,.10);rod('Thigh',(s*.11,0,hip),knee,.085,'denim',16);sphere('Knee',(.15,.16,.15),knee,'denim');rod('Calf',knee,ankle,.064,'denim',16);sphere('Shoe',(.12,.27,.105),(ankle[0],ankle[1]-.055,.065),'rubber',20,10);sphere('Sole',(.125,.28,.027),(ankle[0],ankle[1]-.06,.025),'white',20,8)
 consolidate('Character_static_mesh')
 if not seated:t=clone('hand_flashlight',(.34,-.23,hip+.02));t.rotation_euler.x=.8
 C.ROOT['pose']='static; no rig or animation'
def build():person('organizer_static');person('guest_seated_a',True,1);person('guest_seated_b',True,2)
