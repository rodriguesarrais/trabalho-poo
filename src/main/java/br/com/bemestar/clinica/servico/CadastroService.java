package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.EntidadeNaoEncontradaException;
import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.HorarioAtendimento;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.PacienteConvenio;
import br.com.bemestar.clinica.modelo.PacienteParticular;
import br.com.bemestar.clinica.modelo.PacienteVip;
import br.com.bemestar.clinica.modelo.Validacao;
import br.com.bemestar.clinica.repositorio.Repositorio;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Cadastro de especialidades, medicos (com seus horarios) e pacientes.
 */
public class CadastroService {

    private final Repositorio<Especialidade> especialidades;
    private final Repositorio<Medico> medicos;
    private final Repositorio<Paciente> pacientes;

    public CadastroService(Repositorio<Especialidade> especialidades,
                           Repositorio<Medico> medicos,
                           Repositorio<Paciente> pacientes) {
        this.especialidades = especialidades;
        this.medicos = medicos;
        this.pacientes = pacientes;
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

    public Paciente buscarPaciente(long id) {
        return pacientes.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Paciente", id));
    }

    public List<Paciente> listarPacientes() {
        return pacientes.listarTodos();
    }

    private void garantirCpfUnico(String cpf) {
        String cpfValidado = Validacao.cpf(cpf);
        if (!pacientes.filtrar(p -> p.getCpf().equals(cpfValidado)).isEmpty()) {
            throw new RegraNegocioException("Ja existe um paciente com o CPF informado.");
        }
    }
}
