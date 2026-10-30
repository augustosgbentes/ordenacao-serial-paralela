package sorting;


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
