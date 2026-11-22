package br.com.bemestar.clinica.modelo;

import br.com.bemestar.clinica.excecao.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prescricao medica eletronica, sempre vinculada a uma consulta ja realizada.
 */
public class Prescricao implements Identificavel {

    private final long id;
    private final Consulta consulta;
    private final LocalDateTime emitidaEm;
    private final List<ItemPrescricao> itens;
    private final String observacoes;

    public Prescricao(long id, Consulta consulta, LocalDateTime emitidaEm,
                      List<ItemPrescricao> itens, String observacoes) {
        this.id = id;
        this.consulta = Validacao.naoNulo(consulta, "Consulta");
        this.emitidaEm = Validacao.naoNulo(emitidaEm, "Data de emissao");
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("A prescricao deve conter pelo menos um medicamento.");
        }
        this.itens = List.copyOf(itens);
        this.observacoes = observacoes == null ? "" : observacoes.trim();
    }

    @Override
    public long getId() {
        return id;
    }

    public Consulta getConsulta() {
        return consulta;
    }

    public Paciente getPaciente() {
        return consulta.getPaciente();
    }

    public Medico getMedico() {
        return consulta.getMedico();
    }

    public LocalDateTime getEmitidaEm() {
        return emitidaEm;
    }

    public List<ItemPrescricao> getItens() {
        return itens;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
