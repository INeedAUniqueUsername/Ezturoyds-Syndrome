package game;

import static game.GameWindow.SCREEN_HEIGHT;
import static game.GameWindow.SCREEN_WIDTH;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import body.Body_Starship;
import capture.GifSequenceWriter;
import display.ScreenDamage;
import factories.StarshipFactory;
import helpers.SpaceHelper;
import interfaces.GameObject;
import space.BackgroundStar;
import space.Effect;
import space.Level;
import space.Level_Waves;
import space.Projectile;
import space.RisingText;
import space.Seeker;
import space.SpaceObject;
import space.Starship;
import space.Starship_Player;
import space.Weapon;

public class GamePanel extends JPanel implements ActionListener, MouseListener, KeyListener {
	private boolean active = false;
	private boolean cheat_playerActive = true;
	private boolean strafeMode = false;

	public enum CameraMode {
		FIXED, FOLLOW_PLAYER
	}
	
	public enum VerticalDirection {
		UP, DOWN, NONE;
	}
	public enum HorizontalDirection {
		RIGHT, LEFT, NONE;
	}
	
	public VerticalDirection verticalScroll = VerticalDirection.NONE;
	public HorizontalDirection horizontalScroll = HorizontalDirection.NONE;

	private static final CameraMode camera = CameraMode.FOLLOW_PLAYER;
	public static final double epsilon = .000000000000000000000000000000000000001;
	public static final double LIGHT_SPEED = 90;
	private int cameraOffset_x, cameraOffset_y;
	//final int INTERVAL = 1;
	private SpaceObject pov;
	private Starship_Player player;
	// private Starship_NPC enemy_test;
	private ArrayList<SpaceObject> universe;
	private ArrayList<SpaceObject> objectsCreated;
	private ArrayList<SpaceObject> objectsDestroyed;
	
	private ArrayList<Effect> effects;
	private ArrayList<BackgroundStar> background;
	private Level currentLevel;

	private long score = 0;
	int hits = 0;

	// ScreenCracking screenEffect = new
	// ScreenCracking(GameWindow.SCREEN_CENTER_X, GameWindow.SCREEN_CENTER_Y,
	// 10);
	ScreenDamage screenEffect = new ScreenDamage(new Point(GameWindow.SCREEN_CENTER_X, GameWindow.SCREEN_CENTER_Y));
	/*
	 * ArrayList<Starship> starships; ArrayList<Projectile> projectiles;
	 * ArrayList<Asteroid> asteroids;
	 */
	private ArrayList<String> debugPrint = new ArrayList<String>(0);
	private ArrayList<Consumer<Graphics>> debugDraw = new ArrayList<Consumer<Graphics>>(0);

	// counter for hits
	// private int hits = 0;

	private long tick;
	private boolean gameover = false;
	private boolean scrollBackToPlayer = true;
	private static GamePanel world;
	private BufferedImage lastFrame;
	
	public RecordingThread recording;
	
	volatile int recordingCount = 0;
	
	class RecordingThread extends Thread {
		GifSequenceWriter capture;
		List<RenderedImage> framesLeft;
		public RecordingThread() {
			try {
				recordingCount++;
				ImageOutputStream output = new FileImageOutputStream(new File("Demo" + recordingCount + ".gif"));
				capture = new GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, 1, true);
				framesLeft = Collections.synchronizedList(new LinkedList<RenderedImage>());
				start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public synchronized void addFrame(RenderedImage frame) {
			framesLeft.add(frame);
		}
		int secondsSinceLastFrame = 0;
		public void run() {
			while(secondsSinceLastFrame < 6) {
				if(framesLeft.size() > 0) {
					if(framesLeft.size()%10 == 0) { 
						System.out.println("Writing Frame; " + framesLeft.size() + " left.");
					}
					//printToScreen("Writing Frame");
					try {
						capture.writeToSequence(framesLeft.remove(0));
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					try {
						this.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					secondsSinceLastFrame++;
				}
			}
			System.out.println("Recording Done");
		}
	}
	public GamePanel() {
		//Timer ticker = new Timer(INTERVAL, this);
		//ticker.start();
		
		addKeyListener(this);
		addMouseListener(this);
		world = this;
		clear();
	}
	
	public void clear() {
		score = 0;
		hits = 0;
		setTick(0);
		universe = new ArrayList<>(0);
		objectsCreated = new ArrayList<>(0);
		objectsDestroyed = new ArrayList<>(0);
		effects = new ArrayList<>(0);
		background = new ArrayList<>(0);
	}

	public static GamePanel getWorld() {
		return world;
	}

	public static CameraMode getCameraMode() {
		return camera;
	}

	public void newGame() {
		clear();
		active = true;
		for (int i = 0; i < GameWindow.GAME_WIDTH * GameWindow.GAME_HEIGHT / 40000; i++) {
			background.add(new BackgroundStar(GameWindow.randomGameWidth(), GameWindow.randomGameHeight(),
					SpaceHelper.random(360), 5));
		}

		BufferedImage backgroundImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics backgroundG = backgroundImage.getGraphics();
		backgroundG.setColor(Color.BLACK);
		backgroundG.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
		for (BackgroundStar o : background) {
			o.draw(backgroundG);
		}
		//GameWindow.writeImage(backgroundImage, "Background");
		
		// starships = new ArrayList<Starship>();
		// projectiles = new ArrayList<Projectile>();
		// asteroids = new ArrayList<Asteroid>();

		player = StarshipFactory.createPlayership();
		player.setPosRectangular(GameWindow.GAME_WIDTH / 2, GameWindow.GAME_HEIGHT / 2);
		pov = player;

		universeAdd(player);

		/*
		 * ∂ enemy_test = new Starship_NPC(); addStarship(enemy_test);
		 * enemy_test.setPosRectangular(400, 225); enemy_test.setName("Enemy");
		 * addWeapon(enemy_test, new Weapon(0, 10, 0, 5, 15, 1, 90, Color.RED));
		 * enemy_test.addOrder(new Order_Escort(enemy_test, player));
		 */
		currentLevel = new Level_Waves();
		currentLevel.start();
		lastFrame = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics g = lastFrame.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
		
		setBackground(Color.BLACK);
	}
	public void paintComponent(Graphics g) {
		// g.clearRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
		/*
		 * Starship player = ships.get(0); Asteroid rock = asteroids.get(0);
		 * double angle = player.getAngleTowards(rock); int playerY = (int)
		 * player.getPosY(); int x = (int) player.getPosX(); int y = (int)
		 * (GameWindow.HEIGHT - playerY); int x2 = (int) (x + 50 *
		 * player.cosDegrees(angle)); int y2 = (int) (GameWindow.HEIGHT -
		 * (playerY + 50 * player.sinDegrees(angle)));
		 * 
		 * g.setColor(Color.WHITE); g.drawLine(x, y, x2, y2);
		 */
		super.paintComponent(g);
		repaint();
		
		if (active) {
			tick++;
			updateUniverse();
			updateDraw(lastFrame.createGraphics());
			g.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
			g.drawImage(lastFrame, 0, 0, this);
			if(recording != null) {
				//System.out.println("Recording");
				printToScreen("Recording");
				
				BufferedImage copyFrame = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
				copyFrame.getGraphics().drawImage(lastFrame, 0, 0, null);
				
				recording.addFrame(copyFrame);
				
			}
			/*
			 * BufferedImage b = new BufferedImage(GameWindow.SCREEN_WIDTH,
			 * GameWindow.SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
			 * updateDraw(b.getGraphics()); video.add(b);
			 */
			 
		} else {
			//updateDraw(lastFrame.createGraphics());
			g.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
			g.drawImage(lastFrame, 0, 0, this);
			
			printToScreen("Paused");
			drawDebug((Graphics2D) g);
		}
	}

	public void updateUniverse() {
		//Score is meaningless
		if(player.getActive()) {
			score += tick / 30;
		}
		// Update everything
		currentLevel.update();

		for (BackgroundStar b : background) {
			// b.update();
		}
		for (int i1 = 0; i1 < universe.size(); i1++) {
			SpaceObject o1 = universe.get(i1);

			o1.update();
			// Update all weapons
			if (o1 instanceof Starship) {
				for (Weapon w : ((Starship) o1).getWeapon()) {
					w.update();
					if (w.getFiring() && (w.getFireCooldownLeft() > w.getFireCooldownTime())) {
						//print("--> " + (w.getOwner() == player ? "Human" : "Computer") + " Shot First");
						Projectile shot = w.createShot();
						createSpaceObject(shot);
						w.setFireCooldownLeft(0);
						//print("<--" + (w.getOwner() == player ? "Human" : "Computer") + " Shot First");
					}
				}
			}
			boolean isProjectile = o1 instanceof Projectile;
			boolean isStarship = o1 instanceof Starship;
			for (int i2 = i1 + 1; i2 < universe.size(); i2++) {
				SpaceObject o2 = universe.get(i2);
				if(isProjectile && o2 instanceof Projectile) {
					continue;
				}
				Area intersection = getIntersection(o1, o2);
				if (!intersection.isEmpty()) {
					if(isStarship) {
						if(o2 instanceof Starship) {
							collisionStarshipStarship((Starship) o1, (Starship) o2, intersection);
						} else if(o2 instanceof Projectile) {
							Starship s = (Starship) o1;
							Projectile p = (Projectile) o2;
							if (!p.getOwner().equals(s)) {
								collisionStarshipProjectile(s, p, intersection);
							}
						}
					} else if (isProjectile && o2 instanceof Starship) {
						Starship s = (Starship) o2;
						Projectile p = (Projectile) o1;
						if (!p.getOwner().equals(s)) {
							collisionStarshipProjectile(s, p, intersection);
						}
					}
					/*
					 * else if (o1 instanceof Projectile && o2 instanceof
					 * Projectile) { collisionProjectileProjectile((Projectile)
					 * o1, (Projectile) o2, intersection); }
					 */
				}
			}
		}
		universe.forEach(o -> {
			if(!o.getActive()) {
				objectsDestroyed.add(o);
			}
		});

		// Add objects to be created and remove objects to be destroyed
		for (SpaceObject o : objectsCreated) {
			universeAdd(o);
		}
		objectsCreated.clear();
		for (SpaceObject d : objectsDestroyed) {
			universeRemove(d);
		}
		objectsDestroyed.clear();
	}

	public void updateDraw(Graphics g) {
		int scrollRate = 50;
		switch(verticalScroll) {
		case UP:
			cameraOffset_y += scrollRate;
			break;
		case DOWN:
			cameraOffset_y -= scrollRate;
			break;
		}
		switch(horizontalScroll) {
		case RIGHT:
			cameraOffset_x += scrollRate;
			break;
		case LEFT:
			cameraOffset_x -= scrollRate;
			break;
		}
		if(scrollBackToPlayer) {
			cameraOffset_x *= 0.95;
			cameraOffset_y *= 0.88;
		}
		int lowerLimit = 25;
		//g.setColor(new Color(0, 0, 0, Math.min(255, 75 + (int) (180 * Math.pow(((double) Math.max(player.getStructure() - lowerLimit, 0) / (player.getStructureMax())), 1.5)))));
		g.setColor(new Color(0, 0, 0, Math.min(255, 105 + (int) (150 * Math.pow(((double) Math.max(player.getStructure() - lowerLimit, 0) / (player.getStructureMax())), 1.5)))));
		g.fillRect(0, 0, GameWindow.SCREEN_WIDTH, GameWindow.SCREEN_HEIGHT);

		Graphics2D g2D = ((Graphics2D) g);
		// g2D.rotate(-Math.toRadians(pos_r_player));
		double pos_x_pov = pov.getPosX();
		double pos_y_pov = pov.getPosY();
		double pos_r_pov = pov.getPosR();

		double translateX = GameWindow.SCREEN_CENTER_X - (pos_x_pov + cameraOffset_x);
		double translateY = GameWindow.SCREEN_CENTER_Y + (pos_y_pov + cameraOffset_y);

		g2D.translate(translateX, translateY);
		drawUniverse(g2D);
		g2D.translate(-translateX, -translateY);

		g2D.translate(-cameraOffset_x, cameraOffset_y);
		screenEffect.draw(g2D);
		g2D.translate(cameraOffset_x, -cameraOffset_y);

		// Print all current debug messages on screen. Debug list will only
		// clear when the game is active.
		g2D.setColor(Color.WHITE);
		g2D.setFont(new Font("Consolas", Font.PLAIN, 18));
		final int line_height = 18;
		int print_y = line_height;

		if(player.getActive()) {
			g2D.drawString("Score: " + score, 10, print_y);

			//g2D.drawString("Structure: " + player.getStructure(), 10, print_y);
			g2D.setColor(Color.GREEN);
			g2D.fillRect(10, print_y + 2, (128 * player.getStructure()) / player.getStructureMax(), line_height - 2);
			print_y += line_height * 3;
			g2D.setColor(Color.WHITE);
			g2D.drawString("Coooldown", 10, print_y);
			for(Weapon w : player.getWeapon()) {
				/*
				g2D.setColor(Color.RED);
				g2D.fillRect(10, print_y + 2, (int) (128 * Math.min(1, 1.0 * w.getFireCooldownLeft() / w.getFireCooldownTime())), line_height - 2);
				*/
				w.drawHUD(g2D, new Rectangle(10, print_y+2, 128, line_height - 2));
				print_y += line_height;
			}
		} else {
			if(!gameover) {
				gameover = true;
				tick = 0;
				return;
			}
			//System.out.println("Dead: " + tick);
			if (tick % 90 > 45) {
				int y = (GameWindow.SCREEN_HEIGHT / 2);
				drawStringCentered(g, "Final Score: " + score, 48, Color.red, y);
				drawStringCentered(g, "Press Backspace to restart", 24, Color.red, y + 24);
				drawStringCentered(g, "Press ESC for Title Screen", 24, Color.red, y + 48);
			}
		}
		
		drawDebug(g2D);
		debugPrint.clear();
	}
	public void drawDebug(Graphics2D g2D) {
		final int line_height = 18;
		int print_y = line_height * 4;
		for (String s : debugPrint) {
			g2D.drawString(s, 10, print_y);
			print_y += line_height;
		}
	}

	public void drawUniverse(Graphics2D g2D) {
		g2D.scale(1, -1);
		for (Consumer<Graphics> c : debugDraw) {
			c.accept(g2D);
		}
		debugDraw.clear();
		drawObjects(g2D);
		g2D.scale(1, -1);
		boolean right = pov.getPosX() + GameWindow.SCREEN_WIDTH/2 > GameWindow.GAME_WIDTH;
		boolean left = pov.getPosX() - GameWindow.SCREEN_WIDTH/2 < 0;
		boolean up = pov.getPosY() + GameWindow.SCREEN_HEIGHT/2 > GameWindow.GAME_HEIGHT;
		boolean down = pov.getPosY() - GameWindow.SCREEN_HEIGHT/2 < 0;
		if(right) {
			drawWrap(g2D, GameWindow.GAME_WIDTH, 0);
			
			if(up) {
				drawWrap(g2D, GameWindow.GAME_WIDTH, GameWindow.GAME_HEIGHT);
			}
			if(down) {
				drawWrap(g2D, GameWindow.GAME_WIDTH, -GameWindow.GAME_HEIGHT);
			}
		}
		if(left) {
			drawWrap(g2D, -GameWindow.GAME_WIDTH, 0);
			
			if(up) {
				drawWrap(g2D, -GameWindow.GAME_WIDTH, GameWindow.GAME_HEIGHT);
			}
			if(down) {
				drawWrap(g2D, -GameWindow.GAME_WIDTH, -GameWindow.GAME_HEIGHT);
			}
		}
		if(up) {
			drawWrap(g2D, 0, GameWindow.GAME_HEIGHT);
		}
		if(down) {
			drawWrap(g2D, 0, -GameWindow.GAME_HEIGHT);
		}
	}
	public void drawWrap(Graphics2D g2D, int translateX, int translateY) {
		
		g2D.scale(1, -1);
		g2D.translate(translateX, translateY);
		drawObjects(g2D);
		g2D.translate(-translateX, -translateY);
		g2D.scale(1, -1);
	}
	public void drawObjects(Graphics2D g2D) {
		for (BackgroundStar b : background) {
			b.draw(g2D);
		}
		effects.removeIf(e -> {
			e.update();
			e.draw(g2D);
			return !e.getActive();
		});
		for (SpaceObject o : universe) {
			o.draw(g2D);

			if (o instanceof Starship) {
				for (Weapon w : ((Starship) o).getWeapon()) {
					w.draw(g2D);
				}
			}
		}
	}
	public synchronized void printToScreen(String text) {
		debugPrint.add(text);
	}

	public synchronized void drawToScreen(Consumer<Graphics> c) {
		debugDraw.add(c);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		setMouseState(e.getButton(), true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		setMouseState(e.getButton(), false);
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void setMouseState(int code, boolean state) {
		switch(code) {
		case MouseEvent.BUTTON1:
			player.setFiringMouse(state);
			break;
		/*
		case MouseEvent.BUTTON3:
			SpaceHelper.random(getStarships()).damage(10);
			break;
		*/
		}
	}
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		print("Key Typed");
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		setKeyState(e.getKeyCode(), true);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		setKeyState(e.getKeyCode(), false);
	}

	public void setKeyState(int code, boolean state) {
		switch (code) {
		case KeyEvent.VK_UP:
			player.setThrusting(state);
			break;
		case KeyEvent.VK_DOWN:
			player.setBraking(state);
			break;
		case KeyEvent.VK_LEFT:
			if (strafeMode) {
				player.setStrafingLeft(state);
			} else {
				player.setTurningCCW(state);
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (strafeMode) {
				player.setStrafingRight(state);
			} else {
				player.setTurningCW(state);
			}
			break;
		case KeyEvent.VK_W:
			if (state){// && cameraOffset_y < GameWindow.SCREEN_CENTER_Y-200)
				//cameraOffset_y += 50 * 5;
				verticalScroll = VerticalDirection.UP;
			} else {
				if(verticalScroll == VerticalDirection.UP) {
					verticalScroll = VerticalDirection.NONE;
				}
			}
			break;
		case KeyEvent.VK_S:
			if (state){// && cameraOffset_y > 200-GameWindow.SCREEN_CENTER_Y)
				//cameraOffset_y -= 50 * 5;
				verticalScroll = VerticalDirection.DOWN;
			} else {
				if(verticalScroll == VerticalDirection.DOWN) {
					verticalScroll = VerticalDirection.NONE;
				}
			}
			break;
		case KeyEvent.VK_A:
			if (state){// && cameraOffset_x > 200-GameWindow.SCREEN_CENTER_X)
				//cameraOffset_x -= 50 * 5;
				horizontalScroll = HorizontalDirection.LEFT;
			} else {
				if(horizontalScroll == HorizontalDirection.LEFT) {
					horizontalScroll = HorizontalDirection.NONE;
				}
			}
			break;
		case KeyEvent.VK_D:
			if (state){// && cameraOffset_x < GameWindow.SCREEN_CENTER_X-200) {
				//cameraOffset_x += 50 * 5;
				horizontalScroll = HorizontalDirection.RIGHT;
			}
			else {
				if(horizontalScroll == HorizontalDirection.RIGHT) {
					horizontalScroll = HorizontalDirection.NONE;
				}
			}
			break;
		case KeyEvent.VK_F:
			if(state) {
				scrollBackToPlayer = !scrollBackToPlayer;
			}
			
			break;
		case KeyEvent.VK_SHIFT:
			strafeMode = state;
			break;
		case KeyEvent.VK_X:
			player.setFiringKey(state);
			break;
		case KeyEvent.VK_ESCAPE:
			System.exit(0);
			break;
		case KeyEvent.VK_BACK_SPACE:
			newGame();
			break;
		}
	}

	public double arctanDegrees(double y, double x) {
		double result;
		if (x < 0) {
			result = Math.toDegrees(Math.atan(y / x)) + 180;
		} else if (x == 0) {
			if (y < 0) {
				result = 270;
			} else if (y == 0) {
				result = 0;
			} else // ySpeed > 0
			{
				result = 90;
			}
		} else if (x > 0) {
			result = Math.toDegrees(Math.atan(y / x));
		} else {
			result = 0;
		}
		print("X: " + y);
		print("Y: " + x);
		print("R: " + result);
		return result;
	}

	public static boolean intersects(Shape shapeA, Shape shapeB) {
		//Check if bounds intersect
		if(!shapeA.getBounds2D().intersects(shapeB.getBounds2D())) {
			return false;
		}
		Area areaA = new Area(shapeA);
		areaA.intersect(new Area(shapeB));
		return !areaA.isEmpty();
	}

	public static Area getIntersection(SpaceObject a, SpaceObject b) {
		Area areaA = new Area();
		for (Polygon part : a.getBody().getShapes()) {
			areaA.add(new Area(part));
		}

		Area areaB = new Area();
		for (Polygon part : b.getBody().getShapes()) {
			areaB.add(new Area(part));
		}
		
		//Optimize
		if(!areaA.getBounds2D().intersects(areaB.getBounds2D())) {
			areaA.reset();
		} else {
			areaA.intersect(areaB);
		}
		return areaA;
	}

	// Use these when the universe is in the middle of updating
	public void createSpaceObject(SpaceObject so) {
		objectsCreated.add(so);
	}

	public void destroySpaceObject(SpaceObject so) {
		objectsDestroyed.add(so);
	}

	/*
	 * public void addWeapon(Starship ship, Weapon item) {
	 * ship.installWeapon(item); }
	 * 
	 * public void addStarship(Starship ship) { //starships.add(ship);
	 * universe.add(ship); }
	 * 
	 * public void addProjectile(Projectile projectile) {
	 * //projectiles.add(projectile); universe.add(projectile); }
	 */
	/*
	 * public void addAsteroid(Asteroid asteroid) { //asteroids.add(asteroid);
	 * universe.add(asteroid); }
	 */
	// Warning: Do not use during universe iteration
	private void universeAdd(SpaceObject so) {
		universe.add(so);
	}

	private void universeRemove(SpaceObject so) {
		universe.remove(so);
	}

	private void universeRemove(int i) {
		universe.remove(i);
	}
	/*
	 * public void removeStarship(Starship ship) { universe.remove(ship);
	 * starships.remove(ship); }
	 * 
	 * public void removeProjectile(Projectile projectile) {
	 * universe.remove(projectile); projectiles.remove(projectile); }
	 */

	/*
	 * public void removeAsteroid(Asteroid asteroid) {
	 * universe.remove(asteroid); asteroids.remove(asteroid); }
	 */
	public ScreenDamage getScreenDamage() {
		return screenEffect;
	}

	public Starship getPlayer() {
		return player;
	}

	public ArrayList<SpaceObject> getUniverse() {
		return universe;
	}

	public ArrayList<Starship> getStarships() {
		// return starships;
		ArrayList<Starship> result = new ArrayList<Starship>();
		for (SpaceObject o : universe) {
			if (o instanceof Starship) {
				result.add((Starship) o);
			}
		}
		return result;
	}

	public ArrayList<Projectile> getProjectiles() {
		// return projectiles;
		ArrayList<Projectile> result = new ArrayList<Projectile>();
		for (SpaceObject o : universe) {
			if (o instanceof Projectile) {
				result.add((Projectile) o);
			}
		}
		return result;
	}
	/*
	 * public ArrayList<Asteroid> getAsteroids() { return asteroids; }
	 */

	public double angleBetween(SpaceObject from, SpaceObject to) {
		return to.getAngleFrom(from);
	}

	public boolean exists(Object what) {
		return what != null;
	}

	public boolean isAlive(SpaceObject what) {
		boolean result = false;
		if (what instanceof Starship) {
			result = getStarships().contains(what);
		}
		/*
		 * else if(what instanceof Asteroid) { result =
		 * asteroids.contains(what); }
		 */
		else if (what instanceof Projectile) {
			result = getProjectiles().contains(what);
		}
		return result;
	}

	public void print(String message) {
		System.out.println(getTick() + ". " + message);
	}

	/*
	 * public void collisionAsteroidStarship(Asteroid_Deprecated_2 a, Starship
	 * s) { //print("--> Collision (Starship)"); double angle_asteroid_to_ship =
	 * a.getAngleTowards(s); double angle_ship_to_asteroid = a.getAngleFrom(s);
	 * double asteroidKineticEnergy =
	 * a.getKineticEnergyAngled(angle_asteroid_to_ship); double
	 * shipKineticEnergy = s.getKineticEnergyAngled(angle_ship_to_asteroid);
	 * double totalKineticEnergy = asteroidKineticEnergy + shipKineticEnergy;
	 * double halfKineticEnergy = totalKineticEnergy / 2;
	 * s.impulse(angle_ship_to_asteroid, halfKineticEnergy);
	 * a.impulse(angle_asteroid_to_ship, halfKineticEnergy);
	 * 
	 * s.damage(halfKineticEnergy / 100);
	 * 
	 * //print("Angle (Asteroid --> Ship): " + angle_asteroid_to_ship); //print(
	 * "Angle (Asteroid <-- Ship): " + angle_ship_to_asteroid); //print(
	 * "Momentum (Asteroid) " + asteroidMomentum); //print("Momentum (Ship): " +
	 * shipMomentum); //print("<-- Collision (Starship)"); } public void
	 * collisionAsteroidAsteroid(Asteroid_Deprecated_2 a1, Asteroid_Deprecated_2
	 * a2) { double angle_a1_to_a2 = a1.getAngleTowards(a2); double
	 * angle_a2_to_a1 = a2.getAngleTowards(a1); double halfKineticEnergy =
	 * a1.getKineticEnergyAngled(angle_a1_to_a2) +
	 * a2.getKineticEnergyAngled(angle_a2_to_a1); a1.impulse(angle_a2_to_a1,
	 * halfKineticEnergy); a2.impulse(angle_a1_to_a2, halfKineticEnergy);
	 * a1.damage((int) halfKineticEnergy/1000, a2.getPosX(), a2.getPosY());
	 * a2.damage((int) halfKineticEnergy/1000, a1.getPosX(), a1.getPosY()); }
	 */
	public void collisionStarshipProjectile(Starship s1, Projectile p1, Area intersection) {
		s1.damage(p1.getDamage());
		s1.onAttacked(p1.getOwner());
		s1.accelerateEnergy(p1.getVelAngle(), p1.getKineticEnergy());
		p1.destroy();

		if (p1.getOwner() == player) {
			//score += p1.getDamage() * (0.2 * s1.getDistanceBetween(player)) / (0.1 * p1.getLifetime());
			effects.add(new RisingText(new Point((int) s1.getPosX(), (int) s1.getPosY() + 12), "" + p1.getDamage(), Color.RED));
			score += p1.getDamage() * ((tick % 300) / 30);
			hits++;
			checkHits();
		} else if (s1 == player) {
			effects.add(new RisingText(new Point((int) s1.getPosX(), (int) s1.getPosY() + 12), "" + p1.getDamage(), Color.YELLOW));
			score += p1.getDamage() * ((200 - player.getStructure()) / player.getStructureMax());
		}
		
		
	}

	private void checkHits() {
		/*
		if(hits == 1) {
			effects.add(new RisingText(new Point((int) player.getPosX(), (int) player.getPosY() + 12), "Level up! Engines upgraded!", Color.WHITE));
			player.setThrust(player.getThrust() * 1.5);
			player.setRotationAccel(player.getRotationAccel() * 1.5);
		}
		*/
	}

	public void collisionStarshipStarship(Starship s1, Starship s2, Area intersection) {
		// print("--> GamePanel: Starship-Starship Collision");
		double angle_s1 =
				// s1.getVelAngle()
				angleBetween(s1, s2);
		double angle_s2 =
				// s2.getVelAngle()
				angleBetween(s2, s1);

		// Old collision model
		/*
		 * double kinetic_energy_total = s1.getKineticEnergyAngled(angle_s1) +
		 * s2.getKineticEnergyAngled(angle_s2); double kinetic_energy_half =
		 * kinetic_energy_total / 2; s1.accelerateEnergy(angle_s2,
		 * kinetic_energy_half); s2.accelerateEnergy(angle_s1,
		 * kinetic_energy_half);
		 */

		/*
		 * double angle_diff_ccw = Helper.modRangeDegrees(angle_s1 - angle_s2);
		 * double angle_diff_cw = Helper.modRangeDegrees(angle_s2 - angle_s1);
		 * double angle_diff = Helper.min(angle_diff_ccw, angle_diff_cw);
		 */
		double velAngle_s1 = s1.getVelAngle();
		double velAngle_s2 = s2.getVelAngle();
		double velAngle_diff_ccw = SpaceHelper.modRangeDegrees(velAngle_s1 - velAngle_s2);
		double velAngle_diff_cw = SpaceHelper.modRangeDegrees(velAngle_s2 - velAngle_s1);
		double velAngle_diff = SpaceHelper.min(velAngle_diff_ccw, velAngle_diff_cw);

		double impactEnergy_s1 = Math
				.abs(s1.getKineticEnergy() - s2.getKineticEnergy() * SpaceHelper.cosDegrees(velAngle_diff));
		double impactEnergy_s2 = Math
				.abs(s2.getKineticEnergy() - s1.getKineticEnergy() * SpaceHelper.cosDegrees(velAngle_diff));

		s1.accelerateEnergy(angle_s2, impactEnergy_s1 * 0.01);
		s2.accelerateEnergy(angle_s1, impactEnergy_s2 * 0.01);
		s1.damage((int) (impactEnergy_s1 / s1.getMass()));
		s2.damage((int) (impactEnergy_s1 / s2.getMass()));
		// print("<-- GamePanel: Starship-Starship Collision");
	}

	public void collisionProjectileProjectile(Projectile p1, Projectile p2, Area intersection) {
		p1.damage(p2.getDamage());
		p2.damage(p1.getDamage());

		p1.accelerateEnergy(p2.getVelAngle(), p2.getKineticEnergy());
		p2.accelerateEnergy(p1.getVelAngle(), p1.getKineticEnergy());

		p1.destroy();
		p2.destroy();
	}

	public long getTick() {
		return tick;
	}

	public void setTick(long tick) {
		this.tick = tick;
	}

	public void drawStringCentered(Graphics g, String s, int size, Color color, int y) {
		g.setFont(new Font("Consolas", Font.BOLD, size));
		g.setColor(color);
		g.drawString(s, (GameWindow.SCREEN_WIDTH / 2) - (g.getFontMetrics().stringWidth(s) / 2),
				y - (size / 2)
				);
	}

	public void addEffect(Effect e) {
		effects.add(e);
	}

}
