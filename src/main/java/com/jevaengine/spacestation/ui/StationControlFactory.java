/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui;

import io.github.jevaengine.config.*;
import io.github.jevaengine.graphics.IGraphicFactory;
import io.github.jevaengine.graphics.IImmutableGraphic;
import io.github.jevaengine.ui.Control;
import io.github.jevaengine.ui.IControlFactory;
import io.github.jevaengine.ui.UnsupportedControlException;
import io.github.jevaengine.util.Nullable;

import javax.inject.Inject;
import java.net.URI;

/**
 *
 * @author Jeremy
 */
public class StationControlFactory implements IControlFactory
{
	private final IControlFactory m_controlFactory;
	private final IConfigurationFactory m_configurationFactory;
	private final IGraphicFactory m_graphicFactory;
	
	@Inject
	public StationControlFactory(IControlFactory controlFactory, IConfigurationFactory configurationFactory, IGraphicFactory graphicFactory)
	{
		m_controlFactory = controlFactory;
		m_configurationFactory = configurationFactory;
		m_graphicFactory = graphicFactory;
	}
	
	@Override
	@Nullable
	public Class<? extends Control> lookup(String className)
	{
		if(className.equals(ToggleIcon.COMPONENT_NAME))
			return ToggleIcon.class;
		else if(className.equals(ToggleInteractionNature.COMPONENT_NAME))
			return ToggleInteractionNature.class;
		else if(className.equals(ToggleHand.COMPONENT_NAME))
			return ToggleHand.class;
		else if(className.equals(SimpleItemContainer.COMPONENT_NAME))
			return SimpleItemContainer.class;
		else
			return m_controlFactory.lookup(className);
	}

	@Override
	public <T extends Control> String lookup(Class<T> controlClass)
	{
		if(controlClass.equals(ToggleIcon.class))
			return ToggleIcon.COMPONENT_NAME;
		else if(controlClass.equals(ToggleInteractionNature.class))
			return ToggleInteractionNature.COMPONENT_NAME;
		else if(controlClass.equals(ToggleHand.class))
			return ToggleHand.COMPONENT_NAME;
		else if(controlClass.equals(SimpleItemContainer.class))
			return SimpleItemContainer.COMPONENT_NAME;
		else
			return m_controlFactory.lookup(controlClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Control> T create(Class<T> controlClass, String instanceName, URI config, IImmutableVariable auxConfig) throws ControlConstructionException
	{
		try
		{
			String configPath = config.getPath();
			IImmutableVariable configVar = new ImmutableVariableOverlay(auxConfig, configPath.isEmpty() || configPath.endsWith("/") ? new NullVariable() : m_configurationFactory.create(config));
		
			if(controlClass.equals(ToggleIcon.class))
			{
				IImmutableGraphic active = m_graphicFactory.create(config.resolve(configVar.getChild("active").getValue(String.class)));
				IImmutableGraphic inactive = m_graphicFactory.create(config.resolve(configVar.getChild("inactive").getValue(String.class)));
				
				return (T)new ToggleIcon(instanceName, active, inactive);
			}else if(controlClass.equals(ToggleInteractionNature.class)) {
				IImmutableGraphic helpGraphic = m_graphicFactory.create(config.resolve(configVar.getChild("help").getValue(String.class)));
				IImmutableGraphic harmGraphic = m_graphicFactory.create(config.resolve(configVar.getChild("harm").getValue(String.class)));
				IImmutableGraphic disarmGraphic = m_graphicFactory.create(config.resolve(configVar.getChild("disarm").getValue(String.class)));
				IImmutableGraphic grabGraphic = m_graphicFactory.create(config.resolve(configVar.getChild("grab").getValue(String.class)));
				
				return (T)new ToggleInteractionNature(instanceName, harmGraphic, helpGraphic, disarmGraphic, grabGraphic);
			}else if(controlClass.equals(ToggleHand.class)) {
				IImmutableGraphic left = m_graphicFactory.create(config.resolve(configVar.getChild("left").getValue(String.class)));
				IImmutableGraphic right = m_graphicFactory.create(config.resolve(configVar.getChild("right").getValue(String.class)));
				IImmutableGraphic active = m_graphicFactory.create(config.resolve(configVar.getChild("active").getValue(String.class)));
				
				return (T)new ToggleHand(instanceName, left, right, active);
			}else if(controlClass.equals(SimpleItemContainer.class)) {
				IImmutableGraphic background = m_graphicFactory.create(config.resolve(configVar.getChild("background").getValue(String.class)));
	
				return (T)new SimpleItemContainer(instanceName, background);
			}else
				return m_controlFactory.create(controlClass, instanceName, config, auxConfig);
		} catch(IConfigurationFactory.ConfigurationConstructionException | 
				ValueSerializationException | 
				NoSuchChildVariableException |
				IGraphicFactory.GraphicConstructionException e)
		{
			throw new ControlConstructionException(controlClass.getName(), e);
		}
	}

	@Override
	public Control create(String controlName, String instanceName, URI config, IImmutableVariable auxConfig) throws ControlConstructionException
	{
		Class<? extends Control> ctrlClass = lookup(controlName);
		
		if(ctrlClass == null)
			throw new ControlConstructionException(controlName, new UnsupportedControlException());
		
		return create(ctrlClass, instanceName, config, auxConfig);
	}
}