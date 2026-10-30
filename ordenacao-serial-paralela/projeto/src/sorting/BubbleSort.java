package sorting;

/**
 * Bubble Sort — O(n²).
 * Serial: versão clássica com parada antecipada (se nenhuma troca ocorrer na passada).
 * Paralelo: particionamento em blocos ordenados por threads + intercalação ({@link ChunkMerger}).
 */
public class BubbleSort implements SortAlgorithm {

    @Override public String name() { return "BubbleSort"; }
    @Override public boolean isQuadratic() { return true; }

    @Override
    public void sortSerial(int[] a) {
        bubble(a, 0, a.length);
    }

    @Override
    public void sortParallel(int[] a, int threads) {
        ChunkMerger.sortInChunks(a, threads, (lo, hi) -> bubble(a, lo, hi));
    }

    /** Ordena a[lo, hi). */
    static void bubble(int[] a, int lo, int hi) {
        for (int end = hi - 1; end > lo; end--) {
            boolean swapped = false;
            for (int i = lo; i < end; i++) {
                if (a[i] > a[i + 1]) {
                    int tmp = a[i]; a[i] = a[i + 1]; a[i + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) return;
        }
    }
}
