package br.com.bemestar.clinica.repositorio;

import br.com.bemestar.clinica.modelo.Identificavel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Implementacao do repositorio em memoria, preservando a ordem de cadastro.
 */
public class RepositorioEmMemoria<T extends Identificavel> implements Repositorio<T> {

    private final Map<Long, T> dados = new LinkedHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    @Override
    public long proximoId() {
        return sequencia.incrementAndGet();
    }

    @Override
    public T salvar(T entidade) {
        dados.put(entidade.getId(), entidade);
        return entidade;
    }

    @Override
    public Optional<T> buscarPorId(long id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return List.copyOf(dados.values());
    }

    @Override
    public List<T> filtrar(Predicate<? super T> criterio) {
        return dados.values().stream().filter(criterio).toList();
    }
}
