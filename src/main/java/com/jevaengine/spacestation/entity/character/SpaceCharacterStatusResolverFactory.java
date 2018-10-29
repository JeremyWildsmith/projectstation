package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasType;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IStatusResolver;
import io.github.jevaengine.rpg.entity.character.IStatusResolverFactory;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IActionSceneModel;

import java.util.HashMap;
import java.util.Map;


public class SpaceCharacterStatusResolverFactory implements IStatusResolverFactory {
    private final IStatusResolverFactory base;

    public SpaceCharacterStatusResolverFactory(IStatusResolverFactory base) {
        this.base = base;
    }

    @Override
    public IStatusResolver create(IRpgCharacter host, AttributeSet attributes, IActionSceneModel model) {
        return new SpaceCharacterStatusResolver(base.create(host, attributes, model), host);
    }

    private class SpaceCharacterStatusResolver implements IStatusResolver {
        private static final int BREATH_INTERVAL = 1000;
        private int lastBreath = 0;

        private final IRpgCharacter host;
        private final IStatusResolver base;

        public SpaceCharacterStatusResolver(IStatusResolver base, IRpgCharacter character) {
            this.base = base;
            this.host = character;
        }

        @Override
        public boolean isDead() {
            return this.base.isDead();
        }

        @Override
        public IObserverRegistry getObservers() {
            return this.base.getObservers();
        }

        @Override
        public void update(int deltaTime) {
            this.base.update(deltaTime);

            World world = host.getWorld();

            if(world == null)
                return;

            GasSimulationEntity sim = world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);

            if(sim == null)
                return;

            lastBreath += deltaTime;

            if(lastBreath > BREATH_INTERVAL) {
                lastBreath = 0;
                Vector2D location = host.getBody().getLocation().getXy().round();

                System.out.println(location.x + ", " + location.y + ": ");
                Map<GasType, Float> quantity = new HashMap<>();//sim.getQuantity(location);
                for(Map.Entry<GasType, Float> e : quantity.entrySet())
                    System.out.println("\t" + e.getKey().name() + ", " + e.getValue());
            }
        }

        @Override
        public IActionSceneModel decorate(IActionSceneModel subject) {
            return this.base.decorate(subject);
        }
    }
}