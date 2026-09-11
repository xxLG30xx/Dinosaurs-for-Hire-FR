package fr.teamdeltaisland.patcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Swing shell whose complete visual design is the official Image.png artwork. */
public final class PatcherApplication extends JFrame {
    private static final int DESIGN_WIDTH = 1584;
    private static final int DESIGN_HEIGHT = 993;
    private static final Color INFO_BACKGROUND = new Color(4, 17, 21, 245);
    private static final Color INFO_TEXT = new Color(184, 193, 210);
    private static final Color VALID_TEXT = new Color(71, 220, 106);
    private static final Color ERROR_TEXT = new Color(239, 43, 48);

    private final Map<Cheat, OverlayCheckBox> cheatBoxes = new EnumMap<>(Cheat.class);
    private final OverlayCheckBox translationBox = new OverlayCheckBox(true);
    private final JLabel fileName = dynamicLabel();
    private final JLabel fileSize = dynamicLabel();
    private final JLabel status = dynamicLabel();
    private Path selectedRom;
    private Rectangle normalBounds;

    public PatcherApplication() {
        super("Dinosaurs for Hire ROM Patcher LG30");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);
        setContentPane(createInterface());
        pack();
        setLocationRelativeTo(null);
    }

    private JComponent createInterface() {
        BufferedImage artwork = loadArtwork();
        ArtworkPane layers = new ArtworkPane(artwork);
        layers.setPreferredSize(new Dimension(DESIGN_WIDTH, DESIGN_HEIGHT));

        // Only hit areas and changeable values are superimposed; every static pixel is Image.png.
        addButton(layers, new Rectangle(2, 1, 31, 29), this::closeWindow, "Fermer");
        addButton(layers, new Rectangle(33, 1, 29, 29), this::minimizeWindow, "Réduire");
        addButton(layers, new Rectangle(62, 1, 29, 29), this::toggleZoom, "Agrandir ou restaurer");
        addButton(layers, new Rectangle(44, 389, 448, 78), this::chooseRom, "Choisir la ROM Dinosaurs for Hire (USA)");
        addDynamicValue(layers, fileName, new Rectangle(735, 378, 470, 36));
        addDynamicValue(layers, fileSize, new Rectangle(735, 415, 470, 36));
        addDynamicValue(layers, status, new Rectangle(735, 451, 470, 39));

        addCheckBox(layers, translationBox, new Rectangle(43, 563, 375, 39), "Traduction française LG30");
        addCheat(layers, Cheat.INFINITE_LIVES, new Rectangle(459, 568, 635, 51));
        addCheat(layers, Cheat.INVINCIBILITY, new Rectangle(459, 627, 635, 51));
        addCheat(layers, Cheat.MAX_WEAPONS, new Rectangle(459, 686, 635, 51));
        addCheat(layers, Cheat.INFINITE_BOMBS, new Rectangle(459, 746, 635, 51));
        addCheat(layers, Cheat.LEVEL_SELECT, new Rectangle(459, 802, 635, 90));
        addButton(layers, new Rectangle(1133, 573, 410, 80), this::createRom, "CRÉER MA ROM");
        return layers;
    }

    private static BufferedImage loadArtwork() {
        try {
            var resource = PatcherApplication.class.getResource("/Image.png");
            if (resource == null) throw new IllegalStateException("La ressource Image.png est absente du JAR.");
            BufferedImage image = ImageIO.read(resource);
            if (image == null || image.getWidth() != DESIGN_WIDTH || image.getHeight() != DESIGN_HEIGHT)
                throw new IllegalStateException("La ressource Image.png n'a pas les dimensions officielles 1584 × 993.");
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger l'interface Image.png depuis le JAR.", e);
        }
    }

    private void addCheat(JLayeredPane layers, Cheat cheat, Rectangle bounds) {
        OverlayCheckBox checkBox = new OverlayCheckBox(false);
        cheatBoxes.put(cheat, checkBox);
        addCheckBox(layers, checkBox, bounds, cheat.title);
    }

    private static void addCheckBox(JLayeredPane layers, OverlayCheckBox checkBox, Rectangle bounds, String accessibleName) {
        setLogicalBounds(layers, checkBox, bounds);
        setAccessibleNameIfAvailable(checkBox, accessibleName);
        checkBox.setToolTipText(accessibleName);
        layers.add(checkBox, JLayeredPane.PALETTE_LAYER);
    }

    private static void addButton(JLayeredPane layers, Rectangle bounds, Runnable action, String accessibleName) {
        JButton button = new JButton();
        setLogicalBounds(layers, button, bounds);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAccessibleNameIfAvailable(button, accessibleName);
        button.addActionListener(event -> action.run());
        layers.add(button, JLayeredPane.PALETTE_LAYER);
    }

    private static JLabel dynamicLabel() {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(INFO_BACKGROUND);
        label.setForeground(INFO_TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 19));
        label.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0));
        label.setVisible(false); // The pristine artwork already contains the initial values.
        return label;
    }

    private static void addDynamicValue(JLayeredPane layers, JLabel label, Rectangle bounds) {
        setLogicalBounds(layers, label, bounds);
        layers.add(label, JLayeredPane.MODAL_LAYER);
    }

    private static void setLogicalBounds(JLayeredPane layers, JComponent component, Rectangle bounds) {
        component.putClientProperty("designBounds", new Rectangle(bounds));
        component.setBounds(bounds);
    }

    private static void setAccessibleNameIfAvailable(JComponent component, String name) {
        AccessibleContext context = component.getAccessibleContext();
        if (context != null) context.setAccessibleName(name);
    }

    static void verifyOverlayComponents() {
        OverlayCheckBox checkBox = new OverlayCheckBox(false);
        setAccessibleNameIfAvailable(checkBox, "Test");
        JButton button = new JButton();
        setAccessibleNameIfAvailable(button, "Test");
    }

    private void closeWindow() {
        dispose();
        System.exit(0);
    }

    private void minimizeWindow() {
        setState(Frame.ICONIFIED);
    }

    private void toggleZoom() {
        if (normalBounds != null) {
            setBounds(normalBounds);
            normalBounds = null;
            return;
        }
        normalBounds = getBounds();
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        double scale = Math.max(1.0, Math.min((double) usable.width / DESIGN_WIDTH,
                (double) usable.height / DESIGN_HEIGHT));
        int width = (int) Math.floor(DESIGN_WIDTH * scale);
        int height = (int) Math.floor(DESIGN_HEIGHT * scale);
        setBounds(usable.x + (usable.width - width) / 2, usable.y + (usable.height - height) / 2, width, height);
    }

    private void chooseRom() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choisir la ROM USA officielle");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path candidate = chooser.getSelectedFile().toPath();
        showDynamicValues();
        fileName.setText(candidate.getFileName().toString());
        try {
            byte[] data = RomValidator.readAndValidate(candidate);
            selectedRom = candidate;
            fileSize.setText(String.format("%,d octets", data.length).replace(',', ' '));
            status.setText("✓ ROM USA valide");
            status.setForeground(VALID_TEXT);
        } catch (Exception ex) {
            selectedRom = null;
            fileSize.setText("—");
            status.setText("✗ ROM incompatible");
            status.setForeground(ERROR_TEXT);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ROM incompatible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDynamicValues() {
        fileName.setVisible(true);
        fileSize.setVisible(true);
        status.setVisible(true);
    }

    private void createRom() {
        if (selectedRom == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez et validez d’abord la ROM USA officielle.", "ROM requise", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!translationBox.isSelected()) {
            JOptionPane.showMessageDialog(this, "Activez la traduction française LG30 pour créer la ROM.", "Traduction requise", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(selectedRom.toAbsolutePath().getParent().toFile());
        chooser.setSelectedFile(new java.io.File(PatchEngine.OUTPUT_NAME));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        EnumSet<Cheat> selected = EnumSet.noneOf(Cheat.class);
        cheatBoxes.forEach((cheat, box) -> { if (box.isSelected()) selected.add(cheat); });
        try {
            new PatchEngine().createFile(selectedRom, chooser.getSelectedFile().toPath(), selected);
            JOptionPane.showMessageDialog(this, "ROM créée avec succès :\n" + chooser.getSelectedFile(), "Terminé", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | PatchException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Création impossible", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--verify-installation".equals(args[0])) {
            BufferedImage artwork = loadArtwork();
            System.out.println("Installation valide : interface " + artwork.getWidth() + " × " + artwork.getHeight() + ".");
            return;
        }
        SwingUtilities.invokeLater(() -> new PatcherApplication().setVisible(true));
    }

    private static final class ArtworkPane extends JLayeredPane {
        private final BufferedImage artwork;
        private Point dragStartOnScreen;
        private Point windowStart;
        private ArtworkPane(BufferedImage artwork) {
            this.artwork = artwork;
            MouseAdapter dragHandler = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) {
                    if (event.getY() > scaled(31, getHeight(), DESIGN_HEIGHT)) return;
                    dragStartOnScreen = event.getLocationOnScreen();
                    windowStart = SwingUtilities.getWindowAncestor(ArtworkPane.this).getLocation();
                }
                @Override public void mouseDragged(MouseEvent event) {
                    if (dragStartOnScreen == null || windowStart == null) return;
                    Point current = event.getLocationOnScreen();
                    Window window = SwingUtilities.getWindowAncestor(ArtworkPane.this);
                    window.setLocation(windowStart.x + current.x - dragStartOnScreen.x,
                            windowStart.y + current.y - dragStartOnScreen.y);
                }
                @Override public void mouseReleased(MouseEvent event) { dragStartOnScreen = null; windowStart = null; }
            };
            addMouseListener(dragHandler);
            addMouseMotionListener(dragHandler);
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            graphics.drawImage(artwork, 0, 0, getWidth(), getHeight(), null);
        }
        @Override public void doLayout() {
            for (Component child : getComponents()) {
                if (!(child instanceof JComponent component)) continue;
                Object value = component.getClientProperty("designBounds");
                if (!(value instanceof Rectangle bounds)) continue;
                component.setBounds(scaled(bounds.x, getWidth(), DESIGN_WIDTH),
                        scaled(bounds.y, getHeight(), DESIGN_HEIGHT),
                        scaled(bounds.width, getWidth(), DESIGN_WIDTH),
                        scaled(bounds.height, getHeight(), DESIGN_HEIGHT));
            }
        }
        private static int scaled(int value, int actual, int design) {
            return (int) Math.round((double) value * actual / design);
        }
    }

    /** Transparent over the checked box already drawn in Image.png; paints only its unchecked state. */
    private static final class OverlayCheckBox extends JComponent {
        private boolean selected;
        private OverlayCheckBox(boolean selected) {
            this.selected = selected;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) { setSelected(!isSelected()); }
            });
        }
        private boolean isSelected() { return selected; }
        private void setSelected(boolean value) { selected = value; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            if (selected) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(139, 28, 31));
            g.fillRoundRect(1, 2, 27, 27, 4, 4);
            g.setColor(new Color(220, 220, 220));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(1, 2, 27, 27, 4, 4);
            g.dispose();
        }
    }
}
