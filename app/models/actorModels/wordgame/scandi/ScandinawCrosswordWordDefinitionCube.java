package models.actorModels.wordgame.scandi;

import models.actorModels.wordgame.scandi.ScandinawCrosswordWordDefinition;

import java.util.ArrayList;
import java.util.List;

public class ScandinawCrosswordWordDefinitionCube {
    private List<ScandinawCrosswordWordDefinition> definitions;

    public List<ScandinawCrosswordWordDefinition> getDefinitions() {
        return definitions;
    }

    public ScandinawCrosswordWordDefinitionCube() {
        definitions = new ArrayList<>();
    }

    public boolean addDefinition(ScandinawCrosswordWordDefinition definition) {

        if(this.definitions.size()==2)
            return false;
        if(this.definitions.isEmpty() || this.definitions.get(0).getDirection() != definition.getDirection()) {
            this.definitions.add(definition);
            return true;
        }
        return false;
    }

    public boolean insertable(ScandinawCrosswordWordDefinition definition) {
        if(this.definitions.size()==1) {
            System.out.println("Kérés addDefinitiohoz Pozicio:  " + this.definitions.get(0).getPositio().y +
                    " : " + this.definitions.get(0).getPositio().x);
            System.out.println("Kérés addDefinitiohoz: " + this.definitions.get(0).getDirection() +
                    " <--> " + definition.getDirection());
        }
        if(this.definitions.size()==2)
            return false;
        if(this.definitions.isEmpty() || this.definitions.get(0).getDirection() != definition.getDirection()) {
            return true;
        }
        return false;
    }
}
