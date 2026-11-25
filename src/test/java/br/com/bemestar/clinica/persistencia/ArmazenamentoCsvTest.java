package br.com.bemestar.clinica.persistencia;

import br.com.bemestar.clinica.Clinica;
import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.FormaPagamento;
import br.com.bemestar.clinica.modelo.ItemPrescricao;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.PacienteConvenio;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.modelo.TipoPaciente;
import br.com.bemestar.clinica.repositorio.BaseDeDados;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmazenamentoCsvTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 2, 9, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    Path pasta;

    @Test
    void pastaInexistenteResultaEmBaseVazia() {
        BaseDeDados base = new ArmazenamentoCsv(pasta.resolve("nao-existe")).carregar();
        assertTrue(base.estaVazia());
    }

    @Test
    void salvaECarregaTodosOsDadosPreservandoEstadoERegras() {
        Clinica original = new Clinica(RELOGIO);
        var cad = original.cadastro();
        var esp = cad.cadastrarEspecialidade("Cardiologia; adulto", new BigDecimal("300.00"));
        Medico ana = cad.cadastrarMedico("Ana \"Doutora\" Souza", "11111111111", "(86) 9999", "CRM 1", esp.getId());
        cad.definirHorarioMedico(ana.getId(), DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        cad.definirHorarioMedico(ana.getId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        Paciente maria = cad.cadastrarPacienteConvenio("Maria", "23456789012", "", "Unimed", "0012");
        Paciente joao = cad.cadastrarPacienteParticular("Joao", "12345678901", "");

        LocalDateTime terca10 = AGORA.plusDays(1).withHour(10);
        Consulta realizada = original.agenda().agendar(maria.getId(), ana.getId(), terca10);
        original.agenda().registrarRealizacao(realizada.getId(), "Arritmia\nleve", "PA 140x90");
        original.prontuario().emitirPrescricao(realizada.getId(), List.of(
                new ItemPrescricao("Losartana; 50mg", "1x ao dia", ""),
                new ItemPrescricao("AAS", "1x ao dia", "apos o almoco")), "Retorno em 30 dias");
        original.faturamento().registrarPagamento(realizada.getId(), FormaPagamento.CONVENIO);
        Consulta cancelada = original.agenda().agendar(joao.getId(), ana.getId(), AGORA.withHour(11));
        original.agenda().cancelar(cancelada.getId(), "Imprevisto");
        Consulta encaixe = original.agenda().agendarEncaixe(joao.getId(), ana.getId(), terca10.plusMinutes(10));

        ArmazenamentoCsv armazenamento = new ArmazenamentoCsv(pasta);
        armazenamento.salvar(original.base());
        Clinica recarregada = new Clinica(RELOGIO, armazenamento.carregar());

        Medico anaLida = recarregada.cadastro().buscarMedico(ana.getId());
        assertEquals("Ana \"Doutora\" Souza", anaLida.getNome());
        assertEquals("Cardiologia; adulto", anaLida.getEspecialidade().getNome());
        assertEquals(2, anaLida.getAgendaSemanal().size());
        assertTrue(recarregada.cadastro().buscarPaciente(maria.getId()) instanceof PacienteConvenio);

        Consulta realizadaLida = recarregada.agenda().buscarConsulta(realizada.getId());
        assertEquals(StatusConsulta.REALIZADA, realizadaLida.getStatus());
        assertEquals("Arritmia\nleve", realizadaLida.getDiagnostico());
        assertEquals(2, realizadaLida.getPrescricoes().get(0).getItens().size());
        assertEquals("Losartana; 50mg", realizadaLida.getPrescricoes().get(0).getItens().get(0).medicamento());
        assertTrue(realizadaLida.estaPaga());

        Consulta canceladaLida = recarregada.agenda().buscarConsulta(cancelada.getId());
        assertEquals(StatusConsulta.CANCELADA, canceladaLida.getStatus());
        assertEquals(new BigDecimal("150.00"), canceladaLida.getTaxaCancelamento());
        assertEquals(encaixe.getValor(), recarregada.agenda().buscarConsulta(encaixe.getId()).getValor());

        var relatorio = recarregada.faturamento().gerarRelatorio();
        assertEquals(new BigDecimal("240.00"), relatorio.linha(TipoPaciente.CONVENIO).total());

        // Regras continuam valendo e ids nao sao reutilizados apos o carregamento.
        assertEquals(1, recarregada.agenda().encaixesRestantes(ana.getId(), terca10.toLocalDate()));
        Paciente novo = recarregada.cadastro().cadastrarPacienteVip("Pedro", "34567890123", "");
        assertEquals(3, novo.getId());
    }

    @Test
    void arquivoCorrompidoGeraErroComArquivoELinha() throws Exception {
        Files.writeString(pasta.resolve(ArmazenamentoCsv.ESPECIALIDADES),
                "id;nome;valorBase\n1;Cardiologia;300.00\n2;Dermatologia;abc\n");

        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> new ArmazenamentoCsv(pasta).carregar());

        assertTrue(erro.getMessage().contains(ArmazenamentoCsv.ESPECIALIDADES));
        assertTrue(erro.getMessage().contains("linha 3"));
    }

    @Test
    void referenciaInexistenteEColunasErradasSaoDetectadas() throws Exception {
        Files.writeString(pasta.resolve(ArmazenamentoCsv.MEDICOS),
                "id;nome;cpf;telefone;crm;especialidadeId\n1;Ana;11111111111;;CRM 1;7\n");
        assertThrows(PersistenciaException.class, () -> new ArmazenamentoCsv(pasta).carregar());

        Files.delete(pasta.resolve(ArmazenamentoCsv.MEDICOS));
        Files.writeString(pasta.resolve(ArmazenamentoCsv.ESPECIALIDADES), "id;nome;valorBase\n1;Cardiologia\n");
        assertThrows(PersistenciaException.class, () -> new ArmazenamentoCsv(pasta).carregar());
    }

    @Test
    void arquivarMoveOsArquivosParaBackup() {
        Clinica clinica = new Clinica(RELOGIO);
        clinica.cadastro().cadastrarEspecialidade("Cardiologia", new BigDecimal("300"));
        ArmazenamentoCsv armazenamento = new ArmazenamentoCsv(pasta);
        armazenamento.salvar(clinica.base());

        Path backup = Path.of(armazenamento.arquivarDadosAtuais());

        assertFalse(Files.exists(pasta.resolve(ArmazenamentoCsv.ESPECIALIDADES)));
        assertTrue(Files.exists(backup.resolve(ArmazenamentoCsv.ESPECIALIDADES)));
        assertTrue(armazenamento.carregar().estaVazia());
    }
}
