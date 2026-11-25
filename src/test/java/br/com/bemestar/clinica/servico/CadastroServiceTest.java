package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.excecao.EntidadeNaoEncontradaException;
import br.com.bemestar.clinica.excecao.RegraNegocioException;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Medico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.com.bemestar.clinica.servico.Cenario.TERCA;
import static br.com.bemestar.clinica.servico.Cenario.em;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CadastroServiceTest {

    private final Cenario c = new Cenario();
    private final CadastroService cadastro = c.clinica.cadastro();

    @Test
    void naoPermiteCpfOuCrmDuplicadoNemEspecialidadeRepetida() {
        assertThrows(RegraNegocioException.class,
                () -> cadastro.cadastrarPacienteVip("Outro", "123.456.789-01", ""));
        assertThrows(RegraNegocioException.class,
                () -> cadastro.cadastrarMedico("Outro", "99999999999", "", "crm 1", c.cardiologia.getId()));
        assertThrows(RegraNegocioException.class,
                () -> cadastro.cadastrarEspecialidade("cardiologia", BigDecimal.TEN));
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertThrows(RegraNegocioException.class, () -> cadastro.cadastrarPacienteParticular("", "98765432100", ""));
        assertThrows(RegraNegocioException.class, () -> cadastro.cadastrarPacienteParticular("Ana", "123", ""));
        assertThrows(RegraNegocioException.class, () -> cadastro.cadastrarEspecialidade("Neuro", BigDecimal.ZERO));
        assertThrows(EntidadeNaoEncontradaException.class, () -> cadastro.buscarPaciente(999));
    }

    @Test
    void edicaoMantemDadosNaoInformados() {
        cadastro.atualizarDadosPaciente(c.vip.getId(), "Pedro S. Santos", "");
        assertEquals("Pedro S. Santos", c.vip.getNome());
        assertEquals("", c.vip.getTelefone());

        Especialidade dermatologia = cadastro.cadastrarEspecialidade("Dermatologia", new BigDecimal("200"));
        Medico medico = cadastro.alterarEspecialidadeMedico(c.bruno.getId(), dermatologia.getId());
        assertEquals(dermatologia, medico.getEspecialidade());
    }

    @Test
    void remocaoBloqueadaQuandoHaHistoricoOuVinculos() {
        c.agenda().agendar(c.particular.getId(), c.ana.getId(), em(TERCA, 10, 0));

        assertThrows(RegraNegocioException.class, () -> cadastro.removerPaciente(c.particular.getId()));
        assertThrows(RegraNegocioException.class, () -> cadastro.removerMedico(c.ana.getId()));
        assertThrows(RegraNegocioException.class, () -> cadastro.removerEspecialidade(c.cardiologia.getId()));

        cadastro.removerPaciente(c.vip.getId());
        cadastro.removerMedico(c.bruno.getId());
        assertThrows(EntidadeNaoEncontradaException.class, () -> cadastro.buscarPaciente(c.vip.getId()));
        assertEquals(1, cadastro.listarMedicos().size());
    }
}
