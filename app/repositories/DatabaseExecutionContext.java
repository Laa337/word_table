package repositories;

import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

import javax.inject.Inject;

public class DatabaseExecutionContext extends CustomExecutionContext {

	@Inject
	public DatabaseExecutionContext(ActorSystem as) {
		super(as, "database.dispatcher");
	}
}