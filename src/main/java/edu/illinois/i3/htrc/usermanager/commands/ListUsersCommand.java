package edu.illinois.i3.htrc.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

@Parameters(commandDescription = "List all users")
public class ListUsersCommand {

	@Parameter(
			names = { "-f", "--filter" },
			description = "The user result filter"
	)
	public String filter = "*";

	@ParametersDelegate
	public final HelpCommand helpCommand = new HelpCommand();

}
