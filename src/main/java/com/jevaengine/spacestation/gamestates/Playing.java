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
import io.github.jevaengine.IDisposable;
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
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.dialogue.IDialogueListenerSession;
import io.github.jevaengine.rpg.dialogue.IDialogueSpeakerSession;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.character.IDialogueResolver.IDialogueResolverObserver;
import io.github.jevaengine.rpg.entity.character.IMovementResolver.IMovementDirector;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter.NullRpgCharacter;
import io.github.jevaengine.rpg.entity.character.tasks.AttackTask;
import io.github.jevaengine.rpg.spell.ISpellFactory;
import io.github.jevaengine.rpg.ui.CharacterDialogueQueryFactory;
import io.github.jevaengine.rpg.ui.CharacterDialogueQueryFactory.CharacterDialogueQuery;
import io.github.jevaengine.rpg.ui.CharacterDialogueQueryFactory.ICharacterDialogueQuerySessionObserver;
import io.github.jevaengine.rpg.ui.LoadoutQueryFactory;
import io.github.jevaengine.rpg.ui.LoadoutQueryFactory.LoadoutQuery;
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
import io.github.jevaengine.util.Nullable;
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
import java.util.LinkedList;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jeremy
 */
public class Playing implements IState
{
	private static final URI CROSSHAIR = URI.create("file:///ui/crosshair.jsf");
	private static final URI PLAYING_VIEW_WINDOW = URI.create("file:///ui/windows/playing.jwl");
	private static final URI CHARACTER_DIALOGUE_LAYOUT = URI.create("file:///ui/windows/dialogue/dialogue.jwl");
	private static final URI LOADOUT_WINDOW = URI.create("file:///ui/windows/hud/loadout/layout.jwl");
	
	private IStateContext m_context;
	private final World m_world;
	private Window m_playingWindow;
	
	private final IWindowFactory m_windowFactory;
	private final IParallelWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpriteFactory m_spriteFactory;
	
	private final Logger m_logger = LoggerFactory.getLogger(Playing.class);

	private final PlayerMovementDirector m_playerMovementDirector = new PlayerMovementDirector();
	private PlayerDialogueHandler m_playerDialogueHandler;
	
	private IRpgCharacter m_player = new NullRpgCharacter();

	private final ISpellFactory m_spellFactory;

	private LoadoutQuery m_loadoutView;
	
	public Playing(IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory, World world, ISpellFactory spellFactory)
	{
		m_windowFactory = windowFactory;
		m_worldFactory = worldFactory;
		m_audioClipFactory = audioClipFactory;
		m_spriteFactory = spriteFactory;
		m_world = world;
		m_spellFactory = spellFactory;
	}
	
	@Override
	public void enter(IStateContext context)
	{
		m_context = context;
		
		IRenderable crosshair = new NullGraphic();
		try
		{
			crosshair = m_spriteFactory.create(CROSSHAIR);
		} catch (SpriteConstructionException e)
		{
			m_logger.error("Error constructing crosshair sprite. Using null crosshair sprite.", e);
		}
		
		try
		{
			ISceneBufferFactory sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create());
			FollowCamera camera = new FollowCamera(sceneBufferFactory);
			
			m_playingWindow = m_windowFactory.create(PLAYING_VIEW_WINDOW, new PlayingWindowBehaviourInjector(camera, crosshair));
			context.getWindowManager().addWindow(m_playingWindow);	
			m_playingWindow.center();
			m_playingWindow.focus();
			
			IRpgCharacter playerEntityBuffer = m_world.getEntities().getByName(IRpgCharacter.class, "player");
			
			if(playerEntityBuffer != null)
				m_player = playerEntityBuffer;
			else
				m_logger.error("Character entity was not placed in world. Using null character entity instead.");
			
			camera.attach(m_world);
			camera.setTarget(m_player);
	
			m_playerDialogueHandler = new PlayerDialogueHandler(new CharacterDialogueQueryFactory(context.getWindowManager(), m_windowFactory, CHARACTER_DIALOGUE_LAYOUT, sceneBufferFactory));
	
			m_player.getDialogueResolver().getObservers().add(m_playerDialogueHandler);
			
			//m_loadoutView = new LoadoutQueryFactory(context.getWindowManager(), m_windowFactory, LOADOUT_WINDOW).create(m_player.getLoadout());
			//m_loadoutView.setTopMost(true);
		} catch (WindowConstructionException e)
		{
			m_logger.error("Error occured constructing demo world or world view. Reverting to MainMenu.", e);
			m_context.setState(new MainMenu(m_windowFactory, m_worldFactory, m_audioClipFactory, m_spriteFactory, m_spellFactory));
		}
	}	

	@Override
	public void leave()
	{
		if(m_playingWindow != null)
		{
			m_context.getWindowManager().removeWindow(m_playingWindow);
			m_playingWindow.dispose();
		}
	}
	
	@Override
	public void update(int deltaTime)
	{		
		m_world.update(deltaTime);
		m_playerMovementDirector.update(deltaTime);
	}
	
	private final class PlayerMovementDirector implements IMovementDirector
	{
		private Vector2F m_movementDelta = new Vector2F();
		private boolean m_isQueued = false;
	
		public void setMovementDelta(Vector2F movementDelta)
		{
			m_movementDelta = new Vector2F(movementDelta);
		}
		
		public Vector2F getMovementDelta()
		{
			return new Vector2F(m_movementDelta);
		}
		
		@Override
		public ISteeringBehavior getBehavior()
		{
			if(m_movementDelta.isZero())
				return new NullSteeringBehavior();
			else
				return new DirectionSteeringBehavior(Direction.fromVector(m_movementDelta));
		}

		@Override
		public boolean isDone()
		{
			boolean isDone = m_movementDelta.isZero();
			
			if(isDone)
				m_isQueued = false;
			
			return isDone;
		}
		
		public void update(int deltaTime)
		{
			if(!m_movementDelta.isZero() && !m_isQueued)
			{
				m_isQueued = true;
				m_player.getMovementResolver().queueTop(this);
			}
		}
	}
	
	private class PlayerDialogueHandler implements IDialogueResolverObserver, IDisposable
	{
		private final CharacterDialogueQueryFactory m_dialogueQueryFactory;
		private final Queue<IDialogueListenerSession> m_listenQueue = new LinkedList<>();
		
		@Nullable
		private CharacterDialogueQuery m_currentQuery = null;
		
		public PlayerDialogueHandler(CharacterDialogueQueryFactory dialogueQueryFactory)
		{
			m_dialogueQueryFactory = dialogueQueryFactory;
		}
		
		@Override
		public void dispose()
		{
			if(m_currentQuery != null)
			{
				m_currentQuery.dispose();
				m_currentQuery = null;
			}
			
			for(IDialogueListenerSession s : m_listenQueue)
				s.cancel();
			
			m_listenQueue.clear();
		}
			
		private boolean processNextSession()
		{
			if(m_currentQuery != null || m_listenQueue.isEmpty())
				return false;
			
			IDialogueListenerSession next = m_listenQueue.remove();
			
			if(!next.isActive())
				return processNextSession();
			
			try
			{
				m_currentQuery = m_dialogueQueryFactory.create(next);
				m_playingWindow.setFocusable(false);
				
				Rect2D playingBounds = m_playingWindow.getBounds();
				Vector2D location = new Vector2D(playingBounds.width / 2 - m_currentQuery.getBounds().width / 2,
												 playingBounds.height - m_currentQuery.getBounds().height - 20);
				
				m_currentQuery.setLocation(location);
				m_currentQuery.setMovable(false);
				m_currentQuery.focus();
				m_currentQuery.getObservers().add(new ICharacterDialogueQuerySessionObserver() {
					@Override
					public void sessionEnded()
					{
						m_playingWindow.setFocusable(true);
						m_playingWindow.focus();
						m_currentQuery.dispose();
						m_currentQuery = null;	
						processNextSession();
					}
				});	
			} catch (WindowConstructionException e)
			{
				next.cancel();
				m_logger.error("Error occured attempting to listen to player dialogue session", e);
				return false;
			}
			
			return true;
		}
		
		@Override
		public void speaking(IDialogueSpeakerSession session) { }

		@Override
		public void listening(IDialogueListenerSession session)
		{
			m_listenQueue.add(session);
			processNextSession();
		}
	}
	
	private class PlayingWindowBehaviourInjector extends WindowBehaviourInjector
	{
		private final ICamera m_camera;
		private final IRenderable m_crosshair;
		private WeakReference<IRpgCharacter> m_lastTarget = new WeakReference<>(null);
		
		public PlayingWindowBehaviourInjector(ICamera camera, IRenderable crosshair)
		{
			m_camera = camera;
			m_crosshair = crosshair;
		}
		
		@Override
		public void doInject() throws NoSuchControlException
		{
			final WorldView demoWorldView = getControl(WorldView.class, "worldView");
			final MenuStrip menuStrip = new MenuStrip();
			final Viewport crosshairView = new Viewport(0, 0);
			final Timer timer = new Timer();
			
			crosshairView.setView(m_crosshair);
			crosshairView.setLocation(new Vector2D(150, 150));
			crosshairView.setVisible(false);
			
			addControl(menuStrip);
			addControl(crosshairView);
			addControl(timer);
			
			crosshairView.setStyle(new NullUIStyle());
			
			timer.getObservers().add(new ITimerObserver() {
				@Override
				public void update(int deltaTime) {
					
					if(m_lastTarget.get() != null)
					{
						if(m_lastTarget.get().getWorld() != m_player.getWorld())
							m_lastTarget.clear();
						else
						{
							Vector2D location = demoWorldView.translateWorldToScreen(m_lastTarget.get().getBody().getLocation());
							crosshairView.setLocation(location);
						}
					}
					
					crosshairView.setVisible(m_lastTarget.get() != null);
				}
			});
			
			demoWorldView.setCamera(m_camera);
			
			demoWorldView.getObservers().add(new IWorldViewInputObserver() {
				@Override
				public void mouseEvent(InputMouseEvent event)
				{
					if(event.mouseButton == MouseButton.Right)
					{
						if(event.type == MouseEventType.MouseClicked)
						{
							final Door pickedInteraction = demoWorldView.pick(Door.class, event.location);
							
							if(pickedInteraction == null)
								return;
							
							menuStrip.setContext(new String[] {"Open/Close"}, new IMenuStripListener() {
								
								@Override
								public void onCommand(String command) {
									if(command.equals("Open/Close"))
									{	if(pickedInteraction.isOpen())
											pickedInteraction.close();
										else
											pickedInteraction.open();
									}
								}
							});
								
							menuStrip.setLocation(event.location);
						}
					} else if(event.mouseButton == MouseButton.Left && event.type == MouseEventType.MouseReleased)
					{
							final IRpgCharacter pickedInteraction = demoWorldView.pick(IRpgCharacter.class, event.location);
							
							m_lastTarget = new WeakReference(pickedInteraction);
					}
				}
				
				@Override
				public void keyEvent(InputKeyEvent event)
				{
					if(event.type == KeyEventType.KeyTyped)
						return;
					
					Vector2F vec = m_playerMovementDirector.getMovementDelta();

					switch(event.keyCode)
					{
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
					
					if(event.type == KeyEventType.KeyDown)
					{
						switch(event.keyCode)
						{
							case KeyEvent.VK_SPACE:
								if(m_lastTarget.get() != null && !m_player.getTaskModel().isTaskBlocking())
								{
									if(m_player.getAllegianceResolver().isConflictingAllegiance(m_lastTarget.get()))
										m_player.getTaskModel().addTask(new AttackTask(m_lastTarget.get()));
									else
										m_player.getDialogueResolver().listen(m_lastTarget.get());
								}
								break;
							case KeyEvent.VK_I:
								m_loadoutView.setVisible(!m_loadoutView.isVisible());
								break;
						}
					}
				}
			});
			
			getObservers().add(new IWindowFocusObserver() {
				@Override
				public void onFocusChanged(boolean hasFocus)
				{
					if(!hasFocus)
						m_playerMovementDirector.setMovementDelta(new Vector2F());
				}
			});
		}
	}
}
