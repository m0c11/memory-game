shader_type canvas_item;
render_mode unshaded;
uniform float reveal : hint_range(0.0,1.0) = 0.0;
void fragment(){
 float d=length((UV-vec2(0.5,0.45))*vec2(1.5,1.0));
 float shade=smoothstep(0.15,0.65,d)*0.94;
 COLOR=vec4(0.01,0.02,0.03,shade*(1.0-reveal));
}
