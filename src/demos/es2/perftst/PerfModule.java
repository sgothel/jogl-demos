package demos.es2.perftst;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;

import com.jogamp.opengl.math.FixedPoint;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public abstract class PerfModule {

    public abstract ShaderState initShaderState(GL2ES2 gl);

    public abstract void run(GLAutoDrawable drawable, int loops);

    ShaderState st = null;

    public ShaderState getShaderState() { return st; }
    
    public ShaderState initShaderState(GL2ES2 gl, String vShaderName, String fShaderName) {
        if(st!=null) {
        	return st;
        }

        long t0, t1;

        st = new ShaderState();

        // Create & Compile the shader objects
        ShaderCode vp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, Perftst.class,
                                            "shader", "shader/bin", vShaderName, false);
        ShaderCode fp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, Perftst.class,
                                            "shader", "shader/bin", fShaderName, false);

        // Create & Link the shader program
        ShaderProgram sp = new ShaderProgram();
        sp.add(vp);
        sp.add(fp);

        t0 = System.currentTimeMillis();

        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        t1 = System.currentTimeMillis();

        long dt = t1-t0;

        System.out.println("shader creation: "+dt+" ms");

        // Let's manage all our states using ShaderState.
        st.attachShaderProgram(gl, sp, true);
        
        return st;
    }

    public static final void put(Buffer buffer, int type, float v) {
        switch (type) {
            case GL.GL_UNSIGNED_BYTE:
                ((ByteBuffer)buffer).put((byte)(v*(float)0xFF));
                break;
            case GL.GL_BYTE:
                ((ByteBuffer)buffer).put((byte)(v*(float)0x7F));
                break;
            case GL.GL_UNSIGNED_SHORT:
                ((ShortBuffer)buffer).put((short)(v*(float)0xFFFF));
                break;
            case GL.GL_SHORT:
                ((ShortBuffer)buffer).put((short)(v*(float)0x7FFF));
                break;
            case GL.GL_FLOAT:
                ((FloatBuffer)buffer).put(v);
                break;
            case GL.GL_FIXED:
                ((IntBuffer)buffer).put(FixedPoint.toFixed(v));
                break;
        }
    }

    public static final String getTypeName(int type) {
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

