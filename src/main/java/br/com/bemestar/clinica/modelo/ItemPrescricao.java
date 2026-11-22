package br.com.bemestar.clinica.modelo;

/**
 * Um medicamento prescrito, com posologia e orientacoes de uso.
 */
public record ItemPrescricao(String medicamento, String posologia, String orientacoes) {

    public ItemPrescricao {
        medicamento = Validacao.naoVazio(medicamento, "Medicamento");
        posologia = Validacao.naoVazio(posologia, "Posologia");
        orientacoes = orientacoes == null ? "" : orientacoes.trim();
    }

    @Override
    public String toString() {
        return medicamento + " - " + posologia + (orientacoes.isEmpty() ? "" : " (" + orientacoes + ")");
    }
}
