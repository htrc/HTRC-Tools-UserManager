package org.hathitrust.htrc.tools.usermanager.commands;

import com.beust.jcommander.Parameter;

public class HelpCommand {

	@Parameter(
			names = { "-h", "-?", "-help", "--help" },
			description = "Show help menu",
			help = true
	)
	public boolean help;

}
