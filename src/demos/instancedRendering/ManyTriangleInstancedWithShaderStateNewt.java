/**
 * Copyright 2014 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package demos.instancedRendering;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.swing.SwingUtilities;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

public class ManyTriangleInstancedWithShaderStateNewt implements IInstancedRenderingView {

	protected float winScale = 0.1f;
	private static final float WIN_SCALE_MIN = 1e-3f;
	private static final float WIN_SCALE_MAX = 100f;
	private final FPSAnimator animator;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ManyTriangleInstancedWithShaderStateNewt();
			}
		});
	}

	public ManyTriangleInstancedWithShaderStateNewt() {
		TriangleInstancedRendererWithShaderState renderer = new TriangleInstancedRendererWithShaderState(this);

		GLProfile prof = GLProfile.get(GLProfile.GL4);
		GLCapabilities caps = new GLCapabilities(prof);
		GLWindow glWindow = GLWindow.create(caps);

		glWindow.addGLEventListener(renderer);

		glWindow.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseWheelMoved(MouseEvent e) {
				float[] step = e.getRotation();
				if(step[1] > 0) {
					winScale *= 1.05;
					if(winScale > WIN_SCALE_MAX) winScale = WIN_SCALE_MAX;
				} else if(0 > step[1] ) {
					winScale *= 0.95;
					if(winScale < WIN_SCALE_MIN) winScale = WIN_SCALE_MIN;
				}
			}
		});

		glWindow.addWindowListener(new WindowAdapter() {

			@Override
			public void windowDestroyed(WindowEvent evt) {
				if(animator.isAnimating()) animator.stop();
			}

			@Override
			public void windowDestroyNotify(WindowEvent evt) {
				animator.stop();
			}
		});

    	animator = new FPSAnimator(glWindow, 60, true);
    	animator.start();

    	glWindow.setTitle("Instanced rendering experiment");
        glWindow.setSize(1024, 768);
        glWindow.setUndecorated(false);
        glWindow.setPointerVisible(true);
        glWindow.setVisible(true);
	}

	@Override
	public float getScale() {
		return winScale;
	}

}
