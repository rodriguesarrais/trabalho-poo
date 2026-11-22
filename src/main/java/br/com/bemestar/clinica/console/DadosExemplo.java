package br.com.bemestar.clinica.console;

import br.com.bemestar.clinica.Clinica;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.servico.CadastroService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Carrega dados de exemplo para facilitar a demonstracao do sistema.
 */
public final class DadosExemplo {

    private static final Set<DayOfWeek> DIAS_UTEIS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    private DadosExemplo() {
    }

    public static void carregar(Clinica clinica) {
        CadastroService cadastro = clinica.cadastro();

        Especialidade cardiologia = cadastro.cadastrarEspecialidade("Cardiologia", new BigDecimal("300.00"));
        Especialidade dermatologia = cadastro.cadastrarEspecialidade("Dermatologia", new BigDecimal("200.00"));
        Especialidade pediatria = cadastro.cadastrarEspecialidade("Pediatria", new BigDecimal("180.00"));
        Especialidade ortopedia = cadastro.cadastrarEspecialidade("Ortopedia", new BigDecimal("250.00"));

        Medico ana = cadastro.cadastrarMedico("Ana Souza", "111.111.111-11", "(11) 90000-0001",
                "CRM-SP 100001", cardiologia.getId());
        Medico bruno = cadastro.cadastrarMedico("Bruno Lima", "222.222.222-22", "(11) 90000-0002",
                "CRM-SP 100002", cardiologia.getId());
        Medico carla = cadastro.cadastrarMedico("Carla Mendes", "333.333.333-33", "(11) 90000-0003",
                "CRM-SP 100003", dermatologia.getId());
        Medico daniel = cadastro.cadastrarMedico("Daniel Rocha", "444.444.444-44", "(11) 90000-0004",
                "CRM-SP 100004", pediatria.getId());
        Medico elisa = cadastro.cadastrarMedico("Elisa Prado", "555.555.555-55", "(11) 90000-0005",
                "CRM-SP 100005", ortopedia.getId());

        definirTurno(cadastro, ana, DIAS_UTEIS, LocalTime.of(8, 0), LocalTime.of(12, 0));
        definirTurno(cadastro, bruno, DIAS_UTEIS, LocalTime.of(13, 0), LocalTime.of(18, 0));
        definirTurno(cadastro, carla, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0), LocalTime.of(17, 0));
        definirTurno(cadastro, daniel, EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                LocalTime.of(8, 0), LocalTime.of(16, 0));
        definirTurno(cadastro, elisa, EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY),
                LocalTime.of(8, 0), LocalTime.of(12, 0));

        Paciente joao = cadastro.cadastrarPacienteParticular("Joao Silva", "123.456.789-01", "(11) 98888-0001");
        Paciente maria = cadastro.cadastrarPacienteConvenio("Maria Oliveira", "234.567.890-12", "(11) 98888-0002",
                "Unimed", "0012345");
        Paciente pedro = cadastro.cadastrarPacienteVip("Pedro Santos", "345.678.901-23", "(11) 98888-0003");
        cadastro.cadastrarPacienteConvenio("Lucia Ferreira", "456.789.012-34", "(11) 98888-0004",
                "Amil", "0098765");

        // Algumas consultas futuras, nos primeiros horarios livres de cada medico.
        agendarNoProximoHorarioLivre(clinica, joao, ana);
        agendarNoProximoHorarioLivre(clinica, maria, carla);
        agendarNoProximoHorarioLivre(clinica, pedro, elisa);
    }

    private static void definirTurno(CadastroService cadastro, Medico medico, Set<DayOfWeek> dias,
                                     LocalTime inicio, LocalTime fim) {
        for (DayOfWeek dia : dias) {
            cadastro.definirHorarioMedico(medico.getId(), dia, inicio, fim);
        }
    }

    private static void agendarNoProximoHorarioLivre(Clinica clinica, Paciente paciente, Medico medico) {
        LocalDate data = LocalDate.now().plusDays(1);
        for (int i = 0; i < 14; i++, data = data.plusDays(1)) {
            List<LocalDateTime> livres = clinica.agenda().horariosDisponiveis(medico.getId(), data);
            if (!livres.isEmpty()) {
                clinica.agenda().agendar(paciente.getId(), medico.getId(), livres.get(0));
                return;
            }
        }
    }
}
