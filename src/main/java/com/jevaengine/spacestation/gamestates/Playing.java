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
import com.jevaengine.spacestation.ui.HudFactory;
import com.jevaengine.spacestation.ui.HudFactory.Hud;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.graphics.ISpriteFactory.SpriteConstructionException;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputKeyEvent.KeyEventType;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.joystick.InputMouseEvent.MouseButton;
import io.github.jevaengine.joystick.InputMouseEvent.MouseEventType;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.character.IMovementResolver.IMovementDirector;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter.NullRpgCharacter;
import io.github.jevaengine.rpg.entity.character.tasks.AttackTask;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.MenuStrip;
import io.github.jevaengine.ui.MenuStrip.IMenuStripListener;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.Timer.ITimerObserver;
import io.github.jevaengine.ui.Viewport;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.Window.IWindowFocusObserver;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.ui.WorldView.IWorldViewInputObserver;
import io.github.jevaengine.ui.style.NullUIStyle;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.FollowCamera;
import io.github.jevaengine.world.scene.camera.ICamera;
import io.github.jevaengine.world.steering.DirectionSteeringBehavior;
import io.github.jevaengine.world.steering.ISteeringBehavior;
import io.github.jevaengine.world.steering.ISteeringBehavior.NullSteeringBehavior;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class Playing implements IState {

	private static final URI CROSSHAIR = URI.create("file:///ui/crosshair.jsf");
	private static final URI PLAYING_VIEW_WINDOW = URI.create("file:///ui/windows/playing.jwl");
	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/hud/layout.jwl");

	private IStateContext m_context;
	private final World m_world;
	private Window m_playingWindow;

	private final IWindowFactory m_windowFactory;
	private final IParallelWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpriteFactory m_spriteFactory;

	private final Logger m_logger = LoggerFactory.getLogger(Playing.class);

	private final PlayerMovementDirector m_playerMovementDirector = new PlayerMovementDirector();

	private IRpgCharacter m_player = new NullRpgCharacter();

	private Hud m_hud;

	public Playing(IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory, World world) {
		m_windowFactory = windowFactory;
		m_worldFactory = worldFactory;
		m_audioClipFactory = audioClipFactory;
		m_spriteFactory = spriteFactory;
		m_world = world;
	}

	@Override
	public void enter(IStateContext context) {
		m_context = context;
		
		try {
			ISceneBufferFactory sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create());
			FollowCamera camera = new FollowCamera(sceneBufferFactory);

			m_playingWindow = m_windowFactory.create(PLAYING_VIEW_WINDOW, new PlayingWindowBehaviourInjector(camera));
			context.getWindowManager().addWindow(m_playingWindow);
			m_playingWindow.center();
			m_playingWindow.focus();

			IRpgCharacter playerEntityBuffer = m_world.getEntities().getByName(IRpgCharacter.class, "player");

			if (playerEntityBuffer != null) {
				m_player = playerEntityBuffer;
			} else {
				m_logger.error("Character entity was not placed in world. Using null character entity instead.");
			}

			camera.attach(m_world);
			camera.setTarget(m_player);

			m_hud = new HudFactory(context.getWindowManager(), m_windowFactory, HUD_WINDOW).create();
			m_hud.setTopMost(true);
			m_hud.setMovable(false);
			m_hud.center();
			m_hud.setLocation(new Vector2D(m_hud.getLocation().x,
					m_playingWindow.getBounds().height - m_hud.getBounds().height));
		} catch (WindowConstructionException e) {
			m_logger.error("Error occured constructing demo world or world view. Reverting to MainMenu.", e);
			m_context.setState(new MainMenu(m_windowFactory, m_worldFactory, m_audioClipFactory, m_spriteFactory));
		}
	}

	@Override
	public void leave() {
		if (m_playingWindow != null) {
			m_context.getWindowManager().removeWindow(m_playingWindow);
			m_playingWindow.dispose();
		}
	}

	@Override
	public void update(int deltaTime) {
		m_world.update(deltaTime);
		m_playerMovementDirector.update(deltaTime);
	}

	private final class PlayerMovementDirector implements IMovementDirector {

		private Vector2F m_movementDelta = new Vector2F();
		private boolean m_isQueued = false;

		public void setMovementDelta(Vector2F movementDelta) {
			m_movementDelta = new Vector2F(movementDelta);
		}

		public Vector2F getMovementDelta() {
			return new Vector2F(m_movementDelta);
		}

		@Override
		public ISteeringBehavior getBehavior() {
			if (m_movementDelta.isZero()) {
				return new NullSteeringBehavior();
			} else {
				return new DirectionSteeringBehavior(Direction.fromVector(m_movementDelta));
			}
		}

		@Override
		public boolean isDone() {
			boolean isDone = m_movementDelta.isZero();

			if (isDone) {
				m_isQueued = false;
			}

			return isDone;
		}

		public void update(int deltaTime) {
			if (!m_movementDelta.isZero() && !m_isQueued) {
				m_isQueued = true;
				m_player.getMovementResolver().queueTop(this);
			}
		}
	}

	private class PlayingWindowBehaviourInjector extends WindowBehaviourInjector {

		private final ICamera m_camera;

		public PlayingWindowBehaviourInjector(ICamera camera) {
			m_camera = camera;
		}

		@Override
		public void doInject() throws NoSuchControlException {
			final WorldView demoWorldView = getControl(WorldView.class, "worldView");
			final Timer timer = new Timer();

			addControl(timer);

			demoWorldView.setCamera(m_camera);

			demoWorldView.getObservers().add(new IWorldViewInputObserver() {
				@Override
				public void mouseEvent(InputMouseEvent event) {
					if (event.type == MouseEventType.MouseClicked) {
						final Door pickedInteraction = demoWorldView.pick(Door.class, event.location);

						if (pickedInteraction == null) {
							return;
						}
						if (pickedInteraction.isOpen()) {
							pickedInteraction.close();
						} else {
							pickedInteraction.open();
						}
					}
				}

				@Override
				public void keyEvent(InputKeyEvent event) {
					if (event.type == KeyEventType.KeyTyped) {
						return;
					}

					Vector2F vec = m_playerMovementDirector.getMovementDelta();

					switch (event.keyCode) {
						case KeyEvent.VK_UP:
							vec.y = event.type == KeyEventType.KeyDown ? - 1 : 0;
							break;
						case KeyEvent.VK_DOWN:
							vec.y = event.type == KeyEventType.KeyDown ? 1 : 0;
							break;
						case KeyEvent.VK_RIGHT:
							vec.x = event.type == KeyEventType.KeyDown ? 1 : 0;
							break;
						case KeyEvent.VK_LEFT:
							vec.x = event.type == KeyEventType.KeyDown ? -1 : 0;
							break;
					}

					m_playerMovementDirector.setMovementDelta(vec);
				}
			});

			getObservers().add(new IWindowFocusObserver() {
				@Override
				public void onFocusChanged(boolean hasFocus) {
					if (!hasFocus) {
						m_playerMovementDirector.setMovementDelta(new Vector2F());
					}
				}
			});
		}
	}
}
