package br.com.bemestar.clinica.persistencia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvTest {

    @Test
    void escapaEDesfazEscapeDeCamposEspeciais() {
        String conteudo = Csv.linha("a;b", "diz \"oi\"", "linha1\nlinha2", null, 10);

        List<List<String>> linhas = Csv.ler(conteudo);

        assertEquals(List.of(List.of("a;b", "diz \"oi\"", "linha1\nlinha2", "", "10")), linhas);
    }

    @Test
    void ignoraLinhasEmBrancoEAceitaFinalDeLinhaWindows() {
        assertEquals(List.of(List.of("1", "x"), List.of("2", "y")), Csv.ler("1;x\r\n\r\n2;y"));
    }

    @Test
    void aspasNaoFechadasSaoErro() {
        assertThrows(IllegalArgumentException.class, () -> Csv.ler("1;\"aberto\n"));
    }
}
