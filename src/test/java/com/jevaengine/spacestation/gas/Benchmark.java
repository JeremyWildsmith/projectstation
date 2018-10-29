package com.jevaengine.spacestation.gas;

import io.github.jevaengine.math.Vector2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Benchmark {

    public static void main(String[] args) {
        int width = 200;
        int height = 200;

        System.out.println("Simulating " + (width * height) + " tiles of gas");

        final boolean isBlocking[][] = new boolean[width][height];
        final boolean isAirTight[][] = new boolean[width][height];
        final float heatConductivity[][] = new float[width][height];

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                isAirTight[x][y] = true;
                isBlocking[x][y] = (x == 0 || y == 0 || x == (width - 1) || y == (height - 1));
                if(x == 5) {
                    isBlocking[5][y] = true;
                }
                heatConductivity[x][y] = isBlocking[x][y] ? 0.0f : 1.0f;
            }
        }

        GasSimulation sim = new GasSimulation(new GasSimulation.WorldMapReader() {
            @Override
            public Set<Vector2D> syncWithWorld() {
                return new HashSet<>();
            }

            @Override
            public GasSimulation.WorldMapReader duplicate() {
                return this;
            }

            @Override
            public float getVolume(Vector2D location) {
                return 1.0f;
            }

            @Override
            public boolean isConnected(Vector2D locationA, Vector2D locationB) {

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
        }, 260);

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                if(isBlocking[x][y])
                    continue;

                if(x > 5) {

                    Map<GasType, Float> gas = new HashMap<>();
                    gas.put(GasType.Oxygen, (float) (7.0F + Math.random() * 50));
                    float temperature = (float) (250 + 100 * Math.random());
                    sim.produce(new Vector2D(x, y), new GasSimulation.GasMetaData(gas, temperature));
                }
            }
        }
        while(true) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
                long s = System.nanoTime();
                int cycles = sim.update(100);
                System.out.println((System.nanoTime() - s) / 1000000);
                System.out.println("Cycles: " + cycles);
/*
                System.out.println("====================");
                for(int y = 0; y < height; y++) {
                    for(int x = 0; x < width; x++) {
                        //kPa Pressure
                        System.out.printf("%-3.2f | ", sim.sample(new Vector2D(x, y)).temperature);
                        //Moles
                        //System.out.printf("%-3.2f | ", sim.sample(new Vector2D(x, y)).getTotalMols());
                    }
                    System.out.println();
                }*/

            } catch (InterruptedException e) {}
        }
    }
}
