package br.com.bemestar.clinica.console;

import br.com.bemestar.clinica.Clinica;
import br.com.bemestar.clinica.excecao.ClinicaException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Dinheiro;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.FormaPagamento;
import br.com.bemestar.clinica.modelo.HorarioAtendimento;
import br.com.bemestar.clinica.modelo.ItemPrescricao;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.Pagamento;
import br.com.bemestar.clinica.modelo.Prescricao;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.modelo.TipoPaciente;
import br.com.bemestar.clinica.servico.AgendaService;
import br.com.bemestar.clinica.servico.HistoricoPaciente;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interface de console (menus de texto) do sistema da clinica.
 */
public class ConsoleApp {

    /** Uma opcao de menu: descricao exibida e acao executada. */
    private record Opcao(String descricao, Runnable acao) {
    }

    private final Clinica clinica;
    private final Entrada entrada;
    private final PrintStream saida;

    public ConsoleApp(Clinica clinica, InputStream in, PrintStream out) {
        this.clinica = clinica;
        this.entrada = new Entrada(in, out);
        this.saida = out;
    }

    public void executar() {
        saida.println("==============================================");
        saida.println("   CLINICA BEM ESTAR - Sistema de Gestao");
        saida.println("==============================================");
        try {
            if (entrada.confirmar("Deseja carregar dados de exemplo?")) {
                DadosExemplo.carregar(clinica);
                saida.println("Dados de exemplo carregados.");
            }
            menu("MENU PRINCIPAL", "Sair", List.of(
                    new Opcao("Especialidades", this::menuEspecialidades),
                    new Opcao("Medicos e horarios", this::menuMedicos),
                    new Opcao("Pacientes", this::menuPacientes),
                    new Opcao("Agenda de consultas", this::menuAgenda),
                    new Opcao("Prontuario (historico e prescricoes)", this::menuProntuario),
                    new Opcao("Pagamentos e faturamento", this::menuFaturamento)));
        } catch (EntradaEncerradaException e) {
            saida.println();
        }
        saida.println("Ate logo!");
    }

    /** Laco generico de menu: exibe as opcoes, le a escolha e trata erros de negocio. */
    private void menu(String titulo, String rotuloSaida, List<Opcao> opcoes) {
        while (true) {
            saida.println();
            saida.println("=== " + titulo + " ===");
            for (int i = 0; i < opcoes.size(); i++) {
                saida.printf("%d. %s%n", i + 1, opcoes.get(i).descricao());
            }
            saida.println("0. " + rotuloSaida);
            int escolha = entrada.inteiro("Opcao", 0, opcoes.size());
            if (escolha == 0) {
                return;
            }
            try {
                opcoes.get(escolha - 1).acao().run();
            } catch (ClinicaException e) {
                saida.println("ERRO: " + e.getMessage());
            }
        }
    }

    private void submenu(String titulo, List<Opcao> opcoes) {
        menu(titulo, "Voltar", opcoes);
    }

    // =================================================================== especialidades

    private void menuEspecialidades() {
        submenu("ESPECIALIDADES", List.of(
                new Opcao("Cadastrar especialidade", this::cadastrarEspecialidade),
                new Opcao("Listar especialidades", this::listarEspecialidades),
                new Opcao("Atualizar valor base", this::atualizarValorBase)));
    }

    private void cadastrarEspecialidade() {
        String nome = entrada.texto("Nome");
        Especialidade especialidade = clinica.cadastro().cadastrarEspecialidade(nome, entrada.valor("Valor base da consulta"));
        saida.println("Especialidade cadastrada: " + especialidade);
    }

    private void listarEspecialidades() {
        imprimirLista("Nenhuma especialidade cadastrada.", clinica.cadastro().listarEspecialidades());
    }

    private void atualizarValorBase() {
        listarEspecialidades();
        long id = entrada.id("Id da especialidade");
        Especialidade especialidade = clinica.cadastro().atualizarValorBase(id, entrada.valor("Novo valor base"));
        saida.println("Valor atualizado: " + especialidade);
    }

    // =================================================================== medicos

    private void menuMedicos() {
        submenu("MEDICOS E HORARIOS", List.of(
                new Opcao("Cadastrar medico", this::cadastrarMedico),
                new Opcao("Listar medicos e horarios", this::listarMedicos),
                new Opcao("Definir horario de atendimento", this::definirHorario),
                new Opcao("Remover horario de atendimento", this::removerHorario),
                new Opcao("Listar medicos por especialidade", this::listarMedicosPorEspecialidade)));
    }

    private void cadastrarMedico() {
        if (clinica.cadastro().listarEspecialidades().isEmpty()) {
            saida.println("Cadastre uma especialidade antes de cadastrar medicos.");
            return;
        }
        String nome = entrada.texto("Nome");
        String cpf = entrada.texto("CPF");
        String telefone = entrada.textoOpcional("Telefone");
        String crm = entrada.texto("CRM");
        listarEspecialidades();
        long especialidadeId = entrada.id("Id da especialidade");
        Medico medico = clinica.cadastro().cadastrarMedico(nome, cpf, telefone, crm, especialidadeId);
        saida.println("Medico cadastrado: " + medico);
        while (entrada.confirmar("Deseja definir um horario de atendimento agora?")) {
            lerEDefinirHorario(medico.getId());
        }
    }

    private void listarMedicos() {
        List<Medico> medicos = clinica.cadastro().listarMedicos();
        if (medicos.isEmpty()) {
            saida.println("Nenhum medico cadastrado.");
            return;
        }
        medicos.forEach(this::imprimirMedico);
    }

    private void imprimirMedico(Medico medico) {
        saida.println(medico);
        Collection<HorarioAtendimento> agenda = medico.getAgendaSemanal();
        saida.println("   Horarios: " + (agenda.isEmpty() ? "nenhum definido"
                : agenda.stream().map(HorarioAtendimento::toString).collect(Collectors.joining("; "))));
    }

    private void definirHorario() {
        listarMedicos();
        lerEDefinirHorario(entrada.id("Id do medico"));
    }

    private void lerEDefinirHorario(long medicoId) {
        DayOfWeek dia = entrada.diaDaSemana("Dia da semana");
        LocalTime inicio = entrada.hora("Inicio do atendimento");
        LocalTime fim = entrada.hora("Fim do atendimento");
        imprimirMedico(clinica.cadastro().definirHorarioMedico(medicoId, dia, inicio, fim));
    }

    private void removerHorario() {
        listarMedicos();
        long medicoId = entrada.id("Id do medico");
        DayOfWeek dia = entrada.diaDaSemana("Dia da semana");
        imprimirMedico(clinica.cadastro().removerHorarioMedico(medicoId, dia));
    }

    private void listarMedicosPorEspecialidade() {
        listarEspecialidades();
        List<Medico> medicos = clinica.cadastro().medicosDaEspecialidade(entrada.id("Id da especialidade"));
        if (medicos.isEmpty()) {
            saida.println("Nenhum medico cadastrado nesta especialidade.");
        }
        medicos.forEach(this::imprimirMedico);
    }

    // =================================================================== pacientes

    private void menuPacientes() {
        submenu("PACIENTES", List.of(
                new Opcao("Cadastrar paciente", this::cadastrarPaciente),
                new Opcao("Listar pacientes", this::listarPacientes)));
    }

    private void cadastrarPaciente() {
        TipoPaciente tipo = entrada.opcao("Tipo de paciente", TipoPaciente.values());
        String nome = entrada.texto("Nome");
        String cpf = entrada.texto("CPF");
        String telefone = entrada.textoOpcional("Telefone");
        Paciente paciente = switch (tipo) {
            case PARTICULAR -> clinica.cadastro().cadastrarPacienteParticular(nome, cpf, telefone);
            case CONVENIO -> clinica.cadastro().cadastrarPacienteConvenio(nome, cpf, telefone,
                    entrada.texto("Operadora do convenio"), entrada.texto("Numero da carteirinha"));
            case VIP -> clinica.cadastro().cadastrarPacienteVip(nome, cpf, telefone);
        };
        saida.println("Paciente cadastrado: " + paciente);
    }

    private void listarPacientes() {
        imprimirLista("Nenhum paciente cadastrado.", clinica.cadastro().listarPacientes());
    }

    // =================================================================== agenda

    private void menuAgenda() {
        submenu("AGENDA DE CONSULTAS", List.of(
                new Opcao("Ver horarios disponiveis de um medico", this::horariosDoMedico),
                new Opcao("Ver horarios disponiveis por especialidade", this::horariosPorEspecialidade),
                new Opcao("Agendar consulta com um medico", this::agendarComMedico),
                new Opcao("Agendar consulta por especialidade", this::agendarPorEspecialidade),
                new Opcao("Encaixe emergencial (+20%)", this::agendarEncaixe),
                new Opcao("Cancelar consulta", this::cancelarConsulta),
                new Opcao("Registrar consulta realizada (diagnostico)", this::registrarRealizacao),
                new Opcao("Agenda de um medico no dia", this::agendaDoMedico),
                new Opcao("Listar todas as consultas", this::listarConsultas)));
    }

    private void horariosDoMedico() {
        listarMedicos();
        long medicoId = entrada.id("Id do medico");
        LocalDate data = entrada.data("Data");
        List<LocalDateTime> livres = clinica.agenda().horariosDisponiveis(medicoId, data);
        saida.println(livres.isEmpty() ? "Nenhum horario disponivel nesta data." : "Horarios livres: " + horas(livres));
        saida.println("Encaixes restantes no dia: " + clinica.agenda().encaixesRestantes(medicoId, data)
                + " de " + AgendaService.MAXIMO_ENCAIXES_POR_DIA);
    }

    private void horariosPorEspecialidade() {
        listarEspecialidades();
        long especialidadeId = entrada.id("Id da especialidade");
        LocalDate data = entrada.data("Data");
        Map<Medico, List<LocalDateTime>> livres = clinica.agenda().horariosDisponiveisPorEspecialidade(especialidadeId, data);
        if (livres.isEmpty()) {
            saida.println("Nenhum horario disponivel nesta data.");
        }
        livres.forEach((medico, horarios) -> saida.println(medico + "\n   " + horas(horarios)));
    }

    private void agendarComMedico() {
        long pacienteId = escolherPaciente();
        listarMedicos();
        long medicoId = entrada.id("Id do medico");
        LocalDateTime inicio = entrada.dataHora("Data e hora");
        saida.println("Consulta agendada: " + clinica.agenda().agendar(pacienteId, medicoId, inicio));
    }

    private void agendarPorEspecialidade() {
        long pacienteId = escolherPaciente();
        listarEspecialidades();
        long especialidadeId = entrada.id("Id da especialidade");
        LocalDateTime inicio = entrada.dataHora("Data e hora");
        saida.println("Consulta agendada: " + clinica.agenda().agendarPorEspecialidade(pacienteId, especialidadeId, inicio));
    }

    private void agendarEncaixe() {
        long pacienteId = escolherPaciente();
        listarMedicos();
        long medicoId = entrada.id("Id do medico");
        LocalDateTime inicio = entrada.dataHora("Data e hora do encaixe");
        saida.println("Encaixe agendado: " + clinica.agenda().agendarEncaixe(pacienteId, medicoId, inicio));
    }

    private void cancelarConsulta() {
        long consultaId = entrada.id("Id da consulta");
        Consulta consulta = clinica.agenda().buscarConsulta(consultaId);
        saida.println(consulta);
        if (!entrada.confirmar("Confirma o cancelamento?")) {
            return;
        }
        consulta = clinica.agenda().cancelar(consultaId, entrada.textoOpcional("Motivo"));
        saida.println("Consulta cancelada.");
        if (consulta.getTaxaCancelamento().signum() > 0) {
            saida.println("Cancelamento com menos de 24h de antecedencia: taxa de "
                    + Dinheiro.formatar(consulta.getTaxaCancelamento()) + " (50% do valor).");
        } else {
            saida.println("Cancelamento sem taxa.");
        }
    }

    private void registrarRealizacao() {
        long consultaId = entrada.id("Id da consulta");
        saida.println(clinica.agenda().buscarConsulta(consultaId));
        String diagnostico = entrada.texto("Diagnostico");
        String observacoes = entrada.textoOpcional("Observacoes clinicas");
        Consulta consulta = clinica.agenda().registrarRealizacao(consultaId, diagnostico, observacoes);
        saida.println("Consulta registrada como realizada. Valor a pagar: " + Dinheiro.formatar(consulta.getValor()));
    }

    private void agendaDoMedico() {
        listarMedicos();
        long medicoId = entrada.id("Id do medico");
        LocalDate data = entrada.data("Data");
        imprimirLista("Nenhuma consulta nesta data.", clinica.agenda().consultasDoMedicoNoDia(medicoId, data));
    }

    private void listarConsultas() {
        imprimirLista("Nenhuma consulta cadastrada.", clinica.agenda().listarConsultas());
    }

    // =================================================================== prontuario

    private void menuProntuario() {
        submenu("PRONTUARIO", List.of(
                new Opcao("Emitir prescricao", this::emitirPrescricao),
                new Opcao("Historico do paciente", this::historicoPaciente)));
    }

    private void emitirPrescricao() {
        long consultaId = entrada.id("Id da consulta realizada");
        Consulta consulta = clinica.agenda().buscarConsulta(consultaId);
        saida.println(consulta);
        if (consulta.getStatus() != StatusConsulta.REALIZADA) {
            saida.println("ERRO: Prescricoes so podem ser emitidas para consultas ja realizadas.");
            return;
        }
        List<ItemPrescricao> itens = new ArrayList<>();
        do {
            itens.add(new ItemPrescricao(entrada.texto("Medicamento"), entrada.texto("Posologia"),
                    entrada.textoOpcional("Orientacoes")));
        } while (entrada.confirmar("Adicionar outro medicamento?"));
        String observacoes = entrada.textoOpcional("Observacoes da prescricao");
        Prescricao prescricao = clinica.prontuario().emitirPrescricao(consultaId, itens, observacoes);
        saida.println("Prescricao #" + prescricao.getId() + " emitida.");
    }

    private void historicoPaciente() {
        HistoricoPaciente historico = clinica.prontuario().historicoDoPaciente(escolherPaciente());
        saida.println();
        saida.println("HISTORICO DE " + historico.paciente().getNome().toUpperCase() + " " + historico.paciente());
        saida.println("Consultas: " + historico.consultas().size()
                + " | Realizadas: " + historico.consultasRealizadas().size()
                + " | Prescricoes: " + historico.prescricoes().size());
        if (historico.consultas().isEmpty()) {
            saida.println("Nenhuma consulta registrada.");
        }
        for (Consulta consulta : historico.consultas()) {
            saida.println("-".repeat(60));
            saida.println(consulta);
            if (consulta.getStatus() == StatusConsulta.REALIZADA) {
                saida.println("   Diagnostico: " + consulta.getDiagnostico());
                if (!consulta.getObservacoes().isEmpty()) {
                    saida.println("   Observacoes: " + consulta.getObservacoes());
                }
            }
            if (consulta.getStatus() == StatusConsulta.CANCELADA) {
                saida.println("   Cancelada em " + consulta.getCanceladaEm().format(Entrada.FORMATO_DATA_HORA)
                        + (consulta.getMotivoCancelamento().isEmpty() ? "" : " - " + consulta.getMotivoCancelamento())
                        + " | Taxa: " + Dinheiro.formatar(consulta.getTaxaCancelamento()));
            }
            if (consulta.estaPaga()) {
                Pagamento pagamento = consulta.getPagamento();
                saida.println("   Pagamento: " + Dinheiro.formatar(pagamento.getValor()) + " via "
                        + pagamento.getForma() + " em " + pagamento.getPagoEm().format(Entrada.FORMATO_DATA_HORA));
            }
            for (Prescricao prescricao : consulta.getPrescricoes()) {
                saida.println("   Prescricao #" + prescricao.getId() + " (emitida em "
                        + prescricao.getEmitidaEm().format(Entrada.FORMATO_DATA_HORA) + ")");
                prescricao.getItens().forEach(item -> saida.println("      - " + item));
                if (!prescricao.getObservacoes().isEmpty()) {
                    saida.println("      Obs.: " + prescricao.getObservacoes());
                }
            }
        }
    }

    // =================================================================== faturamento

    private void menuFaturamento() {
        submenu("PAGAMENTOS E FATURAMENTO", List.of(
                new Opcao("Registrar pagamento", this::registrarPagamento),
                new Opcao("Listar pagamentos pendentes", this::listarPendentes),
                new Opcao("Relatorio de faturamento (todo o periodo)",
                        () -> saida.print(clinica.faturamento().gerarRelatorio().formatar())),
                new Opcao("Relatorio de faturamento por periodo", this::relatorioPorPeriodo)));
    }

    private void registrarPagamento() {
        listarPendentes();
        long consultaId = entrada.id("Id da consulta");
        Consulta consulta = clinica.agenda().buscarConsulta(consultaId);
        saida.println("Valor devido: " + Dinheiro.formatar(consulta.getValorCobravel()));
        FormaPagamento forma = entrada.opcao("Forma de pagamento", FormaPagamento.values());
        Pagamento pagamento = clinica.faturamento().registrarPagamento(consultaId, forma);
        saida.println("Pagamento #" + pagamento.getId() + " registrado: " + Dinheiro.formatar(pagamento.getValor()));
    }

    private void listarPendentes() {
        List<Consulta> pendentes = clinica.faturamento().pagamentosPendentes();
        if (pendentes.isEmpty()) {
            saida.println("Nenhum pagamento pendente.");
            return;
        }
        for (Consulta consulta : pendentes) {
            saida.println(consulta + " | Devido: " + Dinheiro.formatar(consulta.getValorCobravel()));
        }
    }

    private void relatorioPorPeriodo() {
        LocalDate inicio = entrada.data("Data inicial");
        LocalDate fim = entrada.data("Data final");
        saida.print(clinica.faturamento().gerarRelatorio(inicio, fim).formatar());
    }

    // =================================================================== auxiliares

    private long escolherPaciente() {
        listarPacientes();
        return entrada.id("Id do paciente");
    }

    private void imprimirLista(String mensagemVazia, List<?> itens) {
        if (itens.isEmpty()) {
            saida.println(mensagemVazia);
            return;
        }
        itens.forEach(saida::println);
    }

    private static String horas(List<LocalDateTime> horarios) {
        return horarios.stream()
                .map(h -> h.toLocalTime().format(Entrada.FORMATO_HORA))
                .collect(Collectors.joining(" "));
    }
}
