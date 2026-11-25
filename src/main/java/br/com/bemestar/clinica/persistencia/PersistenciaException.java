package br.com.bemestar.clinica.persistencia;

import br.com.bemestar.clinica.excecao.ClinicaException;

/**
 * Falha ao ler ou gravar os arquivos de dados (arquivo corrompido, sem
 * permissao de escrita, disco cheio etc.).
 */
public class PersistenciaException extends ClinicaException {

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem);
        initCause(causa);
    }
}
