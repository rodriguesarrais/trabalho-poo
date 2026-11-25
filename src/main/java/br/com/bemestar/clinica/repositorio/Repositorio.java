package br.com.bemestar.clinica.repositorio;

import br.com.bemestar.clinica.modelo.Identificavel;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Contrato generico de armazenamento de entidades. Os servicos dependem desta
 * interface, e nao de uma implementacao especifica.
 */
public interface Repositorio<T extends Identificavel> {

    /** Reserva o proximo id disponivel. */
    long proximoId();

    T salvar(T entidade);

    Optional<T> buscarPorId(long id);

    List<T> listarTodos();

    List<T> filtrar(Predicate<? super T> criterio);

    /** Remove a entidade; retorna false se o id nao existir. */
    boolean remover(long id);

    default boolean estaVazio() {
        return listarTodos().isEmpty();
    }
}
