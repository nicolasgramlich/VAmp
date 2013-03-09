package org.vamp.util;

import jsr166y.ForkJoinTask;
import jsr166y.RecursiveAction;

public abstract class RenderFrameForkJoin extends RecursiveAction {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 6720752963103551309L;

	private static int TRESHOLD = 100 * 100;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final int[] mInputRenderFrameBuffer;
	protected final int mWidth;
	protected final int mHeight;
	protected final int mWindowLeft;
	protected final int mWindowTop;
	protected final int mWindowRight;
	protected final int mWindowBottom;
	protected final int[] mOutputRenderFrameBuffer;

	// ===========================================================
	// Constructors
	// ===========================================================

	public RenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer) {
		this(pInputRenderFrameBuffer, pWidth, pHeight, 0, 0, pWidth, pHeight, pOutputRenderFrameBuffer);
	}

	public RenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer) {
		this.mInputRenderFrameBuffer = pInputRenderFrameBuffer;
		this.mWidth = pWidth;
		this.mHeight = pHeight;
		this.mWindowLeft = pWindowLeft;
		this.mWindowTop = pWindowTop;
		this.mWindowRight = pWindowRight;
		this.mWindowBottom = pWindowBottom;
		this.mOutputRenderFrameBuffer = pOutputRenderFrameBuffer;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract void onCompute();
	protected abstract ForkJoinTask<?>[] onFork(final int pWindowWidthSplit, final int pWindowHeightSplit);

	@Override
	protected void compute() {
		if (this.shouldFork()) {
			this.onFork();
		} else {
			this.onCompute();
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	protected boolean shouldFork() {
		final int windowWidth = this.mWindowRight - this.mWindowLeft;
		final int windowHeight = this.mWindowBottom - this.mWindowTop;

		return windowWidth * windowHeight > TRESHOLD;
	}

	private void onFork() {
		final int windowWidth = this.mWindowRight - this.mWindowLeft;
		final int windowHeight = this.mWindowBottom - this.mWindowTop;

		final int windowWidthHalf = windowWidth / 2;
		final int windowHeightHalf = windowHeight / 2;

		final int windowWidthSplit = this.mWindowLeft + windowWidthHalf;
		final int windowHeightSplit = this.mWindowTop + windowHeightHalf;

		final ForkJoinTask<?>[] forks = this.onFork(windowWidthSplit, windowHeightSplit);

		ForkJoinTask.invokeAll(forks);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}