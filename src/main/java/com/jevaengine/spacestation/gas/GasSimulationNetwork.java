package com.jevaengine.spacestation.gas;

import com.jevaengine.spacestation.entity.BasicDevice;
import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.entity.atmos.ILiquidCarrier;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;

import java.util.*;

public enum GasSimulationNetwork {
    Pipe((World world) -> {
        return new PipeNetworkWorldReader(world);
    }),
    Environment((World world) -> {
        return new EnvironmentWorldReader(world);
    });

    private interface WorldMapReaderFactory {
        GasSimulation.WorldMapReader create(World world);
    }

    public static final float ENVIRONMENT_UNIT_VOLUME = 1.0f;

    WorldMapReaderFactory factory;

    GasSimulationNetwork(WorldMapReaderFactory factory) {
        this.factory = factory;
    }

    public GasSimulation.WorldMapReader createReader(World world) {
        return factory.create(world);
    }

    private static class PipeNetworkWorldReader implements GasSimulation.WorldMapReader, BasicDevice.IDeviceConnectionListener, ILiquidCarrier.ILiquidCarrierObserver, World.IWorldObserver {
        private final World world;
        private boolean isDirty = true;

        private final HashSet<ConnectedPipePair> pipeMap;
        private final HashSet<Vector2D> airTight;
        private final HashMap<Vector2D, Float> volumeMap;

        public PipeNetworkWorldReader(PipeNetworkWorldReader reader) {
            isDirty = false;
            world = reader.world;
            pipeMap = new HashSet(reader.pipeMap);
            airTight = new HashSet(reader.airTight);
            volumeMap = new HashMap<>(reader.volumeMap);
        }

        @Override
        public synchronized GasSimulation.WorldMapReader duplicate() {
            return new PipeNetworkWorldReader(this);
        }

        public PipeNetworkWorldReader(World world) {
            this.world = world;
            pipeMap = new HashSet<>();
            airTight = new HashSet<>();
            volumeMap = new HashMap<>();
        }

        @Override
        public void connectionsChanged() {
            isDirty = true;
        }

        @Override
        public void freeFlowChanged() {
            isDirty = true;
        }

        @Override
        public float getHeatConductivity(Vector2D location) {
            return 1.0f;
        }

        @Override
        public void addedEntity(IEntity e) {
            isDirty = true;
            e.getObservers().add(this);
        }

        @Override
        public void removedEntity(Vector3F loc, IEntity e) {
            isDirty = true;
            e.getObservers().remove(this);
        }

        @Override
        public synchronized Set<Vector2D> syncWithWorld() {
            if(!isDirty)
                return new HashSet<>();

            isDirty = false;

            HashSet<Vector2D> modified = new HashSet<>();

            IEntity entities[] = world.getEntities().all();
            List<ILiquidCarrier> pipeEntities = new ArrayList<>();

            for(IEntity e : entities) {
                if(e instanceof  ILiquidCarrier) {
                    pipeEntities.add((ILiquidCarrier) e);

                    e.getObservers().add(this);
                }
            }

            synchronized (airTight) {
                synchronized (pipeMap) {
                    pipeMap.clear();
                    for (ILiquidCarrier e : pipeEntities) {
                        Vector2D location = e.getBody().getLocation().getXy().round();
                        airTight.add(location);
                        modified.add(location);

                        if(volumeMap.containsKey(location)) {
                            volumeMap.put(location, Math.max(e.getVolume(), volumeMap.get(location)));
                        } else
                            volumeMap.put(location, e.getVolume());

                        for (ILiquidCarrier c : e.getConnections(ILiquidCarrier.class)) {
                            Vector2D connectionLocation = c.getBody().getLocation().getXy().round();

                            modified.add(connectionLocation);
                            airTight.add(connectionLocation);

                            if(c.isFreeFlow() && e.isFreeFlow())
                                pipeMap.add(new ConnectedPipePair(location, connectionLocation));
                        }
                    }
                }
            }

            return modified;
        }

        //These methods are called from another thread.
        @Override
        public boolean isAirTight(Vector2D location) {
            synchronized (airTight) {
                if (airTight.contains(location))
                    return true;

                return false;
            }
        }

        //These methods are called from another thread.
        @Override
        public boolean isConnected(Vector2D a, Vector2D b) {
            synchronized (pipeMap) {
                return pipeMap.contains(new ConnectedPipePair(a, b));
            }
        }

        @Override
        public float getVolume(Vector2D location) {
            synchronized (volumeMap) {
                if(!volumeMap.containsKey(location))
                    return ENVIRONMENT_UNIT_VOLUME;
                else
                    return volumeMap.get(location);
            }
        }
    }

    private static class EnvironmentWorldReader implements GasSimulation.WorldMapReader, World.IWorldObserver, Door.IDoorObserver {
        private final World world;
        private boolean isDirty = true;

        private HashMap<Vector2D, Boolean> isAirTightMap = new HashMap<>();
        private HashMap<Vector2D, Boolean> isBlockingMap = new HashMap<>();
        private HashMap<Vector2D, Float> heatConductivityMap = new HashMap<>();

        public EnvironmentWorldReader(EnvironmentWorldReader reader) {
            isDirty = false;
            world = reader.world;
            isAirTightMap = new HashMap(reader.isAirTightMap);
            isBlockingMap = new HashMap(reader.isBlockingMap);
            heatConductivityMap = new HashMap<>(reader.heatConductivityMap);
        }

        @Override
        public synchronized GasSimulation.WorldMapReader duplicate() {
            return new EnvironmentWorldReader(this);
        }

        public EnvironmentWorldReader(World world) {
            this.world = world;
        }

        @Override
        public void addedEntity(IEntity e) {
            isDirty = true;
        }

        @Override
        public void removedEntity(Vector3F loc, IEntity e) {
            isDirty = true;
        }

        @Override
        public void doorStatusChanged() {
            isDirty = true;
        }

        public synchronized HashSet<Vector2D> syncWithWorld() {
            if(!isDirty)
                return new HashSet<>();

            isDirty = false;

            HashSet<Vector2D> modified = new HashSet<>();
            IEntity entities[] = world.getEntities().all();
            List<Infrastructure> infrastructureEntities = new ArrayList<>();
            List<Door> doors = new ArrayList<>();

            for(IEntity e : entities) {
                if(e instanceof  Infrastructure)
                    infrastructureEntities.add((Infrastructure)e);

                if(e instanceof Door)
                    doors.add((Door)e);
            }

            synchronized (isAirTightMap) {
                isAirTightMap.clear();
                for (Infrastructure e : infrastructureEntities) {
                    Vector2D location = e.getBody().getLocation().getXy().round();
                    boolean airTight = isAirTightMap.containsKey(location) ? isAirTightMap.get(location) : false;
                    airTight = airTight || e.isAirTight();
                    isAirTightMap.put(location, airTight);
                    modified.add(location);
                }//Need to check if air-tight with not air tight. If is at least one airtight, then it is airtight. Could bwe multiple iterations for one location. Soneed to chekc currently mapped value.
            }

            synchronized (isBlockingMap) {
                isBlockingMap.clear();
                for (Infrastructure e : infrastructureEntities) {
                    Vector2D location = e.getBody().getLocation().getXy().round();
                    boolean isBlocking = isBlockingMap.containsKey(location) ? isBlockingMap.get(location) : false;
                    isBlocking = isBlocking || e.getBody().isCollidable();
                    isBlockingMap.put(location, isBlocking);

                    modified.add(location);
                }

                for(Door d : doors) {
                    if(!d.isOpen()) {
                        Vector2D location = d.getBody().getLocation().getXy().round();
                        isBlockingMap.put(location, true);
                        modified.add(location);
                    }

                    d.getObservers().add(this);
                }
            }

            synchronized (heatConductivityMap) {
                heatConductivityMap.clear();
                for (Infrastructure e : infrastructureEntities) {
                    Vector2D location = e.getBody().getLocation().getXy().round();
                    float currentConductivity = heatConductivityMap.containsKey(location) ? heatConductivityMap.get(location) : 1.0f;
                    heatConductivityMap.put(location, Math.min(currentConductivity, e.getHeatConductivity()));
                }
            }

            return modified;
        }

        //These methods are called from another thread.
        @Override
        public boolean isAirTight(Vector2D location) {
            synchronized (isAirTightMap) {
                if (isAirTightMap.containsKey(location))
                    return isAirTightMap.get(location);

                return false;
            }
        }

        //These methods are called from another thread.
        @Override
        public boolean isConnected(Vector2D a, Vector2D b) {
            synchronized (isBlockingMap) {
                float distance = a.difference(b).getLengthSquared();
                if(distance > 2 + Vector2F.TOLERANCE)
                    return false;

                boolean aBlocking = isBlockingMap.containsKey(a) ? isBlockingMap.get(a) : false;
                boolean bBlocking = isBlockingMap.containsKey(b) ? isBlockingMap.get(b) : false;

                return !aBlocking && !bBlocking;
            }
        }


        @Override
        public float getHeatConductivity(Vector2D location) {
            synchronized (heatConductivityMap) {
                if (heatConductivityMap.containsKey(location))
                    return heatConductivityMap.get(location);

                return 0.0f;
            }
        }

        @Override
        public float getVolume(Vector2D location) {
            return ENVIRONMENT_UNIT_VOLUME;
        }
    }


    private static class ConnectedPipePair {
        private Vector2D locationA;
        private Vector2D locationB;

        public ConnectedPipePair(Vector2D a, Vector2D b) {
            locationA = new Vector2D(a);
            locationB = new Vector2D(b);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConnectedPipePair that = (ConnectedPipePair) o;

            return (Objects.equals(locationA, that.locationA) &&
                    Objects.equals(locationB, that.locationB)) ||
                    (Objects.equals(locationB, that.locationA) &&
                            Objects.equals(locationA, that.locationB));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(locationA) * Objects.hashCode(locationB);
        }
    }
}
