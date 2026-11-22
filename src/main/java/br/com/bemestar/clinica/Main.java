package br.com.bemestar.clinica;

import br.com.bemestar.clinica.console.ConsoleApp;

/**
 * Ponto de entrada do sistema da Clinica Bem Estar.
 */
public class Main {

    public static void main(String[] args) {
        new ConsoleApp(new Clinica(), System.in, System.out).executar();
    }
}
