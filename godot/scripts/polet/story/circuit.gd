extends Control
signal completed
signal cancelled
var switches: Array[bool] = [false, false, false]

func _ready() -> void:
	for i in range(3):
		get_node("Card/Switch" + str(i + 1)).pressed.connect(toggle.bind(i))
	$Card/Check.pressed.connect(verify)
	$Card/Close.pressed.connect(func(): cancelled.emit())
	refresh()

func outputs() -> Array[bool]:
	return [switches[0], switches[0] != switches[1], switches[1] != switches[2]]

func toggle(index: int) -> void:
	switches[index] = not switches[index]
	refresh()

func refresh() -> void:
	var lights := outputs()
	for i in range(3):
		get_node("Card/Lamp" + str(i + 1)).modulate = Color(0.6, 1.0, 0.55) if lights[i] else Color(0.35, 0.4, 0.45)
		get_node("Card/State" + str(i + 1)).text = "ЕСТЬ ПИТАНИЕ" if lights[i] else "НЕТ ПИТАНИЯ"
		get_node("Card/Switch" + str(i + 1)).button_pressed = switches[i]

func verify() -> void:
	if outputs() == [true, true, true]:
		completed.emit()
	else:
		$Card/Hint.text = "Не все три линии включены. Повторное нажатие отменяет действие рычага."
