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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
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

	public static final int AMPLIFICATION_MIN = 0;
	public static final int AMPLIFICATION_MAX = 50;
	public static final float AMPLIFICATION_DEFAULT = 5;

	private static final int AMPLIFICATION_SLIDER_FACTOR = 10;

	private static final int AMPLIFICATION_SLIDER_MIN = VAmp.AMPLIFICATION_MIN * VAmp.AMPLIFICATION_SLIDER_FACTOR;
	private static final int AMPLIFICATION_SLIDER_MAX = VAmp.AMPLIFICATION_MAX * VAmp.AMPLIFICATION_SLIDER_FACTOR;
	private static final int AMPLIFICATION_SLIDER_DEFAULT = Math.round(VAmp.AMPLIFICATION_DEFAULT * VAmp.AMPLIFICATION_SLIDER_FACTOR);
	private static final int AMPLIFICATION_SLIDER_LABEL_STEP = 10 * VAmp.AMPLIFICATION_SLIDER_FACTOR;

	public static final int BLUR_RADIUS_MIN = 0;
	public static final int BLUR_RADIUS_MAX = 50;
	public static final int BLUR_RADIUS_DEFAULT = 5;

	private static final int BLUR_RADIUS_SLIDER_MIN = VAmp.BLUR_RADIUS_MIN;
	private static final int BLUR_RADIUS_SLIDER_MAX = VAmp.BLUR_RADIUS_MAX;
	private static final int BLUR_RADIUS_SLIDER_DEFAULT = VAmp.BLUR_RADIUS_DEFAULT;
	private static final int BLUR_RADIUS_SLIDER_LABEL_STEP = 10;

	public static final int FREQUENCY_MIN = 0;
	public static final int FREQUENCY_MAX = 30;
	public static final float FREQUENCY_DEFAULT = 1.5f;

	private static final int FREQUENCY_SLIDER_FACTOR = 10;

	private static final int FREQUENCY_SLIDER_MIN = VAmp.FREQUENCY_MIN * VAmp.FREQUENCY_SLIDER_FACTOR;
	private static final int FREQUENCY_SLIDER_MAX = VAmp.FREQUENCY_MAX * VAmp.FREQUENCY_SLIDER_FACTOR;
	private static final int FREQUENCY_SLIDER_DEFAULT = Math.round(VAmp.FREQUENCY_DEFAULT * VAmp.FREQUENCY_SLIDER_FACTOR);
	private static final int FREQUENCY_SLIDER_LABEL_STEP = 10 * VAmp.FREQUENCY_SLIDER_FACTOR;

	public static final boolean AMPLIFICATION_ABSOLUTE_DEFAULT = false;

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

			final JPanel inputRenderFramePanelContainer = new JPanel(new GridLayout());
			{
				inputRenderFramePanelContainer.setBorder(BorderFactory.createTitledBorder("Input:"));
				inputRenderFramePanelContainer.add(this.mInputRenderFramePanel);
			}
			centerPanel.add(inputRenderFramePanelContainer);

			final JPanel outputRenderFramePanelContainer = new JPanel(new GridLayout());
			{
				outputRenderFramePanelContainer.setBorder(BorderFactory.createTitledBorder("Output:"));
				outputRenderFramePanelContainer.add(this.mOutputRenderFramePanel);
			}
			centerPanel.add(outputRenderFramePanelContainer);
		}
		contentPane.add(centerPanel, BorderLayout.CENTER);

		final JPanel controlsPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder controlsPanelBorder = BorderFactory.createTitledBorder("Controls:");
			controlsPanel.setBorder(controlsPanelBorder);

			final JPanel amplificationPanel = this.createAmplificationPanel();
			final JPanel blurRadiusPanel = this.createBlurRadiusPanel();
			final JPanel frequencyPanel = this.createFrequencyPanel();

			controlsPanel.add(amplificationPanel);
			controlsPanel.add(blurRadiusPanel);
			controlsPanel.add(frequencyPanel);
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

			@Override
			public void mediaMetaChanged(MediaPlayer pMediaPlayer, int pMetaType) {
				final float videoFPS = VAmp.this.mMediaPlayer.getFps();

				VAmp.this.mInputRenderFramePanel.setVideoFPS(videoFPS);
				VAmp.this.mOutputRenderFramePanel.setVideoFPS(videoFPS);
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

	private JPanel createAmplificationPanel() {
		final JPanel amplificationPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder amplificationPanelBorder = BorderFactory.createTitledBorder("");
			this.updateAmplificationTitledBorder(amplificationPanelBorder, VAmp.AMPLIFICATION_DEFAULT, amplificationPanel);
			amplificationPanel.setBorder(amplificationPanelBorder);

			final JSlider amplificationSlider = new JSlider(SwingConstants.HORIZONTAL, VAmp.AMPLIFICATION_SLIDER_MIN, VAmp.AMPLIFICATION_SLIDER_MAX, VAmp.AMPLIFICATION_SLIDER_DEFAULT);
			final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= VAmp.AMPLIFICATION_SLIDER_MAX; i += VAmp.AMPLIFICATION_SLIDER_LABEL_STEP) {
				labels.put(i, new JLabel((i / VAmp.AMPLIFICATION_SLIDER_FACTOR) + "x"));
			}
			amplificationSlider.setLabelTable(labels);

			amplificationSlider.setMajorTickSpacing(10 * VAmp.AMPLIFICATION_SLIDER_FACTOR);
			amplificationSlider.setPaintLabels(true);
			amplificationSlider.setMinorTickSpacing(1 * VAmp.AMPLIFICATION_SLIDER_FACTOR);
			amplificationSlider.setPaintTicks(true);

			amplificationSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final float amplification = ((float) amplificationSlider.getValue() / VAmp.AMPLIFICATION_SLIDER_FACTOR);
					amplificationPanelBorder.setTitle("Amplification: " + String.valueOf(amplification) + "x");
					amplificationPanel.repaint();
					VAmp.this.mOutputRenderFramePanel.setAmplification(amplification, amplification, amplification);
				}
			});

			final JCheckBox amplificationAbsoluteCheckbox = new JCheckBox("Absolute", VAmp.AMPLIFICATION_ABSOLUTE_DEFAULT);
			amplificationAbsoluteCheckbox.setToolTipText("<html>Take the absolute of the amplification delta instead of the signed amplification delta.<br/>The output image will appear brighter and the effect may or may not be better visible.</html>");

			amplificationAbsoluteCheckbox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final boolean amplificationAbsolute = amplificationAbsoluteCheckbox.isSelected();
					VAmp.this.mOutputRenderFramePanel.setAmplificationAbsolute(amplificationAbsolute);
				}
			});

			amplificationPanel.add(amplificationSlider);
			amplificationPanel.add(amplificationAbsoluteCheckbox);
		}
		return amplificationPanel;
	}

	private JPanel createBlurRadiusPanel() {
		final JPanel blurRadiusPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder blurRadiusPanelBorder = BorderFactory.createTitledBorder("");
			this.updateBlurRadiusTitledBorder(blurRadiusPanelBorder, VAmp.BLUR_RADIUS_DEFAULT, blurRadiusPanel);
			blurRadiusPanel.setBorder(blurRadiusPanelBorder);

			final JSlider blurRadiusSlider = new JSlider(SwingConstants.HORIZONTAL, VAmp.BLUR_RADIUS_SLIDER_MIN, VAmp.BLUR_RADIUS_SLIDER_MAX, VAmp.BLUR_RADIUS_SLIDER_DEFAULT);
			final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= VAmp.BLUR_RADIUS_SLIDER_MAX; i += VAmp.BLUR_RADIUS_SLIDER_LABEL_STEP) {
				labels.put(i, new JLabel(i + "px"));
			}
			blurRadiusSlider.setLabelTable(labels);

			blurRadiusSlider.setMajorTickSpacing(5);
			blurRadiusSlider.setPaintLabels(true);
			blurRadiusSlider.setMinorTickSpacing(1);
			blurRadiusSlider.setPaintTicks(true);

			blurRadiusSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final int blurRadius = blurRadiusSlider.getValue();
					VAmp.this.updateBlurRadiusTitledBorder(blurRadiusPanelBorder, blurRadius, blurRadiusPanel);
					VAmp.this.mOutputRenderFramePanel.setBlurRadius(blurRadius);
				}
			});

			blurRadiusPanel.add(blurRadiusSlider);
		}
		return blurRadiusPanel;
	}

	private JPanel createFrequencyPanel() {
		final JPanel frequencyPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder frequencyPanelBorder = BorderFactory.createTitledBorder("");
			this.updateFrequencyTitledBorder(frequencyPanelBorder, VAmp.FREQUENCY_DEFAULT, frequencyPanel);
			frequencyPanel.setBorder(frequencyPanelBorder);

			final JSlider frequencySlider = new JSlider(SwingConstants.HORIZONTAL, VAmp.FREQUENCY_SLIDER_MIN, VAmp.FREQUENCY_SLIDER_MAX, VAmp.FREQUENCY_SLIDER_DEFAULT);
			final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= VAmp.FREQUENCY_SLIDER_MAX; i += VAmp.FREQUENCY_SLIDER_LABEL_STEP) {
				labels.put(i, new JLabel((i / VAmp.FREQUENCY_SLIDER_FACTOR) + "Hz"));
			}
			frequencySlider.setLabelTable(labels);

			frequencySlider.setMajorTickSpacing(5 * VAmp.FREQUENCY_SLIDER_FACTOR);
			frequencySlider.setPaintLabels(true);
			frequencySlider.setMinorTickSpacing(1 * VAmp.FREQUENCY_SLIDER_FACTOR);
			frequencySlider.setPaintTicks(true);

			frequencySlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final float frequency = ((float) frequencySlider.getValue() / VAmp.FREQUENCY_SLIDER_FACTOR);
					VAmp.this.updateFrequencyTitledBorder(frequencyPanelBorder, frequency, frequencyPanel);
					VAmp.this.mOutputRenderFramePanel.setFrequency(frequency);
				}
			});

			frequencyPanel.add(frequencySlider);
		}
		return frequencyPanel;
	}

	private void updateAmplificationTitledBorder(final TitledBorder pTitledBorder, final float pAmplification, final JPanel pTitledBorderPanel) {
		pTitledBorder.setTitle("Amplification: " + String.valueOf(pAmplification) + "x");
		pTitledBorderPanel.repaint();
	}

	private void updateBlurRadiusTitledBorder(final TitledBorder pTitledBorder, final int pBlurRadius, final JPanel pTitledBorderPanel) {
		pTitledBorder.setTitle("Blur-Radius: " + String.valueOf(pBlurRadius) + "px");
		pTitledBorderPanel.repaint();
	}

	private void updateFrequencyTitledBorder(final TitledBorder pTitledBorder, final float pFrequency, final JPanel pTitledBorderPanel) {
		pTitledBorder.setTitle("Frequency: " + String.valueOf(pFrequency) + "Hz");
		pTitledBorderPanel.repaint();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}