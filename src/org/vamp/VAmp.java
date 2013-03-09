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
import javax.swing.SwingUtilities;
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

	private static final float MILLISECONDS_PER_SECOND = 1000;

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

	private final String mMediaFilename;

	private final MediaPlayerFactory mMediaPlayerFactory;

	private final DirectMediaPlayer mMediaPlayer;

	private final InputRenderFramePanel mInputRenderFramePanel;
	private final OutputRenderFramePanel mOutputRenderFramePanel;

	private final RenderFrameCallback mRenderFrameCallback;

	private JPanel mFramePanel;
	private JPanel mAmplificationPanel;
	private JPanel mBlurRadiusPanel;
	private JPanel mFrequencyPanel;

	private JSlider mFrameSlider;
	private boolean mFrameSliderIsChangedProgramatically;

	// ===========================================================
	// Constructors
	// ===========================================================

	public VAmp(final String pMediaFilename, final String[] pVLCArgs) {
		super("VAmp");

		this.mMediaFilename = pMediaFilename;

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

			this.createFramePanel();
			this.createAmplificationPanel();
			this.createBlurRadiusPanel();
			this.createFrequencyPanel();

			controlsPanel.add(this.mFramePanel);
			controlsPanel.add(this.mAmplificationPanel);
			controlsPanel.add(this.mBlurRadiusPanel);
			controlsPanel.add(this.mFrequencyPanel);
		}
		contentPane.add(controlsPanel, BorderLayout.NORTH);

		/* Create the VLC media objects that will in the end play the video and render into the input frame buffer: */
		this.mMediaPlayerFactory = new MediaPlayerFactory(pVLCArgs);
		this.mRenderFrameCallback = new RenderFrameCallback(inputFrame) {
			@Override
			protected void onDisplay(final DirectMediaPlayer pDirectMediaPlayer, final int[] pARGBBuffer) {
				VAmp.this.onFrame();
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
		this.mMediaPlayer.playMedia(this.mMediaFilename);

		this.mMediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			@Override
			public void mediaMetaChanged(final MediaPlayer pMediaPlayer, final int pMetaType) {
				final float fps = VAmp.this.mMediaPlayer.getFps();

				VAmp.this.mInputRenderFramePanel.setVideoFPS(fps);
				VAmp.this.mOutputRenderFramePanel.setVideoFPS(fps);

				final long length = VAmp.this.mMediaPlayer.getLength();
				final int frames = Math.round(length / MILLISECONDS_PER_SECOND * fps);
				if (frames > 0 && fps > 0) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							final int desiredMajorTicks = (int)(frames / fps);
							final int majorTicks = Math.min(desiredMajorTicks, 6);
	
							final int majorTickSpacing = (int)Math.round(fps) * (int)Math.ceil((float)desiredMajorTicks / majorTicks);
							final int minorTickSpacing = majorTickSpacing / 3;
	
							VAmp.this.mFrameSlider.setMajorTickSpacing(majorTickSpacing);
							VAmp.this.mFrameSlider.setMinorTickSpacing(minorTickSpacing);
							VAmp.this.mFrameSlider.setLabelTable(VAmp.this.mFrameSlider.createStandardLabels(majorTickSpacing));
							VAmp.this.mFrameSlider.setMaximum(frames);
							VAmp.this.mFrameSlider.repaint();
						}
					});
				}
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

	protected void onFrame() {
		final float fps = this.mMediaPlayer.getFps();
		final long time = this.mMediaPlayer.getTime();
		final int frame = Math.round(time / MILLISECONDS_PER_SECOND * fps);

		if (!VAmp.this.mFrameSlider.getValueIsAdjusting()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					VAmp.this.mFrameSliderIsChangedProgramatically = true;
					VAmp.this.mFrameSlider.setValue(frame);
					VAmp.this.mFrameSliderIsChangedProgramatically = false;
				}
			});
		}

		VAmp.this.mInputRenderFramePanel.repaint();
		VAmp.this.mOutputRenderFramePanel.notifyInputRenderFrameChanged();
		VAmp.this.mOutputRenderFramePanel.repaint();
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

	private void createFramePanel() {
		this.mFramePanel = new JPanel(new FlowLayout());
		{
			final TitledBorder framePanelBorder = BorderFactory.createTitledBorder("");
			this.mFramePanel.setBorder(framePanelBorder);
			this.updateFrameTitledBorder(0, 0);

			this.mFrameSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 60, 0);

			this.mFrameSlider.setPaintLabels(true);
			this.mFrameSlider.setPaintTicks(true);
			this.mFrameSlider.setMajorTickSpacing(30);
			this.mFrameSlider.setMinorTickSpacing(10);

			this.mFrameSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final int frame = VAmp.this.mFrameSlider.getValue();

					final float fps = VAmp.this.mMediaPlayer.getFps();
					final long length = VAmp.this.mMediaPlayer.getLength();
					final int frames = Math.round(length / MILLISECONDS_PER_SECOND * fps);

					VAmp.this.updateFrameTitledBorder(frame, frames);

					if (!VAmp.this.mFrameSliderIsChangedProgramatically) {
						if (!VAmp.this.mFrameSlider.getValueIsAdjusting()) {
							final long time = Math.round(fps * frame);
							VAmp.this.mMediaPlayer.setTime(time);
						}
					}
				}
			});

			this.mFramePanel.add(this.mFrameSlider);
		}
	}

	private void createAmplificationPanel() {
		this.mAmplificationPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder amplificationPanelBorder = BorderFactory.createTitledBorder("");
			this.mAmplificationPanel.setBorder(amplificationPanelBorder);
			this.updateAmplificationTitledBorder(VAmp.AMPLIFICATION_DEFAULT);

			final JSlider amplificationSlider = new JSlider(SwingConstants.HORIZONTAL, VAmp.AMPLIFICATION_SLIDER_MIN, VAmp.AMPLIFICATION_SLIDER_MAX, VAmp.AMPLIFICATION_SLIDER_DEFAULT);
			final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= VAmp.AMPLIFICATION_SLIDER_MAX; i += VAmp.AMPLIFICATION_SLIDER_LABEL_STEP) {
				labels.put(i, new JLabel((i / VAmp.AMPLIFICATION_SLIDER_FACTOR) + "x"));
			}
			amplificationSlider.setLabelTable(labels);

			amplificationSlider.setPaintLabels(true);
			amplificationSlider.setPaintTicks(true);
			amplificationSlider.setMajorTickSpacing(10 * VAmp.AMPLIFICATION_SLIDER_FACTOR);
			amplificationSlider.setMinorTickSpacing(1 * VAmp.AMPLIFICATION_SLIDER_FACTOR);

			amplificationSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final float amplification = ((float) amplificationSlider.getValue() / VAmp.AMPLIFICATION_SLIDER_FACTOR);
					updateAmplificationTitledBorder(amplification);
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

			this.mAmplificationPanel.add(amplificationSlider);
			this.mAmplificationPanel.add(amplificationAbsoluteCheckbox);
		}
	}

	private void createBlurRadiusPanel() {
		this.mBlurRadiusPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder blurRadiusPanelBorder = BorderFactory.createTitledBorder("");
			this.mBlurRadiusPanel.setBorder(blurRadiusPanelBorder);
			this.updateBlurRadiusTitledBorder(VAmp.BLUR_RADIUS_DEFAULT);

			final JSlider blurRadiusSlider = new JSlider(SwingConstants.HORIZONTAL, VAmp.BLUR_RADIUS_SLIDER_MIN, VAmp.BLUR_RADIUS_SLIDER_MAX, VAmp.BLUR_RADIUS_SLIDER_DEFAULT);
			final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= VAmp.BLUR_RADIUS_SLIDER_MAX; i += VAmp.BLUR_RADIUS_SLIDER_LABEL_STEP) {
				labels.put(i, new JLabel(i + "px"));
			}
			blurRadiusSlider.setLabelTable(labels);

			blurRadiusSlider.setPaintLabels(true);
			blurRadiusSlider.setPaintTicks(true);
			blurRadiusSlider.setMajorTickSpacing(5);
			blurRadiusSlider.setMinorTickSpacing(1);

			blurRadiusSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final int blurRadius = blurRadiusSlider.getValue();
					VAmp.this.updateBlurRadiusTitledBorder(blurRadius);
					VAmp.this.mOutputRenderFramePanel.setBlurRadius(blurRadius);
				}
			});

			this.mBlurRadiusPanel.add(blurRadiusSlider);
		}
	}

	private void createFrequencyPanel() {
		this.mFrequencyPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder frequencyPanelBorder = BorderFactory.createTitledBorder("");
			this.mFrequencyPanel.setBorder(frequencyPanelBorder);
			this.updateFrequencyTitledBorder(VAmp.FREQUENCY_DEFAULT);

			final JSlider frequencySlider = new JSlider(SwingConstants.HORIZONTAL, VAmp.FREQUENCY_SLIDER_MIN, VAmp.FREQUENCY_SLIDER_MAX, VAmp.FREQUENCY_SLIDER_DEFAULT);
			final Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for (int i = 0; i <= VAmp.FREQUENCY_SLIDER_MAX; i += VAmp.FREQUENCY_SLIDER_LABEL_STEP) {
				labels.put(i, new JLabel((i / VAmp.FREQUENCY_SLIDER_FACTOR) + "Hz"));
			}
			frequencySlider.setLabelTable(labels);

			frequencySlider.setPaintLabels(true);
			frequencySlider.setPaintTicks(true);
			frequencySlider.setMajorTickSpacing(5 * VAmp.FREQUENCY_SLIDER_FACTOR);
			frequencySlider.setMinorTickSpacing(1 * VAmp.FREQUENCY_SLIDER_FACTOR);

			frequencySlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final float frequency = ((float) frequencySlider.getValue() / VAmp.FREQUENCY_SLIDER_FACTOR);
					VAmp.this.updateFrequencyTitledBorder(frequency);
					VAmp.this.mOutputRenderFramePanel.setFrequency(frequency);
				}
			});

			this.mFrequencyPanel.add(frequencySlider);
		}
	}

	private void updateFrameTitledBorder(final int pFrame, final int pFrames) {
		final TitledBorder framePanelBorder = (TitledBorder)VAmp.this.mFramePanel.getBorder();
		framePanelBorder.setTitle("Frame: " + String.valueOf(pFrame) + "/" + String.valueOf(pFrames));
		this.mFramePanel.repaint();
	}

	private void updateAmplificationTitledBorder(final float pAmplification) {
		final TitledBorder amplificationPanelBorder = (TitledBorder)VAmp.this.mAmplificationPanel.getBorder();
		amplificationPanelBorder.setTitle("Amplification: " + String.valueOf(pAmplification) + "x");
		this.mAmplificationPanel.repaint();
	}

	private void updateBlurRadiusTitledBorder(final int pBlurRadius) {
		final TitledBorder blurRadiusPanelBorder = (TitledBorder)VAmp.this.mBlurRadiusPanel.getBorder();
		blurRadiusPanelBorder.setTitle("Blur-Radius: " + String.valueOf(pBlurRadius) + "px");
		this.mBlurRadiusPanel.repaint();
	}

	private void updateFrequencyTitledBorder(final float pFrequency) {
		final TitledBorder frequencyPanelBorder = (TitledBorder)VAmp.this.mFrequencyPanel.getBorder();
		frequencyPanelBorder.setTitle("Frequency: " + String.valueOf(pFrequency) + "Hz");
		this.mFrequencyPanel.repaint();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}