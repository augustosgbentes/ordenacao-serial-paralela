import sorting.*; import java.util.*;
public class Teste { public static void main(String[] x){
 SortAlgorithm[] al={new BubbleSort(),new SelectionSort(),new MergeSort(),new QuickSort()};
 Random r=new Random(1);
 for(SortAlgorithm a:al) for(int n:new int[]{0,1,2,3,7,100,1000,20000}) for(int t:new int[]{1,2,3,4,8}){
  int[] v=new int[n]; for(int i=0;i<n;i++) v[i]=r.nextInt(50); int[] e=v.clone(); Arrays.sort(e);
  if(t==1)a.sortSerial(v); else a.sortParallel(v,t);
  if(!Arrays.equals(v,e)) {System.out.println("FALHA "+a.name()+" n="+n+" t="+t);return;}
 } System.out.println("todos ok");}}
