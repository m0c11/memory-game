extends Control
signal chosen(index: int)

func _ready() -> void:
	$Card/Choice1.pressed.connect(func(): chosen.emit(0))
	$Card/Choice2.pressed.connect(func(): chosen.emit(1))
	$Card/Choice3.pressed.connect(func(): chosen.emit(2))

func show_page(speaker: String, message: String, choices: Array) -> void:
	$Card/Speaker.text = speaker
	$Card/Message.text = message
	for i in range(3):
		var button: Button = get_node("Card/Choice" + str(i + 1))
		button.visible = i < choices.size()
		if button.visible:
			button.text = str(choices[i])
	show()
	$Card/Choice1.grab_focus()
