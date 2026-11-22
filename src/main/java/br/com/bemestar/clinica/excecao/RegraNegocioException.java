package br.com.bemestar.clinica.excecao;

/**
 * Lancada quando uma operacao viola uma regra de negocio da clinica
 * (conflito de horario, transicao de status invalida, dado invalido etc.).
 */
public class RegraNegocioException extends ClinicaException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
