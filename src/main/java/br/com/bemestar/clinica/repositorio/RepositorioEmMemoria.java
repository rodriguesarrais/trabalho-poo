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
 * Os dados sao gravados em disco pela camada de persistencia.
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
        // Garante que ids carregados de arquivo nao sejam reutilizados.
        sequencia.accumulateAndGet(entidade.getId(), Math::max);
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

    @Override
    public boolean remover(long id) {
        return dados.remove(id) != null;
    }
}
