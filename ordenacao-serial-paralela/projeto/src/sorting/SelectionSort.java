package sorting;


public class SelectionSort implements SortAlgorithm {

    @Override public String name() { return "SelectionSort"; }
    @Override public boolean isQuadratic() { return true; }

    @Override
    public void sortSerial(int[] a) {
        selection(a, 0, a.length);
    }

    @Override
    public void sortParallel(int[] a, int threads) {
        ChunkMerger.sortInChunks(a, threads, (lo, hi) -> selection(a, lo, hi));
    }


    static void selection(int[] a, int lo, int hi) {
        for (int i = lo; i < hi - 1; i++) {
            int min = i;
            for (int j = i + 1; j < hi; j++) {
                if (a[j] < a[min]) min = j;
            }
            if (min != i) {
                int tmp = a[i]; a[i] = a[min]; a[min] = tmp;
            }
        }
    }
}
