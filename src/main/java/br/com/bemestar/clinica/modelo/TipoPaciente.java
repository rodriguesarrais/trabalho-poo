package br.com.bemestar.clinica.modelo;

/**
 * Categorias de paciente, usadas tambem para agrupar o faturamento.
 */
public enum TipoPaciente {
    PARTICULAR("Particular"),
    CONVENIO("Convenio"),
    VIP("VIP");

    private final String descricao;

    TipoPaciente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
