/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import com.jevaengine.spacestation.liquid.ILiquid;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public interface ILiquidCarrier extends IDevice {
	public float getLiquidVolume();
	public float getSourcedLiquidVolume(List<ILiquidCarrier> requested, float sourceWeight);
	public float getCapacityVolume();
	
	public float add(List<ILiquidCarrier> cause, Map<ILiquid, Float> liquid);
	public Map<ILiquid, Float> remove(List<ILiquidCarrier> cause, float quantity);
}
