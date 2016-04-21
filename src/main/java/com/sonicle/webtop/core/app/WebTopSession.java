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

import com.sonicle.commons.web.ServletUtils;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.security.Principal;
import com.sonicle.webtop.core.CoreLocaleKey;
import com.sonicle.webtop.core.CoreManager;
import com.sonicle.webtop.core.CoreUserSettings;
import com.sonicle.webtop.core.bol.js.JsWTS;
import com.sonicle.webtop.core.bol.model.AuthResource;
import com.sonicle.webtop.core.bol.model.AuthResourceShare;
import com.sonicle.webtop.core.sdk.Environment;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.ServiceManifest;
import com.sonicle.webtop.core.sdk.ServiceMessage;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.core.servlet.ServletHelper;
import com.sonicle.webtop.core.util.SessionUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import net.sf.uadetector.ReadableUserAgent;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class WebTopSession {
	private static final Logger logger = WT.getLogger(WebTopSession.class);
	public static final String PROPERTY_OTP_VERIFIED = "OTPVERIFIED";
	public static final String PROP_REQUEST_DUMP = "REQUESTDUMP";
	
	private Session session;
	private final WebTopApp wta;
	private final PropertyBag props = new PropertyBag();
	private String refererURI = null;
	private Locale userAgentLocale = null;
	private ReadableUserAgent userAgentInfo = null;
	private int initStatus = 0;
	//private boolean initialized = false;
	//private boolean profileInitialized = false;
	//private boolean environmentInitialized = false;
	private UserProfile profile = null;
	private final LinkedHashMap<String, BaseService> services = new LinkedHashMap<>();
	private final HashMap<String, UploadedFile> uploads = new HashMap<>();
	private SessionComManager comm = null;
	
	WebTopSession(WebTopApp wta, Session session) {
		this.wta = wta;
		this.session = session;
	}
	
	void cleanup() throws Exception {
		initStatus = -1;
		ServiceManager svcm = wta.getServiceManager();
		
		RequestDump dump = (RequestDump)popProperty(PROP_REQUEST_DUMP);
		if(profile != null) {
			wta.getLogManager().write(profile.getId(), CoreManifest.ID, "LOGOUT", null, dump, getId(), null);
		}
			
		// Cleanup services
		synchronized(services) {
			for(BaseService instance : services.values()) {
				svcm.cleanupPrivateService(instance);
			}
			services.clear();
		}
		// Cleanup uploads
		synchronized(uploads) {
			for(UploadedFile file : uploads.values()) {
				if(!file.virtual) {
					WT.deleteTempFile(file.id);
				}
			}
			uploads.clear();
		}
	}
	
	Session getSession() {
		return session;
	}
	
	public boolean isReady() {
		return initStatus == 2;
	}
	
	public void setProperty(String key, Object value) {
		synchronized(props) {
			props.set(key, value);
		}
	}
	
	public Object getProperty(String key) {
		synchronized(props) {
			return props.get(key);
		}
	}
	
	public Object popProperty(String key) {
		synchronized(props) {
			if(hasProperty(key)) {
				Object value = props.get(key);
				clearProperty(key);
				return value;
			} else {
				return null;
			}
		}
	}
	
	public void clearProperty(String key) {
		synchronized(props) {
			props.clear(key);
		}
	}
	
	public boolean hasProperty(String key) {
		synchronized(props) {
			return props.has(key);
		}
	}
	
	/**
	 * Returns the session ID.
	 * @return HttpSession unique identifier.
	 */
	public String getId() {
		return session.getId().toString();
	}
	
	/**
	 * Gets parsed user-agent info.
	 * @return A readable ReadableUserAgent object. 
	 */
	public ReadableUserAgent getUserAgent() {
		return userAgentInfo;
	}
	
	/**
	 * Gets the user profile associated to the session.
	 * @return The UserProfile.
	 */
	public UserProfile getUserProfile() {
		return profile;
	}
	
	public String getRefererURI() {
		return refererURI;
	}
	
	/**
	 * Return current locale.
	 * It can be the UserProfile's locale or the locale specified during
	 * the initial HTTP request to the server.
	 * @return The locale.
	 */
	public Locale getLocale() {
		if(profile != null) {
			return profile.getLocale();
		} else {
			return userAgentLocale;
		}
	}
	
	public void dumpRequest(HttpServletRequest request) {
		String remoteIp = ServletUtils.getClientIP(request);
		String userAgent = ServletUtils.getUserAgent(request);
		setProperty(PROP_REQUEST_DUMP, new RequestDump(remoteIp, userAgent));
	}
	
	public synchronized void initProfile(HttpServletRequest request) throws Exception {
		if(initStatus < 0) return;
		if(initStatus == 0) privateInitProfile(request);
	}
	
	public synchronized void initEnvironment(HttpServletRequest request) throws Exception {
		if(initStatus < 0) return;
		if(initStatus == 0) throw new Exception("You need to call initProfile() before calling this method!");
		if(initStatus == 1) privateInitEnvironment(request);
	}
	
	private void privateInitProfile(HttpServletRequest request) throws Exception {
		Principal principal = (Principal)SecurityUtils.getSubject().getPrincipal();
		CoreManager core = new CoreManager(wta.createAdminRunContext(getId()), wta);
		
		refererURI = ServletUtils.getReferer(request);
		userAgentLocale = ServletHelper.homogenizeLocale(request);
		userAgentInfo = wta.getUserAgentInfo(ServletUtils.getUserAgent(request));
		
		// Defines useful instances (NB: keep code assignment order!!!)
		profile = new UserProfile(core, principal);
		
		boolean otpEnabled = wta.getOTPManager().isEnabled(profile.getId());
		if(!otpEnabled) setProperty(PROPERTY_OTP_VERIFIED, true);
		
		initStatus = 1;
	}
	
	private void privateInitEnvironment(HttpServletRequest request) throws Exception {
		ServiceManager svcm = wta.getServiceManager();
		SessionManager sesm = wta.getSessionManager();
		String sessionId = getId();
		CoreManager core = new CoreManager(wta.createAdminRunContext(sessionId), wta);
		
		wta.getLogManager().write(profile.getId(), CoreManifest.ID, "AUTHENTICATED", null, request, getId(), null);
		sesm.registerWebTopSession(sessionId, this);
		comm = new SessionComManager(sesm, sessionId, profile.getId());
		
		// Instantiates services
		BaseService instance = null;
		List<String> serviceIds = core.getPrivateServicesForUser(profile);
		int count = 0;
		// TODO: ordinamento lista servizi (scelta dall'utente?)
		for(String serviceId : serviceIds) {
			// Creates new instance
			if(svcm.hasFullRights(serviceId)) {
				instance = svcm.instantiatePrivateService(serviceId, sessionId, new CoreEnvironment(wta, this));
			} else {
				instance = svcm.instantiatePrivateService(serviceId, sessionId, new Environment(this));
			}
			if(instance != null) {
				addService(instance);
				count++;
			}
		}
		logger.debug("Instantiated {} services", count);
		
		initStatus = 2;
	}
	
	/**
	 * Stores service instance into this session.
	 * @param service 
	 */
	private void addService(BaseService service) {
		String serviceId = service.getManifest().getId();
		synchronized(services) {
			if(services.containsKey(serviceId)) throw new WTRuntimeException("Cannot add service twice");
			services.put(serviceId, service);
		}
	}
	
	/**
	 * Gets a service instance by ID.
	 * @param serviceId The service ID.
	 * @return The service instance, if found.
	 */
	public BaseService getServiceById(String serviceId) {
		if(!isReady()) return null;
		synchronized(services) {
			if(!services.containsKey(serviceId)) throw new WTRuntimeException("No service with ID [{0}]", serviceId);
			return services.get(serviceId);
		}
	}
	
	/**
	 * Gets instantiated services list.
	 * @return A list of service ids.
	 */
	public List<String> getServices() {
		if(!isReady()) return null;
		synchronized(services) {
			return Arrays.asList(services.keySet().toArray(new String[services.size()]));
		}
	}
	
	public void fillStartup(JsWTS js, String layout) {
		if(!isReady()) return;
		js.securityToken = SessionUtils.getCSRFToken();
		js.layoutClassName = StringUtils.capitalize(layout);
		
		// Evaluate services
		JsWTS.Service last = null;
		String deflt = null;
		int index;
		for(String serviceId : getServices()) {
			fillStartupForService(js, serviceId);
			index = js.services.size()-1; // Last inserted
			last = js.services.get(index);
			last.index = index; // Position is (for convenience) also saved inside!
			if((deflt == null) && !last.id.equals(CoreManifest.ID) && !last.maintenance) {
				// Candidate startup (default) service must not be in maintenance
				// and id should not be equal to core service!
				deflt = last.id;
			}
		}
		js.defaultService = deflt;
	}
	
	private JsWTS.Service fillStartupForService(JsWTS js, String serviceId) {
		ServiceManager svcm = wta.getServiceManager();
		ServiceDescriptor sdesc = svcm.getDescriptor(serviceId);
		ServiceManifest manifest = sdesc.getManifest();
		UserProfile.Id pid = profile.getId();
		Locale locale = getLocale();
		
		JsWTS.Permissions perms = new JsWTS.Permissions();
		// Generates service auth permissions
		for(AuthResource res : manifest.getResources()) {
			if(res instanceof AuthResourceShare) continue;
			JsWTS.Actions acts = new JsWTS.Actions();
			for(String act : res.getActions()) {
				if(WT.isPermitted(pid, serviceId, res.getName(), act)) {
					acts.put(act, true);
				}
			}
			if(!acts.isEmpty()) perms.put(res.getName(), acts);
		}
		
		if(svcm.isCoreService(serviceId)) {
			// Defines paths and requires
			js.appRequires.add(manifest.getPrivateServiceJsClassName(true));
			js.appRequires.add(manifest.getLocaleJsClassName(locale, true));
		} else {
			// Defines paths and requires
			js.appPaths.put(manifest.getJsPackageName(), manifest.getJsBaseUrl());
			js.appRequires.add(manifest.getPrivateServiceJsClassName(true));
			js.appRequires.add(manifest.getLocaleJsClassName(locale, true));
		}
		
		// Completes service info
		JsWTS.Service jssvc = new JsWTS.Service();
		jssvc.id = manifest.getId();
		jssvc.xid = manifest.getXId();
		jssvc.ns = manifest.getJsPackageName();
		jssvc.path = manifest.getJsBaseUrl();
		jssvc.localeClassName = manifest.getLocaleJsClassName(locale, true);
		jssvc.serviceClassName = manifest.getPrivateServiceJsClassName(true);
		jssvc.clientOptionsClassName = manifest.getClientOptionsModelJsClassName(true);
		if(sdesc.hasUserOptionsService()) {
			jssvc.userOptions = new JsWTS.ServiceUserOptions(
				manifest.getUserOptionsViewJsClassName(true),
				manifest.getUserOptionsModelJsClassName(true)
			);
		}
		jssvc.name = StringEscapeUtils.escapeJson(wta.lookupResource(serviceId, locale, CoreLocaleKey.SERVICE_NAME));
		jssvc.description = StringEscapeUtils.escapeJson(wta.lookupResource(serviceId, locale, CoreLocaleKey.SERVICE_DESCRIPTION));
		jssvc.version = manifest.getVersion().toString();
		jssvc.build = manifest.getBuildDate();
		jssvc.company = StringEscapeUtils.escapeJson(manifest.getCompany());
		jssvc.maintenance = svcm.isInMaintenance(serviceId);
		
		js.services.add(jssvc);
		js.servicesOptions.add(getClientOptions(serviceId));
		js.servicesPerms.add(perms);
		
		return jssvc;
	}
	
	private JsWTS.Settings getClientOptions(String serviceId) {
		BaseService svc = getServiceById(serviceId);
		
		// Gets initial settings from instantiated service
		HashMap<String, Object> hm = null;
		try {
			WebTopApp.setServiceLoggerDC(serviceId);
			hm = svc.returnClientOptions();
		} catch(Exception ex) {
			logger.error("returnStartupOptions method returns errors", ex);
		} finally {
			WebTopApp.unsetServiceLoggerDC();
		}
		
		JsWTS.Settings is = new JsWTS.Settings();
		if(hm != null) is.putAll(hm);
		
		// Built-in settings
		if(serviceId.equals(CoreManifest.ID)) {
			//is.put("authTicket", generateAuthTicket());
			is.put("isWhatsnewNeeded", isWhatsnewNeeded());
		} else {
			CoreUserSettings cus = new CoreUserSettings(serviceId, profile.getId());
			is.put("viewportToolWidth", cus.getViewportToolWidth());
		}
		return is;
	}
	
	private boolean isWhatsnewNeeded() {
		ServiceManager svcm = wta.getServiceManager();
		boolean needWhatsnew = false;
		for(String serviceId : getServices()) {
			needWhatsnew = needWhatsnew | svcm.needWhatsnew(serviceId, profile);
		}
		return needWhatsnew;
	}
	
	public boolean needWhatsnew(String serviceId, UserProfile profile) {
		if(!isReady()) return false;
		ServiceManager svcm = wta.getServiceManager();
		return svcm.needWhatsnew(serviceId, profile);
	}
	
	public String getWhatsnewHtml(String serviceId, UserProfile profile, boolean full) {
		if(!isReady()) return null;
		ServiceManager svcm = wta.getServiceManager();
		return svcm.getWhatsnew(serviceId, profile, full);
	}
	
	public void resetWhatsnew(String serviceId, UserProfile profile) {
		if(!isReady()) return;
		ServiceManager svcm = wta.getServiceManager();
		svcm.resetWhatsnew(serviceId, profile);
	}
	
	public void nofity(ServiceMessage message) {
		if(!isReady()) return;
		comm.nofity(message);
	}
	
	public void nofity(List<ServiceMessage> messages) {
		if(!isReady()) return;
		comm.nofity(messages);
	}
	
	public List<ServiceMessage> getEnqueuedMessages() {
		if(!isReady()) return null;
		return comm.popEnqueuedMessages();
	}
	
	public void addUploadedFile(UploadedFile uploadedFile) {
		if(!isReady()) return;
		synchronized(uploads) {
			uploads.put(uploadedFile.id, uploadedFile);
		}
	}
	
	public UploadedFile getUploadedFile(String id) {
		if(!isReady()) return null;
		synchronized(uploads) {
			return uploads.get(id);
		}
	}
	
	public boolean hasUploadedFile(String id) {
		if(!isReady()) return false;
		synchronized(uploads) {
			return uploads.containsKey(id);
		}
	}
	
	public void clearUploadedFile(UploadedFile uploadedFile) {
		if(!isReady()) return;
		clearUploadedFile(uploadedFile.id);
	}
	
	public void clearUploadedFile(String id) {
		if(!isReady()) return;
		synchronized(uploads) {
			uploads.remove(id);
		}
	}
	
	public static class UploadedFile {
		public String id;
		public String filename;
		public String mediaType;
		public boolean virtual;
		
		public UploadedFile(String id, String filename, String mediaType, boolean virtual) {
			this.id = id;
			this.filename = filename;
			this.mediaType = mediaType;
			this.virtual = virtual;
		}
	}
	
	public static class RequestDump {
		public String remoteIP;
		public String userAgent;
		
		public RequestDump(String remoteIP, String userAgent) {
			this.remoteIP = remoteIP;
			this.userAgent = userAgent;
		}
	}
}