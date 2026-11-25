package br.com.bemestar.clinica.relatorio;

import br.com.bemestar.clinica.persistencia.PersistenciaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exporta o relatorio de faturamento para um arquivo TXT.
 */
public class ExportadorRelatorio {

    private final Path pasta;

    public ExportadorRelatorio(Path pasta) {
        this.pasta = pasta;
    }

    /** Grava o relatorio em um novo arquivo e retorna o caminho gerado. */
    public Path exportar(RelatorioFaturamento relatorio, LocalDateTime geradoEm) {
        String nome = "faturamento-" + geradoEm.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt";
        Path arquivo = pasta.resolve(nome);
        String conteudo = "Clinica Bem Estar\nGerado em "
                + geradoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n" + relatorio.formatar();
        try {
            Files.createDirectories(pasta);
            Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel exportar o relatorio: " + e.getMessage(), e);
        }
        return arquivo.toAbsolutePath().normalize();
    }
}
