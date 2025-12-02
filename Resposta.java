import java.util.Arrays;

public class Resposta extends Comunicado
{
    private final byte[] vetorOrdenado;

    public Resposta(byte[] vetorOrdenado)
    {
        if (vetorOrdenado == null)
            throw new IllegalArgumentException("Vetor ordenado ausente");

        this.vetorOrdenado = Arrays.copyOf(vetorOrdenado, vetorOrdenado.length);
    }

    public byte[] getVetor()
    {
        return Arrays.copyOf(this.vetorOrdenado, this.vetorOrdenado.length);
    }
}
