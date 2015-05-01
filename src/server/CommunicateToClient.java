package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;

import question.Choice;
import question.Question;

public class CommunicateToClient extends Thread {
	private Server server;
	private InputStream inputStream;
	private Socket socket;
	private String loginId;
	private int clientNumber;
	private BigInteger publicKey;
	private BigInteger choice = BigInteger.ZERO;
	private String ballotProof;
	private Question myQuestion;

	private BigInteger h;

	private BufferedReader bin;
	private PrintWriter pout;
	private boolean isValid = false;
	private boolean voteIsValid = false;
	private BigInteger gPrime;
	private BigInteger s;

	public CommunicateToClient(Server s, Socket socket, int clientNumber) {
		server = s;
		this.socket = socket;
		this.setClientNumber(clientNumber);
		initializeCommunication();
	}

	/**
	 * @definition this method initialize the input and output stream!
	 */
	private void initializeCommunication() {
		try {
			inputStream = socket.getInputStream();
			bin = new BufferedReader(new InputStreamReader(inputStream));
			pout = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @definition this method send data from server to client!
	 * @param data
	 */
	public void sendToClient(String data) {
		pout.println(data);
		pout.flush();
	}

	@Override
	public void run() {
		super.run();
		try {// server listening to client!
			while (true) {
				String command = bin.readLine();
				handleMessage(command);
			}
		} catch (Exception e) {
			System.exit(0);
		}
	}

	/**
	 * @definition this method do the right thing according to the received
	 *             message!!!
	 * @param command
	 * @throws IOException 
	 */
	private void handleMessage(String command) throws Exception {
		String[] splitOfCommands = command.split(" ");
		String cmd = splitOfCommands[0];
		if (cmd.equals("loginId") && server.isPollingIsOpen()) {
			loginId = splitOfCommands[1];
			sendToClient("clientNumber " + server.getNumberOfClients());
			sendToClient("cycleGroup " + server.getCg().getP() + " "
					+ server.getCg().getG());
			server.acceptClient(this);
		} else if (cmd.equals("publicKey")) {
			publicKey = new BigInteger(splitOfCommands[1]);
		} else if (cmd.equals("discreteLogProof")) {
			gPrime = new BigInteger(splitOfCommands[1]);
			s = new BigInteger(splitOfCommands[2]);
			checkDiscreteLogProof();
		} else if (cmd.equals("question")) {
			sendQuestionToClient();
		} else if (cmd.equals("vote")) {
			getAndsaveVote(splitOfCommands[1]);
			ballotProof = bin.readLine();
			ballotProof = ballotProof.substring(8);
			checkBallotProof();
			server.sendClientVoteToOthers(loginId);
		} else if (cmd.equals("myQuestion")) {
			String q = bin.readLine();
			String choices = bin.readLine();
			String[] choicesArray = choices.split(" ");
			int numOfChoices = Integer.parseInt(choicesArray[0]);
			String[] allChoices = new String[numOfChoices];
			for (int i = 0; i < numOfChoices; i++) {
				allChoices[i] = choicesArray[i + 1];
			}
			setQuestion(q, allChoices);
//			myQuestion.printTheQuestion();
		}
	}

	/**
	 * @definition this method check that ballot proof is valid or not!
	 */
	private boolean checkBallotProof() {
		String[] ballot = ballotProof.split(" ");
		BigInteger a1 = new BigInteger(ballot[0]);
		BigInteger b1 = new BigInteger(ballot[1]);
		BigInteger c1 = new BigInteger(ballot[2]);
		BigInteger s1 = new BigInteger(ballot[3]);
		BigInteger a2 = new BigInteger(ballot[4]);
		BigInteger b2 = new BigInteger(ballot[5]);
		BigInteger c2 = new BigInteger(ballot[6]);
		BigInteger s2 = new BigInteger(ballot[7]);
		BigInteger g = server.getCg().getG();
		BigInteger p = server.getCg().getP();
		BigInteger q = p.subtract(BigInteger.ONE).mod(p);
		BigInteger x = g.modPow(s1, p);
		BigInteger y = (publicKey.modPow(c1, p)).multiply(a1).mod(p);
		if(!x.equals(y)){
			System.err.println(1);
			return false;
		}
		x = g.modPow(s2, p);
		y = (publicKey.modPow(c2, p)).multiply(a2).mod(p);
		if(!x.equals(y)){
			System.err.println(2);
			return false;
		}
		x = h.modPow(s1, p);
		y = choice.modPow(c1, p).multiply(b1).mod(p);
		if(!x.equals(y)){
			System.err.println(3);
			return false;
		}
		x = h.modPow(s2, p);
		y = g.modPow(BigInteger.ONE, p).modInverse(p);
		y = choice.multiply(y).mod(p);
		y = (y.modPow(c2, p)).multiply(b2).mod(p);
		if(!x.equals(y)){
			System.err.println(4);
			return false;
		}
		
		MessageDigest digest;
		BigInteger c = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			x = a1.add(b1).mod(p).add(publicKey).mod(p);
			x = x.add(choice).mod(p);
			x = x.add(a2).add(b2).mod(p);
			byte[] hash = digest.digest(x.toString().getBytes("UTF-8"));
			c = new BigInteger(hash).mod(q);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(!c.equals(c1.add(c2).mod(q))){
			System.err.println("5");
			return false;			
		}
		setVoteIsValid(true);
		return true;
	}

	private void getAndsaveVote(String vote) {
		choice = new BigInteger(vote);
	}

	/**
	 * @definition this method send current question to client!
	 */
	private void sendQuestionToClient() {
		Question q = server.getQuestionById(1);
		sendToClient("question");
		sendToClient(q.getQuestion());
		Choice[] choices = q.getChoices();
		int numOfChoices = choices.length;
		String allChoices = numOfChoices + " ";
		for (int i = 0; i < numOfChoices; i++) {
			allChoices = allChoices + choices[i].getChoice() + " ";
		}
		allChoices = allChoices.substring(0, allChoices.length() - 1);
		sendToClient(allChoices);
	}

	/**
	 * @definition this method check that discrete proof logic is valid or not!
	 */
	private boolean checkDiscreteLogProof() {
		BigInteger g = server.getCg().getG();
		BigInteger p = server.getCg().getP();
		BigInteger q = (p.subtract(new BigInteger("1")));

		BigInteger x = g.modPow(s, p);

		BigInteger c = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(gPrime.toString().getBytes("UTF-8"));
			c = new BigInteger(hash).mod(q);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BigInteger y = (publicKey.modPow(c, p)).multiply(gPrime).mod(p);
		if (x.equals(y)) {
			isValid = true;
		} else {
			System.out.println("wrong!");
		}
		return isValid;
	}
	
	private void setQuestion(String q, String[] choices) {
		int numOfChoices = choices.length;
		myQuestion = new Question(q, numOfChoices, 1);
		for (int i = 0; i < numOfChoices; i++) {
			myQuestion.addChoice(choices[i], i);
		}
	}

	public int getClientNumber() {
		return clientNumber;
	}

	public void setClientNumber(int clientNumber) {
		this.clientNumber = clientNumber;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public BigInteger getgPrime() {
		return gPrime;
	}

	public BigInteger getS() {
		return s;
	}

	public BigInteger getPublicKey() {
		return publicKey;
	}

	public BigInteger getChoice() {
		return choice;
	}

	public void setChoice(BigInteger choice) {
		this.choice = choice;
	}

	public BigInteger getH() {
		return h;
	}

	public void setH(BigInteger h) {
		this.h = h;
	}

	public boolean isVoteIsValid() {
		return voteIsValid;
	}

	public void setVoteIsValid(boolean voteIsValid) {
		this.voteIsValid = voteIsValid;
	}

	public String getBallotProof() {
		return ballotProof;
	}

}