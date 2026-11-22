package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;

/**
 * Paciente da clinica. Cada categoria concreta define o seu proprio desconto,
 * de modo que o valor da consulta e calculado de forma polimorfica.
 */
public abstract class Paciente extends Pessoa {

    protected Paciente(long id, String nome, String cpf, String telefone) {
        super(id, nome, cpf, telefone);
    }

    public abstract TipoPaciente getTipo();

    /** Desconto sobre o valor base da especialidade, em fracao (0.20 = 20%). */
    public abstract BigDecimal getPercentualDesconto();

    public BigDecimal aplicarDesconto(BigDecimal valor) {
        return valor.multiply(BigDecimal.ONE.subtract(getPercentualDesconto()));
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getTipo().getDescricao() + "]";
    }
}
