package org.vamp.ui;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import jsr166y.ForkJoinPool;

import org.vamp.VAmp;
import org.vamp.util.blur.BlurMode;
import org.vamp.util.blur.box.HorizontalBoxBlurRenderFrameForkJoin;
import org.vamp.util.blur.box.VerticalBoxBlurRenderFrameForkJoin;
import org.vamp.util.blur.gaussian.HorizontalGaussianBlurRenderFrameForkJoin;
import org.vamp.util.blur.gaussian.VerticalGaussianBlurRenderFrameForkJoin;

public class OutputRenderFramePanel extends RenderFramePanel {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 4599100470001576167L;

	private static final int REFERENCE_RENDER_FRAME_BUFFER_COUNT = 100;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final BufferedImage mInputRenderFrame;
	protected final int[] mInputRenderFrameBuffer;

	protected int mReferenceRenderFrameIndex = 0;
	protected final int[][] mReferenceRenderFrameBuffers = new int[REFERENCE_RENDER_FRAME_BUFFER_COUNT][];
	protected final int[][] mTempRenderFrameBuffers = new int[2][];

	protected float mAmplificationRed = VAmp.AMPLIFICATION_DEFAULT;
	protected float mAmplificationGreen = VAmp.AMPLIFICATION_DEFAULT;
	protected float mAmplificationBlue = VAmp.AMPLIFICATION_DEFAULT;

	protected int mBlurRadius = VAmp.BLUR_RADIUS_DEFAULT;
	protected BlurMode mBlurMode = VAmp.BLUR_MODE_DEFAULT;

	protected int mFrequency = Math.round(VAmp.FREQUENCY_DEFAULT);

	protected boolean mAmplificationAbsolute = VAmp.AMPLIFICATION_ABSOLUTE_DEFAULT;

	protected final ForkJoinPool mForkJoinPool;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OutputRenderFramePanel(final BufferedImage pRenderFrame, final BufferedImage pInputRenderFrame) {
		super(pRenderFrame);

		this.mInputRenderFrame = pInputRenderFrame;
		this.mInputRenderFrameBuffer = ((DataBufferInt) this.mInputRenderFrame.getRaster().getDataBuffer()).getData();

		for (int i = 0; i < this.mReferenceRenderFrameBuffers.length; i++) {
			this.mReferenceRenderFrameBuffers[i] = new int[this.mInputRenderFrameBuffer.length];
		}

		for (int i = 0; i < this.mTempRenderFrameBuffers.length; i++) {
			this.mTempRenderFrameBuffers[i] = new int[this.mInputRenderFrameBuffer.length];
		}

		this.mForkJoinPool = new ForkJoinPool();
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
		this.mBlurRadius = pBlurRadius;
	}

	public void setBlurMode(final BlurMode pBlurMode) {
		this.mBlurMode = pBlurMode;
	}

	public void setFrequency(final float pFrequency) {
		this.mFrequency = Math.round(pFrequency);
	}

	public void setAmplificationAbsolute(final boolean pAmplificationAbsolute) {
		this.mAmplificationAbsolute = pAmplificationAbsolute;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void notifyInputRenderFrameChanged() {
		final int[] inputRenderFrameBuffer = this.mInputRenderFrameBuffer;
		final int[][] tempRenderFrameBuffers = this.mTempRenderFrameBuffers;
		final int[][] referenceRenderFrameBuffers = this.mReferenceRenderFrameBuffers;
		final int[] outputRenderFrameBuffer = this.mRenderFrameBuffer;

		/* Box Blurring: */
		final long blurStartTime = System.currentTimeMillis();

		int[] blurredInputRenderFrameBuffer;
		final int blurRadius = this.mBlurRadius;
		if (blurRadius > 1) {
			switch (this.mBlurMode) {
				case BOX:
					this.mForkJoinPool.invoke(new HorizontalBoxBlurRenderFrameForkJoin(inputRenderFrameBuffer, this.mRenderFrame.getWidth(), this.mRenderFrame.getHeight(), tempRenderFrameBuffers[0], blurRadius));
					this.mForkJoinPool.invoke(new VerticalBoxBlurRenderFrameForkJoin(tempRenderFrameBuffers[0], this.mRenderFrame.getWidth(), this.mRenderFrame.getHeight(), tempRenderFrameBuffers[1], blurRadius));
					break;
				case GAUSSIAN:
					this.mForkJoinPool.invoke(new HorizontalGaussianBlurRenderFrameForkJoin(inputRenderFrameBuffer, this.mRenderFrame.getWidth(), this.mRenderFrame.getHeight(), tempRenderFrameBuffers[0], blurRadius));
					this.mForkJoinPool.invoke(new VerticalGaussianBlurRenderFrameForkJoin(tempRenderFrameBuffers[0], this.mRenderFrame.getWidth(), this.mRenderFrame.getHeight(), tempRenderFrameBuffers[1], blurRadius));
					break;
				default:
					throw new IllegalArgumentException("Unexpected " + BlurMode.class.getSimpleName() + ": '" + this.mBlurMode + "'.");
			}

			blurredInputRenderFrameBuffer = tempRenderFrameBuffers[1];
		} else {
			blurredInputRenderFrameBuffer = inputRenderFrameBuffer;
		}

		final long blurEndTime = System.currentTimeMillis();

		/* Store the current blurred frame as a reference frame: */
		final int pixelCount = this.mInputRenderFrameBuffer.length;
		System.arraycopy(blurredInputRenderFrameBuffer, 0, referenceRenderFrameBuffers[this.mReferenceRenderFrameIndex], 0, pixelCount);

		/* Amplification: */
		final long amplificationStartTime = System.currentTimeMillis();

		final boolean amplificationAbsolute = this.mAmplificationAbsolute;
		final float amplificationRed = this.mAmplificationRed;
		final float amplificationGreen = this.mAmplificationGreen;
		final float amplificationBlue = this.mAmplificationBlue;

		final int referenceRenderFrameBufferIndex = this.calculateReferenceFrameBufferIndex();
		final int[] referenceRenderFrameBuffer = referenceRenderFrameBuffers[referenceRenderFrameBufferIndex];

		for (int i = pixelCount - 1; i >= 0; i--) {
			final int referenceARGB = referenceRenderFrameBuffer[i];
			final int referenceRed = ((referenceARGB >> 16) & 0xFF);
			final int referenceGreen = ((referenceARGB >> 8) & 0xFF);
			final int referenceBlue = (referenceARGB & 0xFF);

			final int tempARGB = blurredInputRenderFrameBuffer[i];
			final int tempRed = ((tempARGB >> 16) & 0xFF);
			final int tempGreen = ((tempARGB >> 8) & 0xFF);
			final int tempBlue = (tempARGB & 0xFF);

			final int deltaRed;
			final int deltaGreen;
			final int deltaBlue;
			if (amplificationAbsolute) {
				deltaRed = Math.abs(referenceRed - tempRed);
				deltaGreen = Math.abs(referenceGreen - tempGreen);
				deltaBlue = Math.abs(referenceBlue - tempBlue);
			} else {
				deltaRed = referenceRed - tempRed;
				deltaGreen = referenceGreen - tempGreen;
				deltaBlue = referenceBlue - tempBlue;
			}

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

		this.mReferenceRenderFrameIndex++;
		this.mReferenceRenderFrameIndex %= REFERENCE_RENDER_FRAME_BUFFER_COUNT;
	}

	private int calculateReferenceFrameBufferIndex() {
		final float videoFPS = this.mVideoFPS;
		final float frequency = this.mFrequency;

		final int frameOffset;
		if (frequency == 0) {
			frameOffset = REFERENCE_RENDER_FRAME_BUFFER_COUNT - 1;
		} else {
			frameOffset = Math.min(Math.round(videoFPS / frequency), REFERENCE_RENDER_FRAME_BUFFER_COUNT - 1);
		}

		int referenceRenderFrameBufferIndex = this.mReferenceRenderFrameIndex - frameOffset;
		if (referenceRenderFrameBufferIndex < 0) {
			referenceRenderFrameBufferIndex += REFERENCE_RENDER_FRAME_BUFFER_COUNT;
		}
		if (referenceRenderFrameBufferIndex < 0) {
			throw new IllegalStateException();
		}
		return referenceRenderFrameBufferIndex;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
