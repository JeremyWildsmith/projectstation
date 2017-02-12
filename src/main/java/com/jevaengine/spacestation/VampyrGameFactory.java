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

import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.game.IGame;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.rpg.spell.ISpellFactory;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.world.IWorldFactory;
import javax.inject.Inject;

public final class VampyrGameFactory implements IGameFactory
{
	private final IInputSource m_inputSource;
	private final IRenderer m_renderer;
	private final ISpriteFactory m_spriteFactory;
	private final IWindowFactory m_windowFactory;
	private final IWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpellFactory m_spellFactory;
	
	@Inject
	public VampyrGameFactory(IInputSource inputSource, IRenderer renderer, ISpriteFactory spriteFactory, IWindowFactory windowFactory, IWorldFactory worldFactory, IEngineThreadPool engineThreadPool, IAudioClipFactory audioClipFactory, ISpellFactory spellFactory)
	{
		m_inputSource = inputSource;
		m_renderer = renderer;
		m_spriteFactory = spriteFactory;
		m_windowFactory = windowFactory;
		m_worldFactory = worldFactory;
		m_audioClipFactory = audioClipFactory;
		m_spellFactory = spellFactory;
	}
	
	public IGame create()
	{
		return new VampyrGame(m_inputSource, m_windowFactory, m_worldFactory, m_spriteFactory, m_audioClipFactory, m_renderer.getResolution(), m_spellFactory);
	}
}
