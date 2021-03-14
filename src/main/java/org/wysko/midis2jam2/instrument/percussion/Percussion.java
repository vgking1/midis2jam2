package org.wysko.midis2jam2.instrument.percussion;

import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

public class Percussion extends Instrument {
	
	public final SnareDrum snareDrum;
	public final BassDrum bassDrum;
	public final Tom tom1;
	public final Tom tom2;
	public final Tom tom3;
	public final Tom tom4;
	public final Tom tom5;
	public final Tom tom6;
	public final Cymbal crash1;
	public final Cymbal splash;
	public final Node drumSetNode = new Node();
	public final Node percussionNode = new Node();
	public final HiHat hiHat;
	private final Cymbal crash2;
//	private final Spatial shadow;
	
	public Percussion(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context);
		/* Percussion only cares about note on. */
		List<MidiNoteOnEvent> noteOnEvents = events.stream()
				.filter(e -> e instanceof MidiNoteOnEvent)
				.map(e -> ((MidiNoteOnEvent) e))
				.collect(Collectors.toList());
		
		
		snareDrum = new SnareDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 40 || e.note == 38).collect(Collectors.toList()));
		
		bassDrum = new BassDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 35 || e.note == 36).collect(Collectors.toList()));
		
		tom1 = new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 41).collect(Collectors.toList()), Tom.TomPitch.LOW_FLOOR);
		
		tom2 = new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 43).collect(Collectors.toList()), Tom.TomPitch.HIGH_FLOOR);
		
		tom3 = new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 45).collect(Collectors.toList()), Tom.TomPitch.LOW);
		
		tom4 = new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 47).collect(Collectors.toList()), Tom.TomPitch.LOW_MID);
		
		tom5 = new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 48).collect(Collectors.toList()), Tom.TomPitch.HIGH_MID);
		
		tom6 = new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 50).collect(Collectors.toList()), Tom.TomPitch.HIGH);
		
		crash1 = new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 49).collect(Collectors.toList()), Cymbal.CymbalType.CRASH_1);
		
		crash2 = new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 57).collect(Collectors.toList()),
				Cymbal.CymbalType.CRASH_2);
		
		splash = new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 55).collect(Collectors.toList()), Cymbal.CymbalType.SPLASH);
		
		hiHat = new HiHat(context,
				noteOnEvents.stream().filter(e -> e.note == 42 || e.note == 44 || e.note == 46).collect(Collectors.toList()));
		
		// Attach nodes to group node
		drumSetNode.attachChild(snareDrum.highLevelNode);
		drumSetNode.attachChild(bassDrum.highLevelNode);
		drumSetNode.attachChild(tom1.highLevelNode);
		drumSetNode.attachChild(tom2.highLevelNode);
		drumSetNode.attachChild(tom3.highLevelNode);
		drumSetNode.attachChild(tom4.highLevelNode);
		drumSetNode.attachChild(tom5.highLevelNode);
		drumSetNode.attachChild(tom6.highLevelNode);
		drumSetNode.attachChild(crash1.highLevelNode);
		drumSetNode.attachChild(crash2.highLevelNode);
		drumSetNode.attachChild(splash.highLevelNode);
		drumSetNode.attachChild(hiHat.highLevelNode);
//		shadow = context.loadModel("DrumShadow.obj","DrumShadow.bmp");
////		final Material material = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
////		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
////		shadow.setQueueBucket(RenderQueue.Bucket.Transparent);
////		shadow.setMaterial(material);
////		shadow.move(0,0.001f,-80);
		
		percussionNode.attachChild(drumSetNode);
//		percussionNode.attachChild(shadow);
		context.getRootNode().attachChild(percussionNode);
		
		
	}
	
	@Override
	public void tick(double time, float delta) {
		snareDrum.tick(time, delta);
		bassDrum.tick(time, delta);
		tom1.tick(time, delta);
		tom2.tick(time, delta);
		tom3.tick(time, delta);
		tom4.tick(time, delta);
		tom5.tick(time, delta);
		tom6.tick(time, delta);
		crash1.tick(time, delta);
		crash2.tick(time, delta);
		splash.tick(time, delta);
		hiHat.tick(time,delta);
	}
}
