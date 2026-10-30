package benchmark;

import sorting.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * "Framework de teste": executa cada algoritmo nas versões serial e paralela,
 * variando tamanho e natureza dos dados e número de threads, com N amostras
 * por configuração, e grava todos os tempos em CSV.
 *
 * Uso:
 *   java -cp out benchmark.Benchmark [arquivo.csv] [amostras] [rotuloNucleos]
 *
 * Para variar o número de NÚCLEOS disponíveis, execute o programa restringindo a
 * afinidade de CPU (Linux: taskset -c 0 java ...; Windows: start /affinity 1 java ...).
 * O número de núcleos visto pela JVM é gravado na coluna "nucleos".
 */
public class Benchmark {

    // ---- Configuração do experimento ----
    static final int[] TAMANHOS_QUADRATICOS = {1_000, 5_000, 10_000, 20_000};
    static final int[] TAMANHOS_NLOGN       = {10_000, 100_000, 1_000_000, 5_000_000};
    static final int[] THREADS              = {2, 4, 8};
    static final long SEED                  = 42L;

    public static void main(String[] args) throws IOException {
        String arquivo = args.length > 0 ? args[0] : "resultados.csv";
        int amostras   = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int nucleos    = Runtime.getRuntime().availableProcessors();

        List<SortAlgorithm> algoritmos = List.of(
                new BubbleSort(), new SelectionSort(), new MergeSort(), new QuickSort());

        boolean novo = !new java.io.File(arquivo).exists();
        try (PrintWriter out = new PrintWriter(new FileWriter(arquivo, true))) {
            if (novo) out.println("algoritmo,versao,threads,nucleos,tamanho,tipo_dados,amostra,tempo_ms");

            aquecer(algoritmos);

            for (SortAlgorithm alg : algoritmos) {
                int[] tamanhos = alg.isQuadratic() ? TAMANHOS_QUADRATICOS : TAMANHOS_NLOGN;
                for (int n : tamanhos) {
                    for (DataGenerator.Tipo tipo : DataGenerator.Tipo.values()) {
                        int[] original = DataGenerator.gerar(n, tipo, SEED);

                        // Execução serial (threads = 1)
                        for (int s = 1; s <= amostras; s++) {
                            double ms = medir(alg, original, 1);
                            registrar(out, alg, "serial", 1, nucleos, n, tipo, s, ms);
                        }
                        // Execuções paralelas variando threads
                        for (int t : THREADS) {
                            for (int s = 1; s <= amostras; s++) {
                                double ms = medir(alg, original, t);
                                registrar(out, alg, "paralelo", t, nucleos, n, tipo, s, ms);
                            }
                        }
                        out.flush();
                    }
                }
            }
        }
        System.out.println("Concluído. Resultados em " + arquivo);
    }

    /** Mede uma execução sobre uma CÓPIA do vetor original. threads == 1 => versão serial. */
    static double medir(SortAlgorithm alg, int[] original, int threads) {
        int[] a = original.clone();
        long t0 = System.nanoTime();
        if (threads == 1) alg.sortSerial(a);
        else alg.sortParallel(a, threads);
        long t1 = System.nanoTime();
        if (!DataGenerator.estaOrdenado(a))
            throw new IllegalStateException(alg.name() + " não ordenou corretamente!");
        return (t1 - t0) / 1_000_000.0;
    }

    /** Aquecimento da JVM (compilação JIT) antes das medições, para não distorcer as primeiras amostras. */
    static void aquecer(List<SortAlgorithm> algoritmos) {
        for (int r = 0; r < 3; r++)
            for (SortAlgorithm alg : algoritmos) {
                int n = alg.isQuadratic() ? 3_000 : 200_000;
                int[] d = DataGenerator.gerar(n, DataGenerator.Tipo.ALEATORIO, r);
                medir(alg, d, 1);
                medir(alg, d, 2);
            }
    }

    static void registrar(PrintWriter out, SortAlgorithm alg, String versao, int threads, int nucleos,
                          int n, DataGenerator.Tipo tipo, int amostra, double ms) {
        String linha = String.format(Locale.US, "%s,%s,%d,%d,%d,%s,%d,%.3f",
                alg.name(), versao, threads, nucleos, n, tipo, amostra, ms);
        out.println(linha);
        System.out.println(linha);
    }
}
