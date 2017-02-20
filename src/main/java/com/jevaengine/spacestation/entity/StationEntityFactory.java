/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.entity.dcpu.ConsoleInterface;
import com.jevaengine.spacestation.entity.dcpu.Dcpu;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.IAssetStreamFactory.AssetStreamConstructionException;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.config.ISerializable;
import io.github.jevaengine.config.IVariable;
import io.github.jevaengine.config.ImmutableVariableOverlay;
import io.github.jevaengine.config.NoSuchChildVariableException;
import io.github.jevaengine.config.NullVariable;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.RpgEntityFactory.DoorDeclaration;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.rpg.item.IItemFactory.ItemContructionException;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory.SceneModelConstructionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class StationEntityFactory implements IEntityFactory {

	private final Logger m_logger = LoggerFactory.getLogger(StationEntityFactory.class);
	private static final AtomicInteger m_unnamedEntityCount = new AtomicInteger();

	private final IAssetStreamFactory m_assetStreamFactory;
	private final IConfigurationFactory m_configurationFactory;
	private final IAnimationSceneModelFactory m_animationSceneModelFactory;
	private final IItemFactory m_itemFactory;

	private final IEntityFactory m_base;
	
	private final IRouteFactory m_routeFactory;

	@Inject
	public StationEntityFactory(IEntityFactory base, IItemFactory itemFactory, IConfigurationFactory configurationFactory, IAnimationSceneModelFactory animationSceneModelFactory, IAssetStreamFactory assetStreamFactory, IRouteFactory routeFactory) {
		m_base = base;
		m_configurationFactory = configurationFactory;
		m_animationSceneModelFactory = animationSceneModelFactory;
		m_itemFactory = itemFactory;
		m_assetStreamFactory = assetStreamFactory;
		m_routeFactory = routeFactory;
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
		PowerWire(Wire.class, "powerWire", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					WireDeclaration decl = auxConfig.getValue(WireDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new PowerWire(instanceName, model);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkWire(NetworkWire.class, "networkWire", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					WireDeclaration decl = auxConfig.getValue(WireDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkWire(instanceName, model);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		ConsoleInterface(ConsoleInterface.class, "consoleInterface", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					ConsoleInterfaceDeclaration decl = auxConfig.getValue(ConsoleInterfaceDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new ConsoleInterface(instanceName, model);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Dcpu(Dcpu.class, "dcpu", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					DcpuDeclaration decl = auxConfig.getValue(DcpuDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));
					
					InputStream firmwareStream = entityFactory.m_assetStreamFactory.create(context.resolve(decl.firmware));
					byte[] firmware = IOUtils.toByteArray(firmwareStream);
					
					return new Dcpu(instanceName, model, firmware, true);
				} catch (ISceneModelFactory.SceneModelConstructionException | IOException | AssetStreamConstructionException | ValueSerializationException e) {
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
		}),
		ItemDrop(ItemDrop.class, "itemDrop", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					ItemDropDeclaration decl = auxConfig.getValue(ItemDropDeclaration.class);
					IItem item = entityFactory.m_itemFactory.create(context.resolve(decl.item));

					return new ItemDrop(instanceName, item);
				} catch (ValueSerializationException | ItemContructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Door(Door.class, "door", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					DoorDeclaration decl = auxConfig.getValue(DoorDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new InteractableDoor(model, instanceName, decl.isOpen);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		AreaPowerController(AreaPowerController.class, "areaPowerController", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					AreaPowerControllerDeclaration decl = auxConfig.getValue(AreaPowerControllerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new AreaPowerController(instanceName, model, entityFactory.m_routeFactory);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
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

	public static final class DcpuDeclaration implements ISerializable {

		public String model;
		public String firmware;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("firmware").setValue(firmware);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				firmware = source.getChild("firmware").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class ConsoleInterfaceDeclaration implements ISerializable {

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

	public static final class ItemDropDeclaration implements ISerializable {

		public String item;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("item").setValue(item);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				item = source.getChild("item").getValue(String.class);
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
	
	
	public static final class AreaPowerControllerDeclaration implements ISerializable {

		public int productionWatts;
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
}
