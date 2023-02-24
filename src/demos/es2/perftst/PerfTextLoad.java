package demos.es2.perftst;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.FloatBuffer;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class PerfTextLoad extends PerfModule {
    static final int MAX_TEXTURE_ENGINES = 8;

    public PerfTextLoad() {
    }

    @Override
	public ShaderState initShaderState(final GL2ES2 gl) {
        return initShaderState(gl, "vbo-vert-text", "ftext");
    }

    Texture[] textures = null;
    TextureData[] textDatas = null;

    protected void runOneSet(final GLAutoDrawable drawable, final String textBaseName, final int numObjs, final int numTextures, final int loops) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(numTextures>MAX_TEXTURE_ENGINES) {
            throw new GLException("numTextures must be within 1.."+MAX_TEXTURE_ENGINES);
        }

        String textName = null;
        textDatas = new TextureData[numObjs];
        textures = new Texture[numTextures];
        try {
            for(int i=0; i<numObjs; i++) {
                textName = "data/"+textBaseName+"."+(i+1)+".tga";
                final URLConnection connText = IOUtil.getResource(textName, Perftst.class.getClassLoader(), Perftst.class);
                if(connText==null) {
                    throw new RuntimeException("couldn't fetch "+textName);
                }
                final InputStream in = connText.getInputStream();
                try {
                    textDatas[i] = TextureIO.newTextureData(gl.getGLProfile(), in, false, TextureIO.TGA);
                    System.out.println(textBaseName+": "+textDatas[i]);
                } finally {
                    IOUtil.close(in, false);
                }
            }

            for(int i=0; i<numTextures; i++) {
                gl.glActiveTexture(i);
                textures[i] = new Texture(GL.GL_TEXTURE_2D);
            }
        } catch (final IOException ioe) {
            System.err.println("couldn't fetch "+textName);
            throw new RuntimeException(ioe);
        }

        //
        // Vertices Data setup
        //

        st.useProgram(gl, true);

        final GLArrayDataServer vertices = GLArrayDataServer.createGLSL("mgl_Vertex", 2, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
        {
            final FloatBuffer vb = (FloatBuffer)vertices.getBuffer();
            vb.put(0f); vb.put(0f);
            vb.put(1f); vb.put(0f);
            vb.put(0f); vb.put(1f);
            vb.put(1f); vb.put(1f);
        }
        vertices.seal(gl, true);

        final GLArrayDataServer texCoords = GLArrayDataServer.createGLSL("mgl_MultiTexCoord0",  2, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
        {
            final FloatBuffer cb = (FloatBuffer)texCoords.getBuffer();
            cb.put(0f); cb.put(0f);
            cb.put(1f); cb.put(0f);
            cb.put(0f); cb.put(1f);
            cb.put(1f); cb.put(1f);
        }
        texCoords.seal(gl, true);

        //
        // texture setup
        //
        final long[] tU = new long[numObjs+1];
        tU[0] = System.currentTimeMillis();
        for(int j=0; j<numTextures; j++) {
            gl.glActiveTexture(j);
            textures[j].updateImage(gl, textDatas[0]);
            tU[j+1] = System.currentTimeMillis();
        }

        final GLUniformData activeTexture = new GLUniformData("mgl_ActiveTexture", 0);
        st.uniform(gl, activeTexture);

        //
        // run loops
        //

        long dtC, dt;
        final long dt2, dt3;
        long dtF, dtS, dtT;
        final long[][] tC = new long[loops][numObjs];
        final long[][] t0 = new long[loops][numObjs];
        final long[][][] t1 = new long[loops][numObjs][numTextures];
        final long[][][] t2 = new long[loops][numObjs][numTextures];
        final long[][][] t3 = new long[loops][numObjs][numTextures];
        final long[][] tF = new long[loops][numObjs];
        final long[][] tS = new long[loops][numObjs];

        for(int i=0; i<loops; i++) {
            for(int j=0; j<numObjs; j++) {
                tC[i][j] = System.currentTimeMillis();

                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                t0[i][j] = System.currentTimeMillis();

                for(int k=0; k<numTextures; k++) {
                    gl.glActiveTexture(GL.GL_TEXTURE0+k);
                    textures[k].enable(gl);
                    textures[k].bind(gl);
                    activeTexture.setData(k);
                    st.uniform(gl, activeTexture);

                    t1[i][j][k] = System.currentTimeMillis();

                    textures[k].updateSubImage(gl, textDatas[j], 0, 0, 0);

                    t2[i][j][k] = System.currentTimeMillis();

                    gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, vertices.getElemCount());

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
                           ",\n text kB/s: " + ( (loops*textBytes/1024.0) / (dt/1000.0) ) );

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

        st.useProgram(gl, false);

        for(int i=0; i<numTextures; i++) {
            textures[i].disable(gl);
            textures[i].destroy(gl);
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
        } catch (final Exception e) {}
        System.gc();
    }

    @Override
	public void run(final GLAutoDrawable drawable, final int loops) {
        runOneSet(drawable, "bob2.64x64", 33, 1, loops);
        runOneSet(drawable, "bob2.128x128", 33, 1, loops);
        runOneSet(drawable, "bob2.128x128",  4, 1, loops);
        runOneSet(drawable, "bob2.256x256",  4, 1, loops);
        runOneSet(drawable, "bob2.512x512",  4, 1, loops);
    }

}
