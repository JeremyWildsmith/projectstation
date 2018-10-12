/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.liquid.ILiquid;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
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
public final class LiquidPipe extends WiredDevice implements ILiquidCarrier {

	private final int MAX_CONNECTIONS = 4;

	private final IAnimationSceneModel m_model;

	private final float m_capacity;

	private final HashMap<ILiquid, Float> m_containedLiquids = new HashMap<>();

	public LiquidPipe(String name, IAnimationSceneModel model, float capacity) {
		super(name, true);
		m_model = model;
        m_capacity = capacity;
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

	public Map<ILiquidCarrier, Float> getDistributionRatios(List<ILiquidCarrier> heaviest, List<ILiquidCarrier> ignore) {
		float totalDifference = 0;
		Map<ILiquidCarrier, Float> relativeDifference = new HashMap<>();
		List<ILiquidCarrier> requested = new ArrayList<>();

		for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
			if(heaviest.contains(c) || ignore.contains(c)) //We won't ever distribute to heaviest
				continue;

			float totalWeight = getLiquidVolume();
			for(ILiquidCarrier subject : getConnections(ILiquidCarrier.class)) {
				if (subject == c || ignore.contains(subject))
					continue;

				requested.clear();
				requested.add(this);
				totalWeight += subject.getSourcedLiquidVolume(requested, 0);
			}

			requested.clear();
			requested.add(c);
			float difference = Math.abs(totalWeight - c.getSourcedLiquidVolume(requested, 0));

			totalDifference += difference;

			relativeDifference.put(c, difference);
		}

		if(totalDifference == 0)
			return new HashMap<>();

		Map<ILiquidCarrier, Float> diffRatios = new HashMap<>();

		for(Map.Entry<ILiquidCarrier, Float> e : relativeDifference.entrySet()) {
			diffRatios.put(e.getKey(), e.getValue() / totalDifference);
		}

		return diffRatios;
	}


	public Map<ILiquidCarrier, Float> getSuckRatios(List<ILiquidCarrier> ignore) {
        List<ILiquidCarrier> lessThan = new ArrayList<>();
        for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
            if(c.getLiquidVolume() < getLiquidVolume())
                lessThan.add(c);
        }

        lessThan.addAll(ignore);

        float totalDifference = 0;
        Map<ILiquidCarrier, Float> relativeDifference = new HashMap<>();
        List<ILiquidCarrier> requested = new ArrayList<>();

        for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
            if(lessThan.contains(c))
                continue;

            requested.clear();
            requested.add(this);
            float totalWeight = c.getSourcedLiquidVolume(requested, 0);

            totalDifference += totalWeight;

            relativeDifference.put(c, totalWeight);
        }

        if(totalDifference == 0)
            return new HashMap<>();

        Map<ILiquidCarrier, Float> diffRatios = new HashMap<>();

        for(Map.Entry<ILiquidCarrier, Float> e : relativeDifference.entrySet()) {
            diffRatios.put(e.getKey(), e.getValue() / totalDifference);
        }

        return diffRatios;

	}

	private float doSuckCycle(List<ILiquidCarrier> ignore, float amount) {
		float amountSucked = 0;
		float maxSuckable = amount;
		boolean accepted = true;
		List<ILiquidCarrier> refused = new ArrayList<>();

		refused.addAll(ignore);
		while(accepted && maxSuckable > Vector2F.TOLERANCE) {
			accepted = false;
			float consumedInCycle = 0;
			for (Map.Entry<ILiquidCarrier, Float> e : getSuckRatios(refused).entrySet()) {
				float quantityPull = Math.min(e.getKey().getLiquidVolume(), e.getValue() * maxSuckable);

				List<ILiquidCarrier> cause = new ArrayList<>();
				cause.add(this);
				Map<ILiquid, Float> sucked = e.getKey().remove(cause, quantityPull);

				cause.remove(this);
				cause.addAll(getConnections(ILiquidCarrier.class));
				add(cause, sucked);

				float acceptedAmount = 0;
				for(Map.Entry<ILiquid, Float> suck : sucked.entrySet()) {
					acceptedAmount += suck.getValue();
				}

				if(acceptedAmount > Vector2F.TOLERANCE) {
					accepted = true;
					consumedInCycle += acceptedAmount;
				} else
					refused.add(e.getKey());
			}
			maxSuckable -= consumedInCycle;
			amountSucked += consumedInCycle;
		}

		return amountSucked;
	}

    private float doPushCycle(List<ILiquidCarrier> heaviest) {
        return doPushCycle(heaviest,-1);
    }

    private float doPushCycle(ILiquidCarrier heaviest, int delta) {
        List<ILiquidCarrier> h = new ArrayList<>();
        h.add(heaviest);
        return doPushCycle(h);
    }

    private float doPushCycle(List<ILiquidCarrier> heaviest, int delta) {
		float amountRemoved = 0;
		float maxDistributable = delta < 0 ? getLiquidVolume() : Math.min(getLiquidVolume(), getLiquidVolume() * calculateFlowRate() * (delta / 1000.0F));
		boolean accepted = true;
		List<ILiquidCarrier> refused = new ArrayList<>();

		while(accepted && maxDistributable > Vector2F.TOLERANCE) {
			accepted = false;
			float consumedInCycle = 0;
			for (Map.Entry<ILiquidCarrier, Float> e : getDistributionRatios(heaviest, refused).entrySet()) {
				float quantityPush = Math.min(e.getKey().getCapacityVolume(), e.getValue() * maxDistributable);

				Map<ILiquid, Float> sample = getSample(quantityPush);

				List<ILiquidCarrier> cause = new ArrayList<>();
				cause.add(this);
				float acceptedAmount = e.getKey().add(cause, sample);
				if(acceptedAmount > maxDistributable * 0.01) {
					accepted = true;
					consumedInCycle += acceptedAmount;
				} else
					refused.add(e.getKey());
			}
			maxDistributable -= consumedInCycle;
			amountRemoved += consumedInCycle;
		}

		Map<ILiquid, Float> removeSample = getSample(amountRemoved);

		for (Map.Entry<ILiquid, Float> e : removeSample.entrySet()) {
			float current = m_containedLiquids.get(e.getKey());
			m_containedLiquids.put(e.getKey(), current - e.getValue());
		}

		return amountRemoved;
	}

	@Override
	public void update(int delta) {
		ILiquidCarrier heaviest = getHeaviestConnection();

		if(heaviest == null)
			return;

		float left = doPushCycle(heaviest, delta);
		doSuckCycle(new ArrayList<>(), left);
	}

	@Override
	protected void connectionChanged() {
		updateModel();
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(getConnections().size() >= MAX_CONNECTIONS)
			return false;
		
		if(!(d instanceof ILiquidCarrier))
			return false;

		if(d instanceof LiquidPipe) {
			boolean sameDepth = Math.abs(this.getBody().getLocation().z - d.getBody().getLocation().z) < Vector2F.TOLERANCE;
			if(!sameDepth)
				return false;
		}
		
		Vector3F delta = d.getBody().getLocation().difference(getBody().getLocation());
		Direction dir = Direction.fromVector(delta.getXy());
		
		if(dir.isDiagonal())
			return false;
			
		return true;
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
	    return m_capacity;
	}
	
	private ILiquidCarrier getHeaviestConnection() {
		List<ILiquidCarrier> carrier = new ArrayList<>();
		carrier.add(this);
		
		ILiquidCarrier heaviestCarrier = null;
		float lastCarrierWeight = Float.MIN_VALUE;
		for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
			float weight = c.getSourcedLiquidVolume(carrier, 0);
			if(weight >= lastCarrierWeight) {
				lastCarrierWeight = weight;
				heaviestCarrier = c;
			}
		}
		
		return heaviestCarrier;
	}

	private float calculateFlowRate() {
		float totalLiquid = getLiquidVolume();

		if(totalLiquid == 0)
			return 0;

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
		if(quantity <= 0)
			return new HashMap<>();

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

	private float createRoom(ILiquidCarrier source, float volumeNeeded) {
        float initial = getLiquidVolume();
        List<ILiquidCarrier> cause = new ArrayList<>();
        cause.add(source);

        ILiquidCarrier heaviest = getHeaviestConnection();
        if(!cause.contains(heaviest) && getConnections(ILiquidCarrier.class).size() > 2)
            cause.add(heaviest);

	    doPushCycle(cause);
	    return initial - getLiquidVolume();
	}

	private float suckLiquid(List<ILiquidCarrier> cause, float volumeNeeded) {
		 return doSuckCycle(cause, volumeNeeded);
	}

	@Override
	public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid) {
		if (cause.contains(this)) {
			return 0;
		}

		ILiquidCarrier source = null;
		for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
            if (cause.indexOf(c) >= 0) {
                source = c;
                break;
            }
        }

        if(source == null)
            throw new IllegalArgumentException();

		cause.add(this);

		float pushVolume = 0;

		for (Map.Entry<ILiquid, Float> e : liquid.entrySet()) {
			pushVolume += e.getValue();
		}

		float room = getCapacityVolume() - getLiquidVolume();

		if (room < pushVolume) {
			pushVolume = createRoom(source, pushVolume - room) + room;
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
		float totalVolumeRemoved = 0;
		
		if (cause.contains(this)) {
			return new HashMap<>();
		}
		
		cause.add(this);
		
		float maxCapacity = getCapacityVolume();
		
		for(float pullVolume = Math.min(maxCapacity, quantity); pullVolume > 0.000001F;) {
			float amountRemoved = 0;
			
			for(Map.Entry<ILiquid, Float> e : removeAmount(cause, pullVolume).entrySet()) {
				float current = totalRemoved.containsKey(e.getKey()) ? totalRemoved.get(e.getKey()) : 0;
				totalRemoved.put(e.getKey(), current + e.getValue());
				amountRemoved += e.getValue();
				totalVolumeRemoved += e.getValue();
			}
			
			pullVolume -= amountRemoved;
			
			if(amountRemoved <= 0.00001F)
				break;
			
		}
		
		for(Map.Entry<ILiquid, Float> e : totalRemoved.entrySet()) {
			float current = m_containedLiquids.get(e.getKey());
			m_containedLiquids.put(e.getKey(), current - e.getValue());
		}
		
		suckLiquid(cause, totalVolumeRemoved);
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

	@Override
	public float getSourcedLiquidVolume(List<ILiquidCarrier> requested, float sourceWeight) {
		if(requested.contains(this))
			return sourceWeight;
		
		requested.add(this);
		
		float volume = getLiquidVolume() + sourceWeight;
		for(ILiquidCarrier c : getConnections(ILiquidCarrier.class)) {
			volume = c.getSourcedLiquidVolume(requested, volume);
		}
		
		return volume;
	}

}
