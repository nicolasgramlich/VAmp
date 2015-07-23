package org.vamp.util.blur.box;

import jsr166y.ForkJoinTask;

public class HorizontalBoxBlurRenderFrameForkJoin extends BoxBlurRenderFrameForkJoin {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 1670779761683246062L;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public HorizontalBoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer, final int pBlurRadius) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pOutputRenderFrameBuffer, pBlurRadius);
	}

	public HorizontalBoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurRadius) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer, pBlurRadius);
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
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, this.mWindowTop, pWindowWidthSplit, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurRadius),
			/* Top Right: */
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, this.mWindowTop, this.mWindowRight, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurRadius),
			/* Bottom Right: */
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, pWindowHeightSplit, this.mWindowRight, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurRadius),
			/* Bottom Left: */
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, pWindowHeightSplit, pWindowWidthSplit, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurRadius)
		};
	}

	@Override
	protected void onCompute() {
		final int blurRadius = this.mBlurRadius;

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

				final int left = Math.max(x - blurRadius, 0);
				final int right = Math.min(x + blurRadius, width - 1);

				int r = 0;
				int g = 0;
				int b = 0;

				for (int i = left; i <= right; i++) {
					final int index = i + yBase;
					final int pixel = inputRenderFrameBuffer[index];
					r += ((pixel >> 16) & 0xFF);
					g += ((pixel >> 8) & 0xFF);
					b += ((pixel >> 0) & 0xFF);
				}

				final float blurWidth = right - left + 1;
				r = Math.round(r / blurWidth);
				g = Math.round(g / blurWidth);
				b = Math.round(b / blurWidth);

				final int outPixel = (r << 16) | (g << 8) | (b << 0);
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
