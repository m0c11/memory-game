extends Node3D
## Behaviour only; actors, scenery, UI, choreography and points are saved scenes.
enum Stage { ARRIVAL, REHEARSAL, BACKSTAGE, READY, DANCE, CELEBRATION, DONE }
const CHAPTERS := ["РАННЕЕ УТРО","РЕПЕТИЦИЯ","В КУЛИСАХ","ПЕРЕД ВЫХОДОМ","ВМЕСТЕ НА СЦЕНЕ","ПОСЛЕ КОНЦЕРТА","МЫ СДЕЛАЛИ ЭТО"]
var stage := Stage.ARRIVAL
var rehearsal := 0
var costume_ready := false
var makeup_ready := false
var spoke_marina := false
var spoke_gleb := false
var watch_only := false
var busy := false
var test_mode := false
var dance_time := 0.0
var next_cue := 0
var cue_hit := false
var hits := 0
var cues: Array = []
var nearest := ""
var choice_callback: Callable
var cached_mouse := Input.MOUSE_MODE_VISIBLE
var notice_time := 0.0
var footstep_time := 0.0
var light_snapshot: Dictionary = {}
var save_failed := false
@onready var level := get_parent()
@onready var player: CharacterBody3D = level.get_node("Player")
@onready var flow := get_node("/root/Journey")
@onready var ui: Control = $HUD/Page
@onready var dialog: Control = $HUD/Page/Dialog
@onready var rhythm: Control = $HUD/Page/Rhythm
@onready var heroine: Node3D = level.get_node("Player/Visual/Heroine")

func _ready() -> void:
 var chart = JSON.parse_string(FileAccess.get_file_as_string("res://resources/studvesna/rhythm.json"))
 if chart is Dictionary: cues = chart.get("cues",[])
 for i in range(3): dialog.get_node("Option%d"%i).pressed.connect(choose.bind(i))
 rhythm.get_node("Watch").pressed.connect(func():
  watch_only = not watch_only
  rhythm.get_node("Watch").text = "Играть в ритм" if watch_only else "Просто смотреть")
 $DanceAudio.finished.connect(finish_dance)
 ui.get_node("FinalReturn").pressed.connect(func(): flow.finish_and_return())
 get_viewport().size_changed.connect(fit_ui)
 fit_ui()
 for lamp in level.get_node("Lighting").get_children():
  if lamp is Light3D: light_snapshot[lamp] = [lamp.light_energy,lamp.light_color]
 call_deferred("startup")

func startup() -> void:
 test_mode = level.get_meta("test_mode",false)
 if test_mode:
  $HUD.hide()
  set_process(false)
  return
 level.get_node("Interface").hide()
 restore(flow.hall_state)
 refresh()
 if stage == Stage.ARRIVAL:
  show_dialog("Даша · раннее утро","Вещи в руках, музыка за дверью, впереди целый день. Сначала оставлю сумку в левой кулисе, потом — на прогон.",["Начать этот день"],func(_choice): pass)
 elif stage == Stage.CELEBRATION: begin_celebration()
 elif stage == Stage.DONE:
  show_final_group()
  ending_dialog()
 else: notice("Продолжаем с сохранённого этапа")

func fit_ui() -> void:
 var size := get_viewport().get_visible_rect().size
 var factor := minf(size.x/1280.0,size.y/720.0)
 ui.scale = Vector2.ONE*factor
 ui.position = (size-Vector2(1280,720)*factor)*0.5

func snapshot() -> Dictionary:
 return {"version":1,"stage":int(stage),"rehearsal":rehearsal,"costume":costume_ready,"makeup":makeup_ready,"marina":spoke_marina,"gleb":spoke_gleb,"watch":watch_only}

func restore(data: Dictionary) -> void:
 stage = Stage.ARRIVAL
 rehearsal = 0
 costume_ready = false
 makeup_ready = false
 spoke_marina = false
 spoke_gleb = false
 watch_only = false
 if data.get("version",0) != 1: return
 var raw_stage = data.get("stage",0)
 var raw_count = data.get("rehearsal",0)
 if raw_stage is int: stage = clampi(raw_stage,0,6) as Stage
 if raw_count is int: rehearsal = clampi(raw_count,0,3)
 costume_ready = data.get("costume",false) == true
 makeup_ready = data.get("makeup",false) == true
 spoke_marina = data.get("marina",false) == true
 spoke_gleb = data.get("gleb",false) == true
 watch_only = data.get("watch",false) == true
 if stage > Stage.REHEARSAL and rehearsal < 3: stage = Stage.REHEARSAL
 if stage >= Stage.READY and not (costume_ready and makeup_ready): stage = Stage.BACKSTAGE
 if stage == Stage.DANCE: stage = Stage.READY

func save_progress() -> bool:
 if test_mode: return true
 flow.hall_state = snapshot()
 save_failed = not flow.persist()
 if save_failed: notice("Не удалось сохранить. Прогресс остаётся в игре; проверь свободное место.",8.0)
 return not save_failed

func refresh() -> void:
 ui.get_node("Objective/Chapter").text = "СТУДВЕСНА · "+CHAPTERS[stage]
 var tasks := ["Оставь вещи в левой кулисе · E у крючков","Поднимись на сцену по боковым ступеням · отметка %d / 3"%mini(rehearsal+1,3),"Подготовь костюм и грим в левой кулисе","Возвращайся на сцену · место у левого края","Даша — часть большого танца","Весь концерт позади. Теперь можно выдохнуть.","Это был наш день."]
 ui.get_node("Objective/Task").text = tasks[stage]
 if stage == Stage.BACKSTAGE:
  ui.get_node("Objective/Task").text = "Костюм: %s · грим: %s\nМожно поговорить с Мариной и заглянуть к Глебу."%["готов" if costume_ready else "у крючков","готов" if makeup_ready else "у зеркала"]
 heroine.set_costume(costume_ready)
 $RehearsalCast.visible = stage == Stage.REHEARSAL
 if stage < Stage.DANCE: level.get_node("BackstageProps").visible = stage != Stage.REHEARSAL
 for point in $Points.get_children():
  point.visible = point_active(point.name)
  point.get_node("Caption").text = str(point.get_meta("label"))
 ui.get_node("Objective").visible = stage != Stage.DANCE
 ui.get_node("Controls").visible = stage < Stage.DANCE
 level.get_node("Backstage/Left/SportsBag").visible = stage != Stage.ARRIVAL

func point_active(id: String) -> bool:
 match id:
  "Bag": return stage == Stage.ARRIVAL
  "Rehearsal0","Rehearsal1","Rehearsal2": return stage == Stage.REHEARSAL and id == "Rehearsal%d"%rehearsal
  "Costume": return stage == Stage.BACKSTAGE and not costume_ready
  "Makeup": return stage == Stage.BACKSTAGE and not makeup_ready
  "Marina","Gleb": return stage in [Stage.ARRIVAL,Stage.REHEARSAL,Stage.BACKSTAGE,Stage.READY]
  "Ready": return stage == Stage.READY
 return false

func _process(delta: float) -> void:
 if test_mode: return
 if notice_time > 0:
  notice_time -= delta
  if notice_time <= 0: ui.get_node("Notice").text = ""
 if stage == Stage.DANCE:
  if $DanceAudio.playing:
   dance_time = maxf(dance_time,$DanceAudio.get_playback_position()+AudioServer.get_time_since_last_mix()-AudioServer.get_output_latency())
  update_dance()
  return
 footstep_time -= delta
 if not busy and player.is_on_floor() and Vector2(player.velocity.x,player.velocity.z).length()>0.5 and footstep_time<=0:
  $Footstep.play()
  footstep_time = 0.31 if Input.is_action_pressed("sprint") else 0.46
 nearest = ""
 var distance := 1.65
 if not busy and stage < Stage.DANCE:
  for point in $Points.get_children():
   if not point_active(point.name): continue
   var d := player.global_position.distance_to(point.global_position)
   if d < distance and can_reach(point):
    distance = d
    nearest = point.name
 ui.get_node("Prompt").text = "E — "+str($Points.get_node(nearest).get_meta("label")) if not nearest.is_empty() else ""

func can_reach(point: Node3D) -> bool:
 var from := player.global_position+Vector3.UP*1.25
 var to := point.global_position+Vector3.UP*1.25
 var query := PhysicsRayQueryParameters3D.create(from,to,1,[player.get_rid()])
 return get_world_3d().direct_space_state.intersect_ray(query).is_empty()

func _input(event: InputEvent) -> void:
 if test_mode or event.is_echo(): return
 if stage == Stage.DANCE and event is InputEventKey and event.pressed and event.physical_keycode in [KEY_LEFT,KEY_RIGHT,KEY_A,KEY_D]:
  hit_cue(0 if event.physical_keycode in [KEY_LEFT,KEY_A] else 1)
  get_viewport().set_input_as_handled()

func _unhandled_input(event: InputEvent) -> void:
 if test_mode or event.is_echo() or stage == Stage.DANCE: return
 if not busy and event.is_action_pressed("interact") and not nearest.is_empty():
  get_viewport().set_input_as_handled()
  interact(nearest)

func interact(id: String) -> void:
 if busy or not point_active(id): return
 match id:
  "Bag": perform_action("Вещи на своём месте","Теперь можно идти на сцену.","bag")
  "Rehearsal0","Rehearsal1","Rehearsal2": perform_action("Держим позицию…",["Свет слева. Вижу отметку.","Теперь вместе в центр.","Последняя позиция. Прогон готов."][rehearsal],"rehearsal")
  "Costume": perform_action("За кулисами","Собрать волосы в гульку. Закрепить чёрный платок на бедре.","costume")
  "Makeup": perform_action("Минута у зеркала","Поправить грим. Посмотреть на себя. Можно выдохнуть.","makeup")
  "Marina":
   show_dialog("Марина","Сколько всего сразу… Но на сцене мы будем вместе. Как ты?",["Волнуюсь. Давай ещё раз вспомним начало.","Хорошо, что мы здесь вместе."],func(choice):
    spoke_marina = true
    save_progress()
    show_dialog("Марина","Начинаем вместе, следим друг за другом. Если собьёмся — продолжим." if choice == 0 else "Да. Давай запомним этот день.",["Улыбнуться Марине"],func(_i): pass))
  "Gleb":
   show_dialog("Глеб","Сейчас ещё проверю реквизит — и снова к своим. У вас скоро выход?",["Да, мы готовимся. Удачи вашему номеру!","Не отвлекаю. Увидимся после концерта!"],func(_choice):
    spoke_gleb = true
    save_progress()
    notice("Глеб улыбается и возвращается к своей группе."))
  "Ready":
   show_dialog("Перед выходом","Мы готовы. Вдох — и вместе к свету. Можно мягко подыгрывать стрелками или просто смотреть номер.",["Выйти · играть в ритм","Выйти · смотреть выступление","Ещё немного побыть в кулисах"],func(choice):
    if choice < 2: start_dance(choice == 1))

func perform_action(title: String, detail: String, kind: String) -> void:
 lock_player(true)
 busy = true
 ui.get_node("Curtain/Title").text = title
 ui.get_node("Curtain/Small").text = detail
 if kind == "rehearsal": ui.get_node("Subtitle").text = detail
 else: ui.get_node("Curtain").show()
 $Cloth.play()
 await get_tree().create_timer(1.8,false).timeout
 if not is_inside_tree(): return
 complete_action(kind)
 ui.get_node("Subtitle").text = ""
 ui.get_node("Curtain").hide()
 busy = false
 lock_player(false)

func complete_action(kind: String) -> void:
 match kind:
  "bag":
   if stage != Stage.ARRIVAL: return
   stage = Stage.REHEARSAL
  "rehearsal":
   if stage != Stage.REHEARSAL: return
   rehearsal = mini(rehearsal+1,3)
   set_rehearsal_light()
   if rehearsal == 3: stage = Stage.BACKSTAGE
  "costume":
   if stage != Stage.BACKSTAGE: return
   costume_ready = true
  "makeup":
   if stage != Stage.BACKSTAGE: return
   makeup_ready = true
 if stage == Stage.BACKSTAGE and costume_ready and makeup_ready: stage = Stage.READY
 $Success.play()
 refresh()
 save_progress()

func set_rehearsal_light() -> void:
 var colors := [Color(1,0.65,0.4),Color(0.7,0.5,1),Color(1,0.3,0.35)]
 for lamp in light_snapshot:
  if "Stage" in lamp.name: lamp.light_color = colors[clampi(rehearsal-1,0,2)]

func lock_player(value: bool) -> void:
 player.set_physics_process(not value)
 player.set_process_unhandled_input(not value)
 if value: player.velocity = Vector3.ZERO
 for portal in [level.get_node("Passages/LeftEntrance"),level.get_node("Passages/RightEntrance"),level.get_node("Backstage/Left/Exit"),level.get_node("Backstage/Right/Exit")]:
  portal.set_process_unhandled_input(not value)
  portal.get_node("Prompt").visible = not value

func show_dialog(speaker: String,text: String,options: Array,callback: Callable) -> void:
 if not busy: cached_mouse = Input.mouse_mode
 busy = true
 lock_player(true)
 Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
 ui.get_node("Prompt").text = ""
 dialog.get_node("Speaker").text = speaker
 dialog.get_node("Text").text = text
 choice_callback = callback
 for i in range(3):
  var button: Button = dialog.get_node("Option%d"%i)
  button.visible = i < options.size()
  if i < options.size(): button.text = options[i]
 dialog.show()
 dialog.get_node("Option0").grab_focus()

func choose(index: int) -> void:
 if not dialog.visible or not dialog.get_node("Option%d"%index).visible: return
 var callback := choice_callback
 choice_callback = Callable()
 dialog.hide()
 busy = false
 lock_player(false)
 Input.mouse_mode = cached_mouse
 $Choice.play()
 if callback.is_valid(): callback.call(index)

func start_dance(watch: bool) -> void:
 if stage != Stage.READY: return
 stage = Stage.DANCE
 watch_only = watch
 busy = true
 lock_player(true)
 player.hide()
 $Cast.hide()
 $Performance.show()
 $Performance/ShowCamera.make_current()
 level.get_node("BakedLighting").hide()
 level.get_node("Architecture/Stage/ProjectionScreen").hide()
 level.get_node("BackstageProps").hide()
 level.get_node("Equipment/Microphone").hide()
 for lamp in light_snapshot:
  lamp.light_energy = light_snapshot[lamp][0]*0.15
  lamp.light_color = Color(0.8,0.12,0.2)
 for actor in $Performance/Dancers.get_children(): actor.performance = true
 $Performance/AnimationPlayer.play("show")
 $Performance/AnimationPlayer.pause()
 dance_time = 0.0
 next_cue = 0
 hits = 0
 cue_hit = false
 Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
 rhythm.show()
 rhythm.get_node("Watch").text = "Играть в ритм" if watch_only else "Просто смотреть"
 refresh()
 ui.get_node("Prompt").text = ""
 ui.get_node("Subtitle").text = "Даша · гулька и чёрный платок на бедре"
 save_progress()
 # Render the new materials before starting the recording's audio clock.
 if DisplayServer.get_name() == "headless": await get_tree().process_frame
 else: await RenderingServer.frame_post_draw
 if not is_inside_tree() or stage != Stage.DANCE: return
 $DanceAudio.play()

func update_dance() -> void:
 $Performance/AnimationPlayer.seek(minf(dance_time,131.74),true)
 if not flow.motion: $Performance/ShowCamera.position = Vector3(0,4.1,7.6)
 while next_cue < cues.size() and dance_time > float(cues[next_cue])+0.42:
  next_cue += 1
  cue_hit = false
 ui.get_node("Subtitle").text = "Даша · гулька и чёрный платок на бедре" if dance_time < 6 else ""
 if watch_only:
  rhythm.get_node("Cue").text = "Вместе на сцене"
  rhythm.get_node("Timing").value = dance_time/131.743333
  rhythm.get_node("Feedback").text = "Можно просто прожить этот момент"
  return
 if next_cue >= cues.size():
  rhythm.get_node("Cue").text = "Последнее движение…"
  return
 var remaining := float(cues[next_cue])-dance_time
 rhythm.get_node("Cue").text = ("←   A / ←" if next_cue%2 == 0 else "D / →   →") if remaining < 1.5 else "Слушай музыку"
 rhythm.get_node("Timing").value = clampf(1.0-remaining/1.5,0,1)
 if not cue_hit: rhythm.get_node("Feedback").text = "Сейчас" if absf(remaining)<0.42 else "Нажми, когда полоска заполнится"

func hit_cue(lane: int) -> bool:
 if stage != Stage.DANCE or watch_only or cue_hit or next_cue >= cues.size(): return false
 if lane == next_cue%2 and absf(dance_time-float(cues[next_cue])) <= 0.42:
  cue_hit = true
  hits += 1
  rhythm.get_node("Feedback").text = "Вместе!"
  return true
 return false

func finish_dance() -> void:
 if stage != Stage.DANCE: return
 $DanceAudio.stop()
 $Performance/AnimationPlayer.stop()
 stage = Stage.CELEBRATION
 refresh()
 save_progress()
 begin_celebration()

func begin_celebration() -> void:
 busy = true
 lock_player(true)
 rhythm.hide()
 ui.get_node("Subtitle").text = ""
 ui.get_node("Curtain/Title").text = "После концерта"
 ui.get_node("Curtain/Small").text = "Последний номер позади. Теперь мы снова все вместе."
 ui.get_node("Curtain").show()
 await get_tree().create_timer(2.5,false).timeout
 if not is_inside_tree(): return
 show_final_group()
 ui.get_node("Curtain").hide()
 show_dialog("Все вместе","Музыка закончилась. Остались шум голосов, улыбки и ощущение, что мы всё-таки сделали это.",["Вместе со всеми: «ФКН!»","Посмотреть финал"],func(_choice): celebrate())

func show_final_group() -> void:
 player.hide()
 $Cast.hide()
 $Performance.hide()
 $Celebration.show()
 $FinalCamera.make_current()
 for lamp in light_snapshot:
  lamp.light_energy = light_snapshot[lamp][0]
  lamp.light_color = light_snapshot[lamp][1]
 level.get_node("BakedLighting").show()
 level.get_node("Architecture/Stage/ProjectionScreen").show()
 level.get_node("BackstageProps").show()
 level.get_node("Equipment/Microphone").show()

func celebrate() -> void:
 busy = true
 lock_player(true)
 $Applause.play()
 for actor in $Celebration.get_children(): actor.celebrating = true
 ui.get_node("Subtitle").text = "ФКН!   ФКН!   ФКН!"
 await get_tree().create_timer(5.5,false).timeout
 if not is_inside_tree(): return
 ui.get_node("Subtitle").text = ""
 stage = Stage.DONE
 refresh()
 if save_progress():
  flow.mark_finished(0)
  flow.persist()
 ending_dialog()

func ending_dialog() -> void:
 show_dialog("Даша · наш день","День пролетел скомканно и слишком быстро. Но этот момент — мы все вместе — останется.",["Дальше по нити воспоминаний","Ещё немного побыть в этом моменте"],func(choice):
  busy = true
  lock_player(true)
  Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
  ui.get_node("FinalReturn").show()
  if choice == 0: flow.finish_and_return()
  else: ui.get_node("FinalReturn").grab_focus())

func notice(text: String,seconds: float = 4.0) -> void:
 ui.get_node("Notice").text = text
 notice_time = seconds

func _exit_tree() -> void:
 for sound in [$DanceAudio,$Choice,$Cloth,$Success,$Applause,$Footstep]: sound.stop()
