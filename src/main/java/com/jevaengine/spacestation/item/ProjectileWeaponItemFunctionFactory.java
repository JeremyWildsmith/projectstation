package com.jevaengine.spacestation.item;

import com.jevaengine.spacestation.entity.projectile.LaserProjectile;
import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.config.NoSuchChildVariableException;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class ProjectileWeaponItemFunctionFactory implements IItemFunctionFactory {

    private Logger logger = LoggerFactory.getLogger(ProjectileWeaponItemFunction.class);

    @Override
    public IItem.IItemFunction create(IItemFactory itemFactory, IEntityFactory entityFactory, ISceneModelFactory modelFactory, IImmutableVariable parameters) {
        URI entity = null;

        try {
            if(parameters.childExists("projectile"))
                entity = new URI(parameters.getChild("projectile").getValue(String.class));
        } catch (NoSuchChildVariableException | URISyntaxException | ValueSerializationException ex) {
            logger.error("Error creating bullet model.", ex);
        }

        return new ProjectileWeaponItemFunction(entityFactory, entity);
    }

    private static class ProjectileWeaponItemFunction implements IItem.IItemFunction {
        private final Logger logger = LoggerFactory.getLogger(ProjectileWeaponItemFunction.class);
        private final IEntityFactory entityFactory;
        private final URI projectileEntity;

        public ProjectileWeaponItemFunction(IEntityFactory entityFactory, URI projectileEntity) {
            this.entityFactory = entityFactory;
            this.projectileEntity = projectileEntity;
        }

        @Override
        public IItem.IWieldTarget[] getWieldTargets() {
            return new SpaceCharacterWieldTarget[] {SpaceCharacterWieldTarget.LeftHand};
        }

        @Override
        public String getName() {
            return "ProjectileWeapon";
        }

        @Override
        public IImmutableAttributeSet use(IRpgCharacter user, IItem.ItemTarget target, AttributeSet itemAttributes, IItem item) {
            Vector2F direction = target.getTargetLocation().getXy().difference(user.getBody().getLocation().getXy());
            if(direction.isZero())
                direction = new Vector2F(Direction.XMinus.getDirectionVector());
            else
                direction = direction.normalize();

            Vector3F origin = user.getBody().getLocation().add(new Vector3F(direction.multiply(0.3f), 1.0f));

            try {
                LaserProjectile projectile = entityFactory.create(LaserProjectile.class, null, projectileEntity);
                user.getWorld().addEntity(projectile);
                projectile.getBody().setLocation(origin);
                projectile.setTravelDirection(new Vector3F(direction, 0));
                projectile.setIgnore(user);
            } catch(IEntityFactory.EntityConstructionException ex) {
                logger.error("Unable to construct weapon projectile.", ex);
            }
            return itemAttributes;
        }

        @Override
        public IItem.ItemUseAbilityTestResults testUseAbility(IRpgCharacter user, IItem.ItemTarget target, IImmutableAttributeSet item) {
            return new IItem.ItemUseAbilityTestResults(true);
        }
    }
}
