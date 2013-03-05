package org.vamp;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;

public abstract class RenderFrameCallback extends RenderCallbackAdapter {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public RenderFrameCallback(final BufferedImage pBufferedImage) {
		super(((DataBufferInt) pBufferedImage.getRaster().getDataBuffer()).getData());
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

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}