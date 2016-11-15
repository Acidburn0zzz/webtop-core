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
import com.sonicle.security.CredentialAlgorithm;
import com.sonicle.security.PasswordUtils;
import com.sonicle.security.auth.DirectoryException;
import com.sonicle.security.auth.EntryException;
import com.sonicle.security.auth.directory.ADDirectory;
import com.sonicle.security.auth.directory.AbstractDirectory;
import com.sonicle.security.auth.directory.AbstractDirectory.UserEntry;
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
import com.sonicle.webtop.core.bol.ORolePermission;
import com.sonicle.webtop.core.bol.OUser;
import com.sonicle.webtop.core.bol.OUserAssociation;
import com.sonicle.webtop.core.bol.OUserInfo;
import com.sonicle.webtop.core.bol.UserUid;
import com.sonicle.webtop.core.bol.model.DirectoryUser;
import com.sonicle.webtop.core.bol.model.DomainEntity;
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
import com.sonicle.webtop.core.sdk.UserPersonalInfo;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.core.userinfo.UserInfoProviderBase;
import com.sonicle.webtop.core.userinfo.UserInfoProviderFactory;
import com.sonicle.webtop.core.util.IdentifierUtils;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public final class UserManager {
	private static final Logger logger = WT.getLogger(UserManager.class);
	private static boolean initialized = false;
	
	/**
	 * Initialization method. This method should be called once.
	 * 
	 * @param wta WebTopApp instance.
	 * @return The instance.
	 */
	static synchronized UserManager initialize(WebTopApp wta) {
		if(initialized) throw new RuntimeException("Initialization already done");
		UserManager usem = new UserManager(wta);
		initialized = true;
		logger.info("UserManager initialized");
		return usem;
	}
	
	private WebTopApp wta = null;
	
	public static final String USERID_USERS = "users";
	public static final String USERID_ADMINISTRATORS = "administrators";
	
	private final Object lock1 = new Object();
	private final HashMap<UserProfile.Id, String> cacheUserToUserUid = new HashMap<>();
	private final HashMap<String, UserProfile.Id> cacheUserUidToUser = new HashMap<>();
	private final Object lock2 = new Object();
	private final HashMap<UserProfile.Id, String> cacheGroupToGroupUid = new HashMap<>();
	private final HashMap<String, UserProfile.Id> cacheGroupUidToGroup = new HashMap<>();
	
	private final HashMap<UserProfile.Id, UserPersonalInfo> cacheUserToPersonalInfo = new HashMap<>();
	private final HashMap<UserProfile.Id, UserProfile.Data> cacheUserToData = new HashMap<>();
	
	/**
	 * Private constructor.
	 * Instances of this class must be created using static initialize method.
	 * @param wta WebTopApp instance.
	 */
	private UserManager(WebTopApp wta) {
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
	
	public static String generateSecretKey() {
		return StringUtils.defaultIfBlank(IdentifierUtils.generateSecretKey(), "0123456789101112");
	}
	
	public UserInfoProviderBase getUserInfoProvider() throws WTException {
		CoreServiceSettings css = new CoreServiceSettings(CoreManifest.ID, "*");
		String providerName = css.getUserInfoProvider();
		return UserInfoProviderFactory.getProvider(providerName, wta.getConnectionManager(), wta.getSettingsManager());
	}
	
	public boolean isUserInfoProviderWritable() {
		try {
			return getUserInfoProvider().canWrite();
		} catch(WTException ex) {
			//TODO: logging?
			return false;
		}
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
			return dao.selectEnabledByInternetDomain(con, internetDomain);
			
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
		try {
			ODomain domain = getDomain(domainId);
			DomainEntity de = new DomainEntity(domain);
			de.setDirPassword(getDirPassword(domain));
			return de;
			
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Invalid directory URI");
		}
	}
	
	private void fillDomain(ODomain o, DomainEntity domain) throws WTException {
		String scheme = null;
		
		o.setDomainId(domain.getDomainId());
		o.setDomainName(domain.getInternetName());
		o.setDescription(domain.getDisplayName());
		o.setEnabled(domain.getEnabled());
		o.setAuthUri(domain.getDirUri());
		
		try {
			scheme = URIUtils.getScheme(domain.getDirUri());
		} catch(URISyntaxException ex) {
			throw new WTException("Invalid directory URI [{0}]", domain.getDirUri());
		}
		
		if (scheme.equals(WebTopDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(null);
			o.setAuthUsername(null);
			o.setAuthPassword(null);
			o.setWebtopAdvSecurity(domain.getDirPasswordPolicy());
		} else if (scheme.equals(LdapWebTopDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getDirConnectionSecurity()));
			o.setAuthUsername(domain.getDirUsername());
			setDirPassword(o, domain.getDirPassword());
			o.setWebtopAdvSecurity(domain.getDirPasswordPolicy());
		} else if (scheme.equals(LdapDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getDirConnectionSecurity()));
			o.setAuthUsername(domain.getDirUsername());
			setDirPassword(o, domain.getDirPassword());
			o.setWebtopAdvSecurity(false);
		} else if (scheme.equals(ImapDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getDirConnectionSecurity()));
			o.setAuthUsername(null);
			o.setAuthPassword(null);
			o.setWebtopAdvSecurity(false);
		} else if (scheme.equals(SmbDirectory.SCHEME) || scheme.equals(SftpDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(null);
			o.setAuthUsername(null);
			o.setAuthPassword(null);
			o.setWebtopAdvSecurity(false);
		} else if (scheme.equals(ADDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getDirConnectionSecurity()));
			o.setAuthUsername(domain.getDirUsername());
			setDirPassword(o, domain.getDirPassword());
			o.setWebtopAdvSecurity(domain.getDirPasswordPolicy());
		} else if (scheme.equals(LdapNethDirectory.SCHEME)) {
			o.setAuthConnectionSecurity(EnumUtils.getName(domain.getDirConnectionSecurity()));
			o.setAuthUsername(domain.getDirUsername());
			setDirPassword(o, domain.getDirPassword());
			o.setWebtopAdvSecurity(false);
		}
		o.setCaseSensitiveAuth(domain.getDirCaseSensitive());
		o.setUserAutoCreation(domain.getUserAutoCreation());
	}
	
	public void addDomain(DomainEntity domain) throws WTException {
		DomainDAO dodao = DomainDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection(false);
			
			logger.debug("Inserting domain");
			ODomain odomain = new ODomain();
			fillDomain(odomain, domain);
			dodao.insert(con, odomain);
			
			OGroup ogroup1 = doGroupInsert(con, odomain.getDomainId(), USERID_ADMINISTRATORS, "Utenti");
			OGroup ogroup2 = doGroupInsert(con, odomain.getDomainId(), USERID_USERS, "Utenti");
			
			DbUtils.commitQuietly(con);
			
			// Update cache
			addToUserUidCache(new GroupUid(ogroup1.getDomainId(), ogroup1.getUserId(), ogroup1.getUserUid()));
			addToUserUidCache(new GroupUid(ogroup2.getDomainId(), ogroup2.getUserId(), ogroup2.getUserUid()));
			
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
		
		try {
			con = wta.getConnectionManager().getConnection(false);
			
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
			
			initUserUidCache();
			initGroupUidCache();
			cleanupUserCache();
			
			//TODO: chiamare controller per eliminare dominio per i servizi
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
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
		AuthManager authm = wta.getAuthManager();
		UserDAO dao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uassdao = UserAssociationDAO.getInstance();
		
		try {
			OUser ouser = dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			if(ouser == null) throw new WTException("User not found [{0}]", pid.toString());
			OUserInfo ouseri = uidao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			if(ouseri == null) throw new WTException("User info not found [{0}]", pid.toString());
			
			List<AssignedGroup> assiGroups = uassdao.viewAssignedByUser(con, ouser.getUserUid());
			List<AssignedRole> assiRoles = authm.listAssignedRoles(con, ouser.getUserUid());
			AuthManager.EntityPermissions perms = authm.extractPermissions(con, ouser.getUserUid());
			
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
	
	
	
	public void addUser(UserEntity user) throws WTException {
		addUser(false, user, null);
	}
	
	public void addUser(boolean updateDirectory, UserEntity user, char[] password) throws WTException {
		AuthManager authm = wta.getAuthManager();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID, false);
			
			ODomain domain = getDomain(user.getDomainId());
			if(domain == null) throw new WTException("Domain not found [{0}]", user.getDomainId());
			
			OUser ouser = null;
			if(updateDirectory) {
				AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
				AbstractDirectory directory = authm.getAuthDirectory(ad.getAuthUri());
				DirectoryOptions opts = wta.createDirectoryOptions(ad);
				
				if(directory.hasCapability(DirectoryCapability.USERS_WRITE)) {
					if(!directory.validateUsername(opts, user.getUserId())) {
						throw new WTException("Username does not satisfy directory requirements [{0}]", ad.getAuthUri().getScheme());
					}
				}
				if(directory.hasCapability(DirectoryCapability.PASSWORD_WRITE)) {
					if(domain.getWebtopAdvSecurity() && !directory.validatePasswordPolicy(opts, password)) {
						throw new WTException("Password does not satisfy directory requirements [{0}]", ad.getAuthUri().getScheme());
					}
				}
				
				ouser = doUserInsert(con, domain, user);
				
				// Insert user in directory (if necessary)
				if(directory.hasCapability(DirectoryCapability.USERS_WRITE)) {
					logger.debug("Adding user into directory");
					try {
						directory.addUser(opts, domain.getDomainId(), createUserEntry(user));
					} catch(EntryException ex1) {
						logger.debug("Skipped: already exists!");
					}
				}
				if(directory.hasCapability(DirectoryCapability.PASSWORD_WRITE)) {
					logger.debug("Updating its password");
					directory.updateUserPassword(opts, domain.getDomainId(), user.getUserId(), password);
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
		AuthManager authm = wta.getAuthManager();
		
		try {
			ODomain domain = getDomain(pid.getDomainId());
			if(domain == null) throw new WTException("Domain not found [{0}]", pid.getDomainId());
			
			AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
			AbstractDirectory directory = authm.getAuthDirectory(ad.getAuthUri());
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
		AuthManager authm = wta.getAuthManager();
		UserDAO udao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(CoreManifest.ID, false);
			
			OUser user = udao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			if(user == null) throw new WTException("User not found [{0}]", pid.toString());
			
			logger.debug("Deleting permissions");
			authm.deletePermissionByRole(con, user.getUserUid());
			logger.debug("Deleting groups associations");
			uadao.deleteByUser(con, user.getUserUid());
			logger.debug("Deleting roles associations");
			authm.deleteRoleAssociationByUser(con, user.getUserUid());
			logger.debug("Deleting userInfo");
			uidao.deleteByDomainUser(con, pid.getDomainId(), pid.getUserId());
			logger.debug("Deleting user");
			udao.deleteByDomainUser(con, pid.getDomainId(), pid.getUserId());
			
			if(cleanupDirectory) {
				ODomain domain = getDomain(pid.getDomainId());
				if(domain == null) throw new WTException("Domain not found [{0}]", pid.getDomainId());
				
				AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
				AbstractDirectory directory = authm.getAuthDirectory(ad.getAuthUri());
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
		AuthManager authm = wta.getAuthManager();
		UserDAO dao = UserDAO.getInstance();
		Connection con = null;
		
		try {
			AuthenticationDomain ad = wta.createAuthenticationDomain(domain);
			AbstractDirectory directory = authm.getAuthDirectory(ad.getAuthUri());
			DirectoryOptions opts = wta.createDirectoryOptions(ad);
			
			con = wta.getConnectionManager().getConnection();
			Map<String, OUser> wtUsers = dao.selectByDomain2(con, domain.getDomainId());
			
			ArrayList<DirectoryUser> items = new ArrayList<>();
			
			if(directory.hasCapability(DirectoryCapability.USERS_READ)) {
				for(UserEntry userEntry : directory.listUsers(opts, domain.getDomainId())) {
					items.add(new DirectoryUser(domain.getDomainId(), userEntry, wtUsers.get(userEntry.userId)));
				}
				
			} else {
				for(OUser ouser : wtUsers.values()) {
					final AbstractDirectory.UserEntry userEntry = new AbstractDirectory.UserEntry(ouser.getUserId(), null, null, ouser.getDisplayName(), null);
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
	
	public UserPersonalInfo getUserPersonalInfo(UserProfile.Id pid) throws WTException {
		UserInfoDAO dao = UserInfoDAO.getInstance();
		Connection con = null;
		
		try {
			con = wta.getConnectionManager().getConnection();
			OUserInfo oui = dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
			return (oui == null) ? null : new UserPersonalInfo(oui);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public boolean updateUserPersonalInfo(UserProfile.Id pid, UserPersonalInfo userPersonalInfo) throws WTException {
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
	
	
	
	
	/*
	public UserProfile.Data userData999(UserProfile.Id pid) throws WTException {
		synchronized(cacheUserToData) {
			if(!cacheUserToData.containsKey(pid)) {
				try {
					OUser user = getUser(pid);
					if(user == null) throw new WTException("User not found [{0}]", pid.toString());
					
					CoreUserSettings cus = new CoreUserSettings(pid);
					UserPersonalInfo info = userPersonalInfo(pid);
					InternetAddress ia = MailUtils.buildInternetAddress(info.getEmail(), user.getDisplayName());
					UserProfile.Data data = new UserProfile.Data(user.getDisplayName(), cus.getLanguageTag(), cus.getTimezone(), ia);
					cacheUserToData.put(pid, data);
					return data;
				} catch(WTException ex) {
					logger.error("Unable to find user [{}]", pid);
					throw ex;
				}
			} else {
				return cacheUserToData.get(pid);
			}
		}
	}
	
	public UserPersonalInfo userPersonalInfo999(UserProfile.Id pid) throws WTException {
		synchronized(lock2) {
			if(!cacheUserToPersonalInfo.containsKey(pid)) {
				try {
					UserInfoProviderBase uip = getUserInfoProvider();
					UserPersonalInfo info = uip.getInfo(pid.getDomainId(), pid.getUserId());
					cacheUserToPersonalInfo.put(pid, info);
					return info;
				} catch(WTException ex) {
					logger.error("Unable to find personal info for user [{}]", pid);
					throw ex;
				}	
			} else {
				return cacheUserToPersonalInfo.get(pid);
			}
		}
	}
	*/
	
	public UserPersonalInfo userPersonalInfo(UserProfile.Id pid) throws WTException {
		synchronized(cacheUserToPersonalInfo) {
			if(!cacheUserToPersonalInfo.containsKey(pid)) {
				UserPersonalInfo upi = getUserPersonalInfo(pid);
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
		return new UserProfile.Id(domain.getDomainName(), pid.getUserId()).toString();
	}
	
	public String getDomainInternetName(String domainId) throws WTException {
		ODomain domain = getDomain(domainId);
		return domain.getDomainName();
	}
	
	
	
	private OGroup doGroupInsert(Connection con, String domainId, String groupId, String displayName) throws DAOException, WTException {
		GroupDAO gdao = GroupDAO.getInstance();
		
		logger.debug("Inserting group");
		OGroup ogroup = new OGroup();
		ogroup.setDomainId(domainId);
		ogroup.setUserId(groupId);
		ogroup.setEnabled(true);
		ogroup.setUserUid(IdentifierUtils.getUUID());
		ogroup.setDisplayName(displayName);
		ogroup.setSecret(null);
		ogroup.setPasswordType(null);
		ogroup.setPassword(null);
		gdao.insert(con, ogroup);
		
		return ogroup;
	}
	
	
	private UserEntry createUserEntry(UserEntity user) {
		return new UserEntry(user.getUserId(), user.getFirstName(), user.getLastName(), user.getDisplayName(), null);
	}
	
	private OUser doUserInsert(Connection con, ODomain domain, UserEntity user) throws DAOException, WTException {
		AuthManager authm = wta.getAuthManager();
		UserDAO udao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		
		InternetAddress email = MailUtils.buildInternetAddress(user.getUserId(), domain.getDomainName(), null);
		if(email == null) throw new WTException("Cannot create a valid email address [{0}, {1}]", user.getUserId(), domain.getDomainName());
		
		// Insert User record
		logger.debug("Inserting user");
		OUser ouser = new OUser();
		ouser.setDomainId(user.getDomainId());
		ouser.setUserId(user.getUserId());
		ouser.setEnabled(user.getEnabled());
		ouser.setUserUid(IdentifierUtils.getUUID());
		ouser.setDisplayName(user.getDisplayName());
		ouser.setSecret(generateSecretKey());
		udao.insert(con, ouser);
		
		// Insert UserInfo record
		logger.debug("Inserting userInfo");
		OUserInfo oui = new OUserInfo();
		oui.setDomainId(user.getDomainId());
		oui.setUserId(user.getUserId());
		oui.setFirstName(user.getFirstName());
		oui.setLastName(user.getLastName());
		oui.setEmail(email.getAddress());
		uidao.insert(con, oui);
		
		logger.debug("Inserting groups associations");
		for(AssignedGroup assiGroup : user.getAssignedGroups()) {
			final OUserAssociation oua = new OUserAssociation();
			final String groupUid = groupToUid(new UserProfile.Id(user.getDomainId(), assiGroup.getGroupId()));
			oua.setUserAssociationId(uadao.getSequence(con).intValue());
			oua.setUserUid(ouser.getUserUid());
			oua.setGroupUid(groupUid);
			uadao.insert(con, oua);
		}
		
		logger.debug("Inserting roles associations");
		for(AssignedRole assiRole : user.getAssignedRoles()) {
			authm.addRoleAssociation(con, ouser.getUserUid(), assiRole.getRoleUid());
		}
		
		// Insert permissions
		logger.debug("Inserting permissions");
		for(ORolePermission perm : user.getPermissions()) {
			authm.addPermission(con, ouser.getUserUid(), perm.getServiceId(), perm.getKey(), perm.getAction(), "*");
		}
		for(ORolePermission perm : user.getServicesPermissions()) {
			authm.addPermission(con, ouser.getUserUid(), CoreManifest.ID, "SERVICE", ServicePermission.ACTION_ACCESS, perm.getInstance());
		}
		
		return ouser;
	}
	
	private void doUserUpdate(Connection con, UserEntity user) throws DAOException, WTException {
		AuthManager authm = wta.getAuthManager();
		UserDAO udao = UserDAO.getInstance();
		UserInfoDAO uidao = UserInfoDAO.getInstance();
		UserAssociationDAO uadao = UserAssociationDAO.getInstance();
		
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
			final OUserAssociation oua = new OUserAssociation();
			final String groupUid = groupToUid(new UserProfile.Id(user.getDomainId(), assiGroup.getGroupId()));
			oua.setUserAssociationId(uadao.getSequence(con).intValue());
			oua.setUserUid(oldUser.getUserUid());
			oua.setGroupUid(groupUid);
			uadao.insert(con, oua);
		}
		
		logger.debug("Updating roles associations");
		LangUtils.CollectionChangeSet<AssignedRole> changeSet2 = LangUtils.getCollectionChanges(oldUser.getAssignedRoles(), user.getAssignedRoles());
		for(AssignedRole assiRole : changeSet2.deleted) {
			authm.deleteRoleAssociation(con, assiRole.getRoleAssociationId());
		}
		for(AssignedRole assiRole : changeSet2.inserted) {
			authm.addRoleAssociation(con, oldUser.getUserUid(), assiRole.getRoleUid());
		}

		logger.debug("Updating permissions");
		LangUtils.CollectionChangeSet<ORolePermission> changeSet3 = LangUtils.getCollectionChanges(oldUser.getPermissions(), user.getPermissions());
		for(ORolePermission perm : changeSet3.deleted) {
			authm.deletePermission(con, perm.getRolePermissionId());
		}
		for(ORolePermission perm : changeSet3.inserted) {
			authm.addPermission(con, oldUser.getUserUid(), perm.getServiceId(), perm.getKey(), perm.getAction(), "*");
		}

		LangUtils.CollectionChangeSet<ORolePermission> changeSet4 = LangUtils.getCollectionChanges(oldUser.getServicesPermissions(), user.getServicesPermissions());
		for(ORolePermission perm : changeSet4.deleted) {
			authm.deletePermission(con, perm.getRolePermissionId());
		}
		for(ORolePermission perm : changeSet4.inserted) {
			authm.addPermission(con, oldUser.getUserUid(), CoreManifest.ID, "SERVICE", ServicePermission.ACTION_ACCESS, perm.getInstance());
		}
	}
	
	private UserProfile.Data getUserData(UserProfile.Id pid) throws WTException {
		CoreUserSettings cus = new CoreUserSettings(pid);
		UserPersonalInfo upi = userPersonalInfo(pid);
		OUser ouser = getUser(pid);
		if(ouser == null) throw new WTException("User not found [{0}]", pid.toString());

		InternetAddress ia = MailUtils.buildInternetAddress(upi.getEmail(), ouser.getDisplayName());
		return new UserProfile.Data(ouser.getDisplayName(), cus.getLanguageTag(), cus.getTimezone(), ia);
	}
	
	private OUserInfo createUserInfo(UserPersonalInfo upi) {
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
	
	private void addToUserCache(UserProfile.Id pid, UserPersonalInfo userPersonalInfo) {
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
}
