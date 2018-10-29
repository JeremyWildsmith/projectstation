/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import com.jevaengine.spacestation.entity.IInteractableEntity;
import com.jevaengine.spacestation.entity.WiredDevice;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class PowerSwitch extends WiredDevice implements IPowerDevice, IInteractableEntity {

	private final IAnimationSceneModel m_model;
	
	private boolean m_isOn;
	
	public PowerSwitch(String name, IAnimationSceneModel model) {
		super(name, false);
		m_model = model;
	}
	
	private IPowerDevice getUpperDevice() {
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);
			
			if(dir == Direction.YMinus)
				return d;
		}
		
		return null;
	}
	
	private IPowerDevice getLowerDevice() {
		for(IPowerDevice d : getConnections(IPowerDevice.class)) {
			Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
			Direction dir = Direction.fromVector(delta);
			
			if(dir == Direction.YPlus)
				return d;
		}
		
		return null;		
	}
	
	@Override
	protected void connectionChanged() { }

	@Override
	protected boolean canConnectTo(IDevice d) {
		if(!super.canConnectTo(d))
			return false;
		
		Vector2F delta = d.getBody().getLocation().difference(this.getBody().getLocation()).getXy();
		Direction dir = Direction.fromVector(delta);
		
		if(!(d instanceof IPowerDevice) || !(d instanceof WiredDevice))
			return false;
		
		if(dir == Direction.YMinus) {
			return getUpperDevice() == null;
		} else if(dir == Direction.YPlus)
			return getLowerDevice() == null;
		
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
	public void update(int delta) {
		m_model.getAnimation(m_isOn ? "on" : "off").setState(IAnimationSceneModel.AnimationSceneModelAnimationState.Play);
	}

	@Override
	public int drawEnergy(List<IDevice> requested, int joules) {
		if(requested.contains(this))
			return 0;
		
		requested.add(this);
		
		if(!m_isOn)
			return 0;
		
		int drawn = getUpperDevice() == null ? 0 : getUpperDevice().drawEnergy(requested, joules);
		drawn += getLowerDevice() == null ? 0 : getLowerDevice().drawEnergy(requested, joules - drawn);
		
		return drawn;
	}

	@Override
	public void interactedWith(IRpgCharacter subject) {
		m_isOn = !m_isOn;
	}

	@Override
	public void interactWith(IRpgCharacter subject, String interaction) {
		interactedWith(subject);
	}

	@Override
	public String[] getInteractions() {
		return new String[0];
	}
	
}
