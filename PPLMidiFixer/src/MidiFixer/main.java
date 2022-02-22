package MidiFixer;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class main {
	
	/*
	public static int microSecsToBpm(byte[] data) {
		
		int int0 = data[0] & 0x00ff;
	    int int1 = data[1] & 0x00ff;
	    int int2 = data[2] & 0x00ff;

	    long t = int0 << 16;
	    t += int1 << 8;
	    t += int2;

	    return (int)(60000000 / t);
	}
	
	public static int pitchBendToInt(int d1, int d2) {
		
		return d1 + (d2 << 7);
	}
	*/
	// CREATE EVENTS -----------------------------------------
	public static MidiEvent createBPMEvent(long ticks, MidiMessage mm) {
		
		return new MidiEvent(mm, ticks);
	}
	
	public static MidiEvent createBankEvent(long ticks) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		sm.setMessage(ShortMessage.CONTROL_CHANGE,0,0);
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createLSBEvent(long ticks, int lsb) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		sm.setMessage(ShortMessage.CONTROL_CHANGE,32,lsb);
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createPatchEvent(long ticks, int patch) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		sm.setMessage(ShortMessage.PROGRAM_CHANGE,patch,0);
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createPanEvent(long ticks, int pan) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		sm.setMessage(ShortMessage.CONTROL_CHANGE,10,pan);
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createNoteOnEvent(long ticks, int key, int velocity) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		sm.setMessage(ShortMessage.NOTE_ON,key,velocity);
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createNoteOffEvent(long ticks, int key, int velocity) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		sm.setMessage(ShortMessage.NOTE_OFF,key,velocity);
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createPitchBendEvent(long ticks, ShortMessage sm) throws InvalidMidiDataException {
		
		return new MidiEvent(sm, ticks);
	}
	
	public static MidiEvent createTrackNameEvent(long ticks, String name) throws InvalidMidiDataException {
		MetaMessage mt = new MetaMessage();
		mt.setMessage(0x03 ,name.getBytes(), name.length());
		
		return new MidiEvent(mt, ticks);
	}
	
	public static MidiEvent createEndTrackEvent(long ticks) throws InvalidMidiDataException {
		MetaMessage mt = new MetaMessage();
		byte[] bet = {}; // empty array
		mt.setMessage(0x2F,bet,0);
		
		return new MidiEvent(mt, ticks);
	}
	
	public static void cleanSequence(Sequence seq) {
		Track[] trackArray = seq.getTracks();
		Track currentTrack;
		for (int i = 0; i < trackArray.length; i++) {
			
			currentTrack = trackArray[i];
			
			if (currentTrack.size() == 3) { //if the track it's empty
				seq.deleteTrack(currentTrack);
			}
			//nothing
		}
	}
	
	/*
	public static void assignMidiChannels(Sequence seq1, Sequence seq2) {
		Track[] trackArray = seq1.getTracks();
		Track currentTrack;
		for (int i = 0; i < trackArray.length; i++) {
			
			currentTrack = trackArray[i];
			
			for(int j = 0; j < currentTrack.size(); j++) {
				
				
			}
		}
	}
	*/


	public static void main(String[] args) throws InvalidMidiDataException, IOException {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		final String filePATH = args[0];
		final String xml = args[1];
		
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			//Files
			File midiFile = new File(filePATH);
			File xmlFile = new File(xml);
			
			Sequence midiSeq = MidiSystem.getSequence(midiFile);
			
			Document doc = db.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			//create new midi sequence
			Sequence convertedMidiSeq = new Sequence(javax.sound.midi.Sequence.PPQ,48);
			
			
			//List all <instrument>
			NodeList instrumentList = doc.getElementsByTagName("instrument");
			
			for (int j = 0; j < instrumentList.getLength(); j++) {
				
				//get the tempo of the midi track
				 if (j == 0) { //if is the first time we check the midi file
						//get the tempo of the miditrack
						Track[] trackArray = midiSeq.getTracks();
						
						//creates track cero
						Track cero = convertedMidiSeq.createTrack();
						
						MidiEvent tempoEvent = trackArray[0].get(0); //get the midi event of track 0
						cero.add(tempoEvent); //copy the tempo event
						cero.add(createEndTrackEvent(0)); //creates end track event
				}
				
				Node currentItem = instrumentList.item(j);
				
				String midiTrackName = currentItem.getAttributes().getNamedItem("name").getNodeValue();
				String midiTrackPatch = currentItem.getAttributes().getNamedItem("number").getNodeValue();
				
				Track convertedTrack = convertedMidiSeq.createTrack();
				
				//insert name and patch number to the track
				convertedTrack.add(createTrackNameEvent(0,midiTrackName)); //name
				//convertedTrack.add(createBankEvent(0));
				//convertedTrack.add(createLSBEvent(0,0));
				convertedTrack.add(createPatchEvent(0,Integer.parseInt(midiTrackPatch))); //patch
				
				//we get into the <sample> nodes
				Element sampleElement = (Element) currentItem;
				NodeList sampleList = sampleElement.getElementsByTagName("sample");
				
				//first we record the <sample> nodes data affiliated with the current <instrument>
				int sampleLSB[] = new int[sampleList.getLength()];
				int samplePatch[] = new int[sampleList.getLength()];
				int sampleTranspose[] = new int[sampleList.getLength()];
				
				for (int m = 0; m < sampleList.getLength(); m++) {
					 
					 //selects the <sample> node
					 Node sampleNode = sampleElement.getElementsByTagName("sample").item(m);
					 
					 String dataLSB = sampleNode.getAttributes().getNamedItem("LSB").getNodeValue();
					 String dataPatch = sampleNode.getAttributes().getNamedItem("patch").getNodeValue();
					 String dataTranspose = sampleNode.getAttributes().getNamedItem("transpose").getNodeValue();
					 
					 sampleLSB[m] = Integer.parseInt(dataLSB);
					 samplePatch[m] = Integer.parseInt(dataPatch);
					 sampleTranspose[m] = Integer.parseInt(dataTranspose);
				}
				 
				//now time to get the midi events
				
				for (int k = 0; k < sampleList.getLength(); k++) {
					 
					 //starts to read the midi file
					 int trackNumber = 0;
					 boolean matchLSB = false;
					 boolean matchPatch = false;
					 
					 	//checks every track from the midi file if it match with the xml data
						for (Track midiTrack : midiSeq.getTracks()) {
							
							
							if (trackNumber == 0) { 
								//nothing
							}
							
							trackNumber++; //skips track 0
							
							//check the current midi track
							for(int i = 0; i < midiTrack.size(); i++) {
								
								MidiEvent currentEvent = midiTrack.get(i);
								MidiMessage currentMessage = currentEvent.getMessage();
								
								if (currentMessage instanceof ShortMessage) {
									ShortMessage currentShort = (ShortMessage) currentMessage;
									int command = currentShort.getCommand();
									
									switch (command) {
										case ShortMessage.CONTROL_CHANGE:
											switch (currentShort.getData1()) {
											case 0: //Bank Select
												break;
											case 32: //Bank Select LSB
												//compares if is a match
												if (sampleLSB[k] == currentShort.getData2()) {
													matchLSB = true;
												} else {
													matchLSB = false;
												}
												break;
											case 10: //PAN
												if (matchLSB && matchPatch) { //if both are true
													//WRITE MIDI EVENT
													convertedTrack.add(createPanEvent(currentEvent.getTick(),currentShort.getData2())); //insert tick and value
												} else {
													//IGNORE
												}
												break;
											}
											break;
										case ShortMessage.PROGRAM_CHANGE:
											//compares if is a match
											if (samplePatch[k] == currentShort.getData1()) {
												matchPatch = true;
											} else {
												matchPatch = false;
											}
											break;
											
										case ShortMessage.PITCH_BEND:
											if (matchLSB && matchPatch) { //if both are true
												//WRITE MIDI EVENT
												convertedTrack.add(createPitchBendEvent(currentEvent.getTick(),(ShortMessage) currentShort)); //insert tick and value
											} else {
												//IGNORE
											}
											break;
											
										case ShortMessage.NOTE_ON:
											if (matchLSB && matchPatch) { //if both are true
												//WRITE MIDI EVENT
												convertedTrack.add(createNoteOnEvent(currentEvent.getTick(), currentShort.getData1() + sampleTranspose[k] ,currentShort.getData2()));
											} else {
												//IGNORE
											}
											break;
										case ShortMessage.NOTE_OFF:
											if (matchLSB && matchPatch) { //if both are true
												//WRITE MIDI EVENT
												convertedTrack.add(createNoteOffEvent(currentEvent.getTick(), currentShort.getData1() + sampleTranspose[k] ,currentShort.getData2()));
											} else {
												//IGNORE
											}
											break;
									}
								}
							}
						}
					 
				 }
				//get the size of the track and add the END track message
				long finalTick = convertedTrack.ticks();
				convertedTrack.add(createEndTrackEvent(finalTick));
			}
			//clean the sequence
			cleanSequence(convertedMidiSeq);
			
			File convertedFile = new File(midiFile + ".fix.mid");
			MidiSystem.write(convertedMidiSeq, 1, convertedFile);
			System.out.println(convertedFile);
			
		} catch (ParserConfigurationException | SAXException | IOException e) { 
			System.out.println("an error just ocurred: " + e); e.printStackTrace();
		}
		
		
		
		
	}

}
