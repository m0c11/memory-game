shader_type canvas_item;
render_mode unshaded;
uniform sampler2D screen_texture : hint_screen_texture, repeat_disable, filter_nearest;
uniform float strength = 1.0;
void fragment() {
 vec3 c = texture(screen_texture, SCREEN_UV).rgb;
 float edge = smoothstep(0.3, 0.73, length(SCREEN_UV - vec2(0.5)));
 c *= 1.0 - edge * 0.12 * strength;
 COLOR = vec4(c, 1.0);
}
