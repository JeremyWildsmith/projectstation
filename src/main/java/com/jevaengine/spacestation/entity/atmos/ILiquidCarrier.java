/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.atmos;

import com.jevaengine.spacestation.entity.power.IDevice;
/**
 *
 * @author Jeremy
 */
public interface ILiquidCarrier extends IDevice {
	boolean isFreeFlow();
	float getVolume();

	interface ILiquidCarrierObserver {
		void freeFlowChanged();
	}
}
