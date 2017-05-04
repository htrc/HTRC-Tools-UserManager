package org.hathitrust.htrc.tools.usermanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.hathitrust.htrc.tools.usermanager.validators.EmailValidator;
import org.hathitrust.htrc.tools.usermanager.validators.FullNameValidator;
import org.hathitrust.htrc.tools.usermanager.validators.PasswordValidator;
import org.hathitrust.htrc.tools.usermanager.validators.UserNameValidator;
import org.wso2.carbon.CarbonConstants;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

@Parameters(commandDescription = "Create a new user")
public class CreateUserCommand {

	@Parameter(
			names = { "-u", "--user" },
			description = "The user id",
			validateWith = UserNameValidator.class,
			required = true
	)
	public String userName;

	@Parameter(
			names = { "-p", "--password" },
			description = "The user's password",
			validateWith = PasswordValidator.class,
			password = true,
			required = true
	)
	public String password;

	@Parameter(
			names = { "-n", "--name" },
			description = "The user's full name: \"First Last\"",
			validateWith = FullNameValidator.class,
			required = true
	)
	public String fullName;

	@Parameter(
			names = { "-e", "--email" },
			description = "The user's email address",
			validateWith = EmailValidator.class,
			required = true
	)
	public String email;

	@Parameter(
			names = { "-x", "--permissions" },
			description = "The user's permissions (example: " + CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION + "/login)",
			variableArity = true
	)
	public List<String> permissions = new ArrayList<String>();

	@ParametersDelegate
	public HelpCommand helpCommand = new HelpCommand();

}
