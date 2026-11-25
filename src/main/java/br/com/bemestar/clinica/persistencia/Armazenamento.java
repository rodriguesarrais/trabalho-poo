package br.com.bemestar.clinica.persistencia;

import br.com.bemestar.clinica.repositorio.BaseDeDados;

/**
 * Contrato da persistencia dos dados. A interface de usuario depende apenas
 * deste contrato, entao trocar CSV por JSON ou banco nao afeta o resto do sistema.
 */
public interface Armazenamento {

    /**
     * Carrega os dados gravados. Se ainda nao houver dados (primeira execucao,
     * arquivos ausentes), retorna uma base vazia.
     *
     * @throws PersistenciaException se algum arquivo existir mas estiver invalido
     */
    BaseDeDados carregar();

    /** Grava todos os dados da base. */
    void salvar(BaseDeDados base);

    /**
     * Move os arquivos atuais para uma pasta de backup, preservando-os para
     * analise, e retorna a descricao de onde foram guardados.
     */
    String arquivarDadosAtuais();

    /** Descricao do local onde os dados sao gravados (para exibicao). */
    String local();
}
