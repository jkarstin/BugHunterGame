package uidaho.cs328.bhg.dist.actors;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;

public class BaseActor extends Group
{
	
	//Static fields
	private static Rectangle worldBounds;
	//Animation fields
	private Animation<TextureRegion> animation;
	private float elapsedTime;
	private boolean animationPaused;
	//Physics fields
	private Vector2 velocityVec;
	private Vector2 accelerationVec;
	private float acceleration;
	private float maxSpeed;
	private float deceleration;
	//Collisions fields
	private Polygon boundaryPolygon;
	
	/*** Constructor ***/
	
	public BaseActor(float x, float y, Stage s)
	{
		//call constructor from Actor class
		super();
		//perform additional initialization tasks
		this.setPosition(x, y);
		s.addActor(this);
		
		//Animation field initialization
		this.animation = null;
		this.elapsedTime = 0;
		this.animationPaused = false;
		//Physics field initialization
		this.velocityVec = new Vector2(0f, 0f);
		this.accelerationVec = new Vector2(0f, 0f);
		this.acceleration = 0f;
		this.maxSpeed = 1000f;
		this.deceleration = 0f;
	}
	
	@Override
	public void act(float dt)
	{
		super.act(dt);
		
		if (!this.animationPaused)
		{
			this.elapsedTime += dt;
		}
	}
	
	/*** Animation methods ***/
	
	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		//apply color tint effect
		Color c = this.getColor();
		batch.setColor(c);
		
		if (this.animation != null && this.isVisible())
		{
			batch.draw(
					this.animation.getKeyFrame(this.elapsedTime),
					this.getX(),
					this.getY(),
					this.getOriginX(),
					this.getOriginY(),
					this.getWidth(),
					this.getHeight(),
					this.getScaleX(),
					this.getScaleY(),
					this.getRotation()
					);
		}
		
		super.draw(batch, parentAlpha);
	}
	
	public void setAnimation(Animation<TextureRegion> animation)
	{
		this.animation = animation;
		TextureRegion tr = this.animation.getKeyFrame(0);
		float w = tr.getRegionWidth();
		float h = tr.getRegionHeight();
		this.setSize(w, h);
		this.setOrigin(w/2, h/2);
		
		//Since size was just set, set boundary rectangle
		if (this.boundaryPolygon == null)
		{
			this.setBoundaryRectangle();
		}
	}
	
	public void setAnimationPaused(boolean pause)
	{
		this.animationPaused = pause;
	}
	
	public Animation<TextureRegion> loadAnimationFromFiles(String[] fileNames, float frameDuration, boolean loop)
	{
		int fileCount = fileNames.length;
		Array<TextureRegion> textureArray = new Array<TextureRegion>();
		
		for (int n=0; n < fileCount; n++)
		{
			String fileName = fileNames[n];
			Texture texture = new Texture(Gdx.files.internal(fileName));
			texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			textureArray.add(new TextureRegion(texture));
		}
		
		Animation<TextureRegion> anim = new Animation<TextureRegion>(frameDuration, textureArray);
		
		if (loop)
		{
			anim.setPlayMode(Animation.PlayMode.LOOP);
		}
		else
		{
			anim.setPlayMode(Animation.PlayMode.NORMAL);
		}
		
		if (this.animation == null)
		{
			this.setAnimation(anim);
		}
		
		return anim;
	}
	
	public Animation<TextureRegion> loadAnimationFromSheet(String fileName, int rows, int cols, float frameDuration, boolean loop)
	{
		Texture texture = new Texture(Gdx.files.internal(fileName));
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		int frameWidth = texture.getWidth() / cols;
		int frameHeight = texture.getHeight() / rows;
		
		TextureRegion[][] temp = TextureRegion.split(texture, frameWidth, frameHeight);
		Array<TextureRegion> textureArray = new Array<TextureRegion>();
		
		for (int r=0; r < rows; r++)
		{
			for (int c=0; c < cols; c++)
			{
				textureArray.add(temp[r][c]);
			}
		}
		
		Animation<TextureRegion> anim = new Animation<TextureRegion>(frameDuration, textureArray);
		
		if (loop)
		{
			anim.setPlayMode(Animation.PlayMode.LOOP);
		}
		else
		{
			anim.setPlayMode(Animation.PlayMode.NORMAL);
		}
		
		if (this.animation == null)
		{
			this.setAnimation(anim);
		}
		
		return anim;
	}
	
	public Animation<TextureRegion> loadTexture(String fileName)
	{
		String[] fileNames = new String[1];
		fileNames[0] = fileName;
		return loadAnimationFromFiles(fileNames, 1f, true);
	}
	
	public boolean isAnimationFinished()
	{
		return this.animation.isAnimationFinished(this.elapsedTime);
	}
	
	public void setOpacity(float opacity)
	{
		this.getColor().a = opacity;
	}
	
	/*** Physics methods ***/
	
	public void setSpeed(float speed)
	{
		//if length is zero, then assume motion angle is zero degrees
		if (this.velocityVec.len() == 0f)
		{
			this.velocityVec.set(speed, 0f);
		}
		else
		{
			this.velocityVec.setLength(speed);
		}
	}
	
	public float getSpeed()
	{
		return this.velocityVec.len();
	}
	
	public void setMotionAngle(float angle)
	{
		this.velocityVec.setAngle(angle);
	}
	
	public float getMotionAngle()
	{
		return this.velocityVec.angle();
	}
	
	public boolean isMoving()
	{
		return (this.getSpeed() > 0f);
	}
	
	public void setAcceleration(float acc)
	{
		this.acceleration = acc;
	}
	
	public void accelerateAtAngle(float angle)
	{
		this.accelerationVec.add(new Vector2(this.acceleration, 0f).setAngle(angle));
	}
	
	public void accelerateForward()
	{
		this.accelerateAtAngle(this.getRotation());
	}
	
	public void setMaxSpeed(float ms)
	{
		this.maxSpeed = ms;
	}
	
	public void setDeceleration(float dec)
	{
		this.deceleration = dec;
	}
	
	public void applyPhysics(float dt)
	{
		//apply acceleration
		this.velocityVec.add(this.accelerationVec.x * dt, this.accelerationVec.y * dt);
		
		float speed = this.getSpeed();
		
		//decrease speed (decelerate) when not accelerating
		if (this.accelerationVec.len() == 0f)
		{
			speed -= this.deceleration * dt;
		}
		
		//keep speed within set bounds
		speed = MathUtils.clamp(speed, 0f, this.maxSpeed);
		
		//update velocity
		this.setSpeed(speed);
		
		//apply velocity
		this.moveBy(this.velocityVec.x * dt, this.velocityVec.y * dt);
		
		//reset acceleration
		this.accelerationVec.set(0f, 0f);
	}
	
	public void centerAtPosition(float x, float y)
	{
		this.setPosition(x - this.getWidth()/2, y - this.getHeight()/2);
	}
	
	public void centerAtActor(BaseActor other)
	{
		this.centerAtPosition(other.getX() + other.getWidth()/2, other.getY() + other.getHeight()/2);
	}
	
	/*** Collisions methods ***/
	
	public void setBoundaryRectangle()
	{
		float w = this.getWidth();
		float h = this.getHeight();
		float[] vertices = {0f, 0f, w, 0f, w, h, 0f, h};
		this.boundaryPolygon = new Polygon(vertices);
	}
	
	public void setBoundaryPolygon(int numSides)
	{
		float w = this.getWidth();
		float h = this.getHeight();
		float[] vertices = new float[2*numSides];
		
		for (int i=0; i < numSides; i++)
		{
			float angle = i * MathUtils.PI2 / numSides;
			//x-coordinate
			vertices[2*i] = w/2 * MathUtils.cos(angle) + w/2;
			//y-coordinate
			vertices[2*i+1] = h/2 * MathUtils.sin(angle) + h/2;
		}
		
		this.boundaryPolygon = new Polygon(vertices);
	}
	
	public Polygon getBoundaryPolygon()
	{
		this.boundaryPolygon.setPosition(this.getX(),  this.getY());
		this.boundaryPolygon.setOrigin(this.getOriginX(), this.getOriginY());
		this.boundaryPolygon.setRotation(this.getRotation());
		this.boundaryPolygon.setScale(this.getScaleX(), this.getScaleY());
		return this.boundaryPolygon;
	}
	
	public boolean overlaps(BaseActor other)
	{
		Polygon poly1 = this.getBoundaryPolygon();
		Polygon poly2 = other.getBoundaryPolygon();
		
		//initial test to improve performance (MUCH more efficient collision detection algorithm)
		if (!poly1.getBoundingRectangle().overlaps(poly2.getBoundingRectangle()))
		{
			return false;
		}
		
		return Intersector.overlapConvexPolygons(poly1, poly2);
	}
	
	public Vector2 preventOverlap(BaseActor other)
	{
		Polygon poly1 = this.getBoundaryPolygon();
		Polygon poly2 = other.getBoundaryPolygon();
		
		//initial test to improve performance
		if (!poly1.getBoundingRectangle().overlaps(poly2.getBoundingRectangle()))
		{
			return null;
		}
		
		MinimumTranslationVector mtv = new MinimumTranslationVector();
		boolean polygonOverlap = Intersector.overlapConvexPolygons(poly1, poly2, mtv);
		
		if (!polygonOverlap)
		{
			return null;
		}
		
		this.moveBy(mtv.normal.x * mtv.depth, mtv.normal.y * mtv.depth);
		return mtv.normal;
	}
	
	/*** World boundary methods ***/
	
	public static void setWorldBounds(float width, float height)
	{
		BaseActor.worldBounds = new Rectangle(0f, 0f, width, height);
	}
	
	public static void setWorldBounds(BaseActor ba)
	{
		BaseActor.setWorldBounds(ba.getWidth(), ba.getHeight());
	}
	
	public void boundToWorld()
	{
		//check left edge
		if (this.getX() < 0f)
		{
			this.setX(0f);
		}
		//check right edge
		if (this.getX() + this.getWidth() > BaseActor.worldBounds.width)
		{
			this.setX(BaseActor.worldBounds.width - this.getWidth());
		}
		//check bottom edge
		if (this.getY() < 0f)
		{
			this.setY(0f);
		}
		//check top edge
		if (this.getY() + this.getHeight() > BaseActor.worldBounds.height)
		{
			this.setY(BaseActor.worldBounds.height - this.getHeight());
		}
	}
	
	public void wrapAroundWorld()
	{
		if (this.getX() + this.getWidth() < 0f)
		{
			this.setX(BaseActor.worldBounds.width);
		}
		if (this.getX() > BaseActor.worldBounds.width)
		{
			this.setX(-this.getWidth());
		}
		if (this.getY() + this.getHeight() < 0f)
		{
			this.setY(BaseActor.worldBounds.height);
		}
		if (this.getY() > BaseActor.worldBounds.height)
		{
			this.setY(-this.getHeight());
		}
	}
	
	/*** Camera methods ***/
	
	public void alignCamera()
	{
		Camera cam = this.getStage().getCamera();
		
		//center camera on actor
		cam.position.set(this.getX() + this.getOriginX(), this.getY() + this.getOriginY(), 0f);
		
		//bound camera to layout
		cam.position.x = MathUtils.clamp(
				cam.position.x,
				cam.viewportWidth/2,
				worldBounds.width - cam.viewportWidth/2
				);
		cam.position.y = MathUtils.clamp(
				cam.position.y,
				cam.viewportHeight/2,
				worldBounds.height - cam.viewportHeight/2
				);
		cam.update();
	}
	
	/*** Asset management methods ***/
	
	public static ArrayList<BaseActor> getList(Stage stage, String className)
	{
		ArrayList<BaseActor> list = new ArrayList<BaseActor>();
		
		@SuppressWarnings("rawtypes")
		Class theClass = null;
		
		try
		{
			theClass = Class.forName(className);
		}
		catch (Exception error)
		{
			error.printStackTrace();
			System.out.println("Use '" + className + ".class.getName()' or '" + className + ".class.getCanonicalName()' for more reliable results");
		}
		
		for (Actor a : stage.getActors())
		{
			if (theClass.isInstance(a))
			{
				list.add((BaseActor)a);
			}
			
		}
		
		return list;
	}
	
	public static int count(Stage stage, String className)
	{
		return BaseActor.getList(stage, className).size();
	}
	
}
