package org.vamp.recorder;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import org.vamp.VAmp;

import ca.randelshofer.AVIOutputStream;
import ca.randelshofer.AVIOutputStream.VideoFormat;

public class Recorder {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected BufferedImage mInputRenderFrame;
	protected BufferedImage mOutputRenderFrame;

	private File mFile;
	private AVIOutputStream mAVIOutputStream;

	private boolean mRecording;
	private boolean mPaused;

	private int mFPS = VAmp.FPS_DEFAULT;
	private boolean mFPSFixed;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Recorder(final BufferedImage pInputRenderFrame, final BufferedImage pOutputRenderFrame) {
		this.mInputRenderFrame = pInputRenderFrame;
		this.mOutputRenderFrame = pOutputRenderFrame;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setFile(final File pFile) {
		this.mFile = pFile;
	}

	public void setFPS(final int pFPS) {
		if (!this.mFPSFixed) {
			this.mFPS = pFPS;
		}
	}

	public void setFPSFixed(final boolean pFPSFixed) {
		this.mFPSFixed = pFPSFixed;
	}

	public void setInputRenderFrame(final BufferedImage pInputRenderFrame) {
		if (this.mRecording) {
			throw new IllegalStateException();
		}

		this.mInputRenderFrame = pInputRenderFrame;
	}

	public void setOutputRenderFrame(final BufferedImage pOutputRenderFrame) {
		if (this.mRecording) {
			throw new IllegalStateException();
		}

		this.mOutputRenderFrame = pOutputRenderFrame;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void record() throws IOException {
		if (this.mFile == null) {
			throw new IllegalStateException();
		}

		this.mAVIOutputStream = new AVIOutputStream(this.mFile, VideoFormat.JPG);
		this.mAVIOutputStream.setFrameRate(this.mFPS);
		this.mAVIOutputStream.setTimeScale(1);

		this.mRecording = true;
		this.mPaused = false;
	}

	public synchronized void pause() {
		this.mPaused = true;
	}

	public synchronized void resume() {
		this.mPaused = false;
	}

	public synchronized void stop() throws IOException {
		this.mRecording = false;

		this.mAVIOutputStream.finish();
		this.mAVIOutputStream.close();
	}

	public synchronized void play() {
		throw new IllegalStateException();
	}

	public synchronized void onFrame() throws IOException {
		if (this.mRecording && !this.mPaused) {
			final ColorModel colorModel = this.mOutputRenderFrame.getColorModel();
			final boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
			final WritableRaster raster = this.mOutputRenderFrame.copyData(null);
	
			final BufferedImage frame = new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
	
			this.mAVIOutputStream.writeFrame(frame);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
