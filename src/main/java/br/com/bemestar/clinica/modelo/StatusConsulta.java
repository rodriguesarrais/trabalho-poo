package br.com.bemestar.clinica.modelo;

/**
 * Ciclo de vida de uma consulta: AGENDADA -> REALIZADA ou AGENDADA -> CANCELADA.
 */
public enum StatusConsulta {
    AGENDADA("Agendada"),
    REALIZADA("Realizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusConsulta(String descricao) {
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
