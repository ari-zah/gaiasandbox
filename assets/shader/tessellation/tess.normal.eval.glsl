#version 410 core

layout (triangles) in;

#define TEXTURE_LOD_BIAS 0.2

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
uniform float u_vc; // v/c
uniform vec3 u_velDir; // Camera velocity direction

#include shader/lib_geometry.glsl
#include shader/lib_relativity.glsl
#endif // relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
uniform vec4 u_hterms; // hpluscos, hplussin, htimescos, htimessin
uniform vec3 u_gw; // Location of gravitational wave, cartesian
uniform mat3 u_gwmat3; // Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
uniform float u_ts; // Time in seconds since start
uniform float u_omgw; // Wave frequency
#include shader/lib_gravwaves.glsl
#endif // gravitationalWaves


uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

uniform float u_heightScale;
uniform vec2 u_heightSize;
uniform sampler2D u_heightTexture;
uniform sampler2D u_normalTexture;


in float l_opacity[gl_MaxPatchVertices];
in vec2 l_texCoords[gl_MaxPatchVertices];
in vec3 l_normal[gl_MaxPatchVertices];
in vec3 l_viewDir[gl_MaxPatchVertices];
in vec3 l_lightCol[gl_MaxPatchVertices];
in vec3 l_lightDir[gl_MaxPatchVertices];
in vec3 l_ambientLight[gl_MaxPatchVertices];
in vec4 l_atmosphereColor[gl_MaxPatchVertices];
in vec4 l_color[gl_MaxPatchVertices];

#include shader/lib_logdepthbuff.glsl
out float o_depth;
out float o_opacity;
out vec2 o_texCoords;
out vec3 o_normal;
out vec3 o_normalTan;
out vec3 o_viewDir;
out vec3 o_lightCol;
out vec3 o_lightDir;
out vec3 o_ambientLight;
out vec4 o_atmosphereColor;
out vec4 o_color;

vec3 calcNormal(vec2 p, vec2 dp){
    vec4 h;
    const vec2 size = vec2(2.0, 0.0);
    h.x = texture(u_heightTexture, vec2(p.x-dp.x, p.y), TEXTURE_LOD_BIAS).r;
    h.y = texture(u_heightTexture, vec2(p.x+dp.x, p.y), TEXTURE_LOD_BIAS).r;
    h.z = texture(u_heightTexture, vec2(p.x, p.y-dp.y), TEXTURE_LOD_BIAS).r;
    h.w = texture(u_heightTexture, vec2(p.x, p.y+dp.y), TEXTURE_LOD_BIAS).r;
    vec3 va = normalize(vec3(size.xy, h.x - h.y));
    vec3 vb = normalize(vec3(size.yx, h.z - h.w));
    vec3 n = cross(va,vb);
    //vec3 n = vec3(h.x - h.y , h.w - h.z, 0.0);
    return normalize(n);
}

void main(void){
    vec4 pos = (gl_TessCoord.x * gl_in[0].gl_Position +
                    gl_TessCoord.y * gl_in[1].gl_Position +
                    gl_TessCoord.z * gl_in[2].gl_Position);

    o_texCoords = (gl_TessCoord.x * l_texCoords[0] + gl_TessCoord.y * l_texCoords[1] + gl_TessCoord.z * l_texCoords[2]);

    // Normal to apply height
    o_normal = normalize(gl_TessCoord.x * l_normal[0] + gl_TessCoord.y * l_normal[1] + gl_TessCoord.z * l_normal[2]);

    // Use height texture to move vertex along normal
    float h = 1.0 - texture(u_heightTexture, o_texCoords, TEXTURE_LOD_BIAS).r;
    vec3 dh = o_normal * h * u_heightScale;
    pos += vec4(dh, 0.0);


    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves


    gl_Position = u_projViewTrans * pos;

    // Plumbing
    o_normalTan = calcNormal(o_texCoords, vec2(1.0 / u_heightSize.x, 1.0 / u_heightSize.y));
    o_depth = getDepthValue(length(pos.xyz));
    o_opacity = l_opacity[0];
    o_viewDir = (gl_TessCoord.x * l_viewDir[0] + gl_TessCoord.y * l_viewDir[1] + gl_TessCoord.z * l_viewDir[2]);
    o_lightCol = (gl_TessCoord.x * l_lightCol[0] + gl_TessCoord.y * l_lightCol[1] + gl_TessCoord.z * l_lightCol[2]);
    o_lightDir = (gl_TessCoord.x * l_lightDir[0] + gl_TessCoord.y * l_lightDir[1] + gl_TessCoord.z * l_lightDir[2]);
    o_ambientLight = (gl_TessCoord.x * l_ambientLight[0] + gl_TessCoord.y * l_ambientLight[1] + gl_TessCoord.z * l_ambientLight[2]);
    o_atmosphereColor = (gl_TessCoord.x * l_atmosphereColor[0] + gl_TessCoord.y * l_atmosphereColor[1] + gl_TessCoord.z * l_atmosphereColor[2]);
    o_color = l_color[0];
}