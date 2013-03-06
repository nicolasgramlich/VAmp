package org.vamp.ui;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;
import java.util.Arrays;

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

	private BufferedImageOp mBlurOp;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OutputRenderFramePanel(final BufferedImage pRenderFrame, final BufferedImage pInputRenderFrame) {
		super(pRenderFrame);

		this.mInputRenderFrame = pInputRenderFrame;
		this.mInputRenderFrameBuffer = ((DataBufferInt) this.mInputRenderFrame.getRaster().getDataBuffer()).getData();

		this.mBlurOp = OutputRenderFramePanel.createBlurOp(3);
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

	private static BufferedImageOp createBlurOp(final int pKernelSize) {
		final float[] kernelData = new float[pKernelSize * pKernelSize];
		Arrays.fill(kernelData, 1f / kernelData.length);

		final Kernel kernel = new Kernel(pKernelSize, pKernelSize, kernelData);
		return new ConvolveOp(kernel);
	}

	public void notifyInputRenderFrameChanged() {
		this.mBlurOp.filter(this.mInputRenderFrame, this.mRenderFrame);

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
