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
package com.jevaengine.spacestation.item;

import com.google.inject.Provider;
import io.github.jevaengine.config.*;
import io.github.jevaengine.config.IConfigurationFactory.ConfigurationConstructionException;
import io.github.jevaengine.graphics.IGraphicFactory;
import io.github.jevaengine.graphics.IGraphicFactory.GraphicConstructionException;
import io.github.jevaengine.graphics.IImmutableGraphic;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet.IAttributeIdentifier;
import io.github.jevaengine.rpg.IImmutableAttributeSet.IImmutableAttribute;
import io.github.jevaengine.rpg.item.DefaultItem;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItem.IItemFunction;
import io.github.jevaengine.rpg.item.IItem.NullItemFunction;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.NullAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory.SceneModelConstructionException;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public final class StationItemFactory implements IItemFactory {

	private final IConfigurationFactory m_configurationFactory;
	private final IGraphicFactory m_graphicFactory;
	private final IAnimationSceneModelFactory m_modelFactory;
	private final Provider<IEntityFactory> m_entityFactory;
	private final Provider<IItemFactory> m_itemFactory;

	@Inject
	public StationItemFactory(IConfigurationFactory configurationFactory, IGraphicFactory graphicFactory, IAnimationSceneModelFactory modelFactory, Provider<IEntityFactory> entityFactory, Provider<IItemFactory> itemFactory) {
		m_configurationFactory = configurationFactory;
		m_graphicFactory = graphicFactory;
		m_modelFactory = modelFactory;
		m_entityFactory = entityFactory;
		m_itemFactory = itemFactory;
	}

	@Override
	public IItem create(URI name) throws ItemContructionException {
		try {
			UsrItemDeclaration itemDecl = m_configurationFactory.create(name).getValue(UsrItemDeclaration.class);

			IImmutableGraphic icon = itemDecl.icon == null ? new NullGraphic() : m_graphicFactory.create(name.resolve(new URI(itemDecl.icon)));
			IAnimationSceneModel model = itemDecl.model == null ? new NullAnimationSceneModel() : m_modelFactory.create(name.resolve(new URI(itemDecl.model)));

			IItemFunction function = new NullItemFunction();

			if (itemDecl.function != null) {
				String functionName = itemDecl.function;
				for (IItemFunction functions[] : new IItemFunction[][]{SpaceArmorItemFunction.values()}) {
					for (IItemFunction f : functions) {
						if (f.getName().equals(functionName)) {
							function = f;
						}
					}
				}

				for (StationItemFunctionFactory f : StationItemFunctionFactory.values()) {
					if (f.getName().equals(functionName)) {
						function = f.create(m_itemFactory.get(), m_entityFactory.get(), m_modelFactory, itemDecl.parameters);
					}
				}
			}

			return new DefaultItem(itemDecl.name, itemDecl.description, function, itemDecl.attributes, icon, model);
		} catch (ConfigurationConstructionException | GraphicConstructionException | ValueSerializationException | SceneModelConstructionException | URISyntaxException e) {
			throw new ItemContructionException(name, e);
		}
	}

	public static class UsrItemDeclaration implements ISerializable {

		public String name;

		@Nullable
		public String icon;

		@Nullable
		public String model;

		public String function;
		public String description;
		public AttributeSet attributes;

		IImmutableVariable parameters = new NullVariable();

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("name").setValue(name);

			if (icon != null) {
				target.addChild("icon").setValue(icon);
			}

			if (model != null) {
				target.addChild("model").setValue(model);
			}

			target.addChild("function").setValue(function);
			target.addChild("description").setValue(description);

			IVariable attributesVariable = target.addChild("attributes");

			for (Map.Entry<IAttributeIdentifier, IImmutableAttribute> attribute : attributes.getSet()) {
				attributesVariable.addChild(attribute.getKey().getName()).setValue(attribute.getValue().get());
			}
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				name = source.getChild("name").getValue(String.class);

				if (source.childExists("icon")) {
					icon = source.getChild("icon").getValue(String.class);
				}

				if (source.childExists("model")) {
					model = source.getChild("model").getValue(String.class);
				}

				description = source.getChild("description").getValue(String.class);

				attributes = new AttributeSet();
				IImmutableVariable attributesVar = source.getChild("attributes");

				/*
				for (String attributeName : attributesVar.getChildren()) {
					for (IAttributeIdentifier attributeIdentifiers[] : new IAttributeIdentifier[][]{UsrWeaponAttribute.values(), UsrArmorItemAttribute.values()}) {
						for (IAttributeIdentifier attributeIdentifier : attributeIdentifiers) {
							if (attributeIdentifier.getName().equals(attributeName)) {
								attributes.get(attributeIdentifier).set(attributesVar.getChild(attributeName).getValue(Double.class).floatValue());
							}
						}
					}
				}*/

				if(source.childExists("parameters")) {
					parameters = source.getChild("parameters");
				}

				function = source.childExists("function") ? source.getChild("function").getValue(String.class) : null;

			} catch (NoSuchChildVariableException e) {
				throw new ValueSerializationException(e);
			}
		}
	}
}
