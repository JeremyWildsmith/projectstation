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

import com.jevaengine.spacestation.gamestates.MainMenu;
import io.github.jevaengine.DefaultEngineThreadPool;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.game.DefaultGame;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.graphics.ISpriteFactory.SpriteConstructionException;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.spell.ISpellFactory;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.ThreadPooledWorldFactory;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StationGame extends DefaultGame implements IStateContext
{
	private IRenderable m_cursor;
	private IState m_state;
	
	private Logger m_logger = LoggerFactory.getLogger(StationGame.class);

	public StationGame(IInputSource inputSource, IWindowFactory windowFactory, IWorldFactory worldFactory, ISpriteFactory spriteFactory, IAudioClipFactory audioClipFactory, Vector2D resolution, ISpellFactory spellFactory)
	{
		super(inputSource, resolution);
	
		try
		{
			m_cursor = spriteFactory.create(URI.create("file:///ui/style/parpg/cursor/cursor.jsf"));
		} catch (SpriteConstructionException e)
		{
			m_logger.error("Unable to construct cursor sprite. Reverting to null graphic for cursor.", e);
			m_cursor = new NullGraphic();
		}
		
		m_state = new EntryState(windowFactory, worldFactory, audioClipFactory, spriteFactory);
		m_state.enter(this);
	}

	@Override
	public void doLogic(int deltaTime)
	{
		m_state.update(deltaTime);
	}
	
	@Override
	protected IRenderable getCursor()
	{
		return m_cursor;
	}
	
	public void setState(IState state)
	{
		m_state.leave();
		m_state = state;
		state.enter(this);
	}
	
	private static class EntryState implements IState
	{
		private final IWindowFactory m_windowFactory;
		private final IParallelWorldFactory m_worldFactory;
		private final IAudioClipFactory m_audioClipFactory;
		private final ISpriteFactory m_spriteFactory;
		
		private IStateContext m_context;

		public EntryState(IWindowFactory windowFactory, IWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory)
		{
			m_windowFactory = windowFactory;
			m_worldFactory = new ThreadPooledWorldFactory(worldFactory, new DefaultEngineThreadPool());
			m_audioClipFactory = audioClipFactory;
			m_spriteFactory = spriteFactory;
		}
		
		@Override
		public void enter(IStateContext context)
		{
			m_context = context;
		}

		@Override
		public void leave() { }

		@Override
		public void update(int deltaTime)
		{
			m_context.setState(new MainMenu(m_windowFactory, m_worldFactory, m_audioClipFactory, m_spriteFactory));
		}
	}
}
