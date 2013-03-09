package edu.illinois.i3.htrc.usermanager.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import edu.illinois.i3.htrc.usermanager.validators.EmailValidator;
import edu.illinois.i3.htrc.usermanager.validators.FullNameValidator;
import edu.illinois.i3.htrc.usermanager.validators.PasswordValidator;
import edu.illinois.i3.htrc.usermanager.validators.UserNameValidator;

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
			description = "The user's permissions (example: login)",
			variableArity = true
	)
	public List<String> permissions = new ArrayList<String>();

	@ParametersDelegate
	public final HelpCommand helpCommand = new HelpCommand();

}
