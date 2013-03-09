package edu.illinois.i3.htrc.usermanager.validators;

import java.util.regex.Pattern;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class UserNameValidator implements IParameterValidator {

	private static final Pattern USERNAME_REGEXP = Pattern.compile("^[a-zA-Z0-9._-|//]{3,30}$");

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!(USERNAME_REGEXP.matcher(value).matches()))
			throw new ParameterException("Invalid user name - must be min 3 characters " +
					"and contain any combination of letters, numbers, underscore, and period, " +
					"up to 30 characters");
	}

}