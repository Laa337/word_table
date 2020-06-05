package repositories.actor.wordtable;

import models.actorModels.wordgame.wordtable.WordTable;
import play.db.jpa.JPAApi;
import play.libs.concurrent.Futures;
import repositories.DatabaseExecutionContext;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

public class JPATableRepository implements TableRepository {
    private final JPAApi jpaApi;
    private DatabaseExecutionContext dec;
    private final Futures futures;

    @Inject
    public JPATableRepository(JPAApi jpaApi, DatabaseExecutionContext dec, Futures futures) {
        this.jpaApi = jpaApi;
        this.dec = dec;
        this.futures = futures;
    }

    public CompletionStage<WordTable> addTable(WordTable table) {
        return transact( (em) -> {
                    em.persist(table);
                    return table;
                }
        );
    }

    @Override
    public CompletionStage<WordTable> getById(long id) {
        return transact( (em) -> em.find(WordTable.class,id));
    }


    @Override
    public CompletionStage<Stream<WordTable>> listAll() {
        return transact((em) -> em.createQuery("select p from WordTable p", WordTable.class).getResultList().stream());
    }

    private <T> CompletionStage<T> transact(Function<EntityManager, T> function) {
        return futures.timeout( CompletableFuture.supplyAsync( ()  -> jpaApi.withTransaction(function), dec  ), Duration.ofSeconds(3) );
    }

}
