/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

/**
 *
 * @author Jeremy
 */
public class WaterLiquid implements ILiquid {

	@Override
	public float getRateOfFlow() {
		return .75F;
	}

	@Override
	public boolean equals(Object obj) {
		return obj.getClass() == this.getClass();
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
