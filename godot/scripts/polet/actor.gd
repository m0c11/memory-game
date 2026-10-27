extends StaticBody2D
## Authored sprite animations; actors turn towards a nearby heroine during conversation.
@export var facing: String = "down"
var walking: bool = false
var last_position: Vector2
@onready var sprite: AnimatedSprite2D = $Sprite

func _ready() -> void:
	last_position = global_position
	sprite.play("idle_" + facing)

func _process(_delta: float) -> void:
	if not is_visible_in_tree(): return
	var movement := global_position - last_position
	last_position = global_position
	walking = movement.length_squared() > 0.01
	if walking:
		facing = direction_name(movement)
	else:
		var player := get_tree().get_first_node_in_group("polet_player") as Node2D
		if player != null: $Name.visible = player.global_position.distance_to(global_position) < 100
		if player != null and player.global_position.distance_to(global_position) < 80:
			facing = direction_name(player.global_position - global_position)
	sprite.play(("walk_" if walking else "idle_") + facing)

func direction_name(direction: Vector2) -> String:
	if absf(direction.x) > absf(direction.y): return "right" if direction.x > 0 else "left"
	return "down" if direction.y >= 0 else "up"
