package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.EntidadeNaoEncontradaException;
import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.HorarioAtendimento;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.PacienteConvenio;
import br.com.bemestar.clinica.modelo.PacienteParticular;
import br.com.bemestar.clinica.modelo.PacienteVip;
import br.com.bemestar.clinica.modelo.Pessoa;
import br.com.bemestar.clinica.modelo.Validacao;
import br.com.bemestar.clinica.repositorio.Repositorio;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Cadastro, edicao e remocao de especialidades, medicos (com seus horarios)
 * e pacientes. Remocoes que deixariam o historico inconsistente sao bloqueadas.
 */
public class CadastroService {

    private final Repositorio<Especialidade> especialidades;
    private final Repositorio<Medico> medicos;
    private final Repositorio<Paciente> pacientes;
    private final Repositorio<Consulta> consultas;

    public CadastroService(Repositorio<Especialidade> especialidades,
                           Repositorio<Medico> medicos,
                           Repositorio<Paciente> pacientes,
                           Repositorio<Consulta> consultas) {
        this.especialidades = especialidades;
        this.medicos = medicos;
        this.pacientes = pacientes;
        this.consultas = consultas;
    }

    // ----------------------------------------------------------- especialidades

    public Especialidade cadastrarEspecialidade(String nome, BigDecimal valorBase) {
        String nomeValidado = Validacao.naoVazio(nome, "Nome da especialidade");
        boolean duplicada = !especialidades.filtrar(e -> e.getNome().equalsIgnoreCase(nomeValidado)).isEmpty();
        if (duplicada) {
            throw new RegraNegocioException("A especialidade " + nomeValidado + " ja esta cadastrada.");
        }
        return especialidades.salvar(new Especialidade(especialidades.proximoId(), nomeValidado, valorBase));
    }

    public Especialidade atualizarValorBase(long especialidadeId, BigDecimal novoValor) {
        Especialidade especialidade = buscarEspecialidade(especialidadeId);
        especialidade.atualizarValorBase(novoValor);
        return especialidade;
    }

    /** Remove uma especialidade que nao tenha medicos associados. */
    public void removerEspecialidade(long especialidadeId) {
        Especialidade especialidade = buscarEspecialidade(especialidadeId);
        if (!medicos.filtrar(m -> m.getEspecialidade().equals(especialidade)).isEmpty()) {
            throw new RegraNegocioException("A especialidade " + especialidade.getNome()
                    + " possui medicos associados e nao pode ser removida.");
        }
        especialidades.remover(especialidadeId);
    }

    public Especialidade buscarEspecialidade(long id) {
        return especialidades.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Especialidade", id));
    }

    public List<Especialidade> listarEspecialidades() {
        return especialidades.listarTodos();
    }

    // ----------------------------------------------------------- medicos

    public Medico cadastrarMedico(String nome, String cpf, String telefone, String crm, long especialidadeId) {
        Especialidade especialidade = buscarEspecialidade(especialidadeId);
        String crmValidado = Validacao.naoVazio(crm, "CRM");
        if (!medicos.filtrar(m -> m.getCrm().equalsIgnoreCase(crmValidado)).isEmpty()) {
            throw new RegraNegocioException("Ja existe um medico com o CRM " + crmValidado + ".");
        }
        String cpfValidado = Validacao.cpf(cpf);
        if (!medicos.filtrar(m -> m.getCpf().equals(cpfValidado)).isEmpty()) {
            throw new RegraNegocioException("Ja existe um medico com o CPF informado.");
        }
        return medicos.salvar(new Medico(medicos.proximoId(), nome, cpfValidado, telefone, crmValidado, especialidade));
    }

    public Medico definirHorarioMedico(long medicoId, DayOfWeek dia, LocalTime inicio, LocalTime fim) {
        Medico medico = buscarMedico(medicoId);
        medico.definirHorario(new HorarioAtendimento(dia, inicio, fim));
        return medico;
    }

    public Medico removerHorarioMedico(long medicoId, DayOfWeek dia) {
        Medico medico = buscarMedico(medicoId);
        medico.removerHorario(dia);
        return medico;
    }

    /** Atualiza nome e/ou telefone do medico; valores em branco mantem o dado atual. */
    public Medico atualizarDadosMedico(long medicoId, String nome, String telefone) {
        Medico medico = buscarMedico(medicoId);
        atualizarPessoa(medico, nome, telefone);
        return medico;
    }

    public Medico alterarEspecialidadeMedico(long medicoId, long especialidadeId) {
        Medico medico = buscarMedico(medicoId);
        medico.alterarEspecialidade(buscarEspecialidade(especialidadeId));
        return medico;
    }

    /** Remove um medico sem consultas registradas (o historico dos pacientes e preservado). */
    public void removerMedico(long medicoId) {
        Medico medico = buscarMedico(medicoId);
        if (!consultas.filtrar(c -> c.getMedico().equals(medico)).isEmpty()) {
            throw new RegraNegocioException("Dr(a). " + medico.getNome()
                    + " possui consultas registradas e nao pode ser removido(a).");
        }
        medicos.remover(medicoId);
    }

    public Medico buscarMedico(long id) {
        return medicos.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Medico", id));
    }

    public List<Medico> listarMedicos() {
        return medicos.listarTodos();
    }

    public List<Medico> medicosDaEspecialidade(long especialidadeId) {
        buscarEspecialidade(especialidadeId);
        return medicos.filtrar(m -> m.getEspecialidade().getId() == especialidadeId);
    }

    // ----------------------------------------------------------- pacientes

    public Paciente cadastrarPacienteParticular(String nome, String cpf, String telefone) {
        garantirCpfUnico(cpf);
        return pacientes.salvar(new PacienteParticular(pacientes.proximoId(), nome, cpf, telefone));
    }

    public Paciente cadastrarPacienteConvenio(String nome, String cpf, String telefone,
                                              String operadora, String numeroCarteirinha) {
        garantirCpfUnico(cpf);
        return pacientes.salvar(new PacienteConvenio(pacientes.proximoId(), nome, cpf, telefone,
                operadora, numeroCarteirinha));
    }

    public Paciente cadastrarPacienteVip(String nome, String cpf, String telefone) {
        garantirCpfUnico(cpf);
        return pacientes.salvar(new PacienteVip(pacientes.proximoId(), nome, cpf, telefone));
    }

    /** Atualiza nome e/ou telefone do paciente; valores em branco mantem o dado atual. */
    public Paciente atualizarDadosPaciente(long pacienteId, String nome, String telefone) {
        Paciente paciente = buscarPaciente(pacienteId);
        atualizarPessoa(paciente, nome, telefone);
        return paciente;
    }

    /** Remove um paciente sem consultas registradas (o historico clinico e preservado). */
    public void removerPaciente(long pacienteId) {
        Paciente paciente = buscarPaciente(pacienteId);
        if (!consultas.filtrar(c -> c.getPaciente().equals(paciente)).isEmpty()) {
            throw new RegraNegocioException("O paciente " + paciente.getNome()
                    + " possui consultas no historico e nao pode ser removido.");
        }
        pacientes.remover(pacienteId);
    }

    public Paciente buscarPaciente(long id) {
        return pacientes.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Paciente", id));
    }

    public List<Paciente> listarPacientes() {
        return pacientes.listarTodos();
    }

    private static void atualizarPessoa(Pessoa pessoa, String nome, String telefone) {
        if (nome != null && !nome.isBlank()) {
            pessoa.setNome(nome);
        }
        if (telefone != null && !telefone.isBlank()) {
            pessoa.setTelefone(telefone);
        }
    }

    private void garantirCpfUnico(String cpf) {
        String cpfValidado = Validacao.cpf(cpf);
        if (!pacientes.filtrar(p -> p.getCpf().equals(cpfValidado)).isEmpty()) {
            throw new RegraNegocioException("Ja existe um paciente com o CPF informado.");
        }
    }
}
