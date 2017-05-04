package org.hathitrust.htrc.tools.usermanager;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.extensions.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.user.mgt.stub.types.carbon.*;
import org.wso2.carbon.utils.NetworkUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import org.hathitrust.htrc.tools.usermanager.commands.UserManagerCommands;
import org.hathitrust.htrc.tools.usermanager.exceptions.UserManagerAuthenticationException;
import org.hathitrust.htrc.tools.usermanager.exceptions.UserManagerException;
import org.hathitrust.htrc.tools.usermanager.utils.PermissionType;
import org.hathitrust.htrc.tools.usermanager.utils.ResourceActionPermission;

public class UserManager {

    private static final Logger log = LoggerFactory.getLogger(UserManager.class);
    private static final ResourceActionPermission[] ALL_PERMISSIONS = new ResourceActionPermission[] {
        ResourceActionPermission.GET, ResourceActionPermission.PUT,
        ResourceActionPermission.DELETE, ResourceActionPermission.AUTHORIZE
    };

    private final UserAdminStub _userAdmin;
    private final UserStoreInfo _userStoreInfo;
    private final UserRealmInfo _userRealmInfo;
    private final WSRegistryServiceClient _registry;
    private final ResourceAdminServiceStub _resourceAdmin;
    private final Config _config;
    private final Pattern _userNameRegexp;
    private final Pattern _roleNameRegexp;

    /**
     * Authenticate against a WSO2 G-Reg server and obtain a <code>UserManager</code> instance
     *
     * @param wso2Url The WSO2 G-Reg service URL
     * @param wso2User The username to authenticate to G-Reg (must have admin privileges)
     * @param wso2Password The password for the WSO2 user
     * @param config The HTRC configuration to use
     * @return An instance of <code>UserManager</code>
     * @throws UserManagerException Thrown if the authentication failed or there was an error communicating with the server
     */
    public static UserManager authenticate(String wso2Url, String wso2User, String wso2Password, Config config) throws UserManagerException {
        if (!wso2Url.endsWith("/")) wso2Url += "/";
        try {
            String authAdminEPR = wso2Url + "AuthenticationAdmin";
            String remoteAddress = NetworkUtils.getLocalHostname();

            if (!(config.hasPath(Constants.CONFIG_HTRC_USER_HOME)
                    && config.hasPath(Constants.CONFIG_HTRC_USER_FILES)
                    && config.hasPath(Constants.CONFIG_HTRC_USER_WORKSETS)
                    && config.hasPath(Constants.CONFIG_HTRC_USER_JOBS)))
                throw new UserManagerException("HTRC configuration missing or incomplete");

            ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
            AuthenticationAdminStub adminStub = new AuthenticationAdminStub(configContext, authAdminEPR);
            adminStub._getServiceClient().getOptions().setManageSession(true);
            if (adminStub.login(wso2User, wso2Password, remoteAddress)) {
                String authCookie = (String) adminStub._getServiceClient().getServiceContext().getProperty(HTTPConstants.COOKIE_STRING);
                return new UserManager(wso2Url, configContext, authCookie, config);
            } else
                throw new UserManagerAuthenticationException("Invalid username and/or password");
        }
        catch (Exception e) {
            log.error("Problem performing WSO2 authentiation", e);
            throw new UserManagerException(e);
        }
    }

    private UserManager(String wso2Url, ConfigurationContext configContext, String authCookie, Config config) throws UserManagerException {
        _config = config;
        String userAdminEPR = wso2Url + "UserAdmin";
        String resourceAdminEPR = wso2Url + "ResourceAdminService";
        try {
            _userAdmin = new UserAdminStub(configContext, userAdminEPR);
            createUserAdminClient(authCookie);

            _userRealmInfo = _userAdmin.getUserRealmInfo();
            _userStoreInfo = _userRealmInfo.getPrimaryUserStoreInfo();
            _registry = new WSRegistryServiceClient(wso2Url, authCookie);

            _resourceAdmin = new ResourceAdminServiceStub(configContext, resourceAdminEPR);
            createResourceAdminClient(authCookie);

            _userNameRegexp = Pattern.compile(_userStoreInfo.getUserNameRegEx().replaceAll("\\\\\\\\", "\\\\"));
            _roleNameRegexp = Pattern.compile(_userStoreInfo.getRoleNameRegEx().replaceAll("\\\\\\\\", "\\\\"));
        }
        catch (AxisFault e) {
            log.error("Error creating UserAdminStub", e);
            throw new UserManagerException(e);
        }
        catch (RegistryException e) {
            log.error("Error creating WSRegistryServiceClient", e);
            throw new UserManagerException(e);
        }
        catch (RemoteException e) {
            log.error("Error obtaining the UserStoreInfo instance", e);
            throw new UserManagerException(e);
        }
        catch (UserAdminUserAdminException e) {
            log.error("Error obtaining the UserRealmInfo instance", e);
            throw new UserManagerException(e);
        }
    }

    private void createClient(ServiceClient serviceClient, String sessionCookie) {
        Options option = serviceClient.getOptions();
        option.setManageSession(true);
        option.setProperty(HTTPConstants.COOKIE_STRING, sessionCookie);
    }

    private void createUserAdminClient(String sessionCookie) {
        ServiceClient serviceClient = _userAdmin._getServiceClient();
        createClient(serviceClient, sessionCookie);
    }

    private void createResourceAdminClient(String sessionCookie) {
        ServiceClient serviceClient = _resourceAdmin._getServiceClient();
        createClient(serviceClient, sessionCookie);
    }

    /**
     * Create a new user for the HTRC platform
     *
     * @param userName The user name
     * @param password The user's password
     * @param claims The <code>ClaimValue[]</code> array of profile claims
     * 				 (see <a href="https://htrc3.pti.indiana.edu:9443/carbon/claim-mgt/claim-view.jsp?store=Internal&dialect=http://wso2.org/claims">available claims</a>)
     * @param permissions The array of permission keys to assign to the user, for example: "/permission/admin/login" (can be <code>null</code>)
     * @see #getRequiredUserClaims()
     * @see #getAvailablePermissions()
     * @throws UserManagerException Thrown if an error occurred
     */
    public void createUser(String userName, String password, ClaimValue[] claims, String[] permissions) throws UserManagerException {
        if (userName == null)
            throw new NullPointerException("userName");

        if (password == null)
            throw new NullPointerException("password");

        try {
            if (!(_userNameRegexp.matcher(userName).matches() && _roleNameRegexp.matcher(userName).matches()))
                throw new UserManagerException("Invalid username; Must conform to both of the following regexps: "
                        + _userNameRegexp.pattern() + " and " + _roleNameRegexp.pattern());

            // javadoc: addUser(String userName, String password, String[] roles, ClaimValue[] claims, String profileName)
            _userAdmin.addUser(userName, password, null, claims, "default");
            log.debug("Created user: {}", userName);

            // javadoc: addRole(String roleName, String[] userList, String[] permissions, boolean isSharedRole)
            _userAdmin.addRole(userName, new String[] { userName }, permissions, false);
            log.debug("Created role: {} with permissions: {}", userName, Arrays.toString(permissions));

            String regUserHome = String.format(_config.getString(Constants.CONFIG_HTRC_USER_HOME), userName);
            String regUserFiles = String.format(_config.getString(Constants.CONFIG_HTRC_USER_FILES), userName);
            String regUserWorksets = String.format(_config.getString(Constants.CONFIG_HTRC_USER_WORKSETS), userName);
            String regUserJobs = String.format(_config.getString(Constants.CONFIG_HTRC_USER_JOBS), userName);

            Collection filesCollection = _registry.newCollection();
            String extra = userName.endsWith("s") ? "'" : "'s";
            filesCollection.setDescription(userName + extra + " file space");
            regUserFiles = _registry.put(regUserFiles, filesCollection);
            log.debug("Created user filespace collection: {}", regUserFiles);

            Collection worksetsCollection = _registry.newCollection();
            worksetsCollection.setDescription(userName + extra + " worksets");
            regUserWorksets = _registry.put(regUserWorksets, worksetsCollection);
            log.debug("Created user worksets collection: {}", regUserWorksets);

            Collection jobsCollection = _registry.newCollection();
            jobsCollection.setDescription(userName + extra + " jobs");
            regUserJobs = _registry.put(regUserJobs, jobsCollection);
            log.debug("Created user jobs collection: {}", regUserJobs);

            String everyone = _userRealmInfo.getEveryOneRole();
            for (ResourceActionPermission permission : ALL_PERMISSIONS) {
                // javadoc: addRolePermission(pathToAuthorize, roleToAuthorize, actionToAuthorize, permissionType);
                _resourceAdmin.addRolePermission(regUserHome, userName, permission.toString(), PermissionType.ALLOW.toString());
                _resourceAdmin.addRolePermission(regUserHome, everyone, permission.toString(), PermissionType.DENY.toString());
            }

            _resourceAdmin.addRolePermission(regUserWorksets, everyone, ResourceActionPermission.GET.toString(), PermissionType.ALLOW.toString());

            log.info("User {} created (permissions: {})", userName, Arrays.toString(permissions));
        }
        catch (Exception e) {
            log.error("Error adding new user: " + userName, e);
            throw new UserManagerException("createUser", e);
        }
    }

    /**
     * Delete an existing user
     *
     * @param userName The user name to delete
     * @param deleteHome True to delete the user's home in the registry, False otherwise
     * @throws UserManagerException Thrown if an error occurred
     */
    public void deleteUser(String userName, boolean deleteHome) throws UserManagerException {
        if (userName == null)
            throw new NullPointerException("userName");

        try {
            _userAdmin.deleteUser(userName);
            _userAdmin.deleteRole(userName);

            if (deleteHome) {
                String regUserHome = String.format(_config.getString(Constants.CONFIG_HTRC_USER_HOME), userName);
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

    /**
     * Change a user's password
     *
     * @param userName The user name
     * @param newPassword The new password
     * @throws UserManagerException Thrown if an error occurred
     */
    public void changePassword(String userName, String newPassword) throws UserManagerException {
        if (userName == null)
            throw new NullPointerException("userName");

        if (newPassword == null)
            throw new NullPointerException("newPassword");

        try {
            _userAdmin.changePassword(userName, newPassword);
            log.info("Changed password for user: {}", userName);
        }
        catch (Exception e) {
            log.error("Error changing password for user: " + userName, e);
            throw new UserManagerException("changePassword", e);
        }
    }

    /**
     * Get the list roles (if <code>userName == null</code>, then return all roles, otherwise return user's roles)
     *
     * @param userName The user name (can be <code>null</code>)
     * @return The set of role names
     * @throws UserManagerException Thrown if an error occurred
     */
    public Set<String> getRoles(String userName) throws UserManagerException {
        try {
            Set<String> roles = new HashSet<String>();
            FlaggedName[] roleNames =
                    (userName != null) ?
                        _userAdmin.getRolesOfUser(userName, "*", 100) :
                            _userAdmin.getAllRolesNames("*", Integer.MAX_VALUE);
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

    /**
     * Get the list of user names (matching a filter)
     *
     * @param filter The filter to use when retrieving the user names
     *               ("*" = retrieve all users; "bo*" = retrieve all users whose user name starts with "bo")
     * @return The set of user names
     * @throws UserManagerException Thrown if an error occurred
     */
    public Set<String> getUserNames(String filter) throws UserManagerException {
        if (filter == null)
            filter = "*";

        try {
            String[] users = _userAdmin.listUsers(filter, Integer.MAX_VALUE);
            log.info("Retrieved user list with filter: {}", filter);
            return new HashSet<String>(Arrays.asList(users));
        }
        catch (Exception e) {
            log.error("Error retrieving user list for filter: " + filter, e);
            throw new UserManagerException("listUsers", e);
        }
    }

    /**
     * Get the list of required user claims (expected to be supplied as part of the createUser request)
     *
     * @return The array of required user claims
     * @throws UserManagerException Thrown if an error occurred
     */
    public String[] getRequiredUserClaims() throws UserManagerException {
        try {
            String[] reqClaims = _userRealmInfo.getRequiredUserClaims();
            log.info("Retrieved the required user claims");
            return reqClaims;
        }
        catch (Exception e) {
            log.error("Error retrieving the list of required user claims", e);
            throw new UserManagerException("getRequiredUserClaims", e);
        }
    }

    /**
     * Get the list of available role permissions
     *
     * @return A <code>Map&lt;String,String&gt;</code> of available role permissions,
     *         where the key represents the permission key, and the value provides a
     *         human readable name for the permission
     * @throws UserManagerException Thrown if an error occurred
     */
    public Map<String, String> getAvailablePermissions() throws UserManagerException {
        try {
            UIPermissionNode permRoot = _userAdmin.getAllUIPermissions();
            Map<String, String> permissions = new LinkedHashMap<String, String>();
            buildPermissionsList(permRoot, permissions);

            return permissions;
        }
        catch (Exception e) {
            log.error("Error retrieving the list of required user claims", e);
            throw new UserManagerException("getRequiredUserClaims", e);
        }
    }

    private void buildPermissionsList(UIPermissionNode permNode, Map<String, String> permissions) {
        permissions.put(permNode.getResourcePath(), permNode.getDisplayName());
        if (permNode.getNodeList() != null)
            for (UIPermissionNode node : permNode.getNodeList())
                buildPermissionsList(node, permissions);
    }


    private static final String APP_NAME = System.getProperty("app.name", UserManager.class.getSimpleName());
    private static final String BASEDIR = System.getProperty("basedir", ".");
    public static final String DEFAULT_CONFIG_FILE = System.getProperty("config", BASEDIR + File.separator + "conf" + File.separator + Constants.CONFIG_FILE_NAME);

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
        Config config = ConfigFactory.parseFile(configFile).resolve();

        if (!(config.hasPath("trustStore.store")
                && config.hasPath("trustStore.type"))) {
            log.error("Trust store configuration missing or incomplete for: {}", commands.configFile);
            System.exit(-4);
        }

        String trustStore = config.getString("trustStore.store");
        if (!(trustStore.startsWith(File.separator) || trustStore.indexOf(":") == 1))
            trustStore = BASEDIR + File.separator + trustStore;

        String trustStorePassword = config.getString("trustStore.password");
        String trustStoreType = config.getString("trustStore.type");

        File trustStoreFile = new File(trustStore);
        if (!trustStoreFile.canRead()) {
            log.error("Cannot read trust store file: {}", trustStore);
            System.exit(-5);
        }

        log.debug("Using trust store: {}", trustStore);

        System.setProperty("javax.net.ssl.trustStore", trustStore);
        if (trustStorePassword != null)
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);

        if (!(config.hasPath("wso2.url")
                && config.hasPath("wso2.user")
                && config.hasPath("wso2.password"))) {
            log.error("WSO2 server configuration missing or incomplete for: {}", commands.configFile);
            System.exit(-6);
        }

        String wso2Url = config.getString("wso2.url");
        String wso2User = config.getString("wso2.user");
        String wso2Password = config.getString("wso2.password");

        Config htrcConfig = config.getConfig("htrc");

        try {
            UserManager userManager = UserManager.authenticate(wso2Url, wso2User, wso2Password, htrcConfig);

            userManager.getAvailablePermissions();
            if ("createUser".equals(jc.getParsedCommand())) {
                if (commands.createUserCommand.helpCommand.help) {
                    jc.usage("createUser");

                    System.out.println("Available permissions, for use with the -x option:");
                    Map<String, String> permissions = userManager.getAvailablePermissions();
                    for (Map.Entry<String, String> permission : permissions.entrySet())
                            System.out.println("\t" + permission.getKey() + "   (" + permission.getValue() + ")");

                    System.exit(-1);
                }

                String userName = commands.createUserCommand.userName;
                String password = commands.createUserCommand.password;
                String[] fullName = commands.createUserCommand.fullName.split(" ");
                assert(fullName.length == 2);
                String email = commands.createUserCommand.email;
                String[] permissions = commands.createUserCommand.permissions.toArray(new String[0]);

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
                Set<String> roles = userManager.getRoles(userName);
                for (String role : roles)
                    System.out.println(role);
            }

            else

            if ("listUsers".equals(jc.getParsedCommand())) {
                String filter = commands.listUsersCommand.filter;
                Set<String> users = userManager.getUserNames(filter);
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
