package Settings;

public class Settings {
    public static final int RMIPort=8082;
    public static final int UDPPort=8081;
    public static final int TCPPort=8080;

    /**          COMANDI
     *
     * FORMATO COMANDI:     LOGIN NICK PW
     *                      LOGOUT NICK TOKEN
     *                      SFIDA NICK TOKEN FRIEND
     *                      AMICIZIA NICK TOKEN FRIEND TYPE
     *                      GET NICK TOKEN TYPE
     */
    public enum REQUEST {
        LOGIN,
        LOGOUT,
        SFIDA,
        AMICIZIA,
        GET
    }
    public enum GetType {
        AMICI,
        CLASSIFICA,
        PENDING
    }
    public enum RQTType {
        RICHIEDI,
        ACCETTA,
        RIFIUTA
    }

    /**                     RISPOSTE
     *
     *  Tutte le risposte a un comando sono precedute da OK/NOK
     *  Le risposte asincrone no
     *
     *  FORMATO RISPOSTE:   OK AMICIZIA TOKEN FRIEND TYPE
     *                      OK SFIDA TOKEN NICK TYPE
     *                      OK TOKEN
     *                      NOK TOKEN ECCEZIONE
     *                      AMICI TOKEN JSON
     *                      CLASSIFICA TOKEN JSON
     *                      PENDING TOKEN JSON
     */
    public enum RESPONSE{
        LOGIN,
        AMICIZIA,
        AMICI,
        CLASSIFICA,
        PENDING,
        SFIDA,
        OK,
        NOK
    }
    public enum RSPType{
        RICHIESTA,
        ACCETTATA,
        RIFIUTATA
    }

    /**         CHALLENGE SETTINGS
     *
     */
    public static final int k=10; //numero di parole utilizzate per la sfida
}
