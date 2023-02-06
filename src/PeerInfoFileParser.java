import java.util.*;
import java.io.*;

public class PeerInfoFileParser {
	private final ArrayList<String> peerCollection;
	private final HashMap<String, NeighborPeerInfo> HashMapOfPeerInfo;

	public PeerInfoFileParser(){
		this.HashMapOfPeerInfo = new HashMap<>();
		this.peerCollection = new ArrayList<>();
	}

	public HashMap<String, NeighborPeerInfo> getHashMapOfPeerInfo(){
		return this.HashMapOfPeerInfo;
	}

	public ArrayList<String> getPeerCollection(){
		return this.peerCollection;
	}

	public void parsePeerInfoConfigFile()
	{
		try {
			var inputBufferedReader = new BufferedReader(new FileReader("PeerInfo.cfg"));
			String line;
			while((line = inputBufferedReader.readLine()) != null) {
				var tokens = line.split("\\s+");
				this.HashMapOfPeerInfo.put(tokens[0],new NeighborPeerInfo(tokens[0], tokens[1], tokens[2], tokens[3]));
				this.peerCollection.add(tokens[0]);
			}
			inputBufferedReader.close();
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
	}

	public NeighborPeerInfo extractPeer(String peerIndex){
		return this.HashMapOfPeerInfo.get(peerIndex);
	}
}

class NeighborPeerInfo {
	public int portNumberOfPeer;
	public int filePresent;
	public String peerIndex;
	public String peerLocation;

	public NeighborPeerInfo(String peerIndex, String peerLocation, String portNumberOfPeer, String filePresent) {
		this.peerIndex = peerIndex;
		this.peerLocation = peerLocation;
		this.portNumberOfPeer = Integer.parseInt(portNumberOfPeer);
		this.filePresent = Integer.parseInt(filePresent);
	}

}