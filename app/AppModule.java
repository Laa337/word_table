import actors.BaseRooter;
import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import play.libs.akka.AkkaGuiceSupport;

import javax.inject.Inject;

public class AppModule extends AbstractModule implements AkkaGuiceSupport {

  @Override
  protected void configure() {
    bind(new TypeLiteral<ActorRef<Object>>() {})
        .toProvider(ActorProvider.class)
        .asEagerSingleton();
  }

  public static class ActorProvider implements Provider<ActorRef<Object>> {
    private final ActorSystem actorSystem;

    @Inject
    public ActorProvider(ActorSystem actorSystem) {
      this.actorSystem = actorSystem;
    }

    @Override
    public ActorRef<Object> get() {
      return Adapter.spawn(actorSystem, BaseRooter.create(), "hello-actor");
    }
  }
}