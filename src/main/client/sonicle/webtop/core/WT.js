Ext.define('Sonicle.webtop.core.WT', {
	alternateClassName: 'WT',
	singleton: true,
	
	/**
	 * @property
	 * Core service ID.
	 */
	ID: 'com.sonicle.webtop.core',
	/**
	 * @property
	 * Core service short ID.
	 */
	XID: 'wt',
	/**
	 * @property
	 * Core service namespace.
	 */
	NS: 'Sonicle.webtop.core',
	
	palette: [
		'AC725E','D06B64','F83A22','FA573C','FF7537','FFAD46','FAD165','FBE983',
		'4986E7','9FC6E7','9FE1E7','92E1C0','42D692','16A765','7BD148','B3DC6C',
		'9A9CFF','B99AFF','A47AE2','CD74E6','F691B2','CCA6AC','CABDBF','C2C2C2',
		'FFFFFF'
	],
	
	loadedCss: null,
	
	constructor: function(cfg) {
		var me = this;
		me.loadedCss = {};
		me.resetHtmlCharEntities();
		me.callParent(cfg);
	},
	
	/**
	 * Returns the application.
	 * This is shorthand reference to Sonicle.webtop.core.getApplication().
	 * @returns {Sonicle.webtop.core.Application} The resulting object.
	 */
	getApp: function() {
		return Sonicle.webtop.core.getApplication();
	},
	
	getColorPalette: function() {
		return this.palette;
	},
	
	reload: function() {
		window.location.reload();
	},
	
	/**
	 * Convenience method for prepending namespace to class name.
	 * @param {String} [ns] The namespace. If not specified, WT.NS is used.
	 * @param {String} cn The class name.
	 * @returns {String} The resulting string.
	 */
	preNs: function(ns, cn) {
		if(arguments.length === 1) {
			cn = ns;
			ns = WT.NS;
		}
		return Ext.String.format('{0}.{1}', ns, cn);
	},
	
	/**
	 * Gets the initial setting value bound to key.
	 * @param {String} [id] The service ID.
	 * @param {String} key The key.
	 * @return {Mixed} Setting value.
	 */
	getOption: function(id, key) {
		if(arguments.length === 1) {
			key = id;
			id = WT.ID;
		}
		var i = this.getApp().getDescriptor(id).getIndex();
		return (WTS.servicesOptions[i] || {})[key];
	},
	
	/**
	 * Returns a string resource.
	 * If id and key are both filled, any other arguments will be used in
	 * conjunction with {@link Ext.String#format} method in order to replace
	 * tokens defined in resource string.
	 * @param {String} [id] The service ID.
	 * @param {String} key The resource key.
	 * @param {Mixed...} [values] The values to use within {@link Ext.String#format} method.
	 * @returns {String} The (formatted) value.
	 */
	res: function(id, key) {
		if(arguments.length === 1) {
			key = id;
			id = WT.ID;
		}
		
		var loc = WT.getApp().getLocale(id);
		if(!loc) return undefined;
		if(arguments.length > 2) {
			var str = loc.strings[key],
					args = Ext.Array.slice(arguments, 2);
			return (Ext.isDefined(str)) ? Ext.String.format(str, args) : loc.strings[key];
		} else {
			return loc.strings[key];
		}
	},
	
	/**
	 * Utility function to return a resource string or string itself.
	 * If passed string contains a resource key preceeded by @ character,
	 * res method is called instead returning the passed value.
	 * @param {type} [id] The service ID.
	 * @param {type} str The string.
	 * @returns {String} The value.
	 */
	resStr: function(id, str) {
		if(arguments.length === 1) {
			str = id;
			id = WT.ID;
		}
		return (str.substr(0, 1) === '@') ? WT.res(id, str.substr(1)) : str;
	},
	
	/**
	 * Builds CSS class name namespacing it using service xid.
	 * @param {String} xid Service short ID.
	 * @param {String} name The CSS class name part.
	 * @return {String} The concatenated CSS class name.
	 */
	cssCls: function(xid, name) {
		return Ext.String.format('{0}-{1}', xid, name);
	},
	
	/**
	 * Builds CSS class name for icons namespacing it using service xid.
	 * For example, using 'service' as name, it will return '{xid}-icon-service'.
	 * Using 'service-l' as name it will return '{xid}-icon-service-l'.
	 * Likewise, using 'service' as name and 'l' as size it will return the
	 * same value: '{xid}-icon-service-l'.
	 * @param {String} xid Service short ID.
	 * @param {String} name The icon name part.
	 * @param {String} [size] Icon size (one of xs,s,m,l).
	 * @return {String} The concatenated CSS class name.
	 */
	cssIconCls: function(xid, name, size) {
		if(size === undefined) {
			return Ext.String.format('{0}-icon-{1}', xid, name);
		} else {
			return Ext.String.format('{0}-icon-{1}-{2}', xid, name, size);
		}
	},
	
	returnIf: function(value, ifEmpty) {
		return (Ext.isEmpty(value)) ? ifEmpty : value;
	},
	
	isXType: function(obj, xtype) {
		if(!Ext.isObject(obj)) return false;
		if(!Ext.isFunction(obj.isXType)) return false;
		return obj.isXType(xtype);
	},
	
	isAction: function(obj) {
		if(!Ext.isObject(obj)) return false;
		return (obj.isAction && Ext.isFunction(obj.execute));
	},
	
	proxy: function(svc, act, rootp, opts) {
		opts = opts || {};
		return {
			type: 'ajax',
			url: 'service-request',
			extraParams: Ext.apply(opts.extraParams || {}, {
				service: svc,
				action: act
			}),
			reader: Ext.apply(opts.reader || {}, {
				type: 'json',
				rootProperty: rootp || 'data',
				messageProperty: 'message'
			}),
			listeners: {
				exception: function(proxy, request, operation, eOpts) {
					//TODO: intl. user error message plus details
					WT.error('Error during action "'+act+'" on service "'+svc+'"',"Ajax Error");
				}
			}
		};
	},
	
	apiProxy: function(svc, act, rootp, opts) {
		rootp = rootp || 'data';
		opts = opts || {};
		return {
			type: 'ajax',
			api: {
				create: 'service-request?crud=create',
				read: 'service-request?crud=read',
				update: 'service-request?crud=update',
				destroy: 'service-request?crud=delete'
			},
			extraParams: Ext.apply(opts.extraParams || {}, {
				service: svc,
				action: act
			}),
			reader: Ext.apply({
				type: 'json',
				rootProperty: rootp,
				messageProperty: 'message'
			}, opts.reader || {}),
			writer: Ext.apply({
				type: 'json',
				writeAllFields: true
			}, opts.writer || {})
		};
	},
	
	optionsProxy: function(svc) {
		return WT.apiProxy(svc, 'UserOptions', 'data', {
			extraParams: {
				options: true
			}
		});
	},
	
	componentLoader: function(svc, act, opts) {
		if(!opts) opts = {};
		return {
			url: 'service-request',
			params: Ext.applyIf({
				service: svc,
				action: act
			}, opts.params || {}),
			contentType: 'html',
			loadMask: true
		};
	},
	
	/**
	 * Makes an Ajax request to server.
	 * @param {String} svc The service ID.
	 * @param {String} act The service action to call.
	 * @param {Object} [opts] Config options.
	 * @param {Function} [opts.callback] The callback function to call.
	 * @param {Boolean} opts.callback.success
	 * @param {Object} opts.callback.json
	 * @param {Object} opts.callback.opts
	 * @param {Object} [opts.scope] The scope (this) for the supplied callbacks.
	 */
	ajaxReq: function(svc, act, opts) {
		var me = this;
		opts = opts || {};
		var fn = opts.callback, scope = opts.scope, sfn = opts.success, ffn = opts.failure;
		var options = {
			url: 'service-request',
			method: 'POST',
			params: Ext.applyIf({
				service: svc,
				action: act
			}, opts.params || {}),
			headers: {"Content-Type": "application/x-www-form-urlencoded; charset=utf-8"},
			success: function(resp, opts) {
				var obj = Ext.decode(resp.responseText);
				if(sfn) Ext.callback(sfn, scope || me, [opts]);
				Ext.callback(fn, scope || me, [obj.success, obj, opts]);
			},
			failure: function(resp, opts) {
				if(ffn) Ext.callback(ffn, scope || me, [opts]);
				Ext.callback(fn, scope || me, [false, null, opts]);
			},
			scope: me
		};
		if(opts.timeout) Ext.apply(options, {timeout: opts.timeout});
		Ext.Ajax.request(options);
	},
	
	/**
	 * Loads a CSS file by adding in the page a new link element.
	 * @param {String} url The URL from which to load the CSS.
	 */
	loadCss: function(url) {
		var me = this;
		//if(!me.loadedCss) me.loadedCss = {};
		if(!me.loadedCss[url]) {
			var doc = window.document;
			var link = doc.createElement('link');
			link.rel = 'stylesheet';
			link.type = 'text/css';
			link.href = url;
			doc.getElementsByTagName('head')[0].appendChild(link);
			me.loadedCss[url] = url;
		}
	},
	
	/**
	 * Asynchronously loads the specified script URL and calls the supplied 
	 * callbacks. A success flag is passed to callback function in order to
	 * determine operation result.
	 * @param {String} url The URL from which to load the script.
	 * @param {Function} cb The callback to call.
	 * @param {Object} scope The scope (this) for the supplied callbacks.
	 */
	loadScriptAsync: function(url,cb,scope) {
		var me = this;
		Ext.Loader.loadScript({
			url: 'resources/'+url,
			onLoad: function() {
				Ext.callback(cb, scope || me, [true]);
			},
			onError: function() {
				Ext.callback(cb, scope || me, [false]);
			},
			scope: me
		});
	},
	
	/**
	 * Decodes (parses) a properties text to an object.
	 * @param {String} text The properties string.
	 * @returns {Object} The resulting object.
	 */
	decodeProps: function(text) {
		var i1, i2 = -1, line, ieq;
		var hm = {}, key, val;
		var done = false;
		while(!done) {
			i1 = i2+1;
			i2 = text.indexOf('\n', i1);
			line = null;
			if(i2 < 0) {
				if(i1 < text.length) line = text.substring(i1, text.length);
				done = true;
			} else {
				line = text.substring(i1, i2);
			}
			if(line) {
				ieq = line.indexOf('=');
				if(ieq < 0) continue;
				key = line.substring(0, ieq);
				val = line.substring(ieq+1);
				hm[key] = val;
			}
		}
		return hm;
	},
	
	/**
	 * Displays an information message.
	 * @param {String} msg The message to display.
	 * @param {Object} [opts] Config options.
	 * @param {String} opts.title A custom title.
	 * @param {Number} opts.buttons A custom bitwise button specifier.
	 */
	info: function(msg, opts) {
		opts = opts || {};
		Ext.Msg.show({
			title: opts.title || WT.res('info'),
			message: msg,
			buttons: opts.buttons || Ext.MessageBox.OK,
			icon: Ext.MessageBox.INFO
		});
	},
	
	/**
	 * Displays a warning message.
	 * @param {String} msg The message to display.
	 * @param {Object} [opts] Config options.
	 * @param {String} opts.title A custom title.
	 * @param {Number} opts.buttons A custom bitwise button specifier.
	 */
	warn: function(msg, opts) {
		opts = opts || {};
		Ext.Msg.show({
			title: opts.title || WT.res('warning'),
			message: msg,
			buttons: opts.buttons || Ext.MessageBox.OK,
			icon: Ext.MessageBox.WARNING
		});
	},
	
	/**
	 * Displays an error message.
	 * @param {String} msg The message to display.
	 * @param {Object} [opts] Config options.
	 * @param {String} opts.title A custom title.
	 * @param {Number} opts.buttons A custom bitwise button specifier.
	 */
	error: function(msg, opts) {
		opts = opts || {};
		Ext.Msg.show({
			title: opts.title || WT.res('error'),
			message: msg,
			buttons: opts.buttons || Ext.MessageBox.OK,
			icon: Ext.MessageBox.ERROR
		});
	},
	
	/**
	 * Displays a confirm message using classic YES+NO buttons.
	 * @param {String} msg The message to display.
	 * @param {Function} cb A callback function which is called after a choice.
	 * @param {String} cb.buttonId The ID of the button pressed.
	 * @param {Object} scope The scope (`this` reference) in which the function will be executed.
	 * @param {Object} opts [opts] Config options.
	 */
	confirm: function(msg, cb, scope, opts) {
		opts = opts || {};
		Ext.Msg.show({
			title: opts.title || WT.res('confirm'),
			message: msg,
			buttons: opts.buttons || Ext.Msg.YESNO,
			icon: Ext.Msg.QUESTION,
			fn: function(bid) {
				Ext.callback(cb, scope, [bid]);
			}
		});
	},
	
	/**
	 * Displays a confirm message using YES+NO+CANCEL buttons.
	 * @param {String} msg The message to display.
	 * @param {Function} cb A callback function which is called after a choice.
	 * @param {String} cb.buttonId The ID of the button pressed.
	 * @param {Object} scope The scope (`this` reference) in which the function will be executed.
	 * @param {Object} opts [opts] Config options.
	 */
	confirmYNC: function(msg, cb, scope, opts) {
		this.confirm(msg, cb, scope, Ext.apply({
			buttons: Ext.Msg.YESNOCANCEL
		}, opts));
	},
	
	//DELETE
	wsMsg: function(service, action, config) {
		return Ext.JSON.encode(Ext.apply(config||{},{ service: service, action: action }));
	},
	
	/*
	 * Builds the src url of a themed image for a service
	 * 
	 * @param {String} sid The service id
	 * @param {String} relPath The relative icon path
	 * @return {String} the imageUrl
	 */
	imageUrl: function(sid, relPath) {
		return Ext.String.format('resources/{0}/laf/{1}/{2}', sid, WT.getOption('laf'), relPath);
	},
	
	/*
	 * Builds the img tag of a themed image for a service
	 * 
	 * @param {String} sid The service id
	 * @param {String} relPath The relative icon path
	 * @param {int} width The icon width
	 * @param {int} height The icon height
	 * @param {String} [others] other custom tag properties
	 * @return {String} the complete image tag
	 */
	imageTag: function(sid,relPath,width,height,others) {
		var src=this.imageUrl(sid,relPath);
		return Ext.String.format('<img src="{0}" width={1} height={2} {3} >',src,width,height,others||'');
	},
	
	/*
	 * Builds the img tag of a core generic image
	 * 
	 * @param {String} relPath The relative icon path
	 * @param {int} width The icon width
	 * @param {int} height The icon height
	 * @param {String} [others] other custom tag properties
	 * @return {String} the complete image tag
	 */
	coreImageTag: function(relPath,width,height,others) {
		var src=this.imageUrl(WT.ID,relPath);
		return Ext.String.format('<img src="{0}" width={1} height={2} {3} >',src,width,height,others||'');
	},
	
	/*
	 * Builds the src url of a global image
	 * 
	 * @param {String} relPath The relative icon path
	 * @return {String} the imageUrl
	 */
	globalImageUrl: function(relPath) {
		return Ext.String.format('resources/{0}/images/{1}',WT.ID,relPath);
	},
	
	/*
	 * Builds the img tag of a core generic image
	 * 
	 * @param {String} relPath The relative icon path
	 * @param {int} width The icon width
	 * @param {int} height The icon height
	 * @param {String} [others] other custom tag properties
	 * @return {String} the complete image tag
	 */
	globalImageTag: function(relPath,width,height,others) {
		var src=this.globalImageUrl(relPath);
		return Ext.String.format('<img src="{0}" width={1} height={2} {3} >',src,width,height,others||'');
	},
	
	/*
	 * Build human readable version of integer number
	 * @param {Integer} value The integer number.
	 * @return {String} A human readable string.
	 */
	getSizeString: function(value) {
		var s = value;
		value = parseInt(value/1024);
		if (value > 0) {
			if (value < 1024) s = value + "KB";
			else s = parseInt(value/1024) + "MB";
		}
		return s;
	},
	
	/**
	 * Adds a set of character entity definitions to the set used by
	 * {@link WT#encodeHtmlEntities} and {@link WT#decodeHtmlEntities}.
	 * 
	 * This object should be keyed by the entity name sequence,
	 * with the value being the textual representation of the entity.
	 * 
	 * @param {Object} entObj The set of character entities to add to the current definitions.
	 */
	addHtmlCharEntities: function(entObj) {
		var me = this, charKeys = [], entityKeys = [], key, echar;
		for (key in entObj) {
			echar = entObj[key];
			me.entityToChar[key] = echar;
			me.charToEntity[echar] = key;
			charKeys.push(echar);
			entityKeys.push(key);
		}
		me.charToEntityRegex = new RegExp('(' + charKeys.join('|') + ')', 'g');
		me.entityToCharRegex = new RegExp('(' + entityKeys.join('|') /*+ '|&#[0-9]{1,5};'*/ + ')', 'g');
	},
	
	/**
	 * Resets the set of character entity definitions used by 
	 * {@link WT#encodeHtmlEntities} and {@link WT#decodeHtmlEntities} 
	 * back to the default state.
	 */
	resetHtmlCharEntities: function() {
		var me = this;
		me.charToEntity = {};
		me.entityToChar = {};
		// add the default set
		me.addHtmlCharEntities({
			'&agrave;':'à',
			'&aacute;':'á',
			'&egrave;':'è',
			'&eacute;':'é',
			'&igrave;':'ì',
			'&iacute;':'í',
			'&ograve;':'ò',
			'&oacute;':'ó',
			'&ugrave;':'ù',
			'&uacute;':'ú'
		});
	},
	
	/**
	 * Convert certain special characters (à, è, etc..) to their HTML character equivalents for literal display in web pages.
	 * @param {String} value The string to encode.
	 * @returns {String} The encoded text.
	 */
	encodeHtmlEntities: function(value) {
		var me = this;
		var htmlEncodeReplaceFn = function(match, capture) {
			return me.charToEntity[capture];
		};
		return (!value) ? value : String(value).replace(me.charToEntityRegex, htmlEncodeReplaceFn);
	},
	
	/**
	 * Convert certain special characters (à, è, etc..) from their HTML character equivalents.
	 * @param {String} value The string to decode.
	 * @returns {String} The decoded text.
	 */
	decodeHtmlEntities: function(value) {
		var me = this;
		var htmlDecodeReplaceFn = function(match, capture) {
            return (capture in me.entityToChar) ? me.entityToChar[capture] : String.fromCharCode(parseInt(capture.substr(2), 10));
        };
		return (!value) ? value : String(value).replace(me.entityToCharRegex, htmlDecodeReplaceFn);
	}
	
});
