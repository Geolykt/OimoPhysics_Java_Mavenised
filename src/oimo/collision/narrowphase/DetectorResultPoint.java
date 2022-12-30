package oimo.collision.narrowphase;
import oimo.common.Vec3;

/**
 * The result point is a pair of the closest points of collision geometries
 * detected by a collision detector. This holds relative closest points for
 * each collision geometry and the amount of the overlap.
 */
public class DetectorResultPoint {
	/**
	 * The first collision geometry's closest point.
	 */
	public Vec3 position1;

	/**
	 * The second collision geometry's closest point.
	 */
	public Vec3 position2;

	/**
	 * The amount of the overlap. This becomes negative if two geometries are
	 * separate.
	 */
	public float depth;

	/**
	 * The identification of the result point.
	 */
	public int id;

	public DetectorResultPoint() {
		position1 = new Vec3();
		position2 = new Vec3();
		depth = 0;
		id = 0;
	}
}