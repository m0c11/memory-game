class_name DecemberFilm
extends Resource
## Assign the real year montage as an Ogg Theora VideoStream in the Inspector.
## Until then, the authored gallery of game memories keeps the ending playable.
@export var video: VideoStream
@export var memories: Array[Texture2D] = []
@export var captions: PackedStringArray = []
@export_range(3.0, 20.0) var seconds_per_memory := 7.0
