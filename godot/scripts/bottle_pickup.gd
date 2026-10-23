class_name BottlePickup
extends Node3D
## A saved scene instance. Collection changes visibility, never spawns geometry.

signal collected(bottle: BottlePickup)

@export var pickup_prompt: String = "E — подобрать бутылку"
@export var discovery_text: String = "Бутылка найдена"

@onready var aim_point: Marker3D = $AimPoint
@onready var focus_ring: MeshInstance3D = $FocusRing
var is_collected: bool = false


func set_focused(value: bool) -> void:
	focus_ring.visible = value and not is_collected


func collect() -> bool:
	if is_collected:
		return false
	is_collected = true
	hide()
	collected.emit(self)
	return true
