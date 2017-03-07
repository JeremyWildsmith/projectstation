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
public enum Liquid  {
	Water(new WaterLiquid()),
	Gasoline(new GasolineLiquid());
	
	private final ILiquid m_liquid;
	Liquid(ILiquid l) {
		m_liquid = l;
	}
	
	ILiquid get() {
		return m_liquid;
	}
	
	public static ILiquid fromName(String name) throws NoSuchLiquidException {
		for(Liquid l : Liquid.values()) {
			if(l.name().equals(name))
				return l.get();
		}
		
		throw new NoSuchLiquidException(name);
	}
	
	public static class NoSuchLiquidException extends Exception {
		public NoSuchLiquidException(String liquid) {
			super("Liquid does not exist: " + liquid);
		}
	}
	
}
