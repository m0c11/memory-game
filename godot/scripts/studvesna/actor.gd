extends Node3D
## Saved paper sprites and animation tracks; no runtime geometry creation.
@export var follow_player := false
@export var face_camera := true
@export var costume := false
@export var scarf := false
@export var phase := 0.0
var performance := false
var celebrating := false
var last_animation := ""
func _ready() -> void: set_costume(costume)
func set_costume(value: bool) -> void:
 costume = value
 $Facing/Rig/Scarf.visible = scarf and costume
 for part in [$Facing/Rig/Torso,$Facing/Rig/LeftArm/Sprite,$Facing/Rig/RightArm/Sprite]:
  part.modulate = Color(0.24,0.25,0.31) if costume else Color.WHITE
func _process(_delta: float) -> void:
 if not is_visible_in_tree(): return
 var camera := get_viewport().get_camera_3d()
 if face_camera and camera: $Facing.global_rotation.y = atan2(camera.global_position.x-global_position.x,camera.global_position.z-global_position.z)
 var moving := false
 if follow_player:
  var player := get_tree().get_first_node_in_group("player") as CharacterBody3D
  if player: moving = Vector2(player.velocity.x,player.velocity.z).length()>0.15
 var next := "cheer" if celebrating else ("dance" if performance else ("walk" if moving else "idle"))
 if last_animation != next:
  last_animation = next
  $AnimationPlayer.play(next,0.2)
  if performance: $AnimationPlayer.seek(phase,true)
