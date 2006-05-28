/*
 * Copyright (c) 2006 Ben Chappell (bwchappell@gmail.com) All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *    
 * - Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *   
 * The names of Ben Chappell, Sun Microsystems, Inc. or the names of
 * contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *    
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. BEN CHAPPELL,
 * SUN MICROSYSTEMS, INC. ("SUN"), AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL BEN
 * CHAPPELL, SUN, OR SUN'S LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT 
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF BEN
 * CHAPPELL OR SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *   
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package demos.particles.engine;

import com.sun.opengl.util.texture.*;

import javax.media.opengl.*;

public class Particle {
  private XYZ currentPos;
  private RGBA rgba;
    
  public Particle() {
    currentPos = new XYZ((float)Math.random(), 
                         (float)Math.random(), 
                         -30.0f               
                         );
        
    rgba = rgba = new RGBA((float)Math.random(), 
                           (float)Math.random(), 
                           (float)Math.random(),
                           (float)Math.random());
  }
    
  public void draw(GL gl, Texture texture, RGBA tendToColor) {
    adjust(tendToColor);
    texture.bind();
    gl.glColor4f(rgba.r,rgba.g,rgba.b,rgba.a);
        
    gl.glBegin(GL.GL_QUADS);
    gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(currentPos.x, currentPos.y-2, currentPos.z);
    gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f(currentPos.x+2, currentPos.y-2, currentPos.z);
    gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f(currentPos.x+2, currentPos.y, currentPos.z);
    gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(currentPos.x, currentPos.y, currentPos.z);
    gl.glEnd();
  }
    
  private void tendToColor(RGBA tendToColor) {
    float red = 0.0f;
    float blue = 0.0f;
    float green = 0.0f;
    float sign=1.0f;

    if(Math.random()>=0.5)
      sign=-1.0f;

    // RED
    if(tendToColor.r <= 1-tendToColor.r) 
      red = tendToColor.r;
    else 
      red = 1-tendToColor.r;

    red = (float)(Math.random()*red*sign+tendToColor.r);

    // GREEN
    if(tendToColor.g <= 1-tendToColor.g) 
      green = tendToColor.g;            
    else 
      green = 1-tendToColor.g;

    green =(float)(Math.random()*green*sign+tendToColor.g);

    // BLUE
    if(tendToColor.b <= 1-tendToColor.b) 
      blue = tendToColor.b;            
    else 
      blue = 1-tendToColor.b;

    blue = (float)(Math.random()*blue*sign+tendToColor.b);

    rgba = new RGBA(red, green, blue, (float)Math.random());
  }
    
  private void tendToPos() {
    XYZ xyz = new XYZ((float)Math.random()-0.5f,(float)Math.random()-0.5f,(float)Math.random()-0.5f);
    currentPos.add(xyz);
  }
    
  private void adjust(RGBA tendToColor) {
    tendToPos();
        
    rgba.a-=Math.random()/100;
    if(rgba.a<=0)
      tendToColor(tendToColor);
  }
    
}
