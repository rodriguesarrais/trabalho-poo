package br.com.bemestar.clinica.repositorio;

import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Especialidade;
import br.com.bemestar.clinica.modelo.Medico;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.Pagamento;
import br.com.bemestar.clinica.modelo.Prescricao;

/**
 * Agrupa os repositorios de todas as entidades do sistema. E o conjunto de
 * dados que a camada de persistencia carrega e grava.
 */
public class BaseDeDados {

    private final Repositorio<Especialidade> especialidades = new RepositorioEmMemoria<>();
    private final Repositorio<Medico> medicos = new RepositorioEmMemoria<>();
    private final Repositorio<Paciente> pacientes = new RepositorioEmMemoria<>();
    private final Repositorio<Consulta> consultas = new RepositorioEmMemoria<>();
    private final Repositorio<Prescricao> prescricoes = new RepositorioEmMemoria<>();
    private final Repositorio<Pagamento> pagamentos = new RepositorioEmMemoria<>();

    public Repositorio<Especialidade> especialidades() {
        return especialidades;
    }

    public Repositorio<Medico> medicos() {
        return medicos;
    }

    public Repositorio<Paciente> pacientes() {
        return pacientes;
    }

    public Repositorio<Consulta> consultas() {
        return consultas;
    }

    public Repositorio<Prescricao> prescricoes() {
        return prescricoes;
    }

    public Repositorio<Pagamento> pagamentos() {
        return pagamentos;
    }

    public boolean estaVazia() {
        return especialidades.estaVazio() && medicos.estaVazio() && pacientes.estaVazio()
                && consultas.estaVazio() && prescricoes.estaVazio() && pagamentos.estaVazio();
    }
}
