package demos.es2.perftst;

import com.jogamp.common.nio.Buffers;
import java.nio.*;
import javax.media.opengl.*;
import com.jogamp.opengl.util.*;


public class PerfUniLoad extends PerfModule {
    static final int MAX_ARRAYS = 12;
    static final int MAX_ARRAY_ELEM = 16;

    GLUniformData[] dummyA, dummyB, dummyC;
    final int dataType=GL.GL_FLOAT;
    
    public PerfUniLoad() {
    }

    public void initShaderState(GL2ES2 gl) {
        initShaderState(gl, "uni-vert-col", "fcolor");
    }

    protected void runOneSet(GLAutoDrawable drawable, int numObjs, int numArrayElem, int loops) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        // 
        // Vertices Data setup
        //

        if(numObjs>MAX_ARRAYS) {
            throw new GLException("numObjs must be within 0.."+MAX_ARRAYS);
        }

        if(numArrayElem>MAX_ARRAY_ELEM) {
            throw new GLException("numArrayElem must be within 0.."+MAX_ARRAY_ELEM);
        }

        st.glUseProgram(gl, true);

        GLArrayDataServer vertices = GLArrayDataServer.createGLSL(gl, "mgl_Vertex", 3, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
        {
            FloatBuffer vb = (FloatBuffer)vertices.getBuffer();
            vb.put(0f); vb.put(0f); vb.put(0f);
            vb.put(1f); vb.put(0f); vb.put(0f);
            vb.put(0f); vb.put(1f); vb.put(0f);
            vb.put(1f); vb.put(1f); vb.put(0f);
        }
        vertices.seal(gl, true);

        GLArrayDataServer colors = GLArrayDataServer.createGLSL(gl, "mgl_Color",  4, GL.GL_FLOAT, true, 4, GL.GL_STATIC_DRAW);
        {
            FloatBuffer cb = (FloatBuffer)colors.getBuffer();
            cb.put(0f); cb.put(0f); cb.put(0f); cb.put(1f);
            cb.put(1f); cb.put(0f); cb.put(0f); cb.put(1f);
            cb.put(0f); cb.put(1f); cb.put(0f); cb.put(1f);
            cb.put(0f); cb.put(0f); cb.put(1f); cb.put(1f);
        }
        colors.seal(gl, true);

        //
        // Uniform Data setup
        //

        GLUniformData[] dummyUni = new GLUniformData[numObjs];

        float x=0f, y=0f, z=0f, w=0f;

        for(int i=0; i<numObjs; i++) {
            FloatBuffer fb = Buffers.newDirectFloatBuffer(4*numArrayElem);

            for(int j=0; j<numArrayElem; j++) {
                // Fill them up
                fb.put(x);
                fb.put(y);
                fb.put(z);
                fb.put(w);
                if(x==0f) x=1f;
                else if(x==1f) { x=0f; y+=0.01f; }
                if(y>1f) { x=0f; y=0f; z+=0.01f; }
            }
            fb.flip();

            dummyUni[i] = new GLUniformData("mgl_Dummy"+i, 4, fb);
        }

        // 
        // run loops
        //

        long dtC, dt, dt2, dt3, dtF, dtS, dtT;
        long[] tC = new long[loops];
        long[] t0 = new long[loops];
        long[][] t1 = new long[loops][numObjs];
        long[][] t2 = new long[loops][numObjs];
        long[] tF = new long[loops];
        long[] tS = new long[loops];

        for(int i=0; i<loops; i++) {
            tC[i] = System.currentTimeMillis();

            gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

            t0[i] = System.currentTimeMillis();

            for(int j=0; j<numObjs; j++) {
                st.glUniform(gl, dummyUni[j]);

                t1[i][j] = System.currentTimeMillis();

                gl.glDrawArrays(GL.GL_LINE_STRIP, 0, vertices.getElementNumber());

                t2[i][j] = System.currentTimeMillis();
            }

            gl.glFinish();

            tF[i] = System.currentTimeMillis();

            drawable.swapBuffers();

            tS[i] = System.currentTimeMillis();
        }

        int uniElements = numObjs * numArrayElem ;
        int uniBytes    = uniElements * Buffers.SIZEOF_FLOAT;

        dt = 0;
        for(int i=1; i<loops; i++) {
            dt += tS[i] - tC[i];
        }

        System.out.println("");
        System.out.println("Loops "+loops+", uniform arrays "+dummyUni.length+", type FLOAT"+
                           ", uniforms array size "+numArrayElem+
                           ",\n total elements "+uniElements+
                           ", total bytes "+uniBytes+", total time: "+dt +
                           "ms, fps(-1): "+(((loops-1)*1000)/dt)+
                           ",\n uni elem/s: " + ((double)(loops*uniElements)/((double)dt/1000.0)));

        for(int i=0; i<loops; i++) {
            dtC= t0[i] - tC[i];
            dtF= tF[i] - t2[i][dummyUni.length-1];
            dtS= tS[i] - tF[i];
            dtT= tS[i] - tC[i];
            if(dtT<=0) dtT=1;
            System.out.println("\tloop "+i+": clear "+dtC+"ms, finish "+dtF+", swap "+dtS+"ms, total: "+ dtT+"ms, fps "+1000/dtT);
            /*
            for(int j=0; j<dummyUni.length; j++) {
                dt = t1[i][j] - t0[i];
                dt2= t2[i][j] - t1[i][j];
                dtT= dt+dt2;
                System.out.println("\t\tobj "+j+": uniform "+dt +"ms, draw "+dt2+"ms, total: "+ dtT);
            } */
        }
        System.out.println("*****************************************************************");


        st.glUseProgram(gl, false);

        try {
            Thread.sleep(100);
        } catch (Exception e) {}
    }

    public void run(GLAutoDrawable drawable, int loops) {
        runOneSet(drawable, 1,    1, loops);

        runOneSet(drawable,  4,    1, loops);
        runOneSet(drawable,  1,    4, loops);

        runOneSet(drawable,  8,    1, loops);
        runOneSet(drawable,  1,    8, loops);

        if(MAX_ARRAYS>8) {
            runOneSet(drawable, MAX_ARRAYS,         1, loops);
            runOneSet(drawable, 1,         MAX_ARRAYS, loops);
        }
        runOneSet(drawable,  1,   16, loops);

        runOneSet(drawable,  2,   16, loops);
        runOneSet(drawable,  4,   16, loops);
        runOneSet(drawable,  8,   16, loops);
        if(MAX_ARRAYS>8) {
            runOneSet(drawable, MAX_ARRAYS,   16, loops);
        }
    }

}
