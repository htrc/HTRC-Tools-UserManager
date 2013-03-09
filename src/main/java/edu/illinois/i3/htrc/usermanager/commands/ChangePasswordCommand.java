package edu.illinois.i3.htrc.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import edu.illinois.i3.htrc.usermanager.validators.PasswordValidator;
import edu.illinois.i3.htrc.usermanager.validators.UserNameValidator;

@Parameters(commandDescription = "Change password for user")
public class ChangePasswordCommand {

	@Parameter(
			names = { "-u", "--user" },
			description = "The user whose password should be changed",
			validateWith = UserNameValidator.class,
			required = true
	)
	public String userName;

	@Parameter(
			names = { "-p", "--password" },
			description = "The new password",
			validateWith = PasswordValidator.class,
			password = true,
			required = true
	)
	public String password;

	@ParametersDelegate
	public final HelpCommand helpCommand = new HelpCommand();
}
