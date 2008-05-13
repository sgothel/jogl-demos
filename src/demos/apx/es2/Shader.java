package demos.apx;

import java.io.UnsupportedEncodingException;
import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;

public class Shader {
    private int program;
    private int vertexShader;
    private int fragmentShader;

    private Shader() {
    }

    public static Shader createBinaryProgram(byte[] vertexShaderCode, int vertexShaderFormat,
                                             byte[] fragmentShaderCode, int fragmentShaderFormat) throws GLException {
        Shader shader = new Shader();
        shader.createBinaryProgramImpl(vertexShaderCode, vertexShaderFormat,
                                       fragmentShaderCode, fragmentShaderFormat);
        shader.useProgram();
        return shader;
    }

    public void setAttribByName(String name, int size, int type, boolean normalized, int stride, Buffer pointer) {
        GL gl = GLU.getCurrentGL();
        int index = gl.glGetAttribLocation(program, name);
        gl.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        gl.glEnableVertexAttribArray(index);
        // FIXME
        //        trackAttribLocation(index);
    }

    public int getUniformLocation(String name) {
        GL gl = GLU.getCurrentGL();
        return gl.glGetUniformLocation(program, name);
    }

    public void useProgram() {
        GL gl = GLU.getCurrentGL();
        gl.glUseProgram(program);
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private void createBinaryProgramImpl(byte[] vertexShaderCode, int vertexShaderFormat,
                                         byte[] fragmentShaderCode, int fragmentShaderFormat) throws GLException {
        allocProgram();
        int[] numBinaryFormats = new int[1];
        GL gl = GLU.getCurrentGL();
        gl.glGetIntegerv(GL.GL_NUM_SHADER_BINARY_FORMATS, numBinaryFormats, 0);
        if (numBinaryFormats[0] > 0) {
            int[] binaryFormats = new int[numBinaryFormats[0]];
            gl.glGetIntegerv(GL.GL_SHADER_BINARY_FORMATS, binaryFormats, 0);
            boolean gotVertexFormat = false;
            boolean gotFragmentFormat = false;
            
            for (int i = 0; i < binaryFormats.length && (!gotVertexFormat || !gotFragmentFormat); i++) {
                if (!gotVertexFormat) {
                    gotVertexFormat = (binaryFormats[i] == vertexShaderFormat);
                }
                if (!gotFragmentFormat) {
                    gotFragmentFormat = (binaryFormats[i] == fragmentShaderFormat);
                }
            }

            if (!gotVertexFormat) {
                throw new RuntimeException("Binary vertex program format 0x" + Integer.toHexString(vertexShaderFormat) +
                                           " not available");
            }

            if (!gotFragmentFormat) {
                throw new RuntimeException("Binary fragment program format 0x" + Integer.toHexString(fragmentShaderFormat) +
                                           " not available");
            }
        }                
        // Set up the shaders
        setupBinaryShader(vertexShader, vertexShaderCode, vertexShaderFormat);
        setupBinaryShader(fragmentShader, fragmentShaderCode, fragmentShaderFormat);

        // Set up the shader program
        gl.glLinkProgram(program);
        if (!glslLog(program, GL.GL_LINK_STATUS, "link")) {
            throw new GLException("Error linking program");
        }
    }

    private void allocProgram() {
        GL gl = GLU.getCurrentGL();
        vertexShader = gl.glCreateShader(GL.GL_VERTEX_SHADER);
        fragmentShader = gl.glCreateShader(GL.GL_FRAGMENT_SHADER);
        program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
    }

    private void setupBinaryShader(int shader,
                                   byte[] shaderData,
                                   int binaryFormat) {
        ByteBuffer buf = ByteBuffer.wrap(shaderData);
        int[] tmp = new int[1];
        tmp[0] = shader;
        GL gl = GLU.getCurrentGL();
        gl.glShaderBinary(1, tmp, 0, binaryFormat, buf, shaderData.length);
    }

    private boolean glslLog(int obj, int checkCompile, String op) {
        boolean success = false;

        GL gl = GLU.getCurrentGL();

        // log output.
        String str = null;
        if (checkCompile == GL.GL_COMPILE_STATUS) {
            int[] len = new int[1];
            gl.glGetShaderiv(obj, GL.GL_INFO_LOG_LENGTH, len, 0);
            if (len[0] > 0) {
                byte[] buf = new byte[len[0]];
                gl.glGetShaderInfoLog(obj, len[0], null, 0, buf, 0);
                try {
                    str = new String(buf, 0, buf.length, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // LINK or VALIDATE
            int[] len = new int[1];
            gl.glGetProgramiv(obj, GL.GL_INFO_LOG_LENGTH, len, 0);
            if (len[0] > 0) {
                byte[] buf = new byte[len[0]];
                gl.glGetProgramInfoLog(obj, len[0], null, 0, buf, 0);
                try {
                    str = new String(buf, 0, buf.length, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        if (str != null) {
            System.out.println("--- ");
            System.out.println(op);
            System.out.println(" log ---");
            System.out.println(str);
        }

        // check the compile / link status.
        if (checkCompile == GL.GL_COMPILE_STATUS) {
            int[] status = new int[1];

            gl.glGetShaderiv(obj, checkCompile, status, 0);
            success = (status[0] != 0);
            if (!success) {
                int[] len = new int[1];
                gl.glGetShaderiv(obj, GL.GL_SHADER_SOURCE_LENGTH, len, 0);
                if (len[0] > 0) {
                    byte[] buf = new byte[len[0]];
                    gl.glGetShaderSource(obj, len[0], null, 0, buf, 0);
                    try {
                        str = new String(buf, 0, buf.length, "US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (str != null) { 
                        System.out.println("--- ");
                        System.out.println(op);
                        System.out.println(" code ---");
                        System.out.println(str);
                    }
                }
            }
        } else { // LINK or VALIDATE
            int[] status = new int[1];
            gl.glGetProgramiv(obj, checkCompile, status, 0);
            success = (status[0] != 0);
        }

        if (!success) {
            System.out.println("--- ");
            System.out.println(op);
            System.out.println(" failed");
        }

        return success;
    }
}
