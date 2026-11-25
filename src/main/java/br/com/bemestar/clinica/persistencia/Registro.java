package br.com.bemestar.clinica.persistencia;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

/**
 * Uma linha lida de um arquivo CSV, com conversores tipados. Guarda o nome do
 * arquivo e o numero da linha para gerar mensagens de erro claras.
 */
final class Registro {

    private final String arquivo;
    private final int numeroLinha;
    private final List<String> campos;

    Registro(String arquivo, int numeroLinha, List<String> campos) {
        this.arquivo = arquivo;
        this.numeroLinha = numeroLinha;
        this.campos = campos;
    }

    String texto(int indice) {
        return campos.get(indice);
    }

    long longo(int indice) {
        return converter(indice, "numero inteiro", Long::parseLong);
    }

    BigDecimal decimal(int indice) {
        return converter(indice, "valor decimal", BigDecimal::new);
    }

    LocalDateTime dataHora(int indice) {
        return converter(indice, "data/hora", LocalDateTime::parse);
    }

    LocalTime hora(int indice) {
        return converter(indice, "horario", LocalTime::parse);
    }

    DayOfWeek diaDaSemana(int indice) {
        return converter(indice, "dia da semana", DayOfWeek::valueOf);
    }

    <E extends Enum<E>> E enumeracao(int indice, Class<E> tipo) {
        return converter(indice, tipo.getSimpleName(), valor -> Enum.valueOf(tipo, valor));
    }

    /** Converte o campo, trocando erros tecnicos por uma mensagem que indica coluna e valor. */
    private <T> T converter(int indice, String descricao, Function<String, T> conversor) {
        String valor = campos.get(indice).trim();
        try {
            return conversor.apply(valor);
        } catch (RuntimeException e) {
            throw erro("coluna " + (indice + 1) + " deveria ser " + descricao + ", mas contem '" + valor + "'");
        }
    }

    PersistenciaException erro(String mensagem) {
        return new PersistenciaException(local() + ": " + mensagem);
    }

    String local() {
        return "arquivo " + arquivo + ", linha " + numeroLinha;
    }
}
