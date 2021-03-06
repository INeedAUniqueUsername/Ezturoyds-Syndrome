package body;
import java.awt.Polygon;
import java.util.ArrayList;

import helpers.SpaceHelper;
import space.Starship;

public class Body_Starship extends Body {
	private Starship owner;
	private int bodySize, headSize;
	public Body_Starship(Starship s) {
		super();
		setOwner(s);
		bodySize = 15;
		headSize = 10;
	}
	public Body_Starship(Starship s, int  bodySize, int headSize) {
		super();
		setOwner(s);
		this.bodySize = bodySize;
		this.headSize = headSize;
	}
	public void setOwner(Starship s) {
		owner = s;
	}
	public Starship getOwner() {
		return owner;
	}
	
	public void updateShapes() {
		setShapes(createTriangle(owner.getPos(), owner.getPosR(), bodySize), createTriangle(owner.polarOffset(owner.getPosR(), bodySize), owner.getPosR(), headSize));
		/*
		final int HEAD_SIZE = 10; //20
		final int BODY_SIZE = 15; //30
		
		final double pos_x = owner.getPosX();
		final double pos_y = owner.getPosY();
		final double pos_r = owner.getPosR();
		
		int[] middleX = new int[4];
		int[] middleY = new int[4];

		int middleFrontX = (int) (pos_x + BODY_SIZE * SpaceHelper.cosDegrees(pos_r));
		int middleFrontY = (int) (pos_y + BODY_SIZE * SpaceHelper.sinDegrees(pos_r));

		middleX[0] = middleFrontX;
		middleY[0] = middleFrontY;

		middleX[1] = (int) (pos_x + BODY_SIZE * SpaceHelper.cosDegrees(pos_r - 120));
		middleY[1] = (int) (pos_y + BODY_SIZE * SpaceHelper.sinDegrees(pos_r - 120));

		middleX[2] = (int) (pos_x + BODY_SIZE * SpaceHelper.cosDegrees(pos_r + 120));
		middleY[2] = (int) (pos_y + BODY_SIZE * SpaceHelper.sinDegrees(pos_r + 120));
		
		middleX[3] = middleFrontX;
		middleY[3] = middleFrontY;
		Polygon middle = new Polygon(middleX, middleY, 4);
		
		int[] headX = new int[4];
		int[] headY = new int[4];
		
		int headFrontX = (int) (middleFrontX + HEAD_SIZE * SpaceHelper.cosDegrees(pos_r));
		int headFrontY = (int) (middleFrontY + HEAD_SIZE * SpaceHelper.sinDegrees(pos_r));

		headX[0] = headFrontX;
		headY[0] = headFrontY;

		headX[1] = (int) (middleFrontX + HEAD_SIZE * SpaceHelper.cosDegrees(pos_r - 120));
		headY[1] = (int) (middleFrontY + HEAD_SIZE * SpaceHelper.sinDegrees(pos_r - 120));

		headX[2] = (int) (middleFrontX + HEAD_SIZE * SpaceHelper.cosDegrees(pos_r + 120));
		headY[2] = (int) (middleFrontY + HEAD_SIZE * SpaceHelper.sinDegrees(pos_r + 120));

		headX[3] = headFrontX;
		headY[3] = headFrontY;
		
		Polygon head = new Polygon(headX, headY, 4);
		
		setShapes(head, middle);
		*/
	}
}
