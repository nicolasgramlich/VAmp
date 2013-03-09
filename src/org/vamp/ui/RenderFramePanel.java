package org.vamp.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JPanel;

public abstract class RenderFramePanel extends JPanel {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = -880391736204088408L;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final BufferedImage mRenderFrame;
	protected final int[] mRenderFrameBuffer;

	protected Dimension mVideoDimension;
	protected float mVideoFPS;
	protected boolean mVideoFPSFixed;

	// ===========================================================
	// Constructors
	// ===========================================================

	public RenderFramePanel(final BufferedImage pRenderFrame) {
		this.mRenderFrame = pRenderFrame;

		this.mRenderFrameBuffer = ((DataBufferInt) this.mRenderFrame.getRaster().getDataBuffer()).getData();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setVideoDimension(final int pVideoWidth, final int pVideoHeight) {
		this.mVideoDimension = new Dimension(pVideoWidth, pVideoHeight);
	}

	public void setVideoFPS(final float pVideoFPS) {
		if (!this.mVideoFPSFixed) {
			this.mVideoFPS = pVideoFPS;
		}
	}

	public void setVideoFPSFixed(final boolean pVideoFPSFixed) {
		this.mVideoFPSFixed = true;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void paint(final Graphics pGraphics) {
		super.paint(pGraphics);

		if (this.mVideoDimension == null) {
			/* Video didn't start yet! */
			return;
		}

		final float videoAspectRatio = (float) this.mVideoDimension.width / this.mVideoDimension.height;
		final float panelAspectRatio = (float) this.getWidth() / this.getHeight();

		final int drawX;
		final int drawY;
		final int drawWidth;
		final int drawHeight;
		if (videoAspectRatio > panelAspectRatio) {
			drawWidth = this.getWidth();
			drawHeight = Math.round(drawWidth / videoAspectRatio);

			drawX = 0;
			drawY = Math.round(0.5f * (this.getHeight() - drawHeight));
		} else {
			drawHeight = this.getHeight();
			drawWidth = Math.round(drawHeight * videoAspectRatio);

			drawY = 0;
			drawX = Math.round(0.5f * (this.getWidth() - drawWidth));
		}

		final Graphics2D graphics2D = (Graphics2D) pGraphics;
		graphics2D.drawImage(this.mRenderFrame, drawX, drawY, drawWidth, drawHeight, null);

		/* Simple frame to make it look a little nicer: */
		graphics2D.drawRect(drawX, drawY, drawWidth - 1, drawHeight - 1);

		// TODO Option do draw reference grid.
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}