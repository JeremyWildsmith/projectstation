package com.jevaengine.spacestation.ui.selectclass;

import io.github.jevaengine.config.*;
import org.apache.commons.lang.NotImplementedException;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class CharacterClassDescription implements ISerializable, Serializable {
    public String name;
    public String description;
    public URI demo;

    @Override
    public void serialize(IVariable target) throws ValueSerializationException {
        throw new NotImplementedException();
    }

    @Override
    public void deserialize(IImmutableVariable source) throws ValueSerializationException {
        try {
            name = source.getChild("name").getValue(String.class);
            description = source.getChild("description").getValue(String.class);
            String sDemo = source.getChild("demo").getValue(String.class);

            demo = new URI(sDemo);
        } catch (NoSuchChildVariableException | URISyntaxException e) {
            throw new ValueSerializationException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterClassDescription that = (CharacterClassDescription) o;
        return name.equals(that.name) &&
                description.equals(that.description) &&
                demo.equals(that.demo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, demo);
    }
}
