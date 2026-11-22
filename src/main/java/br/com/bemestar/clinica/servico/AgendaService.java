package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.EntidadeNaoEncontradaException;
import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.modelo.TipoConsulta;
import br.com.bemestar.clinica.modelo.Validacao;
import br.com.bemestar.clinica.repositorio.Repositorio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia a agenda: horarios disponiveis, agendamentos, encaixes,
 * cancelamentos e registro de consultas realizadas.
 *
 * <p>Regras aplicadas:</p>
 * <ul>
 *   <li>consultas normais duram 30 minutos e so podem ocupar um slot livre do
 *       horario de atendimento do medico (sem dois agendamentos no mesmo horario);</li>
 *   <li>encaixes emergenciais podem ser feitos em qualquer momento do turno, mesmo
 *       em horario ja ocupado, limitados a 2 por medico por dia e com acrescimo de 20%;</li>
 *   <li>um paciente nao pode ter duas consultas agendadas que se sobreponham;</li>
 *   <li>cancelamento com menos de 24h de antecedencia gera taxa de 50%.</li>
 * </ul>
 */
public class AgendaService {

    public static final int MAXIMO_ENCAIXES_POR_DIA = 2;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CadastroService cadastro;
    private final Repositorio<Consulta> consultas;
    private final PoliticaPrecos politicaPrecos;
    private final Clock relogio;

    public AgendaService(CadastroService cadastro, Repositorio<Consulta> consultas,
                         PoliticaPrecos politicaPrecos, Clock relogio) {
        this.cadastro = cadastro;
        this.consultas = consultas;
        this.politicaPrecos = politicaPrecos;
        this.relogio = relogio;
    }

    // ----------------------------------------------------------- disponibilidade

    /** Slots livres (e ainda nao passados) do medico na data informada. */
    public List<LocalDateTime> horariosDisponiveis(long medicoId, LocalDate data) {
        return horariosDisponiveis(cadastro.buscarMedico(medicoId), data);
    }

    /** Slots livres de cada medico da especialidade na data informada. */
    public Map<Medico, List<LocalDateTime>> horariosDisponiveisPorEspecialidade(long especialidadeId, LocalDate data) {
        Map<Medico, List<LocalDateTime>> resultado = new LinkedHashMap<>();
        for (Medico medico : cadastro.medicosDaEspecialidade(especialidadeId)) {
            List<LocalDateTime> livres = horariosDisponiveis(medico, data);
            if (!livres.isEmpty()) {
                resultado.put(medico, livres);
            }
        }
        return resultado;
    }

    /** Quantos encaixes ainda podem ser feitos para o medico na data. */
    public int encaixesRestantes(long medicoId, LocalDate data) {
        Medico medico = cadastro.buscarMedico(medicoId);
        return MAXIMO_ENCAIXES_POR_DIA - encaixesAtivos(medico, data).size();
    }

    private List<LocalDateTime> horariosDisponiveis(Medico medico, LocalDate data) {
        LocalDateTime agora = agora();
        List<Consulta> ocupadas = consultasNormaisAtivas(medico, data);
        return medico.slotsNo(data, Consulta.DURACAO).stream()
                .filter(slot -> !slot.isBefore(agora))
                .filter(slot -> ocupadas.stream().noneMatch(c -> c.getInicio().equals(slot)))
                .toList();
    }

    // ----------------------------------------------------------- agendamento

    /** Agenda uma consulta normal com um medico especifico. */
    public Consulta agendar(long pacienteId, long medicoId, LocalDateTime inicio) {
        Paciente paciente = cadastro.buscarPaciente(pacienteId);
        Medico medico = cadastro.buscarMedico(medicoId);
        LocalDateTime horario = normalizar(inicio);

        garantirNaoPassado(horario);
        if (!medico.slotsNo(horario.toLocalDate(), Consulta.DURACAO).contains(horario)) {
            throw new RegraNegocioException("Dr(a). " + medico.getNome() + " nao possui horario de atendimento"
                    + " iniciando em " + horario.format(FORMATO) + ". Consulte os horarios disponiveis.");
        }
        boolean ocupado = consultasNormaisAtivas(medico, horario.toLocalDate()).stream()
                .anyMatch(c -> c.getInicio().equals(horario));
        if (ocupado) {
            throw new RegraNegocioException("Dr(a). " + medico.getNome() + " ja possui consulta agendada em "
                    + horario.format(FORMATO) + ".");
        }
        garantirPacienteDisponivel(paciente, horario);

        return criarConsulta(paciente, medico, horario, TipoConsulta.NORMAL);
    }

    /** Agenda uma consulta normal com o primeiro medico da especialidade livre no horario. */
    public Consulta agendarPorEspecialidade(long pacienteId, long especialidadeId, LocalDateTime inicio) {
        cadastro.buscarPaciente(pacienteId);
        Especialidade especialidade = cadastro.buscarEspecialidade(especialidadeId);
        LocalDateTime horario = normalizar(inicio);

        for (Medico medico : cadastro.medicosDaEspecialidade(especialidadeId)) {
            if (horariosDisponiveis(medico, horario.toLocalDate()).contains(horario)) {
                return agendar(pacienteId, medico.getId(), horario);
            }
        }
        throw new RegraNegocioException("Nenhum medico de " + especialidade.getNome()
                + " esta disponivel em " + horario.format(FORMATO) + ".");
    }

    /**
     * Agenda um encaixe emergencial. O encaixe deve estar dentro do turno do
     * medico, nao pode coincidir com outro encaixe e cada medico aceita no
     * maximo 2 encaixes por dia. O valor recebe acrescimo de 20%.
     */
    public Consulta agendarEncaixe(long pacienteId, long medicoId, LocalDateTime inicio) {
        Paciente paciente = cadastro.buscarPaciente(pacienteId);
        Medico medico = cadastro.buscarMedico(medicoId);
        LocalDateTime horario = normalizar(inicio);
        LocalDateTime fim = horario.plus(Consulta.DURACAO);

        garantirNaoPassado(horario);
        if (!medico.estaEmAtendimento(horario)) {
            throw new RegraNegocioException("O encaixe deve ser feito dentro do horario de atendimento de Dr(a). "
                    + medico.getNome() + ".");
        }
        List<Consulta> encaixesDoDia = encaixesAtivos(medico, horario.toLocalDate());
        if (encaixesDoDia.size() >= MAXIMO_ENCAIXES_POR_DIA) {
            throw new RegraNegocioException("Dr(a). " + medico.getNome() + " ja atingiu o limite de "
                    + MAXIMO_ENCAIXES_POR_DIA + " encaixes no dia " + horario.toLocalDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        }
        if (encaixesDoDia.stream().anyMatch(c -> c.sobrepoe(horario, fim))) {
            throw new RegraNegocioException("Ja existe outro encaixe nesse horario.");
        }
        garantirPacienteDisponivel(paciente, horario);

        return criarConsulta(paciente, medico, horario, TipoConsulta.ENCAIXE);
    }

    // ----------------------------------------------------------- cancelamento e realizacao

    /**
     * Cancela uma consulta agendada. Se faltarem menos de 24h para o inicio,
     * e cobrada uma taxa de 50% do valor da consulta.
     */
    public Consulta cancelar(long consultaId, String motivo) {
        Consulta consulta = buscarConsulta(consultaId);
        LocalDateTime agora = agora();
        if (consulta.getStatus() == StatusConsulta.AGENDADA && !agora.isBefore(consulta.getInicio())) {
            throw new RegraNegocioException("A consulta ja comecou e nao pode mais ser cancelada.");
        }
        BigDecimal taxa = politicaPrecos.calcularTaxaCancelamento(consulta, agora);
        consulta.cancelar(agora, motivo, taxa);
        return consulta;
    }

    /** Registra a consulta como realizada, com diagnostico e observacoes clinicas. */
    public Consulta registrarRealizacao(long consultaId, String diagnostico, String observacoes) {
        Consulta consulta = buscarConsulta(consultaId);
        consulta.registrarRealizacao(diagnostico, observacoes);
        return consulta;
    }

    // ----------------------------------------------------------- consultas

    public Consulta buscarConsulta(long id) {
        return consultas.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Consulta", id));
    }

    public List<Consulta> listarConsultas() {
        return ordenar(consultas.listarTodos());
    }

    public List<Consulta> consultasDoMedicoNoDia(long medicoId, LocalDate data) {
        Medico medico = cadastro.buscarMedico(medicoId);
        return ordenar(consultas.filtrar(c -> c.getMedico().equals(medico)
                && c.getInicio().toLocalDate().equals(data)));
    }

    // ----------------------------------------------------------- auxiliares

    private Consulta criarConsulta(Paciente paciente, Medico medico, LocalDateTime horario, TipoConsulta tipo) {
        BigDecimal valor = politicaPrecos.calcularValorConsulta(medico.getEspecialidade(), paciente, tipo);
        return consultas.salvar(new Consulta(consultas.proximoId(), paciente, medico, horario, tipo, valor));
    }

    private List<Consulta> consultasNormaisAtivas(Medico medico, LocalDate data) {
        return consultas.filtrar(c -> c.getMedico().equals(medico) && c.estaAtiva() && !c.isEncaixe()
                && c.getInicio().toLocalDate().equals(data));
    }

    private List<Consulta> encaixesAtivos(Medico medico, LocalDate data) {
        return consultas.filtrar(c -> c.getMedico().equals(medico) && c.estaAtiva() && c.isEncaixe()
                && c.getInicio().toLocalDate().equals(data));
    }

    private void garantirPacienteDisponivel(Paciente paciente, LocalDateTime inicio) {
        LocalDateTime fim = inicio.plus(Consulta.DURACAO);
        boolean conflito = !consultas.filtrar(c -> c.getPaciente().equals(paciente)
                && c.getStatus() == StatusConsulta.AGENDADA
                && c.sobrepoe(inicio, fim)).isEmpty();
        if (conflito) {
            throw new RegraNegocioException("O paciente " + paciente.getNome()
                    + " ja possui outra consulta agendada nesse horario.");
        }
    }

    private void garantirNaoPassado(LocalDateTime horario) {
        if (horario.isBefore(agora())) {
            throw new RegraNegocioException("Nao e possivel agendar consultas no passado.");
        }
    }

    private static LocalDateTime normalizar(LocalDateTime horario) {
        return Validacao.naoNulo(horario, "Data/hora da consulta").truncatedTo(ChronoUnit.MINUTES);
    }

    private static List<Consulta> ordenar(List<Consulta> lista) {
        return lista.stream().sorted(Comparator.comparing(Consulta::getInicio)).toList();
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.MINUTES);
    }
}
