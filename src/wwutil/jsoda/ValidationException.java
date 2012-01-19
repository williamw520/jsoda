
package wwutil.jsoda;

public class ValidationException extends JsodaException {

    public ValidationException(String msg) {
        super(msg);
    }

    public ValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
