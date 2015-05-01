package client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import question.Choice;
import question.Question;
import server.CommunicateToClient;

public class Client extends Thread {
	private CommunicationToServer communicationToServer;
	private Socket socket;
	private String host = "127.0.0.1";
	private int port = 8080;
	private String loginId = "";
	private int clientNumber;
	private Question question;
	private Scanner sc;
	private BigInteger privateKey;
	private BigInteger publicKey;
	private BigInteger p;
	private BigInteger g;
	private boolean pollingIsOpen = false;
	private boolean isVote = false;
	private int voteNumber;

	private int numOfClients;
	private AbstractClient[] clients;

	@Override
	public void run() {
		sc = new Scanner(System.in);
		try {
			while (true) {
				if (loginId.length() != 0) {
					while (true) {
						String[] login = sc.nextLine().split(" ");
						if (login.length == 2 && login[0].equals("Login")
								&& login[1].equals(loginId))
							break;
					}
				}
				while (true) {
					String command = sc.nextLine();
					if (!handleCommand(command))
						break;
				}
			}
		} catch (ConnectException e) {
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.run();
	}

	/**
	 * @definition this method handle command that client enter it!
	 * @param command
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private boolean handleCommand(String command) throws UnknownHostException,
			IOException {
		if (command.equals("Login")) {
			socket = new Socket(host, port);
			setLoginId(getLoginId());
			communicationToServer = new CommunicationToServer(socket, this);
			communicationToServer.start();
			communicationToServer.sendToServer("loginId " + loginId);
		} else if (command.equals("Question")) {
			communicationToServer.sendToServer("question");
		} else if (command.equals("Vote") && pollingIsOpen && question != null) {
			if (isVote) {
				System.out.println("You vote before, your choice is :");
				question.getChoices()[voteNumber - 1].printChoice();
			} else {
				getVoteAndSendToServer();
				sendMyQuestionToServer();
				isVote = true;
			}
		} else if (command.equals("Votes")) {
			printAllVotes();
		} else if (command.equals("Verify")) {
			verify();
		} else if (command.equals("Result")) {
			calculateResult();
		} else if (command.equals("Exit")) {
			return false;
		}
		return true;
	}

	public void createAbstractClients(int numOfClients) {
		this.numOfClients = numOfClients;
		clients = new AbstractClient[numOfClients];
	}

	/**
	 * @definition this method compute HI s that client need for voting
	 */
	public void computeHIs() {
		BigInteger h = new BigInteger("1");
		for (int i = 1; i < numOfClients; i++) {
			h = h.multiply(clients[i].getPublicKey().modInverse(p)).mod(p);
		}
		clients[0].setH(h);
		for (int i = 1; i < numOfClients; i++) {
			h = h.multiply(clients[i].getPublicKey()).mod(p);
			h = h.multiply(clients[i - 1].getPublicKey()).mod(p);
			clients[i].setH(h);
		}
	}

	public void addAbstractClient(int clientNum, String loginId, BigInteger pk,
			BigInteger gPrime, BigInteger s) {
		clients[clientNum] = new AbstractClient(loginId, clientNum, pk, gPrime,
				s);
		clients[clientNum].checkDiscreteLogProof(p, g);
	}

	/**
	 * @definition this method send current question to client!
	 */
	private void sendMyQuestionToServer() {
		communicationToServer.sendToServer("myQuestion");
		communicationToServer.sendToServer(question.getQuestion());
		Choice[] choices = question.getChoices();
		int numOfChoices = choices.length;
		String allChoices = numOfChoices + " ";
		for (int i = 0; i < numOfChoices; i++) {
			allChoices = allChoices + choices[i].getChoice() + " ";
		}
		allChoices = allChoices.substring(0, allChoices.length() - 1);
		communicationToServer.sendToServer(allChoices);
	}

	/**
	 * @definition this method verify the proofs of voter!
	 */
	private void verify() {
		for (int i = 0; i < numOfClients; i++) {
			if (clients[i].getClientNumber() == clientNumber)
				continue;
			AbstractClient client = clients[i];
			System.out.println("Voter " + client.getLoginId() + ":");
			if (client.isValid())
				System.out.println("DL Proof: Valid");
			else
				System.out.println("DL Proof: Not Valid");
			if (client.getChoice().equals(BigInteger.ZERO))
				System.out.println("Ballot Proof: Has not sent yet !");
			else if (client.isVoteIsValid())
				System.out.println("Ballot Proof: Valid");
			else
				System.out.println("Ballot Proof: Not Valid");
			System.out.println("______________________________");
		}
	}

	public void saveVote(int clientNum, BigInteger choice, String ballotProof) {
		clients[clientNum].setBallotProof(ballotProof);
		clients[clientNum].setChoice(choice);
		checkBallotProof(ballotProof, clientNum);
	}

	/**
	 * @definition this method calculate result of poling
	 */
	private void calculateResult() {
		BigInteger v = new BigInteger("1");
		for (int i = 0; i < numOfClients; i++) {
			AbstractClient client = clients[i];
			BigInteger choice = client.getChoice();
			if (choice.equals(new BigInteger("0"))) {
				return;
			}
			v = v.multiply(choice).mod(p);
		}
		int i = 0;
		BigInteger t = BigInteger.ONE;
		while (true) {
			if (t.equals(v))
				break;
			t = t.multiply(g).mod(p);
			i++;
		}
		System.out.println("(" + (numOfClients - i) + ")"
				+ " persons have selected :");
		question.getChoices()[0].printChoice();
		System.out.println("_______________________________");
		System.out.println("(" + i + ")" + " persons have selected :");
		question.getChoices()[1].printChoice();
		System.out.println("_______________________________");
		System.out.println("So the winner is :");
		if (numOfClients - i > i)
			question.getChoices()[0].printChoice();
		else if (numOfClients - i < i)
			question.getChoices()[1].printChoice();
		else {
			question.getChoices()[0].printChoice();
			question.getChoices()[1].printChoice();
		}
	}

	/**
	 * @definition this method print all clients vote!
	 */
	private void printAllVotes() {
		for (int i = 0; i < numOfClients; i++) {
			AbstractClient client = clients[i];
			if (client.getChoice().equals(BigInteger.ZERO))
				continue;
			System.out.println("Voter " + client.getLoginId() + " :");
			System.out.println("Public Key : " + client.getPublicKey());
			System.out.println("DL Proof : (" + client.getgPrime() + ", "
					+ client.getS() + ")");

			System.out.println("Ballot : " + client.getChoice());
			String[] x = client.getBallotProof().split(" ");
			System.out.println("Ballot Proof : {(" + x[0] + ", " + x[1] + ", "
					+ x[2] + ", " + x[3] + "), (" + x[4] + ", " + x[5] + ", "
					+ x[6] + ", " + x[7] + ")}");
			System.out.println("____________________________");
		}
	}

	/**
	 * @definition this method check that ballot proof is valid or not!
	 */
	private boolean checkBallotProof(String ballotProof, int clientNum) {
		String[] ballot = ballotProof.split(" ");
		BigInteger publicKey = clients[clientNum].getPublicKey();
		BigInteger a1 = new BigInteger(ballot[0]);
		BigInteger b1 = new BigInteger(ballot[1]);
		BigInteger c1 = new BigInteger(ballot[2]);
		BigInteger s1 = new BigInteger(ballot[3]);
		BigInteger a2 = new BigInteger(ballot[4]);
		BigInteger b2 = new BigInteger(ballot[5]);
		BigInteger c2 = new BigInteger(ballot[6]);
		BigInteger s2 = new BigInteger(ballot[7]);
		BigInteger q = p.subtract(BigInteger.ONE).mod(p);
		BigInteger x = g.modPow(s1, p);
		BigInteger y = (publicKey.modPow(c1, p)).multiply(a1).mod(p);
		if (!x.equals(y)) {
			System.err.println(1);
			return false;
		}
		x = g.modPow(s2, p);
		y = (publicKey.modPow(c2, p)).multiply(a2).mod(p);
		if (!x.equals(y)) {
			System.err.println(2);
			return false;
		}
		BigInteger h = clients[clientNum].getH();
		BigInteger choice = clients[clientNum].getChoice();
		x = h.modPow(s1, p);
		y = choice.modPow(c1, p).multiply(b1).mod(p);
		if (!x.equals(y)) {
			System.err.println(3);
			return false;
		}
		x = h.modPow(s2, p);
		y = g.modPow(BigInteger.ONE, p).modInverse(p);
		y = choice.multiply(y).mod(p);
		y = (y.modPow(c2, p)).multiply(b2).mod(p);
		if (!x.equals(y)) {
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
		if (!c.equals(c1.add(c2).mod(q))) {
			System.err.println("5");
			return false;
		}
		clients[clientNum].setVoteIsValid(true);
		return true;
	}

	/**
	 * @definition this method get vote from client and send them to server!
	 */
	private void getVoteAndSendToServer() {
		voteNumber = getVote();
		BigInteger h = clients[clientNumber].getH();
		BigInteger b = h.modPow(privateKey, p);
		if (voteNumber == 1) {
			b = b.multiply(g.modPow(new BigInteger("0"), p)).mod(p);
		} else if (voteNumber == 2) {
			b = b.multiply(g.modPow(new BigInteger("1"), p)).mod(p);
		}
		communicationToServer.sendToServer("vote " + b);
		communicationToServer.ballotProof(b, voteNumber);
		System.out.println("Your vote was sent.");
	}

	/**
	 * @definition this method get vote from client!
	 * @return
	 */
	private int getVote() {
		String m = "Enter your vote (";
		int numOfChoices = question.getNumberOfChoices();
		for (int i = 1; i < numOfChoices; i++) {
			m = m + i + ", ";
		}
		m = m.substring(0, m.length() - 2);
		m = m + " or " + numOfChoices + ")";
		System.out.println(m);
		return Integer.parseInt(sc.nextLine());
	}

	public BigInteger getHOfClient(int clientNumber) {
		return clients[clientNumber].getH();
	}

	private String getLoginId() {
		System.out.println("Please enter your ID :");
		return sc.nextLine();
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public int getClientNumber() {
		return clientNumber;
	}

	public void setClientNumber(int clientNumber) {
		this.clientNumber = clientNumber;
	}

	public BigInteger getP() {
		return p;
	}

	public void setP(BigInteger p) {
		this.p = p;
	}

	public BigInteger getG() {
		return g;
	}

	public void setG(BigInteger g) {
		this.g = g;
	}

	public BigInteger getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(BigInteger privateKey) {
		this.privateKey = privateKey;
	}

	public BigInteger getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(BigInteger publicKey) {
		this.publicKey = publicKey;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(String q, String[] choices) {
		int numOfChoices = choices.length;
		question = new Question(q, numOfChoices, 1);
		for (int i = 0; i < numOfChoices; i++) {
			question.addChoice(choices[i], i);
		}
	}

	public boolean isPollingIsOpen() {
		return pollingIsOpen;
	}

	public void setPollingIsOpen(boolean pollingIsOpen) {
		this.pollingIsOpen = pollingIsOpen;
	}
}
