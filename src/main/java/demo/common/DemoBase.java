package demo.common;
import oimo.common.DebugDraw;
import oimo.common.Mat3;
import oimo.common.Mat4;
import oimo.common.MathUtil;
import oimo.common.Quat;
import oimo.common.Vec3;
import oimo.dynamics.World;
import oimo.dynamics.callback.RayCastClosest;
import oimo.dynamics.constraint.joint.SphericalJoint;
import oimo.dynamics.constraint.joint.SphericalJointConfig;
import oimo.dynamics.rigidbody.RigidBody;
import oimo.dynamics.rigidbody.RigidBodyConfig;
import oimo.dynamics.rigidbody.RigidBodyType;

/**
 * Base class of demos.
 */
public class DemoBase {
	public String demoName;
	public double dt;
	protected World world;
	protected DemoRenderer renderer;
	UserInput input;
	int count;
	ViewInfo viewInfo;
	double grabbingDistance;
	RigidBody mouseJointDummyBody;
	SphericalJoint mouseJoint;

	public DemoBase(String demoName) {
		this.demoName = demoName;
		count = 0;
	}

	
	public void init(World world, DemoRenderer renderer, UserInput input, ViewInfo viewInfo) {
		this.world = world;
		this.renderer = renderer;
		this.input = input;
		this.viewInfo = viewInfo;
		renderer.camera(new Vec3(0, 6, 18), new Vec3(), new Vec3(0, 1, 0));
		RigidBodyConfig rigidBodyConfig = new RigidBodyConfig();
		rigidBodyConfig.type = RigidBodyType.STATIC;
		mouseJointDummyBody = new RigidBody(rigidBodyConfig);
		mouseJoint = null;
		dt = 1 / 60.0;
	}

	public void initControls(Control[] controls) {
	}

	public void teleportRigidBodies(double thresholdY, double toY, double rangeX, double rangeZ) {
		RigidBody rb = world.getRigidBodyList();
		Vec3 pos = new Vec3();
		Vec3 zero = new Vec3();
		while (rb != null) {
			rb.getPositionTo(pos);
			if (pos.y < thresholdY) {
				pos.y = toY;
				pos.x = MathUtil.randIn(-1, 1) * rangeX;
				pos.z = MathUtil.randIn(-1, 1) * rangeZ;
				rb.setPosition(pos);
				rb.setLinearVelocity(zero);
			}
			rb = rb._next;
		}
	}

	public void update() {
		count++;
		updateMouseJoint();
		double dx=input.mouseX-input.pmouseX;
		double dy=input.mouseY-input.pmouseY;
		if(mouseJoint==null && (dx!=0 || dy!=0) && input.mouseL) {
			Vec3 p=renderer.getCameraPosition();
			Quat q=new Quat();
			Mat3 m=new Mat3();
			Vec3 v= p.cross(new Vec3(0,1,0));
			v.normalize();
			m.identity();
			m.appendRotationEq(dy/200, v.x,v.y,v.z);
			//p.add3Eq(0,dy/50,0);
			p.mulMat3Eq(m);
			
			v= v.cross(p);
			v.normalize();
			m.identity();
			m.appendRotationEq(-dx/200, v.x,v.y,v.z);
			p.mulMat3Eq(m);
			
			
			//p.mulMat3Eq(m);
			
			renderer.camera(p, new Vec3(), new Vec3(0,1,0));
		}
		if(input.mouseScroll!=0 && false) {
			double fov=renderer.getFov();
			if(input.mouseScroll>0) {
				input.mouseScroll--;
				fov=renderer.getFov()/0.98;
				if(input.mouseScroll<0)input.mouseScroll=0;
			}else if(input.mouseScroll<0) {
				input.mouseScroll++;
				fov=renderer.getFov()*0.98;
				if(input.mouseScroll>0)input.mouseScroll=0;
			}
			if(fov<0.1)fov=0.1;
			if(fov>3.0)fov=3.0;
			renderer.perspective(fov,viewInfo.screenWidth/viewInfo.screenHeight);
			input.mouseScroll=0;
		}
	}

	public void drawAdditionalObjects(DebugDraw debugDraw) {
	}

	void updateMouseJoint() {
		Vec3 cameraPos = renderer.getCameraPosition(); // camera

		double screenX = input.mouseX / viewInfo.width - 0.5;
		double screenY = 0.5 - input.mouseY / viewInfo.height;

		Vec3 screenPos = new Vec3(screenX * viewInfo.screenWidth, screenY * viewInfo.screenHeight, -viewInfo.screenDistance);

		Mat4 viewMat = renderer.getViewMatrix();
		viewMat.transposeEq();
		viewMat.e03 = 0; // remove translations
		viewMat.e13 = 0;
		viewMat.e23 = 0;
		viewMat.e33 = 0;
		screenPos.mulMat4Eq(viewMat).normalize();

		if (mouseJoint != null) {
			if (input.mouseL) {
				//var t:Float = grabbingDistance / screenPos.z;
				mouseJointDummyBody.setPosition(cameraPos.add(screenPos.scale(grabbingDistance)));
				mouseJoint.getRigidBody1().wakeUp();
				mouseJoint.getRigidBody2().wakeUp();
			} else {
				world.removeJoint(mouseJoint);
				mouseJoint = null;
			}
		} else {
			if (input.mouseL && !input.pmouseL) { // clicked
				// ray casting
				Vec3 end = cameraPos.add(screenPos.scale(500));

				RayCastClosest closest = new RayCastClosest();
				world.rayCast(cameraPos, end, closest);

				if (!closest.hit) return;

				RigidBody body = closest.shape.getRigidBody();
				Vec3 position = closest.position;

				if (body == null || body.getType() != RigidBodyType.DYNAMIC) return;

				SphericalJointConfig jc = new SphericalJointConfig();
				jc.springDamper.frequency = 4.0;
				jc.springDamper.dampingRatio = 1;
				jc.rigidBody1 = body;
				jc.rigidBody2 = mouseJointDummyBody;
				jc.allowCollision = false;
				jc.localAnchor1 = position.sub(body.getPosition());
				jc.localAnchor1.mulMat3Eq(body.getRotation().transposeEq());
				jc.localAnchor2.zero();
				mouseJointDummyBody.setPosition(position);
				mouseJoint = new SphericalJoint(jc);
				world.addJoint(mouseJoint);
				grabbingDistance = position.sub(cameraPos).length();
			}
		}
	}

	public void draw() {
		renderer.draw(this);
	}

	public String additionalInfo() {
		return "";
	}
}