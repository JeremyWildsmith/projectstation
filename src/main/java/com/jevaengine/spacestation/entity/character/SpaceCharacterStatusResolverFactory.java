package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.DamageCategory;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IStatusResolver;
import io.github.jevaengine.rpg.entity.character.IStatusResolverFactory;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.scene.model.IActionSceneModel;


public class SpaceCharacterStatusResolverFactory implements IStatusResolverFactory {
    public SpaceCharacterStatusResolverFactory() {
    }

    @Override
    public IStatusResolver create(IRpgCharacter host, AttributeSet attributes, IActionSceneModel model) {
        return new SpaceCharacterStatusResolver(host, attributes);
    }

    private class SpaceCharacterStatusResolver implements IStatusResolver {
        private final IRpgCharacter host;
        private final AttributeSet attributes;

        public SpaceCharacterStatusResolver(IRpgCharacter character, AttributeSet attributes) {
            this.host = character;
            this.attributes = attributes;
        }

        public void updateEffectiveHitpoints() {
            float hitpoints = attributes.get(SpaceCharacterAttribute.MaxHitpoints).get();

            for(DamageCategory cat : DamageCategory.values()) {
                hitpoints -= attributes.get(cat.getAffectedAttribute()).get();
            }

            hitpoints = Math.max(0, hitpoints);

            attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).set(hitpoints);
        }

        public boolean isCritical() {
            return attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).isZero();
        }

        public boolean isBleeding() {
            return false;
        }

        @Override
        public boolean isDead() {
            return attributes.get(SpaceCharacterAttribute.EffectiveHitpoints).isZero();
        }

        @Override
        public IObserverRegistry getObservers() {
            return new Observers();
        }

        @Override
        public void update(int deltaTime) {
            updateEffectiveHitpoints();
        }

        @Override
        public IActionSceneModel decorate(IActionSceneModel subject) {
            return subject;
        }
    }
}