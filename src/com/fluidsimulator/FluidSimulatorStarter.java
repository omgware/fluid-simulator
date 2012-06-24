package com.fluidsimulator;

import com.badlogic.gdx.Game;

public class FluidSimulatorStarter extends Game {
	FluidSimulator fluidSimulatorScreen;

	@Override
	public void create() {
		setScreen(switchToFluidSimulator());
	}
	
	public FluidSimulator switchToFluidSimulator() {
		if (fluidSimulatorScreen == null)
			fluidSimulatorScreen = new FluidSimulator(this);
		return fluidSimulatorScreen;
	}
}
