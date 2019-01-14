/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.jevaengine.spacestation.item;

import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItem.IItemFunction;
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.rpg.item.IItem.ItemUseAbilityTestResults;

public enum SpaceArmorItemFunction implements IItemFunction
{
	ChestArmor("Chest Armor", new IWieldTarget[] { SpaceCharacterWieldTarget.Chest }),
	Uniform("Uniform", new IWieldTarget[] { SpaceCharacterWieldTarget.Uniform }),
	Shoes("Shoes", new IWieldTarget[] { SpaceCharacterWieldTarget.Feet }),
	Gloves("Gloves", new IWieldTarget[] { SpaceCharacterWieldTarget.Hands }),
	EarPiece("Ear Piece", new IWieldTarget[] { SpaceCharacterWieldTarget.Ears }),
	Glasses("Glasses", new IWieldTarget[] { SpaceCharacterWieldTarget.Eyes }),
	Helmet("Helmet", new IWieldTarget[] { SpaceCharacterWieldTarget.Mask }),
	LegArmor("Leg Armor", new IWieldTarget[] { SpaceCharacterWieldTarget.Waist }),
	HeadArmor("Head Armor", new IWieldTarget[] { SpaceCharacterWieldTarget.Mask });

	private final String m_name;
	private final IWieldTarget[] m_wieldTargets;

	private SpaceArmorItemFunction(String name, IWieldTarget[] wieldTarget)
	{
		m_name = name;
		m_wieldTargets = wieldTarget;
	}
	
	@Override
	public IWieldTarget[] getWieldTargets()
	{
		return m_wieldTargets;
	}

	@Override
	public String getName()
	{
		return m_name;
	}
	
	@Override
	public ItemUseAbilityTestResults testUseAbility(IRpgCharacter user, IItem.ItemTarget target, IImmutableAttributeSet item)
	{
		return new ItemUseAbilityTestResults(true);
	}

	@Override
	public IImmutableAttributeSet use(IRpgCharacter user, IItem.ItemTarget target, AttributeSet itemAttributes, IItem item)
	{

		AttributeSet impact = new AttributeSet();
		//impact.get(UsrActiveCharacterAttribute.Hits).set(itemAttributes.get(UsrArmorItemAttribute.DefenceBonus).get());

		return impact;
	}

	/*
	public enum UsrArmorItemAttribute implements IAttributeIdentifier
	{
		DefenceBonus("Defence Bonus", "");

		private final String m_name;
		private final String m_description;
		
		private UsrArmorItemAttribute(String name, String description)
		{
			m_name = name;
			m_description = description;
		}
		
		@Override
		public String getName()
		{
			return m_name;
		}

		@Override
		public String getDescription()
		{
			return m_description;
		}
	}*/
}
