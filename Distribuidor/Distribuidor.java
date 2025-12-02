import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Distribuidor
{
    public static final int PORTA_PADRAO = 12345;

    private static class Trabalhador extends Thread
    {
        private final Parceiro servidor;
        private final byte[] parte;
        private final String host;
        private byte[] resultado;
        private Exception falha;

        Trabalhador(Parceiro servidor, byte[] parte, String host)
        {
            this.servidor = servidor;
            this.parte = parte;
            this.host = host;
        }

        @Override
        public void run()
        {
            try
            {
                System.out.println("[D] Enviando parte de tamanho " + this.parte.length + " para " + this.host);
                this.servidor.receba(new Pedido(this.parte));
                Comunicado resposta = this.servidor.envie();
                if (!(resposta instanceof Resposta))
                    throw new IllegalStateException("Objeto inesperado do receptor " + this.host);
                this.resultado = ((Resposta) resposta).getVetor();
                System.out.println("[D] Resposta recebida de " + this.host + ". Elementos: " + this.resultado.length);
            }
            catch (Exception e)
            {
                this.falha = e;
            }
        }

        public byte[] getResultado() throws Exception
        {
            if (this.falha != null)
                throw new Exception("Falha ao comunicar com " + this.host + ": " + this.falha.getMessage(), this.falha);
            return this.resultado == null ? new byte[0] : this.resultado;
        }
    }

    public static void main(String[] args)
    {
        // Ajuste estes IPs antes de executar para apontar para cada Receptor ativo.
        String[] hosts = {"127.0.0.1"};
        int porta = Distribuidor.PORTA_PADRAO;

        Parceiro[] servidores = conectarServidores(hosts, porta);
        if (servidores == null)
            return;

        try
        {
            executarLoop(servidores, hosts);
        }
        finally
        {
            encerrarServidores(servidores);
        }
    }

    private static Parceiro[] conectarServidores(String[] hosts, int porta)
    {
        Parceiro[] servidores = new Parceiro[hosts.length];

        for (int i = 0; i < hosts.length; i++)
        {
            try
            {
                Socket conexao = new Socket(hosts[i], porta);
                ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
                ObjectInputStream receptor = new ObjectInputStream(conexao.getInputStream());
                servidores[i] = new Parceiro(conexao, receptor, transmissor);
                System.out.println("[D] Conectado a " + hosts[i]);
            }
            catch (Exception erro)
            {
                System.err.println("[D] Falha ao conectar em " + hosts[i] + ": " + erro.getMessage());
                for (int j = 0; j < i; j++)
                {
                    try
                    {
                        servidores[j].adeus();
                    }
                    catch (Exception ignorado)
                    {}
                }
                return null;
            }
        }

        return servidores;
    }

    private static void executarLoop(Parceiro[] servidores, String[] hosts)
    {
        String opcao = "S";
        while (opcao.equalsIgnoreCase("S"))
        {
            try
            {
                System.out.print("Digite o tamanho do vetor a ser ordenado: ");
                int tamanho = Teclado.getUmInt();
                if (tamanho <= 0)
                {
                    System.out.println("Informe um tamanho positivo.");
                    continue;
                }

                byte[] vetor = gerarVetor(tamanho);
                System.out.print("Deseja imprimir o vetor gerado? [S/N]: ");
                if (Teclado.getUmString().equalsIgnoreCase("S"))
                    System.out.println(Arrays.toString(vetor));

                long inicioDistribuido = System.currentTimeMillis();
                byte[][] fatias = fatiarVetor(vetor, servidores.length);
                List<byte[]> partesOrdenadas = despacharPedidos(servidores, hosts, fatias);
                int threadsDisponiveis = Math.max(1, Runtime.getRuntime().availableProcessors());
                byte[] resultadoDistribuido = ParallelMergeSorter.mergeAll(partesOrdenadas, threadsDisponiveis);
                long fimDistribuido = System.currentTimeMillis();

                long inicioSequencial = System.currentTimeMillis();
                byte[] resultadoSequencial = ParallelMergeSorter.sortSequential(vetor);
                long fimSequencial = System.currentTimeMillis();

                System.out.println("\n--- Estatisticas ---");
                System.out.println("Tempo distribuido: " + (fimDistribuido - inicioDistribuido) + " ms");
                System.out.println("Tempo sequencial:  " + (fimSequencial - inicioSequencial) + " ms");
                System.out.println("Resultados identicos? " + Arrays.equals(resultadoDistribuido, resultadoSequencial));

                System.out.print("Informe o nome do arquivo texto para gravar o vetor ordenado: ");
                String nomeArquivo = Teclado.getUmString();
                salvarEmArquivo(resultadoDistribuido, nomeArquivo);

                System.out.print("Deseja ordenar um novo vetor? [S/N]: ");
                opcao = Teclado.getUmString();
            }
            catch (Exception erro)
            {
                System.err.println("[D] Falha no processamento: " + erro.getMessage());
                opcao = "S";
            }
        }
    }

    private static List<byte[]> despacharPedidos(Parceiro[] servidores, String[] hosts, byte[][] fatias) throws Exception
    {
        List<byte[]> partesOrdenadas = new ArrayList<>();
        Trabalhador[] trabalhadores = new Trabalhador[servidores.length];

        for (int i = 0; i < servidores.length; i++)
        {
            trabalhadores[i] = new Trabalhador(servidores[i], fatias[i], hosts[i]);
            trabalhadores[i].start();
        }

        for (int i = 0; i < trabalhadores.length; i++)
        {
            Trabalhador trabalhador = trabalhadores[i];
            try
            {
                trabalhador.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new Exception("Processamento interrompido.", e);
            }
            partesOrdenadas.add(trabalhador.getResultado());
        }

        return partesOrdenadas;
    }

    private static byte[] gerarVetor(int tamanho)
    {
        byte[] vetor = new byte[tamanho];
        for (int i = 0; i < tamanho; i++)
            vetor[i] = (byte) ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        return vetor;
    }

    private static byte[][] fatiarVetor(byte[] vetor, int partes)
    {
        byte[][] fatias = new byte[partes][];
        int base = vetor.length / partes;
        int sobra = vetor.length % partes;
        int inicio = 0;

        for (int i = 0; i < partes; i++)
        {
            int incremento = base + (i < sobra ? 1 : 0);
            int fim = inicio + incremento;
            byte[] fatia = Arrays.copyOfRange(vetor, inicio, fim);
            fatias[i] = fatia;
            inicio = fim;
        }

        return fatias;
    }

    private static void salvarEmArquivo(byte[] vetor, String nomeArquivo)
    {
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty())
            nomeArquivo = "vetor-ordenado.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo)))
        {
            for (int i = 0; i < vetor.length; i++)
            {
                writer.write(Byte.toString(vetor[i]));
                writer.newLine();
            }
            System.out.println("[D] Resultado salvo em " + nomeArquivo);
        }
        catch (Exception erro)
        {
            System.err.println("[D] Falha ao salvar arquivo: " + erro.getMessage());
        }
    }

    private static void encerrarServidores(Parceiro[] servidores)
    {
        for (Parceiro servidor : servidores)
        {
            if (servidor == null)
                continue;
            try
            {
                servidor.receba(new ComunicadoEncerramento());
                servidor.adeus();
            }
            catch (Exception erro)
            {
                System.err.println("[D] Erro ao encerrar conexao: " + erro.getMessage());
            }
        }
    }
}
