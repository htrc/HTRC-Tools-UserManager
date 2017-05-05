package org.hathitrust.htrc.tools.usermanager.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import java.util.regex.Pattern;

public class FullNameValidator implements IParameterValidator {

    private static final Pattern FULLNAME_REGEXP = Pattern.compile("^\\w+\\s\\w+$");

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (!FULLNAME_REGEXP.matcher(value).matches()) {
            throw new ParameterException(
                "Invalid full name specified - use \"First Last\" format, for example: \"John Doe\"");
        }
    }

}
