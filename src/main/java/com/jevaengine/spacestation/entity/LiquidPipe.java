/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public final class LiquidPipe extends BasicDevice implements ILiquidCarrier {

	private final int MAX_CONNECTIONS = 2;
	private final float PIPE_WIDTH_METRES = 1;

	private final IAnimationSceneModel m_model;

	private int m_radius;

	private final HashMap<ILiquid, Float> m_containedLiquids = new HashMap<>();

	public LiquidPipe(String name, IAnimationSceneModel model) {
		super(name, true);
		m_model = model;
	}

	private void updateModel() {

		HashSet<Direction> directions = new HashSet<>();
		List<String> directionNames = new ArrayList<>();

		for (IDevice w : getConnections()) {
			Vector3F delta = w.getBody().getLocation().difference(getBody().getLocation());
			Direction d = Direction.fromVector(delta.getXy());
			if (d != Direction.Zero && !d.isDiagonal()) {
				directions.add(d);
			}
		}

		for (Direction d : directions) {
			directionNames.add(d.toString());
		}

		Collections.sort(directionNames);

		String animationName = String.join(",", directionNames);

		if (animationName.isEmpty()) {
			animationName = "idle";
		}

		m_model.getAnimation(animationName).setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);
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
		Map<ILiquidCarrier, Float> relativeDifferences = new HashMap<>();
		float totalDifference = 0;

		for (ILiquidCarrier c : this.getConnections(ILiquidCarrier.class)) {
			float volumeDifference = this.getLiquidVolume() - c.getLiquidVolume();
			if (volumeDifference > 0) {
				relativeDifferences.put(c, volumeDifference);
				totalDifference += volumeDifference;
			}
		}

		float amountFlow = calculateFlowRate() * (delta / 1000.0F);

		float amountRemoved = 0;
		for (Map.Entry<ILiquidCarrier, Float> c : relativeDifferences.entrySet()) {
			float quantityPush = (c.getValue() / totalDifference) * amountFlow;

			Map<ILiquid, Float> sample = getSample(quantityPush);

			List<ILiquidCarrier> cause = new ArrayList<>();
			cause.add(this);
			amountRemoved += c.getKey().add(cause, sample);

		}

		Map<ILiquid, Float> removeSample = getSample(amountRemoved);

		for (Map.Entry<ILiquid, Float> e : removeSample.entrySet()) {
			float current = m_containedLiquids.get(e.getKey());
			m_containedLiquids.put(e.getKey(), current - e.getValue());
		}

	}

	@Override
	protected void connectionChanged() {
		updateModel();
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return getConnections().size() < MAX_CONNECTIONS || !(d instanceof ILiquidCarrier);
	}

	@Override
	public float getLiquidVolume() {
		float total = 0;

		for (Map.Entry<ILiquid, Float> liquid : m_containedLiquids.entrySet()) {
			total += liquid.getValue();
		}

		return total;
	}

	@Override
	public float getCapacityVolume() {
		return (float) Math.PI * m_radius * m_radius * PIPE_WIDTH_METRES;
	}

	private float calculateFlowRate() {
		float totalLiquid = getLiquidVolume();
		float flowRate = 0;

		for (Map.Entry<ILiquid, Float> liquid : m_containedLiquids.entrySet()) {
			flowRate += (liquid.getValue() / totalLiquid) * liquid.getKey().getRateOfFlow();
		}

		return flowRate;
	}

	private Map<ILiquid, Float> getSample(float quantity) {
		return getSample(m_containedLiquids, quantity);
	}

	private Map<ILiquid, Float> getSample(Map<ILiquid, Float> pool, float quantity) {
		float sampleSize = Math.min(quantity, getLiquidVolume());
		float totalLiquid = getLiquidVolume();
		Map<ILiquid, Float> sample = new HashMap<>();

		for (Map.Entry<ILiquid, Float> liquid : pool.entrySet()) {
			sample.put(liquid.getKey(), (liquid.getValue() / totalLiquid) * sampleSize);
		}

		return sample;
	}

	private float createRoom(List<ILiquidCarrier> cause, float volumeNeeded) {

		List<ILiquidCarrier> available = new ArrayList<>();
		List<ILiquidCarrier> accepted = new ArrayList<>();

		accepted.addAll(getConnections(ILiquidCarrier.class));

		float volumeAcquired = 0;

		while (!available.isEmpty()) {
			available.clear();
			available.addAll(accepted);
			accepted.clear();

			Map<ILiquid, Float> sample = getSample((volumeNeeded - volumeAcquired) / available.size());
			for (ILiquidCarrier c : available) {
				
				float amountAccepted = c.add(new ArrayList<>(cause), sample);

				volumeAcquired += amountAccepted;
				if (amountAccepted > 0.000001F) {
					accepted.add(c);
				}
				
				for(Map.Entry<ILiquid, Float> e : sample.entrySet()) {
					float current = m_containedLiquids.get(e.getKey());
					m_containedLiquids.put(e.getKey(), current - amountAccepted);
				}
			}
		}

		return volumeAcquired;
	}

	private float suckLiquid(List<ILiquidCarrier> cause, float volumeNeeded) {

		List<ILiquidCarrier> available = new ArrayList<>();
		List<ILiquidCarrier> accepted = new ArrayList<>();

		accepted.addAll(getConnections(ILiquidCarrier.class));

		float volumeAcquired = 0;

		while (!available.isEmpty()) {
			available.clear();
			available.addAll(accepted);
			accepted.clear();

			for (ILiquidCarrier c : available) {
				float amountTaken = 0;

				Map<ILiquid, Float> sample = c.remove(new ArrayList<>(cause), (volumeNeeded - volumeAcquired) / available.size());

				for(Map.Entry<ILiquid, Float> e : sample.entrySet())
					amountTaken += e.getValue();

				volumeAcquired += amountTaken;
				
				if (amountTaken > 0.000001F) {
					accepted.add(c);
				}
				
				for(Map.Entry<ILiquid, Float> e : sample.entrySet()) {
					float current = m_containedLiquids.containsKey(e.getKey()) ? m_containedLiquids.get(e.getKey()) : 0;
					m_containedLiquids.put(e.getKey(), current + amountTaken);
				}
			}
		}

		return volumeAcquired;
	}

	@Override
	public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid) {
		if (cause.contains(this)) {
			return 0;
		}

		cause.add(this);

		float pushVolume = 0;

		for (Map.Entry<ILiquid, Float> e : liquid.entrySet()) {
			pushVolume += e.getValue();
		}

		float room = getCapacityVolume() - getLiquidVolume();

		if (room < pushVolume) {
			pushVolume = createRoom(cause, pushVolume - room) + room;
		}

		Map<ILiquid, Float> pushSample = getSample(liquid, pushVolume);

		for (Map.Entry<ILiquid, Float> l : pushSample.entrySet()) {
			float current = 0;

			if (m_containedLiquids.containsKey(l.getKey())) {
				current = m_containedLiquids.get(l.getKey());
			}

			m_containedLiquids.put(l.getKey(), l.getValue() + current);
		}

		return pushVolume;
	}

	@Override
	public Map<ILiquid, Float> remove(List<ILiquidCarrier> cause, float quantity) {
		Map<ILiquid, Float> totalRemoved = new HashMap<>();
		
		if (cause.contains(this)) {
			return new HashMap<>();
		}
		
		cause.add(this);
		
		float maxCapacity = getCapacityVolume();
		
		for(float pullVolume = Math.min(maxCapacity, quantity); pullVolume > 0;) {
			float amountRemoved = 0;
			
			for(Map.Entry<ILiquid, Float> e : removeAmount(cause, pullVolume).entrySet()) {
				float current = totalRemoved.containsKey(e.getKey()) ? totalRemoved.get(e.getKey()) : 0;
				totalRemoved.put(e.getKey(), current + e.getValue());
				amountRemoved += e.getValue();
			}
			
			pullVolume -= amountRemoved;
			
			if(amountRemoved <= 0.00001F)
				break;
			
		}
		
		return totalRemoved;
	}
	
	private Map<ILiquid, Float> removeAmount(List<ILiquidCarrier> cause, float quantity) {
		float pullVolume = quantity;

		float presentLiquid = getLiquidVolume();

		Map<ILiquid, Float> pullSample = getSample(pullVolume);

		if (getLiquidVolume() < quantity) {
			pullVolume = suckLiquid(cause, pullVolume - presentLiquid) + presentLiquid;
		}

		return getSample(pullSample, pullVolume);
	}

}
