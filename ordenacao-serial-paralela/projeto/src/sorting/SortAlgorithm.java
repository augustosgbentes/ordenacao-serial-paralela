package sorting;

/**
 * Contrato comum a todos os algoritmos de ordenação do estudo.
 * Cada algoritmo possui uma versão serial e uma versão paralela
 * (que recebe o número de threads a utilizar).
 */
public interface SortAlgorithm {

    /** Nome do algoritmo (usado no CSV e nos gráficos). */
    String name();

    /** Ordena o vetor de forma sequencial (uma única thread). */
    void sortSerial(int[] a);

    /** Ordena o vetor utilizando {@code threads} threads. */
    void sortParallel(int[] a, int threads);

    /** true para algoritmos O(n²) — usados com vetores menores no benchmark. */
    boolean isQuadratic();
}
