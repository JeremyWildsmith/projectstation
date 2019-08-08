package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptom;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;
import com.jevaengine.spacestation.gas.GasMetaData;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.scene.model.IActionSceneModel;

import java.util.ArrayList;
import java.util.List;


public class SpaceShipStatusResolverFactory implements ISpaceCharacterStatusResolverFactory {

    @Override
    public ISpaceCharacterStatusResolver create(IRpgCharacter host, AttributeSet attributes, IActionSceneModel model) {
        if(!(host instanceof SpaceShip))
            throw new RuntimeException("Can only be applied to space character.");

        return new SpaceShipStatusResolver((SpaceShip)host, attributes);
    }

    public class SpaceShipStatusResolver implements ISpaceCharacterStatusResolver {
        private final SpaceShip host;
        private final AttributeSet attributes;

        private final List<ISymptom> symptoms = new ArrayList<>();
        private final Observers m_observers = new Observers();

        public SpaceShipStatusResolver(SpaceShip host, AttributeSet attributes) {
            this.host = host;
            this.attributes = attributes;
        }

        private List<ISymptom> getActiveSymptoms() {
            List<ISymptom> active = new ArrayList<>();

            active.addAll(symptoms);

            for(ISymptom s : symptoms) {
                if(!active.contains(s))
                    continue;

                for(int i = 0; i < active.size(); i++) {
                    if(s != active.get(i) && s.overrides(active.get(i))) {
                        active.remove(i);
                        i--;
                    }
                }
            }

            return active;
        }

        public List<ISymptomDetails> getSymptoms() {
            List<ISymptomDetails> details = new ArrayList<>();

            details.addAll(getActiveSymptoms());

            return details;
        }

        public void addSymptom(ISymptom symtom) {
            affectedBySymptom(symtom);
        }

        public void removeSymptom(String name) {
            ISymptom remove = null;

            for(ISymptom s : symptoms) {
                if(s.getName().equals(name)) {
                    remove = s;
                    break;
                }
            }

            if(remove != null) {
                symptoms.remove(remove);
                m_observers.raise(ISpaceCharacterStatusObserver.class).lostSymptom(remove);
            }
        }

        private void affectedBySymptom(ISymptom symptom) {
            for (ISymptom active : symptoms) {
                if(active.tryConsume(symptom))
                    return;
            }

            symptoms.add(symptom);
            m_observers.raise(ISpaceCharacterStatusObserver.class).affectedBySymptom(symptom);
        }

        private void processSymptoms(int deltaTime) {
            List<ISymptom> active = getActiveSymptoms();

            List<ISymptom> remove = new ArrayList<>();

            for(ISymptom s : symptoms) {
                if(active.contains(s))
                    host.consume(s.getImpact(deltaTime), s.isRecovery());
                else
                    s.elapseIneffective(deltaTime);

                if(s.isGone())
                    remove.add(s);
            }

            symptoms.removeAll(remove);

            for(ISymptom s : remove) {
                m_observers.raise(ISpaceCharacterStatusObserver.class).lostSymptom(s);
            }
        }

        public void updateEffectiveHitpoints() {
            float hitpoints = attributes.get(SpaceCharacterAttribute.MaxHitpoints).get();
            float current = attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).get();

            for(DamageCategory cat : DamageCategory.values()) {
                hitpoints -= attributes.get(cat.getAffectedAttribute()).get();
            }

            hitpoints = Math.max(0, hitpoints);

            attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).set(hitpoints);

            if(Math.abs(current - hitpoints) > 0.01f) {
                m_observers.raise(ISpaceCharacterStatusObserver.class).effectiveHitpointsChanged(hitpoints);
            }
        }

        public boolean isCritical() {
            return attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).isZero();
        }

        @Override
        public boolean isDead() {
            return attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).isZero();
        }

        @Override
        public IObserverRegistry getObservers() {
            return m_observers;
        }

        @Override
        public void update(int deltaTime) {
            processSymptoms(deltaTime);
            updateEffectiveHitpoints();
        }

        @Override
        public IActionSceneModel decorate(IActionSceneModel subject) {
            return subject;
        }
    }
}

