package org.hathitrust.htrc.tools.usermanager.validators;

import java.util.regex.Pattern;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class PasswordValidator implements IParameterValidator {

	private static final Pattern PASSWORD_REGEXP = Pattern.compile("^[\\S]{5,30}$");

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!(PASSWORD_REGEXP.matcher(value).matches()))
			throw new ParameterException("The specified password does not pass validation requirements");
	}

}
