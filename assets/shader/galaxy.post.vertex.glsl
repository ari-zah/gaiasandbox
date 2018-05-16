#ifdef GL_ES
	#define PRECISION mediump
	precision PRECISION float;
#else
	#define PRECISION
#endif

attribute vec4 a_position;
attribute vec2 a_texCoord0;
varying vec2 v_texcoord;

void main()
{
	v_texcoord = a_texCoord0;
	gl_Position = a_position;
}