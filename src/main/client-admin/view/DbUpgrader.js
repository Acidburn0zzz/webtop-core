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
Ext.define('Sonicle.webtop.core.admin.view.DbUpgrader', {
	extend: 'WTA.sdk.DockableView',
	requires: [
		'Sonicle.webtop.core.admin.model.GridUpgradeRow'
	],
	
	dockableConfig: {
		title: '{dbUpgrader.tit}',
		iconCls: 'wta-icon-dbUpgrader-xs'
	},
	
	nextStmtId: null,
	
	constructor: function(cfg) {
		var me = this;
		me.callParent([cfg]);
	},
	
	initComponent: function() {
		var me = this;
		
		Ext.apply(me, {
			tbar: [
				me.addAction('select', {
					text: null,
					tooltip: me.mys.res('dbUpgrader.act-select.tip'),
					iconCls: 'wta-icon-selectStmt-xs',
					handler: function() {
						me.selectStmt(me.nextStmtId);
					}
				}),
				'-',
				me.addAction('play1', {
					text: me.mys.res('dbUpgrader.act-play1.lbl'),
					iconCls: 'wta-icon-play1Stmt-xs',
					handler: function() {
						me.executeStmt('play1');
					}
				}),
				me.addAction('play', {
					text: me.mys.res('dbUpgrader.act-play.lbl'),
					iconCls: 'wta-icon-playStmt-xs',
					handler: function() {
						me.executeStmt('play');
					}
				}),
				me.addAction('skip', {
					text: me.mys.res('dbUpgrader.act-skip.lbl'),
					iconCls: 'wta-icon-skipStmt-xs',
					handler: function() {
						me.executeStmt('skip');
					}
				}), '->',
				{
					xtype: 'tbtext',
					reference: 'tbitag'
				}
			],
			bbar: [{
				xtype: 'tbtext',
				reference: 'tbipending'
			}, '-', {
				xtype: 'tbtext',
				reference: 'tbiok'
			}, '-', {
				xtype: 'tbtext',
				reference: 'tbiskipped'
			}]
		});
		me.callParent(arguments);
		
		me.add({
			region: 'center',
			xtype: 'wtfieldspanel',
			layout: {
				type: 'vbox',
				align: 'stretch'
			},
			items: [{
				xtype: 'textareafield',
				reference: 'fldstmtbody',
				bind: {
					disabled: '{!gp.selection}'
				},
				grow: true,
				fieldLabel: me.mys.res('dbUpdater.stmtBody.lbl')
			}, {
				xtype: 'displayfield',
				bind: {
					value: '{gp.selection.runStatus}',
					disabled: '{!gp.selection}'
				},
				fieldLabel: me.mys.res('dbUpdater.runStatus.lbl')
			}, {
				xtype: 'textareafield',
				bind: {
					value: '{gp.selection.runMessage}',
					disabled: '{!gp.selection}'
				},
				editable: false,
				grow: true,
				fieldLabel: me.mys.res('dbUpdater.runMessage.lbl')
			}, {
				xtype: 'textareafield',
				bind: {
					value: '{gp.selection.comments}',
					disabled: '{!gp.selection}'
				},
				editable: false,
				grow: true,
				fieldLabel: me.mys.res('dbUpdater.comments.lbl')
			}, {
				xtype: 'grid',
				reference: 'gp',
				store: {
					autoLoad: true,
					model: 'Sonicle.webtop.core.admin.model.GridUpgradeRow',
					proxy: WTF.apiProxy(me.mys.ID, 'ManageDbUpgrades', 'stmts'),
					listeners: {
						load: function(s) {
							var o = s.getProxy().getReader().rawData;
							me.nextStmtId = o.nextStmtId;
							me.updateInfo(o);
							me.selectStmt(o.selected);
						}
					}
				},
				columns: [{
					xtype: 'rownumberer'
				}, {
					dataIndex: 'serviceId',
					header: me.mys.res('dbUpdater.gp.serviceId.lbl'),
					flex: 2
				}, {
					dataIndex: 'sequenceNo',
					header: me.mys.res('dbUpdater.gp.sequenceNo.lbl'),
					width: 40
				}, {
					dataIndex: 'stmtDataSource',
					header: me.mys.res('dbUpdater.gp.stmtDataSource.lbl'),
					flex: 2
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'runStatus',
					hideText: false,
					getIconCls: function(v) {
						return Ext.isEmpty(v) ? '' : ('wta-icon-stmtStatus-' + v.toLowerCase() + '-xs');
					},
					iconSize: WTU.imgSizeToPx('xs'),
					header: me.mys.res('dbUpdater.gp.runStatus.lbl'),
					minWidth: 120,
					flex: 1
				}, {
					dataIndex: 'stmtBody',
					header: me.mys.res('dbUpdater.gp.stmtBody.lbl'),
					flex: 3
				}],
				flex: 1
			}]
		});
		
		me.getViewModel().bind({
			bindTo: '{gp.selection}'
		}, function(sel) {
			if (sel) me.lref('fldstmtbody').setValue(sel.get('stmtBody'));
			me.updateDisabled('select');
			me.updateDisabled('play1');
			me.updateDisabled('play');
			me.updateDisabled('skip');
		});
	},
	
	selectStmt: function(id) {
		var gp = this.lref('gp'),
				rec = gp.getStore().getById(id);
		if (rec) {
			gp.setSelection(rec);
		} else {
			gp.getSelectionModel().deselectAll();
		}
	},
	
	updateInfo: function(o) {
		var me = this;
		me.lref('tbipending').setText(me.mys.res('dbUpdater.tbi-pending.lbl') + ': ' + o.pendingCount);
		me.lref('tbiok').setText(me.mys.res('dbUpdater.tbi-ok.lbl') + ': ' + o.okCount);
		me.lref('tbiskipped').setText(me.mys.res('dbUpdater.tbi-skipped.lbl') + ': ' + o.skippedCount);
		me.lref('tbitag').setText(me.mys.res('dbUpdater.tbi-tag.lbl') + ': ' + o.upgradeTag);
	},
	
	updateStmts: function(o) {
		var sto = this.lref('gp').getStore(), rec;
		Ext.iterate(o.data, function(recd) {
			rec = sto.getById(recd['upgradeStmtId']);
			if (rec) rec.set(recd);
		});
	},
	
	executeStmt: function(mode) {
		var me = this,
				sel = me.getSelectedStmt(),
				pars = {crud: mode};
		
		if (mode === 'play1') {
			Ext.apply(pars, {
				stmtId: (sel && sel.getId() === me.nextStmtId) ? sel.getId() : null,
				stmtBody: me.lref('fldstmtbody').getValue()
			});
		} else if (mode === 'play') {
			Ext.apply(pars, {
				stmtBody: me.lref('fldstmtbody').getValue()
			});
		} else if (mode === 'skip') {
			Ext.apply(pars, {
				stmtId: (sel && sel.getId() === me.nextStmtId) ? sel.getId() : null
			});
		} else {
			Ext.raise('Mode "' + mode + '" not supported');
		}
		
		me.wait();
		WT.ajaxReq(me.mys.ID, 'ManageDbUpgrades', {
			params: pars,
			callback: function(success, o) {
				me.unwait();
				if(success) {
					me.nextStmtId = o.nextStmtId;
					me.updateInfo(o);
					me.updateStmts(o);
					me.selectStmt(o.selected);
				}
			}
		});
	},
	
	/*
	_selectStatement: function(id) {
		var gp = this.gpStatements;
		var sm = gp.getSelectionModel();
		if(id) {
			var i = gp.getStore().indexOfId(id);
			sm.selectRow(i, false);
			gp.getView().focusRow(i);
		} else {
			sm.clearSelections();
			this._h_selectionChange(sm);
		}
	},
	*/
	
	
	
	
	
	
	
	getSelectedStmt: function() {
		var sel = this.lref('gp').getSelection();
		return sel.length === 1 ? sel[0] : null;
	},
	
	updateDisabled: function(action) {
		var me = this,
				dis = me.isDisabled(action);
		me.setActionDisabled(action, dis);
	},
	
	/**
	 * @private
	 */
	isNextStmt: function(rec) {
		return rec.getId() === this.nextStmtId;
	},
	
	/**
	 * @private
	 */
	isBeforeNextStmt: function(rec) {
		return rec.getId() < this.nextStmtId;
	},
	
	/**
	 * @private
	 */
	isDisabled: function(action) {
		var me = this, sel;
		switch(action) {
			case 'select':
				if (me.nextStmtId !== null) {
					return false;
				} else {
					return true;
				}
			case 'play1':
				sel = me.getSelectedStmt();
				if(sel && (me.isNextStmt(sel) || (me.isBeforeNextStmt(sel) && sel.isStatusSkipped()))) {
					return false;
				} else {
					return true;
				}
			case 'play':
				sel = me.getSelectedStmt();
				if(sel && me.isNextStmt(sel)) {
					return false;
				} else {
					return true;
				}
			case 'skip':
				if(sel && (me.isNextStmt(sel) || (me.isBeforeNextStmt(sel) && sel.isStatusError()))) {
					return false;
				} else {
					return true;
				}
		}
	}
});