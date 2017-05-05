package org.hathitrust.htrc.tools.usermanager.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import org.hathitrust.htrc.tools.usermanager.validators.PasswordValidator;
import org.hathitrust.htrc.tools.usermanager.validators.UserNameValidator;

@Parameters(commandDescription = "Change password for user")
public class ChangePasswordCommand {

    @Parameter(
        names = {"-u", "--user"},
        description = "The user whose password should be changed",
        validateWith = UserNameValidator.class,
        required = true
    )
    public String userName;

    @Parameter(
        names = {"-p", "--password"},
        description = "The new password",
        validateWith = PasswordValidator.class,
        password = true,
        required = true
    )
    public String password;

    @ParametersDelegate
    public HelpCommand helpCommand = new HelpCommand();
}
