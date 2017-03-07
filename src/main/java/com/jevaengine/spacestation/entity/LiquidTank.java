/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.liquid.ILiquid;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public final class LiquidTank extends WiredDevice implements ILiquidCarrier {
	private final int MAX_CONNECTIONS = 1;
	
	private final IImmutableSceneModel m_model;
	
	private final Map<ILiquid, Float> m_contents = new HashMap<>();
	
	private final float m_radius;
	private final float m_height;
	
	public LiquidTank(String name, IImmutableSceneModel model, float radius, float height) {
		super(name, false);
		m_model = model;
		m_radius = radius;
		m_height = height;
	}
	
	private ILiquidCarrier getConnection() {
		List<ILiquidCarrier> connections = getConnections(ILiquidCarrier.class);
		
		if(connections.isEmpty())
			return null;
		
		return connections.get(0);
	}
	
	private Map<ILiquid, Float> getSample(Map<ILiquid, Float> pool, float quantity) {
		float poolVolume = 0;
		
		for(Map.Entry<ILiquid, Float> e : pool.entrySet())
			poolVolume += e.getValue();
		
		float sampleSize = Math.min(quantity, poolVolume);
		float totalLiquid = poolVolume;
		
		if(totalLiquid == 0)
			return new HashMap<>();
		
		Map<ILiquid, Float> sample = new HashMap<>();

		for (Map.Entry<ILiquid, Float> liquid : pool.entrySet()) {
			sample.put(liquid.getKey(), (liquid.getValue() / totalLiquid) * sampleSize);
		}

		return sample;
	}
		
	@Override
	protected void connectionChanged() { }

	@Override
	protected boolean canConnectTo(IDevice d) {
		return d instanceof LiquidPipe && getConnections().size() < MAX_CONNECTIONS;
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
		ILiquidCarrier connection = getConnection();
		
		if(connection == null)
			return;
		
		float room = connection.getCapacityVolume() - connection.getLiquidVolume();
		
		float amountFlow = room * (delta / 1000.0F) * calculateFlowRate();
		
		Map<ILiquid, Float> push = getSample(m_contents, amountFlow);
		
		List<ILiquidCarrier> cause = new ArrayList<>();
		cause.add(this);
		
		float added = connection.add(cause, push);
		
		for(Map.Entry<ILiquid, Float> e : m_contents.entrySet()) {
			m_contents.put(e.getKey(), e.getValue() - added);
		}
	}
	
	private float calculateFlowRate() {
		float totalLiquid = getLiquidVolume();
		float flowRate = 0;

		for (Map.Entry<ILiquid, Float> liquid : m_contents.entrySet()) {
			flowRate += (liquid.getValue() / totalLiquid) * liquid.getKey().getRateOfFlow();
		}

		return flowRate;
	}

	@Override
	public float getLiquidVolume() {
		float volume = 0;
		
		for(Map.Entry<ILiquid,Float> e : m_contents.entrySet())
			volume += e.getValue();
		
		return volume;
	}

	@Override
	public float getCapacityVolume() {
		return m_height * m_radius * m_radius * (float)Math.PI * 1000; //1000 from cubic metres to litres.
	}

	@Override
	public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid) {
		if(cause.contains(this))
			return 0;
		
		cause.add(this);
		
		float maxInput = getCapacityVolume() - getLiquidVolume();
		
		float totalInput = 0;
		Map<ILiquid, Float> input = getSample(liquid, maxInput);
		
		for(Map.Entry<ILiquid, Float> e : input.entrySet()) {
			float current = m_contents.containsKey(e.getKey()) ? m_contents.get(e.getKey()) : 0;
			m_contents.put(e.getKey(), current + e.getValue());
			totalInput += e.getValue();
		}
		
		return totalInput;
	}

	@Override
	public Map<ILiquid, Float> remove(List<ILiquidCarrier> cause, float quantity) {
		Map<ILiquid, Float> removed = getSample(m_contents, quantity);
		
		for(Map.Entry<ILiquid, Float> e : removed.entrySet()) {
			float current = m_contents.get(e.getKey());
			m_contents.put(e.getKey(), current - e.getValue());
		}
		
		return removed;
	}

	@Override
	public float getSourcedLiquidVolume(List<ILiquidCarrier> requested) {
		return getLiquidVolume();
	}
	
}
