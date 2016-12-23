/*
 * WebTop Services is a Web Application framework developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.webtop.core.app;

import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.LangUtils;
import com.sonicle.commons.MailUtils;
import com.sonicle.commons.URIUtils;
import com.sonicle.commons.db.DbUtils;
import com.sonicle.security.AuthenticationDomain;
import com.sonicle.security.PasswordUtils;
import com.sonicle.security.auth.DirectoryException;
import com.sonicle.security.auth.DirectoryManager;
import com.sonicle.security.auth.EntryException;
import com.sonicle.security.auth.directory.ADDirectory;
import com.sonicle.security.auth.directory.AbstractDirectory;
import com.sonicle.security.auth.directory.AbstractDirectory.AuthUser;
import com.sonicle.security.auth.directory.DirectoryCapability;
import com.sonicle.security.auth.directory.DirectoryOptions;
import com.sonicle.security.auth.directory.ImapDirectory;
import com.sonicle.security.auth.directory.LdapDirectory;
import com.sonicle.security.auth.directory.LdapNethDirectory;
import com.sonicle.security.auth.directory.SftpDirectory;
import com.sonicle.security.auth.directory.SmbDirectory;
import com.sonicle.webtop.core.CoreServiceSettings;
import com.sonicle.webtop.core.CoreUserSettings;
import com.sonicle.webtop.core.app.auth.LdapWebTopDirectory;
import com.sonicle.webtop.core.app.auth.WebTopDirectory;
import com.sonicle.webtop.core.bol.AssignedGroup;
import com.sonicle.webtop.core.bol.AssignedRole;
import com.sonicle.webtop.core.bol.GroupUid;
import com.sonicle.webtop.core.bol.ODomain;
import com.sonicle.webtop.core.bol.OGroup;
import com.sonicle.webtop.core.bol.ORole;
import com.sonicle.webtop.core.bol.ORoleAssociation;
import com.sonicle.webtop.core.bol.ORolePermission;
import com.sonicle.webtop.core.bol.OUser;
import com.sonicle.webtop.core.bol.OUserAssociation;
import com.sonicle.webtop.core.bol.OUserInfo;
import com.sonicle.webtop.core.bol.UserUid;
import com.sonicle.webtop.core.bol.model.DirectoryUser;
import com.sonicle.webtop.core.bol.model.DomainEntity;
import com.sonicle.webtop.core.bol.model.Role;
import com.sonicle.webtop.core.bol.model.RoleEntity;
import com.sonicle.webtop.core.bol.model.RoleWithSource;
import com.sonicle.webtop.core.bol.model.ServicePermission;
import com.sonicle.webtop.core.bol.model.UserEntity;
import com.sonicle.webtop.core.dal.ActivityDAO;
import com.sonicle.webtop.core.dal.AutosaveDAO;
import com.sonicle.webtop.core.dal.CausalDAO;
import com.sonicle.webtop.core.dal.DAOException;
import com.sonicle.webtop.core.dal.DomainDAO;
import com.sonicle.webtop.core.dal.DomainSettingDAO;
import com.sonicle.webtop.core.dal.GroupDAO;
import com.sonicle.webtop.core.dal.MessageQueueDAO;
import com.sonicle.webtop.core.dal.RoleAssociationDAO;
import com.sonicle.webtop.core.dal.RoleDAO;
import com.sonicle.webtop.core.dal.RolePermissionDAO;
import com.sonicle.webtop.core.dal.ServiceStoreEntryDAO;
import com.sonicle.webtop.core.dal.ShareDAO;
import com.sonicle.webtop.core.dal.ShareDataDAO;
import com.sonicle.webtop.core.dal.SnoozedReminderDAO;
import com.sonicle.webtop.core.dal.SysLogDAO;
import com.sonicle.webtop.core.dal.UserAssociationDAO;
import com.sonicle.webtop.core.dal.UserDAO;
import com.sonicle.webtop.core.dal.UserInfoDAO;
import com.sonicle.webtop.core.dal.UserSettingDAO;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.core.util.IdentifierUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public final class WebTopManager {
	private static final Logger logger = WT.getLogger(WebTopManager.class);
	private static boolean initialized = false;
	
	/**
	 * Initialization method. This method should be called once.
	 * 
	 * @param wta WebTopApp instance.
	 * @return The instance.
	 */
	static synchronized WebTopManager initialize(WebTopApp wta) {
		if(initialized) throw new RuntimeException("Initialization already done");
		WebTopManager instance = new WebTopManager(wta);
		initialized = true;
		logger.info("WebTopManager initialized");
		return instance;
	}
	
	private WebTopApp wta = null;
	public static final String SYSADMIN_PSTRING = ServicePermission.permissionString(ServicePermission.namespacedName(CoreManifest.ID, "SYSADMIN"), "ACCESS", "*");
	public static final String WTADMIN_PSTRING = ServicePermission.permissionString(ServicePermission.namespacedName(CoreManifest.ID, "WTADMIN"), "ACCESS", "*");
	public static final String USERID_ADMIN = "admin";
	public static final String USERID_ADMINS = "admins";
	public static final String USERID_USERS = "users";
	
	private final Object lock1 = new Object();
	private final HashMap<UserProfile.Id, String> cacheUserToUserUid = new HashMap<>();
	private final HashMap<String, UserProfile.Id> cacheUserUidToUser = new HashMap<>();
	private final Object lock2 = new Object();
	private final HashMap<UserProfile.Id, String> cacheGroupToGroupUid = new HashMap<>();
	private final HashMap<String, UserProfile.Id> cacheGroupUidToGroup = new HashMap<>();
	
	private final HashMap<UserProfile.Id, UserProfile.PersonalInfo> cacheUserToPersonalInfo = new HashMap<>();
	private final HashMap<UserProfile.Id, UserProfile.Data> cacheUserToData = new HashMap<>();
	
	/**
	 * Private constructor.
	 * Instances of this class must be created using static initialize method.
	 * @param wta WebTopApp instance.
	 */
	private WebTopManager(WebTopApp wta) {
		this.wta = wta;
		initUserUidCache();
		initGroupUidCache();
	}
	
	/**
	 * Performs cleanup process.
	 */
	void cleanup() {
		wta = null;
		cleanupUserUidCache();
		cleanupGroupUidCache();
		cleanupUserCache();
		logger.info("UserManager destroyed");
	}
	
	public void cleanUserProfileCache(UserProfile.Id pid) {
		removeFromUserCache(pid);
	}
	
	public static String generateSecretKey() {
		return StringUtils.defaultIfBlank(IdentifierUtils.generateSecretKey(), "0123456789101112");
	}
	
	public AbstractDirectory getAuthDirectory(String authUri) throws WTException {
		try {
			return getAuthDirectoryByScheme(new URI(authUri).getScheme());
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid authentication URI [{0}]", authUri);
		}
	}
	
	public AbstractDirectory getAuthDirectory(URI authUri) throws WTException {
		return getAuthDirectoryByScheme(authUri.getScheme());
	}
	
	public AbstractDirectory getAuthDirectoryByScheme(String scheme) throws WTException {
		DirectoryManager dirManager = DirectoryManager.getManager();
		AbstractDirectory directory = dirManager.getDirectory(scheme);
		if(directory == null) throw new WTException("Directory not supported [{0}]", scheme);
		return directory;
	}
	
	public List<ODomain> listDomains(boolean enabledOnly) throws WTException {
		DomainDAO dao = DomainDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			if(enabledOnly) {
				return dao.selectEnabled(con);
			} else {
				return dao.selectAll(con);
			}
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public List<ODomain> listByInternetDomain(String internetDomain) throws WTException {
		DomainDAO dao = DomainDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return dao.selectEnabledByInternetName(con, internetDomain);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public ODomain getDomain(String domainId) throws WTException {
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return getDomain(con, domainId);
			
		} catch(SQLException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private ODomain getDomain(Connection con, String domainId) throws WTException {
		DomainDAO dao = DomainDAO.getInstance();
		
		try {
			return dao.selectById(con, domainId);
			
		} catch(DAOException ex) {
			throw new WTException(ex, "DB error");
		}
	}
	
	public DomainEntity getDomainEntity(String domainId) throws WTException {
		
		ODomain domain = getDomain(domainId);
		try {
			DomainEntity de = new DomainEntity(domain);
			de.setAuthPassword(getDirPassword(domain));
			return de;
			
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid directory URI");
		}
	}
	
	public void initDomainWithDefaults(String domainId) throws WTException {
		ODomain odomain = getDomain(domainId);
		if(odomain == null) throw new WTException("Domain not found [{0}]", domainId);
		
		initDomainWithDefaults(odomain);
	}
	
	private void initDomainWithDefaults(ODomain domain) throws WTException {
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			
			logger.debug("Inserting default groups");
			OGroup ogroup1 = doGroupInsert(con, domain.getDomainId(), USERID_ADMINS, "Admins");
			OGroup ogroup2 = doGroupInsert(con, domain.getDomainId(), USERID_USERS, "Users");
			
			// Update cache
			addToGroupUidCache(new GroupUid(ogroup1.getDomainId(), ogroup1.getUserId(), ogroup1.getUserUid()));
			addToGroupUidCache(new GroupUid(ogroup2.getDomainId(), ogroup2.getUserId(), ogroup2.getUserUid()));
			
			logger.debug("Inserting domain admin");
			UserEntity ue = new UserEntity();
			ue.setDomainId(domain.getDomainId());
			ue.setUserId(USERID_ADMIN);
			ue.setEnabled(true);
			ue.setFirstName("DomainAdmin");
			ue.setLastName(domain.getDescription());
			ue.setDisplayName(ue.getFirstName() + " [" + domain.getDescription() + "]");
			ue.getAssignedGroups().add(new AssignedGroup(WebTopManager.USERID_ADMINS));
			addUser(true, ue);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public ODomain addDomain(DomainEntity domain) throws WTException {
		DomainDAO dodao = DomainDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection(false);
			
			logger.debug("Inserting domain");
			ODomain odomain = new ODomain();
			fillDomain(odomain, domain);
			dodao.insert(con, odomain);
			
			DbUtils.commitQuietly(con);
			return odomain;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void updateDomain(DomainEntity domain) throws WTException {
		DomainDAO dodao = DomainDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection(false);
			
			logger.debug("Updating domain");
			ODomain odomain = new ODomain();
			fillDomain(odomain, domain);
			dodao.update(con, odomain);
			
			DbUtils.commitQuietly(con);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void deleteDomain(String domainId) throws WTException {
		DomainDAO domdao = DomainDAO.getInstance();
		Connection con = null;
		ODomain odomain = null;
		List<OUser> ousers = null;
		
		try {
			con = wta.getConnectionManager().getConnection(false);
			
			odomain = domdao.selectById(con, domainId);
			if(odomain == null) throw new WTException("Domain not found [{0}]", odomain);
			ousers = listUsers(domainId, false);
			
			logger.debug("Deleting domain");
			ActivityDAO.getInstance().deleteByDomain(con, domainId);
			CausalDAO.getInstance().deleteByDomain(con, domainId);
			
			AutosaveDAO.getInstance().deleteByDomain(con, domainId);
			ServiceStoreEntryDAO.getInstance().deleteByDomain(con, domainId);
			SnoozedReminderDAO.getInstance().deleteByDomain(con, domainId);
			MessageQueueDAO.getInstance().deleteByDomain(con, domainId);
			SysLogDAO.getInstance().deleteByDomain(con, domainId);
			
			DomainSettingDAO.getInstance().deleteByDomain(con, domainId);
			UserSettingDAO.getInstance().deleteByDomain(con, domainId);
			
			RoleAssociationDAO.getInstance().deleteByDomain(con, domainId);
			RolePermissionDAO.getInstance().deleteByDomain(con, domainId);
			RoleDAO.getInstance().deleteByDomain(con, domainId);
			ShareDAO.getInstance().deleteByDomain(con, domainId);
			ShareDataDAO.getInstance().deleteByDomain(con, domainId);
			
			UserAssociationDAO.getInstance().deleteByDomain(con, domainId);
			UserInfoDAO.getInstance().deleteByDomain(con, domainId);
			UserDAO.getInstance().deleteByDomain(con, domainId);
			GroupDAO.getInstance().deleteByDomain(con, domainId);
			domdao.deleteById(con, domainId);
			
			DbUtils.commitQuietly(con);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
		
		initUserUidCache();
		initGroupUidCache();
		cleanupUserCache();
		
		try {
			AuthenticationDomain ad = wta.createAuthenticationDomain(odomain);
			AbstractDirectory directory = getAuthDirectory(ad.getAuthUri());
			DirectoryOptions opts = wta.createDirectoryOptions(ad);
			
			if(directory.hasCapability(DirectoryCapability.USERS_WRITE)) {
				for(OUser ouser : ousers) {
					final UserProfile.Id pid = new UserProfile.Id(ouser.getDomainId(), ouser.getUserId());
					directory.deleteUser(opts, pid.getDomainId(), pid.getUserId());
				}
			}
			
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid domain auth URI");
		} catch(DirectoryException ex) {
			throw new WTException(ex, "DirectoryException error");
		}

		//TODO: chiamare controller per eliminare dominio per i servizi
	}
	
	public List<OUser> listUsers(String domainId, boolean enabledOnly) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			if(enabledOnly) {
				return dao.selectEnabledByDomain(con, domainId);
			} else {
				return dao.selectByDomain(con, domainId);
			}
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public OUser getUser(UserProfile.Id pid) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public UserEntity getUserEntity(UserProfile.Id pid) throws WTException {
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return getUserEntity(con, pid);
			
		} catch(SQLException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private UserEntity getUserEntity(Connection con, UserProfile.Id pid) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uassdao = UserAssociationDAO.getInstance();
		RoleAssociationDAO rolassdao = RoleAssociationDAO.getInstance();
		
		try {
			OUser ouser = dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			if(ouser == null) throw new WTException("User not found [{0}]", pid.toString());
			OUserInfo ouseri = uidao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			if(ouseri == null) throw new WTException("User info not found [{0}]", pid.toString());
			
			List<AssignedGroup> assiGroups = uassdao.viewAssignedByUser(con, ouser.getUserUid());
			List<AssignedRole> assiRoles = rolassdao.viewAssignedByUser(con, ouser.getUserUid());
			EntityPermissions perms = extractPermissions(con, ouser.getUserUid());
			
			UserEntity user = new UserEntity(ouser, ouseri);
			user.setAssignedGroups(assiGroups);
			user.setAssignedRoles(assiRoles);
			user.setPermissions(perms.others);
			user.setServicesPermissions(perms.services);
			
			return user;
			
		} catch(DAOException ex) {
			throw new WTException(ex, "DB error");
		}
	}
	
	public void addUser(boolean updateDirectory, UserEntity user) throws WTException {
		addUser(updateDirectory, user, null);
	}
	
	public void addUser(boolean updateDirectory, UserEntity user, char[] password) throws WTException {
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection(false);
			
			ODomain domain = getDomain(user.getDomainId());
			if(domain == null) throw new WTException("Domain not found [{0}]", user.getDomainId());
			
			OUser ouser = null;
			if(updateDirectory) {
				AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
				AbstractDirectory authDir = getAuthDirectory(ad.getAuthUri());
				DirectoryOptions opts = wta.createDirectoryOptions(ad);
				
				if(authDir.hasCapability(DirectoryCapability.USERS_WRITE)) {
					if(!authDir.validateUsername(opts, user.getUserId())) {
						throw new WTException("Username does not satisfy directory requirements [{0}]", ad.getAuthUri().getScheme());
					}
				}
				if(authDir.hasCapability(DirectoryCapability.PASSWORD_WRITE)) {
					if(password == null) {
						password = authDir.generatePassword(opts, domain.getAuthPasswordPolicy());
					} else {
						if(domain.getAuthPasswordPolicy() && !authDir.validatePasswordPolicy(opts, password)) {
							throw new WTException("Password does not satisfy directory requirements [{0}]", ad.getAuthUri().getScheme());
						}
					}
				}
				
				ouser = doUserInsert(con, domain, user);
				
				// Insert user in directory (if necessary)
				if(authDir.hasCapability(DirectoryCapability.USERS_WRITE)) {
					logger.debug("Adding user into directory");
					try {
						authDir.addUser(opts, domain.getDomainId(), createAuthUser(user));
					} catch(EntryException ex1) {
						logger.debug("Skipped: already exists!");
					}
				}
				if(authDir.hasCapability(DirectoryCapability.PASSWORD_WRITE)) {
					logger.debug("Updating its password");
					authDir.updateUserPassword(opts, domain.getDomainId(), user.getUserId(), password);
				}
				
			} else {
				ouser = doUserInsert(con, domain, user);
			}
			
			DbUtils.commitQuietly(con);
			
			// Update cache
			addToUserUidCache(new UserUid(ouser.getDomainId(), user.getUserId(), ouser.getUserUid()));

			// Explicitly sets some important (locale & timezone) user settings to their defaults
			UserProfile.Id pid = new UserProfile.Id(ouser.getDomainId(), ouser.getUserId());
			CoreServiceSettings css = new CoreServiceSettings(CoreManifest.ID, ouser.getDomainId());
			CoreUserSettings cus = new CoreUserSettings(pid);
			cus.setLanguageTag(css.getDefaultLanguageTag());
			cus.setTimezone(css.getDefaultTimezone());
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid domain auth URI");
		} catch(DirectoryException ex) {
			throw new WTException(ex, "DirectoryException error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public CheckUserResult checkUser(UserProfile.Id pid) throws WTException {
		return checkUser(pid.getDomainId(), pid.getUserId());
	}
	
	public CheckUserResult checkUser(String domainId, String userId) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			OUser o = dao.selectByDomainUser(con, domainId, userId);
			return new CheckUserResult(o != null, o != null ? o.getEnabled() : false);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void updateUser(UserEntity user) throws WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID, false);
			
			doUserUpdate(con, user);
			
			DbUtils.commitQuietly(con);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public boolean updateUser(UserProfile.Id pid, boolean enabled) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return dao.updateEnabledByDomainUser(con, pid.getDomainId(), pid.getUserId(), enabled) == 1;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void updateUserPassword(UserProfile.Id pid, char[] oldPassword, char[] newPassword) throws WTException {
				
		try {
			ODomain domain = getDomain(pid.getDomainId());
			if(domain == null) throw new WTException("Domain not found [{0}]", pid.getDomainId());
			
			AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
			AbstractDirectory directory = getAuthDirectory(ad.getAuthUri());
			DirectoryOptions opts = wta.createDirectoryOptions(ad);
			
			if(directory.hasCapability(DirectoryCapability.PASSWORD_WRITE)) {
				if(oldPassword != null) {
					directory.updateUserPassword(opts, pid.getDomainId(), pid.getUserId(), oldPassword, newPassword);
				} else {
					directory.updateUserPassword(opts, pid.getDomainId(), pid.getUserId(), newPassword);
				}
			}
			
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid URI");
		} catch(DirectoryException ex) {
			throw new WTException(ex, "Directory error");
		}
	}
	
	public void deleteUser(UserProfile.Id pid, boolean cleanupDirectory) throws WTException {
		UserDAO udao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		RoleAssociationDAO rolassdao = RoleAssociationDAO.getInstance();
		RolePermissionDAO rpdao = RolePermissionDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID, false);
			
			OUser user = udao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			if(user == null) throw new WTException("User not found [{0}]", pid.toString());
			
			logger.debug("Deleting permissions");
			rpdao.deleteByRole(con, user.getUserUid());
			logger.debug("Deleting groups associations");
			uadao.deleteByUser(con, user.getUserUid());
			logger.debug("Deleting roles associations");
			rolassdao.deleteByUser(con, user.getUserUid());
			logger.debug("Deleting userInfo");
			uidao.deleteByDomainUser(con, pid.getDomainId(), pid.getUserId());
			logger.debug("Deleting user");
			udao.deleteByDomainUser(con, pid.getDomainId(), pid.getUserId());
			
			if(cleanupDirectory) {
				ODomain domain = getDomain(pid.getDomainId());
				if(domain == null) throw new WTException("Domain not found [{0}]", pid.getDomainId());
				
				AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
				AbstractDirectory directory = getAuthDirectory(ad.getAuthUri());
				DirectoryOptions opts = wta.createDirectoryOptions(ad);
				
				if(directory.hasCapability(DirectoryCapability.USERS_WRITE)) {
					directory.deleteUser(opts, pid.getDomainId(), pid.getUserId());
				}
			}
			
			DbUtils.commitQuietly(con);
			
			// Update cache
			removeFromUserUidCache(pid);
			removeFromUserCache(pid);
			
			// Cleanup all user settings ?????????????????????????????????????????????????
			//wta.getSettingsManager().clearUserSettings(pid.getDomainId(), pid.getUserId());
			
			// TODO: chiamare controller per gestire pulizia utente sui servizi
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "Invalid URI");
		} catch(DirectoryException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "Directory error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public List<DirectoryUser> listDirectoryUsers(ODomain domain) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
			AbstractDirectory directory = getAuthDirectory(ad.getAuthUri());
			DirectoryOptions opts = wta.createDirectoryOptions(ad);
			
			con = wta.getConnectionManager().getConnection();
			Map<String, OUser> wtUsers = dao.selectByDomain2(con, domain.getDomainId());
			
			ArrayList<DirectoryUser> items = new ArrayList<>();
			
			if(directory.hasCapability(DirectoryCapability.USERS_READ)) {
				for(AuthUser userEntry : directory.listUsers(opts, domain.getDomainId())) {
					items.add(new DirectoryUser(domain.getDomainId(), userEntry, wtUsers.get(userEntry.userId)));
				}
				
			} else {
				for(OUser ouser : wtUsers.values()) {
					final AbstractDirectory.AuthUser userEntry = new AbstractDirectory.AuthUser(ouser.getUserId(), null, null, ouser.getDisplayName(), null);
					items.add(new DirectoryUser(domain.getDomainId(), userEntry, ouser));
				}
			}
			
			return items;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid URI");
		} catch(DirectoryException ex) {
			throw new WTException(ex, "Directory error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public boolean updateUserDisplayName(UserProfile.Id pid, String displayName) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return dao.updateDisplayNameByDomainUser(con, pid.getDomainId(), pid.getUserId(), displayName) == 1;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public UserProfile.PersonalInfo getUserPersonalInfo(UserProfile.Id pid) throws WTException {
		UserInfoDAO dao = UserInfoDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			OUserInfo oui = dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			return (oui == null) ? null : new UserProfile.PersonalInfo(oui);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public boolean updateUserPersonalInfo(UserProfile.Id pid, UserProfile.PersonalInfo userPersonalInfo) throws WTException {
		UserInfoDAO dao = UserInfoDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			OUserInfo oui = createUserInfo(userPersonalInfo);
			oui.setDomainId(pid.getDomainId());
			oui.setUserId(pid.getUserId());
			return dao.update(con, oui) == 1;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public List<OGroup> listGroups(String domainId) throws WTException {
		GroupDAO dao = GroupDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			return dao.selectByDomain(con, domainId);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	/**
	 * Lists domain real roles (those defined as indipendent role).
	 * @param domainId The domain ID.
	 * @return
	 * @throws WTException 
	 */
	public List<Role> listRoles(String domainId) throws WTException {
		RoleDAO dao = RoleDAO.getInstance();
		ArrayList<Role> items = new ArrayList<>();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			
			List<ORole> roles = dao.selectByDomain(con, domainId);
			for(ORole erole : roles) items.add(new Role(erole));
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
		return items;
	}
	
	/**
	 * Lists domain users roles (those coming from a user).
	 * @param domainId The domain ID.
	 * @return
	 * @throws WTException 
	 */
	public List<Role> listUsersRoles(String domainId) throws WTException {
		UserDAO dao = UserDAO.getInstance();
		ArrayList<Role> items = new ArrayList<>();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			
			List<OUser> users = dao.selectEnabledByDomain(con, domainId);
			for(OUser user: users) items.add(new Role(user));
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
		return items;
	}
	
	/**
	 * Lists domain groups roles (those coming from a group).
	 * @param domainId The domain ID.
	 * @return
	 * @throws WTException 
	 */
	public List<Role> listGroupsRoles(String domainId) throws WTException {
		GroupDAO dao = GroupDAO.getInstance();
		ArrayList<Role> items = new ArrayList<>();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			
			List<OGroup> groups = dao.selectByDomain(con, domainId);
			for(OGroup group: groups) items.add(new Role(group));
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
		return items;
	}
	
	public List<AssignedRole> listAssignedRoles(String userUid) throws WTException {
		RoleAssociationDAO rolassdao = RoleAssociationDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			return rolassdao.viewAssignedByUser(con, userUid);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	/**
	 * Retrieves the domain ID for the specified role.
	 * @param uid
	 * @return
	 * @throws WTException 
	 */
	public String getRoleDomain(String uid) throws WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			
			RoleDAO roldao = RoleDAO.getInstance();
			ORole role = roldao.selectByUid(con, uid);
			if(role != null) return role.getDomainId();
			
			UserDAO usedao = UserDAO.getInstance();
			OUser user = usedao.selectByUid(con, uid);
			if(user != null) return user.getDomainId();
			
			GroupDAO grpdao = GroupDAO.getInstance();
			OGroup group = grpdao.selectByUid(con, uid);
			if(group != null) return group.getDomainId();
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
		return null;
	}
	
	public RoleEntity getRole(String uid) throws WTException {
		RoleDAO roldao = RoleDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			
			ORole orole = roldao.selectByUid(con, uid);
			if(orole == null) throw new WTException("Role not found [{0}]", uid);
			
			EntityPermissions perms = extractPermissions(con, uid);
			RoleEntity role = new RoleEntity(orole);
			role.setPermissions(perms.others);
			role.setServicesPermissions(perms.services);
			
			return role;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void addRole(RoleEntity role) throws WTException {
		RoleDAO roldao = RoleDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID, false);
			
			ORole orole = new ORole();
			orole.setRoleUid(IdentifierUtils.getUUID());
			orole.setDomainId(role.getDomainId());
			orole.setName(role.getName());
			orole.setDescription(role.getDescription());
			roldao.insert(con, orole);
			
			for(ORolePermission perm : role.getPermissions()) {
				doInsertPermission(con, orole.getRoleUid(), perm.getServiceId(), perm.getKey(), perm.getAction(), "*");
			}
			for(ORolePermission perm : role.getServicesPermissions()) {
				doInsertPermission(con, orole.getRoleUid(), CoreManifest.ID, "SERVICE", ServicePermission.ACTION_ACCESS, perm.getInstance());
			}
			
			DbUtils.commitQuietly(con);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void updateRole(RoleEntity role) throws WTException {
		RoleDAO roldao = RoleDAO.getInstance();
		RolePermissionDAO rolperdao = RolePermissionDAO.getInstance();
		Connection con = null;
		
		try {
			RoleEntity oldRole = getRole(role.getRoleUid());
			if(oldRole == null) throw new WTException("Role not found [{0}]", role.getRoleUid());
			
			con = WT.getConnection(CoreManifest.ID, false);
			
			ORole orole = new ORole();
			orole.setRoleUid(role.getRoleUid());
			orole.setName(role.getName());
			orole.setDescription(role.getDescription());
			roldao.update(con, orole);
			
			LangUtils.CollectionChangeSet<ORolePermission> changeSet1 = LangUtils.getCollectionChanges(oldRole.getPermissions(), role.getPermissions());
			for(ORolePermission perm : changeSet1.deleted) {
				rolperdao.deleteById(con, perm.getRolePermissionId());
			}
			for(ORolePermission perm : changeSet1.inserted) {
				doInsertPermission(con, oldRole.getRoleUid(), perm.getServiceId(), perm.getKey(), perm.getAction(), "*");
			}
			
			LangUtils.CollectionChangeSet<ORolePermission> changeSet2 = LangUtils.getCollectionChanges(oldRole.getServicesPermissions(), role.getServicesPermissions());
			for(ORolePermission perm : changeSet2.deleted) {
				rolperdao.deleteById(con, perm.getRolePermissionId());
			}
			for(ORolePermission perm : changeSet2.inserted) {
				doInsertPermission(con, oldRole.getRoleUid(), CoreManifest.ID, "SERVICE", ServicePermission.ACTION_ACCESS, perm.getInstance());
			}
			
			DbUtils.commitQuietly(con);
		
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void deleteRole(String uid) throws WTException {
		RoleDAO roldao = RoleDAO.getInstance();
		RoleAssociationDAO rolassdao = RoleAssociationDAO.getInstance();
		RolePermissionDAO rolperdao = RolePermissionDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID, false);
			
			roldao.deleteByUid(con, uid);
			rolassdao.deleteByRole(con, uid);
			rolperdao.deleteByRole(con, uid);
			DbUtils.commitQuietly(con);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public List<String> getComputedRolesAsStringByUser(UserProfile.Id pid, boolean self, boolean transitive) throws WTException {
		ArrayList<String> uids = new ArrayList<>();
		Set<RoleWithSource> roles = getComputedRolesByUser(pid, self, transitive);
		for(RoleWithSource role : roles) {
			uids.add(role.getRoleUid());
		}
		return uids;
	}
	
	public Set<RoleWithSource> getComputedRolesByUser(UserProfile.Id pid, boolean self, boolean transitive) throws WTException {
		WebTopManager usrm = wta.getWebTopManager();
		Connection con = null;
		HashSet<String> roleMap = new HashSet<>();
		LinkedHashSet<RoleWithSource> roles = new LinkedHashSet<>();
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			String userUid = usrm.userToUid(pid);
			
			if(self) {
				UserDAO usedao = UserDAO.getInstance();
				OUser user = usedao.selectByUid(con, userUid);
				roles.add(new RoleWithSource(RoleWithSource.SOURCE_USER, userUid, user.getDomainId(), pid.getUserId(), user.getDisplayName()));
			}
			
			RoleDAO roldao = RoleDAO.getInstance();
			
			// Gets by group
			List<ORole> groles = roldao.selectFromGroupsByUser(con, userUid);
			for(ORole role : groles) {
				if(roleMap.contains(role.getRoleUid())) continue; // Skip duplicates
				roleMap.add(role.getRoleUid());
				roles.add(new RoleWithSource(RoleWithSource.SOURCE_GROUP, role.getRoleUid(), role.getDomainId(), role.getName(), role.getDescription()));
			}
			
			// Gets direct assigned roles
			List<ORole> droles = roldao.selectDirectByUser(con, userUid);
			for(ORole role : droles) {
				if(roleMap.contains(role.getRoleUid())) continue; // Skip duplicates
				roleMap.add(role.getRoleUid());
				roles.add(new RoleWithSource(RoleWithSource.SOURCE_ROLE, role.getRoleUid(), role.getDomainId(), role.getName(), role.getDescription()));
			}
			
			// Get transivite roles (belonging to groups)
			if(transitive) {
				List<ORole> troles = roldao.selectTransitiveFromGroupsByUser(con, userUid);
				for(ORole role : troles) {
					if(roleMap.contains(role.getRoleUid())) continue; // Skip duplicates
					roleMap.add(role.getRoleUid());
					roles.add(new RoleWithSource(RoleWithSource.SOURCE_TRANSITIVE, role.getRoleUid(), role.getDomainId(), role.getName(), role.getDescription()));
				}
			}
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
		return roles;
	}
	
	public List<ORolePermission> listRolePermissions(String roleUid) throws Exception {
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID);
			RolePermissionDAO dao = RolePermissionDAO.getInstance();
			return dao.selectByRoleUid(con, roleUid);
		
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public EntityPermissions extractPermissions(Connection con, String roleUid) throws WTException {
		RolePermissionDAO rolperdao = RolePermissionDAO.getInstance();
		
		List<ORolePermission> operms = rolperdao.selectByRoleUid(con, roleUid);
		ArrayList<ORolePermission> othersPerms = new ArrayList<>();
		ArrayList<ORolePermission> servicesPerms = new ArrayList<>();
		for(ORolePermission operm : operms) {
			if(operm.getInstance().equals("*")) {
				othersPerms.add(operm);
			} else {
				if(operm.getServiceId().equals(CoreManifest.ID) && operm.getKey().equals("SERVICE") && operm.getAction().equals("ACCESS")) {
					servicesPerms.add(operm);
				}
			}
		}
		
		return new EntityPermissions(othersPerms, servicesPerms);
	}
	
	public UserProfile.PersonalInfo userPersonalInfo(UserProfile.Id pid) throws WTException {
		synchronized(cacheUserToPersonalInfo) {
			if(!cacheUserToPersonalInfo.containsKey(pid)) {
				UserProfile.PersonalInfo upi = getUserPersonalInfo(pid);
				if(upi == null) throw new WTException("UserPersonalInfo not found [{0}]", pid.toString());
				cacheUserToPersonalInfo.put(pid, upi);
				return upi;
			} else {
				return cacheUserToPersonalInfo.get(pid);
			}
		}
	}
	
	public UserProfile.Data userData(UserProfile.Id pid) throws WTException {
		synchronized(cacheUserToData) {
			if(!cacheUserToData.containsKey(pid)) {
				UserProfile.Data ud = getUserData(pid);
				cacheUserToData.put(pid, ud);
				return ud;
			} else {
				return cacheUserToData.get(pid);
			}
		}
	}
	
	public String userToUid(UserProfile.Id pid) {
		synchronized(lock1) {
			if(!cacheUserToUserUid.containsKey(pid)) throw new WTRuntimeException("[userToUidCache] Cache miss on key {0}", pid.toString());
			return cacheUserToUserUid.get(pid);
		}
	}
	
	public UserProfile.Id uidToUser(String uid) {
		synchronized(lock1) {
			if(!cacheUserUidToUser.containsKey(uid)) throw new WTRuntimeException("[uidToUserCache] Cache miss on key {0}", uid);
			return cacheUserUidToUser.get(uid);
		}
	}
	
	public String groupToUid(UserProfile.Id pid) {
		synchronized(lock2) {
			if(!cacheGroupToGroupUid.containsKey(pid)) throw new WTRuntimeException("[groupToUidCache] Cache miss on key {0}", pid.toString());
			return cacheGroupToGroupUid.get(pid);
		}
	}
	
	public UserProfile.Id uidToGroup(String uid) {
		synchronized(lock2) {
			if(!cacheGroupUidToGroup.containsKey(uid)) throw new WTRuntimeException("[uidToGroupCache] Cache miss on key {0}", uid);
			return cacheGroupUidToGroup.get(uid);
		}
	}
	
	public String getInternetUserId(UserProfile.Id pid) throws WTException {
		ODomain domain = getDomain(pid.getDomainId());
		return new UserProfile.Id(domain.getInternetName(), pid.getUserId()).toString();
	}
	
	public String getDomainInternetName(String domainId) throws WTException {
		ODomain domain = getDomain(domainId);
		return domain.getInternetName();
	}
	
	
	
	private OGroup doGroupInsert(Connection con, String domainId, String groupId, String displayName) throws DAOException, WTException {
		GroupDAO gdao = GroupDAO.getInstance();
		
		logger.debug("Inserting group [{}]", groupId);
		OGroup ogroup = new OGroup();
		ogroup.setDomainId(domainId);
		ogroup.setUserId(groupId);
		ogroup.setEnabled(true);
		ogroup.setUserUid(IdentifierUtils.getUUID());
		ogroup.setDisplayName(displayName);
		ogroup.setSecret(null);
		gdao.insert(con, ogroup);
		
		return ogroup;
	}
	
	
	private AuthUser createAuthUser(UserEntity user) {
		return new AuthUser(user.getUserId(), user.getFirstName(), user.getLastName(), user.getDisplayName(), null);
	}
	
	private OUser doUserInsert(Connection con, ODomain domain, UserEntity user) throws DAOException, WTException {
		UserDAO udao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		
		InternetAddress email = MailUtils.buildInternetAddress(user.getUserId(), domain.getInternetName(), null);
		if(email == null) throw new WTException("Cannot create a valid email address [{0}, {1}]", user.getUserId(), domain.getInternetName());
		
		// Insert User record
		logger.debug("Inserting user");
		OUser ouser = new OUser();
		ouser.setDomainId(domain.getDomainId());
		ouser.setUserId(user.getUserId());
		ouser.setEnabled(user.getEnabled());
		ouser.setUserUid(IdentifierUtils.getUUID());
		ouser.setDisplayName(user.getDisplayName());
		ouser.setSecret(generateSecretKey());
		udao.insert(con, ouser);
		
		// Insert UserInfo record
		logger.debug("Inserting userInfo");
		OUserInfo oui = new OUserInfo();
		oui.setDomainId(domain.getDomainId());
		oui.setUserId(user.getUserId());
		oui.setFirstName(user.getFirstName());
		oui.setLastName(user.getLastName());
		oui.setEmail(email.getAddress());
		uidao.insert(con, oui);
		
		logger.debug("Inserting groups associations");
		for(AssignedGroup assiGroup : user.getAssignedGroups()) {
			final String groupUid = groupToUid(new UserProfile.Id(user.getDomainId(), assiGroup.getGroupId()));
			doInsertUserAssociation(con, ouser.getUserUid(), groupUid);
			
			/*
			final OUserAssociation oua = new OUserAssociation();
			final String groupUid = groupToUid(new UserProfile.Id(user.getDomainId(), assiGroup.getGroupId()));
			oua.setUserAssociationId(uadao.getSequence(con).intValue());
			oua.setUserUid(ouser.getUserUid());
			oua.setGroupUid(groupUid);
			uadao.insert(con, oua);
			*/
		}
		
		logger.debug("Inserting roles associations");
		for(AssignedRole assiRole : user.getAssignedRoles()) {
			doInsertRoleAssociation(con, ouser.getUserUid(), assiRole.getRoleUid());
		}
		
		// Insert permissions
		logger.debug("Inserting permissions");
		for(ORolePermission perm : user.getPermissions()) {
			doInsertPermission(con, ouser.getUserUid(), perm.getServiceId(), perm.getKey(), perm.getAction(), "*");
		}
		for(ORolePermission perm : user.getServicesPermissions()) {
			doInsertPermission(con, ouser.getUserUid(), CoreManifest.ID, "SERVICE", ServicePermission.ACTION_ACCESS, perm.getInstance());
		}
		
		return ouser;
	}
	
	private void doUserUpdate(Connection con, UserEntity user) throws DAOException, WTException {
		UserDAO udao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		RoleAssociationDAO rolassdao = RoleAssociationDAO.getInstance();
		RolePermissionDAO rpdao = RolePermissionDAO.getInstance();
		
		UserEntity oldUser = getUserEntity(con, user.getProfileId());
		if(oldUser == null) throw new WTException("User not found [{0}]", user.getProfileId().toString());
		
		logger.debug("Updating user");
		OUser ouser = new OUser();
		ouser.setDomainId(user.getDomainId());
		ouser.setUserId(user.getUserId());
		ouser.setEnabled(user.getEnabled());
		ouser.setDisplayName(user.getDisplayName());
		udao.updateEnabledDisplayName(con, ouser);

		logger.debug("Updating userInfo");
		OUserInfo ouseri = new OUserInfo();
		ouseri.setDomainId(user.getDomainId());
		ouseri.setUserId(user.getUserId());
		ouseri.setFirstName(user.getFirstName());
		ouseri.setLastName(user.getLastName());
		uidao.updateFirstLastName(con, ouseri);
		
		logger.debug("Updating groups associations");
		LangUtils.CollectionChangeSet<AssignedGroup> changeSet1 = LangUtils.getCollectionChanges(oldUser.getAssignedGroups(), user.getAssignedGroups());
		for(AssignedGroup assiGroup : changeSet1.deleted) {
			uadao.deleteById(con, assiGroup.getUserAssociationId());
		}
		for(AssignedGroup assiGroup : changeSet1.inserted) {
			final String groupUid = groupToUid(new UserProfile.Id(user.getDomainId(), assiGroup.getGroupId()));
			doInsertUserAssociation(con, oldUser.getUserUid(), groupUid);
			/*
			final OUserAssociation oua = new OUserAssociation();
			final String groupUid = groupToUid(new UserProfile.Id(user.getDomainId(), assiGroup.getGroupId()));
			oua.setUserAssociationId(uadao.getSequence(con).intValue());
			oua.setUserUid(oldUser.getUserUid());
			oua.setGroupUid(groupUid);
			uadao.insert(con, oua);
			*/
		}
		
		logger.debug("Updating roles associations");
		LangUtils.CollectionChangeSet<AssignedRole> changeSet2 = LangUtils.getCollectionChanges(oldUser.getAssignedRoles(), user.getAssignedRoles());
		for(AssignedRole assiRole : changeSet2.deleted) {
			rolassdao.deleteById(con, assiRole.getRoleAssociationId());
		}
		for(AssignedRole assiRole : changeSet2.inserted) {
			doInsertRoleAssociation(con, oldUser.getUserUid(), assiRole.getRoleUid());
		}

		logger.debug("Updating permissions");
		LangUtils.CollectionChangeSet<ORolePermission> changeSet3 = LangUtils.getCollectionChanges(oldUser.getPermissions(), user.getPermissions());
		for(ORolePermission perm : changeSet3.deleted) {
			rpdao.deleteById(con, perm.getRolePermissionId());
		}
		for(ORolePermission perm : changeSet3.inserted) {
			doInsertPermission(con, oldUser.getUserUid(), perm.getServiceId(), perm.getKey(), perm.getAction(), "*");
		}

		LangUtils.CollectionChangeSet<ORolePermission> changeSet4 = LangUtils.getCollectionChanges(oldUser.getServicesPermissions(), user.getServicesPermissions());
		for(ORolePermission perm : changeSet4.deleted) {
			rpdao.deleteById(con, perm.getRolePermissionId());
		}
		for(ORolePermission perm : changeSet4.inserted) {
			doInsertPermission(con, oldUser.getUserUid(), CoreManifest.ID, "SERVICE", ServicePermission.ACTION_ACCESS, perm.getInstance());
		}
	}
	
	private void fillDomain(ODomain o, DomainEntity domain) throws WTException {
		String scheme = null;
		
		o.setDomainId(domain.getDomainId());
		o.setInternetName(domain.getInternetName());
		o.setEnabled(domain.getEnabled());
		o.setDescription(domain.getDescription());
		o.setUserAutoCreation(domain.getUserAutoCreation());
		o.setAuthUri(domain.getAuthUri());
		
		try {
			scheme = URIUtils.getScheme(domain.getAuthUri());
		} catch(URISyntaxException ex) {
			throw new WTException("Invalid directory URI [{0}]", domain.getAuthUri());
		}
		
		if (scheme.equals(WebTopDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(null);
			o.setAuthUsername(null);
			o.setAuthPassword(null);
			o.setAuthPasswordPolicy(domain.getAuthPasswordPolicy());
		} else if (scheme.equals(LdapWebTopDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getAuthConnSecurity()));
			o.setAuthUsername(domain.getAuthUsername());
			setDirPassword(o, domain.getAuthPassword());
			o.setAuthPasswordPolicy(domain.getAuthPasswordPolicy());
		} else if (scheme.equals(LdapDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getAuthConnSecurity()));
			o.setAuthUsername(domain.getAuthUsername());
			setDirPassword(o, domain.getAuthPassword());
			o.setAuthPasswordPolicy(false);
		} else if (scheme.equals(ImapDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getAuthConnSecurity()));
			o.setAuthUsername(null);
			o.setAuthPassword(null);
			o.setAuthPasswordPolicy(false);
		} else if (scheme.equals(SmbDirectory.SCHEME) || scheme.equals(SftpDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(null);
			o.setAuthUsername(null);
			o.setAuthPassword(null);
			o.setAuthPasswordPolicy(false);
		} else if (scheme.equals(ADDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getAuthConnSecurity()));
			o.setAuthUsername(domain.getAuthUsername());
			setDirPassword(o, domain.getAuthPassword());
			o.setAuthPasswordPolicy(domain.getAuthPasswordPolicy());
		} else if (scheme.equals(LdapNethDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getAuthConnSecurity()));
			o.setAuthUsername(domain.getAuthUsername());
			setDirPassword(o, domain.getAuthPassword());
			o.setAuthPasswordPolicy(false);
		}
		o.setAuthCaseSensitive(domain.getAuthCaseSensitive());
	}
	
	private OUserAssociation doInsertUserAssociation(Connection con, String userUid, String groupUid) throws WTException {
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		
		OUserAssociation oua = new OUserAssociation();
		oua.setUserAssociationId(uadao.getSequence(con).intValue());
		oua.setUserUid(userUid);
		oua.setGroupUid(groupUid);
		uadao.insert(con, oua);
		return oua;
	}
	
	private ORoleAssociation doInsertRoleAssociation(Connection con, String userUid, String roleUid) throws WTException {
		RoleAssociationDAO rolassdao = RoleAssociationDAO.getInstance();
		
		ORoleAssociation ora = new ORoleAssociation();
		ora.setRoleAssociationId(rolassdao.getSequence(con).intValue());
		ora.setUserUid(userUid);
		ora.setRoleUid(roleUid);
		rolassdao.insert(con, ora);
		return ora;
	}
	
	private ORolePermission doInsertPermission(Connection con, String roleUid, String serviceId, String key, String action, String instance) throws WTException {
		RolePermissionDAO rpdao = RolePermissionDAO.getInstance();
		
		ORolePermission perm = new ORolePermission();
		perm.setRolePermissionId(rpdao.getSequence(con).intValue());
		perm.setRoleUid(roleUid);
		perm.setServiceId(serviceId);
		perm.setKey(key);
		perm.setAction(action);
		perm.setInstance(instance);
		
		rpdao.insert(con, perm);
		return perm;
	}
	
	private UserProfile.Data getUserData(UserProfile.Id pid) throws WTException {
		CoreUserSettings cus = new CoreUserSettings(pid);
		UserProfile.PersonalInfo upi = userPersonalInfo(pid);
		OUser ouser = getUser(pid);
		if(ouser == null) throw new WTException("User not found [{0}]", pid.toString());

		InternetAddress ia = MailUtils.buildInternetAddress(upi.getEmail(), ouser.getDisplayName());
		return new UserProfile.Data(ouser.getDisplayName(), cus.getLanguageTag(), cus.getTimezone(), ia);
	}
	
	private OUserInfo createUserInfo(UserProfile.PersonalInfo upi) {
		OUserInfo oui = new OUserInfo();
		oui.setTitle(upi.getTitle());
		oui.setFirstName(upi.getFirstName());
		oui.setLastName(upi.getLastName());
		oui.setNickname(upi.getNickname());
		oui.setGender(upi.getGender());
		oui.setEmail(upi.getEmail());
		oui.setTelephone(upi.getTelephone());
		oui.setFax(upi.getFax());
		oui.setPager(upi.getPager());
		oui.setMobile(upi.getMobile());
		oui.setAddress(upi.getAddress());
		oui.setCity(upi.getCity());
		oui.setPostalCode(upi.getPostalCode());
		oui.setState(upi.getState());
		oui.setCountry(upi.getCountry());
		oui.setCompany(upi.getCompany());
		oui.setFunction(upi.getFunction());
		oui.setCustom1(upi.getCustom01());
		oui.setCustom2(upi.getCustom02());
		oui.setCustom3(upi.getCustom03());
		return oui;
	}
	
	private String getDirPassword(ODomain o) {
		return PasswordUtils.decryptDES(o.getAuthPassword(), new String(new char[]{'p','a','s','s','w','o','r','d'}));
	}
	
	private void setDirPassword(ODomain o, String password) {
		o.setAuthPassword(PasswordUtils.encryptDES(password, new String(new char[]{'p','a','s','s','w','o','r','d'})));
	}
	
	private void cleanupUserCache() {
		synchronized(cacheUserToData) {
			cacheUserToData.clear();
		}
		synchronized(cacheUserToPersonalInfo) {
			cacheUserToPersonalInfo.clear();
		}
	}
	
	private void addToUserCache(UserProfile.Id pid, UserProfile.Data userData) {
		synchronized(cacheUserToData) {
			cacheUserToData.put(pid, userData);
		}
	}
	
	private void addToUserCache(UserProfile.Id pid, UserProfile.PersonalInfo userPersonalInfo) {
		synchronized(cacheUserToPersonalInfo) {
			cacheUserToPersonalInfo.put(pid, userPersonalInfo);
		}
	}
	
	private void removeFromUserCache(UserProfile.Id pid) {
		synchronized(cacheUserToData) {
			cacheUserToData.remove(pid);
		}
		synchronized(cacheUserToPersonalInfo) {
			cacheUserToPersonalInfo.remove(pid);
		}
	}
	
	private void initUserUidCache() {
		Connection con = null;
		
		try {
			synchronized(lock1) {
				UserDAO dao = UserDAO.getInstance();
				
				con = wta.getConnectionManager().getConnection();
				List<UserUid> uids = dao.viewAllUids(con);
				cleanupUserUidCache();
				for(UserUid uid : uids) {
					addToUserUidCache(uid);
				}
			}
		} catch(SQLException ex) {
			throw new WTRuntimeException(ex, "Unable to init user UID cache");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private void cleanupUserUidCache() {
		synchronized(lock1) {
			cacheUserToUserUid.clear();
			cacheUserUidToUser.clear();
		}
	}
	
	private void addToUserUidCache(UserUid uid) {
		synchronized(lock1) {
			UserProfile.Id pid = new UserProfile.Id(uid.getDomainId(), uid.getUserId());
			cacheUserToUserUid.put(pid, uid.getUserUid());
			cacheUserUidToUser.put(uid.getUserUid(), pid);
		}
	}
	
	private void removeFromUserUidCache(UserProfile.Id pid) {
		synchronized(lock1) {
			if(cacheUserToUserUid.containsKey(pid)) {
				String uid = cacheUserToUserUid.remove(pid);
				cacheUserUidToUser.remove(uid);
			}
		}
	}
	
	private void initGroupUidCache() {
		Connection con = null;
		
		try {
			synchronized(lock2) {
				GroupDAO dao = GroupDAO.getInstance();
				
				con = wta.getConnectionManager().getConnection();
				List<GroupUid> uids = dao.viewAllUids(con);
				cleanupGroupUidCache();
				for(GroupUid uid : uids) {
					addToGroupUidCache(uid);
				}
			}
		} catch(SQLException ex) {
			throw new WTRuntimeException(ex, "Unable to init group UID cache");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private void cleanupGroupUidCache() {
		synchronized(lock2) {
			cacheGroupToGroupUid.clear();
			cacheGroupUidToGroup.clear();
		}
	}
	
	private void addToGroupUidCache(GroupUid uid) {
		synchronized(lock2) {
			UserProfile.Id pid = new UserProfile.Id(uid.getDomainId(), uid.getUserId());
			cacheGroupToGroupUid.put(pid, uid.getUserUid());
			cacheGroupUidToGroup.put(uid.getUserUid(), pid);
		}
	}
	
	private void removeFromGroupUidCache(UserProfile.Id pid) {
		synchronized(lock2) {
			if(cacheGroupToGroupUid.containsKey(pid)) {
				String uid = cacheGroupToGroupUid.remove(pid);
				cacheGroupUidToGroup.remove(uid);
			}
		}
	}
	
	public static class CheckUserResult {
		public boolean exist;
		public boolean enabled;
		
		public CheckUserResult(boolean exist, boolean enabled) {
			this.exist = exist;
			this.enabled = enabled;
		}
	}
	
	public static class UserUidBag {
		public String userUid;
		public String roleUid;
		
		public UserUidBag() {}
		
		public UserUidBag(String uid, String roleUid) {
			this.userUid = uid;
			this.roleUid = roleUid;
		}
	}
	
	public static class EntityPermissions {
		public ArrayList<ORolePermission> others;
		public ArrayList<ORolePermission> services;
		
		public EntityPermissions(ArrayList<ORolePermission> others, ArrayList<ORolePermission> services) {
			this.others = others;
			this.services = services;
		}
	}
}