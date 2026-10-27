extends CanvasLayer
## Presentation and audio only; the world and all emitters are authored in scenes.
@onready var level = get_parent()
var volume: float = 0.7
var wind: bool = true
var startup_delay: float = 0.2
var ambience_started: bool = false

func _ready() -> void:
	$Air.finished.connect(func(): $Air.play())
	$Air.volume_db = -50.0
	RenderingServer.global_shader_parameter_set("polet_wind", 1.0)

func _process(delta: float) -> void:
	if not is_instance_valid(level.current_location): return
	if not ambience_started:
		startup_delay -= delta
		if startup_delay <= 0:
			ambience_started = true
			$Air.play()
	var outdoors: bool = level.current_location == level.locations.get("camp")
	$Air.volume_db = move_toward($Air.volume_db, -13.0 if outdoors else -25.0, delta * 18.0)
	$Grade.visible = not level.story.busy

func sound(kind: String) -> void:
	var node := get_node_or_null(kind) as AudioStreamPlayer
	if node != null: node.play()

func set_volume(value: float) -> void:
	volume = clampf(value, 0, 1)
	AudioServer.set_bus_volume_db(0, linear_to_db(maxf(volume,0.0001)))

func set_wind(enabled: bool) -> void:
	wind = enabled
	RenderingServer.global_shader_parameter_set("polet_wind", 1.0 if wind else 0.0)

func _exit_tree() -> void:
	for child in get_children():
		if child is AudioStreamPlayer: child.stop()
	AudioServer.set_bus_volume_db(0, 0)
	RenderingServer.global_shader_parameter_set("polet_wind", 0.0)
