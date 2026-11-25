package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.FormaPagamento;
import br.com.bemestar.clinica.modelo.Pagamento;
import br.com.bemestar.clinica.modelo.TipoPaciente;
import br.com.bemestar.clinica.relatorio.RelatorioFaturamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static br.com.bemestar.clinica.servico.Cenario.QUARTA;
import static br.com.bemestar.clinica.servico.Cenario.SEGUNDA;
import static br.com.bemestar.clinica.servico.Cenario.TERCA;
import static br.com.bemestar.clinica.servico.Cenario.em;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaturamentoServiceTest {

    private final Cenario c = new Cenario();

    private Consulta realizada(long pacienteId, long medicoId, java.time.LocalDateTime inicio) {
        Consulta consulta = c.agenda().agendar(pacienteId, medicoId, inicio);
        c.agenda().registrarRealizacao(consulta.getId(), "Diagnostico", "");
        return consulta;
    }

    @Test
    void pagamentoSoPodeSerRegistradoAposARealizacao() {
        Consulta consulta = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        assertThrows(RegraNegocioException.class,
                () -> c.clinica.faturamento().registrarPagamento(consulta.getId(), FormaPagamento.PIX));

        c.agenda().registrarRealizacao(consulta.getId(), "Check-up", "");
        Pagamento pagamento = c.clinica.faturamento().registrarPagamento(consulta.getId(), FormaPagamento.PIX);

        assertEquals(new BigDecimal("300.00"), pagamento.getValor());
        assertTrue(consulta.estaPaga());
    }

    @Test
    void naoPermitePagarDuasVezes() {
        Consulta consulta = realizada(c.vip.getId(), c.ana.getId(), em(TERCA, 10, 0));
        c.clinica.faturamento().registrarPagamento(consulta.getId(), FormaPagamento.DINHEIRO);

        assertThrows(RegraNegocioException.class,
                () -> c.clinica.faturamento().registrarPagamento(consulta.getId(), FormaPagamento.DINHEIRO));
    }

    @Test
    void faturamentoAoConvenioSoParaPacienteDeConvenio() {
        Consulta particular = realizada(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));
        Consulta convenio = realizada(c.convenio.getId(), c.ana.getId(), em(TERCA, 11, 0));

        assertThrows(RegraNegocioException.class,
                () -> c.clinica.faturamento().registrarPagamento(particular.getId(), FormaPagamento.CONVENIO));
        c.clinica.faturamento().registrarPagamento(convenio.getId(), FormaPagamento.CONVENIO);
        assertTrue(convenio.estaPaga());
    }

    @Test
    void cancelamentoComTaxaGeraCobrancaESemTaxaNao() {
        Consulta comTaxa = c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(SEGUNDA, 11, 0));
        Consulta semTaxa = c.agenda().agendar(c.vip.getId(), c.ana.getId(), em(QUARTA, 10, 0));
        c.agenda().cancelar(comTaxa.getId(), "");
        c.agenda().cancelar(semTaxa.getId(), "");

        assertEquals(List.of(comTaxa), c.clinica.faturamento().pagamentosPendentes());
        Pagamento taxa = c.clinica.faturamento().registrarPagamento(comTaxa.getId(), FormaPagamento.CARTAO_CREDITO);
        assertEquals(new BigDecimal("150.00"), taxa.getValor());
        assertThrows(RegraNegocioException.class,
                () -> c.clinica.faturamento().registrarPagamento(semTaxa.getId(), FormaPagamento.PIX));
    }

    @Test
    void relatorioSeparaFaturamentoPorTipoDePaciente() {
        // Particular: 300 (normal) + 360 (encaixe)
        c.clinica.faturamento().registrarPagamento(
                realizada(c.particular.getId(), c.ana.getId(), em(TERCA, 9, 0)).getId(), FormaPagamento.PIX);
        Consulta encaixe = c.agenda().agendarEncaixe(c.particular.getId(), c.bruno.getId(), em(QUARTA, 9, 10));
        c.agenda().registrarRealizacao(encaixe.getId(), "Dor no peito", "");
        c.clinica.faturamento().registrarPagamento(encaixe.getId(), FormaPagamento.PIX);
        // Convenio: 240
        c.clinica.faturamento().registrarPagamento(
                realizada(c.convenio.getId(), c.ana.getId(), em(TERCA, 10, 0)).getId(), FormaPagamento.CONVENIO);
        // VIP: consulta realizada mas nao paga nao entra no faturamento
        realizada(c.vip.getId(), c.ana.getId(), em(TERCA, 11, 0));

        RelatorioFaturamento relatorio = c.clinica.faturamento().gerarRelatorio();

        assertEquals(2, relatorio.linha(TipoPaciente.PARTICULAR).quantidadePagamentos());
        assertEquals(new BigDecimal("660.00"), relatorio.linha(TipoPaciente.PARTICULAR).total());
        assertEquals(new BigDecimal("240.00"), relatorio.linha(TipoPaciente.CONVENIO).total());
        assertEquals(0, relatorio.linha(TipoPaciente.VIP).quantidadePagamentos());
        assertEquals(new BigDecimal("900.00"), relatorio.totalGeral());
        assertEquals(3, relatorio.quantidadeTotal());

        assertEquals(new BigDecimal("900.00"),
                c.clinica.faturamento().gerarRelatorio(SEGUNDA, SEGUNDA).totalGeral());
        assertEquals(0, c.clinica.faturamento().gerarRelatorio(TERCA, QUARTA).totalGeral().signum());
    }
}
