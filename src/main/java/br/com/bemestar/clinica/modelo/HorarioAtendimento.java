package br.com.bemestar.clinica.modelo;

import br.com.bemestar.clinica.excecao.RegraNegocioException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turno de atendimento de um medico em um dia da semana.
 */
public record HorarioAtendimento(DayOfWeek dia, LocalTime inicio, LocalTime fim) {

    public HorarioAtendimento {
        Validacao.naoNulo(dia, "Dia da semana");
        Validacao.naoNulo(inicio, "Horario de inicio");
        Validacao.naoNulo(fim, "Horario de fim");
        if (!inicio.isBefore(fim)) {
            throw new RegraNegocioException("O inicio do turno deve ser anterior ao fim.");
        }
        if (Duration.between(inicio, fim).compareTo(Consulta.DURACAO) < 0) {
            throw new RegraNegocioException("O turno deve comportar pelo menos uma consulta.");
        }
    }

    /** Indica se o horario esta dentro do turno (inicio inclusivo, fim exclusivo). */
    public boolean contem(LocalTime horario) {
        return !horario.isBefore(inicio) && horario.isBefore(fim);
    }

    /** Horarios de inicio de todos os slots completos de uma dada duracao dentro do turno. */
    public List<LocalTime> iniciosDeSlots(Duration duracaoSlot) {
        int duracao = (int) duracaoSlot.toMinutes();
        int primeiro = inicio.toSecondOfDay() / 60;
        int ultimo = fim.toSecondOfDay() / 60;
        List<LocalTime> slots = new ArrayList<>();
        for (int minuto = primeiro; minuto + duracao <= ultimo; minuto += duracao) {
            slots.add(LocalTime.of(minuto / 60, minuto % 60));
        }
        return slots;
    }

    public static String nomeDoDia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
    }

    @Override
    public String toString() {
        return nomeDoDia(dia) + " " + inicio + "-" + fim;
    }
}
