package br.com.bemestar.clinica.modelo;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Medico da clinica, associado a uma especialidade e com uma agenda semanal
 * de horarios de atendimento.
 */
public class Medico extends Pessoa {

    private final String crm;
    private Especialidade especialidade;
    private final Map<DayOfWeek, HorarioAtendimento> agendaSemanal = new EnumMap<>(DayOfWeek.class);

    public Medico(long id, String nome, String cpf, String telefone, String crm, Especialidade especialidade) {
        super(id, nome, cpf, telefone);
        this.crm = Validacao.naoVazio(crm, "CRM");
        this.especialidade = Validacao.naoNulo(especialidade, "Especialidade");
    }

    public String getCrm() {
        return crm;
    }

    public Especialidade getEspecialidade() {
        return especialidade;
    }

    public void alterarEspecialidade(Especialidade especialidade) {
        this.especialidade = Validacao.naoNulo(especialidade, "Especialidade");
    }

    /** Define (ou substitui) o turno do dia da semana informado no horario. */
    public void definirHorario(HorarioAtendimento horario) {
        Validacao.naoNulo(horario, "Horario de atendimento");
        agendaSemanal.put(horario.dia(), horario);
    }

    public void removerHorario(DayOfWeek dia) {
        agendaSemanal.remove(dia);
    }

    public Optional<HorarioAtendimento> horarioNo(DayOfWeek dia) {
        return Optional.ofNullable(agendaSemanal.get(dia));
    }

    public Collection<HorarioAtendimento> getAgendaSemanal() {
        return Collections.unmodifiableCollection(agendaSemanal.values());
    }

    /** Todos os slots do medico na data, ocupados ou nao. */
    public List<LocalDateTime> slotsNo(LocalDate data, Duration duracaoSlot) {
        return horarioNo(data.getDayOfWeek())
                .map(horario -> horario.iniciosDeSlots(duracaoSlot).stream().map(data::atTime).toList())
                .orElse(List.of());
    }

    /** Indica se o medico esta em horario de atendimento no momento informado. */
    public boolean estaEmAtendimento(LocalDateTime momento) {
        return horarioNo(momento.getDayOfWeek())
                .map(horario -> horario.contem(momento.toLocalTime()))
                .orElse(false);
    }

    @Override
    public String toString() {
        return "#" + getId() + " Dr(a). " + getNome() + " (CRM " + crm + ") - " + especialidade.getNome();
    }
}
