package com.jevaengine.spacestation;

import com.jevaengine.spacestation.gas.GasSimulationEntity;
import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.script.IScriptBuilderFactory;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.DefaultWorldFactory;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;

import javax.inject.Inject;
import java.net.URI;

public class SpaceStationFactory extends DefaultWorldFactory {
    private static final float SPACE_TEMPERATURE = 200;

    @Inject
    public SpaceStationFactory(IEngineThreadPool threadPool, IEntityFactory entityFactory, IScriptBuilderFactory scriptFactory, IConfigurationFactory configurationFactory, ISpriteFactory spriteFactory, IAudioClipFactory audioClipFactory, IPhysicsWorldFactory physicsWorldFactory, ISceneModelFactory sceneModelFactory, IWeatherFactory weatherFactory, IEffectMapFactory effectMapFactory) {
        super(threadPool, entityFactory, scriptFactory, configurationFactory, spriteFactory, audioClipFactory, physicsWorldFactory, sceneModelFactory, weatherFactory, effectMapFactory);
    }

    @Override
    protected IEntity createSceneArtifact(WorldConfiguration.SceneArtifactImportDeclaration artifactDecl, URI context) throws IEntityFactory.EntityConstructionException {
        try {
            URI createContext = context.resolve(artifactDecl.model).resolve("entity.jec");
            URI infrastructureUri = createContext.resolve("entity.jec");
            return m_entityFactory.create("infrastructure", null, createContext, m_configurationFactory.create(infrastructureUri));
        } catch (IConfigurationFactory.ConfigurationConstructionException e) {
            throw new IEntityFactory.EntityConstructionException(e);
        }
    }

    @Override
    protected World createBaseWorld(float friction, float metersPerUnit, float logicPerUnit, int worldWidthTiles, int worldHeightTiles, IWeatherFactory.IWeather weather, @Nullable URI worldScript) {
        World world = super.createBaseWorld(friction, metersPerUnit, logicPerUnit, worldWidthTiles, worldHeightTiles, weather, worldScript);
        world.addEntity(new GasSimulationEntity(SPACE_TEMPERATURE));
        return world;
    }
}