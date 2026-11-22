package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Dinheiro;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.TipoConsulta;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regras de calculo de valores da clinica:
 * <ul>
 *   <li>valor da consulta = valor base da especialidade - desconto do tipo de paciente;</li>
 *   <li>encaixe emergencial: acrescimo de 20% sobre o valor;</li>
 *   <li>cancelamento com menos de 24h de antecedencia: taxa de 50% do valor da consulta.</li>
 * </ul>
 */
public class PoliticaPrecos {

    public static final BigDecimal ACRESCIMO_ENCAIXE = new BigDecimal("0.20");
    public static final BigDecimal PERCENTUAL_TAXA_CANCELAMENTO = new BigDecimal("0.50");
    public static final Duration ANTECEDENCIA_MINIMA_CANCELAMENTO = Duration.ofHours(24);

    public BigDecimal calcularValorConsulta(Especialidade especialidade, Paciente paciente, TipoConsulta tipo) {
        BigDecimal valor = paciente.aplicarDesconto(especialidade.getValorBase());
        if (tipo == TipoConsulta.ENCAIXE) {
            valor = valor.multiply(BigDecimal.ONE.add(ACRESCIMO_ENCAIXE));
        }
        return Dinheiro.arredondar(valor);
    }

    public BigDecimal calcularTaxaCancelamento(Consulta consulta, LocalDateTime momentoCancelamento) {
        Duration antecedencia = Duration.between(momentoCancelamento, consulta.getInicio());
        if (antecedencia.compareTo(ANTECEDENCIA_MINIMA_CANCELAMENTO) < 0) {
            return Dinheiro.arredondar(consulta.getValor().multiply(PERCENTUAL_TAXA_CANCELAMENTO));
        }
        return Dinheiro.ZERO;
    }
}
