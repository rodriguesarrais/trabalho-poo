package br.com.bemestar.clinica.servico;

import br.com.bemestar.clinica.modelo.Consulta;
import br.com.bemestar.clinica.modelo.Paciente;
import br.com.bemestar.clinica.modelo.Prescricao;
import br.com.bemestar.clinica.modelo.StatusConsulta;

import java.util.List;

/**
 * Historico completo de um paciente: todas as consultas (em ordem
 * cronologica), com diagnosticos e prescricoes.
 */
public record HistoricoPaciente(Paciente paciente, List<Consulta> consultas) {

    public HistoricoPaciente {
        consultas = List.copyOf(consultas);
    }

    public List<Consulta> consultasRealizadas() {
        return consultas.stream().filter(c -> c.getStatus() == StatusConsulta.REALIZADA).toList();
    }

    public List<Prescricao> prescricoes() {
        return consultas.stream().flatMap(c -> c.getPrescricoes().stream()).toList();
    }
}
