/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class Capacitor extends WiredDevice implements IPowerDevice {

	private final int MAX_CAPACITY = 1800000;
	private final int MAX_INPUT = 1000;
	
	private int m_storedJoules;
	
	private final IImmutableSceneModel m_model;

	public Capacitor(String name, IImmutableSceneModel model, int energyJoules) {
		super(name, false);
		m_model = model;
		m_storedJoules = Math.min(MAX_CAPACITY, energyJoules);
	}
	
	@Override
	protected void connectionChanged() { }

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
		int consumed = 0;
		
		List<IDevice> requested = new ArrayList<>();
		requested.add(this);

		final int maxIntake = Math.min(MAX_INPUT, MAX_CAPACITY - m_storedJoules);
		
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			consumed += d.drawEnergy(requested, maxIntake - consumed);
		}
		
		m_storedJoules += consumed;
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		int drawn = Math.min(m_storedJoules, joules);
		
		m_storedJoules -= drawn;
		
		return drawn;
	}
	
}
