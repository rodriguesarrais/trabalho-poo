package br.com.bemestar.clinica.modelo;

/**
 * Dados comuns a toda pessoa cadastrada na clinica (medicos e pacientes).
 */
public abstract class Pessoa implements Identificavel {

    private final long id;
    private final String nome;
    private final String cpf;
    private String telefone;

    protected Pessoa(long id, String nome, String cpf, String telefone) {
        this.id = id;
        this.nome = Validacao.naoVazio(nome, "Nome");
        this.cpf = Validacao.cpf(cpf);
        this.telefone = telefone == null ? "" : telefone.trim();
    }

    @Override
    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone == null ? "" : telefone.trim();
    }

    @Override
    public String toString() {
        return "#" + id + " " + nome;
    }
}
