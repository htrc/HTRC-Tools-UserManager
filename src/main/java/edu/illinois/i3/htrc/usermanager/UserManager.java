package edu.illinois.i3.htrc.usermanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import net.jmatrix.eproperties.EProperties;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.extensions.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.types.carbon.ClaimValue;
import org.wso2.carbon.user.mgt.stub.types.carbon.FlaggedName;
import org.wso2.carbon.utils.NetworkUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import edu.illinois.i3.htrc.usermanager.commands.UserManagerCommands;
import edu.illinois.i3.htrc.usermanager.exceptions.UserManagerAuthenticationException;
import edu.illinois.i3.htrc.usermanager.exceptions.UserManagerException;
import edu.illinois.i3.htrc.usermanager.utils.PermissionType;
import edu.illinois.i3.htrc.usermanager.utils.ResourceActionPermission;

public class UserManager {

	private static final Logger log = LoggerFactory.getLogger(UserManager.class);
	private static final ResourceActionPermission[] ALL_PERMISSIONS = new ResourceActionPermission[] {
		ResourceActionPermission.GET, ResourceActionPermission.PUT,
		ResourceActionPermission.DELETE, ResourceActionPermission.AUTHORIZE
	};

	private final UserAdminStub _userAdmin;
	private final WSRegistryServiceClient _registry;
	private final ResourceAdminServiceStub _resourceAdmin;
	private final Properties _configProps;

	public static UserManager authenticate(String wso2Url, String wso2User, String wso2Password, ConfigurationContext configContext, Properties configProps) throws UserManagerException {
		if (!wso2Url.endsWith("/")) wso2Url += "/";
		try {
			String authAdminEPR = wso2Url + "AuthenticationAdmin";
			String remoteAddress = NetworkUtils.getLocalHostname();

			if (!(configProps.containsKey(Constants.CONFIG_HTRC_USER_HOME)
					&& configProps.containsKey(Constants.CONFIG_HTRC_USER_FILES)
					&& configProps.containsKey(Constants.CONFIG_HTRC_USER_WORKSETS)))
				throw new UserManagerException("HTRC configuration missing or incomplete");

			AuthenticationAdminStub adminStub = new AuthenticationAdminStub(configContext, authAdminEPR);
			adminStub._getServiceClient().getOptions().setManageSession(true);
			if (adminStub.login(wso2User, wso2Password, remoteAddress)) {
				String authCookie = (String) adminStub._getServiceClient().getServiceContext().getProperty(HTTPConstants.COOKIE_STRING);
				return new UserManager(wso2Url, configContext, authCookie, configProps);
			} else
				throw new UserManagerAuthenticationException("Invalid username and/or password");
		}
		catch (Exception e) {
			log.error("Problem performing WSO2 authentiation", e);
			throw new UserManagerException(e);
		}
	}

	private UserManager(String wso2Url, ConfigurationContext configContext, String authCookie, Properties configProps) throws UserManagerException {
		_configProps = configProps;
		String userAdminEPR = wso2Url + "UserAdmin";
		String resourceAdminEPR = wso2Url + "ResourceAdminService";
		try {
			_userAdmin = new UserAdminStub(configContext, userAdminEPR);
			_registry = new WSRegistryServiceClient(wso2Url, authCookie);
			_resourceAdmin = new ResourceAdminServiceStub(configContext, resourceAdminEPR);
		}
		catch (AxisFault e) {
			log.error("Error creating UserAdminStub", e);
			throw new UserManagerException(e);
		}
		catch (RegistryException e) {
			log.error("Error creating WSRegistryServiceClient", e);
			throw new UserManagerException(e);
		}

    	Options option = _userAdmin._getServiceClient().getOptions();
    	option.setManageSession(true);
    	option.setProperty(HTTPConstants.COOKIE_STRING, authCookie);

    	option = _resourceAdmin._getServiceClient().getOptions();
    	option.setManageSession(true);
    	option.setProperty(HTTPConstants.COOKIE_STRING, authCookie);
	}

	public void createUser(String userName, String password, ClaimValue[] claims, String[] permissions) throws UserManagerException {
		try {
        	// javadoc: addUser(String userName, String password, String[] roles, ClaimValue[] claims, String profileName)
			_userAdmin.addUser(userName, password, null, claims, "default");
			log.debug("Created user: {}", userName);

			// javadoc: addRole(String roleName, String[] userList, String[] permissions)
            _userAdmin.addRole(userName, new String[] { userName }, permissions);
            log.debug("Created role: {} with permissions: {}", userName, Arrays.toString(permissions));

			String regUserHome = String.format(_configProps.getProperty(Constants.CONFIG_HTRC_USER_HOME), userName);
            String regUserFiles = String.format(_configProps.getProperty(Constants.CONFIG_HTRC_USER_FILES), userName);
            String regUserWorksets = String.format(_configProps.getProperty(Constants.CONFIG_HTRC_USER_WORKSETS), userName);

            Collection filesCollection = _registry.newCollection();
            String extra = userName.endsWith("s") ? "'" : "'s";
            filesCollection.setDescription(userName + extra + " file space");
			regUserFiles = _registry.put(regUserFiles, filesCollection);
			log.debug("Created user filespace collection: {}", regUserFiles);

			Collection worksetsCollection = _registry.newCollection();
			worksetsCollection.setDescription(userName + extra + " worksets");
            regUserWorksets = _registry.put(regUserWorksets, worksetsCollection);
            log.debug("Created user worksets collection: {}", regUserWorksets);

            String everyone = "everyone";  //TODO figure out how to get proper name for everyone role
            for (ResourceActionPermission permission : ALL_PERMISSIONS) {
            	// javadoc: addRolePermission(pathToAuthorize, roleToAuthorize, actionToAuthorize, permissionType);
            	_resourceAdmin.addRolePermission(regUserHome, userName, permission.toString(), PermissionType.ALLOW.toString());
            	_resourceAdmin.addRolePermission(regUserHome, everyone, permission.toString(), PermissionType.DENY.toString());
            }

            log.info("User {} created (permissions: {})", userName, Arrays.toString(permissions));
		}
        catch (Exception e) {
			log.error("Error adding new user: " + userName, e);
			throw new UserManagerException("createUser", e);
		}
	}

	public void deleteUser(String userName, boolean deleteHome) throws UserManagerException {
		try {
			_userAdmin.deleteUser(userName);
			_userAdmin.deleteRole(userName);

			if (deleteHome) {
				String regUserHome = String.format(_configProps.getProperty(Constants.CONFIG_HTRC_USER_HOME), userName);
				if (_registry.resourceExists(regUserHome))
					_registry.delete(regUserHome);
			}

			log.info("User {} deleted (deleteHome: {})", userName, deleteHome);
		}
		catch (Exception e) {
			log.error("Error deleting user: " + userName, e);
			throw new UserManagerException("deleteUser", e);
		}
	}

	public void changePassword(String userName, String newPassword) throws UserManagerException {
		try {
			_userAdmin.changePassword(userName, newPassword);
			log.info("Changed password for user: {}", userName);
		}
		catch (Exception e) {
			log.error("Error changing password for user: " + userName, e);
			throw new UserManagerException("changePassword", e);
		}
	}

	public Set<String> listRoles(String userName) throws UserManagerException {
		try {
			Set<String> roles = new HashSet<String>();
			FlaggedName[] roleNames = userName != null ?
					_userAdmin.getRolesOfUser(userName) : _userAdmin.getAllRolesNames();
			for (FlaggedName roleName : roleNames)
				roles.add(roleName.getItemName());

			log.info("Retrieved roles for user: {}", userName);

			return roles;
		}
		catch (Exception e) {
			log.error("Error retrieving role list for user: " + userName, e);
			throw new UserManagerException("listRoles", e);
		}
	}

	public Set<String> listUsers(String filter) throws UserManagerException {
		try {
			String[] users = _userAdmin.listUsers(filter);
			log.info("Retrieved user list with filter: {}", filter);
			return new HashSet<String>(Arrays.asList(users));
		}
		catch (Exception e) {
			log.error("Error retrieving user list for filter: " + filter, e);
			throw new UserManagerException("listUsers", e);
		}
	}

	private static final String APP_NAME = System.getProperty("app.name", UserManager.class.getSimpleName());
	private static final String BASEDIR = System.getProperty("basedir", ".");
	public static final String DEFAULT_CONFIG_FILE = BASEDIR + File.separator + "conf" + File.separator + Constants.CONFIG_FILE_NAME;

	public static void main(String[] args) throws Exception {
		UserManagerCommands commands = new UserManagerCommands();
		JCommander jc = new JCommander(commands);
		jc.setProgramName(APP_NAME);
		jc.addCommand("createUser", commands.createUserCommand);
		jc.addCommand("deleteUser", commands.deleteUserCommand);
		jc.addCommand("changePassword", commands.changePasswordCommand);
		jc.addCommand("listRoles", commands.listRolesCommand);
		jc.addCommand("listUsers", commands.listUsersCommand);

		try {
			jc.parse(args);
			if (commands.helpCommand.help) {
				jc.usage();
				System.exit(-1);
			}

			if (commands.createUserCommand.helpCommand.help) {
				jc.usage("createUser");
				System.exit(-1);
			}

			if (commands.deleteUserCommand.helpCommand.help) {
				jc.usage("deleteUser");
				System.exit(-1);
			}

			if (commands.changePasswordCommand.helpCommand.help) {
				jc.usage("changePassword");
				System.exit(-1);
			}

			if (commands.listRolesCommand.helpCommand.help) {
				jc.usage("listRoles");
				System.exit(-1);
			}

			if (commands.listUsersCommand.helpCommand.help) {
				jc.usage("listUsers");
				System.exit(-1);
			}
		}
		catch (ParameterException e) {
			log.error("Command line parse error", e);
			jc.usage();
			System.exit(-2);
		}

		// Load application properties
		File configFile = new File(commands.configFile);
		EProperties.showVersion = false;
		EProperties properties = new EProperties();
		try {
			properties.load(new FileInputStream(configFile));
		}
		catch (IOException e) {
			log.error("Could not load configuration properties file: " + commands.configFile, e);
			System.exit(-3);
		}

		if (!(properties.containsKey("trustStore->store")
				&& properties.containsKey("trustStore->password")
				&& properties.containsKey("trustStore->type"))) {
			log.error("Trust store configuration missing or incomplete for: {}", commands.configFile);
			System.exit(-4);
		}

		String trustStore = properties.getString("trustStore->store");
		if (!(trustStore.startsWith(File.separator) || trustStore.indexOf(":") == 1))
			trustStore = BASEDIR + File.separator + trustStore;

		String trustStorePassword = properties.getString("trustStore->password");
		String trustStoreType = properties.getString("trustStore->type");

		File trustStoreFile = new File(trustStore);
		if (!trustStoreFile.canRead()) {
			log.error("Cannot read trust store file: {}", trustStore);
			System.exit(-5);
		}

		log.debug("Using trust store: {}", trustStore);

		System.setProperty("javax.net.ssl.trustStore", trustStore);
		System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
		System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);

		if (!(properties.containsKey("wso2->url")
				&& properties.containsKey("wso2->user")
				&& properties.containsKey("wso2->password"))) {
			log.error("WSO2 server configuration missing or incomplete for: {}", commands.configFile);
			System.exit(-6);
		}

		String wso2Url = properties.getString("wso2->url");
		String wso2User = properties.getString("wso2->user");
		String wso2Password = properties.getString("wso2->password");

		Properties htrcProps = properties.getProperties("htrc");

		try {
			ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
			UserManager userManager = UserManager.authenticate(wso2Url, wso2User, wso2Password, configContext, htrcProps);

			if ("createUser".equals(jc.getParsedCommand())) {
				String userName = commands.createUserCommand.userName;
				String password = commands.createUserCommand.password;
				String[] fullName = commands.createUserCommand.fullName.split(" ");
				assert(fullName.length == 2);
				String email = commands.createUserCommand.email;
				String[] permissions = commands.createUserCommand.permissions.toArray(new String[0]);
				for (int i = 0; i < permissions.length; i++)
					permissions[i] = CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION + "/" + permissions[i];

		    	ClaimValue firstNameClaim = new ClaimValue();
		    	firstNameClaim.setClaimURI("http://wso2.org/claims/givenname");
		    	firstNameClaim.setValue(fullName[0]);

		    	ClaimValue lastNameClaim = new ClaimValue();
		    	lastNameClaim.setClaimURI("http://wso2.org/claims/lastname");
		    	lastNameClaim.setValue(fullName[1]);

		    	ClaimValue emailClaim = new ClaimValue();
		    	emailClaim.setClaimURI("http://wso2.org/claims/emailaddress");
		    	emailClaim.setValue(email);

		    	userManager.createUser(userName, password, new ClaimValue[] {
		    			firstNameClaim, lastNameClaim, emailClaim
		    	}, permissions);
			}

			else

			if ("deleteUser".equals(jc.getParsedCommand())) {
				String userName = commands.deleteUserCommand.userName;
				boolean deleteHome = commands.deleteUserCommand.deleteHome;

				userManager.deleteUser(userName, deleteHome);
			}

			else

			if ("changePassword".equals(jc.getParsedCommand())) {
				String userName = commands.changePasswordCommand.userName;
				String newPassword = commands.changePasswordCommand.password;

				userManager.changePassword(userName, newPassword);
			}

			else

			if ("listRoles".equals(jc.getParsedCommand())) {
				String userName = commands.listRolesCommand.userName;
				Set<String> roles = userManager.listRoles(userName);
				for (String role : roles)
					System.out.println(role);
			}

			else

			if ("listUsers".equals(jc.getParsedCommand())) {
				String filter = commands.listUsersCommand.filter;
				Set<String> users = userManager.listUsers(filter);
				for (String user : users)
					System.out.println(user);
			}
		}
		catch (UserManagerException e) {
			System.exit(-7);
		}
		catch (Exception e) {
			log.error("Unhandled error", e);
			System.exit(-7);
		}
	}

}
