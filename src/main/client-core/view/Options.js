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
Ext.define('Sonicle.webtop.core.view.Options', {
	alternateClassName: 'WTA.view.Options',
	extend: 'WTA.sdk.DockableView',
	requires: [
		'WTA.model.Simple',
		'WTA.sdk.UserOptionsView',
		'WTA.sdk.UserOptionsController',
		'WTA.sdk.OptionTabSection'
	],
	
	dockableConfig: {
		title: '{opts.tit}',
		iconCls: 'wt-icon-options-xs',
		width: 650,
		height: 500
	},
	promptConfirm: false,
	
	profileId: null,
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		me.add({
			region: 'center',
			xtype: 'tabpanel',
			reference: 'svctabs',
			plain: true,
			items: [],
			listeners: {
				tabchange: {
					fn: function(s,tab) {
						if(!tab.loaded) {
							tab.loaded = true;
							tab.loadModel({
								data: {id: me.profileId}
							});
						}
					}
				}
			}
		});
		me.on('viewclose', me.onViewClose, me);
		me.initTabs();
	},
	
	onViewClose: function() {
		var me = this,
				tabs = me.lref('svctabs'),
				needLogin = false,
				needReload = false;
		
		// Checks if there is at least one panel that needs...
		tabs.items.each(function(tab) {
			if(tab.needLogin) {
				needLogin = true;
				return false;
			}
		});
		tabs.items.each(function(tab) {
			if(tab.needReload) {
				needReload = true;
				return false;
			}
		});
		
		if(needLogin) {
			WT.confirm(WT.res('opts.confirm.needLogin'), function(bid) {
				if(bid === 'yes') WT.logout();
			});
		} else if(needReload) {
			WT.confirm(WT.res('opts.confirm.needReload'), function(bid) {
				if(bid === 'yes') WT.reload();
			});
		}
	},
	
	initTabs: function() {
		var me = this,
				data = [], uo = null;
		
		if(WT.getVar('profileId') === me.profileId) {
			Ext.each(WT.getApp().getDescriptors(false), function(desc) {
				uo = desc.getUserOptions();
				if(uo) {
					Ext.Array.push(data, Ext.apply(uo, {
						id: desc.getId(),
						xid: desc.getXid(),
						name: desc.getName()
					}));
				}
			});
			me.createTabs(data);
		} else {
			me.on('afterrender', function() {
				me.wait();
				WT.ajaxReq(WT.ID, 'GetOptionsServices', {
					params: {id: me.profileId},
					callback: function(success, json) {
						if(success) me.createTabs(json.data);
						me.unwait();
					}
				});
			}, me, {single: true});
		}
	},
	
	createTabs: function(data) {
		var me = this;
		// Defines dependencies to load (viewClass and model)
		var dep = [];
		Ext.each(data, function(itm) {
			dep.push(itm.viewClassName);
			dep.push(itm.modelClassName);
		});
		
		Ext.require(dep, function() {
			var tabs = me.lref('svctabs');
			tabs.removeAll(true);
			Ext.each(data, function(itm) {
				tabs.add(Ext.create(itm.viewClassName, {
					itemId: itm.id,
					title: itm.name,
					iconCls: WTF.cssIconCls(itm.xid, 'service', 'xs'),
					ID: itm.id,
					XID: itm.xid,
					profileId: me.profileId,
					modelName: itm.modelClassName
				}));
			});
			tabs.setActiveTab(0);
		});
	}
});