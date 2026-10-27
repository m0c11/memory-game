extends Control
## The album layout is authored in main_menu.tscn; this script only selects chapters.
const PHOTOS := [preload("res://assets/menu/studvesna.png"), preload("res://assets/menu/polet.png"), preload("res://assets/menu/venevitino.png")]
const CAPTIONS := ["Знакомый зал. Синие кресла. Волнение перед выходом.", "Последний день смены — и ещё столько тёплых дел.", "Тихая река, свет в окнах и маленькие ночные секреты."]
var selected := 0
var photo_tween: Tween
@onready var journey = get_node("/root/Journey")

func _ready() -> void:
	get_viewport().size_changed.connect(fit_page)
	fit_page()
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	for i in range(3):
		$Page/Chapters.get_child(i).pressed.connect(select_chapter.bind(i))
	$Page/Open.pressed.connect(func(): journey.travel(selected))
	$Page/Continue.pressed.connect(func(): journey.travel(maxi(0, journey.last_chapter)))
	$Page/Settings.pressed.connect(journey.open_settings)
	$Page/Exit.pressed.connect(journey.request_quit)
	selected = maxi(0, journey.last_chapter)
	select_chapter(selected)
	$Page/Continue.text = "Продолжить путешествие" if journey.last_chapter >= 0 else "Начать путешествие"
	$Page/ResumeHint.text = "Вернуться: " + journey.TITLES[journey.last_chapter] if journey.last_chapter >= 0 else "Начнём со сцены, света и музыки."
	$Background.material.set_shader_parameter("motion", journey.motion)
	$Page/Continue.grab_focus()

func fit_page() -> void:
	var viewport := get_viewport_rect().size
	var factor := minf(viewport.x / 1280.0, viewport.y / 720.0)
	$Page.scale = Vector2.ONE * factor
	$Page.position = (viewport - Vector2(1280,720) * factor) * 0.5

func select_chapter(index: int) -> void:
	selected = index
	for i in range(3): $Page/Chapters.get_child(i).set_pressed_no_signal(i == index)
	$Page/PhotoFrame/Chapter.text = "%02d  /  %s" % [index + 1, journey.TITLES[index]]
	$Page/Caption.text = CAPTIONS[index]
	$Page/PhotoFrame/Photo.texture = PHOTOS[index]
	if photo_tween: photo_tween.kill()
	$Page/PhotoFrame/Photo.modulate.a = 0.3
	photo_tween = create_tween()
	photo_tween.tween_property($Page/PhotoFrame/Photo, "modulate:a", 1.0, 0.35)
