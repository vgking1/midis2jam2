/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.brass.*;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox;
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells;
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings;
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir;
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings;
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani;
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar;
import org.wysko.midis2jam2.instrument.family.guitar.Guitar;
import org.wysko.midis2jam2.instrument.family.organ.Accordion;
import org.wysko.midis2jam2.instrument.family.organ.Harmonica;
import org.wysko.midis2jam2.instrument.family.percussion.Percussion;
import org.wysko.midis2jam2.instrument.family.percussive.*;
import org.wysko.midis2jam2.instrument.family.piano.Keyboard;
import org.wysko.midis2jam2.instrument.family.pipe.*;
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax;
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax;
import org.wysko.midis2jam2.instrument.family.soundeffects.Gunshot;
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter;
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing;
import org.wysko.midis2jam2.instrument.family.strings.*;
import org.wysko.midis2jam2.midi.*;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.Camera.*;

public class Midis2jam2 extends AbstractAppState implements ActionListener {
	
	/**
	 * The list of instruments.
	 */
	public final List<Instrument> instruments = new ArrayList<>();
	
	/**
	 * The list of guitar shadows.
	 */
	final List<Spatial> guitarShadows = new ArrayList<>();
	
	private final Node rootNode = new Node("root");
	
	/**
	 * The list of bass guitar shadows.
	 */
	private final List<Spatial> bassGuitarShadows = new ArrayList<>();
	
	/**
	 * The list of harp shadows
	 */
	private final List<Spatial> harpShadows = new ArrayList<>();
	
	/**
	 * When true, midis2jam2 will load the default internal Java MIDI synthesizer, even if an external device is set.
	 */
	public boolean useDefaultSynthesizer = false;
	
	/**
	 * The MIDI file.
	 */
	public MidiFile file;
	
	/**
	 * 3D text for debugging.
	 */
	public BitmapText debugText;
	
	/**
	 * The bitmap font.
	 */
	public BitmapFont bitmapFont;
	
	public Sequence sequence;
	
	/**
	 * Video offset to account for synthesis audio delay.
	 */
	int latencyFix = 250;
	
	/**
	 * The MIDI sequencer.
	 */
	Sequencer sequencer;
	
	/**
	 * True if {@link #sequencer} has begun playing, false otherwise.
	 */
	boolean seqHasRunOnce = false;
	
	/**
	 * The current camera position.
	 */
	Camera currentCamera = CAMERA_1A;
	
	/**
	 * Incremental counter keeping track of how much time has elapsed (or remains until the MIDI begins playback) since
	 * the MIDI began playback
	 */
	double timeSinceStart = -2;
	
	private SimpleApplication app;
	
	boolean tpfHack = false;
	
	/**
	 * The piano stand.
	 */
	private Spatial pianoStand;
	
	/**
	 * The mallet stand.
	 */
	private Spatial malletStand;
	
	/**
	 * The keyboard shadow.
	 */
	private Spatial keyboardShadow;
	
	/**
	 * Converts an angle expressed in degrees to radians.
	 *
	 * @param deg the angle expressed in degrees
	 * @return the angle expressed in radians
	 */
	public static float rad(float deg) {
		return deg / 180 * FastMath.PI;
	}
	
	public Node getRootNode() {
		return rootNode;
	}
	
	public AssetManager getAssetManager() {
		return app.getAssetManager();
	}
	
	/**
	 * Converts an angle expressed in degrees to radians.
	 *
	 * @param deg the angle expressed in degrees
	 * @return the angle expressed in radians
	 */
	public static float rad(double deg) {
		return (float) (deg / 180 * FastMath.PI);
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (Launcher) app;
		
		this.app.getFlyByCamera().setMoveSpeed(100f);
		this.app.getFlyByCamera().setZoomSpeed(-10);
		this.app.getFlyByCamera().setEnabled(true);
		this.app.getFlyByCamera().setDragToRotate(true);
		
		setupKeys();
		setCamera(CAMERA_1A);
		
		
		Spatial stage = loadModel("Stage.obj", "Stage.bmp");
		
		rootNode.attachChild(stage);
		
		initDebugText();
		
		try {
			calculateInstruments();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		
		addShadowsAndStands();
		
		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (timeSinceStart + (latencyFix / 1000.0) >= 0 && !seqHasRunOnce && sequencer.isOpen()) {
					sequencer.setTempoInBPM((float) file.firstTempoInBpm());
					sequencer.start();
					seqHasRunOnce = true;
					new Timer(true).scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							// Find the first tempo we haven't hit and need to execute
							long currentMidiTick = sequencer.getTickPosition();
							for (MidiTempoEvent tempo : file.tempos) {
								if (tempo.time == currentMidiTick) {
									sequencer.setTempoInBPM(60_000_000f / tempo.number);
								}
							}
						}
					}, 0, 1);
				}
			}
		}, 0, 1);
		
		
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		if (tpf > 1 && !tpfHack) {
			/* TODO This is a hack. When the appstate inits, any time spent on the main screen is added to tpf
				on the first launch. */
			tpf = 0;
			tpfHack = true;
		}
		
		if (sequencer == null) return;
		if (sequencer.isOpen())
			timeSinceStart += tpf;
		
		for (Instrument instrument : instruments) {
			if (instrument != null) // Null if not implemented yet
				instrument.tick(timeSinceStart, tpf);
		}
		
		updateShadowsAndStands();
		
		if (timeSinceStart > (sequence.getMicrosecondLength() / 1E6) + 3) {
			Launcher.launcher.goBackToMainScreen();
		}
		
	}
	
	@Override
	public void cleanup() {
		System.out.println("CLEANUP!");
		sequencer.stop();
		sequencer.close();
	}
	
	private void showAll(Node rootNode) {
		for (Spatial child : rootNode.getChildren()) {
			child.setCullHint(Spatial.CullHint.Dynamic);
			if (child instanceof Node) {
				showAll((Node) child);
			}
		}
	}
	
	private void updateShadowsAndStands() {
		if (pianoStand != null)
			pianoStand.setCullHint(instruments.stream().filter(Objects::nonNull).anyMatch(i -> i.visible && i instanceof Keyboard) ?
					Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
		if (keyboardShadow != null) {
			keyboardShadow.setCullHint(instruments.stream().filter(Objects::nonNull).anyMatch(i -> i.visible && i instanceof Keyboard) ?
					Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
			keyboardShadow.setLocalScale(
					1,
					1,
					(int) instruments.stream().filter(k -> k instanceof Keyboard && k.visible).count());
		}
		if (malletStand != null)
			malletStand.setCullHint(instruments.stream().filter(Objects::nonNull).anyMatch(i -> i.visible && i instanceof Mallets) ?
					Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
		
		long guitarVisibleCount = instruments.stream().filter(instrument -> instrument instanceof Guitar && instrument.visible).count();
		for (int i = 0; i < guitarShadows.size(); i++) {
			if (i < guitarVisibleCount) guitarShadows.get(i).setCullHint(Spatial.CullHint.Dynamic);
			else guitarShadows.get(i).setCullHint(Spatial.CullHint.Always);
		}
		long bassGuitarVisibleCount =
				instruments.stream().filter(instrument -> instrument instanceof BassGuitar && instrument.visible).count();
		for (int i = 0; i < bassGuitarShadows.size(); i++) {
			if (i < bassGuitarVisibleCount) bassGuitarShadows.get(i).setCullHint(Spatial.CullHint.Dynamic);
			else bassGuitarShadows.get(i).setCullHint(Spatial.CullHint.Always);
		}
		
		long harpVisibleCount =
				instruments.stream().filter(instrument -> instrument instanceof Harp && instrument.visible).count();
		for (int i = 0; i < harpShadows.size(); i++) {
			if (i < harpVisibleCount) harpShadows.get(i).setCullHint(Spatial.CullHint.Dynamic);
			else harpShadows.get(i).setCullHint(Spatial.CullHint.Always);
		}
	}
	
	/**
	 * Reads the MIDI file and calculates program events, appropriately creating instances of each instrument and
	 * assigning the correct events to respective instruments.
	 */
	private void calculateInstruments() throws ReflectiveOperationException {
		
		List<ArrayList<MidiChannelSpecificEvent>> channels = new ArrayList<>();
		// Create 16 ArrayLists for each channel
		IntStream.range(0, 16).forEach(i -> channels.add(new ArrayList<>()));
		
		// For each track
		for (MidiTrack track : file.tracks) {
			if (track == null) continue;
			// Add important events
			for (MidiEvent event : track.events) {
				if (event instanceof MidiChannelSpecificEvent) {
					MidiChannelSpecificEvent channelEvent = (MidiChannelSpecificEvent) event;
					int channel = channelEvent.channel;
					channels.get(channel).add(channelEvent);
				}
			}
		}
		for (ArrayList<MidiChannelSpecificEvent> channelEvent : channels) {
			channelEvent.sort(MidiChannelSpecificEvent.COMPARE_BY_TIME);
		}
		for (int j = 0, channelsLength = channels.size(); j < channelsLength; j++) {
			ArrayList<MidiChannelSpecificEvent> channel = channels.get(j);
			if (j == 9) { // Percussion channel
				Percussion percussion = new Percussion(this, channel);
				instruments.add(percussion);
				continue;
			}
			/* Skip channels with no note-on events */
			boolean hasANoteOn = channel.stream().anyMatch(e -> e instanceof MidiNoteOnEvent);
			if (!hasANoteOn) continue;
			List<MidiProgramEvent> programEvents = new ArrayList<>();
			for (MidiChannelSpecificEvent channelEvent : channel) {
				if (channelEvent instanceof MidiProgramEvent) {
					programEvents.add(((MidiProgramEvent) channelEvent));
				}
			}
			
			if (programEvents.isEmpty()) { // It is possible for no program event, revert to instrument 0
				programEvents.add(new MidiProgramEvent(0, j, 0));
			}
			
			for (int i = 0; i < programEvents.size() - 1; i++) {
				final MidiProgramEvent a = programEvents.get(i);
				final MidiProgramEvent b = programEvents.get(i + 1);
				/* Remove program events at same time (keep the last one) */
				if (a.time == b.time) {
					programEvents.remove(i);
					i--;
					continue;
				}
				/* Remove program events with same value (keep the first one) */
				if (a.programNum == b.programNum) {
					programEvents.remove(i + 1);
				}
			}
			
			if (programEvents.size() == 1) {
				instruments.add(fromEvents(programEvents.get(0), channel));
			} else {
				for (int i = 0; i < programEvents.size() - 1; i++) {
					List<MidiChannelSpecificEvent> events = new ArrayList<>();
					for (MidiChannelSpecificEvent eventInChannel : channel) {
						if (eventInChannel.time < programEvents.get(i + 1).time) {
							if (i > 0) {
								if (eventInChannel.time >= programEvents.get(i).time) {
									events.add(eventInChannel);
								}
							} else {
								events.add(eventInChannel);
							}
						} else {
							break;
						}
					}
					instruments.add(fromEvents(programEvents.get(i), events));
				}
				List<MidiChannelSpecificEvent> lastInstrumentEvents = new ArrayList<>();
				MidiProgramEvent lastProgramEvent = programEvents.get(programEvents.size() - 1);
				for (MidiChannelSpecificEvent channelEvent : channel) {
					if (channelEvent.time >= lastProgramEvent.time) {
						lastInstrumentEvents.add(channelEvent);
					}
				}
				instruments.add(fromEvents(lastProgramEvent, lastInstrumentEvents));
			}
		}
	}
	
	/**
	 * Given a program event and list of events, returns a new instrument of the correct type containing the specified
	 * events. Follows the GM-1 standard.
	 *
	 * @param programEvent the program event, from which the program number is used
	 * @param events       the list of events to apply to this instrument
	 * @return a new instrument of the correct type containing the specified events
	 */
	@SuppressWarnings("SpellCheckingInspection")
	@Nullable
	private Instrument fromEvents(MidiProgramEvent programEvent,
	                              List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		return switch (programEvent.programNum) {
			// Acoustic Grand Piano
			// Bright Acoustic Piano
			// Electric Grand Piano
			// Honky-tonk Piano
			// Electric Piano 1
			// Electric Piano 2
			// Clavi
			case 0, 1, 2, 3, 4, 5, 7 -> (new Keyboard(this, events, Keyboard.KeyboardSkin.PIANO));
			// Harpsichord
			case 6 -> new Keyboard(this, events, Keyboard.KeyboardSkin.HARPSICHORD);
			// Celesta
			// Tubular Bells
			// FX 3 (Crystal)
			// Tinkle Bell
			case 8, 14, 98, 112 -> new TubularBells(this, events);
			// Glockenspiel
			case 9 -> new Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL);
			// Music Box
			case 10 -> new MusicBox(this, events);
			// Vibraphone
			case 11 -> new Mallets(this, events, Mallets.MalletType.VIBES);
			// Marimba
			case 12 -> new Mallets(this, events, Mallets.MalletType.MARIMBA);
			// Xylophone
			case 13 -> new Mallets(this, events, Mallets.MalletType.XYLOPHONE);
			// Dulcimer
			// Drawbar Organ
			// Percussive Organ
			// Rock Organ
			// Church Organ
			// Reed Organ
			// Orchestra Hit
			case 15, 16, 17, 18, 19, 20, 55 -> new Keyboard(this, events, Keyboard.KeyboardSkin.WOOD);
			// Accordion
			// Tango Accordion
			case 21, 23 -> new Accordion(this, events);
			// Harmonica
			case 22 -> new Harmonica(this, events);
			// Acoustic Guitar (Nylon)
			// Acoustic Guitar (Steel)
			case 24, 25 -> new Guitar(this, events, Guitar.GuitarType.ACOUSTIC);
			// Electric Guitar (jazz)
			// Electric Guitar (clean)
			// Electric Guitar (muted)
			// Overdriven Guitar
			// Distortion Guitar
			// Guitar Harmonics
			// Guitar Fret Noise
			case 26, 27, 28, 29, 30, 31, 120 -> new Guitar(this, events, Guitar.GuitarType.ELECTRIC);
			// Acoustic Bass
			case 32 -> new AcousticBass(this, events, AcousticBass.PlayingStyle.PIZZICATO);
			// Electric Bass (finger)
			// Electric Bass (pick)
			// Fretless Bass
			// Slap Bass 1
			// Slap Bass 2
			// Synth Bass 1
			// Synth Bass 2
			case 33, 34, 35, 36, 37, 38, 39 -> new BassGuitar(this, events);
			// Violin
			// Fiddle
			case 40, 110 -> new Violin(this, events);
			// Viola
			case 41 -> new Viola(this, events);
			// Cello
			case 42 -> new Cello(this, events);
			// Contrabass
			case 43 -> new AcousticBass(this, events, AcousticBass.PlayingStyle.ARCO);
			// Tremolo Strings
			// String Ensemble 1
			// String Ensemble 2
			// Synth Strings 1
			// Synth Strings 2
			// Pad 5 (Bowed)
			case 44, 48, 49, 50, 51, 92 -> new StageStrings(this, events);
			// Pizzicato Strings
			case 45 -> new PizzicatoStrings(this, events);
			// Orchestral Harp
			case 46 -> new Harp(this, events);
			// Timpani
			case 47 -> new Timpani(this, events);
			// Choir Aahs
			// Voice Oohs
			// Synth Voice
			// Lead 6 (Voice)
			// Pad 4 (Choir)
			// Breath Noise
			// Applause
			case 52, 53, 54, 85, 91, 121, 126 -> new StageChoir(this, events);
			// Trumpet
			case 56 -> new Trumpet(this, events, Trumpet.TrumpetType.NORMAL);
			// Trombone
			case 57 -> new Trombone(this, events);
			// Tuba
			case 58 -> new Tuba(this, events);
			// Muted Trumpet
			case 59 -> new Trumpet(this, events, Trumpet.TrumpetType.MUTED);
			// French Horn
			case 60 -> new FrenchHorn(this, events);
			// Brass Section
			// Synth Brass 1
			// Synth Brass 2
			case 61, 62, 63 -> new StageHorns(this, events);
			// Soprano Sax
			case 64 -> new SopranoSax(this, events);
			// Alto Sax
			case 65 -> new AltoSax(this, events);
			// Tenor Sax
			case 66 -> new TenorSax(this, events);
			// Baritone Sax
			case 67 -> new BaritoneSax(this, events);
			// Piccolo
			case 72 -> new Piccolo(this, events);
			// Flute
			case 73 -> new Flute(this, events);
			// Recorder
			case 74 -> new Recorder(this, events);
			// Pan Flute
			case 75 -> new PanFlute(this, events, PanFlute.PipeSkin.WOOD);
			// Blown Bottle
			case 76 -> new BlownBottle(this, events);
			// Whistle
			case 78 -> new Whistles(this, events);
			// Ocarina
			case 79 -> new Ocarina(this, events);
			// Lead 1 (Square)
			// Lead 2 (Sawtooth)
			// Lead 4 (Chiff)
			// Lead 5 (Charang)
			// Lead 7 (Fifths)
			// Lead 8 (Bass + Lead)
			// Pad 1 (New Age)
			// Pad 2 (Warm)
			// Pad 3 (Polysynth)
			// Pad 6 (Metallic)
			// Pad 7 (Halo)
			// Pad 8 (Sweep)
			// FX 1 (Rain)
			// FX 2 (Soundtrack)
			// FX 4 (Atmosphere)
			// FX 5 (Brightness)
			// FX 6 (Goblins)
			// FX 7 (Echoes)
			// FX 8 (Sci-fi)
			case 80, 81, 83, 84, 86, 87, 88, 89, 90, 93, 94, 95, 96, 97, 99, 100, 101, 102, 103 -> new Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH);
			// Lead 3 (Calliope)
			case 82 -> new PanFlute(this, events, PanFlute.PipeSkin.GOLD);
			// Agogo
			case 113 -> new Agogos(this, events);
			// Steel Drums
			case 114 -> new SteelDrums(this, events);
			// Woodblock
			case 115 -> new Woodblocks(this, events);
			// Taiko Drum
			case 116 -> new TaikoDrum(this, events);
			// Melodic Tom
			case 117 -> new MelodicTom(this, events);
			// Synth Drum
			case 118 -> new SynthDrum(this, events);
			// Telephone Ring
			case 124 -> new TelephoneRing(this, events);
			// Helicopter
			case 125 -> new Helicopter(this, events);
			// Gunshot
			case 127 -> new Gunshot(this, events);
			default -> null;
		};
	}
	
	private void initDebugText() {
		bitmapFont = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
		debugText = new BitmapText(bitmapFont, false);
		debugText.setSize(bitmapFont.getCharSet().getRenderedSize());
		debugText.setText("");
		debugText.setLocalTranslation(300, debugText.getLineHeight(), 0);
		this.app.getGuiNode().attachChild(debugText);
	}
	
	private void addShadowsAndStands() {
		if (instruments.stream().anyMatch(i -> i instanceof Keyboard)) {
			pianoStand = loadModel("PianoStand.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(pianoStand);
			pianoStand.move(-50, 32f, -6);
			pianoStand.rotate(0, rad(45), 0);
			
			keyboardShadow = shadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png");
			keyboardShadow.move(-47, 0.1f, -3);
			keyboardShadow.rotate(0, rad(45), 0);
			rootNode.attachChild(keyboardShadow);
		}
		if (instruments.stream().anyMatch(i -> i instanceof Mallets)) {
			malletStand = loadModel("XylophoneLegs.obj", "RubberFoot.bmp", MatType.UNSHADED, 0.9f);
			rootNode.attachChild(malletStand);
			malletStand.setLocalTranslation(new Vector3f(-22, 22.2f, 23));
			malletStand.rotate(0, rad(33.7), 0);
			malletStand.scale(2 / 3f);
		}
		
		// Add guitar shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof Guitar).count(); i++) {
			Spatial shadow = shadow("Assets/GuitarShadow.obj", "Assets/GuitarShadow.png");
			guitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(43.431f + (10 * i), 0.1f + (0.01f * i), 7.063f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-49), 0));
		}
		
		// Add bass guitar shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof BassGuitar).count(); i++) {
			Spatial shadow = shadow("Assets/BassShadow.obj", "Assets/BassShadow.png");
			bassGuitarShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(51.5863f + 7 * i, 0.1f + (0.01f * i), -16.5817f);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-43.5), 0));
		}
		
		// Add harp shadows
		for (long i = 0; i < instruments.stream().filter(instrument -> instrument instanceof Harp).count(); i++) {
			Spatial shadow = shadow("Assets/HarpShadow.obj", "Assets/HarpShadow.png");
			harpShadows.add(shadow);
			rootNode.attachChild(shadow);
			shadow.setLocalTranslation(5 + 14.7f * i, 0.1f, 17 + 10.3f * i);
			shadow.setLocalRotation(new Quaternion().fromAngles(0, rad(-35), 0));
		}
		
		// Add mallet shadows
		List<Instrument> mallets = instruments.stream().filter(instrument -> instrument instanceof Mallets).collect(Collectors.toList());
		for (int i = 0; i < instruments.stream().filter(instrument -> instrument instanceof Mallets).count(); i++) {
			Spatial shadow = shadow("Assets/XylophoneShadow.obj", "Assets/XylophoneShadow.png");
			shadow.setLocalScale(0.6667f);
			mallets.get(i).instrumentNode.attachChild(shadow);
			shadow.setLocalTranslation(0, -22, 0);
		}
	}
	
	@Contract(pure = true)
	public Spatial shadow(String model, String texture) {
		Spatial shadow = this.app.getAssetManager().loadModel(model);
		final Material material = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", this.app.getAssetManager().loadTexture(texture));
		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		material.setFloat("AlphaDiscardThreshold", 0.01f);
		shadow.setQueueBucket(RenderQueue.Bucket.Transparent);
		shadow.setMaterial(material);
		return shadow;
	}
	
	/**
	 * Loads a model given a model and texture paths. Applies unshaded material.
	 *
	 * @param m the path to the model
	 * @param t the path to the texture
	 * @return the model
	 */
	public Spatial loadModel(String m, String t) {
		return loadModel(m, t, MatType.UNSHADED, 0);
	}
	
	/**
	 * Loads a model given a model and texture paths.
	 *
	 * @param m          the path to the model
	 * @param t          the path to the texture
	 * @param type       the type of material
	 * @param brightness the brightness of the reflection
	 * @return the model
	 */
	public Spatial loadModel(String m, String t, MatType type, float brightness) {
		Spatial model = this.app.getAssetManager().loadModel("Assets/" + m);
		Texture texture = this.app.getAssetManager().loadTexture("Assets/" + t);
		Material material;
		switch (type) {
			case UNSHADED -> {
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				material.setTexture("ColorMap", texture);
			}
			case SHADED -> {
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
				material.setTexture("DiffuseMap", texture);
			}
			case REFLECTIVE -> {
				material = new Material(this.app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
				material.setVector3("FresnelParams", new Vector3f(0.1f, brightness, 0.1f));
				material.setBoolean("EnvMapAsSphereMap", true);
				material.setTexture("EnvMap", texture);
			}
			default -> throw new IllegalStateException("Unexpected value: " + type);
		}
		model.setMaterial(material);
		return model;
	}
	
	public Material reflectiveMaterial(String reflectiveTextureFile) {
		Material material = new Material(this.app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		material.setVector3("FresnelParams", new Vector3f(0.1f, 0.9f, 0.1f));
		material.setBoolean("EnvMapAsSphereMap", true);
		material.setTexture("EnvMap", this.app.getAssetManager().loadTexture(reflectiveTextureFile));
		return material;
	}
	
	/**
	 * Registers key handling.
	 */
	private void setupKeys() {
		this.app.getInputManager().deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		this.app.getInputManager().addMapping("cam1", new KeyTrigger(KeyInput.KEY_1));
		this.app.getInputManager().addListener(this, "cam1");
		
		this.app.getInputManager().addMapping("cam2", new KeyTrigger(KeyInput.KEY_2));
		this.app.getInputManager().addListener(this, "cam2");
		
		this.app.getInputManager().addMapping("cam3", new KeyTrigger(KeyInput.KEY_3));
		this.app.getInputManager().addListener(this, "cam3");
		
		this.app.getInputManager().addMapping("cam4", new KeyTrigger(KeyInput.KEY_4));
		this.app.getInputManager().addListener(this, "cam4");
		
		this.app.getInputManager().addMapping("cam5", new KeyTrigger(KeyInput.KEY_5));
		this.app.getInputManager().addListener(this, "cam5");
		
		this.app.getInputManager().addMapping("cam6", new KeyTrigger(KeyInput.KEY_6));
		this.app.getInputManager().addListener(this, "cam6");
		
		this.app.getInputManager().addMapping("slow", new KeyTrigger(KeyInput.KEY_LCONTROL));
		this.app.getInputManager().addListener(this, "slow");
		
		this.app.getInputManager().addMapping("freeCam", new KeyTrigger(KeyInput.KEY_GRAVE));
		this.app.getInputManager().addListener(this, "freeCam");
		
		this.app.getInputManager().addMapping("exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
		this.app.getInputManager().addListener(this, "exit");
	}
	
	/**
	 * Sets the camera position, given a {@link Camera}.
	 *
	 * @param camera the camera to apply
	 */
	private void setCamera(Camera camera) {
		this.app.getCamera().setLocation(camera.location);
		this.app.getCamera().setRotation(camera.rotation);
	}
	
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		this.app.getFlyByCamera().setMoveSpeed(name.equals("slow") && isPressed ? 10 : 100);
		if (name.equals("exit")) {
			if (sequencer.isOpen())
				sequencer.stop();
			Launcher.launcher.goBackToMainScreen();
		}
		if (isPressed && name.startsWith("cam")) {
			try {
				switch (name) {
					case "cam1":
						if (currentCamera == CAMERA_1A) {
							currentCamera = CAMERA_1B;
						} else if (currentCamera == CAMERA_1B) {
							currentCamera = CAMERA_1C;
						} else {
							currentCamera = CAMERA_1A;
						}
						break;
					case "cam2":
						currentCamera = currentCamera == CAMERA_2A ? CAMERA_2B : CAMERA_2A;
						break;
					case "cam3":
						currentCamera = currentCamera == CAMERA_3A ? CAMERA_3B : CAMERA_3A;
						break;
					case "cam4":
						currentCamera = currentCamera == CAMERA_4A ? CAMERA_4B : CAMERA_4A;
						break;
					case "cam5":
						currentCamera = CAMERA_5;
						break;
					case "cam6":
						currentCamera = CAMERA_6;
						break;
				}
				Camera camera = valueOf(currentCamera.name());
				setCamera(camera);
			} catch (IllegalArgumentException ignored) {
			}
		}
	}
	
	public enum MatType {
		UNSHADED,
		SHADED,
		REFLECTIVE
	}
	
	/**
	 * Defines angles for cameras.
	 */
	enum Camera {
		CAMERA_1A(-2, 92, 134, rad(90 - 71.56f), rad(180), 0),
		CAMERA_1B(60, 92, 124, rad(90 - 71.5), rad(180 + 24.4), 0),
		CAMERA_1C(-59.5f, 90.8f, 94.4f, rad(90 - 66.1), rad(180 - 26.4), 0),
		CAMERA_2A(0, 71.8f, 44.5f, rad(90 - 74.3), rad(180 + 44.9), 0),
		CAMERA_2B(-35, 76.4f, 33.6f, rad(90 - 34.2), rad(180 + 18.5), 0),
		CAMERA_3A(-0.2f, 61.6f, 38.6f, rad(90 - 74.5), rad(180), 0),
		CAMERA_3B(-19.6f, 78.7f, 3.8f, rad(90 - 62.3), rad(180 - 16.2), 0),
		CAMERA_4A(0.2f, 81.1f, 32.2f, rad(90 - 69), rad(180 - 48.2), rad(-0.5)),
		CAMERA_4B(35, 25.4f, -19, rad(90 - 140), rad(180 - 61), rad(-2.5)),
		CAMERA_5(5, 432, 24, rad(90 - 7.125f), rad(180), 0),
		CAMERA_6(17, 30.5f, 42.9f, rad(90 - 96.7), rad(180 - 35.7), 0);
		
		final Vector3f location;
		
		final Quaternion rotation;
		
		Camera(float locX, float locY, float locZ, float rotX, float rotY, float rotZ) {
			location = new Vector3f(locX, locY, locZ);
			rotation = new Quaternion().fromAngles(rotX, rotY, rotZ);
		}
	}
}
