package org.wysko.midis2jam2.instrument.percussive;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public abstract class StickDrum extends Drum {
	static final float RECOIL_DISTANCE = -2f;
	final static double MAX_ANGLE = 50;
	final Node highLevelNode = new Node();
	/**
	 * Attach {@link #drum} and {@link #stick} to this and move this for recoil.
	 */
	final Node recoilNode = new Node();
	final Node stickNode = new Node();
	Spatial drum;
	Spatial stick;
	
	protected StickDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
	}
	
	void drumRecoil(double time, float delta) {
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		if (recoil != null) {
			recoilNode.setLocalTranslation(0, (float) (velocityRecoilDampening(recoil.velocity) * StickDrum.RECOIL_DISTANCE), 0);
		} else {
			Vector3f localTranslation = recoilNode.getLocalTranslation();
			if (localTranslation.y < -0.0001) {
				recoilNode.setLocalTranslation(0, Math.min(0, localTranslation.y + (Drum.DRUM_RECOIL_COMEBACK * delta)), 0);
			} else {
				recoilNode.setLocalTranslation(0, 0, 0);
			}
		}
	}
	
	void handleStick(double time, float delta) {
		MidiNoteOnEvent nextHit = null;
		
		if (!hits.isEmpty())
			nextHit = hits.get(0);
		
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time)
			nextHit = hits.remove(0);
		
		double proposedRotation = nextHit == null ? MAX_ANGLE : -1000 * ((6E7 / context.file.tempoBefore(nextHit).number) / 500) * (time - context.file.eventInSeconds(nextHit));
		
		float[] floats = stick.getLocalRotation().toAngles(new float[3]);
		
		if (proposedRotation > MAX_ANGLE) {
			// Not yet ready to strike
			if (floats[0] < MAX_ANGLE) {
				// We have come down, need to recoil
				float xAngle = floats[0] + 5f * delta;
				xAngle = Math.min(rad((float) MAX_ANGLE), xAngle);
				stick.setLocalRotation(new Quaternion().fromAngles(
						xAngle, 0, 0
				));
			}
		} else {
			// Striking
			stick.setLocalRotation(new Quaternion().fromAngles(rad((float) (
					Math.min(MAX_ANGLE, proposedRotation)
			)), 0, 0));
		}
		
		float[] finalAngles = stick.getLocalRotation().toAngles(new float[3]);
		
		if (finalAngles[0] > rad((float) MAX_ANGLE)) {
			// Not yet ready to strike
			stick.setCullHint(Spatial.CullHint.Always);
		} else {
			// Striking or recoiling
			stick.setCullHint(Spatial.CullHint.Dynamic);
		}
		
		
	}
}