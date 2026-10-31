"""Offline vector illustrations. Runtime only loads saved SVG/TS CN resources."""
from pathlib import Path
import math, random
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'assets/last_december'; OUT.mkdir(exist_ok=True)
random.seed(12)
DEFS='''<defs>
<linearGradient id="wall" x2="0" y2="1"><stop stop-color="#303443"/><stop offset="1" stop-color="#776257"/></linearGradient>
<linearGradient id="night" x2="0" y2="1"><stop stop-color="#101e38"/><stop offset="1" stop-color="#526479"/></linearGradient>
<linearGradient id="wood" x2="0" y2="1"><stop stop-color="#a97b59"/><stop offset="1" stop-color="#493c3b"/></linearGradient>
<linearGradient id="skin" x2="1" y2="1"><stop stop-color="#ffe0c2"/><stop offset="1" stop-color="#d69b86"/></linearGradient>
<linearGradient id="hair" x2="1" y2="0.4"><stop stop-color="#332b30"/><stop offset="0.55" stop-color="#785342"/><stop offset="1" stop-color="#3c3035"/></linearGradient>
<linearGradient id="coat" x2="1" y2="1"><stop stop-color="#456075"/><stop offset="1" stop-color="#202c42"/></linearGradient>
<linearGradient id="knit" x2="1" y2="1"><stop stop-color="#f5ddbc"/><stop offset="1" stop-color="#bbaa9e"/></linearGradient>
<radialGradient id="light"><stop stop-color="#ffdc91" stop-opacity=".5"/><stop offset="1" stop-color="#ffdc91" stop-opacity="0"/></radialGradient>
<linearGradient id="floor" x2="0" y2="1"><stop stop-color="#413d44"/><stop offset="1" stop-color="#171b29"/></linearGradient>
</defs>'''
def rect(x,y,w,h,c,r=0,extra=''):return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{c}" {extra}/>'
def path(d,c,s='none',sw=2):return f'<path d="{d}" fill="{c}" stroke="{s}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round"/>'
def ellipse(x,y,rx,ry,c):return f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{c}"/>'
def line(x,y,X,Y,c,w=2):return path(f'M{x} {y} L{X} {Y}','none',c,w)
def svg(name,parts,w=1280,h=720):
 (OUT/(name+'.svg')).write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">{DEFS}'+''.join(parts)+'</svg>')
def city(x,y,w,h):
 a=[rect(x,y,w,h,'url(#night)')]
 for i in range(14):
  bx=x+i*w/14; bh=random.randint(30,int(h*.6));bw=w/14-3
  a +=[rect(bx,y+h-bh,bw,bh,'#243349')]
  for wy in range(int(y+h-bh+10),int(y+h-7),22):
   for wx in range(int(bx+6),int(bx+bw-5),16):
    if random.random()<.5:a+=[rect(wx,wy,6,9,'#e5b779',extra='opacity=".65"')]
 for i in range(34):a+=[ellipse(random.uniform(x,x+w),random.uniform(y,y+h),1.3,1.3,'#c9d6db')]
 return ''.join(a)
def window(x,y,w,h):return [rect(x-10,y-10,w+20,h+20,'#b6b2a4',4), city(x,y,w,h),rect(x+w/2-4,y,8,h,'#c8c4b7'),rect(x,y+h*.61,w,7,'#c8c4b7'),rect(x-20,y+h+5,w+40,13,'#dfd1b7',3)]
def lamp(x,y):return [ellipse(x,y+45,180,190,'url(#light)'),line(x,y+50,x,y+190,'#c5a36a',5),ellipse(x,y+192,39,8,'#524746'),path(f'M{x-38} {y} L{x+38} {y} L{x+58} {y+58} L{x-58} {y+58} Z','#f5cc89')]
def plant(x,y):
 a=[path(f'M{x-20} {y} L{x+20} {y} L{x+14} {y+37} L{x-14} {y+37}Z','#ac7157')]
 for i in range(6):
  dx=(-1 if i%2 else 1)*(22+i*3);dy=-12-i*9
  a +=[line(x,y,x+dx,y+dy,'#667a68'),ellipse(x+dx,y+dy,14,6,'#6c806a')]
 return a
# Room: painted winter interior, perspective furniture, desk, thoughtful small details.
a=[rect(0,0,1280,720,'url(#wall)'),rect(0,470,1280,250,'url(#wood)')]
for x in range(-300,1600,130):a+=[line(640+(x-640)*.55,470,x,720,'#51413f',2)]
a+=window(825,110,300,275)
a += [path('M790 80 Q810 240 790 405 L825 405 L825 80Z','#6c5156'),path('M1125 80 L1160 80 Q1140 260 1170 410 L1125 410Z','#6c5156')]
a += [rect(86,145,440,315,'#5e5357',18),rect(60,350,520,165,'#b3a08d',30),path('M62 385 Q320 340 580 410 L600 520 L65 520Z','#89918e'),rect(100,330,175,65,'#ddd2b9',20),line(130,440,540,470,'#a9b3a8',3)]
a += [rect(660,405,570,23,'#b58b62',6),rect(695,428,18,155,'#65534b'),rect(1180,428,18,155,'#65534b')]+lamp(1175,230)+plant(790,365)
a += [rect(915,388,100,10,'#d1bea2',2),rect(923,376,80,12,'#5c7380',2),rect(1030,385,36,10,'#202c3e',4),rect(520,150,125,155,'#b39471',3),rect(530,160,105,135,'#bbb8aa'),path('M530 295 L570 220 L635 295Z','#738e8b')]
a += [path('M600 510 L600 430 Q645 390 700 435 L700 510Z','#473e43'),line(620,430,620,410,'#d5a668',4),line(680,430,680,410,'#d5a668',4)]
svg('room',a)
# Kitchen.
a=[rect(0,0,1280,720,'#707475'),rect(0,470,1280,250,'url(#floor)')]+window(150,100,300,265)
a +=[rect(525,95,640,155,'#bab5a1',5),rect(525,265,640,155,'#8c9690')]
for x in range(550,1160,100):a +=[line(x,95,x,250,'#747c77'),rect(x+20,155,5,38,'#606b69',2)]
for x in range(530,1160,40):a +=[line(x,270,x,420,'#aeb3a5',1)]
for y in range(270,420,38):a +=[line(525,y,1165,y,'#aeb3a5',1)]
a +=[rect(510,403,670,19,'#cfbf9f',3),rect(530,422,630,155,'#747f75',3),rect(720,435,180,122,'#2a343b',8),rect(735,450,150,88,'#1c2631',4),line(750,442,870,442,'#adab96',4)]
a +=[ellipse(998,399,55,9,'#4f6467'),path('M988 393 L988 355 Q1015 334 1020 366','none','#c0c9be',7)]+plant(575,385)
a +=[path('M100 540 L700 490 L880 720 L0 720Z','url(#wood)'),ellipse(464,562,72,20,'#dbd0b6'),ellipse(464,561,55,14,'#acb59c'),rect(335,490,30,55,'#a6c1c0',6, 'opacity=".65"'),rect(338,510,24,30,'#c9dcd7',3,'opacity=".5"')]
a +=lamp(1050,-30)
svg('kitchen',a)
# winter stop and foyer share city but different framing.
a=[city(0,0,1280,520),rect(0,520,1280,200,'#7e909d'),path('M0 580 Q450 490 1280 550 L1280 720 L0 720Z','#abb4b7'),rect(90,190,460,320,'#354654',8),rect(105,210,430,275,'#607784',extra='opacity=".65"'),rect(90,178,460,20,'#d2d0c1',4),rect(105,490,430,20,'#a48c72'),line(550,190,550,580,'#354654',12),rect(780,200,80,85,'#d1d9cf',8),line(820,280,820,570,'#8d9d9e',7)]
a +=[rect(800,218,40,40,'#466479',5),rect(805,225,30,18,'#b6c8cb',2),ellipse(809,257,4,4,'#2b3d4c'),ellipse(831,257,4,4,'#2b3d4c')]
a +=[line(1120,80,1120,520,'#283d51',9),ellipse(1120,120,160,180,'url(#light)'),rect(1093,90,54,70,'#e8c891',8)]
svg('stop',a)
a=[rect(0,0,1280,720,'#3e4e58'),city(60,70,1160,340),rect(0,405,1280,160,'#6d7d7d'),rect(0,0,1280,65,'#293b4c'),rect(20,60,28,350,'#a2a8a0'),rect(500,60,24,350,'#a2a8a0'),rect(1030,60,24,350,'#a2a8a0'),rect(20,90,1210,10,'#c6baa1')]
for x in [-100,260,800,1150]:
 a +=[rect(x,435,245,300,'#223a4e',40),rect(x+14,448,215,210,'#4f6d7e',30),line(x+35,480,x+200,480,'#79909a',2)]
a += [line(720,5,720,720,'#e2bd71',12),path('M720 55 Q805 40 800 110 L800 150','none','#e2bd71',10),rect(775,145,50,30,'none',8,'stroke="#d5bb85" stroke-width="7"')]
for i in range(7):
 x=100+i*165;a +=[path(f'M{x} 110 L{x+70} 110 L{x-60} 395 L{x-130} 395Z','#b8caca')]
# Quiet window reflections, with no distracting scratch-like diagonals.
a[-7:]=[v.replace('<path ', '<path opacity=".045" ') for v in a[-7:]]
svg('bus',a)
a=[rect(0,0,1280,720,'#233644'),rect(0,460,1280,260,'url(#floor)')]
for x in [100,425,750]:
 a +=[rect(x,105,245,290,'#a78b66',4),rect(x+8,113,229,274,'#213049',3),ellipse(x+120,240,75,110,'url(#light)'),path(f'M{x+20} 365 L{x+225} 365 L{x+120} 180Z','#5b696b')]
a +=[rect(1020,155,220,310,'#142635'),rect(1010,145,240,15,'#d6af6f'),rect(1040,210,85,170,'#788a8b'),line(1128,165,1128,455,'#c7ad83',4),rect(400,420,480,100,'#92705d',8),rect(390,415,500,15,'#d9b78b',4)]
for x in range(50,1280,110):a +=[ellipse(x,42,5,5,'#f5dab0'),ellipse(x,42,45,60,'url(#light)')]
for x in range(445,850,75):a +=[path(f'M{x} 430 L{x+40} 430 L{x+32} 470 L{x+8} 470Z','#b54f51'),ellipse(x+20,427,26,13,'#ecce96')]
svg('foyer',a)
a=[rect(0,0,1280,720,'#111b2a'),rect(180,70,920,365,'#806b5d',8),rect(190,80,900,345,'#d5cdb7',3),ellipse(640,265,430,180,'url(#light)')]
for row in range(3):
 for col in range(9):
  x=-50+col*170+(row%2)*45;y=450+row*95
  a +=[rect(x,y,150,150,'#402f3c',30),rect(x+8,y+8,134,85,'#64434c',24),line(x+28,y+25,x+120,y+25,'#85585b',2)]
svg('cinema',a)
# Busts. Expressions and two persistent outfit variants, not a claim of photoreal likeness.
for person in ['dasha','gleb','mother']:
 for mood in ['soft','smile','worried']:
  for outfit in (['cream','blue'] if person=='dasha' else ['default']):
   hair='url(#hair)' if person=='dasha' else ('#292b36' if person=='gleb' else '#766252')
   clothes='url(#coat)' if outfit=='blue' or person=='gleb' else ('url(#knit)' if person=='dasha' else '#7d8b85')
   p=[ellipse(200,625,170,20,'#172330')]
   if person!='gleb':p +=[path('M85 220 Q60 45 200 38 Q345 30 325 245 L352 460 Q280 515 64 450Z',hair)]
   p +=[path('M50 640 L62 400 Q68 351 145 334 L255 334 Q333 350 340 400 L355 640Z',clothes,'#373b45',3),path('M168 275 L231 275 L237 353 Q202 395 161 353Z','url(#skin)','#b68173',2)]
   if person=='dasha':
    p +=[path('M148 324 Q199 352 248 324 L247 369 Q195 398 149 369Z','url(#knit)','#c6b6a2',2)]
    for x in range(160,241,12):p +=[line(x,338,x,370,'#aa9d91',1)]
   elif person=='gleb':p +=[path('M145 331 L197 375 L256 330 L245 411 L203 391 L158 414Z','#a3acb2'),path('M145 345 L125 438 L190 490 L195 382Z','#35485e'),path('M256 345 L280 435 L210 492 L205 381Z','#35485e')]
   else:p +=[path('M150 330 L200 390 L250 330 L265 375 L205 450 L137 375Z','#abb2a3'),line(202,400,202,630,'#556762',3)]
   p +=[ellipse(117,211,16,26,'#d9a18b'),ellipse(284,211,16,26,'#d9a18b'),path('M116 142 Q128 76 201 76 Q276 80 286 150 L277 246 Q265 288 201 310 Q142 292 122 249Z','url(#skin)','#885c53',2)]
   p +=[path('M107 191 Q82 72 175 53 Q258 24 296 104 Q320 150 288 199 L270 145 Q231 140 204 104 Q171 153 107 191Z',hair,'#41333a',2)]
   if person=='dasha':
    p +=[path('M111 153 Q132 50 236 69','none','#ead7b5',10),path('M112 181 Q101 350 77 426 Q121 401 134 300','none','#946c4e',5),path('M281 187 Q311 350 325 434','none','#a07854',4)]
   if person=='mother':p +=[path('M115 135 Q110 220 95 276 L120 287 M281 146 Q300 225 303 271','none','#a08a72',7)]
   eyebrows='M148 183 L173 177 M227 177 L252 183' if mood!='worried' else 'M148 187 L175 172 M226 172 L252 188'
   p +=[path(eyebrows,'none','#6b4d43',4)]
   for x in [160,240]:
    p +=[ellipse(x,202,13,8,'#fff1df'),ellipse(x+1,202,5.5,8,'#57615a'),ellipse(x+1,202,3,6,'#282f35'),ellipse(x+3,200,2,2,'#fff3dd'),path(f'M{x-13} 200 Q{x} 190 {x+13} 200','none','#4e3e3c',2)]
   p +=[path('M200 207 L193 231 Q199 235 205 231','none','#b78373',2),ellipse(149,232,17,7,'#dd9c8b'),ellipse(253,232,17,7,'#dd9c8b')]
   mouth={'soft':'M183 261 Q200 269 217 260','smile':'M178 255 Q199 283 222 255 Q200 264 178 255Z','worried':'M185 265 Q200 259 215 265'}[mood]
   p +=[path(mouth,'#faf0d9' if mood=='smile' else 'none','#9b6061',2.5)]
   if person=='gleb':p +=[rect(130,184,58,39,'none',10,'stroke="#293540" stroke-width="5"'),rect(212,184,58,39,'none',10,'stroke="#293540" stroke-width="5"'),path('M188 195 Q199 188 212 195','none','#293540',5)]
   if person=='mother':p +=[ellipse(115,249,5,8,'#d2b27e'),ellipse(286,249,5,8,'#d2b27e')]
   p +=[path('M86 428 L101 625 M310 427 L298 625','none','#74757a',2)]
   svg(f'{person}_{outfit}_{mood}',p,400,650)
p=[ellipse(190,265,150,28,'#26313d'),path('M90 260 Q55 139 155 126 Q246 110 275 262Z','#737e83','#424e58',4),path('M95 127 L90 46 L147 77 Q190 58 230 78 L286 39 L273 157 Q245 204 185 203 Q118 196 95 127Z','#8d9698','#485560',4),path('M105 97 L104 66 L135 87Z','#c49c97'),path('M249 84 L273 60 L266 109Z','#c49c97'),path('M120 124 L163 131 M214 130 L253 119','none','#323e49',6),ellipse(143,135,10,4,'#aeb473'),ellipse(233,133,10,4,'#aeb473'),path('M176 153 L198 153 L188 163Z','#b58f8a'),path('M188 163 L188 171 M188 171 L175 179 M188 171 L202 178','none','#46535c',3),path('M262 250 Q343 252 317 183','none','#737e83',28)]
for y in [152,162,173]:p +=[line(130,y,55,y-10,'#c5ccca',2),line(245,y,320,y-10,'#c5ccca',2)]
svg('irzhik',p,380,300)
print('Wrote',len(list(OUT.glob('*.svg'))),'editable vector illustrations')
