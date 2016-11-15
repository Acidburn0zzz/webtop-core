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
Ext.define('Sonicle.webtop.core.admin.Service', {
	extend: 'WT.sdk.Service',
	requires: [
		'Sonicle.webtop.core.admin.model.AdminNode',
		'Sonicle.webtop.core.admin.view.Settings',
		'Sonicle.webtop.core.admin.view.Domain',
		'Sonicle.webtop.core.admin.view.DomainSettings',
		'Sonicle.webtop.core.admin.view.DomainUsers',
		'Sonicle.webtop.core.admin.view.DomainRoles'
	],
	
	init: function() {
		var me = this;
		
		me.initActions();
		me.initCxm();
		
		me.setToolComponent(Ext.create({
			xtype: 'panel',
			layout: 'border',
			referenceHolder: true,
			title: '',
			items: [{
				region: 'center',
				xtype: 'treepanel',
				reference: 'tradmin',
				border: false,
				useArrows: true,
				rootVisible: false,
				store: {
					autoLoad: true,
					autoSync: true,
					model: 'Sonicle.webtop.core.admin.model.AdminNode',
					proxy: WTF.apiProxy(me.ID, 'ManageAdminTree', 'children', {
						writer: {
							allowSingle: false // Always wraps records into an array
						}
					}),
					root: {
						id: 'root',
						expanded: true
					}
				},
				hideHeaders: true,
				listeners: {
					itemclick: function(s, rec, itm, i, e) {
						var type = rec.get('_type'), domainId;
						if(type === 'settings') {
							domainId = rec.get('_domainId');
							if(domainId) {
								me.showDomainSettingsUI(rec);
							} else {
								me.showSettingsUI(rec);
							}
						} else if(type === 'users') {
							me.showDomainUsersUI(rec);
						} else if(type === 'roles') {
							me.showDomainRolesUI(rec);
						}
						
					},
					itemcontextmenu: function(s, rec, itm, i, e) {
						var type = rec.get('_type');
						if(type === 'domains') {
							WT.showContextMenu(e, me.getRef('cxmDomains'), {node: rec});
						} else if(type === 'domain') {
							WT.showContextMenu(e, me.getRef('cxmDomain'), {node: rec});
						}
					}
				}
			}]
		}));
		
		me.setMainComponent(Ext.create({
			xtype: 'tabpanel'
		}));
	},
	
	trAdmin: function() {
		return this.getToolComponent().lookupReference('tradmin');
	},
	
	initActions: function() {
		var me = this;
		me.addAction('addDomain', {
			handler: function() {
				me.addDomainUI();
			}
		});
		me.addAction('editDomain', {
			handler: function() {
				var node = me.getCurrentDomainNode();
				if(node) me.editDomainUI(node);
			}
		});
		me.addAction('deleteDomain', {
			handler: function() {
				var node = me.getCurrentDomainNode();
				if(node) me.deleteDomainUI(node);
			}
		});
	},
	
	initCxm: function() {
		var me = this;
		me.addRef('cxmDomains', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('addDomain')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('addDomain');
				}
			}
		}));
		me.addRef('cxmDomain', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('editDomain'),
				me.getAction('deleteDomain'),
				me.getAction('addDomain')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('editDomain');
					me.updateDisabled('deleteDomain');
					me.updateDisabled('addDomain');
				}
			}
		}));
	},
	
	getCurrentDomainNode: function() {
		var sel = this.trAdmin().getSelection();
		return sel.length > 0 ? sel[0] : null;
	},
	
	showSettingsUI: function(node) {
		var me = this,
				itemId = WTU.forItemId(node.getId());
		
		me.showTab(itemId, function() {
			return Ext.create('Sonicle.webtop.core.admin.view.Settings', {
				mys: me,
				itemId: itemId,
				closable: true
			});
		});
	},
	
	showDomainSettingsUI: function(node) {
		var me = this,
				itemId = WTU.forItemId(node.getId());
		
		me.showTab(itemId, function() {
			return Ext.create('Sonicle.webtop.core.admin.view.DomainSettings', {
				mys: me,
				itemId: itemId,
				domainId: node.get('_domainId'),
				closable: true
			});
		});
	},
	
	showDomainUsersUI: function(node) {
		var me = this,
				itemId = WTU.forItemId(node.getId());
		
		me.showTab(itemId, function() {
			return Ext.create('Sonicle.webtop.core.admin.view.DomainUsers', {
				mys: me,
				itemId: itemId,
				domainId: node.get('_domainId'),
				passwordPolicy: node.get('_passwordPolicy'),
				dirCapPasswordWrite: node.get('_dirCapPasswordWrite'),
				dirCapUsersWrite: node.get('_dirCapUsersWrite'),
				closable: true
			});
		});
	},
	
	showDomainRolesUI: function(node) {
		var me = this,
				itemId = WTU.forItemId(node.getId());
		
		me.showTab(itemId, function() {
			return Ext.create('Sonicle.webtop.core.admin.view.DomainRoles', {
				mys: me,
				itemId: itemId,
				domainId: node.get('_domainId'),
				closable: true
			});
		});
	},
	
	addDomainUI: function() {
		this.addDomain({
			callback: function(success) {
				if(success) this.loadTreeNode('domains');
			}
		});
	},
	
	editDomainUI: function(node) {
		var me = this;
		me.editDomain(node.get('_domainId'), {
			callback: function(success) {
				if(success) this.loadTreeNode('domains');
			}
		});
	},
	
	deleteDomainUI: function(node) {
		var me = this,
				sto = me.trAdmin().getStore();
		WT.confirm(me.res('store.confirm.delete', Ext.String.ellipsis(node.get('text'), 40)), function(bid) {
			if(bid === 'yes') {
				me.deleteDomain(node.get('_domainId'), {
					callback: function(success) {
						if(success) sto.remove(node);
					}
				});
			}
		});
	},
	
	addDomain: function(opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.Domain');
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('new', {
				data: {
					enabled: true,
					dirScheme: 'ldapwebtop',
					dirCaseSensitive: false,
					dirPasswordPolicy: true,
					userAutoCreation: false
				}
			});
		});
	},
	
	editDomain: function(domainId, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.Domain');
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('edit', {
				data: {
					id: domainId
				}
			});
		});
	},
	
	deleteDomain: function(domainId, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageDomains', {
			params: {
				crud: 'delete',
				domainId: domainId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	addRole: function(domainId, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.Role');
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('new', {
				data: {
					domainId: domainId
				}
			});
		});
	},
	
	editRole: function(roleUid, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.Role');
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('edit', {
				data: {
					roleUid: roleUid
				}
			});
		});
	},
	
	addUser: function(passwordPolicy, domainId, userId, firstName, lastName, displayName, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.User', {
					viewCfg: {
						domainId: domainId,
						passwordPolicy: passwordPolicy
					}
				});
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('new', {
				data: {
					domainId: domainId,
					userId: userId,
					enabled: true,
					firstName: firstName,
					lastName: lastName,
					displayName: displayName
				}
			});
		});
	},
	
	editUser: function(profileId, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.User', {
					viewCfg: {
						domainId: WT.fromPid(profileId).domainId
					}
				});
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('edit', {
				data: {
					profileId: profileId
				}
			});
		});
	},
	
	updateUsersStatus: function(profileIds, enabled, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageDomainUsers', {
			params: {
				crud: enabled ? 'enable' : 'disable',
				profileIds: WTU.arrayAsParam(profileIds)
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	changeUserPassword: function(profileId, oldPassword, newPassword, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ChangeUserPassword', {
			params: {
				profileId: profileId,
				oldPassword: oldPassword,
				newPassword: newPassword
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json]);
			}
		});
	},
	
	deleteUsers: function(deep, profileIds, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageDomainUsers', {
			params: {
				crud: 'delete',
				deep: deep,
				profileIds: WTU.arrayAsParam(profileIds)
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	showTab: function(itemId, createFn) {
		var me = this,
				pnl = me.getMainComponent(),
				tab;
		
		tab = pnl.getComponent(itemId);
		if(!tab) tab = pnl.add(createFn());
		pnl.setActiveTab(tab);
	},
	
	loadTreeNode: function(node) {
		var me = this,
				sto = me.trAdmin().getStore(),
				no;
		if(node && node.isNode) {
			no = node;
		} else {
			no = sto.getNodeById(node);
		}
		if(no) sto.load({node: no});
	},
	
	/**
	 * @private
	 */
	updateDisabled: function(action) {
		var me = this,
				dis = me.isDisabled(action);
		me.setActionDisabled(action, dis);
	},
	
	/**
	 * @private
	 */
	isDisabled: function(action) {
		var me = this, sel;
		switch(action) {
			case 'editDomain':
				sel = me.getCurrentDomainNode();
				return sel ? false : true;
			case 'deleteDomain':
				sel = me.getCurrentDomainNode();
				return sel ? false : true;
			case 'addDomain':
				return false;
				//if(!me.isPermitted('STORE_OTHER', 'CREATE')) return true;
		}
	}
});
