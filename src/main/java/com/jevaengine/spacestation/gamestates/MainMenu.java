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
import com.jevaengine.spacestation.StationProjectionFactory;
import io.github.jevaengine.FutureResult;
import io.github.jevaengine.audio.IAudioClip;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.audio.NullAudioClip;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWeatherFactory;
import io.github.jevaengine.world.IWorldFactory.WorldConstructionException;
import io.github.jevaengine.world.TiledEffectMapFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.NullEntityFactory;
import io.github.jevaengine.world.entity.ThreadPooledEntityFactory;
import io.github.jevaengine.world.physics.NullPhysicsWorldFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.UnsortedOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class MainMenu implements IState
{
	private static final URI ENTRY_MAP = URI.create("file:///world/singleplayer/firstStationRework.jmp");
	private static final URI GALAXY_MAP = URI.create("file:///world/singleplayer/space.jmp");
	private static final URI MENU_MAP = URI.create("file:///world/mainMenu.jmp");
	private static final Vector3F OBSERVER_LOCATION = new Vector3F(17.5f, 20, 2);
	private static final float CAMERA_ZOOM = 2.5f;
	private static final URI MENU_AUDIO = URI.create("file:///audio/menu/0.ogg");

	private IStateContext m_context;
	private Window m_window;

	private IAudioClip m_menuAudio = new NullAudioClip();
	private World m_backgroundWorld = null;

	private final Logger m_logger = LoggerFactory.getLogger(MainMenu.class);
	
	public MainMenu() {
	}
	
	@Override
	public void enter(IStateContext context)
	{
		m_context = context;

		try {
			m_menuAudio = context.getAudioClipFactory().create(MENU_AUDIO);
		} catch (IAudioClipFactory.AudioClipConstructionException ex) {
			m_logger.error("Unable to construct background audio.", ex);
		}

		try {
			m_backgroundWorld = context.getWorldFactory().create(MENU_MAP);
		} catch(WorldConstructionException ex) {
			m_logger.error("Error constructing background world.", ex);
		}

		try
		{
			m_menuAudio.repeat();
			m_window = context.getWindowFactory().create(URI.create("file:///ui/windows/mainmenu.jwl"), new MainMenuBehaviourInjector(m_backgroundWorld, OBSERVER_LOCATION, CAMERA_ZOOM));
			context.getWindowManager().addWindow(m_window);
			m_window.center();
			m_window.setMovable(false);
		} catch (WindowConstructionException e)
		{
			m_logger.error("Error constructing demo menu window", e);
		}
		
		
	}

	@Override
	public void leave()
	{
		m_menuAudio.stop();
		m_menuAudio.dispose();
		if(m_window != null)
		{
			m_context.getWindowManager().removeWindow(m_window);
			m_window.dispose();
		}
	}

	@Override
	public void update(int iDelta) { }
	
	public class MainMenuBehaviourInjector extends WindowBehaviourInjector
	{
		private final World backgroundWorld;
		private final Vector3F observerLocation;
		private final float zoom;

		public MainMenuBehaviourInjector(World backgroundWorld, Vector3F observerLocation, float zoom) {
			this.backgroundWorld = backgroundWorld;
			this.observerLocation = observerLocation;
			this.zoom = zoom;
		}

		@Override
		public void doInject() throws NoSuchControlException
		{
			if(backgroundWorld != null) {
				ControlledCamera c = new ControlledCamera(new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create()));
				c.attach(backgroundWorld);
				c.lookAt(observerLocation);
				c.setZoom(zoom);
				getControl(WorldView.class, "worldView").setCamera(c);
			}
			getControl(Button.class, "btnNewGame").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_context.setState(new Loading(ENTRY_MAP, new Loading.ILoadingWorldHandler() {
						@Override
						public void done(FutureResult<World, WorldConstructionException> world) {
							try
							{
								m_context.setState(new Playing(world.get(), m_context.getWorldFactory().create(GALAXY_MAP)));
							} catch (WorldConstructionException e)
							{
								m_logger.error("Unable to enter playing state due to error in loading world", e);
							}
						}
					}));
				}
			});
		}
	}
}
