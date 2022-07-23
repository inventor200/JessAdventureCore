package joeyproductions.jessadventurecore.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * A handy layout manager for a layered pane.
 * Sourced from: https://stackoverflow.com/questions/16025874/jlayeredpane-not-respecting-layers
 * @author Jasper Siepkes
 */
class LayeredPaneLayout implements LayoutManager {

    private final Container target;
    private static final Dimension preferredSize = new Dimension(500, 500);

    public LayeredPaneLayout(final Container target) {
            this.target = target;
    }

    @Override
    public void addLayoutComponent(final String name, final Component comp) {
    }

    @Override
    public void layoutContainer(final Container container) {
            for (final Component component : container.getComponents()) {
                    component.setBounds(new Rectangle(0, 0, target.getWidth(), target.getHeight()));
            }
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
            return preferredLayoutSize(parent);
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
            return preferredSize;
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
    }
}
