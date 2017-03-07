/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.liquid.ILiquid;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public final class FuelChamber extends WiredDevice implements ILiquidCarrier {

	private final IImmutableSceneModel m_model;
	
	private final float m_capacity;
	
	private final Map<ILiquid, Float> m_contents = new HashMap<>();

	public FuelChamber(String name, IImmutableSceneModel model, float capacity) {
		super(name, false);
		m_model = model;
		m_capacity = capacity;
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
		if(!super.canConnectTo(d))
			return false;
		
		if (d instanceof GasEngine) {

			if (getConnections(GasEngine.class).size() > 0) {
				return false;
			}

			Vector2F delta = d.getBody().getLocation().difference(getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);

			return dir == Direction.YPlus;
		} else if (d instanceof ILiquidCarrier) {
			return getConnections(ILiquidCarrier.class).isEmpty();
		}
		
		return false;
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
	public void update(int delta) { }

	@Override
	public float getLiquidVolume() {
		float volume = 0;
		
		for(Map.Entry<ILiquid, Float> e : m_contents.entrySet()) {
			volume += e.getValue();
		}
		
		return volume;
	}

	@Override
	public float getSourcedLiquidVolume(List<ILiquidCarrier> requested) {
		return 0;
	}

	@Override
	public float getCapacityVolume() {
		return m_capacity;
	}

	@Override
	public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid) {
		float room = getCapacityVolume() - getLiquidVolume();
		
		float inputVolume = 0;
		
		for(Map.Entry<ILiquid, Float> e : liquid.entrySet()) {
			inputVolume += e.getValue();
		}
		
		float accepted = Math.min(room, inputVolume);
		
		Map<ILiquid, Float> intake = getSample(liquid, accepted);
		
		for(Map.Entry<ILiquid, Float> e : intake.entrySet()) {
			float current = m_contents.containsKey(e.getKey()) ? m_contents.get(e.getKey()) : 0;
			m_contents.put(e.getKey(), current + e.getValue());
		}
		
		return accepted;
	}

	@Override
	public Map<ILiquid, Float> remove(List<ILiquidCarrier> cause, float quantity) {

		Map<ILiquid, Float> out = getSample(m_contents, quantity);
		
		for(Map.Entry<ILiquid, Float> e : out.entrySet()) {
			float current = m_contents.containsKey(e.getKey()) ? m_contents.get(e.getKey()) : 0;
			m_contents.put(e.getKey(), current - e.getValue());
		}
		
		return out;
	}

}
