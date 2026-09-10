package fr.teamdeltaisland.patcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class PatcherApplication extends JFrame {
    private static final Color BG = new Color(3, 12, 13);
    private static final Color PANEL = new Color(7, 20, 23);
    private static final Color RED = new Color(239, 43, 48);
    private static final Color GREEN = new Color(25, 163, 61);
    private final JLabel fileName = value("Aucun fichier");
    private final JLabel fileSize = value("—");
    private final JLabel status = value("Sélectionnez la ROM USA officielle");
    private final Map<Cheat, JCheckBox> cheatBoxes = new EnumMap<>(Cheat.class);
    private Path selectedRom;

    public PatcherApplication() {
        super("Dinosaurs for Hire ROM Patcher LG30");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 720));
        setSize(1400, 880);
        setLocationRelativeTo(null);
        setContentPane(buildUi());
    }

    private JComponent buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BG); root.setBorder(new EmptyBorder(10, 10, 8, 10));
        root.add(banner(), BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(8, 8)); content.setOpaque(false);
        content.add(selectionPanel(), BorderLayout.NORTH);
        JPanel columns = new JPanel(new GridBagLayout()); columns.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.gridy=0; c.fill=GridBagConstraints.BOTH; c.weighty=1;
        c.gridx=0; c.weightx=.29; columns.add(translationPanel(), c);
        c.gridx=1; c.weightx=.43; columns.add(cheatsPanel(), c);
        c.gridx=2; c.weightx=.28; columns.add(createPanel(), c);
        content.add(columns, BorderLayout.CENTER); root.add(content, BorderLayout.CENTER);
        JLabel footer = new JLabel("⚙  v1.0.0                         Pour usage personnel uniquement. Respectez les droits d’auteur de SEGA.                         TEAM DELTA ISLAND", SwingConstants.CENTER);
        footer.setForeground(Color.WHITE); footer.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JComponent banner() {
        try {
            BufferedImage full = ImageIO.read(PatcherApplication.class.getResource("/banner.png"));
            BufferedImage crop = full.getSubimage(10, 32, full.getWidth()-20, Math.min(288, full.getHeight()-32));
            return new ScaledImage(crop);
        } catch (Exception e) {
            JLabel fallback = new JLabel("DINOSAURS FOR HIRE — ROM PATCHER", SwingConstants.CENTER);
            fallback.setForeground(RED); fallback.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32)); fallback.setPreferredSize(new Dimension(800, 190)); return fallback;
        }
    }

    private JComponent selectionPanel() {
        JPanel p = section("▣  1. SÉLECTION DE LA ROM", new BorderLayout(20, 4));
        JButton choose = button("Choisir la ROM Dinosaurs for Hire (USA)", new Color(190, 30, 35));
        choose.addActionListener(e -> chooseRom()); p.add(choose, BorderLayout.WEST);
        JPanel details = new JPanel(new GridLayout(3, 2, 14, 4)); details.setOpaque(false);
        details.add(label("Fichier sélectionné :")); details.add(fileName); details.add(label("Taille :")); details.add(fileSize); details.add(label("Statut :")); details.add(status);
        p.add(details, BorderLayout.CENTER); return p;
    }

    private JComponent translationPanel() {
        JPanel p = section("●  2. TRADUCTION", new BorderLayout());
        JTextArea text = text("☑  Traduction française LG30\n\nProfitez de Dinosaurs for Hire entièrement en français !\n\n• Menus entièrement traduits\n• Dialogues des personnages traduits et adaptés\n• Textes d’objets, armes et bonus traduits\n• Messages et indications en jeu traduits\n• Noms des niveaux traduits\n• Crédits en français\n• Désormais compatible toutes régions (U / J / E)\n• Intro taguée par l’équipe");
        p.add(text); return p;
    }

    private JComponent cheatsPanel() {
        JPanel p = section("★  3. CHEATS / OPTIONS DE JEU", new GridLayout(0,1,4,4));
        for (Cheat cheat : Cheat.values()) {
            JCheckBox box = new JCheckBox("<html><b>" + cheat.title + "</b><br><span style='color:#b9c0cc'>" + cheat.description + "</span></html>");
            box.setOpaque(false); box.setForeground(Color.WHITE); box.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16)); cheatBoxes.put(cheat, box); p.add(box);
        }
        return p;
    }

    private JComponent createPanel() {
        JPanel p = section("⚙  4. CRÉATION", new BorderLayout(5, 12));
        JButton create = button("▶  CRÉER MA ROM", GREEN); create.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        create.addActionListener(e -> createRom()); p.add(create, BorderLayout.NORTH);
        p.add(text("Le programme créera une nouvelle ROM avec uniquement les options sélectionnées.\n\nLa ROM originale ne sera jamais modifiée.\n\nⓘ INFORMATIONS\n\nDinosaurs for Hire © 1993\nSega Mega Drive / Genesis\nHack réalisé par LG30 / Team Delta Island"), BorderLayout.CENTER); return p;
    }

    private void chooseRom() {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Choisir la ROM USA officielle");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path candidate = chooser.getSelectedFile().toPath(); fileName.setText(candidate.getFileName().toString());
        try {
            byte[] data = RomValidator.readAndValidate(candidate); selectedRom=candidate; fileSize.setText(String.format("%,d octets", data.length).replace(',', ' ')); status.setText("✓ ROM USA valide"); status.setForeground(new Color(71, 220, 106));
        } catch (Exception ex) { selectedRom=null; fileSize.setText("—"); status.setText("✗ " + ex.getMessage()); status.setForeground(RED); JOptionPane.showMessageDialog(this, ex.getMessage(), "ROM incompatible", JOptionPane.ERROR_MESSAGE); }
    }

    private void createRom() {
        if (selectedRom == null) { JOptionPane.showMessageDialog(this, "Sélectionnez et validez d’abord la ROM USA officielle.", "ROM requise", JOptionPane.WARNING_MESSAGE); return; }
        JFileChooser chooser = new JFileChooser(selectedRom.toAbsolutePath().getParent().toFile()); chooser.setSelectedFile(new java.io.File(PatchEngine.OUTPUT_NAME));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        EnumSet<Cheat> selected = EnumSet.noneOf(Cheat.class); cheatBoxes.forEach((cheat, box) -> { if(box.isSelected()) selected.add(cheat); });
        try { new PatchEngine().createFile(selectedRom, chooser.getSelectedFile().toPath(), selected); JOptionPane.showMessageDialog(this, "ROM créée avec succès :\n" + chooser.getSelectedFile(), "Terminé", JOptionPane.INFORMATION_MESSAGE); }
        catch (IOException | PatchException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Création impossible", JOptionPane.ERROR_MESSAGE); }
    }

    private static JPanel section(String title, LayoutManager layout) { JPanel p=new JPanel(layout); p.setBackground(PANEL); p.setBorder(new CompoundBorder(new TitledBorder(new LineBorder(RED,2,true), title, TitledBorder.LEADING,TitledBorder.TOP,new Font(Font.SANS_SERIF,Font.BOLD,18),RED),new EmptyBorder(10,12,10,12))); return p; }
    private static JLabel label(String s) { JLabel l=new JLabel(s); l.setForeground(Color.WHITE); l.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16)); return l; }
    private static JLabel value(String s) { JLabel l=label(s); l.setForeground(new Color(184,193,210)); return l; }
    private static JButton button(String s, Color color) { JButton b=new JButton(s); b.setForeground(Color.WHITE); b.setBackground(color); b.setFocusPainted(false); b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16)); b.setBorder(new CompoundBorder(new LineBorder(color.brighter(),2,true),new EmptyBorder(16,20,16,20))); return b; }
    private static JTextArea text(String s) { JTextArea a=new JTextArea(s); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true); a.setOpaque(false); a.setForeground(Color.WHITE); a.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,15)); return a; }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ignored) { } new PatcherApplication().setVisible(true); }); }

    private static final class ScaledImage extends JComponent {
        private final BufferedImage image; ScaledImage(BufferedImage image){this.image=image;setPreferredSize(new Dimension(1000,190));}
        protected void paintComponent(Graphics g){super.paintComponent(g);g.drawImage(image,0,0,getWidth(),getHeight(),null);}
    }
}
