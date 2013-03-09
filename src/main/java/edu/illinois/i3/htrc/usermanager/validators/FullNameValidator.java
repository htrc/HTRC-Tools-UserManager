package edu.illinois.i3.htrc.usermanager.validators;

import java.util.regex.Pattern;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class FullNameValidator implements IParameterValidator {

	private static final Pattern FULLNAME_REGEXP = Pattern.compile("^\\w+\\s\\w+$");

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!FULLNAME_REGEXP.matcher(value).matches())
			throw new ParameterException("Invalid full name specified - use \"First Last\" format, for example: \"John Doe\"");
	}

}
