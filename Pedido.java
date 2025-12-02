import java.util.Arrays;

public class Pedido extends Comunicado
{
    private final byte[] numeros;

    public Pedido(byte[] numeros)
    {
        if (numeros == null)
            throw new IllegalArgumentException("Vetor ausente");

        this.numeros = Arrays.copyOf(numeros, numeros.length);
    }

    public byte[] getNumeros()
    {
        return Arrays.copyOf(this.numeros, this.numeros.length);
    }

    public int tamanho()
    {
        return this.numeros.length;
    }

    public byte[] ordenar()
    {
        int maxThreads = Runtime.getRuntime().availableProcessors();
        if (maxThreads <= 0)
            maxThreads = 1;

        return ParallelMergeSorter.sort(this.numeros, maxThreads);
    }
}
