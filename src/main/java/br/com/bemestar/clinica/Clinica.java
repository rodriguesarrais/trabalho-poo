package br.com.bemestar.clinica;

import br.com.bemestar.clinica.repositorio.BaseDeDados;
import br.com.bemestar.clinica.servico.AgendaService;
import br.com.bemestar.clinica.servico.CadastroService;
import br.com.bemestar.clinica.servico.FaturamentoService;
import br.com.bemestar.clinica.servico.PoliticaPrecos;
import br.com.bemestar.clinica.servico.ProntuarioService;

import java.time.Clock;

/**
 * Ponto de montagem do sistema: recebe a base de dados e conecta os servicos.
 * O relogio e injetado para permitir testes com datas fixas.
 */
public class Clinica {

    private final BaseDeDados base;
    private final CadastroService cadastro;
    private final AgendaService agenda;
    private final ProntuarioService prontuario;
    private final FaturamentoService faturamento;

    public Clinica() {
        this(Clock.systemDefaultZone());
    }

    public Clinica(Clock relogio) {
        this(relogio, new BaseDeDados());
    }

    public Clinica(Clock relogio, BaseDeDados base) {
        this.base = base;
        this.cadastro = new CadastroService(base.especialidades(), base.medicos(), base.pacientes(), base.consultas());
        this.agenda = new AgendaService(cadastro, base.consultas(), new PoliticaPrecos(), relogio);
        this.prontuario = new ProntuarioService(cadastro, base.consultas(), base.prescricoes(), relogio);
        this.faturamento = new FaturamentoService(base.consultas(), base.pagamentos(), relogio);
    }

    public BaseDeDados base() {
        return base;
    }

    public CadastroService cadastro() {
        return cadastro;
    }

    public AgendaService agenda() {
        return agenda;
    }

    public ProntuarioService prontuario() {
        return prontuario;
    }

    public FaturamentoService faturamento() {
        return faturamento;
    }
}
