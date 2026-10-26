extends CharacterBody2D
## Only movement; the capsule, collision and camera are authored in player.tscn.
@export var walk_speed: float = 120.0
@export var run_speed: float = 190.0
var movement_enabled: bool = true

func _physics_process(_delta: float) -> void:
	var direction := Input.get_vector("move_left", "move_right", "move_forward", "move_back") if movement_enabled else Vector2.ZERO
	velocity = direction * (run_speed if Input.is_action_pressed("sprint") else walk_speed)
	move_and_slide()
	if direction != Vector2.ZERO:
		$Face.position = Vector2(roundf(direction.x * 3.0), -29.0 + roundf(direction.y * 2.0))
