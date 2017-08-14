package org.hathitrust.htrc.tools.usermanager.exceptions;

@SuppressWarnings("serial")
public class UserManagerException extends Exception {

    public UserManagerException(String message) {
        super(message);
    }

    public UserManagerException(Throwable throwable) {
        super(throwable);
    }

    public UserManagerException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
