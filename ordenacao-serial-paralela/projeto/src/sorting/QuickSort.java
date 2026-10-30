package sorting;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Quick Sort — O(n log n) no caso médio.
 * Pivô: mediana de três (evita o pior caso O(n²) em vetores já ordenados ou invertidos).
 * Partição de Hoare. A recursão é feita na menor partição e o laço continua na
 * maior, limitando a profundidade da pilha a O(log n).
 * Paralelo: Fork/Join — as duas partições são processadas em tarefas paralelas
 * até o limiar THRESHOLD.
 */
public class QuickSort implements SortAlgorithm {

    static final int THRESHOLD = 8_192;

    @Override public String name() { return "QuickSort"; }
    @Override public boolean isQuadratic() { return false; }

    @Override
    public void sortSerial(int[] a) {
        quick(a, 0, a.length - 1);
    }

    @Override
    public void sortParallel(int[] a, int threads) {
        ForkJoinPool pool = new ForkJoinPool(threads);
        try {
            pool.invoke(new Task(a, 0, a.length - 1));
        } finally {
            pool.shutdown();
        }
    }

    static void quick(int[] a, int lo, int hi) {
        while (lo < hi) {
            int p = partition(a, lo, hi);
            if (p - lo < hi - p) {
                quick(a, lo, p);
                lo = p + 1;
            } else {
                quick(a, p + 1, hi);
                hi = p;
            }
        }
    }

    /** Partição de Hoare com pivô mediana-de-três. Retorna índice j tal que a[lo..j] <= a[j+1..hi]. */
    static int partition(int[] a, int lo, int hi) {
        int mid = (lo + hi) >>> 1;
        if (a[mid] < a[lo]) swap(a, mid, lo);
        if (a[hi] < a[lo]) swap(a, hi, lo);
        if (a[hi] < a[mid]) swap(a, hi, mid);
        int pivot = a[mid];
        int i = lo - 1, j = hi + 1;
        while (true) {
            do { i++; } while (a[i] < pivot);
            do { j--; } while (a[j] > pivot);
            if (i >= j) return j;
            swap(a, i, j);
        }
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    static class Task extends RecursiveAction {
        final int[] a; final int lo, hi;
        Task(int[] a, int lo, int hi) { this.a = a; this.lo = lo; this.hi = hi; }

        @Override
        protected void compute() {
            if (hi - lo < THRESHOLD) {
                quick(a, lo, hi);
                return;
            }
            int p = partition(a, lo, hi);
            invokeAll(new Task(a, lo, p), new Task(a, p + 1, hi));
        }
    }
}
