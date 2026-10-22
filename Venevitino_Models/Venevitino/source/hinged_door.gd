extends Node3D
## Attach to Door_Hinge. Call toggle() from your interaction system.
@export var open_angle_degrees: float = -100.0
@export var duration: float = 0.45
var is_open := false
var _tween: Tween

func toggle() -> void:
	is_open = not is_open
	if _tween and _tween.is_running():
		_tween.kill()
	_tween = create_tween()
	_tween.tween_property(self, "rotation:y", deg_to_rad(open_angle_degrees) if is_open else 0.0, duration).set_trans(Tween.TRANS_SINE).set_ease(Tween.EASE_IN_OUT)
