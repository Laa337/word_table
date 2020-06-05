package repositories.actor.wordtable;

import com.google.inject.ImplementedBy;
import models.actorModels.wordgame.wordtable.WordTable;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

@ImplementedBy(JPATableRepository.class)
public interface TableRepository {
    CompletionStage<WordTable> addTable(WordTable table);

    CompletionStage<WordTable> getById(long id);

    CompletionStage<Stream<WordTable>> listAll();
}
