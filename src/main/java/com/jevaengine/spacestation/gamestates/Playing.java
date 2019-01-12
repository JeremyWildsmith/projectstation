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
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import com.jevaengine.spacestation.ui.*;
import com.jevaengine.spacestation.ui.HudFactory.Hud;
import com.jevaengine.spacestation.ui.InventoryHudFactory.InventoryHud;
import com.jevaengine.spacestation.ui.LoadoutHudFactory.LoadoutHud;
import com.jevaengine.spacestation.ui.playing.ConsoleInterfaceInteractionHandler;
import com.jevaengine.spacestation.ui.playing.PlayingWindowFactory;
import com.jevaengine.spacestation.ui.playing.PlayingWindowFactory.PlayingWindow;
import com.jevaengine.spacestation.ui.playing.WorldInteractionBehaviorInjector;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter.NullRpgCharacter;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.FollowCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 *
 * @author Jeremy
 */
public class Playing implements IState {

	private static final URI LEM_DISPLAY_WINDOW = URI.create("file:///ui/windows/dcpu/lem/layout.jwl");

	private static final float CAMERA_ZOOM = 2.5f;

	private IStateContext m_context;
	private final World m_world;
	private PlayingWindow m_playingWindow;

	private final Logger m_logger = LoggerFactory.getLogger(Playing.class);

	private IRpgCharacter m_player = new NullRpgCharacter();

	private Hud m_hud;
	private LoadoutHud m_loadoutHud;
	private InventoryHud m_inventoryHud;

	public Playing(World world) {
		m_world = world;
	}

	private WorldInteractionBehaviorInjector.IInteractionHandler[] createInteractionHandlers() {
		LemDisplayFactory lemDisplayFactory = new LemDisplayFactory(m_context.getWindowManager(), m_context.getWindowFactory(), LEM_DISPLAY_WINDOW);
		return new WorldInteractionBehaviorInjector.IInteractionHandler[] {
				new ConsoleInterfaceInteractionHandler(lemDisplayFactory)
		};

	}

	@Override
	public void enter(IStateContext context) {
		m_context = context;
		
		try {
			ISceneBufferFactory sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create());
			FollowCamera camera = new FollowCamera(sceneBufferFactory);
			camera.setZoom(CAMERA_ZOOM);

			IRpgCharacter playerEntityBuffer = m_world.getEntities().getByName(IRpgCharacter.class, "player");

			if (playerEntityBuffer != null) {
				m_player = playerEntityBuffer;
			} else {
				m_logger.error("Character entity was not placed in world. Using null character entity instead.");
			}

			camera.attach(m_world);
			camera.setTarget(m_player);

			Vector2D resolution = context.getWindowManager().getResolution();
			m_hud = new HudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player, m_player.getInventory(), m_player.getLoadout());
			m_hud.setTopMost(true);
			m_hud.setMovable(false);
			m_hud.center();
			m_hud.setLocation(new Vector2D(m_hud.getLocation().x, resolution.y - m_hud.getBounds().height));
			
			m_loadoutHud = new LoadoutHudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player.getLoadout(), m_player.getInventory());
			m_loadoutHud.setMovable(false);
			m_loadoutHud.setTopMost(true);
			m_loadoutHud.setVisible(false);
			m_loadoutHud.center();
			m_loadoutHud.setLocation(new Vector2D(m_loadoutHud.getLocation().x, resolution.y - m_hud.getBounds().height - m_loadoutHud.getBounds().height));
			
			m_inventoryHud = new InventoryHudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player.getLoadout(), m_player.getInventory(), m_player);
			m_inventoryHud.setMovable(false);
			m_inventoryHud.setTopMost(true);
			m_inventoryHud.setVisible(false);
			m_inventoryHud.setLocation(new Vector2D(m_loadoutHud.getLocation().x + m_loadoutHud.getBounds().width + 10,
												  m_loadoutHud.getLocation().y));



			//Gas Simulation Debug
			GasDebugFactory.GasDebug debug = new GasDebugFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player, GasSimulationNetwork.PipeB);
			debug.setMovable(true);
			debug.setTopMost(true);
			debug.setVisible(true);
			debug.setLocation(new Vector2D(0, 0));

			//Gas Simulation Debug
			debug = new GasDebugFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player, GasSimulationNetwork.PipeA);
			debug.setMovable(true);
			debug.setTopMost(true);
			debug.setVisible(true);
			debug.setLocation(new Vector2D(0, 0));

			//Gas Simulation Debug
			debug = new GasDebugFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player, GasSimulationNetwork.PipeC);
			debug.setMovable(true);
			debug.setTopMost(true);
			debug.setVisible(true);
			debug.setLocation(new Vector2D(0, 0));

			//Gas Simulation Debug
			debug = new GasDebugFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player, GasSimulationNetwork.Environment);
			debug.setMovable(true);
			debug.setTopMost(true);
			debug.setVisible(true);
			debug.setLocation(new Vector2D(0, 0));

			//Gas Simulation Debug
			debug = new GasDebugFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player, GasSimulationNetwork.PipeD);
			debug.setMovable(true);
			debug.setTopMost(true);
			debug.setVisible(true);
			debug.setLocation(new Vector2D(0, 0));


			m_hud.getObservers().add(new HudFactory.IHudObserver() {
				@Override
				public void movementSpeedChanged(boolean isRunning) { }

				@Override
				public void inventoryViewChanged(boolean isVisible) {
					m_loadoutHud.setVisible(isVisible);
					m_inventoryHud.setVisible(isVisible);
				}
			});
			
			m_playingWindow = new PlayingWindowFactory(context.getWindowManager(), context.getWindowFactory()).create(camera, m_player, createInteractionHandlers());
			m_playingWindow.center();
			m_playingWindow.focus();
			
		} catch (WindowConstructionException e) {
			m_logger.error("Error occured constructing demo world or world view. Reverting to MainMenu.", e);
			m_context.setState(new MainMenu());
		}
	}

	@Override
	public void leave() {
		if (m_playingWindow != null) {
			m_playingWindow.dispose();
		}
	}

	@Override
	public void update(int deltaTime) {
		m_world.update(deltaTime);
	}
}
