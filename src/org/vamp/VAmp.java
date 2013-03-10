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
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.vamp.recorder.Recorder;
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
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

import com.sun.jna.Memory;

public class VAmp extends JFrame implements RenderCallback {
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

	public static final int FPS_DEFAULT = 30;

	// ===========================================================
	// Fields
	// ===========================================================

	private final String[] mVLCArgs;
	private String mMediaFilename;
	private final JFileChooser mMediaFileChooser = new JFileChooser();

	private final Recorder mRecorder;
	private final JFileChooser mRecordFileChooser = new JFileChooser();

	private final MediaPlayerFactory mMediaPlayerFactory;

	private DirectMediaPlayer mMediaPlayer;

	private BufferedImage mInputRenderFrame;
	private BufferedImage mOutputRenderFrame;

	private final InputRenderFramePanel mInputRenderFramePanel;
	private final OutputRenderFramePanel mOutputRenderFramePanel;

	private final BufferFormatCallback mBufferFormatCallback = new BufferFormatCallback() {
		@Override
		public BufferFormat getBufferFormat(final int pSourceWidth, final int pSourceHeight) {
			/* Let the RenderFramePanels know about the dimension of the video (for aspect ratio): */
			VAmp.this.mInputRenderFramePanel.setVideoDimension(pSourceWidth, pSourceHeight);
			VAmp.this.mOutputRenderFramePanel.setVideoDimension(pSourceWidth, pSourceHeight);

			VAmp.this.updateRenderFrames(pSourceWidth, pSourceHeight);

			return new RV32BufferFormat(pSourceWidth, pSourceHeight);
		}
	};

	private JPanel mMediaPanel;
	private JPanel mFramePanel;
	private JPanel mAmplificationPanel;
	private JPanel mBlurRadiusPanel;
	private JPanel mFrequencyPanel;
	private JPanel mRecordPanel;

	private JSlider mFrameSlider;
	private boolean mFrameSliderIsChangedProgramatically;

	private JCheckBox mCameraCheckbox;
	private boolean mCameraCheckboxIsChangedProgramatically;

	private final MediaPlayerEventAdapter mMediaPlayerEventAdapter = new MediaPlayerEventAdapter() {
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
		this.mRecordFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		try {
			final File file = new File(new File(".").getCanonicalPath());
			this.mMediaFileChooser.setCurrentDirectory(file);
			this.mRecordFileChooser.setCurrentDirectory(file);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		/* Create recorder: */
		this.mRecorder = new Recorder(this.mInputRenderFrame, this.mOutputRenderFrame);

		/* Create the input render frame panel that will display the input frame buffer: */
		this.mInputRenderFramePanel = new InputRenderFramePanel(this.mInputRenderFrame);
		this.mInputRenderFramePanel.setPreferredSize(new Dimension(VAmp.WIDTH, VAmp.HEIGHT));

		/* Create the output render frame panel that will display the output frame buffer: */
		this.mOutputRenderFramePanel = new OutputRenderFramePanel(this.mOutputRenderFrame, this.mInputRenderFrame);
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
			this.createRecordPanel();

			controlsPanel.add(this.mMediaPanel);
			controlsPanel.add(this.mFramePanel);
			controlsPanel.add(this.mAmplificationPanel);
			controlsPanel.add(this.mBlurRadiusPanel);
			controlsPanel.add(this.mFrequencyPanel);
			controlsPanel.add(this.mRecordPanel);
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

	protected void updateRenderFrames(final int pSourceWidth, final int pSourceHeight) {
		this.mInputRenderFrame = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(pSourceWidth, pSourceHeight);
		this.mInputRenderFrame.setAccelerationPriority(1.0f);

		this.mOutputRenderFrame = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(pSourceWidth, pSourceHeight);
		this.mOutputRenderFrame.setAccelerationPriority(1.0f);

		this.mRecorder.setInputRenderFrame(this.mInputRenderFrame);
		this.mRecorder.setOutputRenderFrame(this.mOutputRenderFrame);

		this.mInputRenderFramePanel.setRenderFrame(this.mInputRenderFrame);

		this.mOutputRenderFramePanel.setInputRenderFrame(this.mInputRenderFrame);
		this.mOutputRenderFramePanel.setOutputRenderFrame(this.mOutputRenderFrame);
	}

	private void playMedia(final String pMediaFilename) {
		this.mMediaPlayer = this.mMediaPlayerFactory.newDirectMediaPlayer(this.mBufferFormatCallback, this);

		this.mMediaPlayer.setRepeat(true);

		this.mMediaPlayer.playMedia(pMediaFilename);

		this.mMediaPlayer.addMediaPlayerEventListener(this.mMediaPlayerEventAdapter);
	}

	private void onMediaMetaChanged(final MediaPlayer pMediaPlayer, final int pMetaType) {
		final float fps = pMediaPlayer.getFps();

		VAmp.this.mInputRenderFramePanel.setVideoFPS(fps);
		VAmp.this.mOutputRenderFramePanel.setVideoFPS(fps);
		VAmp.this.mRecorder.setFPS(Math.round(fps));

		final long length = pMediaPlayer.getLength();
		final int frames = Math.round((length / VAmp.MILLISECONDS_PER_SECOND) * fps);
		if ((frames > 0) && (fps > 0)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					// TODO Refactor a little:
					final int desiredMajorTicks = (int) (frames / fps);
					final int majorTicks = Math.min(desiredMajorTicks, 6);

					final int majorTickSpacing = Math.round(fps) * (int) Math.ceil((float) desiredMajorTicks / majorTicks);
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

	@Override
	public void display(final DirectMediaPlayer pMediaPlayer, final Memory[] pNativeBuffer, final BufferFormat pBufferFormat) {
		final int width = pBufferFormat.getWidth();
		final int height = pBufferFormat.getHeight();

		final int[] argbBuffer = ((DataBufferInt) this.mInputRenderFrame.getRaster().getDataBuffer()).getData();

		final long size = pNativeBuffer[0].size();
		final IntBuffer intBuffer = pNativeBuffer[0].getByteBuffer(0L, size).asIntBuffer();
		intBuffer.get(argbBuffer, 0, width * height);

		this.onFrame();
	}

	private void onFrame() {
		final float fps = this.mMediaPlayer.getFps();
		final long time = this.mMediaPlayer.getTime();
		final int frame = Math.round((time / VAmp.MILLISECONDS_PER_SECOND) * fps);

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

		this.mInputRenderFramePanel.repaint();
		this.mOutputRenderFramePanel.notifyInputRenderFrameChanged();
		this.mOutputRenderFramePanel.repaint();

		try {
			this.mRecorder.onFrame();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void setFrame(final int pFrame) {
		if (this.mMediaPlayer != null) {
			final float fps = this.mMediaPlayer.getFps();
			final long length = this.mMediaPlayer.getLength();
			final int frames = Math.round((length / VAmp.MILLISECONDS_PER_SECOND) * fps);
			final long time = Math.round(fps * pFrame);

			this.updateFrameTitledBorder(pFrame, frames);

			if (!this.mFrameSliderIsChangedProgramatically) {
				if (!this.mFrameSlider.getValueIsAdjusting()) {
					if (time > length) {
						this.mMediaPlayer.setTime(length);
					} else {
						this.mMediaPlayer.setTime(time);
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
								VAmp.this.mRecorder.setFPS(VAmp.FPS_DEFAULT);
								VAmp.this.mRecorder.setFPSFixed(true);
								VAmp.this.mOutputRenderFramePanel.setVideoFPS(VAmp.FPS_DEFAULT);
								VAmp.this.mOutputRenderFramePanel.setVideoFPSFixed(true);
								break;
							case ItemEvent.DESELECTED:
								VAmp.this.stopMedia();
								VAmp.this.mRecorder.setFPSFixed(false);
								VAmp.this.mOutputRenderFramePanel.setVideoFPSFixed(false);
								VAmp.this.playMedia(VAmp.this.mMediaFilename);
								break;
						}
					}
				}
			});

			this.mMediaPanel.add(browseMediaFileButton);
			this.mMediaPanel.add(this.mCameraCheckbox);
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
					VAmp.this.updateAmplificationTitledBorder(amplification);
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

			final JPanel blurModePanel = new JPanel(new GridLayout(2, 1));
			{
				final ButtonGroup blurModeButtonGroup = new ButtonGroup();

				final JRadioButton gaussianBlurModeRadioButton = new JRadioButton("Gaussian", VAmp.BLUR_MODE_DEFAULT == BlurMode.GAUSSIAN);
				blurModeButtonGroup.add(gaussianBlurModeRadioButton);
				gaussianBlurModeRadioButton.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(final ItemEvent pItemEvent) {
						switch (pItemEvent.getStateChange()) {
							case ItemEvent.SELECTED:
								VAmp.this.mOutputRenderFramePanel.setBlurMode(BlurMode.GAUSSIAN);
								break;
						}
					}
				});

				final JRadioButton boxBlurModeRadioButton = new JRadioButton("Box", VAmp.BLUR_MODE_DEFAULT == BlurMode.BOX);
				blurModeButtonGroup.add(boxBlurModeRadioButton);
				boxBlurModeRadioButton.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(final ItemEvent pItemEvent) {
						switch (pItemEvent.getStateChange()) {
							case ItemEvent.SELECTED:
								VAmp.this.mOutputRenderFramePanel.setBlurMode(BlurMode.BOX);
								break;
						}
					}
				});

				blurModePanel.add(gaussianBlurModeRadioButton);
				blurModePanel.add(boxBlurModeRadioButton);
			}

			this.mBlurRadiusPanel.add(blurRadiusSlider);
			this.mBlurRadiusPanel.add(blurModePanel);
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

	private void createRecordPanel() {
		this.mRecordPanel = new JPanel(new FlowLayout());
		{
			final TitledBorder recordPanelBorder = BorderFactory.createTitledBorder("Record:");
			this.mRecordPanel.setBorder(recordPanelBorder);

			final JButton browseMediaFileButton = new JButton("Browse");
			final JButton recordButton = new JButton(VAmp.getImageIcon("/img/record.png"));
			final JButton pauseButton = new JButton(VAmp.getImageIcon("/img/pause.png"));
			final JButton resumeButton = new JButton(VAmp.getImageIcon("/img/resume.png"));
			final JButton stopButton = new JButton(VAmp.getImageIcon("/img/stop.png"));
			final JButton playButton = new JButton(VAmp.getImageIcon("/img/play.png"));

			recordButton.setEnabled(false);
			pauseButton.setEnabled(false);
			resumeButton.setEnabled(false);
			stopButton.setEnabled(false);
			playButton.setEnabled(false);

			browseMediaFileButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					final int result = VAmp.this.mRecordFileChooser.showSaveDialog(VAmp.this);

					if (result == JFileChooser.APPROVE_OPTION) {
						final File selectedFile = VAmp.this.mRecordFileChooser.getSelectedFile();
						if (selectedFile.isDirectory()) {
							VAmp.this.mRecorder.setFile(new File(selectedFile, System.currentTimeMillis() + ".avi"));
						} else {
							VAmp.this.mRecorder.setFile(selectedFile);
						}

						recordButton.setEnabled(true);
						pauseButton.setEnabled(false);
						resumeButton.setEnabled(false);
						stopButton.setEnabled(false);
						playButton.setEnabled(false);
					}
				}
			});

			recordButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					try {
						VAmp.this.mRecorder.record();

						recordButton.setEnabled(false);
						pauseButton.setEnabled(true);
						resumeButton.setEnabled(false);
						stopButton.setEnabled(true);
						playButton.setEnabled(false);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			});

			pauseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					VAmp.this.mRecorder.pause();

					recordButton.setEnabled(false);
					pauseButton.setEnabled(false);
					resumeButton.setEnabled(true);
					stopButton.setEnabled(true);
					playButton.setEnabled(false);
				}
			});

			resumeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					VAmp.this.mRecorder.resume();

					recordButton.setEnabled(false);
					pauseButton.setEnabled(true);
					resumeButton.setEnabled(false);
					stopButton.setEnabled(true);
					playButton.setEnabled(false);
				}
			});

			stopButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					try {
						VAmp.this.mRecorder.stop();

						recordButton.setEnabled(true);
						pauseButton.setEnabled(false);
						resumeButton.setEnabled(false);
						stopButton.setEnabled(false);
						playButton.setEnabled(true);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			});

			playButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent pActionEvent) {
					VAmp.this.mRecorder.play();
				}
			});

			this.mRecordPanel.add(browseMediaFileButton);
			this.mRecordPanel.add(recordButton);
			this.mRecordPanel.add(pauseButton);
			this.mRecordPanel.add(resumeButton);
			this.mRecordPanel.add(stopButton);
			this.mRecordPanel.add(playButton);
		}
	}

	private void updateFrameTitledBorder(final int pFrame, final int pFrames) {
		final TitledBorder framePanelBorder = (TitledBorder) VAmp.this.mFramePanel.getBorder();
		framePanelBorder.setTitle("Frame: " + String.valueOf(pFrame) + "/" + String.valueOf(pFrames));
		this.mFramePanel.repaint();
	}

	private void updateAmplificationTitledBorder(final float pAmplification) {
		final TitledBorder amplificationPanelBorder = (TitledBorder) VAmp.this.mAmplificationPanel.getBorder();
		amplificationPanelBorder.setTitle("Amplification: " + String.valueOf(pAmplification) + "x");
		this.mAmplificationPanel.repaint();
	}

	private void updateBlurRadiusTitledBorder(final int pBlurRadius) {
		final TitledBorder blurRadiusPanelBorder = (TitledBorder) VAmp.this.mBlurRadiusPanel.getBorder();
		blurRadiusPanelBorder.setTitle("Blur-Radius: " + String.valueOf(pBlurRadius) + "px");
		this.mBlurRadiusPanel.repaint();
	}

	private void updateFrequencyTitledBorder(final float pFrequency) {
		final TitledBorder frequencyPanelBorder = (TitledBorder) VAmp.this.mFrequencyPanel.getBorder();
		frequencyPanelBorder.setTitle("Frequency: " + String.valueOf(pFrequency) + "Hz");
		this.mFrequencyPanel.repaint();
	}

	private static ImageIcon getImageIcon(final String pFilename) {
		try {
			return new ImageIcon(ImageIO.read(VAmp.class.getResourceAsStream(pFilename)));
		} catch (final IOException e) {
			return null;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}