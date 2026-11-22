package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pagamento de uma consulta (valor da consulta realizada ou taxa de cancelamento).
 */
public class Pagamento implements Identificavel {

    private final long id;
    private final Consulta consulta;
    private final BigDecimal valor;
    private final FormaPagamento forma;
    private final LocalDateTime pagoEm;

    public Pagamento(long id, Consulta consulta, BigDecimal valor, FormaPagamento forma, LocalDateTime pagoEm) {
        this.id = id;
        this.consulta = Validacao.naoNulo(consulta, "Consulta");
        this.valor = Dinheiro.arredondar(Validacao.positivo(valor, "Valor do pagamento"));
        this.forma = Validacao.naoNulo(forma, "Forma de pagamento");
        this.pagoEm = Validacao.naoNulo(pagoEm, "Data do pagamento");
    }

    @Override
    public long getId() {
        return id;
    }

    public Consulta getConsulta() {
        return consulta;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public LocalDateTime getPagoEm() {
        return pagoEm;
    }

    /** Tipo do paciente no momento do pagamento, usado no relatorio de faturamento. */
    public TipoPaciente getTipoPaciente() {
        return consulta.getPaciente().getTipo();
    }
}
