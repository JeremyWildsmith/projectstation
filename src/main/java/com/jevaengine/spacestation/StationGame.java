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
import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.game.DefaultGame;
import io.github.jevaengine.graphics.IFontFactory;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.graphics.ISpriteFactory.SpriteConstructionException;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.rpg.spell.ISpellFactory;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.ThreadPooledWorldFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.IParallelEntityFactory;
import io.github.jevaengine.world.entity.ThreadPooledEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public final class StationGame extends DefaultGame implements IStateContext
{
	private IRenderable m_cursor;
	private IState m_state;
	
	private Logger m_logger = LoggerFactory.getLogger(StationGame.class);

	private final IFontFactory m_fontFactory;
	private final IEntityFactory m_entityFactory;
	private final IWindowFactory m_windowFactory;
	private final IWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpriteFactory m_spriteFactory;
	private final IParallelWorldFactory m_parallelWorldFactory;
	private final IPhysicsWorldFactory m_physicsWorldFactory;
	private final IParallelEntityFactory m_parallelEntityFactory;
	private final IEffectMapFactory m_effectMapFactory;
	private final IItemFactory m_itemFactory;

	public StationGame(IItemFactory itemFactory, IFontFactory fontFactory, IPhysicsWorldFactory physicsWorldFactory, IEngineThreadPool threadPool, IEffectMapFactory effectMapFactory, IEntityFactory entityFactory, IInputSource inputSource, IWindowFactory windowFactory, IWorldFactory worldFactory, ISpriteFactory spriteFactory, IAudioClipFactory audioClipFactory, Vector2D resolution, ISpellFactory spellFactory)
	{
		super(inputSource, resolution);
	
		try
		{
			m_cursor = spriteFactory.create(URI.create("file:///ui/cursor.jsf"));
		} catch (SpriteConstructionException e)
		{
			m_logger.error("Unable to construct cursor sprite. Reverting to null graphic for cursor.", e);
			m_cursor = new NullGraphic();
		}

		m_itemFactory = itemFactory;
		m_parallelEntityFactory = new ThreadPooledEntityFactory(entityFactory, threadPool);
		m_effectMapFactory = effectMapFactory;
		m_physicsWorldFactory = physicsWorldFactory;
		m_fontFactory = fontFactory;
		m_audioClipFactory = audioClipFactory;
		m_entityFactory = entityFactory;
		m_spriteFactory = spriteFactory;
		m_worldFactory = worldFactory;
		m_windowFactory = windowFactory;
		m_parallelWorldFactory = new ThreadPooledWorldFactory(worldFactory, threadPool);


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

	@Override
	public IItemFactory getItemFactory() {
		return m_itemFactory;
	}

	@Override
	public IPhysicsWorldFactory getPhysicsWorldFactory() {
		return m_physicsWorldFactory;
	}

	@Override
	public IEntityFactory getEntityFactory() {
		return m_entityFactory;
	}

	@Override
	public IParallelEntityFactory getParallelEntityFactory() {
		return m_parallelEntityFactory;
	}

	@Override
	public IEffectMapFactory getEffectMapFactory() {
		return m_effectMapFactory;
	}

	@Override
	public IFontFactory getFontFactory() {
		return m_fontFactory;
	}

	@Override
	public IWindowFactory getWindowFactory() {
		return m_windowFactory;
	}

	@Override
	public IWorldFactory getWorldFactory() {
		return m_worldFactory;
	}

	@Override
	public IParallelWorldFactory getParallelWorldFactory() {
		return m_parallelWorldFactory;
	}

	@Override
	public IAudioClipFactory getAudioClipFactory() {
		return m_audioClipFactory;
	}

	@Override
	public ISpriteFactory getSpriteFactory() {
		return m_spriteFactory;
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
			m_context.setState(new MainMenu());
		}
	}
}
