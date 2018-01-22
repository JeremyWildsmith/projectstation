/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.item;

import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.rpg.item.usr.UsrWieldTarget;
import io.github.jevaengine.world.entity.IEntity;

/**
 *
 * @author Jeremy
 */
public class ToolItemFunction implements IItem.IItemFunction {

	
	
	@Override
	public IItem.IWieldTarget[] getWieldTargets() {
		return new IWieldTarget[] {UsrWieldTarget.LeftArm, UsrWieldTarget.RightArm};
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public IImmutableAttributeSet use(IRpgCharacter user, IEntity target, AttributeSet item) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public IItem.ItemUseAbilityTestResults testUseAbility(IRpgCharacter user, IEntity target, IImmutableAttributeSet item) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
