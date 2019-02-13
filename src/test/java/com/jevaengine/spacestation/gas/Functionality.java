package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Functionality {

    public static void main(String[] args) {
        int width = 10;
        int height = 3;

        final boolean isBlocking[][] = new boolean[width][height];
        final boolean isAirTight[][] = new boolean[width][height];
        final float heatConductivity[][] = new float[width][height];

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                isAirTight[x][y] = true;
                isBlocking[x][y] = (x == 0 || y == 0 || x == (width - 1) || y == (height - 1));
                heatConductivity[x][y] = 0.0f;
            }
        }

        GasSimulation sim = new GasSimulation(new GasSimulationWorldMapReader() {
            @Override
            public Set<Vector2D> syncWithWorld(Map<GasSimulationNetwork, GasSimulation> n) {
                return new HashSet<>();
            }

            @Override
            public Map<GasSimulationNetwork.ConnectedLinkPair, GasSimulation> getLinks() {
                return new HashMap<>();
            }

            @Override
            public GasSimulationWorldMapReader duplicate() {
                return this;
            }

            @Override
            public float getVolume(Vector2D location) {
                return location.x <= 1 ? 1.0F : 0.5F;
            }

            private boolean isInBounds(Vector2D v) {
                if(v.x < 0 || v.x >= width || v.y < 0 || v.y >= height)
                    return false;

                return true;
            }

            @Override
            public boolean isConnected(Vector2D locationA, Vector2D locationB) {
                if(!isInBounds(locationA) || !isInBounds(locationB))
                    return false;

                return !isBlocking[locationA.x][locationA.y] && !isBlocking[locationB.x][locationB.y];
            }

            @Override
            public boolean isAirTight(Vector2D location) {
                if(location.x < 0 || location.x >= isAirTight.length)
                    return false;

                if(location.y < 0 || location.y >= isAirTight[location.x].length)
                    return false;

                return isAirTight[location.x][location.y];
            }

            @Override
            public float getHeatConductivity(Vector2D location) {
                if(location.x < 0 || location.x >= heatConductivity.length)
                    return 0;

                if(location.y < 0 || location.y >= heatConductivity[location.x].length)
                    return 0;

                return heatConductivity[location.x][location.y];
            }

            @Override
            public float getTemperature(Vector2D location) {
                return 200;
            }
        }, 260);

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                if(isBlocking[x][y])
                    continue;

                    if(x ==1 && y == 1) {
                        Map<GasType, Float> gas = new HashMap<>();
                        gas.put(GasType.Oxygen, 1f);
                        sim.produce(new Vector2D(x, y), new GasMetaData(gas));
                    }
            }
        }
        int updateIterations = 0;
        while(true) {
            try {
                updateIterations++;
                TimeUnit.MILLISECONDS.sleep(10);
                long s = System.nanoTime();
                int cycles = sim.update(10);
                System.out.println((System.nanoTime() - s) / 1000000);
                System.out.println("Cycles: " + cycles);
                System.out.println("UI: " + updateIterations);

                if(updateIterations == 200) {
                    HashMap<GasType, Float> gas = new HashMap<>();
                    gas.put(GasType.Oxygen, 400.0f);
                    sim.produce(new Vector2D(1,1), new GasMetaData(gas));
                }

                System.out.println("====================");
                for(int y = 0; y < height; y++) {
                    for(int x = 0; x < width; x++) {
                        //kPa Pressure
                        if(x >= 1 && x <=2 && y == 1)
                        {
                            int i = 0;
                            i++;
                        }
                        float locationVolume = sim.getVolume(new Vector2D(x,y));
                        float locationTemp = sim.getTemperature(new Vector2D(x,y));
                        GasMetaData data =  sim.sample(new Vector2D(x, y));
                        float pressure = data.calculatePressure(locationVolume, locationTemp);

                        System.out.printf("%-3.2f | ", pressure / 1000.0f);
                        //Moles
                        //System.out.printf("%-3.2f | ", sim.sample(new Vector2D(x, y)).getTotalMols());
                    }
                    System.out.println();
                }

            } catch (InterruptedException e) {}
        }
    }
}
