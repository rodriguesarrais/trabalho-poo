package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;

/**
 * Paciente VIP: 30% de desconto.
 */
public class PacienteVip extends Paciente {

    private static final BigDecimal DESCONTO = new BigDecimal("0.30");

    public PacienteVip(long id, String nome, String cpf, String telefone) {
        super(id, nome, cpf, telefone);
    }

    @Override
    public TipoPaciente getTipo() {
        return TipoPaciente.VIP;
    }

    @Override
    public BigDecimal getPercentualDesconto() {
        return DESCONTO;
    }
}
