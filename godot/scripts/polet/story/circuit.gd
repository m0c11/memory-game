extends Control
## A saved 4x3 low-voltage lighting board. Rotate pieces to reach all three outputs.
signal completed
signal cancelled
const SHAPES := [0,6,10,10,10,15,10,10,0,3,10,10]
const INITIAL := [0,1,1,0,1,0,0,1,0,2,1,1]
var rotations: Array = INITIAL.duplicate()
func _ready() -> void:
 for i in range(12):get_node("Card/Tile%d"%i).pressed.connect(turn.bind(i))
 $Card/Check.pressed.connect(verify)
 $Card/Close.pressed.connect(func():cancelled.emit())
 $Card/Help.pressed.connect(hint)
 refresh()
func mask(index: int) -> int:
 var bits: int=SHAPES[index]
 for i in range(int(rotations[index])%4): bits=((bits<<1)&15)|(bits>>3)
 return bits
func reached() -> Array[int]:
 var visited: Array[int]=[]
 if mask(4)&8==0:return visited
 var queue: Array[int]=[4]
 while not queue.is_empty():
  var index: int=queue.pop_front()
  if index in visited:continue
  visited.append(index)
  for d in range(4):
   var x: int=index%4+[0,1,0,-1][d]
   var y: int=index/4+[-1,0,1,0][d]
   if x<0 or x>3 or y<0 or y>2:continue
   var next:=y*4+x
   if mask(index)&(1<<d) and mask(next)&(1<<((d+2)%4)):queue.append(next)
 return visited
func outputs() -> Array[bool]:
 var live:=reached()
 return [3 in live and mask(3)&2!=0,7 in live and mask(7)&2!=0,11 in live and mask(11)&2!=0]
func turn(index: int) -> void:
 rotations[index]=(int(rotations[index])+1)%4
 get_parent().get_parent().sound("Click")
 refresh()
func refresh() -> void:
 var live:=reached()
 var glyphs:={0:"·",3:"└",6:"┌",9:"┘",12:"┐",5:"│",10:"─",15:"┼"}
 for i in range(12):
  var button: Button=get_node("Card/Tile%d"%i)
  button.text=glyphs.get(mask(i),"+")
  button.disabled=SHAPES[i]==0
  button.modulate=Color(1,.85,.45) if i in live else Color(.64,.73,.8)
 var lit:=outputs()
 for i in range(3):get_node("Card/Output%d"%i).modulate=Color(1,.84,.4) if lit[i] else Color(.45,.5,.55)
func hint() -> void:
 for i in range(12):
  if mask(i)!=SHAPES[i]:rotations[i]=0;refresh();$Card/Hint.text="Один участок поправлен. Следуй за золотой дорожкой от источника.";return
func verify() -> void:
 if outputs()==[true,true,true]:completed.emit()
 else:$Card/Hint.text="Дорожка ещё обрывается. Поверни элементы и проведи свет к трём выходам справа."
