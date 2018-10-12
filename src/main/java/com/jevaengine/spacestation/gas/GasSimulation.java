package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.*;
import io.github.jevaengine.world.Direction;

import java.util.*;
import java.util.List;

public class GasSimulation {

    private static final int NORMALIZE_INTERVAL = 100;

    private static final float ACTIVE_THRESHOLD = 0.1F;

    private static final int ACTIVE_TIMEOUT = 10000;

    private static final int MAP_CACHE_DUMP_INTERVAL = 1000;

    private static final int SCALER = 1;

    private WorldMapReader m_world;

    public HashMap<Vector2D, Float> gasMappings = new HashMap<>();

    private HashMap<String, Integer> m_flags = new HashMap<>();

    private int sinceLastTick = 0;
    private int sinceMapCacheDump = 0;


    private HashMap<Vector2D, Integer> activeLastPass = new HashMap<>();
    private Set<Vector2D> processedLastPass = new HashSet<>();

    private HashMap<Vector2D, Boolean> isAirTightMap = new HashMap<>();
    private HashMap<Vector2D, Boolean> isBlockingMap = new HashMap<>();

    private Set<Vector2D> queueDeactivate = new HashSet<>();
    private Set<Vector2D> queueActivate = new HashSet<>();

    private List<HashSet<Vector2D>> groups = new ArrayList<>();

    private final int flowRate;

    public GasSimulation(WorldMapReader world, float flowRateRatio) {
        flowRate = Math.round(NORMALIZE_INTERVAL * flowRateRatio);
        m_world = world;
    }

    public HashSet<Vector2D> getGroup(Vector2D member) {
        for(HashSet<Vector2D> e : groups)
        {
            if(e.contains(member))
                return e;
        }

        HashSet<Vector2D> g = new HashSet<>();
        g.add(member);
        groups.add(g);

        return g;
    }

    public float consume(Vector2D location, float volume) {
        if(!gasMappings.containsKey(location))
            return 0;

        float current = gasMappings.get(location);
        float consumed = Math.min(current, volume);

        current -= consumed;
        gasMappings.put(location, current);
        activeLastPass.put(location, 0);

        return consumed / SCALER;
    }

    public void produce(Vector2D location, float volume) {
        if(!m_world.isAirTight(location))
            return;

        if(!gasMappings.containsKey(location))
            gasMappings.put(location, 0.0F);

        float current = gasMappings.get(location);
        current += volume * SCALER;

        gasMappings.put(location, current);
        activeLastPass.put(location, 0);
    }

    public void set(Vector2D location, float volume) {
        if(!m_world.isAirTight(location))
            return;

        gasMappings.put(location, volume * SCALER);
        activeLastPass.put(location, 0);
    }

    public float getQuantity(Vector2D location) {
        if(!gasMappings.containsKey(location))
            return 0;

        activeLastPass.put(location, 0);

        return gasMappings.get(location) / SCALER;
    }


    public float getAreaQuantity(Vector2D location) {
        if(!gasMappings.containsKey(location))
            return 0;

        activeLastPass.put(location, 0);

        float total = 0;
        for(Vector2D v : getGroup(location))
        {
            total += getQuantity(location);
        }

        return total;
    }

    private void normalize(Vector2D origin, int deltaTime) {
        Queue<Vector2D> toProcess = new LinkedList<>();
        HashSet<Vector2D> processed = new HashSet<>();
        toProcess.add(origin);

        float biggestDelta = 0;
        Vector2D biggestDeltaLocation = null;

        HashSet<Vector2D> group = getGroup(origin);
        group.clear();

        while(!toProcess.isEmpty()) {
            Vector2D location = toProcess.remove();
            group.add(location);

            processed.add(location);

            if(processedLastPass.contains(location))
                continue;

            Map<Direction, Float> giveMappings = new HashMap<>();

            float thisValue = gasMappings.containsKey(location) ? gasMappings.get(location) : 0;

            if(!m_world.isAirTight(location))
                thisValue = 0;

            float totalGive = 0;

            float highestNeighbor = 0;
            float lowestNeighbor = 0;

            for(int i = 0; i < 2; i++) {
                for (Direction d : Direction.HV_DIRECTIONS) {
                    Vector2D normDest = location.add(d.getDirectionVector());

                    if (processed.contains(normDest))
                        continue;

                    if (m_world.isBlocking(normDest))
                        continue;

                    float cellValue = 0;
                    if (m_world.isAirTight(normDest)) {
                        if (!gasMappings.containsKey(normDest))
                            gasMappings.put(normDest, 0.0F);

                        cellValue = gasMappings.get(normDest);
                    } else {
                        gasMappings.remove(normDest);
                        cellValue = 0;
                    }

                    float difference = thisValue - cellValue;
                    //If i have more quantity than them
                    if (difference >= 0 && i == 1) {
                        highestNeighbor = Math.max(highestNeighbor, cellValue);
                        lowestNeighbor = Math.min(lowestNeighbor, cellValue);

                        totalGive += difference;

                        giveMappings.put(d, difference);
                    } else if (difference < -ACTIVE_THRESHOLD && i == 0) {
                        float take = -difference * (((float) flowRate) / deltaTime);
                        gasMappings.put(normDest, gasMappings.get(normDest) - take);
                        thisValue += take;
                    }
                }
            }

            float maxDistributable = Math.min(totalGive, thisValue);

            if(maxDistributable > biggestDelta) {
                biggestDelta = maxDistributable;
                biggestDeltaLocation = location;
            }

            float amountDistributable = Math.min(maxDistributable, maxDistributable * (((float)flowRate) / deltaTime));
            float amountLeft = thisValue - amountDistributable;
            float remainingDifference =  thisValue - amountLeft;

            totalGive += remainingDifference;

            boolean processNeighbors = true;
            if(amountDistributable < ACTIVE_THRESHOLD) {
                if(activeLastPass.containsKey(location))
                    queueDeactivate.add(location);

                processNeighbors = false;
            } else
                queueActivate.add(location);

            if(totalGive < ACTIVE_THRESHOLD) {
                gasMappings.put(location, thisValue);
                continue;
            }

            gasMappings.put(location, amountLeft + amountDistributable * remainingDifference / totalGive);

            for(Map.Entry<Direction, Float> e : giveMappings.entrySet()) {

                float giveAmount = amountDistributable * (e.getValue()  / totalGive);

                Vector2D normDest = location.add(e.getKey().getDirectionVector());

                boolean destIsAirTight = m_world.isAirTight(normDest);


                if(processNeighbors && destIsAirTight && !processed.contains(normDest) && !toProcess.contains(normDest))
                    toProcess.add(normDest);

                if(destIsAirTight) {
                    float destValue = gasMappings.containsKey(normDest) ? gasMappings.get(normDest) : 0;

                    gasMappings.put(normDest, destValue + giveAmount);
                }
            }
        }

        if(biggestDeltaLocation != null)
            queueActivate.add(biggestDeltaLocation);

        processedLastPass.addAll(processed);
    }

    /*
    @Override
    public IImmutableSceneModel getModel() {
        return new DecoratedSceneModel(new NullSceneModel(), new ModelView());
    }*/

    public void update(int delta) {
        sinceLastTick += delta;

        if(sinceMapCacheDump >= MAP_CACHE_DUMP_INTERVAL) {
            sinceMapCacheDump = 0;
            isAirTightMap.clear();
            isBlockingMap.clear();
        } else
            sinceMapCacheDump += delta;

        processedLastPass.clear();
        if(sinceLastTick > NORMALIZE_INTERVAL) {
            sinceLastTick -= NORMALIZE_INTERVAL;

            List<Vector2D> remove = new ArrayList<>();
            List<Vector2D> refresh = new ArrayList<>();

            for(Map.Entry<Vector2D, Integer> e : activeLastPass.entrySet()) {
                if (!processedLastPass.contains(e.getKey()))
                    normalize(e.getKey(), NORMALIZE_INTERVAL);

                int newLife = e.getValue() + NORMALIZE_INTERVAL;

                if(newLife > ACTIVE_TIMEOUT)
                    remove.add(e.getKey());
                else
                    refresh.add(e.getKey());
            }

            for(Vector2D v : queueActivate)
                activeLastPass.put(v, 0);

            queueActivate.clear();

            remove.removeAll(queueDeactivate);
            for(Vector2D v : remove)
                activeLastPass.remove(v);

            refresh.removeAll(queueDeactivate);
            for(Vector2D v : refresh)
                activeLastPass.put(v, activeLastPass.get(v) + NORMALIZE_INTERVAL);

            for(Vector2D v : queueDeactivate)
                activeLastPass.remove(v);

            queueDeactivate.clear();

        }
    }

    /*
    private class ModelView implements IImmutableSceneModel.ISceneModelComponent {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public boolean testPick(int x, int y, float scale) {
            return false;
        }

        @Override
        public Rect3F getBounds() {
            return new Rect3F(0, 0, 100, 100, 100, 100);
        }

        @Override
        public Vector3F getOrigin() {
            return new Vector3F(0, 0, 100);
        }

        @Override
        public void render(Graphics2D g, int x, int y, float scale) {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, 400,400);
            g.setColor(Color.white);
            for(Map.Entry<Vector2D, Float> e : gasMappings.entrySet()) {
                int xPos = x + (e.getKey().x - 6) * 40;
                int yPos = x + (e.getKey().y - 6) * 40;

                g.drawString(String.format("%.02f", e.getValue()),xPos, yPos);
            }
        }
    }
    */

    public void removedEntity(Vector3F loc){
        Vector2D location = loc.getXy().round();

        if(m_world.isAirTight(location))
            return;

        for(Direction d : Direction.ALL_DIRECTIONS) {
            Vector2D v = location.add(d.getDirectionVector());

            if(!m_world.isBlocking(v) && m_world.isAirTight(v)) {
                if(!gasMappings.containsKey(location))
                    gasMappings.put(location, 0.0f);

                activeLastPass.put(location, 0);

                return;
            }
        }
    }

    public interface WorldMapReader {
        boolean isBlocking(Vector2D location);
        boolean isAirTight(Vector2D location);
    }
}
