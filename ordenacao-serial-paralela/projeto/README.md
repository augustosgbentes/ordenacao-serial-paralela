# Ordenação Serial x Paralela em Java

Trabalho de Computação Paralela e Concorrente — UNIFOR.

Análise de desempenho de **Bubble Sort, Selection Sort, Merge Sort e Quick Sort** nas versões serial e paralela,
variando tamanho e natureza dos dados, número de threads (1, 2, 4, 8) e número de núcleos (1, 2 e 8),
com 5 amostras por configuração — 2.880 medições no total.

## Estrutura

```
src/sorting/        SortAlgorithm, BubbleSort, SelectionSort, MergeSort, QuickSort, ChunkMerger
src/benchmark/      DataGenerator, Benchmark (framework de teste -> CSV)
src/visualizacao/   GraficoDinamico (gráficos dinâmicos em Swing — ponto extra)
test/               Teste de corretude de todos os algoritmos
analise/analise.py  Estatísticas e gráficos (pandas + matplotlib)
resultados/         resultados.csv (todas as medições)
                    resultados_pc_8nucleos.csv   (PC, 8 núcleos lógicos)
                    resultados_1e2nucleos.csv    (VM Linux, 1 e 2 núcleos via taskset)
graficos/           estatisticas.csv, figuras do relatório e PNGs do GraficoDinamico
Relatorio_Ordenacao_Serial_Paralela.docx
```

## Estratégias de paralelização

| Algoritmo | Paralelo |
|---|---|
| Merge Sort, Quick Sort | Fork/Join (`RecursiveAction`), limiar de 8.192 elementos |
| Bubble Sort, Selection Sort | Vetor dividido em T blocos, cada thread ordena um bloco, depois intercalação em árvore |

## Principais resultados (8 núcleos, maior n, dados aleatórios)

| Algoritmo | Serial (ms) | 8 threads (ms) | Speedup |
|---|---|---|---|
| Merge Sort (5.000.000) | 1154,6 | 262,8 | 4,39× |
| Quick Sort (5.000.000) | 586,8 | 160,0 | 3,67× |
| Selection Sort (20.000) | 135,1 | 7,8 | 17,2× |
| Bubble Sort (20.000) | 582,4 | 17,4 | 33,4× |

O speedup dos algoritmos O(n²) é superlinear porque dividir o vetor em T blocos reduz o trabalho de n² para n²/T
(o efeito aparece até com 1 núcleo). Detalhes no relatório.

## Como executar

### IntelliJ IDEA
1. **File → Open** → pasta do projeto. Configure um JDK 17+ em **File → Project Structure → Project**.
2. Rode o `main` de `benchmark.Benchmark` com os argumentos `resultados/meu_pc.csv 5`.
3. Rode o `main` de `visualizacao.GraficoDinamico` (sem argumentos usa `resultados/resultados.csv`).

### Linha de comando (Windows — CMD)
```
javac -d out src\sorting\*.java src\benchmark\*.java src\visualizacao\*.java
javac -cp out -d out test\Teste.java && java -cp out Teste

java -cp out benchmark.Benchmark resultados/meu_pc.csv 5

:: variando núcleos (máscara de afinidade: 1 = 1 núcleo, 3 = 2, F = 4, FF = 8)
start /wait /affinity 1 java -cp out benchmark.Benchmark resultados/meu_pc.csv 5

java -cp out visualizacao.GraficoDinamico resultados/resultados.csv
```

### Linha de comando (Linux/Mac)
```bash
javac -d out $(find src -name "*.java")
java -cp out benchmark.Benchmark resultados/meu_pc.csv 5
taskset -c 0 java -cp out benchmark.Benchmark resultados/meu_pc.csv 5   # 1 núcleo
java -cp out visualizacao.GraficoDinamico resultados/resultados.csv
```

### Estatísticas e figuras
```
pip install pandas matplotlib
python analise/analise.py resultados/resultados.csv graficos
```

O Benchmark **acrescenta** linhas ao CSV indicado; use um nome de arquivo novo para uma nova bateria de testes.
