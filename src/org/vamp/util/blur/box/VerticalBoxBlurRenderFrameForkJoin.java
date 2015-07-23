package org.vamp.util.blur.box;

import jsr166y.ForkJoinTask;

public class VerticalBoxBlurRenderFrameForkJoin extends BoxBlurRenderFrameForkJoin {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = -4028722110267620930L;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public VerticalBoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer, final int pBlurSize) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pOutputRenderFrameBuffer, pBlurSize);
	}

	public VerticalBoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurWidth) {
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
			new VerticalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, this.mWindowTop, pWindowWidthSplit, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurRadius),
			/* Top Right: */
			new VerticalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, this.mWindowTop, this.mWindowRight, pWindowHeightSplit, this.mOutputRenderFrameBuffer, this.mBlurRadius),
			/* Bottom Right: */
			new VerticalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, pWindowWidthSplit, pWindowHeightSplit, this.mWindowRight, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurRadius),
			/* Bottom Left: */
			new VerticalBoxBlurRenderFrameForkJoin(this.mInputRenderFrameBuffer, this.mWidth, this.mHeight, this.mWindowLeft, pWindowHeightSplit, pWindowWidthSplit, this.mWindowBottom, this.mOutputRenderFrameBuffer, this.mBlurRadius)
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
		final int height = this.mHeight;

		final int[] inputRenderFrameBuffer = this.mInputRenderFrameBuffer;
		final int[] outputRenderFrameBuffer = this.mOutputRenderFrameBuffer;

		for (int y = windowTop; y < windowBottom; y++) {
			for (int x = windowLeft; x < windowRight; x++) {
				final int top = Math.max(y - blurRadius, 0);
				final int bottom = Math.min(y + blurRadius, height - 1);

				int r = 0;
				int g = 0;
				int b = 0;

				for (int i = top; i <= bottom; i++) {
					final int index = x + (i * width);
					final int pixel = inputRenderFrameBuffer[index];
					r += ((pixel >> 16) & 0xFF);
					g += ((pixel >> 8) & 0xFF);
					b += ((pixel >> 0) & 0xFF);
				}

				final float blurHeight = bottom - top + 1;
				r = Math.round(r / blurHeight);
				g = Math.round(g / blurHeight);
				b = Math.round(b / blurHeight);

				final int outPixel = (r << 16) | (g << 8) | (b << 0);
				final int outIndex = x + (y * width);
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
