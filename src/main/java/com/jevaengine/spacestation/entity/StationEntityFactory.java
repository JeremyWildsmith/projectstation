/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.gas.GasType;
import com.jevaengine.spacestation.liquid.GasLiquid;
import com.jevaengine.spacestation.liquid.ILiquid;
import com.jevaengine.spacestation.liquid.Liquid;
import com.jevaengine.spacestation.liquid.Liquid.NoSuchLiquidException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
		return create(entityName, instanceName, config, new NullVariable());
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

					return new LiquidPipe(instanceName, model, decl.capacity);
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

					LiquidTank tank = new LiquidTank(instanceName, model, decl.capacity);

					if (decl.defaultLiquid != null) {
						ILiquid l = null;
						if(decl.isGas)
							l = new GasLiquid(GasType.fromName(decl.defaultLiquid));
						else
							l = Liquid.fromName(decl.defaultLiquid);

						Map<ILiquid, Float> add = new HashMap<>();
						add.put(l, decl.defaultVolume);
						tank.add(new ArrayList<ILiquidCarrier>(), add);
					}

					return tank;
				} catch (ValueSerializationException | SceneModelConstructionException | NoSuchLiquidException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		ElectricMotor(ElectricMotor.class, "electricMotor", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					ElectricMotorDeclaration decl = auxConfig.getValue(ElectricMotorDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new ElectricMotor(instanceName, model, decl.rpm, decl.powerConsumptionWatts);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		FuelChamber(FuelChamber.class, "fuelChamber", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					FuelChamberDeclaration decl = auxConfig.getValue(FuelChamberDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new FuelChamber(instanceName, model, decl.capacity);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		GasEngine(GasEngine.class, "gasEngine", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					GasEngineDeclaration decl = auxConfig.getValue(GasEngineDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new GasEngine(instanceName, model, decl.startupTime, decl.starterRpm, decl.gasConsumption, decl.outputRpm);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		ApcJunction(ApcJunction.class, "apcJunction", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					ApcJunctionDeclaration decl = auxConfig.getValue(ApcJunctionDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new ApcJunction(instanceName, model);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		PowerSwitch(PowerSwitch.class, "powerSwitch", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					PowerSwitchDeclaration decl = auxConfig.getValue(PowerSwitchDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new PowerSwitch(instanceName, model);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Diode(Diode.class, "diode", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					DiodeDeclaration decl = auxConfig.getValue(DiodeDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new Diode(instanceName, model);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Capacitor(Capacitor.class, "capacitor", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					CapacitorDeclaration decl = auxConfig.getValue(CapacitorDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new Capacitor(instanceName, model, decl.charge);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Alternator(Alternator.class, "alternator", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					AlternatorDeclaration decl = auxConfig.getValue(AlternatorDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new Alternator(instanceName, model, decl.wattPerRpm);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Infrastructure(Infrastructure.class, "infrastructure", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					InfrastructureDeclaration decl = auxConfig.getValue(InfrastructureDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new Infrastructure(model, true, !decl.blocking, decl.type, decl.isAirTight);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		GasVent(GasVent.class, "gasVent", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					GasVentDeclaration decl = auxConfig.getValue(GasVentDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new GasVent(instanceName, model);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		LiquidPump(LiquidPump.class, "liquidPump", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					LiquidPumpDeclaration decl = auxConfig.getValue(LiquidPumpDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new LiquidPump(instanceName, model);
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

	public static final class GasVentDeclaration implements ISerializable {

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


	public static final class InfrastructureDeclaration implements ISerializable {
		public String model;
		public String[] type;
		public boolean blocking = false;
		public boolean isAirTight = false;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("type").setValue(type);
            target.addChild("blocking").setValue(blocking);
            target.addChild("isAirTight").setValue(isAirTight);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				type = source.getChild("type").getValues(String[].class);
                blocking = source.getChild("blocking").getValue(Boolean.class);

                if(source.childExists("isAirTight"))
                    isAirTight = source.getChild("isAirTight").getValue(Boolean.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class PowerSwitchDeclaration implements ISerializable {

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


	public static final class LiquidPumpDeclaration implements ISerializable {

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
		public float capacity;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("capacity").setValue(capacity);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				capacity = source.getChild("capacity").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class LiquidTankDeclaration implements ISerializable {

		public String model;
		public float capacity;
		public String defaultLiquid;
		public float defaultVolume;
		public boolean isGas = false;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("capacity").setValue(capacity);
			target.addChild("defaultLiquid").setValue(defaultLiquid);
			target.addChild("defaultVolume").setValue(defaultVolume);
			target.addChild("isGas").setValue(isGas);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				capacity = source.getChild("capacity").getValue(Double.class).floatValue();

				if (source.childExists("defaultLiquid")) {
					defaultLiquid = source.getChild("defaultLiquid").getValue(String.class);
					defaultVolume = source.getChild("defaultVolume").getValue(Double.class).floatValue();
				}

				if(source.childExists("isGas"))
					isGas = source.getChild("isGas").getValue(Boolean.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class ElectricMotorDeclaration implements ISerializable {

		public String model;
		public int powerConsumptionWatts;
		public int rpm;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("powerConsumptionWatts").setValue(powerConsumptionWatts);
			target.addChild("rpm").setValue(rpm);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				powerConsumptionWatts = source.getChild("powerConsumptionWatts").getValue(Integer.class);
				rpm = source.getChild("rpm").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class FuelChamberDeclaration implements ISerializable {

		public String model;
		public float capacity;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("capacity").setValue(capacity);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				capacity = source.getChild("capacity").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class GasEngineDeclaration implements ISerializable {

		public String model;
		public int startupTime;
		public int starterRpm;
		public int outputRpm;
		public float gasConsumption;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("startupTime").setValue(startupTime);
			target.addChild("starterRpm").setValue(starterRpm);
			target.addChild("outputRpm").setValue(outputRpm);
			target.addChild("gasConsumption").setValue(gasConsumption);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				startupTime = source.getChild("startupTime").getValue(Integer.class);
				starterRpm = source.getChild("starterRpm").getValue(Integer.class);
				outputRpm = source.getChild("outputRpm").getValue(Integer.class);
				gasConsumption = source.getChild("gasConsumption").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class ApcJunctionDeclaration implements ISerializable {

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
	
	public static final class DiodeDeclaration implements ISerializable {
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
	
	public static final class CapacitorDeclaration implements ISerializable {
		public String model;
		public int charge;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("charge").setValue(charge);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				charge = source.getChild("charge").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
	
	public static final class AlternatorDeclaration implements ISerializable {
		public String model;
		public int wattPerRpm;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("wattPerRpm").setValue(wattPerRpm);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				wattPerRpm = source.getChild("wattPerRpm").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
}
