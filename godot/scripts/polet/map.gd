extends Node2D
@onready var level = get_parent().get_parent()
func _process(_delta: float) -> void:
	visible = level.overview
	if not visible: return
	if level.current_location == level.locations.camp:
		$You.position = level.player.global_position
	else:
		var id: String = str(level.current_location.get_meta("location_id"))
		var marker := "Cabin" + id.get_slice("_",1) if "_" in id else ("Dorm" if id == "dorm" else "Glass")
		$You.position = level.locations.camp.get_node("Markers/" + marker).global_position
	var stage: int = level.story.stage
	var targets: Array[Vector2] = [Vector2(480,448),Vector2(840,1155),Vector2(1945,1580),Vector2(1824,1216),Vector2(1016,1584),Vector2(1824,1216)]
	$Goal.visible = stage < targets.size()
	if $Goal.visible: $Goal.position = targets[stage]
