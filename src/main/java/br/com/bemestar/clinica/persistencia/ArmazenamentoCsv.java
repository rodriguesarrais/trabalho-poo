package br.com.bemestar.clinica.persistencia;

import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.FormaPagamento;
import br.com.bemestar.clinica.modelo.HorarioAtendimento;
import br.com.bemestar.clinica.modelo.Identificavel;
import br.com.bemestar.clinica.modelo.ItemPrescricao;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.PacienteConvenio;
import br.com.bemestar.clinica.modelo.PacienteParticular;
import br.com.bemestar.clinica.modelo.PacienteVip;
import br.com.bemestar.clinica.modelo.Pagamento;
import br.com.bemestar.clinica.modelo.Prescricao;
import br.com.bemestar.clinica.modelo.StatusConsulta;
import br.com.bemestar.clinica.modelo.TipoConsulta;
import br.com.bemestar.clinica.modelo.TipoPaciente;
import br.com.bemestar.clinica.repositorio.BaseDeDados;
import br.com.bemestar.clinica.repositorio.Repositorio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Persistencia em arquivos CSV (um arquivo por entidade) dentro de uma pasta.
 *
 * <p>Na leitura, as entidades sao reconstruidas pelos proprios construtores e
 * metodos do dominio (por exemplo, {@code Consulta.cancelar}), de modo que
 * as regras de negocio validam tambem os dados vindos do disco. Arquivos
 * ausentes sao tratados como vazios; arquivos invalidos geram uma
 * {@link PersistenciaException} indicando arquivo e linha.</p>
 *
 * <p>Na gravacao, cada arquivo e escrito primeiro em um arquivo temporario e
 * depois movido para o lugar definitivo, evitando arquivos pela metade.</p>
 */
public class ArmazenamentoCsv implements Armazenamento {

    static final String ESPECIALIDADES = "especialidades.csv";
    static final String MEDICOS = "medicos.csv";
    static final String HORARIOS = "horarios.csv";
    static final String PACIENTES = "pacientes.csv";
    static final String CONSULTAS = "consultas.csv";
    static final String PRESCRICOES = "prescricoes.csv";
    static final String ITENS_PRESCRICAO = "itens_prescricao.csv";
    static final String PAGAMENTOS = "pagamentos.csv";

    private static final List<String> ARQUIVOS = List.of(ESPECIALIDADES, MEDICOS, HORARIOS, PACIENTES,
            CONSULTAS, PRESCRICOES, ITENS_PRESCRICAO, PAGAMENTOS);

    private final Path pasta;

    public ArmazenamentoCsv(Path pasta) {
        this.pasta = pasta;
    }

    @Override
    public String local() {
        return pasta.toAbsolutePath().normalize().toString();
    }

    // =================================================================== leitura

    @Override
    public BaseDeDados carregar() {
        BaseDeDados base = new BaseDeDados();
        if (!Files.isDirectory(pasta)) {
            return base;
        }

        ler(ESPECIALIDADES, 3, r -> base.especialidades().salvar(
                new Especialidade(r.longo(0), r.texto(1), r.decimal(2))));

        ler(MEDICOS, 6, r -> base.medicos().salvar(new Medico(r.longo(0), r.texto(1), r.texto(2), r.texto(3),
                r.texto(4), buscar(base.especialidades(), r.longo(5), "especialidade", r))));

        ler(HORARIOS, 4, r -> buscar(base.medicos(), r.longo(0), "medico", r).definirHorario(
                new HorarioAtendimento(r.diaDaSemana(1), r.hora(2), r.hora(3))));

        ler(PACIENTES, 7, r -> base.pacientes().salvar(criarPaciente(r)));

        ler(CONSULTAS, 12, r -> base.consultas().salvar(criarConsulta(r, base)));

        Map<Long, List<ItemPrescricao>> itensPorPrescricao = new HashMap<>();
        ler(ITENS_PRESCRICAO, 4, r -> itensPorPrescricao.computeIfAbsent(r.longo(0), id -> new ArrayList<>())
                .add(new ItemPrescricao(r.texto(1), r.texto(2), r.texto(3))));

        ler(PRESCRICOES, 4, r -> {
            Consulta consulta = buscar(base.consultas(), r.longo(1), "consulta", r);
            Prescricao prescricao = new Prescricao(r.longo(0), consulta, r.dataHora(2),
                    itensPorPrescricao.getOrDefault(r.longo(0), List.of()), r.texto(3));
            consulta.adicionarPrescricao(prescricao);
            base.prescricoes().salvar(prescricao);
        });

        ler(PAGAMENTOS, 5, r -> {
            Consulta consulta = buscar(base.consultas(), r.longo(1), "consulta", r);
            Pagamento pagamento = new Pagamento(r.longo(0), consulta, r.decimal(2),
                    r.enumeracao(3, FormaPagamento.class), r.dataHora(4));
            consulta.registrarPagamento(pagamento);
            base.pagamentos().salvar(pagamento);
        });

        return base;
    }

    private static Paciente criarPaciente(Registro r) {
        long id = r.longo(0);
        TipoPaciente tipo = r.enumeracao(1, TipoPaciente.class);
        return switch (tipo) {
            case PARTICULAR -> new PacienteParticular(id, r.texto(2), r.texto(3), r.texto(4));
            case CONVENIO -> new PacienteConvenio(id, r.texto(2), r.texto(3), r.texto(4), r.texto(5), r.texto(6));
            case VIP -> new PacienteVip(id, r.texto(2), r.texto(3), r.texto(4));
        };
    }

    private static Consulta criarConsulta(Registro r, BaseDeDados base) {
        Consulta consulta = new Consulta(r.longo(0),
                buscar(base.pacientes(), r.longo(1), "paciente", r),
                buscar(base.medicos(), r.longo(2), "medico", r),
                r.dataHora(3), r.enumeracao(4, TipoConsulta.class), r.decimal(5));
        StatusConsulta status = r.enumeracao(6, StatusConsulta.class);
        switch (status) {
            case REALIZADA -> consulta.registrarRealizacao(r.texto(7), r.texto(8));
            case CANCELADA -> consulta.cancelar(r.dataHora(9), r.texto(10), r.decimal(11));
            case AGENDADA -> {
                // estado inicial; nada a fazer
            }
        }
        return consulta;
    }

    private static <T extends Identificavel> T buscar(Repositorio<T> repositorio, long id, String entidade,
                                                     Registro r) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> r.erro("referencia a " + entidade + " #" + id + " inexistente"));
    }

    /**
     * Le um arquivo (ignorando o cabecalho) e entrega cada linha ao consumidor.
     * Qualquer erro de conversao ou de regra de negocio e convertido em
     * PersistenciaException com o arquivo e a linha do problema.
     */
    private void ler(String nomeArquivo, int colunas, Consumer<Registro> consumidor) {
        Path arquivo = pasta.resolve(nomeArquivo);
        if (!Files.exists(arquivo)) {
            return;
        }
        List<List<String>> linhas;
        try {
            linhas = Csv.ler(Files.readString(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel ler o arquivo " + nomeArquivo + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new PersistenciaException("Arquivo " + nomeArquivo + " invalido: " + e.getMessage(), e);
        }
        for (int i = 1; i < linhas.size(); i++) { // linha 0 = cabecalho
            Registro registro = new Registro(nomeArquivo, i + 1, linhas.get(i));
            if (linhas.get(i).size() != colunas) {
                throw registro.erro("esperadas " + colunas + " colunas, encontradas " + linhas.get(i).size());
            }
            try {
                consumidor.accept(registro);
            } catch (PersistenciaException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new PersistenciaException(registro.local() + ": " + e.getMessage(), e);
            }
        }
    }

    // =================================================================== gravacao

    @Override
    public void salvar(BaseDeDados base) {
        try {
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel criar a pasta de dados " + local() + ": "
                    + e.getMessage(), e);
        }

        StringBuilder especialidades = new StringBuilder(Csv.linha("id", "nome", "valorBase"));
        for (Especialidade e : base.especialidades().listarTodos()) {
            especialidades.append(Csv.linha(e.getId(), e.getNome(), e.getValorBase().toPlainString()));
        }

        StringBuilder medicos = new StringBuilder(Csv.linha("id", "nome", "cpf", "telefone", "crm", "especialidadeId"));
        StringBuilder horarios = new StringBuilder(Csv.linha("medicoId", "dia", "inicio", "fim"));
        for (Medico m : base.medicos().listarTodos()) {
            medicos.append(Csv.linha(m.getId(), m.getNome(), m.getCpf(), m.getTelefone(), m.getCrm(),
                    m.getEspecialidade().getId()));
            for (HorarioAtendimento h : m.getAgendaSemanal()) {
                horarios.append(Csv.linha(m.getId(), h.dia().name(), h.inicio(), h.fim()));
            }
        }

        StringBuilder pacientes = new StringBuilder(Csv.linha("id", "tipo", "nome", "cpf", "telefone",
                "operadora", "carteirinha"));
        for (Paciente p : base.pacientes().listarTodos()) {
            String operadora = p instanceof PacienteConvenio c ? c.getOperadora() : "";
            String carteirinha = p instanceof PacienteConvenio c ? c.getNumeroCarteirinha() : "";
            pacientes.append(Csv.linha(p.getId(), p.getTipo().name(), p.getNome(), p.getCpf(), p.getTelefone(),
                    operadora, carteirinha));
        }

        StringBuilder consultas = new StringBuilder(Csv.linha("id", "pacienteId", "medicoId", "inicio", "tipo",
                "valor", "status", "diagnostico", "observacoes", "canceladaEm", "motivoCancelamento",
                "taxaCancelamento"));
        for (Consulta c : base.consultas().listarTodos()) {
            consultas.append(Csv.linha(c.getId(), c.getPaciente().getId(), c.getMedico().getId(), c.getInicio(),
                    c.getTipo().name(), c.getValor().toPlainString(), c.getStatus().name(), c.getDiagnostico(),
                    c.getObservacoes(), c.getCanceladaEm(), c.getMotivoCancelamento(),
                    c.getTaxaCancelamento().toPlainString()));
        }

        StringBuilder prescricoes = new StringBuilder(Csv.linha("id", "consultaId", "emitidaEm", "observacoes"));
        StringBuilder itens = new StringBuilder(Csv.linha("prescricaoId", "medicamento", "posologia", "orientacoes"));
        for (Prescricao p : base.prescricoes().listarTodos()) {
            prescricoes.append(Csv.linha(p.getId(), p.getConsulta().getId(), p.getEmitidaEm(), p.getObservacoes()));
            for (ItemPrescricao item : p.getItens()) {
                itens.append(Csv.linha(p.getId(), item.medicamento(), item.posologia(), item.orientacoes()));
            }
        }

        StringBuilder pagamentos = new StringBuilder(Csv.linha("id", "consultaId", "valor", "forma", "pagoEm"));
        for (Pagamento p : base.pagamentos().listarTodos()) {
            pagamentos.append(Csv.linha(p.getId(), p.getConsulta().getId(), p.getValor().toPlainString(),
                    p.getForma().name(), p.getPagoEm()));
        }

        gravar(ESPECIALIDADES, especialidades);
        gravar(MEDICOS, medicos);
        gravar(HORARIOS, horarios);
        gravar(PACIENTES, pacientes);
        gravar(CONSULTAS, consultas);
        gravar(PRESCRICOES, prescricoes);
        gravar(ITENS_PRESCRICAO, itens);
        gravar(PAGAMENTOS, pagamentos);
    }

    private void gravar(String nomeArquivo, CharSequence conteudo) {
        Path destino = pasta.resolve(nomeArquivo);
        Path temporario = pasta.resolve(nomeArquivo + ".tmp");
        try {
            Files.writeString(temporario, conteudo, StandardCharsets.UTF_8);
            try {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel gravar " + nomeArquivo + ": " + e.getMessage(), e);
        }
    }

    // =================================================================== backup

    @Override
    public String arquivarDadosAtuais() {
        String carimbo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = pasta.resolve("backup-" + carimbo);
        try {
            Files.createDirectories(backup);
            for (String nome : ARQUIVOS) {
                Path arquivo = pasta.resolve(nome);
                if (Files.exists(arquivo)) {
                    Files.move(arquivo, backup.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel mover os arquivos para backup: " + e.getMessage(), e);
        }
        return backup.toAbsolutePath().normalize().toString();
    }
}
