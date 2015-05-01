package client;

import java.math.BigInteger;
import java.security.MessageDigest;

public class AbstractClient {
	private String loginId;
	private int clientNumber;
	private BigInteger publicKey;
	private BigInteger gPrime;
	private BigInteger s;
	private BigInteger choice = BigInteger.ZERO;
	private String ballotProof;
	private boolean isValid = false;
	private boolean voteIsValid = false;
	private BigInteger h;

	public AbstractClient(String loginId, int clientNumber,
			BigInteger publicKey, BigInteger gPrime, BigInteger s) {
		this.loginId = loginId;
		this.clientNumber = clientNumber;
		this.publicKey = publicKey;
		this.gPrime = gPrime;
		this.s = s;
	}
	
	/**
	 * @definition this method check that discrete proof logic is valid or not!
	 * @param splitOfCommands
	 */
	public boolean checkDiscreteLogProof(BigInteger p, BigInteger g) {
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
		if(x.equals(y)){
			isValid = true;
		}
		else{
			System.out.println("wrong!");
		}
		return isValid;
	}

	public String getLoginId() {
		return loginId;
	}

	public int getClientNumber() {
		return clientNumber;
	}

	public BigInteger getPublicKey() {
		return publicKey;
	}

	public BigInteger getgPrime() {
		return gPrime;
	}

	public BigInteger getS() {
		return s;
	}

	public BigInteger getChoice() {
		return choice;
	}

	public void setChoice(BigInteger choice) {
		this.choice = choice;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public BigInteger getH() {
		return h;
	}

	public void setH(BigInteger h) {
		this.h = h;
	}

	public String getBallotProof() {
		return ballotProof;
	}

	public void setBallotProof(String ballotProof) {
		this.ballotProof = ballotProof;
	}

	public boolean isVoteIsValid() {
		return voteIsValid;
	}

	public void setVoteIsValid(boolean voteIsValid) {
		this.voteIsValid = voteIsValid;
	}
}
