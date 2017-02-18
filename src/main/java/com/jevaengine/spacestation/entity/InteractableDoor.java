/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;

/**
 *
 * @author Jeremy
 */
public class InteractableDoor extends Door implements IInteractableEntity {

	public InteractableDoor(IAnimationSceneModel model, String name, boolean isOpen) {
		super(model, name, isOpen);
	}

	@Override
	public void interactedWith(IRpgCharacter subject) {
		if(isOpen())
			close();
		else
			open();
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
