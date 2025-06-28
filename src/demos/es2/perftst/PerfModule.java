package demos.es2.perftst;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.math.FixedPoint;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public abstract class PerfModule {

    public abstract ShaderState initShaderState(GL2ES2 gl);

    public abstract void run(GLAutoDrawable drawable, int loops);

    ShaderState st = null;

    public ShaderState getShaderState() { return st; }

    public ShaderState initShaderState(final GL2ES2 gl, final String vShaderName, final String fShaderName) {
        if(st!=null) {
        	return st;
        }

        long t0, t1;

        st = new ShaderState();

        // Create & Compile the shader objects
        final ShaderCode vp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, Perftst.class,
                                            "shader", "shader/bin", vShaderName, false);
        final ShaderCode fp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, Perftst.class,
                                            "shader", "shader/bin", fShaderName, false);

        // Create & Link the shader program
        final ShaderProgram sp = new ShaderProgram();
        sp.add(vp);
        sp.add(fp);

        t0 = System.currentTimeMillis();

        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        t1 = System.currentTimeMillis();

        final long dt = t1-t0;

        System.out.println("shader creation: "+dt+" ms");

        // Let's manage all our states using ShaderState.
        st.attachShaderProgram(gl, sp, true);

        return st;
    }

    public static final void put(final Buffer buffer, final int type, final float v) {
        switch (type) {
            case GL.GL_UNSIGNED_BYTE:
                ((ByteBuffer)buffer).put((byte)(v*0xFF));
                break;
            case GL.GL_BYTE:
                ((ByteBuffer)buffer).put((byte)(v*0x7F));
                break;
            case GL.GL_UNSIGNED_SHORT:
                ((ShortBuffer)buffer).put((short)(v*0xFFFF));
                break;
            case GL.GL_SHORT:
                ((ShortBuffer)buffer).put((short)(v*0x7FFF));
                break;
            case GL.GL_FLOAT:
                ((FloatBuffer)buffer).put(v);
                break;
            case GL.GL_FIXED:
                ((IntBuffer)buffer).put(FixedPoint.toFixed(v));
                break;
        }
    }

    public static final String getTypeName(final int type) {
        switch (type) {
            case GL.GL_UNSIGNED_BYTE:
                return "GL_UNSIGNED_BYTE";
            case GL.GL_BYTE:
                return "GL_BYTE";
            case GL.GL_UNSIGNED_SHORT:
                return "GL_UNSIGNED_SHORT";
            case GL.GL_SHORT:
                return "GL_SHORT";
            case GL.GL_FLOAT:
                return "GL_FLOAT";
            case GL.GL_FIXED:
                return "GL_FIXED";
        }
        return null;
    }

}

