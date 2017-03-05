/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

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

					return new ConsoleInterface(instanceName, model, decl.nodeName);
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
					
					return new Dcpu(instanceName, model, firmware, true, decl.nodeName);
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
		}),
		AreaNetworkController(AreaNetworkController.class, "areaNetworkController", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					AreaNetworkControllerDeclaration decl = auxConfig.getValue(AreaNetworkControllerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new AreaNetworkController(instanceName, model, entityFactory.m_routeFactory, decl.netlist, decl.ipAddress);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkInterfaceController(NetworkInterfaceController.class, "networkInterfaceController", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkInterfaceControllerDeclaration decl = auxConfig.getValue(NetworkInterfaceControllerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkInterfaceController(instanceName, model, decl.nodeName);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		ProgrammableIntervalTimer(ProgrammableIntervalTimer.class, "programmableIntervalTimer", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					ProgrammableIntervalTimerDeclaration decl = auxConfig.getValue(ProgrammableIntervalTimerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new ProgrammableIntervalTimer(instanceName, model, decl.nodeName);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkDoor(NetworkDoor.class, "networkDoor", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkDoorDeclaration decl = auxConfig.getValue(NetworkDoorDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkDoor(model, instanceName, true, decl.nodeName);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		DoorController(DoorController.class, "doorController", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					DoorControllerDeclaration decl = auxConfig.getValue(DoorControllerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new DoorController(instanceName, model, decl.nodeName, decl.port);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		LiquidPipe(LiquidPipe.class, "liquidPipe", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					LiquidPipeDeclaration decl = auxConfig.getValue(LiquidPipeDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new LiquidPipe(instanceName, model, decl.radius);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		LiquidTank(LiquidTank.class, "liquidTank", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					LiquidTankDeclaration decl = auxConfig.getValue(LiquidTankDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new LiquidTank(instanceName, model, decl.radius, decl.height);
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
		public String nodeName;
		
		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("firmware").setValue(firmware);
			target.addChild("nodeName").setValue(nodeName);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				firmware = source.getChild("firmware").getValue(String.class);
				nodeName = source.getChild("nodeName").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class ConsoleInterfaceDeclaration implements ISerializable {

		public String nodeName;
		public String model;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("nodeName").setValue(nodeName);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				nodeName = source.getChild("nodeName").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class NetworkInterfaceControllerDeclaration implements ISerializable {

		public String nodeName;
		public String model;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("nodeName").setValue(nodeName);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				nodeName = source.getChild("nodeName").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	
	public static final class ProgrammableIntervalTimerDeclaration implements ISerializable {

		public String nodeName;
		public String model;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("nodeName").setValue(nodeName);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				nodeName = source.getChild("nodeName").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class NetworkDoorDeclaration implements ISerializable {

		public String nodeName;
		public String model;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("nodeName").setValue(nodeName);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				nodeName = source.getChild("nodeName").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class DoorControllerDeclaration implements ISerializable {

		public String nodeName;
		public String model;
		public int port;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("nodeName").setValue(nodeName);
			target.addChild("port").setValue(port);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				nodeName = source.getChild("nodeName").getValue(String.class);
				port = source.getChild("port").getValue(Integer.class);
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
	
	public static final class AreaNetworkControllerDeclaration implements ISerializable {
		public String model;
		public String netlist;
		public int ipAddress;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("netlist").setValue(netlist);
			target.addChild("ipAddress").setValue(ipAddress);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				netlist = source.getChild("netlist").getValue(String.class);
				ipAddress = source.getChild("ipAddress").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class LiquidPipeDeclaration implements ISerializable {
		public String model;
		public float radius;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("radius").setValue(radius);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				radius = source.getChild("radius").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class LiquidTankDeclaration implements ISerializable {
		public String model;
		public float radius;
		public float height;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("radius").setValue(radius);
			target.addChild("height").setValue(height);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				radius = source.getChild("radius").getValue(Double.class).floatValue();
				height = source.getChild("height").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
}
