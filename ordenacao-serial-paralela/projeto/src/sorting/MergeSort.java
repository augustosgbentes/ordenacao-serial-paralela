package sorting;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MergeSort implements SortAlgorithm {

    static final int THRESHOLD = 8_192;

    @Override public String name() { return "MergeSort"; }
    @Override public boolean isQuadratic() { return false; }

    @Override
    public void sortSerial(int[] a) {
        int[] buf = new int[a.length];
        mergeSort(a, buf, 0, a.length);
    }

    @Override
    public void sortParallel(int[] a, int threads) {
        int[] buf = new int[a.length];
        ForkJoinPool pool = new ForkJoinPool(threads);
        try {
            pool.invoke(new Task(a, buf, 0, a.length));
        } finally {
            pool.shutdown();
        }
    }

    static void mergeSort(int[] a, int[] buf, int lo, int hi) {
        if (hi - lo < 2) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid);
        mergeSort(a, buf, mid, hi);
        if (a[mid - 1] <= a[mid]) return; // já está em ordem
        ChunkMerger.merge(a, buf, lo, mid, hi);
    }

    static class Task extends RecursiveAction {
        final int[] a, buf; final int lo, hi;
        Task(int[] a, int[] buf, int lo, int hi) { this.a = a; this.buf = buf; this.lo = lo; this.hi = hi; }

        @Override
        protected void compute() {
            if (hi - lo <= THRESHOLD) {
                mergeSort(a, buf, lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            invokeAll(new Task(a, buf, lo, mid), new Task(a, buf, mid, hi));
            if (a[mid - 1] <= a[mid]) return;
            ChunkMerger.merge(a, buf, lo, mid, hi);
        }
    }
}
