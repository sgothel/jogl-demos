package demos.es2.perftst;

import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.util.*;
import javax.media.opengl.glsl.*;

import com.sun.javafx.newt.*;

public class PerfVBOLoad extends PerfModule {

    public PerfVBOLoad() {
    }

    public void initShaderState(GL2ES2 gl) {
        initShaderState(gl, "vbo-vert-col", "fcolor");
    }

    protected void runOneSet(GLAutoDrawable drawable, int dataType, int numObjs, int numVertices, int loops) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        // 
        // data setup
        //

        GLArrayDataServer[] vertices = new GLArrayDataServer[numObjs];
        GLArrayDataServer[] colors   = new GLArrayDataServer[numObjs];

        float x=0f, y=0f, z=0f;
        float r=1f, g=1f, b=1f;

        for(int i=0; i<numObjs; i++) {
            vertices[i] = GLArrayDataServer.createGLSL("mgl_Vertex", 3, dataType, true, numVertices, GL.GL_STATIC_DRAW);
            {
                Buffer verticeb = vertices[i].getBuffer();
                for(int j=0; j<numVertices; j++) {
                    // Fill them up
                    put(verticeb, dataType, x);
                    put(verticeb, dataType, y);
                    put(verticeb, dataType, z);
                    if(x==0f) x=1f;
                    else if(x==1f) { x=0f; y+=0.01f; }
                    if(y>1f) { x=0f; y=0f; z+=0.01f; }
                }
            }
            colors[i] = GLArrayDataServer.createGLSL("mgl_Color",  4, dataType, true, numVertices, GL.GL_STATIC_DRAW);
            {
                // Fill them up
                Buffer colorb = colors[i].getBuffer();
                for(int j =0; j<numVertices; j++) {
                    put(colorb, dataType, r);
                    put(colorb, dataType, g);
                    put(colorb, dataType, b);
                    put(colorb, dataType, 1f-(float)i/10);
                    if(r<=1f) r+=0.01f;
                    else if(g<=1f) g+=0.01f;
                    else if(b<=1f) b+=0.01f;
                    else { r=0f; g=0f; b=0f; } 
                }
            }
        }

        // 
        // run loops
        //

        long dtC, dt, dt2, dt3, dtF, dtS, dtT;
        long tStart;
        long[] tC = new long[loops];
        long[] t0 = new long[loops];
        long[][] t1 = new long[loops][numObjs];
        long[][] t2 = new long[loops][numObjs];
        long[][] t3 = new long[loops][numObjs];
        long[] tF = new long[loops];
        long[] tS = new long[loops];

        // Push the 1st uniform down the path 
        st.glUseProgram(gl, true);

        tStart = System.currentTimeMillis();

        for(int i=0; i<loops; i++) {
            tC[i] = System.currentTimeMillis();

            gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

            t0[i] = System.currentTimeMillis();

            for(int j=0; j<numObjs; j++) {
                if(i==0) {
                    vertices[j].seal(gl, true);
                } else {
                    vertices[j].enableBuffer(gl, true);
                }

                t1[i][j] = System.currentTimeMillis();

                if(i==0) {
                    colors[j].seal(gl, true);
                } else {
                    colors[j].enableBuffer(gl, true);
                }

                t2[i][j] = System.currentTimeMillis();

                gl.glDrawArrays(GL.GL_LINE_STRIP, 0, vertices[j].getElementNumber());
                vertices[j].enableBuffer(gl, false);
                colors[j].enableBuffer(gl, false);

                t3[i][j] = System.currentTimeMillis();
            }

            gl.glFinish();

            tF[i] = System.currentTimeMillis();

            drawable.swapBuffers();

            tS[i] = System.currentTimeMillis();
        }

        dt = tS[loops-1] - tStart;
        int verticesElements = vertices[0].getElementNumber() * numObjs;
        int verticesBytes    = verticesElements * vertices[0].getComponentSize()* vertices[0].getComponentNumber();
        int colorsElements   = colors[0].getElementNumber()   * colors.length;
        int colorsBytes      = colorsElements * colors[0].getComponentSize()* colors[0].getComponentNumber();
        System.out.println("");

        System.out.println("Loops "+loops+", objects "+numObjs+", type "+getTypeName(dataType)+
                           ", vertices p.o. "+vertices[0].getElementNumber()+
                           ", colors p.o. "+colors[0].getElementNumber()+
                           ",\n total elements "+(verticesElements+colorsElements)+
                           ", total bytes "+(verticesBytes+colorsBytes)+", total time: "+dt +
                           "ms, fps: "+((loops*1000)/dt)+
                           ",\n col.vert./s: " + ((double)(loops*verticesElements)/((double)dt/1000.0)));

        for(int i=0; i<loops; i++) {
            dtC= t0[i] - tC[i];
            dtF= tF[i] - t3[i][numObjs-1];
            dtS= tS[i] - tF[i];
            dtT= tS[i] - tC[i];
            if(dtT<=0) dtT=1;
            System.out.println("\tloop "+i+": clear "+dtC+"ms, finish "+dtF+", swap "+dtS+"ms, total: "+ dtT+"ms, fps "+1000/dtT);
            /*
            for(int j=0; j<numObjs; j++) {
                dt = t1[i][j] - t0[i];
                dt2= t2[i][j] - t1[i][j];
                dt3= t3[i][j] - t2[i][j];
                dtT= dt+dt2+dt3;
                System.out.println("\t\tobj "+j+": vertices "+dt +"ms, colors "+dt2+"ms, draw "+dt3+"ms, total: "+ dtT);
            } */
        }
        System.out.println("*****************************************************************");


        st.glUseProgram(gl, false);

        try {
            Thread.sleep(1000);
        } catch (Exception e) {}
    }

    protected void runOneSet(GLAutoDrawable drawable, int numObjs, int numVertices, int loops) {
        runOneSet(drawable, GL.GL_UNSIGNED_BYTE, numObjs, numVertices, loops);
        runOneSet(drawable, GL.GL_BYTE, numObjs, numVertices, loops);
        runOneSet(drawable, GL.GL_UNSIGNED_SHORT, numObjs, numVertices, loops);
        runOneSet(drawable, GL.GL_SHORT, numObjs, numVertices, loops);
        runOneSet(drawable, GL.GL_FLOAT, numObjs, numVertices, loops);

        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(gl.isGLES2()) {
            runOneSet(drawable, GL.GL_FIXED, numObjs, numVertices, loops);
        }
    }

    public void run(GLAutoDrawable drawable, int loops) {
        runOneSet(drawable, 1,    100, loops);
        runOneSet(drawable, 3,    100, loops);

        runOneSet(drawable, 1,   1000, loops);
        runOneSet(drawable, 3,   1000, loops);

        runOneSet(drawable, 1,  10000, loops);
        runOneSet(drawable, 3,  10000, loops);

        runOneSet(drawable, 1, 100000, loops);
        runOneSet(drawable, 3, 100000, loops);
    }

}
