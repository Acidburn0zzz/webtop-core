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
Ext.define('Sonicle.webtop.core.mixin.PanelUtil', {
	alternateClassName: 'WTA.mixin.PanelUtil',
	extend: 'Ext.Mixin',
	mixinConfig: {
		id: 'panelutil'
	},
	
	/**
	 * Generates an `id` concatenating {@link Ext.Component#getId component's id}
	 * with provided string suffix.
	 * @param {String} suffix The suffix to append.
	 * @returns {String} The generated `id`.
	 */
	sufId: function(suffix) {
		return this.getId()+'-'+suffix;
	},
	
	/**
	 * Convenience method to get the toolbar docked on 'top'.
	 * @returns {Ext.toolbar.Toolbar}
	 */
	getTopBar: function() {
		var ret = this.getDockedItems('toolbar[dock="top"]');
		return (ret && (ret.length > 0)) ? ret[0] : null;
	},
	
	/**
	 * Convenience method to get the toolbar docked on 'bottom'.
	 * @returns {Ext.toolbar.Toolbar}
	 */
	getBottomBar: function() {
		var ret = this.getDockedItems('toolbar[dock="bottom"]');
		return (ret && (ret.length > 0)) ? ret[0] : null;
	},
	
	/**
	 * Convenience method for getting a reference to a container.
	 * @param {Ext.container.Container} cmp The component on which calling {@link Ext.container.Container#lookupReference}
	 * @param {String} path Reference path to follow
	 * @returns {Ext.container.Container}
	 */
	lref: function(cmp, path) {
		if(arguments.length === 1) {
			path = cmp;
			cmp = this;
		}
		var i, keys = path.split('.');
		for(i=0; i<keys.length; i++) {
			cmp = cmp.lookupReference(keys[i]);
			if(!cmp) break;
		}
		return cmp;
	},
	
	/**
	 * Convenience method that returns {@link Ext.app.ViewModel#data viewModel}.
	 * @returns {Ext.app.ViewModel}
	 */
	getVM: function() {
		return this.getViewModel();
	},
	
	/**
	 * Convenience method that returns {@link Ext.app.ViewModel#data viewModel data}.
	 * @returns {Object}
	 */
	getVMData: function() {
		return this.getVM().data;
	}
});	
