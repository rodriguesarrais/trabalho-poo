package br.com.bemestar.clinica;

import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.repositorio.Repositorio;
import br.com.bemestar.clinica.repositorio.RepositorioEmMemoria;
import br.com.bemestar.clinica.servico.AgendaService;
import br.com.bemestar.clinica.servico.CadastroService;
import br.com.bemestar.clinica.servico.FaturamentoService;
import br.com.bemestar.clinica.servico.PoliticaPrecos;
import br.com.bemestar.clinica.servico.ProntuarioService;

import java.time.Clock;

/**
 * Ponto de montagem do sistema: cria os repositorios e conecta os servicos.
 * O relogio e injetado para permitir testes com datas fixas.
 */
public class Clinica {

    private final CadastroService cadastro;
    private final AgendaService agenda;
    private final ProntuarioService prontuario;
    private final FaturamentoService faturamento;

    public Clinica() {
        this(Clock.systemDefaultZone());
    }

    public Clinica(Clock relogio) {
        Repositorio<Consulta> consultas = new RepositorioEmMemoria<>();
        this.cadastro = new CadastroService(new RepositorioEmMemoria<>(), new RepositorioEmMemoria<>(),
                new RepositorioEmMemoria<>());
        this.agenda = new AgendaService(cadastro, consultas, new PoliticaPrecos(), relogio);
        this.prontuario = new ProntuarioService(cadastro, consultas, new RepositorioEmMemoria<>(), relogio);
        this.faturamento = new FaturamentoService(consultas, new RepositorioEmMemoria<>(), relogio);
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
