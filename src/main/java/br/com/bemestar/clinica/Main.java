package br.com.bemestar.clinica;

import br.com.bemestar.clinica.console.ConsoleApp;
import br.com.bemestar.clinica.persistencia.ArmazenamentoCsv;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Ponto de entrada do sistema da Clinica Bem Estar.
 *
 * <p>Uso: {@code java -jar clinica.jar [pasta-de-dados]}. Sem argumento, os
 * dados ficam na pasta "dados" do diretorio atual.</p>
 */
public class Main {

    public static void main(String[] args) {
        Path pastaDados = Path.of(args.length > 0 ? args[0] : "dados");
        new ConsoleApp(new ArmazenamentoCsv(pastaDados), pastaDados.resolve("relatorios"),
                Clock.systemDefaultZone(), System.in, System.out).executar();
    }
}
