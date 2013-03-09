package org.vamp.util.blur.gaussian;

import org.vamp.util.blur.BlurRenderFrameForkJoin;


public abstract class GaussianBlurRenderFrameForkJoin extends BlurRenderFrameForkJoin {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 4633412215367996025L;

	// ===========================================================
	// Fields
	// ===========================================================

	protected float[] mKernel;

	// ===========================================================
	// Constructors
	// ===========================================================

	public GaussianBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int[] pOutputRenderFrameBuffer, final int pBlurRadius) {
		this(pInputRenderFrameBuffer, pWidth, pHeight, 0, 0, pWidth, pHeight, pOutputRenderFrameBuffer, pBlurRadius);
	}

	public GaussianBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurRadius) {
		this(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer, pBlurRadius, GaussianBlurRenderFrameForkJoin.makeKernel(pBlurRadius));
	}

	public GaussianBlurRenderFrameForkJoin(final int[] pInputRenderFrameBuffer, final int pWidth, final int pHeight, final int pWindowLeft, final int pWindowTop, final int pWindowRight, final int pWindowBottom, final int[] pOutputRenderFrameBuffer, final int pBlurRadius, final float[] pKernel) {
		super(pInputRenderFrameBuffer, pWidth, pHeight, pWindowLeft, pWindowTop, pWindowRight, pWindowBottom, pOutputRenderFrameBuffer, pBlurRadius);

		this.mKernel = pKernel;
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

	public static float[] makeKernel(final int pRadius) {
		final float sigma = pRadius / 3f;
		final float sigmaSquared2 = 2 * sigma * sigma;
		final float sqrtSigmaPi2 = (float) Math.sqrt(2 * Math.PI * sigma);
		final float radiusSquared = pRadius * pRadius;

		final int rowCount = (pRadius * 2) + 1;
		final float[] kernel = new float[rowCount];
		float total = 0;
		for (int index = 0, row = -pRadius; row <= pRadius; index++, row++) {
			final float distance = row * row;
			if (distance > radiusSquared) {
				kernel[index] = 0;
			} else {
				kernel[index] = (float) Math.exp(-(distance) / sigmaSquared2) / sqrtSigmaPi2;
			}
			total += kernel[index];
		}

		for (int i = 0; i < rowCount; i++) {
			kernel[i] /= total;
		}

		return kernel;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
