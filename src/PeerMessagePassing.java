import java.lang.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class PeerMessagePassing implements Runnable {

	private boolean connSet = false;
	private boolean initBool = false;
	private String lastPeerID;
	private final HandshakeInfo handshakeInfo;
	private volatile int dwnRate = 0;
	private volatile ObjectOutputStream objectOutputStream;
	private volatile ObjectInputStream objectInputStream;
	private final PeerFunctionsManager peerFunctionsManager;


	public void run() {
		try {
			var bytes = this.handshakeInfo.formHSMessage();
			this.objectOutputStream.write(bytes);
			this.objectOutputStream.flush();
			while (true) {
				if (!this.connSet) {
					var rspMessage = new byte[32];
					this.objectInputStream.readFully(rspMessage);

					// Handshake message send
					this.handshakeInfo.getHSMessage(rspMessage);
					this.lastPeerID = this.handshakeInfo.getPID();
					this.peerFunctionsManager.insertConnectedPeers(this, this.lastPeerID);
					this.peerFunctionsManager.insertConnectedThreads(this.lastPeerID, Thread.currentThread());
					this.connSet = true;
					if (this.initBool) {
						this.peerFunctionsManager.getLogging().makeLogForTCPConnection(this.lastPeerID);
					}
					else {
						this.peerFunctionsManager.getLogging().makeLogForTCPConnectionServer(this.lastPeerID);
					}

					if (this.peerFunctionsManager.isFilePresent() || this.peerFunctionsManager.findAvailableChunk(this.peerFunctionsManager.getpID()).cardinality() >= 1) {
						try {
							var bitSet = this.peerFunctionsManager.findAvailableChunk(this.peerFunctionsManager.getpID());
							var messageInfo = new MessageInfo('5', bitSet.toByteArray());
							this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				else {
					while (this.objectInputStream.available() <= 3) {
					}
					var responseMessageSize = this.objectInputStream.readInt();
					var rspMessage = new byte[responseMessageSize];
					this.objectInputStream.readFully(rspMessage);
					var msgType = (char) rspMessage[0];
					var messageInfo = new MessageInfo();
					messageInfo.extractOriginalMsg(responseMessageSize, rspMessage);
					switch (msgType) {
						case '0':
							this.peerFunctionsManager.setRequestedPeerInfotoZero(this.lastPeerID);
							this.peerFunctionsManager.getLogging().makeLogForChokedNeighbors(this.lastPeerID);
							break;
						case '1':
							var reqIdx = this.peerFunctionsManager.getRequestedChunk(this.lastPeerID);
							if (reqIdx == -1) {
								this.writeNotIntMsg();
							} else {
								this.writeReqMsg(reqIdx);
							}
							this.peerFunctionsManager.getLogging().makeLogForUnchokedNeighbors(this.lastPeerID);
							break;
						case '2':
							this.peerFunctionsManager.insertToIntList(this.lastPeerID);
							this.peerFunctionsManager.getLogging().makeLogForInterestedMessage(this.lastPeerID);
							break;
						case '3':
							this.peerFunctionsManager.deleteFromIntList(this.lastPeerID);
							this.peerFunctionsManager.getLogging().makeLogForNotInterestedMessage(this.lastPeerID);
							break;
						case '4': {
							var chunkIdx = messageInfo.receiveChunkIndex();
							this.peerFunctionsManager.modifyChunkStatus(this.lastPeerID, chunkIdx);
							if (this.peerFunctionsManager.testForPeerCompletion()) {
								this.peerFunctionsManager.clearAndExitThreads();
							}
							if (this.peerFunctionsManager.isPeerInterested(this.lastPeerID)) {
								this.writeIntMsg();
							} else {
								this.writeNotIntMsg();
							}
							this.peerFunctionsManager.getLogging().makeLogForHaveMessage(this.lastPeerID, chunkIdx);
							break;
						}
						case '5':
							var bitSet = messageInfo.extractBitMsg();
//							this.processBitFieldMessage(bitSet);
							this.peerFunctionsManager.modifyBits(this.lastPeerID, bitSet);
							if (!this.peerFunctionsManager.isFilePresent()) {
								if (this.peerFunctionsManager.isPeerInterested(this.lastPeerID)) {
									this.writeIntMsg();
								} else {
									this.writeNotIntMsg();
								}
							}
							break;
						case '6':
							if (this.peerFunctionsManager.fetchUnchokPeerList().contains(this.lastPeerID)
									|| (this.peerFunctionsManager.fetchOptimizedUnchokedPeer() != null && this.peerFunctionsManager.fetchOptimizedUnchokedPeer().compareTo(this.lastPeerID) == 0)) {
								int pieceIndex = messageInfo.receiveChunkIndex();
								try {
									ByteArrayOutputStream stream = new ByteArrayOutputStream();
									var array = ByteBuffer.allocate(4).putInt(pieceIndex).array();
									stream.write(array);
									stream.write(this.peerFunctionsManager.readContentofFile(pieceIndex));
									MessageInfo am = new MessageInfo('7', stream.toByteArray());
									this.writeObjectToOutputStream(am.constructOriginalMsg());
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
							break;
						case '7': {
							var chunkIdx = messageInfo.receiveChunkIndex();
							var chunk = messageInfo.receiveChunk();
							this.peerFunctionsManager.writeContentToFile(chunk, chunkIdx);
							this.peerFunctionsManager.modifyChunkStatus(this.peerFunctionsManager.getpID(), chunkIdx);
							this.dwnRate++;
							var allPeersAreDone = this.peerFunctionsManager.testForPeerCompletion();
							this.peerFunctionsManager.getLogging().makeLogForDownloadedPiece(this.lastPeerID, chunkIdx, this.peerFunctionsManager.fetchAvailablePiecesAfterDone());
							this.peerFunctionsManager.getPeerInfo(chunkIdx, null);
							this.peerFunctionsManager.sendHaveToAll(chunkIdx);
							if (this.peerFunctionsManager.findAvailableChunk(this.peerFunctionsManager.getpID()).cardinality() != this.peerFunctionsManager.getChunkcnt()) {
								reqIdx = this.peerFunctionsManager.getRequestedChunk(this.lastPeerID);
								if (reqIdx != -1) {
									this.writeReqMsg(reqIdx);
								} else {
									this.writeNotIntMsg();
								}
							} else {
								this.peerFunctionsManager.getLogging().makeLogForDownloadFinish();
								if (allPeersAreDone) {
									this.peerFunctionsManager.clearAndExitThreads();
								}
								this.writeNotIntMsg();
							}
							break;
						}
						default:
							break;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getDwnRate() {
		return this.dwnRate;
	}

	public void setDwnRateToZero() {
		this.dwnRate = 0;
	}

	public PeerMessagePassing(Socket socket, PeerFunctionsManager peerFunctionsManager) {
		this.peerFunctionsManager = peerFunctionsManager;
		try {
			this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			this.objectOutputStream.flush();
			this.objectInputStream = new ObjectInputStream(socket.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.handshakeInfo = new HandshakeInfo(this.peerFunctionsManager.getpID());
	}

	public void setLastPeerID(String pid) {
		this.initBool = true;
		this.lastPeerID = pid;
	}

	public synchronized void writeObjectToOutputStream(byte[] bytes) {
		try {
			this.objectOutputStream.write(bytes);
			this.objectOutputStream.flush();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeChokMsg() {
		try {
			var messageInfo = new MessageInfo('0');
			this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeUnchokMsg() {
		try {
			var messageInfo = new MessageInfo('1');
			this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeHaveMsg(int i) {
		try {
			var array = ByteBuffer.allocate(4).putInt(i).array();
			var messageInfo = new MessageInfo('4', array);
			this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeReqMsg(int i) {
		try {
			var array = ByteBuffer.allocate(4).putInt(i).array();
			var messageInfo = new MessageInfo('6', array);
			this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeIntMsg() {
		try {
			var messageInfo = new MessageInfo('2');
			this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeNotIntMsg() {
		try {
			var messageInfo = new MessageInfo('3');
			this.writeObjectToOutputStream(messageInfo.constructOriginalMsg());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class HandshakeInfo {
	private final String HSHeader;
	private String PID;
	public String getPID(){
		return this.PID;
	}
	public HandshakeInfo(String PID) {
		this.HSHeader = "P2PFILESHARINGPROJ";
		this.PID = PID;
	}

	public byte[] formHSMessage() {
		var outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(this.HSHeader.getBytes(StandardCharsets.UTF_8));
			outputStream.write(new byte[10]);
			outputStream.write(this.PID.getBytes(StandardCharsets.UTF_8));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return outputStream.toByteArray();
	}

	public void getHSMessage(byte[] m){
		var message = new String(m,StandardCharsets.UTF_8);
		this.PID = message.substring(28,32);
	}
}

class MessageInfo {
	private int msgSize;
	private byte[] msgContent;
	private char typeOfMsg;

	public MessageInfo() {

	}

	public MessageInfo(char typeOfMsg) {
		this.typeOfMsg = typeOfMsg;
		this.msgContent = new byte[0];
		this.msgSize = 1;
	}

	public MessageInfo(char typeOfMsg, byte[] msgContent) {
		this.msgContent = msgContent;
		this.typeOfMsg = typeOfMsg;
		this.msgSize = 1 + this.msgContent.length;
	}

	public BitSet extractBitMsg() {
		var bitSet = new BitSet();
		bitSet = BitSet.valueOf(this.msgContent);
		return bitSet;
	}

	public byte[] receiveChunk() {
		var length = this.msgSize - 5;
		var chunk = new byte[length];
		System.arraycopy(this.msgContent, 4, chunk, 0, length);
		return chunk;
	}

	public byte[] constructOriginalMsg() {
		var msgStream = new ByteArrayOutputStream();
		try {
			var msgBytes = ByteBuffer.allocate(4).putInt(this.msgSize).array();
			msgStream.write(msgBytes);
			msgStream.write((byte) this.typeOfMsg);
			msgStream.write(this.msgContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return msgStream.toByteArray();
	}

	public int receiveChunkIndex() {
		var size = new byte[4];
		System.arraycopy(this.msgContent, 0, size, 0, 4);
		var msgByte = ByteBuffer.wrap(size);
		return msgByte.getInt();
	}

	public void extractOriginalMsg(int size, byte[] msg) {
		this.typeOfMsg = (char) msg[0];
		this.msgSize = size;
		byte[] resp = new byte[this.msgSize - 1];
		System.arraycopy(msg, 1, resp, 0, this.msgSize - 1);
		this.msgContent = resp;
	}
}
