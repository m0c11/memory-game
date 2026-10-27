extends Control
signal chosen(index: int)
const PORTRAITS = {
	"heroine": preload("res://assets/polet/characters/heroine_portrait.png"),
	"child": preload("res://assets/polet/characters/child_portrait.png"),
	"director": preload("res://assets/polet/characters/director_portrait.png"),
	"counselor": preload("res://assets/polet/characters/counselor_portrait.png")
}

func _ready() -> void:
	$Card/Choice1.pressed.connect(func(): chosen.emit(0))
	$Card/Choice2.pressed.connect(func(): chosen.emit(1))
	$Card/Choice3.pressed.connect(func(): chosen.emit(2))

func show_page(speaker: String, message: String, choices: Array) -> void:
	var role := "heroine"
	if speaker in ["Ребёнок", "Последний отъезд"]: role = "child"
	elif speaker == "Директриса": role = "director"
	elif speaker in ["Вожатая", "Вожатые"]: role = "counselor"
	$Card/Portrait.texture = PORTRAITS[role]
	$Card/Speaker.text = speaker
	$Card/Message.text = message
	for i in range(3):
		var button: Button = get_node("Card/Choice" + str(i + 1))
		button.visible = i < choices.size()
		if button.visible:
			button.text = str(choices[i])
	show()
	$Card/Choice1.grab_focus()
