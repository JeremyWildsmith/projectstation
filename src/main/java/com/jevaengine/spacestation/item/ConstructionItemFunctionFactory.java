package com.jevaengine.spacestation.item;

import com.jevaengine.spacestation.entity.Infrastructure;
import io.github.jevaengine.config.*;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.search.RadialSearchFilter;
import org.apache.commons.lang.NotImplementedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

public class ConstructionItemFunctionFactory implements IItemFunctionFactory {
    @Override
    public IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, ISceneModelFactory modelFactory, IImmutableVariable parameters) {
        try {
            ConstructionRule[] rules = parameters.getChild("rules").getValues(ConstructionRule[].class);
            return new ConstructionItemFunction(itemFactory, entityFactory, rules);
        } catch (ValueSerializationException | NoSuchChildVariableException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ConstructionRule implements ISerializable {
        private String above = null;
        private float depth = 0;
        private String entityClass;
        private String entityConfig;
        private String with = null;
        private IItem withItem = null;

        private boolean replace = false;

        private IItem getWithItem(IItemFactory itemFactory) {
            try {
                if (with != null && withItem == null)
                    withItem = itemFactory.create(new URI(with));

                return withItem;
            } catch (IItemFactory.ItemContructionException | URISyntaxException e)
            {
                throw new RuntimeException(e);
            }
        }

        private Vector3F calculateBuildLocation(IRpgCharacter user) {
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

            return new Vector3F(buildLoc.round(), depth);
        }

        public void use(IItemFactory itemFactory, IEntityFactory entityFactory, IRpgCharacter user) {
            try {
                Vector3F location = calculateBuildLocation(user);

                if(replace) {
                    Infrastructure[] belowEntities = user.getWorld().getEntities().search(Infrastructure.class, new RadialSearchFilter<Infrastructure>(location.getXy(), 0.4F));

                    for(Infrastructure inf : belowEntities) {
                        if (inf.getBody().getLocation().z < depth && inf.hasInfrastructureType(above))
                            user.getWorld().removeEntity(inf);
                    }
                }

                IEntity e = entityFactory.create(entityClass, null, new URI(entityConfig));
                user.getWorld().addEntity(e);
                e.getBody().setLocation(location);

                IItemSlot handSlot = user.getLoadout().getSlot(SpaceCharacterWieldTarget.LeftHand);
                IItem handSlotItem = handSlot.getItem();

                handSlot.clear();

                IItem useWith = getWithItem(itemFactory);
                if(useWith != null)
                    user.getInventory().removeItem(useWith);

                for(IItemSlot s : user.getInventory().getSlots()) {
                    if(!s.isEmpty() && (s.getItem().getFunction().equals(handSlotItem.getFunction()))) {
                        handSlot.setItem(s.clear());
                        break;
                    }
                }
            } catch (IEntityFactory.EntityConstructionException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public IItem.ItemUseAbilityTestResults testUseability(IItemFactory itemFactory, IRpgCharacter user, IItem targetItem) {
            IItem withItem = getWithItem(itemFactory);

            if(withItem == null && targetItem != null ||
                    (withItem != null && targetItem != null && !withItem.getFunction().equals(targetItem.getFunction())))
                return new IItem.ItemUseAbilityTestResults(false, "These two items cannot be used together");


            World world = user.getWorld();

            if(world == null)
                return new IItem.ItemUseAbilityTestResults(false, "Player must be in world to use this item.");


            Vector2F location = calculateBuildLocation(user).getXy();

            Infrastructure[] belowEntities = world.getEntities().search(Infrastructure.class, new RadialSearchFilter<Infrastructure>(location, 0.4F));

            if(belowEntities.length > 0 && above == null)
                return new IItem.ItemUseAbilityTestResults(false, "Item must be constructed in an empty space.");


            boolean satisfied = above == null ? true : false;

            for(Infrastructure e : belowEntities) {
                if(e.getBody().getLocation().z < depth && e.hasInfrastructureType(above))
                    satisfied = true;

                if(e.getBody().getLocation().z > depth)
                    return new IItem.ItemUseAbilityTestResults(false, "You cannot construct this object on this surface.");

                if(Math.abs(e.getBody().getLocation().z - depth) < Vector2F.TOLERANCE && !replace)
                    return new IItem.ItemUseAbilityTestResults(false, "Another object is already here. There s no room for this object.");
            }

            if(!satisfied)
                return new IItem.ItemUseAbilityTestResults(false, "Item must be constructed above " + above);

            return new IItem.ItemUseAbilityTestResults(true);

        }

        @Override
        public void serialize(IVariable target) throws ValueSerializationException {
            throw new ValueSerializationException(new NotImplementedException());
        }

        @Override
        public void deserialize(IImmutableVariable source) throws ValueSerializationException {
            try {
                if (source.childExists("depth"))
                    depth = source.getChild("depth").getValue(Double.class).floatValue();

                if (source.childExists("above"))
                    above = source.getChild("above").getValue(String.class);

                entityClass = source.getChild("entity").getValue(String.class);
                entityConfig = source.getChild("config").getValue(String.class);

                if (source.childExists("replace"))
                    replace = source.getChild("replace").getValue(Boolean.class);

                if(source.childExists("with"))
                    with = source.getChild("with").getValue(String.class);

            } catch (NoSuchChildVariableException e) {
                throw new ValueSerializationException(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstructionRule that = (ConstructionRule) o;
            return Float.compare(that.depth, depth) == 0 &&
                    replace == that.replace &&
                    Objects.equals(above, that.above) &&
                    Objects.equals(entityClass, that.entityClass) &&
                    Objects.equals(entityConfig, that.entityConfig);
        }

        @Override
        public int hashCode() {
            return Objects.hash(above, depth, entityClass, entityConfig, replace);
        }
    }

    public static class ConstructionItemFunction implements IItem.IItemFunction {

        private final IEntityFactory entityFactory;
        private final IItemFactory itemFactory;
        private final ConstructionRule[] rules;

        public ConstructionItemFunction(IItemFactory itemFactory, IEntityFactory entityFactory, ConstructionRule[] rules) {
            this.itemFactory = itemFactory;
            this.entityFactory = entityFactory;
            this.rules = rules;
        }

        @Override
        public IItem.IWieldTarget[] getWieldTargets() {
            return new SpaceCharacterWieldTarget[] {SpaceCharacterWieldTarget.LeftHand};
        }

        @Override
        public String getName() {
            return "Construction";
        }

        @Override
        public IImmutableAttributeSet use(IRpgCharacter user, IItem.ItemTarget target, AttributeSet itemAttributes, IItem item) {
            IItem targetItem = target.getTarget(IItem.class);


            for(ConstructionRule r : rules) {
                if(r.testUseability(itemFactory, user, targetItem).isUseable()) {
                    r.use(itemFactory, entityFactory, user);
                    break;
                }
            }

            return new AttributeSet();
        }

        @Override
        public IItem.ItemUseAbilityTestResults testUseAbility(IRpgCharacter user, IItem.ItemTarget target, IImmutableAttributeSet item) {

            IItem targetItem = target.getTarget(IItem.class);

            IItem.ItemUseAbilityTestResults first = null;

            for(ConstructionRule r : rules) {
                IItem.ItemUseAbilityTestResults result = r.testUseability(itemFactory, user, targetItem);

                if(first == null)
                    first = result;

                if(result.isUseable())
                    return result;
            }

            return first;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstructionItemFunction that = (ConstructionItemFunction) o;
            return Arrays.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(rules);
        }
    }
}
