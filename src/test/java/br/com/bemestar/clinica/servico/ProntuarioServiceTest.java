package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.ItemPrescricao;
import br.com.bemestar.clinica.modelo.Prescricao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.bemestar.clinica.servico.Cenario.QUARTA;
import static br.com.bemestar.clinica.servico.Cenario.TERCA;
import static br.com.bemestar.clinica.servico.Cenario.em;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProntuarioServiceTest {

    private final Cenario c = new Cenario();
    private final List<ItemPrescricao> itens = List.of(
            new ItemPrescricao("Losartana 50mg", "1 comprimido a cada 12h", "Por 30 dias"));

    @Test
    void prescricaoSoPodeSerEmitidaParaConsultaRealizada() {
        Consulta consulta = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        assertThrows(RegraNegocioException.class,
                () -> c.clinica.prontuario().emitirPrescricao(consulta.getId(), itens, ""));

        c.agenda().registrarRealizacao(consulta.getId(), "Hipertensao arterial", "PA 150x95");
        Prescricao prescricao = c.clinica.prontuario().emitirPrescricao(consulta.getId(), itens, "Retorno em 30 dias");

        assertEquals(consulta, prescricao.getConsulta());
        assertEquals(List.of(prescricao), consulta.getPrescricoes());
    }

    @Test
    void prescricaoExigePeloMenosUmMedicamento() {
        Consulta consulta = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));
        c.agenda().registrarRealizacao(consulta.getId(), "Check-up", "");

        assertThrows(RegraNegocioException.class,
                () -> c.clinica.prontuario().emitirPrescricao(consulta.getId(), List.of(), ""));
    }

    @Test
    void historicoTrazConsultasEmOrdemComDiagnosticosEPrescricoes() {
        Consulta segunda = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(QUARTA, 9, 0));
        Consulta primeira = c.agenda().agendar(c.particular.getId(), c.bruno.getId(), em(TERCA, 9, 0));
        Consulta cancelada = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(QUARTA, 11, 0));
        c.agenda().agendar(c.vip.getId(), c.ana.getId(), em(TERCA, 10, 0)); // outro paciente

        c.agenda().registrarRealizacao(primeira.getId(), "Arritmia", "");
        c.clinica.prontuario().emitirPrescricao(primeira.getId(), itens, "");
        c.agenda().cancelar(cancelada.getId(), "Viagem");

        HistoricoPaciente historico = c.clinica.prontuario().historicoDoPaciente(c.particular.getId());

        assertEquals(List.of(primeira, segunda, cancelada), historico.consultas());
        assertEquals(List.of(primeira), historico.consultasRealizadas());
        assertEquals("Arritmia", historico.consultasRealizadas().get(0).getDiagnostico());
        assertEquals(1, historico.prescricoes().size());
    }
}
