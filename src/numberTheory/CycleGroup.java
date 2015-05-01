package numberTheory;
import java.math.BigInteger;
import java.util.Random;


public class CycleGroup {
	private int n = 200;
	private int iteration = 100;
	private BigInteger q;
	private BigInteger p;
	private BigInteger g;
	private BigInteger Zero = new BigInteger("0");
	private BigInteger One = new BigInteger("1");
	private BigInteger Two = new BigInteger("2");
	
	public CycleGroup() {
		find_prime(iteration);
		generator();
	}
	
	/**
	 * @definition this method find a prime number with miller Rabin algorithm
	 * @param iteration
	 * @return
	 */
	public BigInteger find_prime(int iteration) {
		while(true){
			BigInteger r;
			while(true){
				r = Two.pow(n - 1).add(new BigInteger(n, new Random()));
				if(r.compareTo(Two.pow(n)) < 0)
						break;
			}
			if(!isPrime(r, iteration))
				continue;
			BigInteger prime = r.multiply(Two).add(One);
			if(isPrime(prime, iteration)){
				setP(prime);
				setQ(r);
				return prime;
			}
		}
	}
	
	/**
	 * @definition this method create a generator for group Z{P}
	 * @return
	 */
	public BigInteger generator() {
		while(true){
			BigInteger r = new BigInteger(p.bitLength(), new Random()).mod(p);
			if(r.equals(One))
				continue;
			BigInteger check = power(r, q, p);
			if(check.equals(p.subtract(One))){
				g = r;
				return r;
			}
		}
	}
	
	/**
	 * @definition check that number is prime or not with miller rabin method!
	 * @param number
	 * @param iteration
	 * @return
	 */
	public boolean isPrime(BigInteger number, int iteration) {
		if (number.equals(Zero))
			return false;
		else if(number.equals(One))
			return false;
		else if(number.mod(Two).equals(Zero))
			return false;
		else if(number.equals(Two))
			return true;
		
		BigInteger nMinusOne = number.subtract(One);
		BigInteger odd = nMinusOne;
		int log = 1;
		while (odd.mod(Two).equals(Zero)){
			log++;
			odd = odd.divide(Two);
		}
		
		int i = 1;
		while (i <= iteration){
			BigInteger random = new BigInteger(number.bitLength(), new Random()).mod(nMinusOne).add(One);
			BigInteger power = power(random, odd, number);
			BigInteger num = odd;
			int j = 1;
			while(j <= log && !power.equals(One) && !power.equals(nMinusOne)){
				power = (power.multiply(power)).mod(number);
				j++;
			}
			if (!power.equals(nMinusOne) && j > 1)
				return false;
			i++;
		}
		return true;
	}
	
	public BigInteger power(BigInteger a, BigInteger b, BigInteger mod){
		return a.modPow(b, mod);
	}


	public BigInteger getQ() {
		return q;
	}


	public void setQ(BigInteger q) {
		this.q = q;
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
	
}
