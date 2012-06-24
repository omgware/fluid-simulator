package com.fluidsimulator;

import java.awt.Toolkit;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.fullscreen = true;
		config.resizable = false;
		config.title = "Fluid Simulator";
		config.vSyncEnabled = false;
		config.width = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		config.height = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		config.useGL20 = false;
		new LwjglApplication(new FluidSimulatorStarter(), config);
	}

}
