package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.modelo.TipoConsulta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static br.com.bemestar.clinica.servico.Cenario.QUARTA;
import static br.com.bemestar.clinica.servico.Cenario.SEGUNDA;
import static br.com.bemestar.clinica.servico.Cenario.TERCA;
import static br.com.bemestar.clinica.servico.Cenario.em;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaServiceTest {

    private final Cenario c = new Cenario();

    @Test
    void agendaConsultaNormalComValorCalculado() {
        Consulta consulta = c.agenda().agendar(c.convenio.getId(), c.ana.getId(), em(TERCA, 10, 0));

        assertEquals(StatusConsulta.AGENDADA, consulta.getStatus());
        assertEquals(TipoConsulta.NORMAL, consulta.getTipo());
        assertEquals(new BigDecimal("240.00"), consulta.getValor());
        assertEquals(em(TERCA, 10, 30), consulta.getFim());
    }

    @Test
    void naoPermiteDoisAgendamentosNoMesmoHorarioParaOMesmoMedico() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendar(c.vip.getId(), c.ana.getId(), em(TERCA, 10, 0)));
    }

    @Test
    void outroMedicoPodeAtenderNoMesmoHorario() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        Consulta consulta = c.agenda().agendar(c.vip.getId(), c.bruno.getId(), em(TERCA, 10, 0));
        assertEquals(c.bruno, consulta.getMedico());
    }

    @Test
    void rejeitaHorarioForaDoTurnoOuForaDaGradeDe30Minutos() {
        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 13, 0)));
        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 15)));
        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA.plusDays(3), 10, 0)));
    }

    @Test
    void rejeitaAgendamentoNoPassado() {
        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(SEGUNDA, 8, 30)));
    }

    @Test
    void horariosDisponiveisExcluemHorariosPassadosEOcupados() {
        // Turno 08:00-12:00 e agora sao 09:00: restam 09:00, 09:30, ..., 11:30.
        assertEquals(6, c.agenda().horariosDisponiveis(c.ana.getId(), SEGUNDA).size());

        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(SEGUNDA, 10, 0));

        List<LocalDateTime> livres = c.agenda().horariosDisponiveis(c.ana.getId(), SEGUNDA);
        assertEquals(5, livres.size());
        assertFalse(livres.contains(em(SEGUNDA, 10, 0)));
        assertEquals(8, c.agenda().horariosDisponiveis(c.ana.getId(), TERCA).size());
    }

    @Test
    void agendamentoPorEspecialidadeEscolheUmMedicoLivre() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        Consulta consulta = c.agenda().agendarPorEspecialidade(c.vip.getId(), c.cardiologia.getId(), em(TERCA, 10, 0));

        assertEquals(c.bruno, consulta.getMedico());
    }

    @Test
    void agendamentoPorEspecialidadeFalhaQuandoNinguemEstaLivre() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));
        c.agenda().agendar(c.convenio.getId(), c.bruno.getId(), em(TERCA, 10, 0));

        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendarPorEspecialidade(c.vip.getId(), c.cardiologia.getId(), em(TERCA, 10, 0)));
    }

    @Test
    void pacienteNaoPodeTerConsultasSobrepostas() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendar(c.particular.getId(), c.bruno.getId(), em(TERCA, 10, 0)));
    }

    @Test
    void encaixePodeOcuparHorarioJaAgendadoComAcrescimoDe20PorCento() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        Consulta encaixe = c.agenda().agendarEncaixe(c.vip.getId(), c.ana.getId(), em(TERCA, 10, 10));

        assertEquals(TipoConsulta.ENCAIXE, encaixe.getTipo());
        assertEquals(new BigDecimal("252.00"), encaixe.getValor());
    }

    @Test
    void limiteDeDoisEncaixesPorMedicoPorDia() {
        c.agenda().agendarEncaixe(c.particular.getId(), c.ana.getId(), em(TERCA, 9, 0));
        c.agenda().agendarEncaixe(c.convenio.getId(), c.ana.getId(), em(TERCA, 10, 0));
        assertEquals(0, c.agenda().encaixesRestantes(c.ana.getId(), TERCA));

        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendarEncaixe(c.vip.getId(), c.ana.getId(), em(TERCA, 11, 0)));
        // O limite e por medico e por dia.
        c.agenda().agendarEncaixe(c.vip.getId(), c.bruno.getId(), em(TERCA, 11, 0));
        c.agenda().agendarEncaixe(c.vip.getId(), c.ana.getId(), em(QUARTA, 11, 0));
    }

    @Test
    void encaixeCanceladoLiberaVagaDeEncaixe() {
        Consulta primeiro = c.agenda().agendarEncaixe(c.particular.getId(), c.ana.getId(), em(TERCA, 9, 0));
        c.agenda().agendarEncaixe(c.convenio.getId(), c.ana.getId(), em(TERCA, 10, 0));

        c.agenda().cancelar(primeiro.getId(), "Paciente melhorou");

        assertEquals(1, c.agenda().encaixesRestantes(c.ana.getId(), TERCA));
    }

    @Test
    void encaixeDeveFicarDentroDoTurnoESemSobreporOutroEncaixe() {
        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendarEncaixe(c.particular.getId(), c.ana.getId(), em(TERCA, 12, 0)));

        c.agenda().agendarEncaixe(c.particular.getId(), c.ana.getId(), em(TERCA, 9, 0));
        assertThrows(RegraNegocioException.class,
                () -> c.agenda().agendarEncaixe(c.vip.getId(), c.ana.getId(), em(TERCA, 9, 15)));
    }

    @Test
    void cancelamentoComMenosDe24hCobraTaxaDe50PorCento() {
        Consulta consulta = c.agenda().agendar(c.convenio.getId(), c.ana.getId(), em(SEGUNDA, 11, 0));

        c.agenda().cancelar(consulta.getId(), "Imprevisto");

        assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
        assertEquals(new BigDecimal("120.00"), consulta.getTaxaCancelamento());
        assertEquals(new BigDecimal("120.00"), consulta.getValorCobravel());
    }

    @Test
    void cancelamentoComAntecedenciaDe24hOuMaisNaoCobraTaxa() {
        Consulta exatamente24h = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 9, 0));
        Consulta quarta = c.agenda().agendar(c.vip.getId(), c.ana.getId(), em(QUARTA, 10, 0));

        c.agenda().cancelar(exatamente24h.getId(), "");
        c.agenda().cancelar(quarta.getId(), "");

        assertEquals(0, exatamente24h.getTaxaCancelamento().signum());
        assertEquals(0, quarta.getTaxaCancelamento().signum());
    }

    @Test
    void cancelamentoLiberaOHorario() {
        Consulta consulta = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));
        c.agenda().cancelar(consulta.getId(), "");

        assertTrue(c.agenda().horariosDisponiveis(c.ana.getId(), TERCA).contains(em(TERCA, 10, 0)));
        c.agenda().agendar(c.vip.getId(), c.ana.getId(), em(TERCA, 10, 0));
    }

    @Test
    void naoPermiteCancelarConsultaJaRealizadaOuCancelada() {
        Consulta realizada = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));
        c.agenda().registrarRealizacao(realizada.getId(), "Hipertensao", "");
        Consulta cancelada = c.agenda().agendar(c.vip.getId(), c.ana.getId(), em(TERCA, 11, 0));
        c.agenda().cancelar(cancelada.getId(), "");

        assertThrows(RegraNegocioException.class, () -> c.agenda().cancelar(realizada.getId(), ""));
        assertThrows(RegraNegocioException.class, () -> c.agenda().cancelar(cancelada.getId(), ""));
    }
}
