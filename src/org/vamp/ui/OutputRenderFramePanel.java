package org.vamp.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import jsr166y.ForkJoinPool;

import org.vamp.VAmp;
import org.vamp.util.blur.HorizontalBoxBlurRenderFrameForkJoin;
import org.vamp.util.blur.VerticalBoxBlurRenderFrameForkJoin;

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

	protected boolean mReferenceRenderFrameBufferInitialized;
	protected final int[] mReferenceRenderFrameBuffer;
	protected final int[][] mTempRenderFrameBuffers = new int[2][];

	protected float mAmplificationRed = VAmp.AMPLIFICATION_DEFAULT;
	protected float mAmplificationGreen = VAmp.AMPLIFICATION_DEFAULT;
	protected float mAmplificationBlue = VAmp.AMPLIFICATION_DEFAULT;

	protected int mBlurSize = OutputRenderFramePanel.convertBlurRadiusToBlurSize(VAmp.BLUR_RADIUS_DEFAULT);

	protected final ForkJoinPool mForkJoinPool;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OutputRenderFramePanel(final BufferedImage pRenderFrame, final BufferedImage pInputRenderFrame) {
		super(pRenderFrame);

		this.mInputRenderFrame = pInputRenderFrame;
		this.mInputRenderFrameBuffer = ((DataBufferInt) this.mInputRenderFrame.getRaster().getDataBuffer()).getData();

		this.mReferenceRenderFrameBuffer = new int[this.mInputRenderFrameBuffer.length];

		for (int i = 0; i < this.mTempRenderFrameBuffers.length; i++) {
			this.mTempRenderFrameBuffers[i] = new int[this.mInputRenderFrameBuffer.length];
		}

		this.mForkJoinPool = new ForkJoinPool();

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent pMouseEvent) {
				OutputRenderFramePanel.this.mReferenceRenderFrameBufferInitialized = false;
			}
		});
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setAmplification(final float pAmplificationRed, final float pAmplificationGreen, final float pAmplificationBlue) {
		this.mAmplificationRed = pAmplificationRed;
		this.mAmplificationGreen = pAmplificationGreen;
		this.mAmplificationBlue = pAmplificationBlue;
	}

	public void setBlurRadius(final int pBlurRadius) {
		this.mBlurSize = OutputRenderFramePanel.convertBlurRadiusToBlurSize(pBlurRadius);

		this.mReferenceRenderFrameBufferInitialized = false;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	private static int convertBlurRadiusToBlurSize(final int pBlurRadius) {
		return (2 * pBlurRadius) + 1;
	}

	public void notifyInputRenderFrameChanged() {
		final int[] inputRenderFrameBuffer = this.mInputRenderFrameBuffer;
		final int[][] tempRenderFrameBuffers = this.mTempRenderFrameBuffers;
		final int[] referenceRenderFrameBuffer = this.mReferenceRenderFrameBuffer;
		final int[] outputRenderFrameBuffer = this.mRenderFrameBuffer;

		/* Box Blurring: */
		final long blurStartTime = System.currentTimeMillis();

		int[] blurredInputRenderFrameBuffer;
		final int blurSize = this.mBlurSize;
		if (blurSize > 1) {
			this.mForkJoinPool.invoke(new HorizontalBoxBlurRenderFrameForkJoin(inputRenderFrameBuffer, this.mRenderFrame.getWidth(), this.mRenderFrame.getHeight(), tempRenderFrameBuffers[0], blurSize));
			this.mForkJoinPool.invoke(new VerticalBoxBlurRenderFrameForkJoin(tempRenderFrameBuffers[0], this.mRenderFrame.getWidth(), this.mRenderFrame.getHeight(), tempRenderFrameBuffers[1], blurSize));

			blurredInputRenderFrameBuffer = tempRenderFrameBuffers[1];
		} else {
			blurredInputRenderFrameBuffer = inputRenderFrameBuffer;
		}

		final long blurEndTime = System.currentTimeMillis();

		/* Reference frame: */
		final int pixelCount = this.mInputRenderFrameBuffer.length;
		if (this.mReferenceRenderFrameBufferInitialized == false) {
			this.mReferenceRenderFrameBufferInitialized = true;
			System.arraycopy(blurredInputRenderFrameBuffer, 0, referenceRenderFrameBuffer, 0, pixelCount);
		}

		/* Amplification: */
		final long amplificationStartTime = System.currentTimeMillis();

		final float amplificationRed = this.mAmplificationRed;
		final float amplificationGreen = this.mAmplificationGreen;
		final float amplificationBlue = this.mAmplificationBlue;

		for (int i = pixelCount - 1; i >= 0; i--) {
			final int referenceARGB = referenceRenderFrameBuffer[i];
			final int referenceRed = ((referenceARGB >> 16) & 0xFF);
			final int referenceGreen = ((referenceARGB >> 8) & 0xFF);
			final int referenceBlue = (referenceARGB & 0xFF);

			final int tempARGB = blurredInputRenderFrameBuffer[i];
			final int tempRed = ((tempARGB >> 16) & 0xFF);
			final int tempGreen = ((tempARGB >> 8) & 0xFF);
			final int tempBlue = (tempARGB & 0xFF);

			final int deltaRed = referenceRed - tempRed;
			final int deltaGreen = referenceGreen - tempGreen;
			final int deltaBlue = referenceBlue - tempBlue;

			final int inputARGB = inputRenderFrameBuffer[i];
			final int inputRed = ((inputARGB >> 16) & 0xFF);
			final int inputGreen = ((inputARGB >> 8) & 0xFF);
			final int inputBlue = (inputARGB & 0xFF);

			final int outputRed = Math.max(0, Math.min(255, Math.round(inputRed + (amplificationRed * deltaRed))));
			final int outputGreen = Math.max(0, Math.min(255, Math.round(inputGreen + (amplificationGreen * deltaGreen))));
			final int outputBlue = Math.max(0, Math.min(255, Math.round(inputBlue + (amplificationBlue * deltaBlue))));

			outputRenderFrameBuffer[i] = (outputRed << 16) | (outputGreen << 8) | (outputBlue);
		}
		final long amplificationEndTime = System.currentTimeMillis();

		System.out.println("Blur: " + (blurEndTime - blurStartTime) + "ms" + "\t\tAmp: " + (amplificationEndTime - amplificationStartTime) + "ms");
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
