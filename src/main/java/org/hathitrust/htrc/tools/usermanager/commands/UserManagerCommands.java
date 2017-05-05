package org.hathitrust.htrc.tools.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.hathitrust.htrc.tools.usermanager.UserManager;

public class UserManagerCommands {

    @Parameter(
        names = {"-c", "--config"},
        description = "The configuration file to use"
    )
    public String configFile = UserManager.DEFAULT_CONFIG_FILE;

    @ParametersDelegate
    public HelpCommand helpCommand = new HelpCommand();
    public CreateUserCommand createUserCommand = new CreateUserCommand();
    public DeleteUserCommand deleteUserCommand = new DeleteUserCommand();
    public ChangePasswordCommand changePasswordCommand = new ChangePasswordCommand();
    public ListRolesCommand listRolesCommand = new ListRolesCommand();
    public ListUsersCommand listUsersCommand = new ListUsersCommand();

}
