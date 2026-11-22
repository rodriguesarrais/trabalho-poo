package br.com.bemestar.clinica.modelo;

import br.com.bemestar.clinica.excecao.RegraNegocioException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Consulta medica. Guarda o valor calculado no agendamento e controla as
 * transicoes de status (realizacao, cancelamento), o diagnostico, as
 * prescricoes emitidas e o pagamento.
 */
public class Consulta implements Identificavel {

    /** Toda consulta dura 30 minutos. */
    public static final Duration DURACAO = Duration.ofMinutes(30);

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final long id;
    private final Paciente paciente;
    private final Medico medico;
    private final LocalDateTime inicio;
    private final TipoConsulta tipo;
    private final BigDecimal valor;

    private StatusConsulta status = StatusConsulta.AGENDADA;
    private String diagnostico;
    private String observacoes;
    private LocalDateTime canceladaEm;
    private String motivoCancelamento;
    private BigDecimal taxaCancelamento = Dinheiro.ZERO;
    private Pagamento pagamento;
    private final List<Prescricao> prescricoes = new ArrayList<>();

    public Consulta(long id, Paciente paciente, Medico medico, LocalDateTime inicio,
                    TipoConsulta tipo, BigDecimal valor) {
        this.id = id;
        this.paciente = Validacao.naoNulo(paciente, "Paciente");
        this.medico = Validacao.naoNulo(medico, "Medico");
        this.inicio = Validacao.naoNulo(inicio, "Data/hora da consulta");
        this.tipo = Validacao.naoNulo(tipo, "Tipo de consulta");
        this.valor = Dinheiro.arredondar(Validacao.positivo(valor, "Valor da consulta"));
    }

    // ----------------------------------------------------------- regras de status

    /** Cancela a consulta, registrando o momento, o motivo e a taxa cobrada (pode ser zero). */
    public void cancelar(LocalDateTime momento, String motivo, BigDecimal taxa) {
        exigirStatus(StatusConsulta.AGENDADA, "cancelar");
        this.canceladaEm = Validacao.naoNulo(momento, "Momento do cancelamento");
        this.motivoCancelamento = motivo == null ? "" : motivo.trim();
        this.taxaCancelamento = Dinheiro.arredondar(Validacao.naoNulo(taxa, "Taxa de cancelamento"));
        this.status = StatusConsulta.CANCELADA;
    }

    /** Marca a consulta como realizada, registrando o diagnostico no historico do paciente. */
    public void registrarRealizacao(String diagnostico, String observacoes) {
        exigirStatus(StatusConsulta.AGENDADA, "registrar a realizacao de");
        this.diagnostico = Validacao.naoVazio(diagnostico, "Diagnostico");
        this.observacoes = observacoes == null ? "" : observacoes.trim();
        this.status = StatusConsulta.REALIZADA;
    }

    public void adicionarPrescricao(Prescricao prescricao) {
        if (status != StatusConsulta.REALIZADA) {
            throw new RegraNegocioException("Prescricoes so podem ser emitidas para consultas ja realizadas.");
        }
        if (prescricao.getConsulta() != this) {
            throw new RegraNegocioException("A prescricao pertence a outra consulta.");
        }
        prescricoes.add(prescricao);
    }

    /**
     * Valor que pode ser cobrado no status atual: o valor da consulta se ela foi
     * realizada, a taxa se ela foi cancelada, e zero enquanto estiver agendada.
     */
    public BigDecimal getValorCobravel() {
        return switch (status) {
            case REALIZADA -> valor;
            case CANCELADA -> taxaCancelamento;
            case AGENDADA -> Dinheiro.ZERO;
        };
    }

    public void registrarPagamento(Pagamento pagamento) {
        Validacao.naoNulo(pagamento, "Pagamento");
        if (estaPaga()) {
            throw new RegraNegocioException("A consulta #" + id + " ja foi paga.");
        }
        if (getValorCobravel().signum() == 0) {
            throw new RegraNegocioException("A consulta #" + id + " nao possui valor a cobrar.");
        }
        if (pagamento.getValor().compareTo(getValorCobravel()) != 0) {
            throw new RegraNegocioException("O valor pago deve ser igual ao valor devido ("
                    + Dinheiro.formatar(getValorCobravel()) + ").");
        }
        this.pagamento = pagamento;
    }

    private void exigirStatus(StatusConsulta esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException("Nao e possivel " + acao + " uma consulta com status "
                    + status.getDescricao().toLowerCase() + ".");
        }
    }

    // ----------------------------------------------------------- consultas de estado

    public LocalDateTime getFim() {
        return inicio.plus(DURACAO);
    }

    /** Indica se a consulta ocupa algum instante do intervalo [inicio, fim). */
    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return inicio.isBefore(outroFim) && outroInicio.isBefore(getFim());
    }

    /** Consultas canceladas nao ocupam a agenda. */
    public boolean estaAtiva() {
        return status != StatusConsulta.CANCELADA;
    }

    public boolean isEncaixe() {
        return tipo == TipoConsulta.ENCAIXE;
    }

    public boolean estaPaga() {
        return pagamento != null;
    }

    // ----------------------------------------------------------- getters

    @Override
    public long getId() {
        return id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public TipoConsulta getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusConsulta getStatus() {
        return status;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public BigDecimal getTaxaCancelamento() {
        return taxaCancelamento;
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public List<Prescricao> getPrescricoes() {
        return Collections.unmodifiableList(prescricoes);
    }

    @Override
    public String toString() {
        return "#" + id + " " + inicio.format(FORMATO) + " | " + tipo.getDescricao()
                + " | " + medico.getEspecialidade().getNome() + " - Dr(a). " + medico.getNome()
                + " | Paciente: " + paciente.getNome() + " (" + paciente.getTipo().getDescricao() + ")"
                + " | " + Dinheiro.formatar(valor) + " | " + status.getDescricao();
    }
}
