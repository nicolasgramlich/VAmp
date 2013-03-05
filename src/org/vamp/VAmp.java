package org.vamp;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;

public class VAmp {
	private final EmbeddedMediaPlayerComponent mEmbeddedMediaPlayerComponent;

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new VAmp(args);
			}
		});
	}

	private VAmp(final String[] pArgs) {
		final JFrame frame = new JFrame("VAmp");

		this.mEmbeddedMediaPlayerComponent = new EmbeddedMediaPlayerComponent();

		frame.setContentPane(this.mEmbeddedMediaPlayerComponent);

		frame.setLocation(10, 10);
		frame.setSize(960, 540);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		this.mEmbeddedMediaPlayerComponent.getMediaPlayer().playMedia(pArgs[0]);
	}
}