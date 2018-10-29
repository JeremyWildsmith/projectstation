/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import com.jevaengine.spacestation.entity.atmos.ILiquidCarrier;
import com.jevaengine.spacestation.entity.WiredDevice;
import com.jevaengine.spacestation.entity.atmos.LiquidPipe;
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

	public static final float FUEL_CHAMBER_VOLUME = 10.0f;

	private final IImmutableSceneModel m_model;

	private final float m_capacity;

	public FuelChamber(String name, IImmutableSceneModel model, float capacity) {
		super(name, false);
		m_model = model;
		m_capacity = capacity;
	}
	@Override
	public float getVolume() {
		return FUEL_CHAMBER_VOLUME;
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
	public boolean isFreeFlow() {
		return false;
	}

	@Override
	public IImmutableSceneModel getModel() {
		return m_model;
	}

	@Override
	public void update(int delta) { }

}
