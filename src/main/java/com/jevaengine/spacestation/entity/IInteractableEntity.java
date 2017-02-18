/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.entity.IEntity;

/**
 *
 * @author Jeremy
 */
public interface IInteractableEntity extends IEntity {
	void interactedWith(IRpgCharacter subject);
	void interactWith(IRpgCharacter subject, String interaction);
	String[] getInteractions();
}
