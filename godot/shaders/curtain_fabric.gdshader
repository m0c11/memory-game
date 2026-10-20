shader_type spatial;
render_mode diffuse_burley, specular_schlick_ggx;

uniform sampler2D fabric_color : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D fabric_normal : hint_normal, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D fabric_orm : filter_linear_mipmap_anisotropic, repeat_enable;
uniform float fabric_height : hint_range(0.1, 8.0) = 3.8;
uniform float sway_amplitude : hint_range(0.0, 0.05, 0.001) = 0.022;
uniform float sway_speed : hint_range(0.0, 2.0, 0.01) = 0.75;
uniform float phase_offset : hint_range(0.0, 6.283) = 0.0;
uniform float normal_strength : hint_range(0.0, 1.0) = 0.3;

void vertex() {
	// The fabric and its hem share local coordinates. Both move together;
	// the hanging rail and rings keep their original materials and position.
	float free_length = clamp(1.0 - VERTEX.y / fabric_height, 0.0, 1.0);
	float weight = free_length * free_length;
	float phase_a = TIME * sway_speed + VERTEX.x * 1.9 + phase_offset;
	float phase_b = TIME * sway_speed * 1.37 + VERTEX.y * 1.6 + VERTEX.x * 0.8 + phase_offset;
	float wave = 0.7 * sin(phase_a) + 0.3 * sin(phase_b);
	float slope_x = sway_amplitude * weight * (1.33 * cos(phase_a) + 0.24 * cos(phase_b));
	float slope_y = sway_amplitude * (
		-2.0 * free_length / fabric_height * wave + weight * 0.48 * cos(phase_b)
	);

	VERTEX.z += sway_amplitude * weight * wave;
	// Keep lighting and the normal map aligned with the deformed surface.
	NORMAL = normalize(vec3(NORMAL.x - slope_x * NORMAL.z, NORMAL.y - slope_y * NORMAL.z, NORMAL.z));
	TANGENT = normalize(vec3(TANGENT.xy, TANGENT.z + dot(vec2(slope_x, slope_y), TANGENT.xy)));
	BINORMAL = normalize(vec3(BINORMAL.xy, BINORMAL.z + dot(vec2(slope_x, slope_y), BINORMAL.xy)));
}

void fragment() {
	vec3 orm = texture(fabric_orm, UV).rgb;
	ALBEDO = texture(fabric_color, UV).rgb;
	ROUGHNESS = orm.g;
	METALLIC = orm.b;
	NORMAL_MAP = texture(fabric_normal, UV).rgb;
	NORMAL_MAP_DEPTH = normal_strength;
}
