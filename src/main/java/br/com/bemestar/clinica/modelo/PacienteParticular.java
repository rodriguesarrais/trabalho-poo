package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;

/**
 * Paciente particular: paga o valor integral da consulta.
 */
public class PacienteParticular extends Paciente {

    public PacienteParticular(long id, String nome, String cpf, String telefone) {
        super(id, nome, cpf, telefone);
    }

    @Override
    public TipoPaciente getTipo() {
        return TipoPaciente.PARTICULAR;
    }

    @Override
    public BigDecimal getPercentualDesconto() {
        return BigDecimal.ZERO;
    }
}
