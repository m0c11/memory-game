shader_type canvas_item;
global uniform float polet_wind;
void vertex() {
 float phase = MODEL_MATRIX[3].x * 0.013 + MODEL_MATRIX[3].y * 0.009;
 VERTEX.x += sin(TIME * 1.1 + phase) * pow(1.0 - UV.y, 2.0) * 0.85 * polet_wind;
}
