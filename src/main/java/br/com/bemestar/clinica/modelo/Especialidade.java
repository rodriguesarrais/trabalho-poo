package br.com.bemestar.clinica.modelo;

import java.math.BigDecimal;

/**
 * Especialidade medica oferecida pela clinica, com o valor base da consulta
 * (antes de descontos por tipo de paciente ou acrescimo de encaixe).
 */
public class Especialidade implements Identificavel {

    private final long id;
    private final String nome;
    private BigDecimal valorBase;

    public Especialidade(long id, String nome, BigDecimal valorBase) {
        this.id = id;
        this.nome = Validacao.naoVazio(nome, "Nome da especialidade");
        this.valorBase = Dinheiro.arredondar(Validacao.positivo(valorBase, "Valor base"));
    }

    @Override
    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public void atualizarValorBase(BigDecimal novoValor) {
        this.valorBase = Dinheiro.arredondar(Validacao.positivo(novoValor, "Valor base"));
    }

    @Override
    public String toString() {
        return "#" + id + " " + nome + " (valor base " + Dinheiro.formatar(valorBase) + ")";
    }
}
