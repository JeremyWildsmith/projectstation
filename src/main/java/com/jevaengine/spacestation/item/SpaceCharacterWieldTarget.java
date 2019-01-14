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

import io.github.jevaengine.rpg.item.IItem.IWieldTarget;

public enum SpaceCharacterWieldTarget implements IWieldTarget
{
	LeftArm("LeftArm"),
	RightArm("RightArm"),
	Jacket("Jacket"),
	Uniform("Uniform"),
	Eyes("Eyes"),
	Ears("Ears"),
	Hands("Hands"),
	LeftHand("LeftHand"),
	Mask("Mask"),
	Chest("Chest"),
	Head("Head"),
	LeftLeg("LeftLeg"),
	RightLeg("RightLeg"),
	Feet("Feet"),
	Waist("Waist"),
	Consumable("Consumable");

	public static final SpaceCharacterWieldTarget ARMOR_WIELD_TARGETS[] = {Chest, LeftLeg, RightLeg, Waist, Head, Feet};
	public static final SpaceCharacterWieldTarget WEAPON_WIELD_TARGETS[] = {LeftHand, LeftArm, RightArm};

	private final String m_name;

	SpaceCharacterWieldTarget(String name)
	{
		m_name = name;
	}
	
	@Override
	public String getName()
	{
		return m_name;
	}
}
