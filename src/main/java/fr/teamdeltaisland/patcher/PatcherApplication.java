package fr.teamdeltaisland.patcher;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Swing shell whose complete visual design is the official Image.png artwork. */
public final class PatcherApplication extends JFrame {
    private static final int ARTWORK_WIDTH = 1584;
    private static final int ARTWORK_HEIGHT = 993;
    private static final Color TEXT = new Color(184, 193, 210);

    private final Map<Cheat, JCheckBox> cheatBoxes = new EnumMap<Cheat, JCheckBox>(Cheat.class);
    private final JLabel fileName = overlayLabel("Aucun fichier", SwingConstants.LEFT);
    private final JLabel fileSize = overlayLabel("—", SwingConstants.LEFT);
    private final JLabel status = overlayLabel("Sélectionnez la ROM USA officielle", SwingConstants.LEFT);
    private final OverlayPane overlay;
    private Path selectedRom;
    private Rectangle restoredBounds;
    private Point dragOrigin;

    public PatcherApplication() {
        super("Dinosaurs for Hire ROM Patcher LG30");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setResizable(true);
        overlay = new OverlayPane(loadArtwork());
        setContentPane(overlay);
        installOverlays();
        setSize(ARTWORK_WIDTH, ARTWORK_HEIGHT);
        setLocationRelativeTo(null);
    }

    private void installOverlays() {
        addHotspot(new Rectangle(8, 3, 20, 24), new Runnable() {
            public void run() { dispose(); }
        }, "Fermer");
        addHotspot(new Rectangle(31, 3, 23, 24), new Runnable() {
            public void run() { setState(ICONIFIED); }
        }, "Réduire");
        addHotspot(new Rectangle(56, 3, 24, 24), new Runnable() {
            public void run() { toggleMaximized(); }
        }, "Agrandir ou restaurer");

        addHotspot(new Rectangle(45, 389, 447, 79), new Runnable() {
            public void run() { chooseRom(); }
        }, "Choisir la ROM Dinosaurs for Hire (USA)");
        addHotspot(new Rectangle(1134, 573, 408, 79), new Runnable() {
            public void run() { createRom(); }
        }, "Créer ma ROM");

        addOverlay(fileName, new Rectangle(742, 383, 455, 31));
        addOverlay(fileSize, new Rectangle(742, 420, 455, 31));
        addOverlay(status, new Rectangle(742, 456, 455, 31));

        JCheckBox translation = transparentCheckBox("Traduction française LG30", true);
        addOverlay(translation, new Rectangle(43, 563, 370, 35));

        int[] rows = {568, 628, 686, 746, 804};
        Cheat[] cheats = {
                Cheat.INFINITE_LIVES, Cheat.INVINCIBILITY, Cheat.MAX_WEAPONS,
                Cheat.INFINITE_BOMBS, Cheat.LEVEL_SELECT
        };
        for (int i = 0; i < cheats.length; i++) {
            JCheckBox checkBox = transparentCheckBox(cheats[i].title, false);
            cheatBoxes.put(cheats[i], checkBox);
            addOverlay(checkBox, new Rectangle(458, rows[i], 625, 45));
        }

        MouseAdapter windowDrag = new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (event.getY() <= 31 && getExtendedState() != MAXIMIZED_BOTH) dragOrigin = event.getPoint();
            }

            public void mouseDragged(MouseEvent event) {
                if (dragOrigin != null && getExtendedState() != MAXIMIZED_BOTH) {
                    Point screen = event.getLocationOnScreen();
                    setLocation(screen.x - dragOrigin.x, screen.y - dragOrigin.y);
                }
            }

            public void mouseReleased(MouseEvent event) { dragOrigin = null; }
        };
        overlay.addMouseListener(windowDrag);
        overlay.addMouseMotionListener(windowDrag);
    }

    private void chooseRom() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choisir la ROM USA officielle");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path candidate = chooser.getSelectedFile().toPath();
        fileName.setText(candidate.getFileName().toString());
        try {
            byte[] data = RomValidator.readAndValidate(candidate);
            selectedRom = candidate;
            fileSize.setText(String.format("%,d octets", data.length).replace(',', ' '));
            status.setText("✓ ROM USA valide");
            status.setForeground(new Color(71, 220, 106));
        } catch (Exception exception) {
            selectedRom = null;
            fileSize.setText("—");
            status.setText("✗ " + exception.getMessage());
            status.setForeground(new Color(239, 43, 48));
            JOptionPane.showMessageDialog(this, exception.getMessage(), "ROM incompatible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createRom() {
        if (selectedRom == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez et validez d’abord la ROM USA officielle.", "ROM requise", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(selectedRom.toAbsolutePath().getParent().toFile());
        chooser.setSelectedFile(new File(PatchEngine.OUTPUT_NAME));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        EnumSet<Cheat> selected = EnumSet.noneOf(Cheat.class);
        for (Map.Entry<Cheat, JCheckBox> entry : cheatBoxes.entrySet())
            if (entry.getValue().isSelected()) selected.add(entry.getKey());
        try {
            new PatchEngine().createFile(selectedRom, chooser.getSelectedFile().toPath(), selected);
            JOptionPane.showMessageDialog(this, "ROM créée avec succès :\n" + chooser.getSelectedFile(), "Terminé", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Création impossible", JOptionPane.ERROR_MESSAGE);
        } catch (PatchException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Création impossible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleMaximized() {
        if (getExtendedState() == MAXIMIZED_BOTH) {
            setExtendedState(NORMAL);
            if (restoredBounds != null) setBounds(restoredBounds);
        } else {
            restoredBounds = getBounds();
            setExtendedState(MAXIMIZED_BOTH);
        }
    }

    private void addHotspot(Rectangle bounds, final Runnable action, String accessibleName) {
        JPanel hotspot = new JPanel();
        hotspot.setOpaque(false);
        hotspot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (hotspot.getAccessibleContext() != null) hotspot.getAccessibleContext().setAccessibleName(accessibleName);
        hotspot.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) { action.run(); }
        });
        addOverlay(hotspot, bounds);
    }

    private void addOverlay(java.awt.Component component, Rectangle bounds) {
        ((javax.swing.JComponent) component).putClientProperty("artworkBounds", bounds);
        overlay.add(component);
    }

    private static JCheckBox transparentCheckBox(String accessibleName, boolean selected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(selected);
        checkBox.setOpaque(false);
        checkBox.setContentAreaFilled(false);
        checkBox.setBorderPainted(false);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (checkBox.getAccessibleContext() != null) {
            checkBox.getAccessibleContext().setAccessibleName(accessibleName);
        }
        return checkBox;
    }

    private static JLabel overlayLabel(String value, int alignment) {
        JLabel label = new JLabel(value, alignment);
        label.setForeground(TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        return label;
    }

    private static BufferedImage loadArtwork() {
        try {
            BufferedImage artwork = ImageIO.read(PatcherApplication.class.getResource("/Image.png"));
            if (artwork == null || artwork.getWidth() != ARTWORK_WIDTH || artwork.getHeight() != ARTWORK_HEIGHT)
                throw new IllegalStateException("Image.png doit mesurer exactement 1584 x 993 pixels.");
            return artwork;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de charger Image.png.", exception);
        }
    }

    private static void verifyInstallation() throws IOException, PatchException {
        InputStream patch = PatcherApplication.class.getResourceAsStream("/patch/translation.dfhp.b64");
        if (patch == null) throw new PatchException("Ressource de patch absente.");
        patch.close();
        InputStream imageStream = PatcherApplication.class.getResourceAsStream("/Image.png");
        if (imageStream == null) throw new PatchException("Image.png absent.");
        BufferedImage image;
        try { image = ImageIO.read(imageStream); }
        finally { imageStream.close(); }
        if (image == null || image.getWidth() != ARTWORK_WIDTH || image.getHeight() != ARTWORK_HEIGHT)
            throw new PatchException("Dimensions de Image.png incorrectes.");
        System.out.println("OK - installation autonome vérifiée (Image.png 1584x993 et patch embarqués)");
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--verify-installation".equals(args[0])) {
            try { verifyInstallation(); }
            catch (Exception exception) {
                System.err.println("ERREUR - " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new PatcherApplication().setVisible(true); }
        });
    }

    private static final class OverlayPane extends JPanel {
        private final BufferedImage artwork;

        OverlayPane(BufferedImage artwork) {
            this.artwork = artwork;
            setLayout(null);
            setPreferredSize(new Dimension(ARTWORK_WIDTH, ARTWORK_HEIGHT));
        }

        public void doLayout() {
            double scaleX = getWidth() / (double) ARTWORK_WIDTH;
            double scaleY = getHeight() / (double) ARTWORK_HEIGHT;
            for (java.awt.Component component : getComponents()) {
                Rectangle reference = (Rectangle) ((javax.swing.JComponent) component).getClientProperty("artworkBounds");
                if (reference != null) component.setBounds(
                        (int) Math.round(reference.x * scaleX), (int) Math.round(reference.y * scaleY),
                        (int) Math.round(reference.width * scaleX), (int) Math.round(reference.height * scaleY));
            }
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            copy.drawImage(artwork, 0, 0, getWidth(), getHeight(), null);
            copy.dispose();
        }
    }
}
