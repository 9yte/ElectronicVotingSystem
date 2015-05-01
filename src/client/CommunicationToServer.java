package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Random;


public class CommunicationToServer extends Thread {
	private Socket socket;
	private Client client;
	private InputStream inputStream;
	private BufferedReader bin;
	private PrintWriter pout;
	BigInteger one = new BigInteger("1");
	BigInteger two = new BigInteger("2");

	public CommunicationToServer(Socket socket, Client client) {
		this.socket = socket;
		this.client = client;
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
	 * @definition this method send data from client to server!
	 * @param data
	 */
	public void sendToServer(String data) {
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
	 * @definition this method do the right thing after receiving command from client
	 * @param command
	 * @throws Exception
	 */
	private void handleMessage(String command) throws Exception {
		String[] splitOfCommands = command.split(" ");
		String cmd = splitOfCommands[0];
		if (cmd.equals("clientNumber"))
			client.setClientNumber(new Integer(splitOfCommands[1]));
		else if (cmd.equals("cycleGroup")) {
			client.setP(new BigInteger(splitOfCommands[1]));
			client.setG(new BigInteger(splitOfCommands[2]));
			generateKeysAndSendThem();
		} else if (cmd.equals("question")) {
			String q = bin.readLine();
			String choices = bin.readLine();
			String[] choicesArray = choices.split(" ");
			int numOfChoices = Integer.parseInt(choicesArray[0]);
			String[] allChoices = new String[numOfChoices];
			for (int i = 0; i < numOfChoices; i++) {
				allChoices[i] = choicesArray[i + 1];
			}
			client.setQuestion(q, allChoices);
			client.getQuestion().printTheQuestion();
		} else if (cmd.equals("FirstStepFin")) {
			client.setPollingIsOpen(true);
			int numOfClients = Integer.parseInt(splitOfCommands[1]);
			client.createAbstractClients(numOfClients);
			getClientsInfo(numOfClients);
		} else if (cmd.equals("clientVote")) {
			saveVote(Integer.parseInt(splitOfCommands[1]));
		}
	}

	/**
	 * @definition this method save vote of client!!!
	 * @param clientNum
	 * @throws IOException
	 */
	private void saveVote(int clientNum) throws IOException {
		String vote = bin.readLine();
		String ballotProof = bin.readLine();
		client.saveVote(clientNum, new BigInteger(vote.split(" ")[1]),
				ballotProof);
	}

	/**
	 * @throws IOException
	 * @definition this method get clients public key and DL proofs from
	 *             server!!!
	 */
	private void getClientsInfo(int numOfClients) throws IOException {
		for (int i = 0; i < numOfClients; i++) {
			String info = bin.readLine();
			String loginId = info.split(" ")[1];
			String number = bin.readLine();
			int clientNumber = Integer.parseInt(number.split(" ")[1]);
			String pk = bin.readLine();
			BigInteger publicKey = new BigInteger(pk.split(" ")[1]);
			String dlProof = bin.readLine();
			BigInteger gPrime = new BigInteger(dlProof.split(" ")[1]);
			BigInteger s = new BigInteger(dlProof.split(" ")[2]);
			client.addAbstractClient(clientNumber, loginId, publicKey, gPrime,
					s);
		}
		client.computeHIs();
	}

	/**
	 * @definition this method generate private and public keys and send them to
	 *             server!!!
	 */
	private void generateKeysAndSendThem() {
		BigInteger p = client.getP();
		BigInteger g = client.getG();
		BigInteger q = (p.subtract(one)).divide(two);
		BigInteger pk = new BigInteger(p.bitLength(), new Random()).mod(p);
		client.setPrivateKey((g.modPow(two, p)).modPow(pk, p));
		client.setPublicKey(g.modPow(client.getPrivateKey(), p));
		sendToServer("publicKey " + client.getPublicKey());
		discreteLogProof(client.getPrivateKey());
	}

	/**
	 * @definition this method send requirements data for discrete log proof of
	 *             knowing x from g^x
	 * @param x
	 */
	private void discreteLogProof(BigInteger x) {
		BigInteger g = client.getG();
		BigInteger p = client.getP();
		BigInteger q = (p.subtract(one));
		BigInteger h = g.modPow(x, p);

		BigInteger w = new BigInteger(p.bitLength(), new Random()).mod(p);
		w = (g.modPow(two, p)).modPow(w, p);
		BigInteger gPrime = g.modPow(w, p);

		MessageDigest digest;
		BigInteger c = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(gPrime.toString().getBytes("UTF-8"));
			c = new BigInteger(hash).mod(q);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BigInteger s = (c.multiply(x).mod(q)).add(w).mod(q);
		sendToServer("discreteLogProof " + gPrime + " " + s);
	}

	/**
	 * @definition this method send requirements data for ballot proof
	 * @param x
	 */
	public void ballotProof(BigInteger b, int choiceNumber) {
		BigInteger g = client.getG();
		BigInteger p = client.getP();
		BigInteger q = (p.subtract(one));
		BigInteger h = client.getHOfClient(client.getClientNumber());

		BigInteger w = new BigInteger(p.bitLength(), new Random()).mod(p);
		w = (g.modPow(two, p)).modPow(w, p);
		BigInteger sI = new BigInteger(p.bitLength(), new Random()).mod(p);
		sI = (g.modPow(two, p)).modPow(sI, p);
		BigInteger cI = new BigInteger(p.bitLength(), new Random()).mod(p);
		cI = (g.modPow(two, p)).modPow(cI, p);
		BigInteger x = g.modPow(sI, p);
		BigInteger y = client.getPublicKey().modPow(cI, p).modInverse(p);
		BigInteger aI = x.multiply(y).mod(p);
		x = h.modPow(sI, p);
		if (choiceNumber == 1)
			y = (g.modPow(BigInteger.ONE, p)).modInverse(p);
		else
			y = (g.modPow(BigInteger.ZERO, p)).modInverse(p);
		y = b.multiply(y).mod(p);
		y = (y.modPow(cI, p)).modInverse(p);
		BigInteger bI = x.multiply(y).mod(p);

		BigInteger aK = g.modPow(w, p);
		BigInteger bK = h.modPow(w, p);

		MessageDigest digest;
		BigInteger c = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			x = aI.add(bI).mod(p).add(client.getPublicKey()).mod(p);
			x = x.add(b).mod(p);
			x = x.add(aK).add(bK).mod(p);
			byte[] hash = digest.digest(x.toString().getBytes("UTF-8"));
			c = new BigInteger(hash).mod(q);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BigInteger cK = c.subtract(cI).mod(q);
		BigInteger sK = (cK.multiply(client.getPrivateKey()).mod(q)).add(w)
				.mod(q);
		if (choiceNumber == 1)
			sendToServer("dlProof " + aK + " " + bK + " " + cK + " " + sK + " "
					+ aI + " " + bI + " " + cI + " " + sI);
		else
			sendToServer("dlProof " + aI + " " + bI + " " + cI + " " + sI + " "
					+ aK + " " + bK + " " + cK + " " + sK);
	}
}
