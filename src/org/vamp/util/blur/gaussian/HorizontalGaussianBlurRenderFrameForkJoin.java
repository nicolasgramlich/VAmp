package org.vamp.util.blur.gaussian;

import jsr166y.ForkJoinTask;

public class HorizontalGaussianBlurRenderFrameForkJoin extends GaussianBlurRenderFrameForkJoin {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 5371958777961698167L;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public HorizontalGaussianBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer, final int pBlurRadius) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pOutputRenderFrameBuffer, pBlurRadius);
	}

	public HorizontalGaussianBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurRadius) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer, pBlurRadius);
	}

	public HorizontalGaussianBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurRadius, final float[] pKernel) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer, pBlurRadius, pKernel);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected ForkJoinTask<?>[] onFork(final int pWindowWidthSplit, final int pWindowHeightSplit) {
		return new ForkJoinTask[] {
			/* Top Left: */
			new HorizontalGaussianBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, this.mWindowTop, pWindowWidthSplit, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurRadius, this.mKernel),
			/* Top Right: */
			new HorizontalGaussianBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, this.mWindowTop, this.mWindowRight, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurRadius, this.mKernel),
			/* Bottom Right: */
			new HorizontalGaussianBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, pWindowHeightSplit, this.mWindowRight, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurRadius, this.mKernel),
			/* Bottom Left: */
			new HorizontalGaussianBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, pWindowHeightSplit, pWindowWidthSplit, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurRadius, this.mKernel)
		};
	}

	@Override
	protected void onCompute() {
		final int blurRadius = this.mBlurRadius;
		final float[] kernel = this.mKernel;

		final int windowLeft = this.mWindowLeft;
		final int windowRight = this.mWindowRight;
		final int windowTop = this.mWindowTop;
		final int windowBottom = this.mWindowBottom;
		final int width = this.mWidth;

		final int[] inputRenderFrameBuffer = this.mInputRenderFrameBuffer;
		final int[] outputRenderFrameBuffer = this.mOutputRenderFrameBuffer;

		for (int y = windowTop; y < windowBottom; y++) {
			for (int x = windowLeft; x < windowRight; x++) {
				final int yBase = y * width;

				float r = 0;
				float g = 0;
				float b = 0;

				for (int i = -blurRadius; i <= blurRadius; i++) {
					final float factor = kernel[blurRadius + i];

					final int index;
					/* Clamping: */
					if (x + i < 0) {
						index = yBase;
					} else if(x + i >= width) {
						index = yBase + width - 1;
					} else {
						index = yBase + x + i;
					}

					final int pixel = inputRenderFrameBuffer[index];
					r += factor * ((pixel >> 16) & 0xFF);
					g += factor * ((pixel >> 8) & 0xFF);
					b += factor * ((pixel >> 0) & 0xFF);
				}

				final int outPixel = (Math.round(r) << 16) | (Math.round(g) << 8) | (Math.round(b) << 0);
				final int outIndex = x + yBase;
				outputRenderFrameBuffer[outIndex] = outPixel;
			}
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
