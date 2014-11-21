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
package com.sonicle.webtop.core;

import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.servlet.ServletUtils;
import com.sonicle.webtop.core.bol.OUser;
import com.sonicle.webtop.core.dal.UserDAO;
import com.sonicle.webtop.core.sdk.BaseOptionManager;
import com.sonicle.webtop.core.sdk.JsOptions;
import com.sonicle.webtop.core.sdk.Service;
import com.sonicle.webtop.core.sdk.UserData;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.userdata.UserDataProviderBase;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class CoreOptions extends BaseOptionManager {
	
	public static final Logger logger = Service.getLogger(CoreOptions.class);
	
	public void processOptions(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Connection con = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			
			con = getCoreConnection();
			CoreServiceSettings css = new CoreServiceSettings(getDomainId(), getServiceId());
			CoreUserSettings cus = new CoreUserSettings(getDomainId(), getUserId(), getServiceId());
			UserDAO udao = UserDAO.getInstance();
			OUser user = udao.selectByDomainUser(con, getDomainId(), getUserId());
			if(user == null) throw new WTException("Unable to find a user [{0}, {1}]", getDomainId(), getUserId());
			
			UserDataProviderBase udp = getUserDataProvider();
			UserData ud = udp.getUserData(user.getDomainId(), user.getUserId());
			
			if(crud.equals("read")) {
				String id = ServletUtils.getStringParameter(request, "id", true);
				
				// Main
				JsOptions main = new JsOptions();
				main.put("displayName", user.getDisplayName());
				main.put("locale", user.getLocale());
				main.put("rtl", cus.getRightToLeft());
				main.put("theme", cus.getTheme());
				main.put("laf", cus.getLookAndFeel());
				
				// TFA
				JsOptions tfa = new JsOptions();
				tfa.put("enabled", css.getTFAEnabled());
				tfa.put("deviceTrustEnabled", css.getTFADeviceTrustEnabled());
				tfa.put("delivery", cus.getTFADelivery());
				tfa.put("emailAddress", cus.getTFAEmailAddress());
				
				JsOptions opts = new JsOptions();
				opts.put("id", id);
				opts.putAll(main);
				opts.putPrefixed("tfa", tfa);
				opts.putPrefixed("usd", ud.getMap());
				new JsonResult("options", opts).printTo(out);
				
			} else if(crud.equals("update")) {
				JsOptions opts = ServletUtils.getPayload(request, JsOptions.class);
				
				// User
				if(opts.containsKey("theme")) cus.setTheme(opts.getString("theme"));
				if(opts.containsKey("laf")) cus.setLookAndFeel(opts.getString("laf"));
				if(opts.containsKey("displayName")) user.setDisplayName(opts.getString("displayName"));
				if(opts.containsKey("locale")) user.setLanguageTag(opts.getString("locale"));
				udao.update(con, user);
				
				// TFA
				
				
				// UserData
				if(udp.canWrite()) {
					ud.setMap(opts.getPrefixed("usd"));
					udp.setUserData(getDomainId(), getUserId(), ud);
				}
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error executing action Options", ex);
			new JsonResult(false).printTo(out);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
}