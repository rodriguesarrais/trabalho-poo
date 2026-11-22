package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.Clinica;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Cenario comum dos testes. O relogio fica fixo em segunda-feira,
 * 02/03/2026 as 09:00, para que as regras de tempo sejam deterministicas.
 */
class Cenario {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 2, 9, 0);
    static final LocalDate SEGUNDA = AGORA.toLocalDate();
    static final LocalDate TERCA = SEGUNDA.plusDays(1);
    static final LocalDate QUARTA = SEGUNDA.plusDays(2);

    final Clinica clinica = new Clinica(Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    final Especialidade cardiologia;
    final Medico ana;
    final Medico bruno;
    final Paciente particular;
    final Paciente convenio;
    final Paciente vip;

    Cenario() {
        CadastroService cadastro = clinica.cadastro();
        cardiologia = cadastro.cadastrarEspecialidade("Cardiologia", new BigDecimal("300.00"));
        ana = cadastro.cadastrarMedico("Ana Souza", "11111111111", "", "CRM 1", cardiologia.getId());
        bruno = cadastro.cadastrarMedico("Bruno Lima", "22222222222", "", "CRM 2", cardiologia.getId());
        for (DayOfWeek dia : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY}) {
            cadastro.definirHorarioMedico(ana.getId(), dia, LocalTime.of(8, 0), LocalTime.of(12, 0));
            cadastro.definirHorarioMedico(bruno.getId(), dia, LocalTime.of(8, 0), LocalTime.of(12, 0));
        }
        particular = cadastro.cadastrarPacienteParticular("Joao Silva", "12345678901", "");
        convenio = cadastro.cadastrarPacienteConvenio("Maria Oliveira", "23456789012", "", "Unimed", "123");
        vip = cadastro.cadastrarPacienteVip("Pedro Santos", "34567890123", "");
    }

    AgendaService agenda() {
        return clinica.agenda();
    }

    static LocalDateTime em(LocalDate data, int hora, int minuto) {
        return data.atTime(hora, minuto);
    }
}
