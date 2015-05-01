package question;

public class Question {
	private String question;
	private Choice[] choices;
	private int numberOfChoices;
	private int questionNumber;
	
	public Question(String question, int numberOfSolutions, int questionNumber) {
		this.question = question;
		this.numberOfChoices = numberOfSolutions;
		this.questionNumber = questionNumber;
		choices = new Choice[numberOfSolutions];
	}
	
	public void addChoice(String solution, int solutionNumber){
		Choice nextSolution = new Choice(solutionNumber, solution);
		choices[solutionNumber] = nextSolution;
	}
	
	/**
	 * @definition this method print the question with all choices!
	 */
	public void printTheQuestion() {
		System.out.println("Question :");
		System.out.println(question);
		printAllChoices();
	}
	
	/**
	 * @definition this method print all choices!
	 */
	public void printAllChoices() {
		for (int i = 0; i < choices.length; i++) {
			choices[i].printChoice();
		}
	}
	
	public String getQuestion() {
		return question;
	}
	
	public int getNumberOfChoices() {
		return numberOfChoices;
	}

	public int getQuestionNumber() {
		return questionNumber;
	}

	public Choice[] getChoices() {
		return choices;
	}

}
