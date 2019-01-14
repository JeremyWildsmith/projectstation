package com.jevaengine.spacestation.item;

import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.config.NoSuchChildVariableException;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BodyPartItemFunctionFactory implements IItemFunctionFactory {
    private final Logger logger = LoggerFactory.getLogger(BodyPartItemFunctionFactory.class);

    @Override
    public IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, ISceneModelFactory modelFactory, IImmutableVariable parameters) {
        String targetName = null;

        try {
            targetName = parameters.getChild("target").getValue(String.class);
        } catch (NoSuchChildVariableException | ValueSerializationException ex) {
            logger.error("Unable to get body part wield target.", ex);
        }

        IItem.IWieldTarget tgt = null;
        for(IItem.IWieldTarget t : SpaceCharacterWieldTarget.values()) {
            if(t.getName().compareTo(targetName) == 0)
                tgt = t;
        }

        if(tgt == null) {
            logger.error("Error finding wield target : " + targetName);
        }

        final IItem.IWieldTarget wieldTarget = tgt;

        return new IItem.IItemFunction() {
            @Override
            public IItem.IWieldTarget[] getWieldTargets() {
                if(wieldTarget == null)
                    return new IItem.IWieldTarget[0];
                else
                    return new IItem.IWieldTarget[] {wieldTarget};
            }

            @Override
            public String getName() {
                return "Body Part";
            }

            @Override
            public IImmutableAttributeSet use(IRpgCharacter user, IItem.ItemTarget target, AttributeSet itemAttributes, IItem item) {
                return itemAttributes;
            }

            @Override
            public IItem.ItemUseAbilityTestResults testUseAbility(IRpgCharacter user, IItem.ItemTarget target, IImmutableAttributeSet item) {
                return new IItem.ItemUseAbilityTestResults(false);
            }
        };
    }
}
