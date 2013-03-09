package org.vamp;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
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
import org.vamp.ui.WrapLayout;
import org.vamp.util.blur.BlurMode;

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

	public static final BlurMode BLUR_MODE_DEFAULT = BlurMode.GAUSSIAN;

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

	private final String[] mVLCArgs;
	private String mMediaFilename;
	private final JFileChooser mMediaFileChooser = new JFileChooser();

	private final MediaPlayerFactory mMediaPlayerFactory;

	private DirectMediaPlayer mMediaPlayer;

	private final BufferedImage mInputFrame;
	private final BufferedImage mOutputFrame;

	private final InputRenderFramePanel mInputRenderFramePanel;
	private final OutputRenderFramePanel mOutputRenderFramePanel;

	private final BufferFormatCallback mBufferFormatCallback = new BufferFormatCallback() {
		@Override
		public BufferFormat getBufferFormat(final int pSourceWidth, final int pSourceHeight) {
			/* Let the RenderFramePanels know about the dimension of the video (for aspect ratio): */
			VAmp.this.mInputRenderFramePanel.setVideoDimension(pSourceWidth, pSourceHeight);
			VAmp.this.mOutputRenderFramePanel.setVideoDimension(pSourceWidth, pSourceHeight);

			return new RV32BufferFormat(VAmp.WIDTH, VAmp.HEIGHT);
		}
	};

	private JPanel mMediaPanel;
	private JPanel mFramePanel;
	private JPanel mAmplificationPanel;
	private JPanel mBlurRadiusPanel;
	private JPanel mFrequencyPanel;

	private JSlider mFrameSlider;
	private boolean mFrameSliderIsChangedProgramatically;

	private JCheckBox mCameraCheckbox;
	private boolean mCameraCheckboxIsChangedProgramatically;

	private MediaPlayerEventAdapter mMediaPlayerEventAdapter = new MediaPlayerEventAdapter() {
		@Override
		public void mediaMetaChanged(final MediaPlayer pMediaPlayer, final int pMetaType) {
			VAmp.this.onMediaMetaChanged(pMediaPlayer, pMetaType);
		}
	};

	// ===========================================================
	// Constructors
	// ===========================================================

	public VAmp(final String pMediaFilename, final String[] pVLCArgs) {
		super("VAmp");

		this.mMediaFilename = pMediaFilename;
		this.mVLCArgs = pVLCArgs;

		this.mMediaFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		try {
			final File file = new File(new File(".").getCanonicalPath());
			this.mMediaFileChooser.setCurrentDirectory(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.mInputFrame = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(VAmp.WIDTH, VAmp.HEIGHT);
		this.mInputFrame.setAccelerationPriority(1.0f);

		this.mOutputFrame = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(VAmp.WIDTH, VAmp.HEIGHT);
		this.mOutputFrame.setAccelerationPriority(1.0f);

		/* Create the input render frame panel that will display the input frame buffer: */
		this.mInputRenderFramePanel = new InputRenderFramePanel(this.mInputFrame);
		this.mInputRenderFramePanel.setPreferredSize(new Dimension(VAmp.WIDTH, VAmp.HEIGHT));

		/* Create the output render frame panel that will display the output frame buffer: */
		this.mOutputRenderFramePanel = new OutputRenderFramePanel(this.mOutputFrame, this.mInputFrame);
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

		final JPanel controlsPanel = new JPanel(new WrapLayout());
		{
			final TitledBorder controlsPanelBorder = BorderFactory.createTitledBorder("Controls:");
			controlsPanel.setBorder(controlsPanelBorder);

			this.createMediaPanel();
			this.createFramePanel();
			this.createAmplificationPanel();
			this.createBlurRadiusPanel();
			this.createFrequencyPanel();

			controlsPanel.add(this.mMediaPanel);
			controlsPanel.add(this.mFramePanel);
			controlsPanel.add(this.mAmplificationPanel);
			controlsPanel.add(this.mBlurRadiusPanel);
			controlsPanel.add(this.mFrequencyPanel);
		}
		contentPane.add(controlsPanel, BorderLayout.NORTH);

		/* Create the VLC media objects that will in the end play the video and render into the input frame buffer: */
		this.mMediaPlayerFactory = new MediaPlayerFactory(this.mVLCArgs);
		this.playMedia(this.mMediaFilename);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent pWindowEvent) {
				if (VAmp.this.mMediaPlayer != null) {
					VAmp.this.mMediaPlayer.release();
				}
				VAmp.this.mMediaPlayerFactory.release();
				System.exit(0);
			}
		});
	}

	private void playMedia(final String pMediaFilename) {
		final RenderFrameCallback renderFrameCallback = new RenderFrameCallback(this.mInputFrame) {
			@Override
			protected void onDisplay(final DirectMediaPlayer pDirectMediaPlayer, final int[] pARGBBuffer) {
				VAmp.this.onFrame();
			}
		};

		this.mMediaPlayer = this.mMediaPlayerFactory.newDirectMediaPlayer(this.mBufferFormatCallback, renderFrameCallback);

		this.mMediaPlayer.setRepeat(true);

		this.mMediaPlayer.playMedia(pMediaFilename);

		this.mMediaPlayer.addMediaPlayerEventListener(this.mMediaPlayerEventAdapter);
	}

	private void onMediaMetaChanged(final MediaPlayer pMediaPlayer, final int pMetaType) {
		final float fps = pMediaPlayer.getFps();

		VAmp.this.mInputRenderFramePanel.setVideoFPS(fps);
		VAmp.this.mOutputRenderFramePanel.setVideoFPS(fps);

		final long length = pMediaPlayer.getLength();
		final int frames = Math.round(length / MILLISECONDS_PER_SECOND * fps);
		if (frames > 0 && fps > 0) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					// TODO Refactor a little:
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

	private void stopMedia() {
		if (this.mMediaPlayer != null) {
			this.mMediaPlayer.setRepeat(false);
			this.mMediaPlayer.removeMediaPlayerEventListener(this.mMediaPlayerEventAdapter);

			this.mMediaPlayer.pause();

//			this.mMediaPlayer.stop();

			this.mMediaPlayer.release();

			this.mMediaPlayer = null;
		}
	}

	private void onFrame() {
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

	private void setFrame(final int pFrame) {
		if (VAmp.this.mMediaPlayer != null) {
			final float fps = VAmp.this.mMediaPlayer.getFps();
			final long length = VAmp.this.mMediaPlayer.getLength();
			final int frames = Math.round(length / MILLISECONDS_PER_SECOND * fps);
			final long time = Math.round(fps * pFrame);

			VAmp.this.updateFrameTitledBorder(pFrame, frames);

			if (!VAmp.this.mFrameSliderIsChangedProgramatically) {
				if (!VAmp.this.mFrameSlider.getValueIsAdjusting()) {
					if (time > length) {
						VAmp.this.mMediaPlayer.setTime(length);
					} else {
						VAmp.this.mMediaPlayer.setTime(time);
					}
				}
			}
		}
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

	private void createMediaPanel() {
		this.mMediaPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder mediaPanelBorder = BorderFactory.createTitledBorder("Media:");
			this.mMediaPanel.setBorder(mediaPanelBorder);

			final JButton browseMediaFileButton = new JButton("Browse");
			browseMediaFileButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					final int result = VAmp.this.mMediaFileChooser.showOpenDialog(VAmp.this);

					if (result == JFileChooser.APPROVE_OPTION) {
						VAmp.this.stopMedia();
						final File selectedFile = VAmp.this.mMediaFileChooser.getSelectedFile();
						VAmp.this.mMediaFilename = selectedFile.getAbsolutePath();
						VAmp.this.playMedia(VAmp.this.mMediaFilename);

						VAmp.this.mCameraCheckboxIsChangedProgramatically = true;
						VAmp.this.mCameraCheckbox.setSelected(false);
						VAmp.this.mCameraCheckboxIsChangedProgramatically = false;
					}
				}
			});

			this.mCameraCheckbox = new JCheckBox("Camera", false);
			this.mCameraCheckbox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent pItemEvent) {
					if (!VAmp.this.mCameraCheckboxIsChangedProgramatically) {
						switch (pItemEvent.getStateChange()) {
							case ItemEvent.SELECTED:
								VAmp.this.stopMedia();
								VAmp.this.playMedia("qtcapture://");
								VAmp.this.mOutputRenderFramePanel.setVideoFPS(30);
								VAmp.this.mOutputRenderFramePanel.setVideoFPSFixed(true);
								break;
							case ItemEvent.DESELECTED:
								VAmp.this.stopMedia();
								VAmp.this.mOutputRenderFramePanel.setVideoFPSFixed(false);
								VAmp.this.playMedia(VAmp.this.mMediaFilename);
								break;
						}
					}
				}
			});

			this.mMediaPanel.add(browseMediaFileButton);
			this.mMediaPanel.add(mCameraCheckbox);
		}
	}

	private void createFramePanel() {
		this.mFramePanel = new JPanel(new FlowLayout());
		{
			final TitledBorder framePanelBorder = BorderFactory.createTitledBorder("");
			this.mFramePanel.setBorder(framePanelBorder);
			this.updateFrameTitledBorder(0, 0);

			this.mFrameSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 30, 0);

			this.mFrameSlider.setPaintLabels(true);
			this.mFrameSlider.setPaintTicks(true);
			this.mFrameSlider.setMajorTickSpacing(30);
			this.mFrameSlider.setMinorTickSpacing(10);

			this.mFrameSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent pChangeEvent) {
					final int frame = VAmp.this.mFrameSlider.getValue();

					VAmp.this.setFrame(frame);
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

			amplificationAbsoluteCheckbox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent pItemEvent) {
					switch (pItemEvent.getStateChange()) {
						case ItemEvent.SELECTED:
						case ItemEvent.DESELECTED:
							final boolean amplificationAbsolute = amplificationAbsoluteCheckbox.isSelected();
							VAmp.this.mOutputRenderFramePanel.setAmplificationAbsolute(amplificationAbsolute);
							break;
					} 
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

			final JCheckBox blurModeCheckbox;
			switch (VAmp.BLUR_MODE_DEFAULT) {
				case GAUSSIAN:
					blurModeCheckbox = new JCheckBox("Gaussian", true);
					break;
				case BOX:
					blurModeCheckbox = new JCheckBox("Box", false);
					break;
				default:
					throw new IllegalArgumentException("Unexpected " + BlurMode.class.getSimpleName() + ": '" + VAmp.BLUR_MODE_DEFAULT + "'.");
			}
			blurModeCheckbox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent pItemEvent) {
					switch (pItemEvent.getStateChange()) {
						case ItemEvent.SELECTED:
							blurModeCheckbox.setText("Gaussian");
							VAmp.this.mOutputRenderFramePanel.setBlurMode(BlurMode.GAUSSIAN);
							break;
						case ItemEvent.DESELECTED:
							blurModeCheckbox.setText("Box");
							VAmp.this.mOutputRenderFramePanel.setBlurMode(BlurMode.BOX);
							break;
					} 
				}
			});

			this.mBlurRadiusPanel.add(blurRadiusSlider);
			this.mBlurRadiusPanel.add(blurModeCheckbox);
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