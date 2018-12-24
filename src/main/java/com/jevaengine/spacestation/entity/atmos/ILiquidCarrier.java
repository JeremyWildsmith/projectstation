/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.power.IDevice;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.math.Vector2D;

import java.util.Map;

/**
 *
 * @author Jeremy
 */
public interface ILiquidCarrier extends IDevice {
	boolean isFreeFlow();
	float getVolume();
	GasSimulationNetwork getNetwork();

	Map<Vector2D, GasSimulationNetwork> getLinks();

	interface ILiquidCarrierObserver {
		void freeFlowChanged();
		void networkChanged();
		void linksChanged();
	}
}
