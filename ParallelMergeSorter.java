import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ParallelMergeSorter
{
    private static final int LIMIAR_MERGE_PARALELO = 200_000;

    private ParallelMergeSorter() {}

    public static byte[] sort(byte[] dados, int maxThreads)
    {
        if (dados == null || dados.length == 0)
            return new byte[0];

        int threads = Math.max(1, maxThreads);
        int partes = Math.min(threads, dados.length);

        List<byte[]> pedacos = particionar(dados, partes);
        List<Thread> ativos = new ArrayList<>(pedacos.size());
        List<SortWorker> workers = new ArrayList<>(pedacos.size());

        for (byte[] pedaco : pedacos)
        {
            SortWorker worker = new SortWorker(pedaco);
            Thread t = new Thread(worker);
            workers.add(worker);
            ativos.add(t);
            t.start();
        }

        for (Thread t : ativos)
        {
            try
            {
                t.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        return mergeAll(pedacos, threads);
    }

    public static byte[] sortSequential(byte[] dados)
    {
        if (dados == null || dados.length == 0)
            return new byte[0];

        byte[] copia = Arrays.copyOf(dados, dados.length);
        byte[] buffer = new byte[copia.length];
        mergeSort(copia, buffer, 0, copia.length - 1);
        return copia;
    }

    public static byte[] mergeAll(List<byte[]> partes, int maxThreads)
    {
        if (partes == null || partes.isEmpty())
            return new byte[0];

        List<byte[]> atual = new ArrayList<>();
        for (byte[] parte : partes)
        {
            if (parte == null)
                continue;
            if (parte.length == 0)
            {
                atual.add(new byte[0]);
            }
            else
            {
                atual.add(parte);
            }
        }

        if (atual.isEmpty())
            return new byte[0];

        int threads = Math.max(1, maxThreads);

        while (atual.size() > 1)
        {
            List<byte[]> proximo = new ArrayList<>();
            List<MergeJob> jobs = new ArrayList<>();

            for (int i = 0; i < atual.size(); i += 2)
            {
                if (i + 1 < atual.size())
                {
                    jobs.add(new MergeJob(atual.get(i), atual.get(i + 1)));
                }
                else
                {
                    proximo.add(atual.get(i));
                }
            }

            executarEmLotes(jobs, threads, proximo);
            atual = proximo;
        }

        return atual.get(0);
    }

    private static List<byte[]> particionar(byte[] dados, int partes)
    {
        List<byte[]> resultado = new ArrayList<>(partes);
        int tamanho = dados.length;
        int base = tamanho / partes;
        int sobra = tamanho % partes;
        int inicio = 0;

        for (int i = 0; i < partes; i++)
        {
            int incremento = base + (i < sobra ? 1 : 0);
            int fim = inicio + incremento;
            byte[] pedaco = Arrays.copyOfRange(dados, inicio, fim);
            resultado.add(pedaco);
            inicio = fim;
        }

        return resultado;
    }

    private static void executarEmLotes(List<MergeJob> jobs, int maxThreads, List<byte[]> destino)
    {
        int indice = 0;
        while (indice < jobs.size())
        {
            int lote = Math.min(maxThreads, jobs.size() - indice);
            int threadsPorJob = Math.max(1, maxThreads / lote);
            List<Thread> ativos = new ArrayList<>(lote);

            for (int i = 0; i < lote; i++)
            {
                MergeJob job = jobs.get(indice + i);
                job.definirLimiteThreads(threadsPorJob);
                Thread t = new Thread(job);
                ativos.add(t);
                t.start();
            }

            for (int i = 0; i < lote; i++)
            {
                MergeJob job = jobs.get(indice + i);
                try
                {
                    ativos.get(i).join();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                destino.add(job.getResultado());
            }

            indice += lote;
        }
    }

    private static void mergeSort(byte[] vetor, byte[] buffer, int inicio, int fim)
    {
        if (inicio >= fim)
            return;

        int meio = (inicio + fim) / 2;
        mergeSort(vetor, buffer, inicio, meio);
        mergeSort(vetor, buffer, meio + 1, fim);
        intercalar(vetor, buffer, inicio, meio, fim);
    }

    private static void intercalar(byte[] vetor, byte[] buffer, int inicio, int meio, int fim)
    {
        int i = inicio;
        int j = meio + 1;
        int k = inicio;

        while (i <= meio && j <= fim)
        {
            if (vetor[i] <= vetor[j])
                buffer[k++] = vetor[i++];
            else
                buffer[k++] = vetor[j++];
        }

        while (i <= meio)
            buffer[k++] = vetor[i++];

        while (j <= fim)
            buffer[k++] = vetor[j++];

        for (int idx = inicio; idx <= fim; idx++)
            vetor[idx] = buffer[idx];
    }

    private static final class SortWorker implements Runnable
    {
        private final byte[] valores;

        private SortWorker(byte[] valores)
        {
            this.valores = valores;
        }

        @Override
        public void run()
        {
            if (this.valores.length <= 1)
                return;

            byte[] buffer = new byte[this.valores.length];
            mergeSort(this.valores, buffer, 0, this.valores.length - 1);
        }
    }

    private static final class MergeJob implements Runnable
    {
        private final byte[] esquerdo;
        private final byte[] direito;
        private int maxThreads = 1;
        private byte[] resultado;

        private MergeJob(byte[] esquerdo, byte[] direito)
        {
            this.esquerdo = esquerdo == null ? new byte[0] : esquerdo;
            this.direito  = direito  == null ? new byte[0] : direito;
        }

        public void definirLimiteThreads(int maxThreads)
        {
            this.maxThreads = Math.max(1, maxThreads);
        }

        @Override
        public void run()
        {
            this.resultado = mergeComThreads(this.esquerdo, this.direito, this.maxThreads);
        }

        public byte[] getResultado()
        {
            return this.resultado == null ? new byte[0] : this.resultado;
        }
    }

    public static byte[] merge(byte[] esquerdo, byte[] direito)
    {
        byte[] a = esquerdo == null ? new byte[0] : esquerdo;
        byte[] b = direito  == null ? new byte[0] : direito;
        byte[] resultado = new byte[a.length + b.length];

        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length)
        {
            if (a[i] <= b[j])
                resultado[k++] = a[i++];
            else
                resultado[k++] = b[j++];
        }

        while (i < a.length)
            resultado[k++] = a[i++];

        while (j < b.length)
            resultado[k++] = b[j++];

        return resultado;
    }

    private static byte[] mergeComThreads(byte[] esquerdo, byte[] direito, int maxThreads)
    {
        byte[] a = esquerdo == null ? new byte[0] : esquerdo;
        byte[] b = direito  == null ? new byte[0] : direito;

        int total = a.length + b.length;
        if (total == 0)
            return new byte[0];

        int threads = Math.max(1, maxThreads);
        if (threads == 1 || total < LIMIAR_MERGE_PARALELO)
            return merge(a, b);

        int tarefas = Math.min(threads, Math.max(1, total / LIMIAR_MERGE_PARALELO));
        if (tarefas <= 1)
            return merge(a, b);

        byte[] destino = new byte[total];
        List<Thread> workers = new ArrayList<>(tarefas);

        int base = total / tarefas;
        int sobra = total % tarefas;
        int inicioSaida = 0;

        for (int i = 0; i < tarefas; i++)
        {
            int incremento = base + (i < sobra ? 1 : 0);
            int fimSaida = inicioSaida + incremento;

            int inicioA = coRank(inicioSaida, a, b);
            int inicioB = inicioSaida - inicioA;
            int fimA = coRank(fimSaida, a, b);
            int fimB = fimSaida - fimA;

            SegmentoMerge segmento = new SegmentoMerge(a, b, destino, inicioA, fimA, inicioB, fimB, inicioSaida);
            Thread t = new Thread(segmento);
            workers.add(t);
            t.start();

            inicioSaida = fimSaida;
        }

        for (Thread t : workers)
        {
            try
            {
                t.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        return destino;
    }

    private static int coRank(int k, byte[] esquerdo, byte[] direito)
    {
        int m = esquerdo.length;
        int n = direito.length;

        int i = Math.min(k, m);
        int j = k - i;
        int iLow = Math.max(0, k - n);
        int iHigh = Math.min(k, m);

        while (true)
        {
            if (i > 0 && j < n && esquerdo[i - 1] > direito[j])
            {
                iHigh = i - 1;
                int delta = (i - iLow + 1) / 2;
                i -= delta;
                j = k - i;
            }
            else if (j > 0 && i < m && direito[j - 1] >= esquerdo[i])
            {
                iLow = i + 1;
                int delta = (iHigh - i + 1) / 2;
                i += delta;
                j = k - i;
            }
            else
            {
                return i;
            }
        }
    }

    private static final class SegmentoMerge implements Runnable
    {
        private final byte[] esquerdo;
        private final byte[] direito;
        private final byte[] destino;
        private final int inicioEsquerdo;
        private final int fimEsquerdo;
        private final int inicioDireito;
        private final int fimDireito;
        private final int inicioDestino;

        private SegmentoMerge(byte[] esquerdo, byte[] direito, byte[] destino, int inicioEsquerdo, int fimEsquerdo, int inicioDireito, int fimDireito, int inicioDestino)
        {
            this.esquerdo = esquerdo;
            this.direito = direito;
            this.destino = destino;
            this.inicioEsquerdo = inicioEsquerdo;
            this.fimEsquerdo = fimEsquerdo;
            this.inicioDireito = inicioDireito;
            this.fimDireito = fimDireito;
            this.inicioDestino = inicioDestino;
        }

        @Override
        public void run()
        {
            int i = this.inicioEsquerdo;
            int j = this.inicioDireito;
            int k = this.inicioDestino;

            while (i < this.fimEsquerdo && j < this.fimDireito)
            {
                if (this.esquerdo[i] <= this.direito[j])
                    this.destino[k++] = this.esquerdo[i++];
                else
                    this.destino[k++] = this.direito[j++];
            }

            while (i < this.fimEsquerdo)
                this.destino[k++] = this.esquerdo[i++];

            while (j < this.fimDireito)
                this.destino[k++] = this.direito[j++];
        }
    }
}
