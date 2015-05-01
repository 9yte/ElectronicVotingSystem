package question;

public class Choice {
	private int choiceNumber;
	private String choice;
	
	public Choice(int solutionNumber, String solution) {
		this.choice = solution;
		this.choiceNumber = solutionNumber;
	}

	public int getSolutionNumber() {
		return choiceNumber;
	}
	
	/**
	 * @definition this method print the choice!
	 */
	public void printChoice() {
		System.out.println((choiceNumber + 1) + ". " + choice);
	}
	
	public String getChoice() {
		return choice;
	}
}
