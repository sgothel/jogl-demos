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

import javax.media.opengl.*;
import com.sun.opengl.util.texture.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class Engine {

  private Texture texture;
  private List/*<Particle>*/ particles;
  private String path;
  public RGBA tendToColor;
    
  public Engine(int numParticles, String path) {
    this.path=path;
        
    tendToColor = new RGBA(1.0f, 1.0f, 1.0f, 1.0f);
        
    particles = new ArrayList/*<Particle>*/(numParticles);
    for(int i=0; i<numParticles; i++)
      particles.add(new Particle());
  }
    
  public void addParticle() {
    particles.add(new Particle());
  }
    
  public void removeParticle() {
    if(particles.size()-1 >= 0)
      particles.remove(particles.size()-1);
  }
    
  public void draw(GL gl) {
        
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    gl.glLoadIdentity();      
        
    for(int i=0; i<particles.size(); i++) {
      ((Particle) particles.get(i)).draw(gl, texture, tendToColor);
    }
  }
    
  public void init() {               
    try {
      ClassLoader c1 = this.getClass().getClassLoader();
      URL url = c1.getResource(path);
      texture = TextureIO.newTexture(url, false, null);
      texture.enable();
    }
    catch(IOException e) {
      e.printStackTrace();
    }
    catch(GLException e) {
      e.printStackTrace();
    }
  }
    
  public void reset() {
    int numParticles = particles.size();
    particles = new ArrayList/*<Particle>*/(numParticles);
    for(int i=0; i<numParticles; i++)
      particles.add(new Particle());
  }

}
