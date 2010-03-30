package demos.es2.perftst;

import java.nio.*;
import java.io.*;
import java.net.*;
import javax.media.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.*;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

public class PerfTextLoad extends PerfModule {
    static final int MAX_TEXTURE_ENGINES = 8;

    public PerfTextLoad() {
    }

    public void initShaderState(GL2ES2 gl) {
        initShaderState(gl, "vbo-vert-text", "ftext");
    }

    Texture[] textures = null;
    TextureData[] textDatas = null;

    protected void runOneSet(GLAutoDrawable drawable, String textBaseName, int numObjs, int numTextures, int loops) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(numTextures>MAX_TEXTURE_ENGINES) {
            throw new GLException("numTextures must be within 1.."+MAX_TEXTURE_ENGINES);
        }

        String textName = null;
        textDatas = new TextureData[numObjs];
        textures = new Texture[numTextures];
        try {
            for(int i=0; i<numObjs; i++) {
                textName = "data/"+textBaseName+"."+(i+1)+".tga";
                URL urlText = Locator.getResource(Perftst.class, textName);
                if(urlText==null) {
                    throw new RuntimeException("couldn't fetch "+textName);
                }
                textDatas[i] = TextureIO.newTextureData(gl.getGLProfile(), urlText.openStream(), false, TextureIO.TGA);
                System.out.println(textBaseName+": "+textDatas[i]);
            }

            for(int i=0; i<numTextures; i++) {
                gl.glActiveTexture(i);
                textures[i] = new Texture(GL.GL_TEXTURE_2D);
            }
        } catch (IOException ioe) {
            System.err.println("couldn't fetch "+textName);
            throw new RuntimeException(ioe);
        }

        // 
        // Vertices Data setup
        //

        st.glUseProgram(gl, true);

        GLArrayDataServer vertices = GLArrayDataServer.createGLSL(gl, "mgl_Vertex", 2, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
        {
            FloatBuffer vb = (FloatBuffer)vertices.getBuffer();
            vb.put(0f); vb.put(0f);
            vb.put(1f); vb.put(0f);
            vb.put(0f); vb.put(1f);
            vb.put(1f); vb.put(1f);
        }
        vertices.seal(gl, true);

        GLArrayDataServer texCoords = GLArrayDataServer.createGLSL(gl, "mgl_MultiTexCoord0",  2, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
        {
            FloatBuffer cb = (FloatBuffer)texCoords.getBuffer();
            cb.put(0f); cb.put(0f);
            cb.put(1f); cb.put(0f);
            cb.put(0f); cb.put(1f);
            cb.put(1f); cb.put(1f);
        }
        texCoords.seal(gl, true);

        //
        // texture setup
        //
        long[] tU = new long[numObjs+1];
        tU[0] = System.currentTimeMillis();
        for(int j=0; j<numTextures; j++) {
            gl.glActiveTexture(j);
            textures[j].updateImage(textDatas[0]);
            tU[j+1] = System.currentTimeMillis();
        }

        GLUniformData activeTexture = new GLUniformData("mgl_ActiveTexture", 0);
        st.glUniform(gl, activeTexture);
    
        // 
        // run loops
        //

        long dtC, dt, dt2, dt3, dtF, dtS, dtT;
        long[][] tC = new long[loops][numObjs];
        long[][] t0 = new long[loops][numObjs];
        long[][][] t1 = new long[loops][numObjs][numTextures];
        long[][][] t2 = new long[loops][numObjs][numTextures];
        long[][][] t3 = new long[loops][numObjs][numTextures];
        long[][] tF = new long[loops][numObjs];
        long[][] tS = new long[loops][numObjs];

        for(int i=0; i<loops; i++) {
            for(int j=0; j<numObjs; j++) {
                tC[i][j] = System.currentTimeMillis();

                gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

                t0[i][j] = System.currentTimeMillis();

                for(int k=0; k<numTextures; k++) {
                    gl.glActiveTexture(GL.GL_TEXTURE0+k);
                    textures[k].enable();
                    textures[k].bind();
                    activeTexture.setData(k);
                    st.glUniform(gl, activeTexture);

                    t1[i][j][k] = System.currentTimeMillis();

                    textures[k].updateSubImage(textDatas[j], 0, 0, 0);

                    t2[i][j][k] = System.currentTimeMillis();

                    gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, vertices.getElementNumber());

                    t3[i][j][k] = System.currentTimeMillis();
                }
                gl.glFinish();

                tF[i][j] = System.currentTimeMillis();

                drawable.swapBuffers();

                tS[i][j] = System.currentTimeMillis();

                /*try {
                    Thread.sleep(100);
                } catch (Exception e) {} */
            }

        }

        int textBytes = 0;
        for(int j=0; j<numObjs; j++) {
            textBytes += textDatas[j].getEstimatedMemorySize();
        }
        textBytes*=numTextures;

        dt = 0;
        for(int i=1; i<loops; i++) {
            for(int j=0; j<numObjs; j++) {
                dt += tS[i][j] - tC[i][j];
            }
        }

        System.out.println("");
        System.out.println("Texture "+textBaseName+", loops "+loops+", textures "+numTextures+", objects "+numObjs+
                           ", total bytes "+textBytes+", total time: "+dt +
                           "ms, fps(-1): "+(((loops-1)*numObjs*1000)/dt)+
                           ",\n text kB/s: " + ( ((double)(loops*textBytes)/1024.0) / ((double)dt/1000.0) ) );

        for(int i=0; i<loops; i++) {
            dtC = 0;
            dtF = 0;
            dtS = 0;
            dtT = 0;
            for(int j=0; j<numObjs; j++) {
                dtC += t0[i][j] - tC[i][j];
                dtF += tF[i][j] - t3[i][j][numTextures-1];
                dtS += tS[i][j] - tF[i][j];
                dtT += tS[i][j] - tC[i][j];
            }
            if(dtT<=0) dtT=1;
            System.out.println("\tloop "+i+": clear "+dtC+"ms, finish "+dtF+", swap "+dtS+"ms, total: "+ dtT+"ms, fps "+(numObjs*1000)/dtT);
            /*
            for(int j=0; j<dummyUni.length; j++) {
                dt = t1[i][j] - t0[i];
                dt2= t2[i][j] - t1[i][j];
                dt3= t3[i][j] - t2[i][j];
                dtT= dt+dt2+dt3;
                System.out.println("\t\tobj "+j+": setup "+dt +"ms, update "+dt2 +"ms, draw "+dt3+"ms, total: "+ dtT);
            } */
        }
        System.out.println("*****************************************************************");

        st.glUseProgram(gl, false);

        for(int i=0; i<numTextures; i++) {
            textures[i].disable();
            textures[i].dispose();
            textures[i]=null;
        }
        for(int i=0; i<numObjs; i++) {
            textDatas[i] = null;
        }
        textures=null;
        textDatas=null;
        System.gc();
        try {
            Thread.sleep(100);
        } catch (Exception e) {}
        System.gc();
    }

    public void run(GLAutoDrawable drawable, int loops) {
        runOneSet(drawable, "bob2.64x64", 33, 1, loops);
        runOneSet(drawable, "bob2.128x128", 33, 1, loops);
        runOneSet(drawable, "bob2.128x128",  4, 1, loops);
        runOneSet(drawable, "bob2.256x256",  4, 1, loops);
        runOneSet(drawable, "bob2.512x512",  4, 1, loops);
    }

}
