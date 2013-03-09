package org.vamp.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Subclass of {@link FlowLayout} that updates its {@link FlowLayout#preferredLayoutSize(Container)}.
 */
public class WrapLayout extends FlowLayout {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 5008159757042661400L;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public WrapLayout() {
		super();
	}

	public WrapLayout(final int pAlign) {
		super(pAlign);
	}

	public WrapLayout(final int pAlign, final int pHGap, final int pVGap) {
		super(pAlign, pHGap, pVGap);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public Dimension preferredLayoutSize(final Container pTarget) {
		return this.getLayoutSize(pTarget, true);
	}

	@Override
	public Dimension minimumLayoutSize(final Container pTarget) {
		final Dimension minimum = this.getLayoutSize(pTarget, false);
		minimum.width -= (this.getHgap() + 1);
		return minimum;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private Dimension getLayoutSize(final Container pTarget, final boolean pPreferred) {
		synchronized (pTarget.getTreeLock()) {
			final Dimension targetSize = pTarget.getSize();
			final int targetWidth;
			if (targetSize.width == 0) {
				targetWidth = Integer.MAX_VALUE;
			} else {
				targetWidth = targetSize.width;
			}

			final int hgap = this.getHgap();
			final int vgap = this.getVgap();

			final Insets insets = pTarget.getInsets();
			final int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
			final int maxWidth = targetWidth - horizontalInsetsAndGap;

			final Dimension result = new Dimension(0, 0);
			int rowWidth = 0;
			int rowHeight = 0;

			final int targetComponentCount = pTarget.getComponentCount();
			for (int i = 0; i < targetComponentCount; i++) {
				final Component component = pTarget.getComponent(i);

				if (component.isVisible()) {
					final Dimension componentDimension = pPreferred ? component.getPreferredSize() : component.getMinimumSize();

					/* If we can't add the component to current row, start a new one: */
					if ((rowWidth + componentDimension.width) > maxWidth) {
						this.addRow(result, rowWidth, rowHeight);
						rowWidth = 0;
						rowHeight = 0;
					}

					/* Add a horizontal gap for all components after the first: */
					if (rowWidth != 0) {
						rowWidth += hgap;
					}

					rowWidth += componentDimension.width;
					rowHeight = Math.max(rowHeight, componentDimension.height);
				}
			}

			this.addRow(result, rowWidth, rowHeight);

			result.width += horizontalInsetsAndGap;
			result.height += insets.top + insets.bottom + (vgap * 2);

			/* When using a scroll pane or the DecoratedLookAndFeel we need to make sure the preferred
			 * size is less than the size of the target container so shrinking the container size
			 * works correctly. Removing the horizontal gap is an easy way to do this. */
			final Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, pTarget);
			if (scrollPane != null) {
				result.width -= (hgap + 1);
			}

			return result;
		}
	}

	private void addRow(final Dimension pDimension, final int pRowWidth, final int pRowHeight) {
		pDimension.width = Math.max(pDimension.width, pRowWidth);

		if (pDimension.height > 0) {
			pDimension.height += this.getVgap();
		}

		pDimension.height += pRowHeight;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}