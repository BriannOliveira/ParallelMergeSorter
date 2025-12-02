import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class OrdenacaoSequencial
{
    public static void main(String[] args)
    {
        try
        {
            System.out.print("Informe o tamanho do vetor para ordenacao sequencial: ");
            int tamanho = Teclado.getUmInt();
            if (tamanho <= 0)
            {
                System.out.println("Nada a ordenar.");
                return;
            }

            byte[] vetor = gerarVetor(tamanho);
            System.out.print("Deseja visualizar o vetor gerado? [S/N]: ");
            if (Teclado.getUmString().equalsIgnoreCase("S"))
                System.out.println(Arrays.toString(vetor));

            long inicio = System.currentTimeMillis();
            byte[] ordenado = ParallelMergeSorter.sortSequential(vetor);
            long fim = System.currentTimeMillis();
            System.out.println("Tempo gasto na ordenacao sequencial: " + (fim - inicio) + " ms");

            System.out.print("Nome do arquivo texto para salvar o resultado: ");
            salvarEmArquivo(ordenado, Teclado.getUmString());
        }
        catch (Exception erro)
        {
            System.err.println("Falha durante a ordenacao sequencial: " + erro.getMessage());
        }
    }

    private static byte[] gerarVetor(int tamanho)
    {
        byte[] vetor = new byte[tamanho];
        for (int i = 0; i < tamanho; i++)
            vetor[i] = (byte) ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        return vetor;
    }

    private static void salvarEmArquivo(byte[] vetor, String nomeArquivo)
    {
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty())
            nomeArquivo = "sequencial-ordenado.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo)))
        {
            for (byte valor : vetor)
            {
                writer.write(Byte.toString(valor));
                writer.newLine();
            }
            System.out.println("Resultado salvo em " + nomeArquivo);
        }
        catch (Exception erro)
        {
            System.err.println("Falha ao gravar arquivo: " + erro.getMessage());
        }
    }
}
