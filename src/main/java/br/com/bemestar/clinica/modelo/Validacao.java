package br.com.bemestar.clinica.modelo;

import br.com.bemestar.clinica.excecao.RegraNegocioException;

import java.math.BigDecimal;

/**
 * Validacoes reutilizadas pelas classes do dominio.
 */
public final class Validacao {

    private Validacao() {
    }

    public static <T> T naoNulo(T valor, String campo) {
        if (valor == null) {
            throw new RegraNegocioException(campo + " e obrigatorio(a).");
        }
        return valor;
    }

    public static String naoVazio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(campo + " nao pode ficar em branco.");
        }
        return valor.trim();
    }

    public static BigDecimal positivo(BigDecimal valor, String campo) {
        naoNulo(valor, campo);
        if (valor.signum() <= 0) {
            throw new RegraNegocioException(campo + " deve ser maior que zero.");
        }
        return valor;
    }

    /**
     * Normaliza um CPF para os seus 11 digitos, aceitando a pontuacao usual
     * (ex.: "123.456.789-09").
     */
    public static String cpf(String valor) {
        String digitos = naoVazio(valor, "CPF").replaceAll("[.\\-\\s]", "");
        if (!digitos.matches("\\d{11}")) {
            throw new RegraNegocioException("O CPF deve conter exatamente 11 digitos.");
        }
        return digitos;
    }
}
