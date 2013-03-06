package org.vamp;

import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import uk.co.caprica.vlcj.discovery.NativeDiscovery;

public class Main {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public static void main(final String[] pArgs) {
		new NativeDiscovery().discover();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final String mediaFilename = pArgs[0];
				final String[] vlcArgs = (pArgs.length == 1) ? new String[] {} : Arrays.copyOfRange(pArgs, 1, pArgs.length);
				final VAmp vamp = new VAmp(mediaFilename, vlcArgs);

				vamp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				vamp.setLocation(100, 100);
				vamp.setVisible(true);
				vamp.pack();
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