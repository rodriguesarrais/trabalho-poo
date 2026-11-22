package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.PacienteConvenio;
import br.com.bemestar.clinica.modelo.PacienteParticular;
import br.com.bemestar.clinica.modelo.PacienteVip;
import br.com.bemestar.clinica.modelo.TipoConsulta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoliticaPrecosTest {

    private final PoliticaPrecos politica = new PoliticaPrecos();
    private final Especialidade cardiologia = new Especialidade(1, "Cardiologia", new BigDecimal("300.00"));

    private final Paciente particular = new PacienteParticular(1, "Joao", "12345678901", "");
    private final Paciente convenio = new PacienteConvenio(2, "Maria", "23456789012", "", "Unimed", "1");
    private final Paciente vip = new PacienteVip(3, "Pedro", "34567890123", "");

    @Test
    void consultaNormalAplicaDescontoDoTipoDePaciente() {
        assertEquals(new BigDecimal("300.00"), politica.calcularValorConsulta(cardiologia, particular, TipoConsulta.NORMAL));
        assertEquals(new BigDecimal("240.00"), politica.calcularValorConsulta(cardiologia, convenio, TipoConsulta.NORMAL));
        assertEquals(new BigDecimal("210.00"), politica.calcularValorConsulta(cardiologia, vip, TipoConsulta.NORMAL));
    }

    @Test
    void encaixeAplicaAcrescimoDe20PorCentoSobreOValorComDesconto() {
        assertEquals(new BigDecimal("360.00"), politica.calcularValorConsulta(cardiologia, particular, TipoConsulta.ENCAIXE));
        assertEquals(new BigDecimal("288.00"), politica.calcularValorConsulta(cardiologia, convenio, TipoConsulta.ENCAIXE));
        assertEquals(new BigDecimal("252.00"), politica.calcularValorConsulta(cardiologia, vip, TipoConsulta.ENCAIXE));
    }
}
