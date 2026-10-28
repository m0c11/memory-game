shader_type canvas_item;
render_mode unshaded;
uniform float revealed : hint_range(0.0,1.0) = 0.0;
varying vec4 tint;
void vertex(){ tint = COLOR; }
void fragment(){
 if (UV.x > revealed) discard;
 float braid = 0.84 + 0.16*sin(UV.x*5000.0 + UV.y*12.0);
 COLOR = vec4(tint.rgb*braid,tint.a*smoothstep(0.0,0.25,UV.y)*smoothstep(0.0,0.25,1.0-UV.y));
}
