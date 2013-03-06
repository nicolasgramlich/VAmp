package org.vamp;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

public class VAmp extends JFrame {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = -5466882865417468244L;

	private static final int WIDTH = 720;
	private static final int HEIGHT = 480;

	// ===========================================================
	// Fields
	// ===========================================================

	private final MediaPlayerFactory mMediaPlayerFactory;

	private final DirectMediaPlayer mMediaPlayer;

	private final InputRenderFramePanel mInputRenderFramePanel;
	private final OutputRenderFramePanel mOutputRenderFramePanel;

	private final RenderFrameCallback mRenderFrameCallback;

	// ===========================================================
	// Constructors
	// ===========================================================

	public VAmp(final String pMediaFilename, final String[] pVLCArgs) {
		super("VAmp");

		/* Create to input frame buffer that VLC will render into: */
		final BufferedImage inputFrame = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(VAmp.WIDTH, VAmp.HEIGHT);
		inputFrame.setAccelerationPriority(1.0f);

		/* Create the output frame buffer that our results will be displayed in: */ 
		final BufferedImage outputFrame = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(VAmp.WIDTH, VAmp.HEIGHT);
		outputFrame.setAccelerationPriority(1.0f);

		/* Create the input render frame panel that will display the input frame buffer: */
		this.mInputRenderFramePanel = new InputRenderFramePanel(inputFrame);
		this.mInputRenderFramePanel.setPreferredSize(new Dimension(VAmp.WIDTH, VAmp.HEIGHT));
		this.mInputRenderFramePanel.setSize(VAmp.WIDTH, VAmp.HEIGHT);

		/* Create the output render frame panel that will display the output frame buffer: */
		this.mOutputRenderFramePanel = new OutputRenderFramePanel(outputFrame, inputFrame);
		this.mOutputRenderFramePanel.setPreferredSize(new Dimension(VAmp.WIDTH, VAmp.HEIGHT));
		this.mOutputRenderFramePanel.setSize(VAmp.WIDTH, VAmp.HEIGHT);

		final Container contentPane = this.getContentPane();
		contentPane.setLayout(new GridLayout(1, 2));

		contentPane.add(this.mInputRenderFramePanel);
		contentPane.add(this.mOutputRenderFramePanel);

		/* Create the VLC media objects that will in the end play the video and render into the input frame buffer: */
		this.mMediaPlayerFactory = new MediaPlayerFactory(pVLCArgs);
		this.mRenderFrameCallback = new RenderFrameCallback(inputFrame) {
			@Override
			protected void onDisplay(final DirectMediaPlayer pDirectMediaPlayer, final int[] pARGBBuffer) {
				VAmp.this.mInputRenderFramePanel.repaint();
				VAmp.this.mOutputRenderFramePanel.notifyInputRenderFrameChanged();
				VAmp.this.mOutputRenderFramePanel.repaint();
			}
		};

		this.mMediaPlayer = this.mMediaPlayerFactory.newDirectMediaPlayer(new BufferFormatCallback() {
			@Override
			public BufferFormat getBufferFormat(final int pSourceWidth, final int pSourceHeight) {
				/* Let the RenderFramePanels know about the dimension of the video (for aspect ratio): */ 
				VAmp.this.mInputRenderFramePanel.setVideoDimension(pSourceWidth, pSourceHeight);
				VAmp.this.mOutputRenderFramePanel.setVideoDimension(pSourceWidth, pSourceHeight);

				return new RV32BufferFormat(VAmp.WIDTH, VAmp.HEIGHT);
			}
		}, this.mRenderFrameCallback);

		this.mMediaPlayer.playMedia(pMediaFilename);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent pWindowEvent) {
				VAmp.this.mMediaPlayer.release();
				VAmp.this.mMediaPlayerFactory.release();
				System.exit(0);
			}
		});
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