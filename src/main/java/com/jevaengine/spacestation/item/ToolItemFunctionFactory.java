package com.jevaengine.spacestation.item;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.entity.ItemDrop;
import io.github.jevaengine.config.*;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.search.RadialSearchFilter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.URI;
import java.net.URISyntaxException;

public class ToolItemFunctionFactory implements IItemFunctionFactory {
    private static final String CONSTRUCT_FLAG_PREFIX = "CONSTRUCT_";

    @Override
    public IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, ISceneModelFactory modelFactory, IImmutableVariable parameters) {
        try {
            ToolRule[] rules = parameters.getChild("rules").getValues(ToolRule[].class);
            return new ToolItemFunction(itemFactory, rules);
        } catch (ValueSerializationException | NoSuchChildVariableException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ToolRule implements ISerializable {
        private String above;
        private String give = null;
        private boolean destroy = false;

        private Vector2F calculateUseLocation(IRpgCharacter user) {
            Vector2F playerLocation = user.getBody().getLocation().getXy();

            Direction playerDirecton = user.getModel().getDirection();

            switch(playerDirecton) {
                case XYMinusPlus:
                case XYPlus:
                    playerDirecton = Direction.YPlus;
                    break;
                case XYMinus:
                case XYPlusMinus:
                    playerDirecton = Direction.YMinus;
            }

            Vector2F buildLoc = playerLocation.add(playerDirecton.getDirectionVector());

            return new Vector2F(buildLoc.round());
        }

        public void use(IItemFactory itemFactory, IRpgCharacter user) {
            try {
                World world = user.getWorld();

                if(world == null)
                    return;

                Vector2F location = calculateUseLocation(user);

                Infrastructure[] belowEntities = world.getEntities().search(Infrastructure.class, new RadialSearchFilter<Infrastructure>(location, 0.4F));

                Infrastructure top = null;
                for(Infrastructure e : belowEntities) {
                    if(top == null || e.getBody().getLocation().z > top.getBody().getLocation().z)
                        top = e;
                }

                if(top == null || !top.hasInfrastructureType(above))
                    return;

                if(destroy)
                    world.removeEntity(top);

                IItem item = itemFactory.create(new URI(give));
                IEntity itemDrop = new ItemDrop(item);
                world.addEntity(itemDrop);
                itemDrop.getBody().setLocation(new Vector3F(location, user.getBody().getLocation().z - 0.01F));
            } catch (IItemFactory.ItemContructionException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public IItem.ItemUseAbilityTestResults testUseability(IRpgCharacter user) {
            World world = user.getWorld();

            if(world == null)
                return new IItem.ItemUseAbilityTestResults(false, "Player must be in a world to use this item.");

            Vector2F location = calculateUseLocation(user);

            Infrastructure[] belowEntities = world.getEntities().search(Infrastructure.class, new RadialSearchFilter<Infrastructure>(location, 0.4F));

            Infrastructure top = null;
            for(Infrastructure e : belowEntities) {
                if(top == null || e.getBody().getLocation().z > top.getBody().getLocation().z)
                    top = e;
            }

            if(top == null || !top.hasInfrastructureType(above))
                return new IItem.ItemUseAbilityTestResults(false, "There is no infrastructure beneath the player to use this item on.");

            return new IItem.ItemUseAbilityTestResults(true);
        }

        @Override
        public void serialize(IVariable target) throws ValueSerializationException {
            throw new ValueSerializationException(new NotImplementedException());
        }

        @Override
        public void deserialize(IImmutableVariable source) throws ValueSerializationException {
            try {
                if (source.childExists("give"))
                    give = source.getChild("give").getValue(String.class);

                if (source.childExists("destroy"))
                    destroy = source.getChild("destroy").getValue(Boolean.class);

                above = source.getChild("above").getValue(String.class);

            } catch (NoSuchChildVariableException e) {
                throw new ValueSerializationException(e);
            }
        }
    }

    public static class ToolItemFunction implements IItem.IItemFunction {

        private final IItemFactory itemFactory;
        private final ToolRule[] rules;

        public ToolItemFunction(IItemFactory itemFactory, ToolRule[] rules) {
            this.itemFactory = itemFactory;
            this.rules = rules;
        }

        @Override
        public IItem.IWieldTarget[] getWieldTargets() {
            return new IItem.IWieldTarget[] {SpaceCharacterWieldTarget.LeftHand};
        }

        @Override
        public String getName() {
            return "Construction";
        }

        @Override
        public IImmutableAttributeSet use(IRpgCharacter user, IItem.ItemTarget target, AttributeSet itemAttributes, IItem item) {
            for(ToolRule r : rules) {
                if(r.testUseability(user).isUseable()) {
                    r.use(itemFactory, user);
                    break;
                }
            }

            return new AttributeSet();
        }

        @Override
        public IItem.ItemUseAbilityTestResults testUseAbility(IRpgCharacter user, IItem.ItemTarget target, IImmutableAttributeSet item) {
            IItem.ItemUseAbilityTestResults first = null;

            for(ToolRule r : rules) {
                IItem.ItemUseAbilityTestResults result = r.testUseability(user);

                if(first == null)
                    first = result;

                if(result.isUseable())
                    return result;
            }

            return first;
        }
    }
}
