package org.vamp.ui;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;

public class OutputRenderFramePanel extends RenderFramePanel {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 4599100470001576167L;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final BufferedImage mInputRenderFrame;
	protected final int[] mInputRenderFrameBuffer;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OutputRenderFramePanel(final BufferedImage pRenderFrame, final BufferedImage pInputRenderFrame) {
		super(pRenderFrame);

		this.mInputRenderFrame = pInputRenderFrame;
		this.mInputRenderFrameBuffer = ((DataBufferInt) this.mInputRenderFrame.getRaster().getDataBuffer()).getData();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void notifyInputRenderFrameChanged() {
		/* Blur: */
		final float[] kernelData = new float[] {
			1 / 9f, 1 / 9f, 1 / 9f,
			1 / 9f, 1 / 9f, 1 / 9f,
			1 / 9f, 1 / 9f, 1 / 9f
		};

		final Kernel kernel = new Kernel(3, 3, kernelData);
		final BufferedImageOp op = new ConvolveOp(kernel);
		op.filter(this.mInputRenderFrame, this.mRenderFrame);

		final int[] outputRenderFrameBuffer = this.mRenderFrameBuffer;

		/* Quick RGB to GRAYScale conversion for demonstration purposes: */
		for (int i = 0; i < outputRenderFrameBuffer.length; i++) {
			final int argb = outputRenderFrameBuffer[i];
			final int b = (argb & 0xFF);
			final int g = ((argb >> 8) & 0xFF);
			final int r = ((argb >> 16) & 0xFF);
			final int grey = (r + g + g + b) >> 2; // performance optimized - not real grey!
			outputRenderFrameBuffer[i] = (grey << 16) + (grey << 8) + grey;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
