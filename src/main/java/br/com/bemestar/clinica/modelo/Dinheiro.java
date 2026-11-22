package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Utilitarios monetarios. Todos os valores sao BigDecimal com duas casas
 * decimais, evitando erros de arredondamento de ponto flutuante.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static String formatar(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", arredondar(valor));
    }
}
