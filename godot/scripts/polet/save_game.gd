class_name PoletSave
extends RefCounted
const PATH := "user://polet_last_day_v1.json"
const CLEANING := ["linen", "rubbish", "mop", "linen_b", "rubbish_b", "mop_b", "linen_c", "rubbish_c", "mop_c"]
const GARLAND := ["garland_box", "garland_left", "garland_right", "garland_middle"]
const TASKS := ["linen","rubbish","mop","linen_b","rubbish_b","mop_b","linen_c","rubbish_c","mop_c","child","radio","garland_box","garland_left","garland_right","garland_middle","garland","circuit","music","ride_shop","bicycle","finale"]

static func sanitize(data: Variant) -> Dictionary:
	if not data is Dictionary or (data.get("version") != 1 and data.get("version") != 2) or not data.get("finished") is Array:
		return {}
	var flags: Array[String] = []
	for task in TASKS:
		if task in data.finished: flags.append(task)
	if data.get("version") == 1:
		if flags.has("child") or ("linen" in flags and "rubbish" in flags and "mop" in flags):
			for id in CLEANING:
				if id not in flags: flags.append(id)
		if "garland" in flags:
			for id in GARLAND:
				if id not in flags: flags.append(id)
		if "bicycle" in flags: flags.append("ride_shop")
	# Reject impossible progression rather than loading a soft-locked quest.
	var requirements := {"child": CLEANING, "radio": ["child"], "garland_box": ["radio"], "garland_left": ["garland_box"], "garland_right": ["garland_box"], "garland_middle": ["garland_left","garland_right"], "garland": GARLAND, "circuit": ["garland"], "music": ["garland","circuit"], "ride_shop": ["music"], "bicycle": ["ride_shop"], "finale": ["bicycle"]}
	for task in TASKS:
		if task in flags and requirements.has(task):
			for required in requirements[task]:
				if required not in flags: flags.erase(task); break
	var switches: Array[bool] = [false,false,false]
	if data.get("switches") is Array and data.switches.size() == 3:
		for i in range(3): switches[i] = data.switches[i] == true
	var outfit := 0
	if data.get("outfit") is float or data.get("outfit") is int: outfit = clampi(int(data.outfit),0,2)
	var volume := 0.7
	if data.get("volume") is float or data.get("volume") is int: volume = clampf(float(data.volume),0.0,1.0)
	var hits := 0
	if data.get("music_hits") is float or data.get("music_hits") is int: hits = clampi(int(data.music_hits),0,2)
	var routing: Array = [0,1,1,0,1,0,0,1,0,2,1,1]
	if data.get("routing") is Array and data.routing.size()==12:
		for i in range(12):
			if data.routing[i] is int or data.routing[i] is float: routing[i]=clampi(int(data.routing[i]),0,3)
	return {"version":2,"routing":routing,"run_id":maxi(0,int(data.get("run_id",0))),"finished":flags,"switches":switches,"outfit":outfit,"volume":volume,"wind":data.get("wind",true) == true,"music_hits":hits}

static func read_data(path: String = PATH) -> Dictionary:
	for candidate in [path, path + ".bak"]:
		if FileAccess.file_exists(candidate):
			var data := read_file(candidate)
			if not data.is_empty(): return data
	return {}

static func write_data(data: Dictionary, path: String = PATH) -> Error:
	var clean := sanitize(data)
	if clean.is_empty(): return ERR_INVALID_DATA
	var file := FileAccess.open(path + ".tmp", FileAccess.WRITE)
	if file == null: return FileAccess.get_open_error()
	file.store_string(JSON.stringify(clean, "\t"))
	file.flush()
	var error := file.get_error()
	file.close()
	if error != OK: return error
	if FileAccess.file_exists(path):
		# Keep the last valid checkpoint if a write or a later load is interrupted.
		if not read_file(path).is_empty():
			error = DirAccess.copy_absolute(path, path + ".bak")
			if error != OK: return error
	return DirAccess.rename_absolute(path + ".tmp", path)

static func read_file(path: String) -> Dictionary:
	var parser := JSON.new()
	if parser.parse(FileAccess.get_file_as_string(path)) != OK: return {}
	return sanitize(parser.data)
