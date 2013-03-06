package org.vamp;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.vamp.ui.InputRenderFramePanel;
import org.vamp.ui.OutputRenderFramePanel;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
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

	private static final int WIDTH = 640;
	private static final int HEIGHT = 352;

	public static final int AMPLIFICATION_MAX = 25;
	public static final float AMPLIFICATION_DEFAULT = 2.5f;

	private static final int AMPLIFICATION_SLIDER_FACTOR = 10;

	private static final int AMPLIFICATION_SLIDER_MAX = AMPLIFICATION_MAX * AMPLIFICATION_SLIDER_FACTOR;
	private static final int AMPLIFICATION_SLIDER_DEFAULT = Math.round(AMPLIFICATION_DEFAULT * AMPLIFICATION_SLIDER_FACTOR);
	private static final int AMPLIFICATION_SLIDER_STEP = 50;

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

		/* Create the output render frame panel that will display the output frame buffer: */
		this.mOutputRenderFramePanel = new OutputRenderFramePanel(outputFrame, inputFrame);
		this.mOutputRenderFramePanel.setPreferredSize(new Dimension(VAmp.WIDTH, VAmp.HEIGHT));

		final Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		final JPanel centerPanel = new JPanel();
		{
			centerPanel.setLayout(new GridLayout(1, 2));
	
			centerPanel.add(this.mInputRenderFramePanel);
			centerPanel.add(this.mOutputRenderFramePanel);
		}
		contentPane.add(centerPanel, BorderLayout.CENTER);

		final JPanel controlsPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder controlsPanelBorder = BorderFactory.createTitledBorder("Controls:");
			controlsPanel.setBorder(controlsPanelBorder);
			
			final JPanel amplificationPanel = new JPanel(new FlowLayout());
			{
				final TitledBorder amplificationPanelBorder = BorderFactory.createTitledBorder("Amplification:");
				amplificationPanel.setBorder(amplificationPanelBorder);

				final JLabel amplificationSliderLabel = new JLabel(String.valueOf(AMPLIFICATION_DEFAULT) + "x", (Icon)null, 0);

				final JSlider amplificationSlider = new JSlider(JSlider.HORIZONTAL, 0, AMPLIFICATION_SLIDER_MAX, AMPLIFICATION_SLIDER_DEFAULT);
				Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
				for (int i = 0; i <= AMPLIFICATION_SLIDER_MAX; i += AMPLIFICATION_SLIDER_STEP) {
					labels.put(i, new JLabel((i / AMPLIFICATION_SLIDER_FACTOR) + "x"));
				}
				amplificationSlider.setLabelTable(labels);

				amplificationSlider.setMajorTickSpacing(10);
				amplificationSlider.setPaintLabels(true);
				amplificationSlider.setMinorTickSpacing(1);
				amplificationSlider.setPaintTicks(true);

				amplificationSlider.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent pChangeEvent) {
//					if(!amplificationSlider.getValueIsAdjusting()) {
						final float amplification = ((float)amplificationSlider.getValue() / AMPLIFICATION_SLIDER_FACTOR);
						amplificationSliderLabel.setText(String.valueOf(amplification) + "x");
						mOutputRenderFramePanel.setAmplification(amplification, amplification, amplification);
//					}
					}
				});
				amplificationPanel.add(amplificationSlider);
				amplificationPanel.add(amplificationSliderLabel);
			}
			controlsPanel.add(amplificationPanel);
		}
		contentPane.add(controlsPanel, BorderLayout.NORTH);

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

		this.mMediaPlayer.setRepeat(true);
		this.mMediaPlayer.playMedia(pMediaFilename);
		this.mMediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			@Override
			public void timeChanged(final MediaPlayer pMediaPlayer, final long pTime) {
//				System.out.println(pTime);
			}
		});

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