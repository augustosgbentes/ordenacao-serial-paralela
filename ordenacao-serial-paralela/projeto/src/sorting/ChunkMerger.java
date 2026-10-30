package sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;


public final class ChunkMerger {

    private ChunkMerger() {}

    public static void sortInChunks(int[] a, int threads, BiConsumer<Integer, Integer> sortRange) {
        int n = a.length;
        if (threads <= 1 || n < 2) {
            sortRange.accept(0, n);
            return;
        }
        int t = Math.min(threads, n);
        int[] bounds = new int[t + 1];
        for (int i = 0; i <= t; i++) bounds[i] = (int) ((long) n * i / t);

        ExecutorService pool = Executors.newFixedThreadPool(t);
        try {
            // Fase 1: ordenação local de cada bloco
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < t; i++) {
                final int lo = bounds[i], hi = bounds[i + 1];
                futures.add(pool.submit(() -> sortRange.accept(lo, hi)));
            }
            waitAll(futures);

            // Fase 2: intercalação em árvore (log2 T rodadas)
            int[] buffer = new int[n];
            List<int[]> runs = new ArrayList<>();
            for (int i = 0; i < t; i++) runs.add(new int[]{bounds[i], bounds[i + 1]});

            while (runs.size() > 1) {
                List<int[]> next = new ArrayList<>();
                futures.clear();
                for (int i = 0; i + 1 < runs.size(); i += 2) {
                    final int lo = runs.get(i)[0], mid = runs.get(i)[1], hi = runs.get(i + 1)[1];
                    futures.add(pool.submit(() -> merge(a, buffer, lo, mid, hi)));
                    next.add(new int[]{lo, hi});
                }
                if (runs.size() % 2 == 1) next.add(runs.get(runs.size() - 1));
                waitAll(futures);
                runs = next;
            }
        } finally {
            pool.shutdown();
        }
    }


    static void merge(int[] a, int[] buf, int lo, int mid, int hi) {
        System.arraycopy(a, lo, buf, lo, hi - lo);
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) a[k++] = (buf[i] <= buf[j]) ? buf[i++] : buf[j++];
        while (i < mid) a[k++] = buf[i++];
        while (j < hi) a[k++] = buf[j++];
    }

    private static void waitAll(List<Future<?>> futures) {
        try {
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
