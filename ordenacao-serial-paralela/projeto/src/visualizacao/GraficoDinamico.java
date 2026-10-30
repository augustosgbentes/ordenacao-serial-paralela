package visualizacao;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GraficoDinamico extends JFrame {

    record Linha(String alg, int threads, int nucleos, int n, String tipo, double ms) {}
    record Ponto(int n, double media, double dp) {}

    static final Color[] CORES = {
            new Color(0x2a6fdb), new Color(0xe07b39), new Color(0x2e9e6b), new Color(0xb5408f)};

    final List<Linha> dados;
    final JComboBox<String> cbAlg, cbTipo, cbNucleos, cbMetrica;
    final JCheckBox chkLog;
    final Painel painel = new Painel();

    public GraficoDinamico(List<Linha> dados) {
        super("Ordenação Serial x Paralela — Gráficos Dinâmicos");
        this.dados = dados;
        cbAlg = new JComboBox<>(distintos(Linha::alg));
        cbTipo = new JComboBox<>(distintos(Linha::tipo));
        cbNucleos = new JComboBox<>(distintos(l -> String.valueOf(l.nucleos())));
        cbMetrica = new JComboBox<>(new String[]{"Tempo médio (ms)", "Speedup"});
        chkLog = new JCheckBox("Escala log", true);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Algoritmo:")); topo.add(cbAlg);
        topo.add(new JLabel("Dados:")); topo.add(cbTipo);
        topo.add(new JLabel("Núcleos:")); topo.add(cbNucleos);
        topo.add(new JLabel("Métrica:")); topo.add(cbMetrica);
        topo.add(chkLog);
        cbAlg.addActionListener(e -> painel.repaint());
        cbTipo.addActionListener(e -> painel.repaint());
        cbNucleos.addActionListener(e -> painel.repaint());
        cbMetrica.addActionListener(e -> painel.repaint());
        chkLog.addActionListener(e -> painel.repaint());

        add(topo, BorderLayout.NORTH);
        add(painel, BorderLayout.CENTER);
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    String[] distintos(java.util.function.Function<Linha, String> f) {
        return dados.stream().map(f).distinct().sorted().toArray(String[]::new);
    }


    static Map<Integer, List<Ponto>> series(List<Linha> dados, String alg, String tipo, int nucleos) {
        Map<Integer, Map<Integer, List<Double>>> g = new TreeMap<>();
        for (Linha l : dados)
            if (l.alg().equals(alg) && l.tipo().equals(tipo) && l.nucleos() == nucleos)
                g.computeIfAbsent(l.threads(), k -> new TreeMap<>())
                 .computeIfAbsent(l.n(), k -> new ArrayList<>()).add(l.ms());
        Map<Integer, List<Ponto>> res = new TreeMap<>();
        g.forEach((t, porN) -> {
            List<Ponto> pts = new ArrayList<>();
            porN.forEach((n, v) -> {
                double m = v.stream().mapToDouble(d -> d).average().orElse(0);
                double var = v.stream().mapToDouble(d -> (d - m) * (d - m)).sum() / Math.max(1, v.size() - 1);
                pts.add(new Ponto(n, m, Math.sqrt(var)));
            });
            res.put(t, pts);
        });
        return res;
    }

    class Painel extends JPanel {
        Painel() { setBackground(Color.WHITE); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            desenhar(dados, (Graphics2D) g0, getWidth(), getHeight(),
                    (String) cbAlg.getSelectedItem(), (String) cbTipo.getSelectedItem(),
                    Integer.parseInt((String) cbNucleos.getSelectedItem()),
                    cbMetrica.getSelectedIndex() == 1, chkLog.isSelected());
        }
    }


    static void desenhar(List<Linha> dados, Graphics2D g, int w, int h, String alg, String tipo, int nucleos,
                  boolean speedup, boolean log) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);

        Map<Integer, List<Ponto>> s = series(dados, alg, tipo, nucleos);
        if (s.isEmpty()) { g.setColor(Color.GRAY); g.drawString("Sem dados", w / 2, h / 2); return; }

        // Converte para speedup, se pedido: S = Tserial / Tparalelo
        if (speedup && s.containsKey(1)) {
            Map<Integer, Double> serial = s.get(1).stream().collect(Collectors.toMap(Ponto::n, Ponto::media));
            Map<Integer, List<Ponto>> sp = new TreeMap<>();
            s.forEach((t, pts) -> sp.put(t, pts.stream()
                    .map(p -> new Ponto(p.n(), serial.get(p.n()) / p.media(), 0)).toList()));
            s = sp;
            log = false;
        }

        int ml = 100, mr = 150, mt = 50, mb = 60;
        int pw = w - ml - mr, ph = h - mt - mb;
        List<Integer> ns = s.values().iterator().next().stream().map(Ponto::n).toList();

        double ymin = Double.MAX_VALUE, ymax = 0;
        for (List<Ponto> pts : s.values()) for (Ponto p : pts) {
            ymin = Math.min(ymin, Math.max(1e-3, p.media() - p.dp()));
            ymax = Math.max(ymax, p.media() + p.dp());
        }
        if (!log) ymin = 0;
        ymax *= 1.1;
        final double y0 = log ? Math.log10(ymin) : ymin, y1 = log ? Math.log10(ymax) : ymax;
        final boolean lg = log;
        java.util.function.DoubleUnaryOperator Y = v -> mt + ph - (( (lg ? Math.log10(Math.max(v, 1e-3)) : v) - y0) / (y1 - y0)) * ph;
        java.util.function.IntUnaryOperator X = i -> ml + (int) (ns.size() == 1 ? pw / 2.0 : (double) i / (ns.size() - 1) * pw);

        // Título
        g.setColor(new Color(0x222222));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(alg + " — dados " + tipo.toLowerCase() + " — " + nucleos + " núcleo(s)", ml, 30);

        // Grade e eixo Y
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int k = 0; k <= 5; k++) {
            double frac = k / 5.0;
            double val = lg ? Math.pow(10, y0 + frac * (y1 - y0)) : y0 + frac * (y1 - y0);
            int yy = (int) (mt + ph - frac * ph);
            g.setColor(new Color(0xeeeeee)); g.drawLine(ml, yy, ml + pw, yy);
            g.setColor(new Color(0x555555));
            String tick = val >= 100 ? String.format("%.0f", val) : String.format("%.2f", val);
            g.drawString(tick, ml - 8 - g.getFontMetrics().stringWidth(tick), yy + 4);
        }
        // Eixo X
        for (int i = 0; i < ns.size(); i++) {
            g.setColor(new Color(0x555555));
            String lbl = String.format("%,d", ns.get(i)).replace(',', '.');
            g.drawString(lbl, X.applyAsInt(i) - g.getFontMetrics().stringWidth(lbl) / 2, mt + ph + 20);
        }
        g.drawString("Tamanho do vetor (n)", ml + pw / 2 - 60, h - 15);
        g.rotate(-Math.PI / 2);
        g.drawString(speedup ? "Speedup (x)" : "Tempo médio (ms)" + (lg ? " — log" : ""), -(mt + ph / 2 + 50), 18);
        g.rotate(Math.PI / 2);
        g.setColor(new Color(0x999999)); g.drawRect(ml, mt, pw, ph);

        // Séries
        int idx = 0, ly = mt + 10;
        for (var e : s.entrySet()) {
            Color c = CORES[idx++ % CORES.length];
            g.setColor(c);
            g.setStroke(new BasicStroke(2.2f));
            Path2D path = new Path2D.Double();
            List<Ponto> pts = e.getValue();
            for (int i = 0; i < pts.size(); i++) {
                double x = X.applyAsInt(i), y = Y.applyAsDouble(pts.get(i).media());
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                g.fillOval((int) x - 4, (int) y - 4, 8, 8);
                if (pts.get(i).dp() > 0) { // barra de desvio-padrão
                    g.setStroke(new BasicStroke(1f));
                    double ya = Y.applyAsDouble(pts.get(i).media() + pts.get(i).dp());
                    double yb = Y.applyAsDouble(Math.max(1e-3, pts.get(i).media() - pts.get(i).dp()));
                    g.draw(new Line2D.Double(x, ya, x, yb));
                    g.setStroke(new BasicStroke(2.2f));
                }
            }
            g.draw(path);
            String nome = e.getKey() == 1 ? "Serial" : e.getKey() + " threads";
            g.fillRect(ml + pw + 15, ly, 14, 14);
            g.setColor(new Color(0x333333));
            g.drawString(nome, ml + pw + 35, ly + 12);
            ly += 22;
        }
        if (speedup) { // linha de referência S = 1
            g.setColor(Color.GRAY);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0));
            int yy = (int) Y.applyAsDouble(1);
            g.drawLine(ml, yy, ml + pw, yy);
        }
    }

    static List<Linha> lerCsv(String arquivo) throws IOException {
        return Files.readAllLines(Path.of(arquivo)).stream().skip(1).filter(l -> !l.isBlank()).map(l -> {
            String[] c = l.split(",");
            return new Linha(c[0], Integer.parseInt(c[2]), Integer.parseInt(c[3]),
                    Integer.parseInt(c[4]), c[5], Double.parseDouble(c[7]));
        }).toList();
    }

    public static void main(String[] args) throws Exception {
        String arquivo = args.length > 0 ? args[0] : "resultados/resultados.csv";
        List<Linha> dados = lerCsv(arquivo);

        if (args.length > 2 && args[1].equals("--png")) { // exportação sem interface
            System.setProperty("java.awt.headless", "true");
            File dir = new File(args[2]); dir.mkdirs();
            for (String alg : dados.stream().map(Linha::alg).distinct().toList())
                for (String tipo : dados.stream().map(Linha::tipo).distinct().toList())
                    for (int nuc : dados.stream().map(Linha::nucleos).distinct().toList()) {
                        BufferedImage img = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = img.createGraphics();
                        desenhar(dados, g, 1000, 600, alg, tipo, nuc, false, true);
                        g.dispose();
                        ImageIO.write(img, "png", new File(dir, alg + "_" + tipo + "_" + nuc + "nuc.png"));
                    }
            System.out.println("PNGs exportados para " + dir);
            return;
        }
        SwingUtilities.invokeLater(() -> new GraficoDinamico(dados).setVisible(true));
    }

}
