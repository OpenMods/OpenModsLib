package openmods.gui.misc;

import com.google.common.base.Preconditions;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

public class Trackball {
	public static class TrackballWrapper {
		private final Trackball target = new Trackball();
		private final float radius;
		private boolean isDragging;

		public TrackballWrapper(int radiusPx) {
			this.radius = radiusPx;
		}

		public void update(final Matrix4f matrix, int mouseX, int mouseY, boolean isButtonDown) {
			float mx = mouseX / radius;
			float my = mouseY / radius;

			if (!isDragging && isButtonDown) {
				isDragging = true;
				target.startDrag(mx, my);
			} else if (isDragging && !isButtonDown) {
				isDragging = false;
				target.endDrag(mx, my);
			}

			target.applyTransform(matrix, mx, my, isDragging);
		}

		public void setTransform(Matrix4f transform) {
			target.lastTransform = transform;
		}
	}

	private Vector3f dragStart;
	private Matrix4f lastTransform;

	public Trackball() {
		lastTransform = new Matrix4f();
		lastTransform.setIdentity();
	}

	private static Vector3f calculateSpherePoint(float x, float y) {
		Vector3f result = new Vector3f(x, y, 0);

		float sqrZ = 1 - result.dot(result);

		if (sqrZ > 0) {
			result.setZ((float)Math.sqrt(sqrZ));
		}

		result.normalize();
		return result;
	}

	private Matrix4f getTransform(float mouseX, float mouseY) {
		Preconditions.checkNotNull(dragStart, "Draging not started");
		Vector3f current = calculateSpherePoint(mouseX, mouseY);

		float dot = dragStart.dot(current);
		if (Math.abs(dot - 1) < 0.0001) {
			return lastTransform;
		}

		Vector3f axis = dragStart.copy();
		axis.cross(current);

		if (!axis.normalize()) {
			return lastTransform;
		}

		float angle = 2 * (float)(Math.acos(dot));

		final Quaternion rot = new Quaternion(axis, angle, false);

		Matrix4f rotation = new Matrix4f(rot);
		rotation.mul(lastTransform);
		return rotation;

	}

	private void applyTransform(final Matrix4f matrix, float mouseX, float mouseY, boolean isDragging) {
		matrix.mul(isDragging? getTransform(mouseX, mouseY) : lastTransform);
	}

	private void startDrag(float mouseX, float mouseY) {
		dragStart = calculateSpherePoint(mouseX, mouseY);
	}

	private void endDrag(float mouseX, float mouseY) {
		lastTransform = getTransform(mouseX, mouseY);
	}
}
