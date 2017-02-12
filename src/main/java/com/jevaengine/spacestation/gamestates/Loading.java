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
package com.jevaengine.spacestation.gamestates;

import com.jevaengine.spacestation.IState;
import com.jevaengine.spacestation.IStateContext;
import io.github.jevaengine.FutureResult;
import io.github.jevaengine.IInitializationMonitor;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.ValueGuage;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory.WorldConstructionException;
import io.github.jevaengine.world.World;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class Loading implements IState
{
	private IStateContext m_context;
	private Window m_window;
	
	private final IWindowFactory m_windowFactory;
	private final IParallelWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpriteFactory m_spriteFactory;
	
	private final Logger m_logger = LoggerFactory.getLogger(Loading.class);
	
	private final URI m_worldName;
	private volatile FutureResult<World, WorldConstructionException> m_loadedWorld;
	
	private final ILoadingWorldHandler m_handler;
	
	public Loading(IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory, URI world, ILoadingWorldHandler handler)
	{
		m_handler = handler;
		m_windowFactory = windowFactory;
		m_worldFactory = worldFactory;
		m_audioClipFactory = audioClipFactory;
		m_spriteFactory = spriteFactory;
		m_worldName = world;
	}
	
	@Override
	public void enter(IStateContext context)
	{
		m_context = context;
		try
		{
			final LoadingBehaviourInjector behavior = new LoadingBehaviourInjector();
			
			m_window = m_windowFactory.create(URI.create("file:///ui/windows/loading.jwl"), behavior);
			context.getWindowManager().addWindow(m_window);
			m_window.center();
			
			m_worldFactory.create(m_worldName, new IInitializationMonitor<World, WorldConstructionException>() {

				@Override
				public void completed(FutureResult<World, WorldConstructionException> result) {
					m_loadedWorld = result;
				}

				@Override
				public void statusChanged(float progress, String status) {
					behavior.setProgress(progress);
				}
			});
		} catch (WindowConstructionException e)
		{
			m_logger.error("Error constructing loading window", e);
		}
	}

	@Override
	public void leave()
	{
		if(m_window != null)
		{
			m_context.getWindowManager().removeWindow(m_window);
			m_window.dispose();
		}
	}

	@Override
	public void update(int iDelta)
	{
		if(m_loadedWorld != null)
		{
			m_handler.done(m_loadedWorld);
			m_loadedWorld = null;
		}
	}
	
	public interface ILoadingWorldHandler
	{
		void done(FutureResult<World, WorldConstructionException> world);
	}
	
	public class LoadingBehaviourInjector extends WindowBehaviourInjector
	{	
		private ValueGuage m_progressGuage;
		
		@Override
		public void doInject() throws NoSuchControlException
		{
			m_progressGuage = getControl(ValueGuage.class, "loadingProgressBar");
		}
		
		public void setProgress(float progress)
		{
			m_progressGuage.setValue(progress);
		}
	}
}
