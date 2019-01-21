/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.*;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public class LiquidTank extends WiredDevice implements ILiquidCarrier {
	private final float TANK_VOLUME = 100;

	private final int MAX_CONNECTIONS = 1;
	
	private final IImmutableSceneModel m_model;

	private final Map<GasType, Float> m_pendingGas = new HashMap<>();

	private World m_world = null;
	private GasSimulationEntity m_sim = null;

	private final GasSimulationNetwork m_simNetwork = GasSimulationNetwork.PipeA;

	public LiquidTank(String name, IImmutableSceneModel model) {
		super(name, false);
		m_model = model;
	}

	@Override
	public GasSimulationNetwork getNetwork() {
		return m_simNetwork;
	}

	@Override
	public Map<Vector2D, GasSimulationNetwork> getLinks() {
		HashMap<Vector2D, GasSimulationNetwork> links = new HashMap<>();

		ILiquidCarrier connection = getConnection();

		if(connection != null && connection.getNetwork() != this.m_simNetwork)
			links.put(connection.getBody().getLocation().getXy().round(), connection.getNetwork());

		return links;
	}

	public void add(GasType t, float amount) {
		float current = m_pendingGas.containsKey(t) ? m_pendingGas.get(t) : 0;

		current += amount;

		m_pendingGas.put(t, current);
	}

	@Override
	public float getVolume() {
		return TANK_VOLUME;
	}

	@Override
	public boolean isFreeFlow() {
		return true;
	}

	private ILiquidCarrier getConnection() {
		List<ILiquidCarrier> connections = getConnections(ILiquidCarrier.class);
		
		if(connections.isEmpty())
			return null;
		
		return connections.get(0);
	}

	@Override
	protected void connectionChanged() {
		m_observers.raise(ILiquidCarrierObserver.class).linksChanged();
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return (d instanceof LiquidPipe || d instanceof LiquidPump) && getConnections().size() < MAX_CONNECTIONS;
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	@Override
	public void update(int delta) {
		if(m_world != getWorld()) {
			m_world = getWorld();
			m_sim = m_world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
		}

		if(m_sim == null)
			return;

		if(m_pendingGas.isEmpty())
			return;

		GasMetaData data = new GasMetaData(m_pendingGas, 0);
		Vector2D loc = this.getBody().getLocation().getXy().round();
		m_sim.produce(m_simNetwork, loc, data);
		m_pendingGas.clear();
	}
}
