package br.com.bemestar.clinica.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import br.com.bemestar.clinica.modelo.HorarioAtendimento;

/**
 * Leitura validada de dados digitados no console. Cada metodo repete a
 * pergunta ate receber um valor valido.
 */
public class Entrada {

    public static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final BufferedReader leitor;
    private final PrintStream saida;

    public Entrada(InputStream entrada, PrintStream saida) {
        this.leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.saida = saida;
    }

    public String texto(String rotulo) {
        while (true) {
            String valor = linha(rotulo + ": ");
            if (!valor.isBlank()) {
                return valor.trim();
            }
            saida.println("  Campo obrigatorio.");
        }
    }

    public String textoOpcional(String rotulo) {
        return linha(rotulo + " (opcional): ").trim();
    }

    public int inteiro(String rotulo, int minimo, int maximo) {
        while (true) {
            String valor = linha(rotulo + ": ").trim();
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
            } catch (NumberFormatException ignorada) {
                // cai na mensagem abaixo
            }
            saida.printf("  Informe um numero entre %d e %d.%n", minimo, maximo);
        }
    }

    public long id(String rotulo) {
        while (true) {
            String valor = linha(rotulo + ": ").trim();
            try {
                long numero = Long.parseLong(valor.replace("#", ""));
                if (numero > 0) {
                    return numero;
                }
            } catch (NumberFormatException ignorada) {
                // cai na mensagem abaixo
            }
            saida.println("  Informe um id numerico valido.");
        }
    }

    public BigDecimal valor(String rotulo) {
        while (true) {
            String valor = linha(rotulo + " (ex.: 250,00): ").trim().replace("R$", "").trim();
            try {
                BigDecimal numero = new BigDecimal(valor.replace(".", "").replace(",", "."));
                if (numero.signum() > 0) {
                    return numero;
                }
            } catch (NumberFormatException ignorada) {
                // cai na mensagem abaixo
            }
            saida.println("  Informe um valor maior que zero.");
        }
    }

    public LocalDate data(String rotulo) {
        while (true) {
            try {
                return LocalDate.parse(linha(rotulo + " (dd/mm/aaaa): ").trim(), FORMATO_DATA);
            } catch (DateTimeParseException e) {
                saida.println("  Data invalida.");
            }
        }
    }

    public LocalDateTime dataHora(String rotulo) {
        while (true) {
            try {
                return LocalDateTime.parse(linha(rotulo + " (dd/mm/aaaa hh:mm): ").trim(), FORMATO_DATA_HORA);
            } catch (DateTimeParseException e) {
                saida.println("  Data/hora invalida.");
            }
        }
    }

    public LocalTime hora(String rotulo) {
        while (true) {
            try {
                return LocalTime.parse(linha(rotulo + " (hh:mm): ").trim(), FORMATO_HORA);
            } catch (DateTimeParseException e) {
                saida.println("  Horario invalido.");
            }
        }
    }

    public DayOfWeek diaDaSemana(String rotulo) {
        DayOfWeek[] dias = DayOfWeek.values();
        for (int i = 0; i < dias.length; i++) {
            saida.printf("  %d. %s%n", i + 1, HorarioAtendimento.nomeDoDia(dias[i]));
        }
        return dias[inteiro(rotulo, 1, dias.length) - 1];
    }

    /** Exibe as opcoes de um enum numeradas e retorna a escolhida. */
    public <E extends Enum<E>> E opcao(String rotulo, E[] opcoes) {
        for (int i = 0; i < opcoes.length; i++) {
            saida.printf("  %d. %s%n", i + 1, opcoes[i]);
        }
        return opcoes[inteiro(rotulo, 1, opcoes.length) - 1];
    }

    public boolean confirmar(String pergunta) {
        while (true) {
            String valor = linha(pergunta + " (s/n): ").trim().toLowerCase();
            if (valor.equals("s") || valor.equals("sim")) {
                return true;
            }
            if (valor.equals("n") || valor.equals("nao")) {
                return false;
            }
            saida.println("  Responda s ou n.");
        }
    }

    private String linha(String prompt) {
        saida.print(prompt);
        saida.flush();
        try {
            String linha = leitor.readLine();
            if (linha == null) {
                throw new EntradaEncerradaException();
            }
            return linha;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
