/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.config.ISerializable;
import io.github.jevaengine.config.IVariable;
import io.github.jevaengine.config.ImmutableVariableOverlay;
import io.github.jevaengine.config.NoSuchChildVariableException;
import io.github.jevaengine.config.NullVariable;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class StationEntityFactory implements IEntityFactory {

	private final Logger m_logger = LoggerFactory.getLogger(StationEntityFactory.class);
	private static final AtomicInteger m_unnamedEntityCount = new AtomicInteger();

	private final IConfigurationFactory m_configurationFactory;
	private final IAnimationSceneModelFactory m_animationSceneModelFactory;

	private final IEntityFactory m_base;

	@Inject
	public StationEntityFactory(IEntityFactory base, IConfigurationFactory configurationFactory, IAnimationSceneModelFactory animationSceneModelFactory) {
		m_base = base;
		m_configurationFactory = configurationFactory;
		m_animationSceneModelFactory = animationSceneModelFactory;
	}

	@Override
	@Nullable
	public Class<? extends IEntity> lookup(String className) {
		for (StationEntity e : StationEntity.values()) {
			if (e.getName().equals(className)) {
				return e.getEntityClass();
			}
		}

		return m_base.lookup(className);
	}

	@Override
	@Nullable
	public <T extends IEntity> String lookup(Class<T> entityClass) {
		for (StationEntity e : StationEntity.values()) {
			if (e.getEntityClass().equals(entityClass)) {
				return e.getName();
			}
		}

		return m_base.lookup(entityClass);
	}

	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName, URI config) throws IEntityFactory.EntityConstructionException {
		IImmutableVariable configVar = new NullVariable();

		try {
			configVar = m_configurationFactory.create(config);
		} catch (IConfigurationFactory.ConfigurationConstructionException e) {
			m_logger.error("Unable to insantiate configuration for entity. Using null configuration instead.", e);
		}

		return create(entityClass, instanceName, configVar);
	}

	@Override
	public IEntity create(String entityName, @Nullable String instanceName, URI config) throws IEntityFactory.EntityConstructionException {
		IImmutableVariable configVar = new NullVariable();

		try {
			configVar = m_configurationFactory.create(config);
		} catch (IConfigurationFactory.ConfigurationConstructionException e) {
			m_logger.error("Unable to insantiate configuration for entity. Using null configuration instead.", e);
		}

		return create(entityName, instanceName, configVar);
	}

	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName, IImmutableVariable config) throws IEntityFactory.EntityConstructionException {
		return create(entityClass, instanceName, URI.create(""), config);
	}

	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName) throws IEntityFactory.EntityConstructionException {
		return create(entityClass, instanceName, new NullVariable());
	}

	@Override
	public IEntity create(String entityName, @Nullable String instanceName, IImmutableVariable config) throws IEntityFactory.EntityConstructionException {
		Class<? extends IEntity> entityClass = lookup(entityName);

		if (entityClass == null) {
			throw new IEntityFactory.EntityConstructionException(instanceName, new IEntityFactory.UnsupportedEntityTypeException(entityClass));
		}

		return create(entityClass, instanceName, config);
	}

	@Override
	public IEntity create(String entityClass, @Nullable String instanceName) throws IEntityFactory.EntityConstructionException {
		return create(entityClass, instanceName, new NullVariable());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName, URI config, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {

		String configPath = config.getPath();

		IImmutableVariable varConfig = auxConfig;

		try {
			varConfig = new ImmutableVariableOverlay(varConfig,
					configPath.isEmpty() || configPath.endsWith("/") ? new NullVariable() : m_configurationFactory.create(config));
		} catch (IConfigurationFactory.ConfigurationConstructionException e) {
			m_logger.error("Error occured constructing configuration for entity, ignoring external configuration and using just aux config.", e);
		}

		for (StationEntity e : StationEntity.values()) {
			if (e.getEntityClass().equals(entityClass)) {
				return (T) e.getBuilder().create(this, instanceName == null ? this.getClass().getName() + m_unnamedEntityCount.getAndIncrement() : instanceName,
						config, varConfig);
			}
		}

		return m_base.create(entityClass, instanceName, config, auxConfig);
	}

	@Override
	public IEntity create(String entityClass, @Nullable String instanceName, URI config, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
		Class<? extends IEntity> entityClazz = lookup(entityClass);

		if (entityClazz == null) {
			throw new IEntityFactory.EntityConstructionException(instanceName, new IEntityFactory.UnsupportedEntityTypeException(entityClass));
		}

		return create(entityClazz, instanceName, config, auxConfig);
	}

	private enum StationEntity {
		Wire(Wire.class, "wire", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					WireDeclaration decl = auxConfig.getValue(WireDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));
					
					return new Wire(instanceName, model);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		SolarPanel(SolarPanel.class, "solarPanel", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					SolarPanelDeclaration decl = auxConfig.getValue(SolarPanelDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));
					
					return new SolarPanel(instanceName, model, decl.productionWatts);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		});

		private final Class<? extends IEntity> m_class;
		private final String m_name;
		private final EntityBuilder m_builder;

		StationEntity(Class<? extends IEntity> clazz, String name, EntityBuilder builder) {
			m_class = clazz;
			m_name = name;
			m_builder = builder;
		}

		public String getName() {
			return m_name;
		}

		public Class<? extends IEntity> getEntityClass() {
			return m_class;
		}

		private EntityBuilder getBuilder() {
			return m_builder;
		}

		public static abstract class EntityBuilder {

			protected final Logger m_logger = LoggerFactory.getLogger(EntityBuilder.class);

			public abstract IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException;
		}
	}

	public static final class WireDeclaration implements ISerializable {

		public String model;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class SolarPanelDeclaration implements ISerializable {
		public int productionWatts;
		public String model;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("productionWatts").setValue(productionWatts);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				productionWatts = source.getChild("productionWatts").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
}
