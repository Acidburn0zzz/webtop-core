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
package com.sonicle.webtop.core.servlet;

import com.sonicle.commons.web.ServletUtils;
import com.sonicle.webtop.core.CoreLocaleKey;
import com.sonicle.webtop.core.app.CoreManifest;
import com.sonicle.webtop.core.CoreServiceSettings;
import com.sonicle.webtop.core.app.OTPManager;
import com.sonicle.webtop.core.app.WebTopApp;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.bol.js.JsTrustedDevice;
import com.sonicle.webtop.core.bol.js.TrustedDeviceCookie;
import com.sonicle.webtop.core.sdk.UserProfile;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

/**
 *
 * @author malbinola
 */
public class Otp extends HttpServlet {
	
	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		WebTopApp wta = WebTopApp.get(request);
		WebTopSession wts = WebTopSession.get(request);
		
		try {
			WebTopApp.logger.trace("Servlet: otp [{}]", ServletHelper.getSessionID(request));
			UserProfile.Id pid = wts.getUserProfile().getId();
			CoreServiceSettings css = new CoreServiceSettings(CoreManifest.ID, pid.getDomainId());
			Locale locale = wts.getLocale();
			
			boolean skip = skipOTP(wta, pid, request);
			if(skip) throw new SkipException();
			
			OTPManager otpm = wta.getOTPManager();
			String deliveryMode = otpm.getDeliveryMode(pid);
			OTPManager.Config config = null;
			if(!wts.hasProperty("OTP_CONFIG")) {
				config = otpm.prepareCheckCode(pid);
				wts.setProperty("OTP_CONFIG", config); // Save for later...
				wts.setProperty("OTP_TRIES", 0); // Save for later...
				
				buildPage(wta, css, locale, deliveryMode, null, response);
				
			} else {
				config = (OTPManager.Config)wts.getProperty("OTP_CONFIG");
				Integer tries = (Integer)wts.getProperty("OTP_TRIES");
				if(tries == null) throw new NoMoreTriesException();
				tries++;
				
				int userCode = ServletUtils.getIntParameter(request, "wtcode", 0);
				if(otpm.checkCode(pid, config, userCode)) {
					if(css.getOTPDeviceTrustEnabled()) {
						boolean trust = ServletUtils.getBooleanParameter(request, "wttrust", false);
						if(trust) {
							String userAgent = ServletUtils.getUserAgent(request);
							JsTrustedDevice js = otpm.trustThisDevice(pid, userAgent);
							otpm.writeTrustedDeviceCookie(pid, response, new TrustedDeviceCookie(js));
						}
					}
					wts.clearProperty("OTP_CONFIG");
					wts.clearProperty("OTP_TRIES");
					throw new SkipException();
					
				} else {
					if(tries >= 3) throw new NoMoreTriesException();
					wts.setProperty("OTP_TRIES", tries); // Save for later...
					String failureMessage = wta.lookupResource(locale, CoreLocaleKey.TPL_OTP_ERROR_FAILURE, true);
					buildPage(wta, css, locale, deliveryMode, failureMessage, response);
				}
			}
			
		} catch(NoMoreTriesException ex) {
			SecurityUtils.getSubject().logout();
			if(wts != null) wts.clearProperty(WebTopSession.PROPERTY_OTP_VERIFIED);
			ServletUtils.forwardRequest(request, response, "login");
		} catch(SkipException ex) {
			if(wts != null) wts.setProperty(WebTopSession.PROPERTY_OTP_VERIFIED, true);
			ServletUtils.forwardRequest(request, response, "start");
		} catch(Exception ex) {
			WebTopApp.logger.error("Error in otp servlet!", ex);
			//TODO: pagina di errore
		} finally {
			ServletHelper.setCacheControl(response);
			ServletHelper.setPageContentType(response);
			WebTopApp.clearLoggerDC();
		}
	}
	
	private boolean skipOTP(WebTopApp wta, UserProfile.Id pid, HttpServletRequest request) {
		OTPManager otpm = wta.getOTPManager();
		CoreServiceSettings css = new CoreServiceSettings(CoreManifest.ID, pid.getDomainId());
		
		// Tests enabling parameters
		boolean sysEnabled = css.getOTPEnabled(), profileEnabled = otpm.isEnabled(pid);
		if(!sysEnabled || !profileEnabled) { //TODO: valutare se escludere admin
			WebTopApp.logger.debug("OTP check skipped [{}, {}]", sysEnabled, profileEnabled);
			return true;
		}
		
		String remoteIP = ServletUtils.getClientIP(request);
		WebTopApp.logger.debug("Checking OTP from remote address {}", remoteIP);
		
		// Checks if request comes from a configured trusted network and skip check
		if(otpm.isTrusted(pid, remoteIP)) {
			WebTopApp.logger.debug("OTP check skipped: request comes from a trusted address.");
			return true;
		}
		
		// Checks cookie that marks a trusted device
		JsTrustedDevice td = null;
		TrustedDeviceCookie cookie = otpm.readTrustedDeviceCookie(pid, request);
		if((cookie != null) && otpm.isThisDeviceTrusted(pid, cookie)) {
			td = otpm.getTrustedDevice(pid, cookie.deviceId);
		}
		
		if(td != null) {
			WebTopApp.logger.debug("OTP check skipped: request comes from a trusted device [{}]", td.deviceId);
			return true;
		}
		
		return false;
	}
	
	private void buildPage(WebTopApp wta, CoreServiceSettings css, Locale locale, String deliveryMode, String failureMessage, HttpServletResponse response) throws IOException, TemplateException {
		Map tplMap = new HashMap();
		ServletHelper.fillPageVars(tplMap, locale, wta);
		ServletHelper.fillSystemVars(tplMap, locale, wta);
		tplMap.put("showFailure", !StringUtils.isBlank(failureMessage));
		tplMap.put("failureMessage", failureMessage);
		tplMap.put("helpTitle", wta.lookupResource(locale, CoreLocaleKey.TPL_OTP_HELPTITLE, true));
		tplMap.put("deliveryTitle", wta.lookupResource(locale, CoreLocaleKey.TPL_OTP_DELIVERY_TITLE, true));
		tplMap.put("deliveryMode", deliveryMode);
		tplMap.put("deliveryInfo", wta.lookupResource(locale, MessageFormat.format(CoreLocaleKey.TPL_OTP_DELIVERY_INFO, deliveryMode), true));
		tplMap.put("codePlaceholder", wta.lookupResource(locale, CoreLocaleKey.TPL_OTP_CODE_PLACEHOLDER, true));
		tplMap.put("submitLabel", wta.lookupResource(locale, CoreLocaleKey.TPL_OTP_SUBMIT_LABEL, true));
		tplMap.put("trustLabel", wta.lookupResource(locale, CoreLocaleKey.TPL_OTP_TRUST_LABEL, true));
		tplMap.put("showTrustCheckbox", css.getOTPDeviceTrustEnabled());
		
		// Load and build template
		Template tpl = wta.loadTemplate("com/sonicle/webtop/core/otp.html");
		tpl.process(tplMap, response.getWriter());
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
	}
	
	private static class NoMoreTriesException extends Exception {
		public NoMoreTriesException() {
			super();
		}
	}

	private static class SkipException extends Exception {
		public SkipException() {
			super();
		}
	}
}
