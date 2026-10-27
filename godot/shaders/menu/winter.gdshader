shader_type canvas_item;
render_mode unshaded;
uniform bool motion = true;
void fragment() {
    vec2 p = UV;
    float t = motion ? TIME * 0.025 : 0.0;
    vec3 col = mix(vec3(0.025,0.055,0.085), vec3(0.09,0.18,0.20), 1.0-p.y);
    col += vec3(0.13,0.075,0.025) * exp(-length((p-vec2(0.78,0.24))*vec2(1.3,1.0))*4.0);
    for (int i=0; i<3; i++) {
        float layer=float(i)+1.0;
        vec2 q=p*vec2(40.0,24.0)*layer-vec2(t*layer,t*layer*3.0);
        vec2 cell=floor(q);
        float seed=fract(sin(dot(cell,vec2(12.9898,78.233)))*43758.5453);
        float flake=(1.0-smoothstep(0.02,0.09,length(fract(q)-vec2(seed,0.5))))*step(0.82,seed);
        col += vec3(0.7,0.81,0.85)*flake*0.28/layer;
    }
    COLOR=vec4(col,1.0);
}
