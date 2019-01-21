package com.jevaengine.spacestation.gas;

import com.jevaengine.spacestation.entity.Infrastructure;
import io.github.jevaengine.math.*;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GasSimulationEntity implements IEntity {
    public static String INSTANCE_NAME = "FREE_GAS_SIMULATION";

    private World m_world;

    private final String m_instanceName;

    private final float m_defaultTemperature;

    private HashMap<String, Integer> flags = new HashMap<>();

    //private WorldObserver m_worldObserver = new WorldObserver();

    private Map<GasSimulationNetwork, GasSimulation> simulation = new HashMap<>();
    private Map<GasSimulationNetwork, GasSimulation> cachedSimulation = new HashMap<>();
    private Map<GasSimulationNetwork, List<ISimulationAction>> queuedActions = new HashMap<>();

    private Thread gasSimulationThread = new Thread(new GasSimulationThread());

    private IImmutableSceneModel m_model = new GasSimulationModel();

    public GasSimulationEntity(float defaultTemperature) {
        this.m_instanceName = INSTANCE_NAME;
        m_defaultTemperature = defaultTemperature;

        gasSimulationThread.setPriority(Thread.MIN_PRIORITY);
    }

    public GasMetaData consume(GasSimulationNetwork network, Vector2D location, float volume) {
        synchronized (cachedSimulation) {
            GasSimulation sim = cachedSimulation.get(network);

            if (sim == null)
                return new GasMetaData();

            synchronized (queuedActions) {
                final float volumeData = volume;
                final Vector2D locationData = new Vector2D(location);
                queuedActions.get(network).add((GasSimulation s) -> {
                    s.consume(locationData, volumeData);
                });
            }

            return sim.consume(location, volume);
        }
    }

    public void produce(GasSimulationNetwork network, Vector2D location, GasMetaData gas) {
        synchronized (cachedSimulation) {
            GasSimulation sim = cachedSimulation.get(network);

            if (sim == null)
                return;

            synchronized (queuedActions) {
                final GasMetaData gasData = new GasMetaData(gas);
                final Vector2D locationData = new Vector2D(location);
                queuedActions.get(network).add((GasSimulation s) -> {
                    s.produce(locationData, gasData);
                });
            }

            sim.produce(location, gas);
        }
    }

    public GasMetaData sample(GasSimulationNetwork network, Vector2D location) {
        synchronized (cachedSimulation) {
            GasSimulation sim = cachedSimulation.get(network);

            if (sim == null)
                return new GasMetaData();

            return sim.sample(location);
        }
    }

    public float getVolume(GasSimulationNetwork network, Vector2D location) {
        synchronized (cachedSimulation) {
            GasSimulation sim = cachedSimulation.get(network);

            if (sim == null)
                return GasSimulationNetwork.ENVIRONMENT_UNIT_VOLUME;

            return sim.getVolume(location);
        }
    }

    @Override
    public World getWorld() {
        return m_world;
    }

    @Override
    public void associate(World world) {
        for(IEntity e : world.getEntities().all()) {
            if (e instanceof GasSimulationEntity)
                throw new WorldAssociationException("Cannot have more than one gas simulation per world.");
        }

        m_world = world;


        for(GasSimulationNetwork n : GasSimulationNetwork.values()){
            GasSimulationWorldMapReader reader = n.createReader(m_world);
            simulation.put(n, new GasSimulation(reader, m_defaultTemperature));
            cachedSimulation.put(n, new GasSimulation(reader, m_defaultTemperature));
            queuedActions.put(n, new ArrayList<>());
        }

        //m_world.getObservers().add(m_worldObserver);

        if(!gasSimulationThread.isAlive())
            gasSimulationThread.start();

    }

    @Override
    public void disassociate() {
        //m_world.getObservers().remove(m_worldObserver);
        m_world = null;
        gasSimulationThread.interrupt();
    }

    @Override
    public String getInstanceName() {
        return m_instanceName;
    }

    @Override
    public Map<String, Integer> getFlags() {
        return flags;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IImmutableSceneModel getModel() {
        return m_model;
    }

    @Override
    public IPhysicsBody getBody() {
        return new NonparticipantPhysicsBody();
    }

    @Override
    public IEntityTaskModel getTaskModel() {
        return new NullEntityTaskModel();
    }

    @Override
    public IObserverRegistry getObservers() {
        return new Observers();
    }

    @Override
    public EntityBridge getBridge() {
        return new EntityBridge(this);
    }

    @Override
    public void update(int delta) {
        for(GasSimulation s : simulation.values())
            s.syncGameLoop(simulation);
    }

    @Override
    public void dispose() { }

    /*
    private class WorldObserver implements World.IWorldObserver {
        @Override
        public void addedEntity(IEntity e) { }

        @Override
        public void removedEntity(Vector3F loc, IEntity e) {
            for(GasSimulation s : simulation.values()) {
                if(e instanceof Infrastructure && simulation.get(GasSimulationNetwork.Environment) == s) {
                    int i = 0;
                }
                s.removedEntity(loc);
            }
        }
    }*/

    private class GasSimulationThread implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(GasSimulationThread.class);
        @Override
        public void run() {
            try {
                long lastUpdate = System.currentTimeMillis();
                while (!Thread.interrupted()) {
                    Thread.sleep(1);
                    long currentMillis = System.currentTimeMillis();
                    int elapsed = (int) (currentMillis - lastUpdate);
                    if (elapsed <= 5)
                        continue;

                    lastUpdate = currentMillis;

                    int cycles = 0;
                    synchronized (queuedActions) {
                        for (Map.Entry<GasSimulationNetwork, List<ISimulationAction>> e : queuedActions.entrySet()) {
                            GasSimulation sim = simulation.get(e.getKey());
                            for (ISimulationAction a : e.getValue()) {
                                a.perform(sim);
                            }

                            e.getValue().clear();
                        }
                    }

                    for (Map.Entry<GasSimulationNetwork, GasSimulation> e : simulation.entrySet()) {
                        cycles += e.getValue().update(elapsed);

                        synchronized (cachedSimulation) {
                            cachedSimulation.put(e.getKey(), new GasSimulation(e.getValue()));
                        }
                    }
                }
                logger.info("Gas Simulation Thread interrupted.");

            } catch (Throwable e) {
                logger.error("Exception occurred in Gas Simulation Thread.", e);
            }
        }
    }

    private class GasSimulationModel implements IImmutableSceneModel {
        private static final float CEIL_DEPTH = 2;

        @Override
        public ISceneModel clone() throws SceneModelNotCloneableException {
            throw new SceneModelNotCloneableException();
        }

        @Override
        public Collection<ISceneModelComponent> getComponents(Matrix3X3 projection) {
            /*List<ISceneModelComponent> c = new ArrayList<>();
            Vector3D translation = projection.dot(new Vector3F(1, 1, 0)).round();

            HashMap<Vector2D, GasMetaData> gas = null;
            GasSimulationWorldMapReader reader = null;
            synchronized (cachedSimulation) {
                gas = cachedSimulation.get(GasSimulationNetwork.Environment).getGasMap();
                reader = cachedSimulation.get(GasSimulationNetwork.Environment).getReader();
            }

            for(Map.Entry<Vector2D, GasMetaData> v : gas.entrySet()) {
                if (v.getValue().getTotalMols() < 0.00001f)
                    continue;

                float volume = reader.getVolume(v.getKey());
                Color color = v.getValue().getColor(volume);

                if (color != null) {
                    float depth = CEIL_DEPTH * v.getValue().getPercentColour(volume);
                    Vector3F location = new Vector3F(v.getKey(), 0.1f + depth);
                    c.add(new GasModelComponent(location, translation.x, color));
                }
            }

            return c;*/

            return new ArrayList<>();
        }

        @Override
        public Rect3F getAABB() {
            return new Rect3F(0, 0, 0, Float.MAX_VALUE / 2, Float.MAX_VALUE / 2, Float.MAX_VALUE / 2);
        }

        @Override
        public Direction getDirection() {
            return Direction.Zero;
        }

        @Override
        public PhysicsBodyShape getBodyShape() {
            return new PhysicsBodyShape(PhysicsBodyShape.PhysicsBodyShapeType.Box, getAABB());
        }
    }

    private static class GasModelComponent implements IImmutableSceneModel.ISceneModelComponent {
        private Vector3F location;
        private float dimensions;
        private Color color;
        public GasModelComponent(Vector3F location, float dimensions, Color color) {
            this.location = location;
            this.dimensions = dimensions;
            this.color = color;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean testPick(int x, int y, float scale) {
            return false;
        }

        @Override
        public Rect3F getBounds() {
            return new Rect3F(0,0,0, dimensions, dimensions, 0.1f);
        }

        @Override
        public Vector3F getOrigin() {
            return this.location;//.add(new Vector3F(-0.5F, -0.5F, 0));
        }

        @Override
        public void render(Graphics2D g, int x, int y, float scale) {
            g.setColor(color);
            float width = dimensions * scale;
            float height = dimensions * scale;

            g.fillRect(x - (int)(width / 2), y - (int)(height / 2), Math.round(width), Math.round(height));
        }
    }

    private interface ISimulationAction {
        void perform(GasSimulation s);
    }
}
