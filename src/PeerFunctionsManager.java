import java.io.*;
import java.lang.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toMap;

public class PeerFunctionsManager {
	private final String pID;
	private NeighborPeerInfo neighborPeerInfo;
	private HashMap<String, NeighborPeerInfo> stringNeighborPeerInfoHashMap;
	private ArrayList<String> peerArraylist;
	private final HashMap<String, PeerMessagePassing> connectedPeers;
	private final HashMap<String, Thread> connectedThreads;
	private volatile ServerSocket serverSocket;
	private final CommonConfigFileParser commonConfigFileParser;
	private final PeerInfoFileParser peerInfoFileParser;
	private final Logging logging;
	private final HashMap<String, BitSet> availablePieces;
	private volatile String[] reqPeerInfo;
	private volatile HashSet<String> peerUnchokList;
	private final HashSet<String> peerInterestedList;
	private volatile String optimizedUnchokedPeer;
	private int chunkcnt;
	private volatile RandomAccessFile randomAccessFile;
	private final Choking choking;
	private final Unchocking unchocking;
	private final EndProcess endProcess;
	private Thread thread;
	private volatile Boolean finished;

	public PeerFunctionsManager(String pID) {
		this.pID = pID;
		this.stringNeighborPeerInfoHashMap = new HashMap<>();
		this.availablePieces = new HashMap<>();
		this.peerUnchokList = new HashSet<>();
		this.peerInterestedList = new HashSet<>();
		this.connectedThreads = new HashMap<>();
		this.commonConfigFileParser = new CommonConfigFileParser();
		this.peerInfoFileParser = new PeerInfoFileParser();

		this.logging = new Logging(this.pID);
		this.peerArraylist = new ArrayList<>();
		this.connectedPeers = new HashMap<>();
		this.finished = false;
		HashMap<String, Integer> dwnRateHashamap = new HashMap<>();
		this.endProcess = new EndProcess(this);

		this.startPeerInitialization();
		this.choking = new Choking(this);

		this.choking.startExecution();
		this.unchocking = new Unchocking(this);

		this.unchocking.startExecution();
	}

	public void startPeerInitialization() {
		try {
			this.commonConfigFileParser.parseLoadCommonConfig();
			this.peerInfoFileParser.parsePeerInfoConfigFile();


			this.chunkcnt = this.computeChunkCnt();
			this.reqPeerInfo = new String[this.chunkcnt];
			this.neighborPeerInfo = this.peerInfoFileParser.extractPeer(this.pID);
			this.stringNeighborPeerInfoHashMap = this.peerInfoFileParser.getHashMapOfPeerInfo();
			this.peerArraylist = this.peerInfoFileParser.getPeerCollection();
			//make directory for the peer
			var folderName = "peer_" + this.pID;
			var dirName = new File(folderName);
			dirName.mkdir();
			var fName = folderName + "/" + fetchFName();
			dirName = new File(fName);
			if (!isFilePresent()) {
				dirName.createNewFile();
			}
			this.randomAccessFile = new RandomAccessFile(dirName, "rw");
			if (!isFilePresent()) {
				this.randomAccessFile.setLength(this.fetchFSize());
			}

			//initialize the piece availability
			for (String pid : this.stringNeighborPeerInfoHashMap.keySet()) {
				var bitSet = new BitSet(this.chunkcnt);
				if (this.stringNeighborPeerInfoHashMap.get(pid).filePresent == 1) {
					bitSet.set(0, this.chunkcnt);
					this.availablePieces.put(pid, bitSet);
				}
				else {
					bitSet.clear();
					this.availablePieces.put(pid, bitSet);
				}
			}

			// Start the server for each peer
			this.serverSocket = new ServerSocket(this.neighborPeerInfo.portNumberOfPeer);
			var peerInitiation = new PeerInitiation(this.serverSocket, this);
			this.thread = new Thread(peerInitiation);
			this.thread.start();
			System.out.println("Peer " + this.pID + " Started");


			//create Neighbour connections
			Thread.sleep(5000);
			for (String pid : this.peerArraylist) {
				if (pid.equals(this.pID)) {
					break;
				}
				else {
					var peerInfo = this.stringNeighborPeerInfoHashMap.get(pid);
					var socket = new Socket(peerInfo.peerLocation, peerInfo.portNumberOfPeer);
					var peerMessagePassing = new PeerMessagePassing(socket, this);
					peerMessagePassing.setLastPeerID(pid);
					this.insertConnectedPeers(peerMessagePassing, pid);
					var newThread = new Thread(peerMessagePassing);
					this.insertConnectedThreads(pid, newThread);
					newThread.start();
					System.out.println("Connection started with Peer " + pid);
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void writeContentToFile(byte[] content, int i) {
		try {
			var pidx = this.fetchChunkSize() * i;
			this.randomAccessFile.seek(pidx);
			this.randomAccessFile.write(content);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] readContentofFile(int i) {
		try {
			var pidx = this.fetchChunkSize() * i;
			var pieceSize = this.fetchChunkSize();
			if (i == getChunkcnt() - 1) {
				pieceSize = this.fetchFSize() % this.fetchChunkSize();
			}
			this.randomAccessFile.seek(pidx);
			var bytes = new byte[pieceSize];
			this.randomAccessFile.read(bytes);
			return bytes;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

	public HashMap<String, Integer> fetchDwnRates() {
		var dwnRateHashMap = new HashMap<String, Integer>();
		for (var s : this.connectedPeers.keySet()) {
			dwnRateHashMap.put(s, this.connectedPeers.get(s).getDwnRate());
		}
		return dwnRateHashMap;
	}

	public synchronized void sendHaveToAll(int i) {
		for (var s : this.connectedPeers.keySet()) {
			this.connectedPeers.get(s).writeHaveMsg(i);
		}
	}

	public synchronized void modifyChunkStatus(String pID, int i) {
		this.availablePieces.get(pID).set(i);
	}

	public synchronized void modifyBits(String pID, BitSet bitSet) {
		this.availablePieces.remove(pID);
		this.availablePieces.put(pID, bitSet);
	}

	public synchronized void insertConnectedPeers(PeerMessagePassing peerMessagePassing, String s) {
		this.connectedPeers.put(s, peerMessagePassing);
	}

	public synchronized void insertConnectedThreads(String s, Thread thread) {
		this.connectedThreads.put(s, thread);
	}

	public PeerMessagePassing fetchPeerManagerInfo(String s) {
		return this.connectedPeers.get(s);
	}

	public BitSet findAvailableChunk(String s) {
		return this.availablePieces.get(s);
	}

	public synchronized boolean isPeerInterested(String s) {
		var availableChunk = this.findAvailableChunk(s);
		var bitSet = this.findAvailableChunk(this.pID);
		int i = 0;
		while (i < availableChunk.size() && i < this.chunkcnt) {
			if (availableChunk.get(i) && !bitSet.get(i)) {
				return true;
			}
			i++;
		}
		return false;
	}

	public synchronized void getPeerInfo(int index, String pID) {
		this.reqPeerInfo[index] = pID;
	}

	public synchronized int getRequestedChunk(String pID) {
		var bitSet = this.findAvailableChunk(pID);
		var availableChunk = this.findAvailableChunk(this.pID);
		int i = 0;
		while (i < bitSet.size() && i < this.chunkcnt) {
			if (this.reqPeerInfo[i] == null && bitSet.get(i) && !availableChunk.get(i)) {
				getPeerInfo(i, pID);
				return i;
			}
			i++;
		}
		return -1;
	}

	public synchronized void setRequestedPeerInfotoZero(String pID) {
		int i = 0;
		while (i < this.reqPeerInfo.length) {
			if (this.reqPeerInfo[i] != null && this.reqPeerInfo[i].compareTo(pID) == 0) {
				getPeerInfo(i, null);
			}
			i++;
		}
	}

	public String getpID() {
		return this.pID;
	}

	public Logging getLogging() {
		return this.logging;
	}

	public boolean isFilePresent() {
		return this.neighborPeerInfo.filePresent == 1;
	}

	public int fetchPreferredNumberOfUsers() {
		return this.commonConfigFileParser.PreferredNumberOfUsers;
	}

	public int fetchInterval() {
		return this.commonConfigFileParser.Interval;
	}

	public int fetchOptInterval() {
		return this.commonConfigFileParser.OptInterval;
	}

	public String fetchFName() {
		return this.commonConfigFileParser.FName;
	}

	public int fetchFSize() {
		return this.commonConfigFileParser.FSize;
	}

	public int fetchChunkSize() {
		return this.commonConfigFileParser.ChunkSize;
	}

	public int computeChunkCnt() {
		var size = (fetchFSize() / fetchChunkSize());
		if (fetchFSize() % fetchChunkSize() != 0) {
			size += 1;
		}
		return size;
	}

	public int getChunkcnt() {
		return this.chunkcnt;
	}

	public int fetchAvailablePiecesAfterDone() {
		return this.availablePieces.get(this.pID).cardinality();
	}

	public synchronized void insertToIntList(String lastPeerId) {
		this.peerInterestedList.add(lastPeerId);
	}

	public synchronized void deleteFromIntList(String lastPeerId) {
		if (this.peerInterestedList != null) {
			this.peerInterestedList.remove(lastPeerId);
		}
	}

	public synchronized void setIntList() {
		this.peerInterestedList.clear();
	}

	public synchronized HashSet<String> fetchIntPeerList() {
		return this.peerInterestedList;
	}

	public synchronized HashSet<String> fetchUnchokPeerList() {
		return this.peerUnchokList;
	}

	public synchronized void clearUnchokList() {
		this.peerUnchokList.clear();
	}

	public synchronized void modifyUnchokedlist(HashSet<String> unchokedList) {
		this.peerUnchokList = unchokedList;
	}

	public synchronized void setOptimizedUnchokedPeer(String pIdx) {
		this.optimizedUnchokedPeer = pIdx;
	}

	public synchronized String fetchOptimizedUnchokedPeer() {
		return this.optimizedUnchokedPeer;
	}

	public synchronized boolean testForPeerCompletion() {
		for (var s : this.availablePieces.keySet()) {
			if (this.availablePieces.get(s).cardinality() != this.chunkcnt) {
				return false;
			}
		}
		return true;
	}

	public synchronized Unchocking fetchHandlerForUnchoking() {
		return this.unchocking;
	}

	public synchronized Choking fetchHandlerForChoking() {
		return this.choking;
	}

	public synchronized RandomAccessFile fetchrandomAccessFile() {
		return this.randomAccessFile;
	}

	public synchronized ServerSocket getServerSocket() {
		return this.serverSocket;
	}

	public synchronized Thread fetchCurrentThread() {
		return this.thread;
	}

	public synchronized Boolean ifFinished() {
		return this.finished;
	}

	public synchronized void shutDownAllThreadHandlers() {
		for (var s : this.connectedThreads.keySet()) {
			this.connectedThreads.get(s).stop();
		}
	}

	public synchronized void clearAndExitThreads() {
		try {
			this.fetchHandlerForUnchoking().exitExecution();
			this.fetchHandlerForChoking().exitExecution();
			this.clearUnchokList();
			this.setOptimizedUnchokedPeer(null);
			this.setIntList();
			this.fetchrandomAccessFile().close();
			this.getLogging().terminateLogging();
			this.getServerSocket().close();
			this.fetchCurrentThread().stop();
			this.finished = true;
			this.endProcess.startExecution(6);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class PeerInitiation implements Runnable {
	private final ServerSocket serverSocket;
	private final PeerFunctionsManager peerFunctionsManager;

	public PeerInitiation(ServerSocket serverSocket, PeerFunctionsManager peerFunctionsManager) {
		this.peerFunctionsManager = peerFunctionsManager;
		this.serverSocket = serverSocket;
	}

	public void run() {
		while (true) {
			try {
				var peer = this.serverSocket.accept();
				var peerMessagePassing = new PeerMessagePassing(peer, this.peerFunctionsManager);
				new Thread(peerMessagePassing).start();
			}
			catch (SocketException e) {
				break;
			}
			catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}
}

class Choking implements Runnable {
	private final int unchokingInterval;
	private final int numberOfPreferredNeighbors;
	private final Random random = new Random();
	private final ScheduledExecutorService scheduledExecutorService;
	private final PeerFunctionsManager peerFunctionsManager;
	Choking(PeerFunctionsManager peerFunctionsManager) {
		this.peerFunctionsManager = peerFunctionsManager;
		this.numberOfPreferredNeighbors = peerFunctionsManager.fetchPreferredNumberOfUsers();
		this.unchokingInterval = peerFunctionsManager.fetchInterval();
		this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
	}

	public void run() {
		try {
			var peerCollectionlist = new HashSet<String>();
			var unchokedCollectionOfPeers = new HashSet<String>(this.peerFunctionsManager.fetchUnchokPeerList());
			var interestedPeerList = new ArrayList<String>(this.peerFunctionsManager.fetchIntPeerList());
			if (interestedPeerList.size() > 0) {
				int minPreferredNeighbors = Math.min(this.numberOfPreferredNeighbors, interestedPeerList.size());
				if (this.peerFunctionsManager.fetchAvailablePiecesAfterDone() == this.peerFunctionsManager.getChunkcnt()) {
					int i = 0;
					while(i < minPreferredNeighbors) {
						var successorPeer = interestedPeerList.get(this.random.nextInt(interestedPeerList.size()));
						var peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(successorPeer);

						while (peerCollectionlist.contains(successorPeer)) {
							successorPeer = interestedPeerList.get(this.random.nextInt(interestedPeerList.size()));
							peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(successorPeer);
						}
						if (!unchokedCollectionOfPeers.contains(successorPeer)) {
							if (this.peerFunctionsManager.fetchOptimizedUnchokedPeer() == null || this.peerFunctionsManager.fetchOptimizedUnchokedPeer().compareTo(successorPeer) != 0 ) {
								peerMessagePassing.writeUnchokMsg();
							}
						}
						else {
							unchokedCollectionOfPeers.remove(successorPeer);
						}
						peerCollectionlist.add(successorPeer);
						peerMessagePassing.setDwnRateToZero();
						i++;
					}
				}
				else {
					var downloadRate = new HashMap<String, Integer>(this.peerFunctionsManager.fetchDwnRates());
					var downloadRates = downloadRate.entrySet().stream()
							.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
							.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
					var entryIterator = downloadRates.entrySet().iterator();
					var cnt = 0;
					while (entryIterator.hasNext() && cnt < minPreferredNeighbors) {
						var entry = entryIterator.next();
						if (interestedPeerList.contains(entry.getKey())) {
							var peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(entry.getKey());
							if (!unchokedCollectionOfPeers.contains(entry.getKey())) {
								String optimisticUnchokedPeer = this.peerFunctionsManager.fetchOptimizedUnchokedPeer();
								if (optimisticUnchokedPeer == null || optimisticUnchokedPeer.compareTo(entry.getKey()) != 0 ) {
									peerMessagePassing.writeUnchokMsg();
								}
							}
							else {
								unchokedCollectionOfPeers.remove(entry.getKey());
							}
							peerCollectionlist.add(entry.getKey());
							peerMessagePassing.setDwnRateToZero();
							cnt++;
						}
					}
				}
				this.peerFunctionsManager.modifyUnchokedlist(peerCollectionlist);
				if(peerCollectionlist.size() > 0){
					this.peerFunctionsManager.getLogging().makeLogForPreferredNeighbors(new ArrayList<>(peerCollectionlist));
				}
				for (String i : unchokedCollectionOfPeers) {
					var peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(i);
					peerMessagePassing.writeChokMsg();
				}
			}
			else {
				this.peerFunctionsManager.clearUnchokList();
				for (String i : unchokedCollectionOfPeers) {
					var peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(i);
					peerMessagePassing.writeChokMsg();
				}
				if(this.peerFunctionsManager.testForPeerCompletion()) {
					this.peerFunctionsManager.clearAndExitThreads();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void startExecution() {
		this.scheduledExecutorService.scheduleAtFixedRate(this, 6, this.unchokingInterval, TimeUnit.SECONDS);
	}

	public void exitExecution() {
		this.scheduledExecutorService.shutdownNow();
	}
}

class Unchocking implements Runnable {
	private final int unchokingInterval;
	private final PeerFunctionsManager peerFunctionsManager;
	private final Random random = new Random();
	private final ScheduledExecutorService scheduler;

	Unchocking(PeerFunctionsManager padmin) {
		this.peerFunctionsManager = padmin;
		this.unchokingInterval = padmin.fetchOptInterval();
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	public void run() {
		try {
			var optimisticUnchokedPeer = this.peerFunctionsManager.fetchOptimizedUnchokedPeer();
			var interested = new ArrayList<String>(this.peerFunctionsManager.fetchIntPeerList());
			interested.remove(optimisticUnchokedPeer);
			int size = interested.size();
			if (size > 0) {
				var successorPeer = interested.get(random.nextInt(size));
				while (this.peerFunctionsManager.fetchUnchokPeerList().contains(successorPeer)) {
					size--;
					interested.remove(successorPeer);
					if(size > 0) {
						successorPeer = interested.get(random.nextInt(size));
					}
					else {
						successorPeer = null;
						break;
					}
				}
				this.peerFunctionsManager.setOptimizedUnchokedPeer(successorPeer);
				if(successorPeer != null) {
					var peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(successorPeer);
					peerMessagePassing.writeUnchokMsg();
					this.peerFunctionsManager.getLogging()
							.makeLogForOptUnchokedNeighbor(this.peerFunctionsManager.fetchOptimizedUnchokedPeer());
				}
				if (optimisticUnchokedPeer != null && !this.peerFunctionsManager.fetchUnchokPeerList().contains(optimisticUnchokedPeer)) {
					this.peerFunctionsManager.fetchPeerManagerInfo(optimisticUnchokedPeer).writeChokMsg();
				}
			}
			else {
				var currOptimisticUnchokedPeer = this.peerFunctionsManager.fetchOptimizedUnchokedPeer();
				this.peerFunctionsManager.setOptimizedUnchokedPeer(null);
				if (currOptimisticUnchokedPeer != null && !this.peerFunctionsManager.fetchUnchokPeerList().contains(currOptimisticUnchokedPeer)) {
					var peerMessagePassing = this.peerFunctionsManager.fetchPeerManagerInfo(currOptimisticUnchokedPeer);
					peerMessagePassing.writeChokMsg();
				}
				if(this.peerFunctionsManager.testForPeerCompletion()) {
					this.peerFunctionsManager.clearAndExitThreads();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startExecution() {
		this.scheduler.scheduleAtFixedRate(this, 6, this.unchokingInterval, TimeUnit.SECONDS);
	}

	public void exitExecution() {
		this.scheduler.shutdownNow();
	}
}

class EndProcess implements Runnable {
	private final ScheduledExecutorService scheduler;
	private final PeerFunctionsManager peerFunctionsManager;
	EndProcess(PeerFunctionsManager peerFunctionsManager) {
		this.scheduler = Executors.newScheduledThreadPool(1);
		this.peerFunctionsManager = peerFunctionsManager;
	}

	public void run() {
		try {
			if(this.peerFunctionsManager.ifFinished()) {
				this.peerFunctionsManager.shutDownAllThreadHandlers();
				this.exitExecution();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void startExecution(int tInter) {
		scheduler.scheduleAtFixedRate(this, 30, tInter * 2L, TimeUnit.SECONDS);
	}

	public void exitExecution() {
		this.scheduler.shutdownNow();
	}
}



