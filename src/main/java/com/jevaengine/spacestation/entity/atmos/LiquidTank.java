/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulation;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import com.jevaengine.spacestation.gas.GasType;
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

	public LiquidTank(String name, IImmutableSceneModel model) {
		super(name, false);
		m_model = model;
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
	protected void connectionChanged() { }

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

		GasSimulation.GasMetaData data = new GasSimulation.GasMetaData(m_pendingGas, 0);
		Vector2D loc = this.getBody().getLocation().getXy().round();
		m_sim.produce(GasSimulationNetwork.Pipe, loc, data);
		m_pendingGas.clear();
	}
}
