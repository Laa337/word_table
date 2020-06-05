package repositories.actor.scandi;

import models.actorModels.wordgame.scandi.ScandiTable;
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

public class JPAScandiRepository implements ScandiRepository {
    private final JPAApi jpaApi;
    private DatabaseExecutionContext dec;
    private final Futures futures;

    @Inject
    public JPAScandiRepository(JPAApi jpaApi, DatabaseExecutionContext dec, Futures futures) {
        this.jpaApi = jpaApi;
        this.dec = dec;
        this.futures = futures;
    }

    public CompletionStage<ScandiTable> addTable(ScandiTable table) {
        return transact( (em) -> {
            System.out.println("Table: " + table.getJsonTable());
                    em.persist(table);
                    return table;
                }
        );
    }

    public CompletionStage<ScandiTable> getById(long id) {
        return transact( (em) -> em.find(ScandiTable.class,id));
    }

    public CompletionStage<Stream<ScandiTable>> listAll()  {
        return transact((em) -> em.createQuery("select p from ScandiTable p", ScandiTable.class).getResultList().stream());
    }

    private <T> CompletionStage<T> transact(Function<EntityManager, T> function) {
        return futures.timeout( CompletableFuture.supplyAsync( ()  -> jpaApi.withTransaction(function), dec  ), Duration.ofSeconds(3) );
    }
}
