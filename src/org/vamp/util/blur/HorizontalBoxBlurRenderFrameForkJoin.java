package org.vamp.util.blur;

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

	public HorizontalBoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer, final int pBlurSize) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pOutputRenderFrameBuffer, pBlurSize);
	}

	public HorizontalBoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurWidth) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer, pBlurWidth);
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
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, this.mWindowTop, pWindowWidthSplit, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurSize),
			/* Top Right: */
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, this.mWindowTop, this.mWindowRight, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurSize),
			/* Bottom Right: */
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, pWindowHeightSplit, this.mWindowRight, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurSize),
			/* Bottom Left: */
			new HorizontalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, pWindowHeightSplit, pWindowWidthSplit, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurSize)
		};
	}

	@Override
	protected void onCompute() {
		final int blurWidthHalf = (this.mBlurSize - 1) / 2;

		final int windowLeft = this.mWindowLeft;
		final int windowRight = this.mWindowRight;
		final int windowTop = this.mWindowTop;
		final int windowBottom = this.mWindowBottom;
		final int width = this.mWidth;

		final int[] inputRenderFrameBuffer = this.mInputRenderFrameBuffer;
		final int[] outputRenderFrameBuffer = this.mOutputRenderFrameBuffer;

		for (int x = windowLeft; x < windowRight; x++) {
			for (int y = windowTop; y < windowBottom; y++) {
				final int yBase = y * width;

				final int left = Math.max(x - blurWidthHalf, 0);
				final int right = Math.min(x + blurWidthHalf, width - 1);

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

				final int blurWidth = right - left + 1;
				r = r / blurWidth;
				g = g / blurWidth;
				b = b / blurWidth;

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
