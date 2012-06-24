package com.fluidsimulator;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		//JoglApplicationConfiguration config = new JoglApplicationConfiguration();
		config.fullscreen = true;
		config.resizable = false;
		config.title = "Fluid Simulator";
		config.vSyncEnabled = false;
		config.width = 1920;
		config.height = 1080;
		config.useGL20 = false;
		new LwjglApplication(new FluidSimulatorStarter(), config);
		//new JoglApplication(new FluidSimulatorStarter(), config);
	}

}
