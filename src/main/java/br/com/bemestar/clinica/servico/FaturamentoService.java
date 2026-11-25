package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.EntidadeNaoEncontradaException;
import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.FormaPagamento;
import br.com.bemestar.clinica.modelo.Pagamento;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.modelo.TipoPaciente;
import br.com.bemestar.clinica.relatorio.RelatorioFaturamento;
import br.com.bemestar.clinica.repositorio.Repositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Registro de pagamentos e geracao do relatorio de faturamento por tipo de paciente.
 */
public class FaturamentoService {

    private final Repositorio<Consulta> consultas;
    private final Repositorio<Pagamento> pagamentos;
    private final Clock relogio;

    public FaturamentoService(Repositorio<Consulta> consultas, Repositorio<Pagamento> pagamentos, Clock relogio) {
        this.consultas = consultas;
        this.pagamentos = pagamentos;
        this.relogio = relogio;
    }

    /**
     * Registra o pagamento de uma consulta. O valor e o devido pela consulta:
     * o valor integral se ela foi realizada, ou a taxa se ela foi cancelada.
     */
    public Pagamento registrarPagamento(long consultaId, FormaPagamento forma) {
        Consulta consulta = consultas.buscarPorId(consultaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta", consultaId));
        if (consulta.estaPaga()) {
            throw new RegraNegocioException("A consulta #" + consultaId + " ja foi paga.");
        }
        if (consulta.getStatus() == StatusConsulta.AGENDADA) {
            throw new RegraNegocioException("O pagamento so pode ser registrado apos a realizacao da consulta.");
        }
        if (consulta.getValorCobravel().signum() == 0) {
            throw new RegraNegocioException("A consulta #" + consultaId
                    + " foi cancelada sem taxa; nao ha valor a cobrar.");
        }
        if (forma == FormaPagamento.CONVENIO && consulta.getPaciente().getTipo() != TipoPaciente.CONVENIO) {
            throw new RegraNegocioException("Apenas pacientes de convenio podem ter a consulta faturada ao convenio.");
        }
        Pagamento pagamento = new Pagamento(pagamentos.proximoId(), consulta, consulta.getValorCobravel(),
                forma, LocalDateTime.now(relogio));
        consulta.registrarPagamento(pagamento);
        return pagamentos.salvar(pagamento);
    }

    /** Consultas realizadas (ou canceladas com taxa) que ainda nao foram pagas. */
    public List<Consulta> pagamentosPendentes() {
        return consultas.filtrar(c -> !c.estaPaga() && c.getValorCobravel().signum() > 0).stream()
                .sorted(Comparator.comparing(Consulta::getInicio))
                .toList();
    }

    public List<Pagamento> listarPagamentos() {
        return pagamentos.listarTodos();
    }

    /** Relatorio de faturamento de todo o periodo. */
    public RelatorioFaturamento gerarRelatorio() {
        return RelatorioFaturamento.gerar(null, null, pagamentos.listarTodos());
    }

    /** Relatorio de faturamento dos pagamentos feitos entre as datas (inclusive). */
    public RelatorioFaturamento gerarRelatorio(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual a data final.");
        }
        List<Pagamento> doPeriodo = pagamentos.filtrar(p -> {
            LocalDate data = p.getPagoEm().toLocalDate();
            return !data.isBefore(inicio) && !data.isAfter(fim);
        });
        return RelatorioFaturamento.gerar(inicio, fim, doPeriodo);
    }
}
