shader_type canvas_item;
uniform vec4 coat_color : source_color = vec4(0.7, 0.3, 0.2, 1.0);
uniform bool recolor = false;
varying vec4 vertex_color;
void vertex() { vertex_color = COLOR; }
void fragment() {
 vec4 c = texture(TEXTURE, UV);
 if (recolor && c.g > c.r * 1.15 && c.b > c.r * 1.1 && c.g > c.b * 1.015) {
  float shade = max(c.g, c.b);
  c.rgb = coat_color.rgb * shade * 1.8;
 }
 COLOR = c * vertex_color;
}
