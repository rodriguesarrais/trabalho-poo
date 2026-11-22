package br.com.bemestar.clinica.relatorio;

import br.com.bemestar.clinica.modelo.Dinheiro;
import br.com.bemestar.clinica.modelo.Pagamento;
import br.com.bemestar.clinica.modelo.TipoPaciente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Relatorio de faturamento separado por tipo de paciente (particular,
 * convenio e VIP). Todos os tipos aparecem, mesmo sem pagamentos.
 *
 * @param inicio inicio do periodo (null = desde o inicio)
 * @param fim    fim do periodo (null = ate hoje)
 * @param linhas totais por tipo de paciente
 */
public record RelatorioFaturamento(LocalDate inicio, LocalDate fim, Map<TipoPaciente, Linha> linhas) {

    /** Totais de um tipo de paciente. */
    public record Linha(TipoPaciente tipo, int quantidadePagamentos, BigDecimal total) {
    }

    public RelatorioFaturamento {
        linhas = Collections.unmodifiableMap(new EnumMap<>(linhas));
    }

    public static RelatorioFaturamento gerar(LocalDate inicio, LocalDate fim, Collection<Pagamento> pagamentos) {
        Map<TipoPaciente, Linha> linhas = new EnumMap<>(TipoPaciente.class);
        for (TipoPaciente tipo : TipoPaciente.values()) {
            linhas.put(tipo, new Linha(tipo, 0, Dinheiro.ZERO));
        }
        for (Pagamento pagamento : pagamentos) {
            Linha atual = linhas.get(pagamento.getTipoPaciente());
            linhas.put(atual.tipo(), new Linha(atual.tipo(), atual.quantidadePagamentos() + 1,
                    atual.total().add(pagamento.getValor())));
        }
        return new RelatorioFaturamento(inicio, fim, linhas);
    }

    public Linha linha(TipoPaciente tipo) {
        return linhas.get(tipo);
    }

    public BigDecimal totalGeral() {
        return linhas.values().stream().map(Linha::total).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    public int quantidadeTotal() {
        return linhas.values().stream().mapToInt(Linha::quantidadePagamentos).sum();
    }

    /** Texto do relatorio pronto para exibicao no console. */
    public String formatar() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo = (inicio == null && fim == null)
                ? "todo o periodo"
                : inicio.format(formato) + " a " + fim.format(formato);

        StringBuilder texto = new StringBuilder();
        texto.append("RELATORIO DE FATURAMENTO - ").append(periodo).append('\n');
        texto.append(String.format("%-14s %12s %18s%n", "Tipo", "Pagamentos", "Total"));
        texto.append("-".repeat(46)).append('\n');
        for (Linha linha : linhas.values()) {
            texto.append(String.format("%-14s %12d %18s%n", linha.tipo().getDescricao(),
                    linha.quantidadePagamentos(), Dinheiro.formatar(linha.total())));
        }
        texto.append("-".repeat(46)).append('\n');
        texto.append(String.format("%-14s %12d %18s%n", "TOTAL", quantidadeTotal(), Dinheiro.formatar(totalGeral())));
        return texto.toString();
    }
}
