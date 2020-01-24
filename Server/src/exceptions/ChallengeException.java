package exceptions;

public class ChallengeException extends Exception {
    public ChallengeException(String message) {
        super(message);
    }
    public ChallengeException(){
        super();
    }
}
