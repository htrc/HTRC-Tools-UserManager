package edu.illinois.i3.htrc.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import edu.illinois.i3.htrc.usermanager.validators.UserNameValidator;

@Parameters(commandDescription = "Delete an existing user")
public class DeleteUserCommand {

	@Parameter(
			names = { "-u", "--user" },
			description = "The user id to delete",
			validateWith = UserNameValidator.class,
			required = true
	)
	public String userName;

	@Parameter(
			names = { "-r", "--remove-home" },
			description = "Remove the user's home from the registry"
	)
	public boolean deleteHome = false;

	@ParametersDelegate
	public final HelpCommand helpCommand = new HelpCommand();

}
