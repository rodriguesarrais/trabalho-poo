package br.com.bemestar.clinica.modelo;

/**
 * Tipo da consulta: normal (em um slot da agenda) ou encaixe emergencial.
 */
public enum TipoConsulta {
    NORMAL("Normal"),
    ENCAIXE("Encaixe emergencial");

    private final String descricao;

    TipoConsulta(String descricao) {
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
