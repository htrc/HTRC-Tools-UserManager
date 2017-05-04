package org.hathitrust.htrc.tools.usermanager.commands;

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
	public HelpCommand helpCommand = new HelpCommand();

}
