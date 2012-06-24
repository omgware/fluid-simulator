package com.fluidsimulator;

import java.util.ArrayList;
import java.util.Iterator;

import javolution.util.FastMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.fluidsimulator.gameobjects.fluid.Particle;
import com.fluidsimulator.gameobjects.fluid.SpatialTable;
import com.fluidsimulator.gameobjects.fluid.Spring;

public class FluidSimulator implements Screen, InputProcessor {

	protected FluidSimulatorStarter game;
	private GL10 gl = null;
	public static final boolean USE_FIXED_TIMESTEP = true;
	// Tune these statics for platform specific behaviors
	public static final boolean IS_DESKTOP = true;
	public static final float LINE_VELOCITY_FACTOR = (IS_DESKTOP) ? 0.3f : 0.3f;
	public static final int LINE_DENSITY_FACTOR = (IS_DESKTOP) ? 5 : 10;
	public static final int WORLD_WIDTH = (IS_DESKTOP) ? 200 : 80;
	public static final int WORLD_HEIGHT = (IS_DESKTOP) ? 120 : 50;
	public static final int INITIAL_HEIGHT = (IS_DESKTOP) ? 5 : 0;
	public static final float TIMESTEP = 0.022f;
	public static float GRAVITY_FORCE = -1.0f;
	public static final Vector2 gravityVect = new Vector2(0.0f, GRAVITY_FORCE);
	public static final int SIZE = 10000;
	public static final int ANDROID_SIZE = 600;
	public static final int MAX_NEIGHBORS = 25;
	public static final int wpadding = (IS_DESKTOP) ? 20 : 10;
	public static final int hpadding = (IS_DESKTOP) ? 20 : 10;
	public static final float collisionForce = (IS_DESKTOP) ? 0.3f : 0.1f;
	public static final int SCREEN_WIDTH = (IS_DESKTOP) ? 1920 : 480;
	public static final int SCREEN_HEIGHT = (IS_DESKTOP) ? 1080 : 320;
	protected SpriteBatch batch;
	protected BitmapFont font;
	protected OrthographicCamera camera;

	private SpatialTable<Particle> particles = new SpatialTable<Particle>(
			WORLD_WIDTH, WORLD_HEIGHT) {

		@Override
		protected int posX(Particle value) {
			return (int) ((value.pos.x + (WORLD_WIDTH / 2) + 0.3f) / CELL_SIZE);
		}

		@Override
		protected int posY(Particle value) {
			return (int) ((value.pos.y + 0.3f) / CELL_SIZE);
		}
		
		@Override
		protected int prevPosX(Particle value) {
			return (int) ((value.prevPos.x + (WORLD_WIDTH / 2) + 0.3f) / CELL_SIZE);
		}

		@Override
		protected int prevPosY(Particle value) {
			return (int) ((value.prevPos.y + 0.3f) / CELL_SIZE);
		}
	};

	public ArrayList<Spring> springs;
	public FastMap<Integer, ArrayList<Integer>> springPresenceTable;
	public Iterator<Spring> springIter;
	public ArrayList<Particle> disposableParticles;
	public ArrayList<Particle> tempParticles;
	// Most of these can be tuned at runtime with F1-F8 and keys 1-8 (no numpad)
	public static int CELL_SIZE = 1;
	public static float H = 20.0f;
	public static float H2 = H * H;
	public static float K = 0.504f;
	public static float K_ = 10.0f;
	public static float K_NEAR = K * K_;
	public static float SIGMA = 0.0f;
	public static final float BETA = 0.3f;
	public static float P0 = 10.0f;
	public static final float ATTRACT_FORCE = ((float) WORLD_WIDTH) / 300;
	public static final float ATTRACT_RANGE = ((float) WORLD_WIDTH) * 2;
	public static final float REPULSE_FORCE = ((float) WORLD_WIDTH) / 30;
	public static final float REPULSE_RANGE = ((float) WORLD_WIDTH) / 2;
	public float EMITTER_FORCE = ((float) WORLD_WIDTH) / 2;
	public float K_SPRING = 0.3f;
	public static final float REST_LENGTH = 5.0f;
	public static final float REST_LENGTH2 = REST_LENGTH * REST_LENGTH;
	
	public static final float YELD_RATIO_STRETCH = 0.5f;
	public static final float YELD_RATIO_COMPRESS = 0.5f;
	public static final float PLASTICITY = 0.5f;
	public static final int VELOCITY_CAP = 150;
	public static final int PARTICLE_SIZE = 20;
	float deformation;
	float dropRadiusK = 0.1f;
	float dropRadius;
	int dropRadiusPixel;
	int dropRadiusPixel2;

	// Temp variables mostly for calculations and graphics processing purpose
	int i;
	int j;
	int z;
	int len;
	int len2;
	int w;
	float q;
	float qq;
	float D;
	float u;
	float I;
	float deltaTime2;
	Vector2 dx = new Vector2(0.0f, 0.0f);
	Vector2 rij = new Vector2(0.0f, 0.0f);
	Vector2 tempVect = new Vector2(0.0f, 0.0f);
	Vector2 tempVect2 = new Vector2(0.0f, 0.0f);
	Vector2 attract_vect = new Vector2(0.0f, 0.0f);
	Vector2 repulse_vect = new Vector2(0.0f, 0.0f);
	boolean isAttracting;
	boolean isRepulsing;
	public float checkTime;
	public float checkTime2;
	public float checkTime3;
	Texture dropTexture;
	Texture dropTexture2;
	Vector3 dropCoordinate = new Vector3(0.0f, 0.0f, 0.0f);
	Vector3 dropSize = new Vector3(0.0f, 0.0f, 0.0f);
	Vector3 tempVect3 = new Vector3(0.0f, 0.0f, 0.0f);
	Particle tempParticle;
	float tempFloat = 0;
	long tempLong = 0;
	boolean tempBoolean = false;
	int tempInt = 0;
	Spring tempSpring;
	Mesh lineMesh;
	float[] lineVertices;
	int vertexIndex = 0;
	protected Vector3 testPoint = new Vector3();
	protected Vector2 testPoint2D = new Vector2();

	// Modes
	int emitType = 3;
	boolean expandMode = false;
	boolean slowMotion = false;
	boolean crazyMode = false;
	boolean hudEnabled = true;
	boolean wireFrame = false;
	boolean linesMode = true;
	boolean smokeMode = false;
	boolean whiteBackground = false;
	boolean particleRendering = true;
	boolean viscoElasticityEnabled = false;
	boolean plasticityEnabled = false;
	boolean initializePlasticity = false;
	boolean initializeViscoelasticity = false;
	boolean exitApp;

	public FluidSimulator(FluidSimulatorStarter fluidSimulatorStarter) {
		this.game = fluidSimulatorStarter;
	}

	private void createWorld() {
		if (IS_DESKTOP) {
			dropTexture = new Texture("data/fluid_drop_red_64.png");
			dropTexture2 = new Texture("data/fluid_drop_blue_64.png");
		}
		dropRadius = 0.1f + dropRadiusK;
		dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
		dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
		if (IS_DESKTOP) {
			lineMesh = new Mesh(false, (SIZE) * 2, 0, new VertexAttribute(Usage.Position, 2, "a_pos"), new VertexAttribute(Usage.ColorPacked, 4, "a_color"));
			lineVertices = new float[(SIZE) * 2 * 2 * 3];
			disposableParticles = new ArrayList<Particle>(SIZE);
		} else {
			lineMesh = new Mesh(false, (ANDROID_SIZE) * 2, 0, new VertexAttribute(Usage.Position, 2, "a_pos"), new VertexAttribute(Usage.ColorPacked, 4, "a_color"));
			lineVertices = new float[(ANDROID_SIZE) * 2 * 2 * 3];
		}

		// On Android populate directly
		if (!IS_DESKTOP) {
			for (float j = INITIAL_HEIGHT + hpadding + 2; j < WORLD_HEIGHT - 2; j += 1.0f) {
				for (float i = -WORLD_WIDTH / 3; i < WORLD_WIDTH / 3; i += 1.0f) {
					particles.add(new Particle(i, j));
					tempParticle = particles.get(particles.size() - 1);
					tempParticle.type = (emitType);
					if (particles.size() >= ANDROID_SIZE)
						return;
				}
			}
		}
	}

	@Override
	public void show() {
		particles.initialize();
		camera = new OrthographicCamera(WORLD_WIDTH, WORLD_HEIGHT);
		camera.position.set(0, (WORLD_HEIGHT / 2) - 1, 0);
		createWorld();
		batch = new SpriteBatch(SIZE);
		font = new BitmapFont();
		Gdx.input.setInputProcessor(this);
	}

	protected void performLogic(float deltaTime) {
		applyViscosity(deltaTime);
		if (!expandMode) {
			len = particles.size();
			for (i=0; i<len; i++) {
				particles.get(i).prevPos.set(particles.get(i).pos);
				particles.get(i).pos.set(particles.get(i).pos.x + (deltaTime * particles.get(i).velocity.x),
						particles.get(i).pos.y + (deltaTime * particles.get(i).velocity.y));
			}
		}

		particles.rehash();

		if (viscoElasticityEnabled) {
			if (initializeViscoelasticity) {
				initializeViscoelasticity();
				initializeViscoelasticity = false;
			}
			calculateViscoelasticity(deltaTime);
		} else if (plasticityEnabled) {
			if (initializePlasticity) {
				initializePlasticity();
				initializePlasticity = false;
			}
			calculatePlasticity(deltaTime);
		}

		doubleDensityRelaxation(deltaTime);

		len = particles.size();
		for (i=0; i<len; i++) {
			particles.get(i).velocity.set((particles.get(i).pos.x - particles.get(i).prevPos.x) / deltaTime,
					(particles.get(i).pos.y - particles.get(i).prevPos.y) / deltaTime);
			applyGravity(particles.get(i));
			wallCollision(particles.get(i));
			attract(particles.get(i));
			repulse(particles.get(i));
			if (IS_DESKTOP)
				capVelocity(particles.get(i).velocity);
			if (IS_DESKTOP)
				prepareDeleteOutOfBoundsParticles(particles.get(i));
			//particles.updatePosition(particles.get(i));
		}
	}
	
	private static void capVelocity(Vector2 v) {
		if (v.x > VELOCITY_CAP)
			v.x = VELOCITY_CAP;
		else if (v.x < -VELOCITY_CAP)
			v.x = -VELOCITY_CAP;
		if (v.y > VELOCITY_CAP)
			v.y = VELOCITY_CAP;
		else if (v.y < -VELOCITY_CAP)
			v.y = -VELOCITY_CAP;
	}

	private void initializePlasticity() {
		springs.clear();
		len = particles.size();
		for (i=0; i<len; i++) {
			for (j=0; j<len; j++) {
				if (particles.get(i).hashCode() == particles.get(j).hashCode())
					continue;
				q = particles.get(i).pos.dst(particles.get(j).pos);
				rij.set(particles.get(j).pos);
				rij.sub(particles.get(i).pos);
				rij.mul(1 / q);
				if (q < REST_LENGTH) {
					springs.add(new Spring(particles.get(i), particles.get(j), q));
				}
			}
			particles.get(i).velocity.set(0, 0);
		}
	}

	private void calculatePlasticity(float deltaTime) {
		len = springs.size();
		for (i=0; i<len; i++) {
			springs.get(i).update();
			if (springs.get(i).currentDistance == 0)
				continue;
			rij.set(springs.get(i).pj.pos);
			rij.sub(springs.get(i).pi.pos);
			rij.mul(1 / springs.get(i).currentDistance);
			// D = deltaTime2 * K_SPRING * (1 - (springs.get(i).restLength/REST_LENGTH)) *
			// (springs.get(i).restLength - springs.get(i).currentDistance);
			D = deltaTime * K_SPRING * (springs.get(i).restLength - springs.get(i).currentDistance);
			rij.mul(D * 0.5f);
			springs.get(i).pi.pos.set(springs.get(i).pi.pos.x - rij.x, springs.get(i).pi.pos.y - rij.y);
			springs.get(i).pj.pos.set(springs.get(i).pj.pos.x + rij.x, springs.get(i).pj.pos.y + rij.y);
		}
	}

	private void initializeViscoelasticity() {
		len = particles.size();
		for (i=0; i<len; i++) {
			springPresenceTable.put(particles.get(i).hashCode(), new ArrayList<Integer>(SIZE));
			particles.get(i).velocity.set(0, 0);
		}
	}

	/** NOTE: still in testing 
	 * tune YELD_RATIO_STRETCH, YELD_RATIO_COMPRESS and K_SPRING
	 * to test viscoelasticity
	 **/
	private void calculateViscoelasticity(float deltaTime) {
		deltaTime2 = (deltaTime * deltaTime);
		len = particles.size();
		for (i=0; i<len; i++) {
			if (particles.sizeNearby(particles.get(i)) <= 1)
				continue;
			tempParticles = particles.nearby(particles.get(i));
			len2 = tempParticles.size();
			if (len2 > MAX_NEIGHBORS)
				len2 = MAX_NEIGHBORS;
			for (j=0; j<len2; j++) {
				if (particles.get(i).hashCode() == particles.get(j).hashCode()
						|| particles.get(i).pos.dst2(particles.get(j).pos) > REST_LENGTH2)
					continue;
				if (!springPresenceTable.get(particles.get(i).hashCode()).contains(
						particles.get(j).hashCode())) {
					springs.add(new Spring(particles.get(i), particles.get(j), REST_LENGTH));
					springPresenceTable.get(particles.get(i).hashCode()).add(particles.get(j).hashCode());
				}
			}
		}

		for (springIter = springs.iterator(); springIter.hasNext();) {
			tempSpring = springIter.next();
			tempSpring.update();
			// Stretch
			if (tempSpring.currentDistance > (tempSpring.restLength + deformation)) {
				tempSpring.restLength += deltaTime
						* PLASTICITY
						* (tempSpring.currentDistance - tempSpring.restLength - (YELD_RATIO_STRETCH * tempSpring.restLength));
			}
			// Compress
			else if (tempSpring.currentDistance < (tempSpring.restLength - deformation)) {
				tempSpring.restLength -= deltaTime
						* PLASTICITY
						* (tempSpring.restLength
								- (YELD_RATIO_COMPRESS * tempSpring.restLength) - tempSpring.currentDistance);
			}
			// Remove springs with restLength longer than REST_LENGTH
			if (tempSpring.restLength > REST_LENGTH) {
				springIter.remove();
				springPresenceTable.get(tempSpring.pi.hashCode()).remove(
						(Integer) tempSpring.pj.hashCode());
			} else {
				if (tempSpring.currentDistance == 0)
					continue;
				rij.set(tempSpring.pj.pos);
				rij.sub(tempSpring.pi.pos);
				rij.mul(1 / tempSpring.currentDistance);
				//D = deltaTime2 * K_SPRING * (1 - (tempSpring.restLength/REST_LENGTH)) * (tempSpring.restLength - tempSpring.currentDistance);
				D = deltaTime * K_SPRING * (tempSpring.restLength - tempSpring.currentDistance);
				rij.mul(D * 0.5f);
				tempSpring.pi.pos.set(tempSpring.pi.pos.x - rij.x,
						tempSpring.pi.pos.y - rij.y);
				tempSpring.pj.pos.set(tempSpring.pj.pos.x + rij.x,
						tempSpring.pj.pos.y + rij.y);
			}
		}
	}

	private static void applyGravity(Particle pi) {
		pi.velocity.set(pi.velocity.x + gravityVect.x, pi.velocity.y
				+ gravityVect.y);
	}

	private void applyViscosity(float deltaTime) {
		len = particles.size();
		for (i=0; i<len; i++) {
			/*particles.get(i).density = 0;
			particles.get(i).nearDensity = 0;*/
			tempParticles = particles.nearby(particles.get(i));
			len2 = tempParticles.size();
			if (len2 > MAX_NEIGHBORS)
				len2 = MAX_NEIGHBORS;
			for (j=0; j<len2; j++) {
				q = particles.get(i).pos.dst2(tempParticles.get(j).pos);
				if ((q < H2) && (q != 0)) {
					q = (float) Math.sqrt(q);
					rij.set(tempParticles.get(j).pos);
					rij.sub(particles.get(i).pos);
					rij.mul(1 / q);
					q /= H;
					/*qq = ((1 - q) * (1 - q));
					particles.get(i).density += qq;
					particles.get(i).nearDensity += qq * (1 - q);*/

					tempVect.set(particles.get(i).velocity);
					tempVect.sub(tempParticles.get(j).velocity);
					u = tempVect.dot(rij);
					if (u <= 0.0f)
						continue;
					I = (deltaTime * (1 - q) * (SIGMA * u + BETA * u * u));
					rij.mul(I * 0.5f);
					tempVect.set(particles.get(i).velocity);
					tempVect.sub(rij);
					particles.get(i).velocity.set(tempVect);
					tempVect.set(tempParticles.get(j).velocity);
					tempVect.add(rij);
					tempParticles.get(j).velocity.set(tempVect);
				}
			}
		}
	}

	private void doubleDensityRelaxation(float deltaTime) {
		if (crazyMode)
			deltaTime2 = deltaTime;
		else
			deltaTime2 = (deltaTime * deltaTime);
		len = particles.size();
		for (i=0; i<len; i++) {
			particles.get(i).density = 0; 
			particles.get(i).nearDensity = 0; 
			tempParticles = particles.nearby(particles.get(i));
			len2 = tempParticles.size();
			if (len2 > MAX_NEIGHBORS)
				len2 = MAX_NEIGHBORS;
			for (j=0; j<len2; j++) {
				q = particles.get(i).pos.dst2(tempParticles.get(j).pos); 
				if (q < H2 && q != 0) { 
					q = (float)Math.sqrt(q); 
					q /= H; 
					qq = ((1 - q) * (1 - q)); 
					particles.get(i).density += qq;
					particles.get(i).nearDensity += qq * (1 - q); 
				}
			}
			
			particles.get(i).pressure = (K * (particles.get(i).density - P0));
			particles.get(i).nearPressure = (K_NEAR * particles.get(i).nearDensity);
			dx.set(0.0f, 0.0f);
			for (j=0; j<len2; j++) {
				q = particles.get(i).pos.dst2(tempParticles.get(j).pos);
				if ((q < H2) && (q != 0)) {
					q = (float) Math.sqrt(q);
					rij.set(tempParticles.get(j).pos);
					rij.sub(particles.get(i).pos);
					rij.mul(1 / q);
					q /= H;

					D = (deltaTime2 * (particles.get(i).pressure * (1 - q) + particles.get(i)
							.nearPressure * (1 - q) * (1 - q)));
					rij.mul(D * 0.5f);
					tempParticles.get(j).pos.set(tempParticles.get(j).pos.x + rij.x, tempParticles.get(j).pos.y + rij.y);
					dx.sub(rij);
				}
			}
			particles.get(i).pos.set(particles.get(i).pos.add(dx));
		}
	}

	private void attract(Particle pi) {
		if (isAttracting) {
			if (pi.pos.dst2(testPoint2D) > ATTRACT_RANGE)
				return;
			attract_vect.set(testPoint2D);
			attract_vect.sub(pi.pos);
			pi.velocity.set(pi.velocity.x
					+ (attract_vect.x * ATTRACT_FORCE), pi.velocity.y
					+ (attract_vect.y * ATTRACT_FORCE));
		}
	}

	private void repulse(Particle pi) {
		if (isRepulsing) {
			if (pi.pos.dst2(testPoint2D) > REPULSE_RANGE)
				return;
			repulse_vect.set(pi.pos);
			repulse_vect.sub(testPoint2D);
			pi.velocity.set(pi.velocity.x
					+ (repulse_vect.x * REPULSE_FORCE), pi.velocity.y
					+ (repulse_vect.y * REPULSE_FORCE));
		}
	}

	private void wallCollision(Particle pi) {
		tempVect.set(0, 0);

		if (pi.pos.x > (WORLD_WIDTH / 2 - wpadding))
			tempVect.sub((pi.pos.x - (WORLD_WIDTH / 2 - wpadding))
					/ collisionForce, 0);

		else if (pi.pos.x < (-WORLD_WIDTH / 2 + wpadding))
			tempVect.add(((-WORLD_WIDTH / 2 + wpadding) - pi.pos.x)
					/ collisionForce, 0);

		if (pi.pos.y > (WORLD_HEIGHT - hpadding))
			tempVect.sub(0, (pi.pos.y - (WORLD_HEIGHT - hpadding))
					/ collisionForce);

		else if (pi.pos.y < (INITIAL_HEIGHT + hpadding))
			tempVect.add(0, ((INITIAL_HEIGHT + hpadding) - pi.pos.y)
					/ collisionForce);

		pi.velocity.set(pi.velocity.x + tempVect.x, pi.velocity.y
				+ tempVect.y);
	}

	private void prepareDeleteOutOfBoundsParticles(Particle pi) {
		if ((pi.pos.x < -WORLD_WIDTH / 2)
				|| (pi.pos.x > WORLD_WIDTH / 2) || (pi.pos.y < 0)
				|| (pi.pos.y > WORLD_HEIGHT)) {
			disposableParticles.add(pi);
		}
	}

	private void prepareDeleteOutOfBoundsParticles() {
		len = disposableParticles.size();
		for (i=0; i<len; i++)
			particles.remove(disposableParticles.get(i));
	}

	private void deleteOutOfBoundsParticles() {
		disposableParticles.clear();
	}

	public boolean touchDown(int x, int y, int pointer, int button) {
		camera.unproject(testPoint.set(x, y, 0.0f));
		testPoint2D.x = testPoint.x;
		testPoint2D.y = testPoint.y;
		if (button == Buttons.LEFT) {
			if (!IS_DESKTOP) {
				isRepulsing = true;
				isAttracting = false;
			} else {
				isAttracting = false;
				isRepulsing = false;
				if (particles.size() < SIZE - 1) {
					if ((testPoint2D.x < (-WORLD_WIDTH / 2 + wpadding) || testPoint2D.x > (WORLD_WIDTH / 2 - wpadding))
							|| (testPoint2D.y < (INITIAL_HEIGHT + hpadding) || testPoint2D.y > (WORLD_HEIGHT - hpadding)))
						return false;
					particles.add(new Particle(testPoint2D));
					particles.get(particles.size() - 1).type = (emitType);
					if (viscoElasticityEnabled)
						springPresenceTable.put(
								particles.get(particles.size() - 1).hashCode(),
								new ArrayList<Integer>(SIZE));
				}
			}
		}
		if (button == Buttons.RIGHT) {
			isAttracting = true;
		}
		if (button == Buttons.MIDDLE) {
			isRepulsing = true;
		}
		return false;
	}

	public boolean touchDragged(int x, int y, int pointer) {
		camera.unproject(testPoint.set(x, y, 0.0f));
		testPoint2D.x = testPoint.x;
		testPoint2D.y = testPoint.y;
		if ((IS_DESKTOP) && (!isAttracting) && (!isRepulsing)
				&& (particles.size() < SIZE - 1)) {
			if ((testPoint2D.x < (-WORLD_WIDTH / 2 + wpadding) || testPoint2D.x > (WORLD_WIDTH / 2 - wpadding))
					|| (testPoint2D.y < (INITIAL_HEIGHT + hpadding) || testPoint2D.y > (WORLD_HEIGHT - hpadding)))
				return false;
			for (float i = -5; i < 5; i += 0.5f) {
				if (particles.size() >= SIZE - 1)
					break;
				particles.add(new Particle(testPoint2D.x + i, testPoint2D.y));
				tempParticle = particles.get(particles.size() - 1);
				tempParticle.type = (emitType);
				if (viscoElasticityEnabled)
					springPresenceTable.put(tempParticle.hashCode(),
							new ArrayList<Integer>(SIZE));
				tempParticle.velocity.set(tempParticle.velocity.x,
						tempParticle.velocity.y - EMITTER_FORCE * 5);
			}
		}

		return false;
	}

	public boolean touchUp(int x, int y, int pointer, int button) {
		if (!IS_DESKTOP) {
			isRepulsing = false;
			isAttracting = false;
		}
		if (button == Buttons.RIGHT) {
			isAttracting = false;
		}
		if (button == Buttons.MIDDLE) {
			isRepulsing = false;
		}
		return false;
	}

	private void logic(float deltaTime) {
		if (USE_FIXED_TIMESTEP) {
			checkTime2 += deltaTime;
			if (checkTime2 > TIMESTEP) {
				if (IS_DESKTOP) {
					try {
						if (slowMotion)
							performLogic(TIMESTEP / 4);
						else
							performLogic(TIMESTEP);
					} catch (NullPointerException npe) {
						particles.rehash();
					}
				}
				else
					performLogic(TIMESTEP);
				
				//checkTime2 -= TIMESTEP;
				checkTime2 = 0;
			}
		} else {
			try {
				if (slowMotion)
					performLogic(deltaTime / 4);
				else
					performLogic(deltaTime);
			} catch (NullPointerException npe) {
				particles.rehash();
			}
		}

		if (IS_DESKTOP) {
			checkTime += deltaTime;
			if (checkTime > 0) {
				prepareDeleteOutOfBoundsParticles();
				deleteOutOfBoundsParticles();
				checkTime = 0.0f;
			}
		}
	}

	public void render(float deltaTime) {
		logic(deltaTime);
		if (gl == null) {
			gl = Gdx.gl10;
			gl.glEnable(GL10.GL_LINE_SMOOTH);
			gl.glEnable(GL10.GL_BLEND);
			gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		}
		if (whiteBackground)
			gl.glClearColor(255, 255, 255, 255);
		else
			gl.glClearColor(0, 0, 0, 255);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		camera.update();
		//camera.apply(Gdx.gl10);
		
		// Begin Batch
		if (particleRendering) {
			if (!linesMode) {
				len = particles.size();
				batch.begin();
				for (i=0; i<len; i++) {
					dropCoordinate.set(particles.get(i).pos.x - dropRadius,
							particles.get(i).pos.y - dropRadius, 0.0f);
					camera.project(dropCoordinate);
					if (particles.get(i).type == 1)
						batch.draw(dropTexture, dropCoordinate.x,
								dropCoordinate.y, dropRadiusPixel,
								dropRadiusPixel);
					else
						batch.draw(dropTexture2, dropCoordinate.x,
								dropCoordinate.y, dropRadiusPixel,
								dropRadiusPixel);
				}
				batch.end();
				if (wireFrame && springs != null && springs.size() > 0) {
					vertexIndex = 0;
					len = springs.size();
					for (i=0; i<len; i++) {
						dropCoordinate.set(springs.get(i).pi.pos.x - dropRadius,
								springs.get(i).pi.pos.y - dropRadius, 0.0f);
						camera.project(dropCoordinate);
						tempVect3.set(springs.get(i).pj.pos.x - dropRadius,
								springs.get(i).pj.pos.y - dropRadius, 0.0f);
						camera.project(tempVect3);
						lineVertices[vertexIndex++] = dropCoordinate.x;
						lineVertices[vertexIndex++] = dropCoordinate.y;
						lineVertices[vertexIndex++] = Color.toFloatBits(255,
								255, 255, 255);
						lineVertices[vertexIndex++] = tempVect3.x;
						lineVertices[vertexIndex++] = tempVect3.y;
						lineVertices[vertexIndex++] = Color.toFloatBits(255,
								255, 255, 255);
					}
					lineMesh.setVertices(lineVertices, 0, vertexIndex);
					lineMesh.render(GL10.GL_TRIANGLE_STRIP);
				}
			} else {
				vertexIndex = 0;
				len = particles.size();
				for (i=0; i<len; i++) {
					if (smokeMode) {
						// Red Fire
						if (emitType == 1) {
							tempInt = 255 - (int) ((System.currentTimeMillis() - particles.get(i)
									.spawnTime) / 50);
							// Start by decreasing B value to 0
							particles.get(i).setBGrad(240 - (int) ((System
									.currentTimeMillis() - particles.get(i).spawnTime) / (3 * 1)));
							// Then decrease G value to 0
							if (particles.get(i).bGrad < 150)
								particles.get(i).setGGrad(255 - (int) ((System
										.currentTimeMillis() - particles.get(i)
										.spawnTime) / (10 * 1)));
							// Then decrease R value to 0
							if (particles.get(i).gGrad < 150)
								particles.get(i).setRGrad(255 - (int) ((System
										.currentTimeMillis() - particles.get(i)
										.spawnTime) / (25 * 1)));
							if (tempInt <= 0 || particles.get(i).rGrad == 0) {
								disposableParticles.add(particles.get(i));
								continue;
							}
						}
						// Blue Fire
						else if (emitType == 2) {
							tempInt = 255 - (int) ((System.currentTimeMillis() - particles.get(i)
									.spawnTime) / 50);
							// Start by decreasing R value to 0
							particles.get(i).setRGrad(240 - (int) ((System
									.currentTimeMillis() - particles.get(i).spawnTime) / (3 * 1)));
							// Then decrease G value to 0
							if (particles.get(i).rGrad < 150)
								particles.get(i).setGGrad(255 - (int) ((System
										.currentTimeMillis() - particles.get(i)
										.spawnTime) / (10 * 1)));
							// Then decrease B value to 0
							if (particles.get(i).gGrad < 150)
								particles.get(i).setBGrad(255 - (int) ((System
										.currentTimeMillis() - particles.get(i)
										.spawnTime) / (25 * 1)));
							if (tempInt <= 0 || particles.get(i).bGrad == 0) {
								disposableParticles.add(particles.get(i));
								continue;
							}
						}
						// Green Fire
						else if (emitType == 3) {
							tempInt = 255 - (int) ((System.currentTimeMillis() - particles.get(i)
									.spawnTime) / 50);
							// Start by decreasing R and B values to 0
							particles.get(i).setRGrad(240 - (int) ((System
									.currentTimeMillis() - particles.get(i).spawnTime) / (10 * 1)));
							particles.get(i).setBGrad(240 - (int) ((System
									.currentTimeMillis() - particles.get(i).spawnTime) / (10 * 1)));
							// Then decrease G value to 0
							if (particles.get(i).rGrad < 150)
								particles.get(i).setGGrad(255 - (int) ((System
										.currentTimeMillis() - particles.get(i)
										.spawnTime) / (25 * 1)));
							if (tempInt <= 0 || particles.get(i).gGrad == 0) {
								disposableParticles.add(particles.get(i));
								continue;
							}
						}
					}
					dropCoordinate.set(particles.get(i).pos.x - dropRadius,
							particles.get(i).pos.y - dropRadius, 0.0f);
					camera.project(dropCoordinate);
					lineVertices[vertexIndex++] = dropCoordinate.x;
					lineVertices[vertexIndex++] = dropCoordinate.y;
					if (smokeMode)
						lineVertices[vertexIndex++] = Color.toFloatBits(
								particles.get(i).rGrad, particles.get(i).gGrad, particles.get(i).bGrad,
								tempInt);
					else {
						if (emitType == 1)
							lineVertices[vertexIndex++] = Color.toFloatBits(
									255, (int) particles.get(i).density
											* LINE_DENSITY_FACTOR, 0, 255);
						else if (emitType == 2)
							lineVertices[vertexIndex++] = Color
									.toFloatBits((int) particles.get(i).density
											* LINE_DENSITY_FACTOR, 0, 255, 255);
						else if (emitType == 3)
							lineVertices[vertexIndex++] = Color.toFloatBits(0,
									200, (int) particles.get(i).density
											* LINE_DENSITY_FACTOR, 255);
					}
					lineVertices[vertexIndex++] = dropCoordinate.x
							+ particles.get(i).velocity.x * LINE_VELOCITY_FACTOR;
					lineVertices[vertexIndex++] = dropCoordinate.y
							+ particles.get(i).velocity.y * LINE_VELOCITY_FACTOR;
					if (smokeMode)
						lineVertices[vertexIndex++] = Color.toFloatBits(
								particles.get(i).rGrad, particles.get(i).gGrad, particles.get(i).bGrad,
								tempInt);
					else {
						if (emitType == 1)
							lineVertices[vertexIndex++] = Color.toFloatBits(
									255, (int) particles.get(i).density
											* LINE_DENSITY_FACTOR, 0, 255);
						else if (emitType == 2)
							lineVertices[vertexIndex++] = Color
									.toFloatBits((int) particles.get(i).density
											* LINE_DENSITY_FACTOR, 0, 255, 255);
						else if (emitType == 3)
							lineVertices[vertexIndex++] = Color.toFloatBits(0,
									200, (int) particles.get(i).density
											* LINE_DENSITY_FACTOR, 255);
					}
				}
				lineMesh.setVertices(lineVertices, 0, vertexIndex);
				lineMesh.render(GL10.GL_LINES);
			}
		}

		if (hudEnabled) {
			batch.begin();
			font.draw(batch,
					"FLUID SIMULATION fps:" + Gdx.graphics.getFramesPerSecond()
							+ ", particles: " + particles.size(), 0.0f, 20.0f);
				if (IS_DESKTOP) {
					font.draw(batch, "deltaTime: " + deltaTime, 0.0f, 40.0f);
					font.draw(batch, "(SPACE) Fluid Type: " + emitType
							+ "    (+/-) Gravity: " + GRAVITY_FORCE
							+ "    (UP/DOWN) Emitter: " + EMITTER_FORCE
							+ "    (E) Expand: " + expandMode + "    (S) Slow: "
							+ slowMotion + "    (C) Crazy: " + crazyMode
							+ "    (L) Lines: " + linesMode + "    (W) Wires: "
							+ wireFrame, 330.0f, 60.0f);
					font.draw(batch, "(K) Smoke: " + smokeMode + "    (P) Plasticity: "
							+ plasticityEnabled + "    (V) ViscoElasticity: "
							+ viscoElasticityEnabled + "    (R) Particle Render: "
							+ particleRendering, 330.0f, 40.0f);
					font.draw(
							batch,
							"K: " + K + "    Sigma: " + SIGMA
									+ "    Density: " + P0 + "    H: " + H
									+ "    Cell: " + CELL_SIZE + "    K_: " + K_
									+ "    Rad: "
									+ dropRadiusK + "    K_SRING: "
									+ K_SPRING, 330.0f, 20.0f);
					font.draw(batch, "(" + (int) testPoint.x + "," + (int) testPoint.y
							+ ")", 10.0f, Gdx.graphics.getHeight());
			}
			batch.end();
		}

		// Exit the application if ESC has been pressed
		if (exitApp)
			Gdx.app.exit();
	}

	public boolean keyUp(int keycode) {
		// Switch to Game Screen
		/*
		 * if (keycode == Input.Keys.ENTER) game.setScreen(game.switchToGame());
		 */

		// K
		if (keycode == Input.Keys.F1 && K < 0.5f) {
			K += 0.01f;
			K_NEAR = K * K_;
		} else if (keycode == Input.Keys.NUM_1 && K > 0.004f) {
			K -= 0.01f;
			if (K < 0.004f)
				K = 0.004f;
			K_NEAR = K * K_;
		}

		// SIGMA
		else if (keycode == Input.Keys.F2 && SIGMA < 1.0f)
			SIGMA += 0.1f;
		else if (keycode == Input.Keys.NUM_2 && SIGMA > 0.0f)
			SIGMA -= 0.1f;

		// DENSITY
		else if (keycode == Input.Keys.F3 && P0 < 1000.0f)
			P0 += 10.0f;
		else if (keycode == Input.Keys.NUM_3 && P0 > 10.0f)
			P0 -= 10.0f;

		// H
		else if (keycode == Input.Keys.F4 && H < 50.0f) {
			H += 5.0f;
			H2 = H * H;
		} else if (keycode == Input.Keys.NUM_4 && H > 5.0f) {
			H -= 5.0f;
			H2 = H * H;
		}

		// CELL_SIZE
		else if (keycode == Input.Keys.F5 && CELL_SIZE < 50) {
			CELL_SIZE += 1;
			particles.rehash();
		} else if (keycode == Input.Keys.NUM_5 && CELL_SIZE > 1) {
			CELL_SIZE -= 1;
			particles.rehash();
		}

		// K_
		if (keycode == Input.Keys.F6 && K_ < 50.0f) {
			K_ += 1;
			K_NEAR = K * K_;
		} else if (keycode == Input.Keys.NUM_6 && K_ > 1.0f) {
			K_ -= 1;
			K_NEAR = K * K_;
		}

		// dropRadiusK
		if (keycode == Input.Keys.F8 && dropRadiusK < 1.5f) {
			dropRadiusK += 0.1f;
			dropRadius = 0.1f + dropRadiusK;
			dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
			dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
		} else if (keycode == Input.Keys.NUM_8 && dropRadiusK >= 0.1f) {
			dropRadiusK -= 0.1f;
			if (dropRadiusK < 0.1f)
				dropRadiusK = 0;
			dropRadius = 0.1f + dropRadiusK;
			dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
			dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
		}

		// K_SPRING
		if (keycode == Input.Keys.F9 && K_SPRING < 5.0f)
			K_SPRING += 0.1f;
		else if (keycode == Input.Keys.NUM_9 && K_SPRING > 0.2f)
			K_SPRING -= 0.1f;

		if (keycode == Input.Keys.BACKSPACE && isAttracting) {
			for (Particle pi : particles) {
				if (pi.pos.dst2(testPoint2D) < ATTRACT_RANGE) {
					disposableParticles.add(pi);
				}
			}
		} else if (keycode == Input.Keys.BACKSPACE)
			game.setScreen(game.switchToFluidSimulator());

		// Change Particle color
		if (keycode == Input.Keys.SPACE) {
			emitType += 1;
			if (emitType > 3) {
				emitType = 1;
			}
		}

		// Increase/Decrease Gravity
		if ((keycode == Input.Keys.PLUS) && (GRAVITY_FORCE > -1.0f)) {
			GRAVITY_FORCE -= 0.1f;
			gravityVect.set(0.0f, GRAVITY_FORCE);
		} else if ((keycode == Input.Keys.MINUS) && (GRAVITY_FORCE < 0.0f)) {
			GRAVITY_FORCE += 0.1f;
			if (GRAVITY_FORCE > -0.1f)
				GRAVITY_FORCE = 0.0f;
			gravityVect.set(0.0f, GRAVITY_FORCE);
		}

		// Increase/Decrease Emitter Force
		if ((keycode == Input.Keys.DOWN) && (EMITTER_FORCE > -100)) {
			EMITTER_FORCE -= 10;
		} else if ((keycode == Input.Keys.UP) && (EMITTER_FORCE < 100)) {
			EMITTER_FORCE += 10;
		}

		// Enable/Disable Expand Mode
		if (keycode == Input.Keys.E)
			expandMode = !expandMode;

		// Enable/Disable Stop Motion
		if (keycode == Input.Keys.S)
			slowMotion = !slowMotion;

		// Enable/Disable Crazy Mode
		if (keycode == Input.Keys.C)
			crazyMode = !crazyMode;

		// Enable/Disable HUD
		if (keycode == Input.Keys.H)
			hudEnabled = !hudEnabled;

		// Enable/Disable Plasticity
		if (keycode == Input.Keys.P && !viscoElasticityEnabled && IS_DESKTOP) {
			plasticityEnabled = !plasticityEnabled;
			if (plasticityEnabled) {
				initializePlasticity = true;
				if (springs == null && springPresenceTable == null) {
					springs = new ArrayList<Spring>(SIZE * 230);
					springPresenceTable = new FastMap<Integer, ArrayList<Integer>>(
							SIZE);
				}
			} else {
				springs.clear();
				springPresenceTable.clear();
			}
		}

		// Enable/Disable ViscoElasticity
		if (keycode == Input.Keys.V && !plasticityEnabled && IS_DESKTOP) {
			viscoElasticityEnabled = !viscoElasticityEnabled;
			if (viscoElasticityEnabled) {
				initializeViscoelasticity = true;
				if (springs == null && springPresenceTable == null) {
					springs = new ArrayList<Spring>(SIZE * 230);
					springPresenceTable = new FastMap<Integer, ArrayList<Integer>>(
							SIZE);
				}
				springs.clear();
				springPresenceTable.clear();
			} else {
				springs.clear();
				springPresenceTable.clear();
				System.gc();
			}
		}

		// Enable/Disable WireFrame mode
		if (keycode == Input.Keys.W) {
			if (!linesMode) {
				wireFrame = !wireFrame;
			}
		}

		// Enable/Disable Lines mode
		if (keycode == Input.Keys.L) {
			linesMode = !linesMode;
		}

		// Enable/Disable Smoke Mode
		if (keycode == Input.Keys.K)
			smokeMode = !smokeMode;

		// Enable/Disable Particle Rendering
		if (keycode == Input.Keys.R)
			particleRendering = !particleRendering;

		// Enable/Disable White Background
		if (keycode == Input.Keys.X)
			whiteBackground = !whiteBackground;

		// Exit
		if (keycode == Input.Keys.ESCAPE) {
			exitApp = true;
		}

		return false;
	}

	public void dispose() {
		if (IS_DESKTOP)
			disposableParticles.clear();
		particles.clear();
		if (springs != null)
			springs.clear();
		if (springPresenceTable != null)
			springPresenceTable.clear();
		lineVertices = null;
		lineMesh.dispose();
		if (IS_DESKTOP) {
			dropTexture.dispose();
			dropTexture2.dispose();
		}
		lineMesh = null;
	}

	@Override
	public boolean keyDown(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchMoved(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resize(int arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}
}