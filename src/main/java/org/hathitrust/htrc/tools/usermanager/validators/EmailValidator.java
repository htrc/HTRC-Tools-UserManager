package org.hathitrust.htrc.tools.usermanager.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import java.util.regex.Pattern;

public class EmailValidator implements IParameterValidator {

    private static final Pattern EMAIL_REGEXP = Pattern
        .compile("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$");

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (!EMAIL_REGEXP.matcher(value).matches()) {
            throw new ParameterException(
                "Invalid email address specified - not RFC 5322 compliant.");
        }
    }

}
