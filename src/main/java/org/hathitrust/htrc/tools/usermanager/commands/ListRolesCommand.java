package org.hathitrust.htrc.tools.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import org.hathitrust.htrc.tools.usermanager.validators.UserNameValidator;

@Parameters(commandDescription = "List all defined roles")
public class ListRolesCommand {

    @Parameter(
        names = {"-u", "--user"},
        description = "The user whose roles to list",
        validateWith = UserNameValidator.class
    )
    public String userName;

    @ParametersDelegate
    public HelpCommand helpCommand = new HelpCommand();

}
