package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.EntidadeNaoEncontradaException;
import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.ItemPrescricao;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.Prescricao;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.repositorio.Repositorio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Historico de consultas/diagnosticos e emissao de prescricoes eletronicas.
 */
public class ProntuarioService {

    private final CadastroService cadastro;
    private final Repositorio<Consulta> consultas;
    private final Repositorio<Prescricao> prescricoes;
    private final Clock relogio;

    public ProntuarioService(CadastroService cadastro, Repositorio<Consulta> consultas,
                             Repositorio<Prescricao> prescricoes, Clock relogio) {
        this.cadastro = cadastro;
        this.consultas = consultas;
        this.prescricoes = prescricoes;
        this.relogio = relogio;
    }

    /** Emite uma prescricao vinculada a uma consulta ja realizada. */
    public Prescricao emitirPrescricao(long consultaId, List<ItemPrescricao> itens, String observacoes) {
        Consulta consulta = consultas.buscarPorId(consultaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta", consultaId));
        if (consulta.getStatus() != StatusConsulta.REALIZADA) {
            throw new RegraNegocioException("Prescricoes so podem ser emitidas para consultas ja realizadas.");
        }
        Prescricao prescricao = new Prescricao(prescricoes.proximoId(), consulta,
                LocalDateTime.now(relogio), itens, observacoes);
        consulta.adicionarPrescricao(prescricao);
        return prescricoes.salvar(prescricao);
    }

    public Prescricao buscarPrescricao(long id) {
        return prescricoes.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Prescricao", id));
    }

    /** Historico completo do paciente, em ordem cronologica. */
    public HistoricoPaciente historicoDoPaciente(long pacienteId) {
        Paciente paciente = cadastro.buscarPaciente(pacienteId);
        List<Consulta> doPaciente = consultas.filtrar(c -> c.getPaciente().equals(paciente)).stream()
                .sorted(Comparator.comparing(Consulta::getInicio))
                .toList();
        return new HistoricoPaciente(paciente, doPaciente);
    }
}
