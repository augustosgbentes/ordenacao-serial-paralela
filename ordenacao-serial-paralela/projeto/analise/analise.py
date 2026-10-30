"""Análise estatística do CSV gerado pelo Benchmark e geração dos gráficos do relatório.

Uso: python analise/analise.py resultados/resultados.csv graficos

O CSV pode conter execuções com diferentes números de núcleos (coluna "nucleos").
As figuras 1, 3 e 4 usam a configuração com MAIS núcleos; a figura 2 compara todas.
"""
import sys, os
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

csv = sys.argv[1] if len(sys.argv) > 1 else "resultados/resultados.csv"
out = sys.argv[2] if len(sys.argv) > 2 else "graficos"
os.makedirs(out, exist_ok=True)

df = pd.read_csv(csv)
g = (df.groupby(["algoritmo", "nucleos", "tipo_dados", "tamanho", "threads"])["tempo_ms"]
       .agg(media="mean", dp="std", minimo="min", maximo="max", amostras="count").reset_index())
g["cv_%"] = 100 * g["dp"] / g["media"]
serial = g[g.threads == 1][["algoritmo", "nucleos", "tipo_dados", "tamanho", "media"]].rename(columns={"media": "t_serial"})
g = g.merge(serial, on=["algoritmo", "nucleos", "tipo_dados", "tamanho"])
g["speedup"] = g["t_serial"] / g["media"]
g["eficiencia"] = g["speedup"] / g[["threads", "nucleos"]].min(axis=1)
g.to_csv(os.path.join(out, "estatisticas.csv"), index=False, float_format="%.4f")

NUC = int(g.nucleos.max())               # ambiente principal
TODOS_NUC = sorted(g.nucleos.unique())
T_MAX = int(g.threads.max())
CORES = {1: "#2a6fdb", 2: "#e07b39", 4: "#2e9e6b", 8: "#b5408f"}
CORES_ALG = ["#2a6fdb", "#e07b39", "#2e9e6b", "#b5408f"]
ALGS = ["BubbleSort", "SelectionSort", "MergeSort", "QuickSort"]
plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})

def rot(t): return "Serial" if t == 1 else f"{t} threads"
def nuc_txt(n): return f"{n} núcleo" + ("" if n == 1 else "s")

# Figura 1: tempo x tamanho, dados aleatórios, ambiente principal
fig, axs = plt.subplots(2, 2, figsize=(10, 7.5))
for ax, alg in zip(axs.flat, ALGS):
    d = g[(g.algoritmo == alg) & (g.nucleos == NUC) & (g.tipo_dados == "ALEATORIO")]
    for t, s in d.groupby("threads"):
        ax.errorbar(s.tamanho, s.media, yerr=s.dp, marker="o", ms=4, capsize=3, label=rot(t), color=CORES.get(t))
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_title(alg)
    ax.set_xlabel("Tamanho do vetor (n)"); ax.set_ylabel("Tempo médio (ms)")
    ax.grid(alpha=.3, which="both")
axs[0, 0].legend()
fig.suptitle(f"Tempo de execução × tamanho — dados aleatórios, {nuc_txt(NUC)} (média ± desvio-padrão, 5 amostras)")
fig.tight_layout(); fig.savefig(os.path.join(out, "fig1_tempo_tamanho.png"), dpi=160); plt.close(fig)

# Figura 2: speedup x threads, maior n, um painel por número de núcleos
fig, axs = plt.subplots(1, len(TODOS_NUC), figsize=(4.2 * len(TODOS_NUC), 4), sharey=True, squeeze=False)
for ax, nuc in zip(axs[0], TODOS_NUC):
    for alg, cor in zip(ALGS, CORES_ALG):
        d = g[(g.algoritmo == alg) & (g.nucleos == nuc) & (g.tipo_dados == "ALEATORIO")]
        d = d[d.tamanho == d.tamanho.max()]
        ax.plot(d.threads, d.speedup, marker="o", color=cor,
                label=f"{alg} (n={d.tamanho.max():,})".replace(",", "."))
    ax.plot([1, T_MAX], [1, T_MAX], color="gray", ls=":", lw=1, label="Speedup ideal (linear)")
    ax.axhline(1, color="gray", ls="--", lw=1)
    ax.set_xticks([1, 2, 4, 8]); ax.set_xlabel("Threads"); ax.set_title(f"{nuc_txt(nuc)} disponíve" + ("l" if nuc == 1 else "is"))
    ax.grid(alpha=.3)
axs[0][0].set_ylabel("Speedup (T serial / T paralelo)")
axs[0][-1].legend(fontsize=7.5)
fig.suptitle("Speedup por número de threads — dados aleatórios, maior tamanho de cada algoritmo")
fig.tight_layout(); fig.savefig(os.path.join(out, "fig2_speedup_threads.png"), dpi=160); plt.close(fig)

# Figura 3: efeito da natureza dos dados (maior n, ambiente principal)
fig, axs = plt.subplots(1, 4, figsize=(12, 3.6))
tipos = ["ALEATORIO", "ORDENADO", "INVERSO"]
for ax, alg in zip(axs, ALGS):
    d = g[(g.algoritmo == alg) & (g.nucleos == NUC)]
    d = d[d.tamanho == d.tamanho.max()]
    ths = sorted(d.threads.unique()); w = 0.8 / len(ths)
    for i, t in enumerate(ths):
        vals = [d[(d.tipo_dados == tp) & (d.threads == t)].media.iloc[0] for tp in tipos]
        ax.bar([x + (i - (len(ths) - 1) / 2) * w for x in range(3)], vals, w, label=rot(t), color=CORES.get(t))
    ax.set_xticks(range(3)); ax.set_xticklabels(["Aleat.", "Orden.", "Inverso"])
    ax.set_title(alg); ax.set_yscale("log")
axs[0].set_ylabel("Tempo médio (ms, log)"); axs[0].legend(fontsize=7)
fig.suptitle(f"Efeito da natureza dos dados — maior tamanho, {nuc_txt(NUC)}")
fig.tight_layout(); fig.savefig(os.path.join(out, "fig3_natureza_dados.png"), dpi=160); plt.close(fig)

# Figura 4: speedup x tamanho (ambiente principal, máximo de threads, aleatório)
fig, ax = plt.subplots(figsize=(7, 4))
for alg, cor in zip(ALGS, CORES_ALG):
    d = g[(g.algoritmo == alg) & (g.nucleos == NUC) & (g.tipo_dados == "ALEATORIO") & (g.threads == T_MAX)]
    ax.plot(d.tamanho, d.speedup, marker="o", label=alg, color=cor)
ax.axhline(1, color="gray", ls="--", lw=1); ax.set_xscale("log"); ax.set_yscale("log")
ax.set_xlabel("Tamanho do vetor (n)"); ax.set_ylabel("Speedup (log)"); ax.grid(alpha=.3, which="both"); ax.legend()
ax.set_title(f"Speedup com {T_MAX} threads ({nuc_txt(NUC)}) × tamanho da entrada (dados aleatórios)")
fig.tight_layout(); fig.savefig(os.path.join(out, "fig4_speedup_tamanho.png"), dpi=160); plt.close(fig)

pd.set_option("display.width", 200)
print(g[g.tipo_dados == "ALEATORIO"][["algoritmo", "nucleos", "tamanho", "threads", "media", "dp", "cv_%", "speedup", "eficiencia"]]
      .round(3).to_string(index=False))
