import java.util.ArrayList;

public class Receptor
{
    public static String PORTA_PADRAO = "12345";

    public static void main (String[] args)
    {
        if (args.length>1)
        {
            System.err.println ("Uso esperado: java Receptor [PORTA]\n");
            return;
        }

        String porta=Receptor.PORTA_PADRAO;

        if (args.length==1)
            porta = args[0];

        ArrayList<Parceiro> usuarios =
        new ArrayList<Parceiro> ();

        AceitadoraDeConexao aceitadoraDeConexao=null;
        try
        {
            aceitadoraDeConexao =
            new AceitadoraDeConexao (porta, usuarios);
            aceitadoraDeConexao.start();
        }
        catch (Exception erro)
        {
            System.err.println ("Escolha uma porta apropriada e liberada para uso!\n");
            return;
        }

        System.out.println("[R] Servidor ativo na porta " + porta + ". Aguarde pedidos.");
    }
}
