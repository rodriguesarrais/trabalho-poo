package br.com.bemestar.clinica.modelo;

/**
 * Formas de pagamento aceitas pela clinica.
 */
public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartao de debito"),
    CARTAO_CREDITO("Cartao de credito"),
    PIX("Pix"),
    CONVENIO("Faturado ao convenio");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
