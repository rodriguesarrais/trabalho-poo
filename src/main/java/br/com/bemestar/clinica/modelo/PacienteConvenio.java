package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;

/**
 * Paciente de convenio (plano de saude): 20% de desconto.
 */
public class PacienteConvenio extends Paciente {

    private static final BigDecimal DESCONTO = new BigDecimal("0.20");

    private final String operadora;
    private final String numeroCarteirinha;

    public PacienteConvenio(long id, String nome, String cpf, String telefone,
                            String operadora, String numeroCarteirinha) {
        super(id, nome, cpf, telefone);
        this.operadora = Validacao.naoVazio(operadora, "Operadora do convenio");
        this.numeroCarteirinha = Validacao.naoVazio(numeroCarteirinha, "Numero da carteirinha");
    }

    public String getOperadora() {
        return operadora;
    }

    public String getNumeroCarteirinha() {
        return numeroCarteirinha;
    }

    @Override
    public TipoPaciente getTipo() {
        return TipoPaciente.CONVENIO;
    }

    @Override
    public BigDecimal getPercentualDesconto() {
        return DESCONTO;
    }

    @Override
    public String toString() {
        return super.toString() + " " + operadora + " #" + numeroCarteirinha;
    }
}
