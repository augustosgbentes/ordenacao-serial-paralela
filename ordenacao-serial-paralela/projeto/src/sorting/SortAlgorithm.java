package sorting;

public interface SortAlgorithm {


    String name();


    void sortSerial(int[] a);


    void sortParallel(int[] a, int threads);


    boolean isQuadratic();
}
