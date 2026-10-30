package benchmark;

import java.util.Random;

/** Gera os conjuntos de dados de entrada, variando tamanho e natureza. */
public final class DataGenerator {

    public enum Tipo { ALEATORIO, ORDENADO, INVERSO }

    private DataGenerator() {}

    /**
     * @param n    tamanho do vetor
     * @param tipo natureza dos dados
     * @param seed semente (fixa) para que todas as execuções usem exatamente os mesmos dados
     */
    public static int[] gerar(int n, Tipo tipo, long seed) {
        int[] a = new int[n];
        switch (tipo) {
            case ALEATORIO -> {
                Random r = new Random(seed);
                for (int i = 0; i < n; i++) a[i] = r.nextInt(n * 10); // intervalo [0, 10n)
            }
            case ORDENADO -> { for (int i = 0; i < n; i++) a[i] = i; }
            case INVERSO  -> { for (int i = 0; i < n; i++) a[i] = n - i; }
        }
        return a;
    }

    public static boolean estaOrdenado(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }
}
