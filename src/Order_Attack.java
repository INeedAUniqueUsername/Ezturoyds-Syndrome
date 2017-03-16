import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Order_Attack extends Behavior {
	private SpaceObject target;
	public Order_Attack(Starship_NPC o, SpaceObject t) {
		super(o);
		setTarget(t);
	}
	
	public SpaceObject getTarget() {
		return target;
	}
	public void setTarget(SpaceObject t) {
		target = t;
	}
	public void update() {
		
		//Problem: Target is dead
		if(!target.getActive()) {
			System.out.println("Attack order done");
			setActive(false);
			return; //Done
		}
		
		//Problem: Objects are too close to us
		ArrayList<SpaceObject> objectsTooClose = getObjectsTooClose();
		int objectsTooCloseCount = objectsTooClose.size();
		if(objectsTooCloseCount > 0)
		{
			double angle_destination = 0;
			for(SpaceObject o : objectsTooClose)
			{
				angle_destination += owner.getAngleFrom(o);
			}
			angle_destination /= objectsTooCloseCount;
			owner.turnDirection(owner.calcTurnDirection(angle_destination));
			setThrusting(ThrustingState.THRUST);
			System.out.println("Destination Angle: " + angle_destination);
			return; //Done
		}
		
		//Problems: None. Attack as normal
		double[] targetStats = getNearestTargetClone(owner, target);
		double target_x = targetStats[0];
		double target_y = targetStats[1];
		double target_distance = targetStats[2];
		
		ThrustingState action_thrusting = ThrustingState.NOTHING;
		RotatingState action_rotation = RotatingState.NOTHING;
		StrafingState action_strafing = StrafingState.NOTHING;
		AttackingState action_weapon = AttackingState.NOTHING;
		//double angle_to_target = getAngleTowardsPos(target_x_focus, target_y_focus);
		double distance_to_target = target_distance;
		
		double angle_to_target = owner.calcFireAngle(
				target_x,
				target_y,
				target.getVelX(),
				target.getVelY(),
				owner.getWeaponPrimary().getProjectileSpeed()
				);
		double faceAngleDiff = owner.calcFutureAngleDifference(angle_to_target);
		
		double velAngle = owner.getVelAngle();
		double velAngleDiffCCW = SpaceObject.modRangeDegrees(angle_to_target - velAngle);
		double velAngleDiffCW = SpaceObject.modRangeDegrees(velAngle - angle_to_target);
		
		double velAngleDiff = SpaceObject.min(velAngleDiffCCW, velAngleDiffCW);
		
		//double velDiff = owner.getVelRadial(angle_to_target) - target.getVelRadial(angle_to_target);
		
		if(faceAngleDiff > owner.getController().getMaxAngleDifference())
		{
			action_rotation = owner.calcTurnDirection(angle_to_target);
		}
		else
		{
			owner.printToWorld("Status (Facing): Aligned");
			action_weapon = AttackingState.FIRE;
		}
		
		if(velAngleDiff > 120)
		{
			action_thrusting = ThrustingState.BRAKE;
			owner.printToWorld("Status: Brake");
		}
		else if(velAngleDiff > 60)
		{
			owner.printToWorld("Status: Nothing");
		}
		else
		{
			action_thrusting = ThrustingState.THRUST;
			owner.printToWorld("Status: Thrust");
		}
		if(distance_to_target > owner.getWeaponPrimary().getProjectileRange()) //owner.getMaxSeparationFromTarget()
		{
			//Move towards target
			action_thrusting = ThrustingState.THRUST;
			
			owner.printToWorld("Status (Distance): Far");
		} else if(distance_to_target < owner.getController().getMinSeparationFromTarget()) {
			//Move away from target
			action_rotation = owner.calcTurnDirection(owner.getAngleFrom(target));
			if(faceAngleDiff > 90)
			{
				action_thrusting = ThrustingState.THRUST;
			}
		} else {
			action_thrusting = ThrustingState.BRAKE;
			owner.printToWorld("Status (Distance): Close");
		}
		owner.printToWorld("Angle to Target: " + angle_to_target);
		owner.printToWorld("Max Facing Angle Difference: " + owner.getController().getMaxAngleDifference());
		owner.printToWorld("Velocity Angle: " + velAngle);
		owner.printToWorld("Velocity Angle Difference CCW: " + velAngleDiffCCW);
		owner.printToWorld("Velocity Angle Difference CW: " + velAngleDiffCW);
		owner.printToWorld("Velocity Angle Difference: " + velAngleDiff);
		owner.printToWorld("Weapons: " + action_weapon);
		setActions(action_thrusting, action_rotation, action_strafing, action_weapon);
	}
	public boolean avoidNearbyObjects() {
		return true;
	}
	public ArrayList<SpaceObject> getObjectsTooClose() {
		ArrayList<SpaceObject> result = new ArrayList<SpaceObject>();
		for(SpaceObject o : GamePanel.world.getStarships())
		{
			if(!o.equals(owner))
			{
				if(owner.getDistanceBetween(o) < owner.getController().getMinSeparationFromOthers())
				{
					System.out.println(o.getName() + " is " + owner.getDistanceBetween(o) + " away (too close).");
					result.add(o);
				}
			} else {
				System.out.println("It's you.");
			}
		}
		return result;
	}
}