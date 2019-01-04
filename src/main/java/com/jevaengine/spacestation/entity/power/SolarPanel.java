/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import com.jevaengine.spacestation.entity.BasicDevice;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public class SolarPanel extends BasicDevice implements IPowerDevice {
	private final ISceneModel m_model;
	
	private final int m_productionWatt;
	private int m_joules;
	
	public SolarPanel(String name, ISceneModel model, int productionWatt) {
		super(name, false);
		m_model =  model;
		m_productionWatt = productionWatt;
		m_joules = 0;
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
		m_joules = Math.min(m_productionWatt, m_joules + (int)Math.ceil(delta / 1000.0F * m_productionWatt));
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		int provided = Math.min(m_joules, joules);
		
		m_joules -= provided;
		
		return provided;
	}

	@Override
	protected boolean canConnectTo(IDevice d) {
		return (d instanceof IPowerDevice);
	}
}
