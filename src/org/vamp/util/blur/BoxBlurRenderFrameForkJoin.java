package org.vamp.util.blur;


public abstract class BoxBlurRenderFrameForkJoin extends RenderFrameForkJoin {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = -2668124020841163715L;

	private static int TRESHOLD = 100 * 100;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final int mBlurSize;

	// ===========================================================
	// Constructors
	// ===========================================================

	public BoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer, final int pBlurSize) {
		this(pInputRenderFrameBuffer, pWidth, pHeight, 0, 0, pWidth, pHeight, pOutputRenderFrameBuffer, pBlurSize);
	}

	public BoxBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurWidth) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer);

		this.mBlurSize = pBlurWidth;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected boolean shouldFork() {
		final int windowWidth = this.mWindowRight - this.mWindowLeft;
		final int windowHeight = this.mWindowBottom - this.mWindowTop;

		return windowWidth * windowHeight > TRESHOLD;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}