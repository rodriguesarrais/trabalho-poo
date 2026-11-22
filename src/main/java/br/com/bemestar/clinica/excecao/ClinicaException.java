package br.com.bemestar.clinica.excecao;

/**
 * Excecao base de todos os erros do dominio da clinica. A interface com o
 * usuario so precisa capturar este tipo para exibir a mensagem ao operador.
 */
public class ClinicaException extends RuntimeException {

    public ClinicaException(String mensagem) {
        super(mensagem);
    }
}
