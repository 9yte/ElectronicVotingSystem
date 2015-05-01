package server;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.jws.Oneway;

import question.*;
import numberTheory.*;

public class Server {
	private HashMap<Integer, Question> questions;
	private HashMap<String, CommunicateToClient> clients;
	private boolean pollingIsOpen = true;
	private int numberOfQuestions;
	private int socketPort = 8080;
	private int numberOfClients = 0;
	private CycleGroup cg;
	private Scanner sc;
	private AcceptClient acceptClient;
	ServerSocket serverSocket;

	public Server() {
		questions = new HashMap<Integer, Question>();
		clients = new HashMap<String, CommunicateToClient>();
	}

	public void runServer() {
		setCg(new CycleGroup());
		try {
			serverSocket = new ServerSocket(socketPort);
			sc = new Scanner(System.in);
			while(true){
				while (true) {
					String command = sc.nextLine();
					if (command.equals("Executive"))
						break;
				}
				acceptClient = new AcceptClient(serverSocket, this);
				acceptClient.start();
				while (true) {
					String command = sc.nextLine();
					if (!handleCommand(command))
						break;
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * @definition this method do the right thing according to the given command
	 *             from console!
	 * @param command
	 * @return
	 * @throws IOException 
	 */
	private boolean handleCommand(String command) throws IOException {
		if (command.equals("Add"))
			getQuestion();
		else if (command.equals("Question"))
			questions.get(numberOfQuestions).printTheQuestion();
		else if (command.equals("Votes"))
			printAllVotes();
		else if (command.equals("Verify"))
			verify();
		else if (command.equals("FirstStepFin")) {
			setPollingIsOpen(false);
			serverSocket.close();
			computeHIs();
			sendAllClientPacketsToOthers();
		} else if (command.equals("Exit")) {
			System.out.println("Good Bye!");
			return false;
		} else if (command.equals("Result"))
			calculateResult();
		return true;
	}

	/**
	 * @definition this method print all clients vote!
	 */
	private void printAllVotes() {
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			String loginId = entry.getKey();
			CommunicateToClient client = entry.getValue();
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
	 * @definition this method calculate result of poling
	 */
	private void calculateResult() {
		BigInteger v = new BigInteger("1");
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			String loginId = entry.getKey();
			CommunicateToClient client = entry.getValue();
			BigInteger choice = client.getChoice();
			if (choice.equals(new BigInteger("0"))) {
				return;
			}
			v = v.multiply(choice).mod(cg.getP());
		}
		int i = 0;
		BigInteger g = cg.getG();
		BigInteger t = BigInteger.ONE;
		while (true) {
			if (t.equals(v))
				break;
			t = t.multiply(g).mod(cg.getP());
			i++;
		}
		System.out.println("(" + (numberOfClients - i) + ")"
				+ " persons have selected :");
		questions.get(1).getChoices()[0].printChoice();
		System.out.println("_______________________________");
		System.out.println("(" + i + ")" + " persons have selected :");
		questions.get(1).getChoices()[1].printChoice();
		System.out.println("_______________________________");
		System.out.println("So the winner is :");
		if (numberOfClients - i > i)
			questions.get(1).getChoices()[0].printChoice();
		else if (numberOfClients - i < i)
			questions.get(1).getChoices()[1].printChoice();
		else {
			questions.get(1).getChoices()[0].printChoice();
			questions.get(1).getChoices()[1].printChoice();
		}
	}

	/**
	 * @definition this method send all receive packets from clients to others,
	 *             including DL and Ballot proof and public key
	 */
	private void sendAllClientPacketsToOthers() {
		sendToAll("FirstStepFin " + numberOfClients);
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			CommunicateToClient client = entry.getValue();
			sendToAll("loginId " + client.getLoginId());
			sendToAll("clientNumber " + client.getClientNumber());
			sendToAll("publicKey " + client.getPublicKey());
			sendToAll("dlProof " + client.getgPrime() + " " + client.getS());
		}
	}

	/**
	 * @definition this method send all receive packets from clients to others,
	 *             including DL and Ballot proof and public key
	 */
	public void sendClientVoteToOthers(String id) {
		CommunicateToClient voter = clients.get(id);
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			CommunicateToClient client = entry.getValue();
			client.sendToClient("clientVote " + voter.getClientNumber());
			client.sendToClient("ballot " + voter.getChoice());
			client.sendToClient(voter.getBallotProof());
		}
	}

	/**
	 * @definition this method compute HI s that client need for voting
	 */
	public void computeHIs() {
		BigInteger h = new BigInteger("1");
		BigInteger p = cg.getP();
		CommunicateToClient[] allClients = new CommunicateToClient[numberOfClients];
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			String loginId = entry.getKey();
			CommunicateToClient client = entry.getValue();
			allClients[client.getClientNumber()] = client;
		}
		for (int i = 1; i < numberOfClients; i++) {
			h = h.multiply(allClients[i].getPublicKey().modInverse(p)).mod(p);
		}
		allClients[0].setH(h);
		for (int i = 1; i < numberOfClients; i++) {
			h = h.multiply(allClients[i].getPublicKey()).mod(p);
			h = h.multiply(allClients[i - 1].getPublicKey()).mod(p);
			allClients[i].setH(h);
		}
	}

	/**
	 * @definition this method send message to all clients!
	 * @param message
	 */
	private void sendToAll(String message) {
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			CommunicateToClient client = entry.getValue();
			client.sendToClient(message);
		}
	}

	/**
	 * @definition this method verify the proofs of voter!
	 */
	private void verify() {
		for (Entry<String, CommunicateToClient> entry : clients.entrySet()) {
			String loginId = entry.getKey();
			CommunicateToClient client = entry.getValue();
			System.out.println("Voter " + loginId + ":");
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

	/**
	 * @definition this method read question, and add it to the all questions!
	 */
	private void getQuestion() {
		// getting the question!
		System.out.println("Please enter the Question :");
		String question = "";
		while (true) {
			String token = sc.next();
			if (token.charAt(token.length() - 1) == '#') {
				if (token.length() > 1)
					question = question + " "
							+ token.substring(0, token.length() - 1);
				break;
			}
			question = question + " " + token;
		}
		question = question.substring(1);

		// getting number of choices!
		System.out.println("Please enter the number of choices :");
		int numOfSolutions = sc.nextInt();
		numberOfQuestions++;
		Question q = new Question(question, numOfSolutions, numberOfQuestions);

		// getting the choices!
		for (int i = 0; i < numOfSolutions; i++) {
			System.out.println("Please enter choice number " + (i + 1) + ":");
			q.addChoice(sc.next(), i);
		}
		questions.put(numberOfQuestions, q);
		System.out.println("The Question was added.");
	}

	/**
	 * @definition this method add newClient to clients of server
	 * @param newClient
	 */
	public void acceptClient(CommunicateToClient newClient) {
		clients.put(newClient.getLoginId(), newClient);
		newClient.setClientNumber(numberOfClients);
		increaseNumberOfClients();
	}

	private void increaseNumberOfClients() {
		numberOfClients++;
	}

	public Question getQuestionById(int questionId) {
		return questions.get(questionId);
	}

	public int getNumberOfClients() {
		return numberOfClients;
	}

	public CycleGroup getCg() {
		return cg;
	}

	public void setCg(CycleGroup cg) {
		this.cg = cg;
	}

	public boolean isPollingIsOpen() {
		return pollingIsOpen;
	}

	public void setPollingIsOpen(boolean pollingIsOpen) {
		this.pollingIsOpen = pollingIsOpen;
	}
}
