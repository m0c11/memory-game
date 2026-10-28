extends Control
@onready var journey = get_node("/root/Journey")

func _ready() -> void:
	get_viewport().size_changed.connect(fit_page)
	fit_page()
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	$Page/Start.pressed.connect(start_game)
	$Page/Continue.pressed.connect(func(): journey.travel(-2))
	$Page/Settings.pressed.connect(journey.open_settings)
	$Page/Exit.pressed.connect(journey.request_quit)
	$NewGame.confirmed.connect(journey.start_new_game)
	$Page/Continue.disabled = not journey.map_started
	$Background.material.set_shader_parameter("motion",journey.motion)
	if journey.map_started: $Page/Continue.grab_focus()
	else: $Page/Start.grab_focus()

func start_game() -> void:
	if journey.map_started: $NewGame.popup_centered()
	else: journey.start_new_game()

func fit_page() -> void:
	var viewport := get_viewport_rect().size
	var factor := minf(viewport.x / 1280.0, viewport.y / 720.0)
	$Page.scale = Vector2.ONE * factor
	$Page.position = (viewport - Vector2(1280,720) * factor) * 0.5
