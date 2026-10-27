extends CharacterBody2D
@export var walk_speed: float = 120.0
@export var run_speed: float = 190.0
var movement_enabled: bool = true
var working: bool = false
var facing: String = "down"
var step_time: float = 0.0
var outfit: int = 0
@onready var sprite: AnimatedSprite2D = $Sprite

func set_outfit(index: int) -> void:
	outfit = clampi(index, 0, 2)
	var colors := [Color(0.22,0.52,0.58), Color(0.73,0.34,0.23), Color(0.93,0.86,0.66)]
	sprite.material.set_shader_parameter("recolor", outfit != 0)
	sprite.material.set_shader_parameter("coat_color", colors[outfit])

func _physics_process(delta: float) -> void:
	var direction := Input.get_vector("move_left", "move_right", "move_forward", "move_back") if movement_enabled else Vector2.ZERO
	var running := Input.is_action_pressed("sprint")
	velocity = direction * (run_speed if running else walk_speed)
	var previous := global_position
	move_and_slide()
	if direction != Vector2.ZERO:
		if absf(direction.x) > absf(direction.y): facing = "right" if direction.x > 0 else "left"
		else: facing = "down" if direction.y >= 0 else "up"
	var moving := previous.distance_squared_to(global_position) > 0.01
	sprite.speed_scale = 1.5 if running and moving else 1.0
	sprite.play(("work_" if working else ("walk_" if moving else "idle_")) + facing)
	step_time -= delta
	if moving and step_time <= 0.0:
		$Step.pitch_scale = 1.15 if running else 1.0
		$Step.play()
		step_time = 0.25 if running else 0.36

func _exit_tree() -> void:
	$Step.stop()
