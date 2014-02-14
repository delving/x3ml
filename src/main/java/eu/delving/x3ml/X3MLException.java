package eu.delving.x3ml;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLException extends RuntimeException {

    public X3MLException(String s) {
        super(s);
    }

    public X3MLException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
