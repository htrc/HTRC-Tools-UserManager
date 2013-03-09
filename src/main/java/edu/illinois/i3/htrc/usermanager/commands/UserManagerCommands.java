package edu.illinois.i3.htrc.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import edu.illinois.i3.htrc.usermanager.UserManager;

public class UserManagerCommands {

	@Parameter(
			names = { "-c", "--config" },
			description = "The configuration file to use"
	)
	public String configFile = UserManager.DEFAULT_CONFIG_FILE;

	@ParametersDelegate
	public final HelpCommand helpCommand = new HelpCommand();
	public final CreateUserCommand createUserCommand = new CreateUserCommand();
	public final DeleteUserCommand deleteUserCommand = new DeleteUserCommand();
	public final ChangePasswordCommand changePasswordCommand = new ChangePasswordCommand();
	public final ListRolesCommand listRolesCommand = new ListRolesCommand();
	public final ListUsersCommand listUsersCommand = new ListUsersCommand();

}
