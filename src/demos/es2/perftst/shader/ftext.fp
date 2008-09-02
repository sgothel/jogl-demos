
#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
#else
  #define MEDIUMP
  #define HIGHP
#endif

varying   vec4          mgl_TexCoord0;
uniform   sampler2D     mgl_ActiveTexture;

void main (void)
{
    gl_FragColor = vec4(texture2D(mgl_ActiveTexture, mgl_TexCoord0.st).rgb, 1.0);
}

