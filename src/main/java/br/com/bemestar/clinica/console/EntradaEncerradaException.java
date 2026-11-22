package br.com.bemestar.clinica.console;

/**
 * Sinaliza que a entrada padrao foi encerrada (fim de arquivo / Ctrl+D).
 */
class EntradaEncerradaException extends RuntimeException {

    EntradaEncerradaException() {
        super("Entrada encerrada.");
    }
}
