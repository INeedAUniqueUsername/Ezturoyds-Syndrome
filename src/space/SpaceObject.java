package space;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import body.Body;
import body.IBody;
import game.GamePanel;
import game.GameWindow;
import helpers.SpaceHelper;
import interfaces.GameObject;

public abstract class SpaceObject implements GameObject {
	private String name = "";
	private final double c = 9131.35261864;
	
	protected double pos_x; //Transition to get/set
	protected double pos_y; //Transition to get/set
	protected double pos_r; //Transition to get/set
	
	protected double vel_x; //Transition to get/set
	protected double vel_y; //Transition to get/set
	protected double vel_r; //Transition to get/set
	private IBody body;
	private double size;
	
	//private int last_collision_tick = 0;
	
	private boolean active = true;
	/*	=	=	=	=		Setters			=	=	=	=	=*/
	public SpaceObject() {
		setPos(0, 0, 0);
		setVel(0, 0, 0);
		setBody(new Body());
	}
	public final void setName(String name_new) { name = name_new; }
	public final String getName() { return name; }
	public final void printToWorld(String text) {
		if(name.equals(""))
		{
			GamePanel.getWorld().printToScreen(text);
		}
		else
		{
			GamePanel.getWorld().printToScreen("[" + getClass().getName() + "]" + " " + name + " - " + text);
		}
		
	}
	
	public final int factorialAddition(int input)
	{
		int result = 0;
		while(input > 0)
		{
			result += input;
			input--;
		}
		return result;
	}
	
	public final Point2D.Double calcFuturePos(double time) {
		return new Point2D.Double(pos_x + time * vel_x, pos_y + time * vel_y);
	}
	public final Point2D.Double calcFireTargetPos(Point2D.Double pos_diff, Point2D.Double vel_diff, double weapon_speed) {
		Point2D.Double posDiff = SpaceHelper.calcFireSolutionTargetPosDiff(pos_diff, vel_diff, weapon_speed);
		return new Point2D.Double(getPosX() + posDiff.getX(), getPosY() + posDiff.getY());
	}
	public final double calcFireAngle(SpaceObject target, double projectile_speed) {
		return calcFireAngle(target.getPosX(), target.getPosY(), target.getVelX(), target.getVelY(), projectile_speed);
	}
	public final double calcFireAngle(double target_pos_x, double target_pos_y, double target_vel_x, double target_vel_y, double projectile_speed) {
		return SpaceHelper.calcFireAngle(
				new Point2D.Double(
						target_pos_x - getPosX(),
						target_pos_y - getPosY()
						),
				new Point2D.Double(
						target_vel_x - getVelX(),
						target_vel_y - getVelY()
						),
				projectile_speed
				);
	}
	public final double calcFireDistance(double target_pos_x, double target_pos_y, double target_vel_x, double target_vel_y, double projectile_speed) {
		return SpaceHelper.calcFireDistance(
				new Point2D.Double(
						target_pos_x - getPosX(),
						target_pos_y - getPosY()
						),
				new Point2D.Double(
						target_vel_x - getVelX(),
						target_vel_y - getVelY()
						),
				projectile_speed
				);
	}
	
	public final void setPosRectangular(double x, double y) {
		pos_x = x;
		pos_y = y;
	}
	public final void setPosRectangular(Point2D pos) {
		pos_x = pos.getX();
		pos_y = pos.getY();
	}
	public final void setPosR(double posR) { pos_r = posR; }
	public final void setPos(double x, double y, double r) {
		pos_x = x;
		pos_y = y;
		pos_r = r;
	}
	public final void setVelRectangular(double x, double y)	{
		vel_x = x;
		vel_y = y;
	}
	public final void setVelRectangular(Point2D vel) {
		vel_x = vel.getX();
		vel_y = vel.getY();
	}
	public final void setVelR(double r) { vel_r = r; }
	public final void setVel(double x, double y, double r) {
		vel_x = x;
		vel_y = y;
		vel_r = r;
	}
	public final void setVelPolar(double angle, double speed) {
		setVelRectangular(speed*SpaceHelper.cosDegrees(angle), speed*SpaceHelper.sinDegrees(angle));
	}
	public final void incPosRectangular(double x, double y) {
		pos_x += x;
		pos_y += y;
	}
	public final void incPosPolar(double angle, double distance) {
		pos_x += distance * SpaceHelper.cosDegrees(angle);
		pos_y += distance * SpaceHelper.sinDegrees(angle);
	}
	public final void incVelRectangular(double x, double y)
	{
		setVelRectangular(getVelX() + x, getVelY() + y);
	}
	public final void incVelPolar(double angle, double speed)
	{
		setVelRectangular(getVelX() + speed*SpaceHelper.cosDegrees(angle), getVelY() + speed*SpaceHelper.sinDegrees(angle));
	}
	
	/*	=	=	=	=		Velocity		=	=	=	=	=*/
	
	
	public final void accelerate(double angle, double speed) {
		vel_x += speed*SpaceHelper.cosDegrees(angle);
		vel_y += speed*SpaceHelper.sinDegrees(angle);
	}
	public final void accelerateEnergy(double angle, double kineticEnergy) {
		accelerate(kineticEnergy > 0 ? angle : angle + 180, getAcceleration(kineticEnergy));
	}
	public final double getAcceleration(double kineticEnergy) {
		return Math.sqrt((2*Math.abs(kineticEnergy)/getRelativisticMass()));
	}
	public final double getRelativisticMass() {
		double ratio = 1 - (Math.pow(getVelSpeed(), 2)/Math.pow(GamePanel.LIGHT_SPEED, 2));
		if(ratio < GamePanel.epsilon) {
			ratio = GamePanel.epsilon;
		}
		return size / Math.sqrt(ratio);
	}
	public final void decelerate(double speed) {
		int velAngle = (int) SpaceHelper.arctanDegrees(vel_y, vel_x);
		int decelAngle = velAngle + 180;
		double xSpeedOriginal = vel_x;
		double ySpeedOriginal = vel_y;
		
		vel_x = (vel_x + speed*SpaceHelper.cosDegrees(decelAngle));
		vel_y = (vel_y + speed*SpaceHelper.sinDegrees(decelAngle));
		
		if(Math.abs(vel_x) > Math.abs(xSpeedOriginal)) {
			vel_x = 0;
		}
		if(Math.abs(vel_y) > Math.abs(ySpeedOriginal)) {
			vel_y = 0;
		}
	}
	
	/*	=	=	=	=		Trigonometry		=	=	=	=	=*/
	
	public final void rotateLeft(double accel) {
		vel_r = vel_r + accel;
	}
	public final void rotateRight(double accel) {
		vel_r = vel_r - accel;
	}
	public final void setBody(IBody b) {
		body = b;
	}
	public final IBody getBody() {
		return body;
	}
	public final void updateSize()
	{
		size = 0;
		for(Polygon part : body.getShapes())
		{
			size += Math.abs(SpaceHelper.polygonArea(part.xpoints, part.ypoints, part.npoints));
		}
		//System.out.println("Size: " + size);
	}
	public final void setSize(int size) {
		this.size = size;
	}
	public abstract void update();
	public void draw(Graphics g) {
		updateBody();
		drawBody(g);
	}
	public final void drawBody(Graphics g) { body.draw(g); }
	public void destroy()
	{
		setActive(false);
		onDestroy();
	}
	public void onDestroy() {}
	public final boolean getActive() { return active; }
	public final void setActive(boolean b) { active = b; }
	public final double getAngleTowards(SpaceObject other)
	{
		return SpaceHelper.getAngleTowardsPos(getPos(), other.getPos());
	}
	public final double getAngleFrom(SpaceObject other)
	{
		return SpaceHelper.getAngleFromPos(getPos(), other.getPos());
	}
	
	public final double getAngleTowardsPos (Point2D.Double pos)
	{
		return SpaceHelper.arctanDegrees(pos.getY() - getPosY(), pos.getX() - getPosX());
	}
	public final double getAngleFromPos(Point2D.Double pos)
	{
		return SpaceHelper.arctanDegrees(getPosY() - pos.getY(), getPosX() - pos.getX());
	}
	
	public final double getDistanceBetween(SpaceObject target)
	{
		return SpaceHelper.getDistanceBetweenPos(getPos(), target.getPos());
	}
	public final double getDistanceBetweenPos(Point2D.Double pos)
	{
		return SpaceHelper.getDistanceBetweenPos(getPos(), pos);		
	}
	public final void updatePosition() {
		pos_r = pos_r + vel_r;
		pos_x = pos_x + vel_x;
		pos_y = pos_y + vel_y;
		
		if(pos_x < 0) {
			pos_x += GameWindow.GAME_WIDTH;
		} else if(pos_x > GameWindow.GAME_WIDTH) {
			pos_x -= GameWindow.GAME_WIDTH;
		}
		
		if(pos_y < 0) {
			pos_y += GameWindow.GAME_HEIGHT;
		}
		if(pos_y > GameWindow.GAME_HEIGHT) {
			pos_y -= GameWindow.GAME_HEIGHT;
		}
	}
	public final void updateBody() { body.updateShapes(); }
	public final Point2D.Double getPos() { return new Point2D.Double(pos_x, pos_y); }
	public final double getPosX() { return pos_x; }
	public final double getPosY() { return pos_y; }
	public final double getPosR() { return pos_r; }
	public final Point2D.Double polarOffset(double angle, double distance) {
		return new Point2D.Double(pos_x + distance * SpaceHelper.cosDegrees(angle), pos_y + distance * SpaceHelper.sinDegrees(angle));
	}
	public final double getVelAngle() {
		if(!(vel_x == 0 && vel_y == 0))
		{
			return SpaceHelper.arctanDegrees(vel_y, vel_x);
		}
		else
		{
			return pos_r;
		}
	}
	public final double getVelAtAngle(double angle) {
		return getVelSpeed()*SpaceHelper.cosDegrees(getVelAngle() - angle);
	}
	public final Point2D.Double getVel() {
		return new Point2D.Double(getVelX(), getVelY());
	}
	
	public final double getVelX() { return vel_x; }
	public final double getVelY() { return vel_y; }
	public final double getVelR() { return vel_r; }
	public final double getVelSpeed() {
		return Math.sqrt(Math.pow(vel_x, 2) + Math.pow(vel_y, 2));
	}
	public final double getMass() { return size; }
	public final double getKineticEnergy() {
		//System.out.println("Speed: " + getVelSpeed());
		//System.out.println("Size: " + size);
		//System.out.println("Momentum: " + getVelSpeed()*size);
		//System.out.println("Vel Speed: " + getVelSpeed());
		//System.out.println("Size: " + getMass());
		return 0.5*getMass()*Math.pow(getVelSpeed(), 2);
	}
	public final double getKineticEnergyAngled(double angle) {
		/*
		double angleCW = Math.abs(pos_r - angle);
		double angleCCW = Math.abs(angle - pos_r);
		double angleDiff;
		if(angleCW < angleCCW)
		{
			angleDiff = angleCW;
		}
		else
		{
			angleDiff = angleCCW;
		}
		
		return getMomentum()*cosDegrees(angleDiff);
		*/
		//System.out.println("Angle: " + angle);
		//System.out.println("Vel Angle: " + angle);
		//System.out.println("Kinetic Energy: " + getKineticEnergy());
		//System.out.println("Angled Kinetic Energy: " + getKineticEnergy()*SpaceHelper.cosDegrees(getVelAngle()-angle));
		return getKineticEnergy()*SpaceHelper.cosDegrees(getVelAngle()-angle);
	}
	
	public final void print(String message) { //System.out.println(GamePanel.getWorld().getTick() + ". " + message);
	}
	public final boolean exists(Object o) { return o != null; }
	public void onCollision(SpaceObject other) { }
}
