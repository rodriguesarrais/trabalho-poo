package br.com.bemestar.clinica.excecao;

/**
 * Lancada quando uma entidade e buscada por um id que nao existe.
 */
public class EntidadeNaoEncontradaException extends ClinicaException {

    public EntidadeNaoEncontradaException(String entidade, long id) {
        super(entidade + " #" + id + " nao encontrado(a).");
    }
}
