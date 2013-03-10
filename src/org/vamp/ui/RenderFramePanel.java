package org.vamp.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
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

		final Graphics2D graphics2D = (Graphics2D) pGraphics;

		final int width = this.getWidth();
		final int height = this.getHeight();

		if (this.mVideoDimension == null) {
			/* Video didn't start yet! */
			graphics2D.setColor(Color.WHITE);
			graphics2D.fillRect(0, 0, width - 1, height - 1);
			graphics2D.setColor(Color.BLACK);
			graphics2D.drawRect(0, 0, width - 1, height - 1);

			final String string = "VAmp";

			final FontMetrics fontMetrics = pGraphics.getFontMetrics();
			final int stringWidth = fontMetrics.stringWidth(string);
			final int fontDescent = fontMetrics.getDescent();

			graphics2D.setPaint(Color.BLACK);
			graphics2D.drawString(string, (width - stringWidth) * 0.5f, (height * 0.5f) + fontDescent);

			return;
		}

		final float videoAspectRatio = (float) this.mVideoDimension.width / this.mVideoDimension.height;
		final float panelAspectRatio = (float) width / height;

		final int drawX;
		final int drawY;
		final int drawWidth;
		final int drawHeight;
		if (videoAspectRatio > panelAspectRatio) {
			drawWidth = width;
			drawHeight = Math.round(drawWidth / videoAspectRatio);

			drawX = 0;
			drawY = Math.round(0.5f * (height - drawHeight));
		} else {
			drawHeight = height;
			drawWidth = Math.round(drawHeight * videoAspectRatio);

			drawY = 0;
			drawX = Math.round(0.5f * (width - drawWidth));
		}

		graphics2D.drawImage(this.mRenderFrame, drawX, drawY, drawWidth, drawHeight, null);

		/* Simple frame to make it look a little nicer: */
		graphics2D.setColor(Color.BLACK);
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