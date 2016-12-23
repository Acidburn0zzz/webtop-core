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
package com.sonicle.webtop.core.bol.model;

import com.sonicle.commons.EnumUtils;
import com.sonicle.security.ConnectionSecurity;
import com.sonicle.webtop.core.bol.ODomain;
import java.net.URISyntaxException;

/**
 *
 * @author malbinola
 */
public class DomainEntity {
	private String domainId;
	private String internetName;
	private Boolean enabled;
	private String description;
	private Boolean userAutoCreation;
	private String authUri;
	private String authUsername;
	private String authPassword;
	private ConnectionSecurity authConnSecurity;
	private Boolean authCaseSensitive;
	private Boolean authPasswordPolicy;
	
	public DomainEntity() {}
	
	public DomainEntity(ODomain o) throws URISyntaxException {
		domainId = o.getDomainId();
		internetName = o.getInternetName();
		enabled = o.getEnabled();
		description = o.getDescription();
		userAutoCreation = o.getUserAutoCreation();
		authUri = o.getAuthUri();
		authUsername = o.getAuthUsername();
		authPassword = o.getAuthPassword();
		authConnSecurity = EnumUtils.getEnum(ConnectionSecurity.class, o.getAuthConnectionSecurity());
		authCaseSensitive = o.getAuthCaseSensitive();
		authPasswordPolicy = o.getAuthPasswordPolicy();
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getInternetName() {
		return internetName;
	}

	public void setInternetName(String internetName) {
		this.internetName = internetName;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public Boolean getUserAutoCreation() {
		return userAutoCreation;
	}

	public void setUserAutoCreation(Boolean userAutoCreation) {
		this.userAutoCreation = userAutoCreation;
	}

	public String getAuthUri() {
		return authUri;
	}

	public void setAuthUri(String authUri) {
		this.authUri = authUri;
	}

	public String getAuthUsername() {
		return authUsername;
	}

	public void setAuthUsername(String authUsername) {
		this.authUsername = authUsername;
	}

	public String getAuthPassword() {
		return authPassword;
	}

	public void setAuthPassword(String authPassword) {
		this.authPassword = authPassword;
	}
	
	public ConnectionSecurity getAuthConnSecurity() {
		return authConnSecurity;
	}

	public void setAuthConnSecurity(ConnectionSecurity authConnSecurity) {
		this.authConnSecurity = authConnSecurity;
	}

	public Boolean getAuthCaseSensitive() {
		return authCaseSensitive;
	}

	public void setAuthCaseSensitive(Boolean authCaseSensitive) {
		this.authCaseSensitive = authCaseSensitive;
	}

	public Boolean getAuthPasswordPolicy() {
		return authPasswordPolicy;
	}

	public void setAuthPasswordPolicy(Boolean authPasswordPolicy) {
		this.authPasswordPolicy = authPasswordPolicy;
	}
}