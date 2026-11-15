using System;
using System.Diagnostics;
using System.IO;

// Lançador do Cozinha em Dia. Apenas chama scripts\preparar-e-executar.ps1,
// que contém toda a lógica (verificação do JDK e do PostgreSQL, e a aplicação).
internal static class Lancador
{
    private static int Main(string[] args)
    {
        string pasta = AppDomain.CurrentDomain.BaseDirectory;
        string script = Path.Combine(pasta, "scripts", "preparar-e-executar.ps1");

        if (!File.Exists(script))
        {
            Console.WriteLine("Arquivo nao encontrado: " + script);
            Console.WriteLine("Mantenha o CozinhaEmDia.exe na pasta raiz do projeto, junto da pasta scripts.");
            Console.WriteLine("Pressione ENTER para fechar.");
            Console.ReadLine();
            return 1;
        }

        var info = new ProcessStartInfo("powershell.exe",
            "-NoProfile -ExecutionPolicy Bypass -File \"" + script + "\" " + string.Join(" ", args))
        {
            UseShellExecute = false,
            WorkingDirectory = pasta
        };

        using (Process processo = Process.Start(info))
        {
            processo.WaitForExit();
            return processo.ExitCode;
        }
    }
}
