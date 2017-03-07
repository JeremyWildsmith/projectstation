/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.liquid;

/**
 *
 * @author Jeremy
 */
public final class GasolineLiquid implements ILiquid {

	@Override
	public float getRateOfFlow() {
		return 0.5F;
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
