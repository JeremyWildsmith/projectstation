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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.jevaengine.spacestation.entity.StationEntityFactory;
import com.jevaengine.spacestation.entity.character.SpaceCharacterFactory;
import com.jevaengine.spacestation.item.StationItemFactory;
import com.jevaengine.spacestation.ui.StationControlFactory;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.audio.NullAudioClipFactory;
import io.github.jevaengine.config.CachedConfigurationFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.json.JsonConfigurationFactory;
import io.github.jevaengine.game.FrameRenderer;
import io.github.jevaengine.game.FrameRenderer.RenderFitMode;
import io.github.jevaengine.game.GameDriver;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.*;
import io.github.jevaengine.joystick.FrameInputSource;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.rpg.dialogue.IDialogueRouteFactory;
import io.github.jevaengine.rpg.dialogue.ScriptedDialogueRouteFactory;
import io.github.jevaengine.rpg.entity.RpgEntityFactory;
import io.github.jevaengine.rpg.entity.character.IRpgCharacterFactory;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.rpg.ui.RpgControlFactory;
import io.github.jevaengine.script.IScriptBuilderFactory;
import io.github.jevaengine.ui.DefaultControlFactory;
import io.github.jevaengine.ui.IControlFactory;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.TiledEffectMapFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.pathfinding.AStarRouteFactory;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.scene.model.ExtentionMuxedAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.ExtentionMuxedSceneModelFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory;
import io.github.jevaengine.world.scene.model.particle.DefaultParticleEmitterFactory;
import io.github.jevaengine.world.scene.model.particle.IParticleEmitterFactory;
import io.github.jevaengine.world.scene.model.sprite.SpriteSceneModelFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

public class Main implements WindowListener
{
	private static final int WINX = 1280;
	private static final int WINY = 1024;

	private static boolean IS_FULL_SCREEN = false;
	private int m_displayX = WINX;
	private int m_displayY = WINY;

	private JFrame m_frame;
	private GameDriver m_gameDriver;
	
	public static void main(String[] args)
	{
		System.setProperty("sun.java2d.opengl", "true");
		Main m = new Main();
		m.entry(args);
	}

	public void entry(String[] args)
	{
		m_frame = new JFrame();
		try
		{
			if (args.length >= 1)
			{
				String[] resolution = args[0].split("x");

				if (resolution.length != 2)
					throw new RuntimeException("Invalid resolution");

				try
				{
					m_displayX = Integer.parseInt(resolution[0]);
					m_displayY = Integer.parseInt(resolution[1]);
				} catch (NumberFormatException e)
				{
					throw new RuntimeException("Resolution improperly formed");
				}
			}

			if (m_displayX <= 0 || m_displayY <= 0)
			{
				m_displayX = WINX;
				m_displayY = WINY;
			}
			
			SwingUtilities.invokeAndWait(new Runnable()
			{

				public void run()
				{
					m_frame.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().getImage(""), new Point(), "trans"));

					m_frame.setIgnoreRepaint(true);
					m_frame.setBackground(Color.black);
					m_frame.setResizable(false);
					m_frame.setTitle("Project Station");
					m_frame.setSize(m_displayX, m_displayY);
					//m_frame.setUndecorated(true);
					m_frame.setVisible(true);
					m_frame.addWindowListener(Main.this);
					m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				}
			});
		} catch (RuntimeException | InterruptedException | InvocationTargetException ex)
		{
			throw new RuntimeException(ex);
			//JOptionPane.showMessageDialog(null, "An error occured initializing the game: " + ex);
			//return;
		}

		Injector injector = Guice.createInjector(new EngineModule());
		m_gameDriver = injector.getInstance(GameDriver.class);
		m_gameDriver.begin();
		//m_frame.dispose();
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		m_gameDriver.stop();
	}

	private final class EngineModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(IRouteFactory.class).to(AStarRouteFactory.class);
			bind(IInputSource.class).toInstance(FrameInputSource.create(m_frame));
			bind(IGameFactory.class).to(StationGameFactory.class);
			bind(IRpgCharacterFactory.class).to(SpaceCharacterFactory.class);
			bind(IEntityFactory.class).toProvider(new Provider<IEntityFactory>() {
				@Inject
				private IScriptBuilderFactory scriptBuilderFactory;

				@Inject
				private IAudioClipFactory audioClipFactory;

				@Inject
				private IConfigurationFactory configurationFactory;

				@Inject
				private IRpgCharacterFactory characterFactory;

				@Inject
				private IParticleEmitterFactory particleEmitterFactory;

				@Inject
				private IAnimationSceneModelFactory animationSceneModelFactory;

				@Inject
				private IAssetStreamFactory assetStreamFactory;

				@Inject
				private IItemFactory itemFactory;

				@Inject
				private IRouteFactory rotueFactory;

				@Inject
				private ISceneModelFactory modelFactory;

				@Override
				public IEntityFactory get() {
					IEntityFactory base = new RpgEntityFactory(scriptBuilderFactory, audioClipFactory, configurationFactory, characterFactory, particleEmitterFactory, animationSceneModelFactory, modelFactory);
					return new StationEntityFactory(base, itemFactory, configurationFactory, animationSceneModelFactory, assetStreamFactory, rotueFactory);
				}
			});

			bind(IWorldFactory.class).to(SpaceStationFactory.class);

			bind(IDialogueRouteFactory.class).to(ScriptedDialogueRouteFactory.class);
			bind(IItemFactory.class).to(StationItemFactory.class);
			
			bind(IControlFactory.class).toProvider(new Provider<IControlFactory>() {
				@Inject
				private IGraphicFactory graphicFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Override
				public IControlFactory get() {
					IControlFactory baseFactory = new RpgControlFactory(new DefaultControlFactory(configurationFactory), configurationFactory);
					return new StationControlFactory(baseFactory, configurationFactory, graphicFactory);
				}
			});

			bind(IEffectMapFactory.class).to(TiledEffectMapFactory.class);
			bind(IRenderer.class).toInstance(new FrameRenderer(m_frame, IS_FULL_SCREEN, RenderFitMode.Stretch));
			bind(IParticleEmitterFactory.class).to(DefaultParticleEmitterFactory.class);
			
			bind(IAssetStreamFactory.class).toProvider(new Provider<IAssetStreamFactory>() {
				@Override
				public IAssetStreamFactory get() {
					return new StationAssetStreamFactory(Paths.get("").toUri());
				}
			});
			
			bind(IConfigurationFactory.class).toProvider(new Provider<IConfigurationFactory>(){
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Override
				public IConfigurationFactory get() {
					return new CachedConfigurationFactory(new JsonConfigurationFactory(assetStreamFactory));
				}
			});
			
			bind(IGraphicFactory.class).toProvider(new Provider<IGraphicFactory>() {
				@Inject
				private IConfigurationFactory configurationFactory;

				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private IRenderer renderer;
				
				@Override
				public IGraphicFactory get() {
					ExtentionMuxedGraphicFactory muxedGraphicFactory = new ExtentionMuxedGraphicFactory(new BufferedImageGraphicFactory(renderer, assetStreamFactory));
					IGraphicFactory graphicFactory = new CachedGraphicFactory(muxedGraphicFactory);
					muxedGraphicFactory.put(".sgf", new ShadedGraphicFactory(new DefaultGraphicShaderFactory(this, configurationFactory), graphicFactory, configurationFactory));
					return graphicFactory;
				}
			});
			
			bind(ISceneModelFactory.class).toProvider(new Provider<ISceneModelFactory>() {
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private ISpriteFactory spriteFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Inject
				private IAudioClipFactory audioClipFactory;
				
				@Inject
				private IParticleEmitterFactory particleEmitterFactory;
				
				@Override
				public ISceneModelFactory get() {
					ExtentionMuxedSceneModelFactory muxedFactory = new ExtentionMuxedSceneModelFactory(new SpriteSceneModelFactory(configurationFactory, spriteFactory, audioClipFactory));
					muxedFactory.put("jpar", particleEmitterFactory);
					
					return muxedFactory;
				}
			});
			
			
			bind(IAnimationSceneModelFactory.class).toProvider(new Provider<IAnimationSceneModelFactory>() {
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private ISpriteFactory spriteFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Inject
				private IAudioClipFactory audioClipFactory;
				
				@Inject
				private IParticleEmitterFactory particleEmitterFactory;
				
				@Override
				public IAnimationSceneModelFactory get() {
					ExtentionMuxedAnimationSceneModelFactory muxedFactory = new ExtentionMuxedAnimationSceneModelFactory(new SpriteSceneModelFactory(configurationFactory, spriteFactory, audioClipFactory));
					muxedFactory.put("jpar", particleEmitterFactory);
					
					return muxedFactory;
				}
			});
		}
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) { }
	
	@Override
	public void windowClosed(WindowEvent arg0) { }

	@Override
	public void windowDeactivated(WindowEvent arg0) { }

	@Override
	public void windowDeiconified(WindowEvent arg0) { }

	@Override
	public void windowIconified(WindowEvent arg0) { }

	@Override
	public void windowOpened(WindowEvent arg0) { }
}
