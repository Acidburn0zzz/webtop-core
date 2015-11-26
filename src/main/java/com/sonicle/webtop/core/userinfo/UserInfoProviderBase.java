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
package com.sonicle.webtop.core.userinfo;

import com.sonicle.webtop.core.sdk.interfaces.IConnectionProvider;
import com.sonicle.webtop.core.sdk.UserPersonalInfo;
import com.sonicle.webtop.core.sdk.interfaces.IServiceSettingReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author malbinola
 */
public abstract class UserInfoProviderBase {
	
	protected static final Logger logger = (Logger) LoggerFactory.getLogger(UserInfoProviderBase.class);
	protected IConnectionProvider conp = null;
	protected IServiceSettingReader setm = null;
	
	public UserInfoProviderBase(IConnectionProvider conp, IServiceSettingReader setm) {
		this.conp = conp;
		this.setm = setm;
	}
	
	public abstract boolean canWrite();
	public abstract boolean addUser(String domainId, String userId);
	public abstract boolean deleteUser(String domainId, String userId);
	public abstract UserPersonalInfo getInfo(String domainId, String userId);
	public abstract boolean setInfo(String domainId, String userId, UserPersonalInfo info);
}