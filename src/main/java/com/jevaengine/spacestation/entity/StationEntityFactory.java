/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;
import com.jevaengine.spacestation.entity.atmos.*;
import com.jevaengine.spacestation.entity.network.*;
import com.jevaengine.spacestation.entity.power.*;
import com.jevaengine.spacestation.entity.projectile.LaserProjectile;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import com.jevaengine.spacestation.gas.GasType;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.IAssetStreamFactory.AssetStreamConstructionException;
import io.github.jevaengine.config.*;
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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jevaengine.spacestation.entity.Infrastructure.IRubbleProducer;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

		return create(entityClass, instanceName, config, configVar);
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

	public enum StationEntity {
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
		Planet(Planet.class, "planet", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					PlanetDeclaration decl = auxConfig.getValue(PlanetDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new Planet(instanceName, model);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		Wormhole(Wormhole.class, "wormhole", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					WormholeDeclaration decl = auxConfig.getValue(WormholeDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new Wormhole(instanceName, model, decl.link);
				} catch (ISceneModelFactory.SceneModelConstructionException | ValueSerializationException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkWire(com.jevaengine.spacestation.entity.network.NetworkWire.class, "networkWire", new EntityBuilder() {
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

					return new InteractableDoor(model, instanceName, decl.isOpen, decl.isLocked);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkInterfaceController(com.jevaengine.spacestation.entity.network.NetworkInterfaceController.class, "networkInterfaceController", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkInterfaceControllerDeclaration decl = auxConfig.getValue(NetworkInterfaceControllerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkInterfaceController(instanceName, model, decl.ipAddress);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		LiquidPipe(com.jevaengine.spacestation.entity.atmos.LiquidPipe.class, "liquidPipe", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					LiquidPipeDeclaration decl = auxConfig.getValue(LiquidPipeDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new LiquidPipe(instanceName, model, decl.network);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		LiquidTank(com.jevaengine.spacestation.entity.atmos.LiquidTank.class, "liquidTank", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					LiquidTankDeclaration decl = auxConfig.getValue(LiquidTankDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					LiquidTank tank = new LiquidTank(instanceName, model);

					if (decl.gas != null) {
						GasType t = GasType.fromName(decl.gas);

						tank.add(t, decl.mols);
					}

					return tank;
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		ElectricMotor(com.jevaengine.spacestation.entity.power.ElectricMotor.class, "electricMotor", new EntityBuilder() {
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
		FuelChamber(com.jevaengine.spacestation.entity.power.FuelChamber.class, "fuelChamber", new EntityBuilder() {
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
		GasEngine(com.jevaengine.spacestation.entity.power.GasEngine.class, "gasEngine", new EntityBuilder() {
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
		Capacitor(com.jevaengine.spacestation.entity.power.Capacitor.class, "capacitor", new EntityBuilder() {
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
		Alternator(com.jevaengine.spacestation.entity.power.Alternator.class, "alternator", new EntityBuilder() {
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
					final InfrastructureDeclaration decl = auxConfig.getValue(InfrastructureDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					IRubbleProducer rubbleProducer = null;
					if(decl.produce != null) {
						rubbleProducer = () -> {
							try {
								IItem item = entityFactory.m_itemFactory.create(new URI(decl.produce));
								return new ItemDrop(item);
							} catch (ItemContructionException | URISyntaxException ex) {
								m_logger.error("Unable to produce infrastructure rubble.", ex);
								return null;
							}
						};
					}

					return new Infrastructure(instanceName, model, true, !decl.blocking, decl.type, decl.isAirTight, decl.isTransparent, decl.heatConductivity, decl.baseDamageMapping, decl.damageMultiplierMapping, decl.hitpoints, decl.hitpointsAnimationMapping, rubbleProducer, decl.consumes);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		GasVent(com.jevaengine.spacestation.entity.atmos.GasVent.class, "gasVent", new EntityBuilder() {
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
		LiquidPump(com.jevaengine.spacestation.entity.atmos.LiquidPump.class, "liquidPump", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					LiquidPumpDeclaration decl = auxConfig.getValue(LiquidPumpDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new LiquidPump(instanceName, model, decl.pressure, decl.volumePerSecond);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkAirQualitySensor(com.jevaengine.spacestation.entity.network.NetworkAirQualitySensor.class, "networkAirQualitySensor", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkAirQualitySensorDeclaration decl = auxConfig.getValue(NetworkAirQualitySensorDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkAirQualitySensor(instanceName, model, decl.ipAddress, decl.invert);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkValve(com.jevaengine.spacestation.entity.network.NetworkValve.class, "networkValve", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkValveDeclaration decl = auxConfig.getValue(NetworkValveDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkValve(instanceName, model, decl.ipAddress, decl.isOpen);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		PressureCollapseValve(PressureCollapseValve.class, "pressureCollapseValve", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					PressureCollapseValveDeclaration decl = auxConfig.getValue(PressureCollapseValveDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new PressureCollapseValve(instanceName, model, decl.collapsePressure);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		ItemLocker(ItemLocker.class, "itemLocker", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					ItemLockerDeclaration decl = auxConfig.getValue(ItemLockerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new ItemLocker(instanceName, model, decl.capacity);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkPowerMeter(NetworkPowerMeter.class, "networkPowerMeter", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkPowerMeterDeclaration decl = auxConfig.getValue(NetworkPowerMeterDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkPowerMeter(instanceName, model, decl.ipAddress);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkToggleControl(NetworkToggleControl.class, "networkToggleControl", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkToggleControlDeclaration decl = auxConfig.getValue(NetworkToggleControlDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkToggleControl(instanceName, model, decl.ipAddress, decl.destAddress, decl.isOn);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkDoorController(NetworkDoorController.class, "networkDoorController", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws IEntityFactory.EntityConstructionException {
				try {
					NetworkDoorControllerDeclaration decl = auxConfig.getValue(NetworkDoorControllerDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkDoorController(instanceName, model, decl.ipAddress);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new IEntityFactory.EntityConstructionException(e);
				}
			}
		}),
		NetworkBinarySignalInverter(NetworkBinarySignalInverter.class, "networkBinarySignalInverter", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws EntityConstructionException {
				try {
					NetworkBinarySignalInverterDeclaration decl = auxConfig.getValue(NetworkBinarySignalInverterDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new NetworkBinarySignalInverter(instanceName, model);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new EntityConstructionException(e);
				}
			}
		}),
		LaserProjectile(com.jevaengine.spacestation.entity.projectile.LaserProjectile.class, "laserProjetile", new EntityBuilder() {
			@Override
			public IEntity create(StationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws EntityConstructionException {
				try {
					LaserProjectileDeclaration decl = auxConfig.getValue(LaserProjectileDeclaration.class);
					IAnimationSceneModel model = entityFactory.m_animationSceneModelFactory.create(context.resolve(decl.model));

					return new LaserProjectile(instanceName, model, decl.damage, decl.impactRadius, decl.speed, decl.life);
				} catch (ValueSerializationException | SceneModelConstructionException e) {
					throw new EntityConstructionException(e);
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

	public static final class PlanetDeclaration implements ISerializable {

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

	public static final class WormholeDeclaration implements ISerializable {

		public String model;
		public String link;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("link").setValue(link);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				link = source.getChild("link").getValue(String.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}


	public static final class LaserProjectileDeclaration implements ISerializable {

		public String model;
		public float speed;
		public int life;
		public DamageDescription damage;
		public float impactRadius;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("speed").setValue(speed);
			target.addChild("life").setValue(life);
			target.addChild("damage").setValue(damage);
			target.addChild("impactRadius").setValue(impactRadius);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				speed = source.getChild("speed").getValue(Double.class).floatValue();
				life = source.getChild("life").getValue(Integer.class);
				damage = source.getChild("damage").getValue(DamageDescription.class);
				impactRadius = source.getChild("impactRadius").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}


	public static final class NetworkBinarySignalInverterDeclaration implements ISerializable {

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
		public boolean isTransparent = false;
		public float heatConductivity = 0;
		private String produce = null;
		public boolean consumes = true;

		private int hitpoints = 0;

		public Map<DamageCategory, Integer> baseDamageMapping = new HashMap<>();
		public Map<DamageSeverity, Integer> damageMultiplierMapping = new HashMap<>();

		public Map<Integer, String> hitpointsAnimationMapping = new HashMap<>();

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("type").setValue(type);
            target.addChild("blocking").setValue(blocking);
			target.addChild("isAirTight").setValue(isAirTight);
			target.addChild("heatConductivity").setValue(heatConductivity);
			target.addChild("isTransparent").setValue(isTransparent);
			target.addChild("hitpoints").setValue(hitpoints);
			target.addChild("consumes").setValue(consumes);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				hitpoints = source.getChild("hitpoints").getValue(Integer.class);
				type = source.getChild("type").getValues(String[].class);
				blocking = source.getChild("blocking").getValue(Boolean.class);
				heatConductivity = source.getChild("heatConductivity").getValue(Double.class).floatValue();

				if(source.childExists("isAirTight"))
					isAirTight = source.getChild("isAirTight").getValue(Boolean.class);
				if(source.childExists("isTransparent"))
					isTransparent = source.getChild("isTransparent").getValue(Boolean.class);

				if(source.childExists("consumes"))
					consumes = source.getChild("consumes").getValue(Boolean.class);

				if(source.childExists("damage")) {
					IImmutableVariable damage = source.getChild("damage");

					if(damage.childExists("produce"))
						produce = damage.getChild("produce").getValue(String.class);

					if(damage.childExists("base")) {
						IImmutableVariable baseDamage = damage.getChild("base");
						for(DamageCategory c : DamageCategory.values()) {
							if(baseDamage.childExists(c.toString())) {
								int base = baseDamage.getChild(c.toString()).getValue(Integer.class);
								baseDamageMapping.put(c, base);
							}
						}
					}

					if(damage.childExists("multiplier")) {
						IImmutableVariable multiplier = damage.getChild("multiplier");
						for(DamageSeverity s : DamageSeverity.values()) {
							if(multiplier.childExists(s.toString())) {
								int m = multiplier.getChild(s.toString()).getValue(Integer.class);
								damageMultiplierMapping.put(s, m);
							}
						}
					}


					if(damage.childExists("animation")) {
						IImmutableVariable animations = damage.getChild("animation");
						for(String a : animations.getChildren()) {
							int hp = animations.getChild(a).getValue(Integer.class);
							hitpointsAnimationMapping.put(hp, a);
						}
					}
				}
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


	public static final class NetworkPowerMeterDeclaration implements ISerializable {
		public String model;
		public int ipAddress;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("ipAddress").setValue(ipAddress);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				ipAddress = source.getChild("ipAddress").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class NetworkAirQualitySensorDeclaration implements ISerializable {

		public String model;
		public int ipAddress = 0;
		public boolean invert = false;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("ipAddress").setValue(ipAddress);
			target.addChild("invert").setValue(invert);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);

				if(source.childExists("ipAddress"))
					ipAddress = source.getChild("ipAddress").getValue(Integer.class);

				if(source.childExists("invert"))
					invert = source.getChild("invert").getValue(Boolean.class);

			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}


	public static final class NetworkToggleControlDeclaration implements ISerializable {

		public String model;
		public int ipAddress = 0;
		public int destAddress = 0;
		public boolean isOn = true;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("ipAddress").setValue(ipAddress);
			target.addChild("destAddress").setValue(destAddress);
			target.addChild("isOn").setValue(isOn);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);

				if(source.childExists("ipAddress"))
					ipAddress = source.getChild("ipAddress").getValue(Integer.class);

				if(source.childExists("destAddress"))
					destAddress = source.getChild("destAddress").getValue(Integer.class);

				if(source.childExists("isOn"))
					isOn = source.getChild("isOn").getValue(Boolean.class);

			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}


	public static final class NetworkDoorControllerDeclaration implements ISerializable {

		public String model;
		public int ipAddress = 0;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("ipAddress").setValue(ipAddress);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);

				if(source.childExists("ipAddress"))
					ipAddress = source.getChild("ipAddress").getValue(Integer.class);

			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}


	public static final class NetworkValveDeclaration implements ISerializable {

		public String model;
		public int ipAddress = 0;
		public boolean isOpen = true;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("ipAddress").setValue(ipAddress);
			target.addChild("isOpen").setValue(isOpen);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);

				if(source.childExists("ipAddress"))
					ipAddress = source.getChild("ipAddress").getValue(Integer.class);

				if(source.childExists("isOpen"))
					isOpen = source.getChild("isOpen").getValue(Boolean.class);

			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class PressureCollapseValveDeclaration implements ISerializable {
		public String model;
		public float collapsePressure = 0;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("collapsePressure").setValue(collapsePressure);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				collapsePressure = source.getChild("collapsePressure").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class ItemLockerDeclaration implements ISerializable {
		public String model;
		public int capacity;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("capacity").setValue(capacity);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				capacity = source.getChild("capacity").getValue(Integer.class);
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}


	public static final class NetworkInterfaceControllerDeclaration implements ISerializable {
		public String model;
		public int ipAddress = 0;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("ipAddress").setValue(ipAddress);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);

				if(source.childExists("ipAddress"))
					ipAddress = source.getChild("ipAddress").getValue(Integer.class);
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
		public float pressure;
		public float volumePerSecond;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("pressure").setValue(pressure);
			target.addChild("volumePerSecond").setValue(volumePerSecond);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				pressure = source.getChild("pressure").getValue(Double.class).floatValue();
				volumePerSecond = source.getChild("volumePerSecond").getValue(Double.class).floatValue();
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class LiquidPipeDeclaration implements ISerializable {
		public String model;
		public GasSimulationNetwork network;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(network.name());
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);
				network = GasSimulationNetwork.valueOf(source.getChild("network").getValue(String.class));
			} catch (NoSuchChildVariableException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}

	public static final class LiquidTankDeclaration implements ISerializable {

		public String model;
		public String gas;
		public float mols;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("model").setValue(model);
			target.addChild("gas").setValue(gas);
			target.addChild("mols").setValue(mols);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				model = source.getChild("model").getValue(String.class);

				if (source.childExists("gas")) {
					gas = source.getChild("gas").getValue(String.class);
					mols = source.getChild("mols").getValue(Double.class).floatValue();
				}
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
