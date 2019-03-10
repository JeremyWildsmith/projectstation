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
package com.jevaengine.spacestation;

import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.IParallelEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;

/**
 *
 * @author Jeremy
 */
public interface IStateContext
{
	void setState(IState state);
	WindowManager getWindowManager();

	IFontFactory getFontFactory();
	IPhysicsWorldFactory getPhysicsWorldFactory();
	IEntityFactory getEntityFactory();
	IParallelEntityFactory getParallelEntityFactory();
	IWindowFactory getWindowFactory();
	IParallelWorldFactory getParallelWorldFactory();
	IWorldFactory getWorldFactory();
	IAudioClipFactory getAudioClipFactory();
	ISpriteFactory getSpriteFactory();
	IEffectMapFactory getEffectMapFactory();

	IItemFactory getItemFactory();

	IConfigurationFactory getConfigFactory();
}
