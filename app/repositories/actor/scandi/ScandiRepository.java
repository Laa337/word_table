package repositories.actor.scandi;

import com.google.inject.ImplementedBy;
import models.actorModels.wordgame.scandi.ScandiTable;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

@ImplementedBy(JPAScandiRepository.class)
public interface ScandiRepository {
    CompletionStage<ScandiTable> addTable(ScandiTable table);

    CompletionStage<ScandiTable> getById(long id);
    CompletionStage<Stream<ScandiTable>> listAll();
}
