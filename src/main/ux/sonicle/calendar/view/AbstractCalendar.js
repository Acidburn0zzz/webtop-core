/**
 * @class Sonicle.calendar.view.AbstractCalendar
 * @extends Ext.BoxComponent
 * <p>This is an abstract class that serves as the base for other calendar views. This class is not
 * intended to be directly instantiated.</p>
 * <p>When extending this class to create a custom calendar view, you must provide an implementation
 * for the <code>renderItems</code> method, as there is no default implementation for rendering events
 * The rendering logic is totally dependent on how the UI structures its data, which
 * is determined by the underlying UI template (this base class does not have a template).</p>
 * @constructor
 * @param {Object} config The config object
 */
Ext.define('Sonicle.calendar.view.AbstractCalendar', {
	extend: 'Ext.Component',
	alias: 'widget.calendarview',
	requires: [
		'Sonicle.Date',
		'Sonicle.calendar.data.EventMappings'
	],
	
	/**
	 * @cfg {Number} dayCount
	 * The number of days to display in the view (defaults to 1)
	 */
	dayCount: 1,
	
	/**
	 * @cfg {Number} startDay
	 * The 0-based index for the day on which the calendar week begins (0=Sunday, which is the default)
	 */
	startDay: 0,
	
	/**
	 * @cfg {Boolean} startDayIsStatic
	 * If you set <tt>startDayIsStatic</tt> to <tt>true</tt>, then the view will *always* begin on
	 * {@link #startDay}. For any {@link #dayCount} less than 7, days outside the startDay + dayCount range
	 * will not be viewable. If a date that is not in the viewable range is set into the view it will
	 * automatically advance to the first viewable date for the current range.  This could be useful for
	 * creating custom views like a weekday-only or weekend-only view.
	 * 
	 * Some example {@link Sonicle.calendar.CalendarPanel CalendarPanel} configs:
	 * 
	 *		// Weekdays only:
	 *		showMultiDayView: true,
	 *		multiDayViewCfg: {
	 *			dayCount: 5,
	 *			startDay: 1,
	 *			startDayIsStatic: true
	 *		}
	 *
	 *		// Weekends only:
	 *		showMultiDayView: true,
	 *		multiDayViewCfg: {
	 *			dayCount: 2,
	 *			startDay: 6,
	 *			startDayIsStatic: true
	 *		}
	 */
	startDayIsStatic: false,
	
	/**
	 * @cfg {Boolean} user24HourTime
	 * Determines whether times should be displayed as 12 hour times with am/pm (default)
	 * or 24 hour / military format.
	 */
	use24HourTime: false,
	
	/**
	 * @cfg {Boolean} spansHavePriority
	 * Allows switching between two different modes of rendering events that span multiple days. When true,
	 * span events are always sorted first, possibly at the expense of start dates being out of order (e.g., 
	 * a span event that starts at 11am one day and spans into the next day would display before a non-spanning 
	 * event that starts at 10am, even though they would not be in date order). This can lead to more compact
	 * layouts when there are many overlapping events. If false (the default), events will always sort by start date
	 * first which can result in a less compact, but chronologically consistent layout.
	 */
	spansHavePriority: false,
	
	/**
	 * @cfg {Boolean} trackMouseOver
	 * Whether or not the view tracks and responds to the browser mouseover event on contained elements (defaults to
	 * true). If you don't need mouseover event highlighting you can disable this.
	 */
	trackMouseOver: true,
	
	/**
	 * @cfg {Boolean} enableFx
	 * Determines whether or not visual effects for CRUD actions are enabled (defaults to true). If this is false
	 * it will override any values for {@link #enableAddFx}, {@link #enableUpdateFx} or {@link enableRemoveFx} and
	 * all animations will be disabled.
	 */
	enableFx: true,
	
	/**
	 * @cfg {Boolean} enableAddFx
	 * True to enable a visual effect on adding a new event (the default), false to disable it. Note that if 
	 * {@link #enableFx} is false it will override this value. The specific effect that runs is defined in the
	 * {@link #doAddFx} method.
	 */
	enableAddFx: true,
	
	/**
	 * @cfg {Boolean} enableUpdateFx
	 * True to enable a visual effect on updating an event, false to disable it (the default). Note that if 
	 * {@link #enableFx} is false it will override this value. The specific effect that runs is defined in the
	 * {@link #doUpdateFx} method.
	 */
	enableUpdateFx: false,
	
	/**
	 * @cfg {Boolean} enableRemoveFx
	 * True to enable a visual effect on removing an event (the default), false to disable it. Note that if 
	 * {@link #enableFx} is false it will override this value. The specific effect that runs is defined in the
	 * {@link #doRemoveFx} method.
	 */
	enableRemoveFx: true,
	
	/**
	 * @cfg {Boolean} enableDD
	 * True to enable drag and drop in the calendar view (the default), false to disable it
	 */
	enableDD: true,
	
	/**
	 * @cfg {Boolean} monitorResize
	 * True to monitor the browser's resize event (the default), false to ignore it. If the calendar view is rendered
	 * into a fixed-size container this can be set to false. However, if the view can change dimensions (e.g., it's in 
	 * fit layout in a viewport or some other resizable container) it is very important that this config is true so that
	 * any resize event propagates properly to all subcomponents and layouts get recalculated properly.
	 */
	monitorResize: true,
	
	/**
	 * @cfg {String} todayText
	 * The text to display in the current day's box in the calendar when {@link #showTodayText} is true (defaults to 'Today')
	 */
	todayText: 'Today',
	
	/**
	 * @cfg {String} ddCreateEventText
	 * The text to display inside the drag proxy while dragging over the calendar to create a new event (defaults to 
	 * 'Create event for {0}' where {0} is a date range supplied by the view)
	 */
	ddCreateEventText: 'Create event for {0}',
	
	/**
	 * @cfg {String} ddCopyEventText
	 * The text to display inside the drag proxy while alt-dragging an event to copy it (defaults to
	 * 'Copy event to {0}' where {0} is the updated event start date/time supplied by the view)
	 */
	ddCopyEventText: 'Copy event to {0}',
	
	/**
	 * @cfg {String} ddMoveEventText
	 * The text to display inside the drag proxy while dragging an event to reposition it (defaults to 
	 * 'Move event to {0}' where {0} is the updated event start date/time supplied by the view)
	 */
	ddMoveEventText: 'Move event to {0}',
	
	/**
	 * @cfg {String} ddResizeEventText
	 * The string displayed to the user in the drag proxy while dragging the resize handle of an event (defaults to 
	 * 'Update event to {0}' where {0} is the updated event start-end range supplied by the view). Note that 
	 * this text is only used in views
	 * that allow resizing of events.
	 */
	ddResizeEventText: 'Update event to {0}',
	
	/**
	 * @cfg {String} ddDateFormat
	 * String used for formatting date in texts ({@link #ddCreateEventText}, {@link #ddCopyEventText} or 
	 * {@link #ddMoveEventText}) displayed in the drag proxy while dragging an event.
	 */
	ddDateFormat: 'n/j',
	
	//private properties -- do not override:
	weekCount: 1,
	eventSelector: '.ext-cal-evt',
	eventOverClass: 'ext-evt-over',
	eventElIdDelimiter: '-evt-',
	dayElIdDelimiter: '-day-',
	
	/**
	 * Returns a string of HTML template markup to be used as the body portion of the event template created
	 * by {@link #getEventTemplate}. This provdes the flexibility to customize what's in the body without
	 * having to override the entire XTemplate. This string can include any valid {@link Ext.Template} code, and
	 * any data tokens accessible to the containing event template can be referenced in this string.
	 * @return {String} The body template string
	 */
	getEventBodyMarkup: Ext.emptyFn,
	// must be implemented by a subclass

	/**
	 * <p>Returns the XTemplate that is bound to the calendar's event store (it expects records of type
	 * {@link Sonicle.calendar.EventRecord}) to populate the calendar views with events. Internally this method
	 * by default generates different markup for browsers that support CSS border radius and those that don't.
	 * This method can be overridden as needed to customize the markup generated.</p>
	 * <p>Note that this method calls {@link #getEventBodyMarkup} to retrieve the body markup for events separately
	 * from the surrounding container markup.  This provdes the flexibility to customize what's in the body without
	 * having to override the entire XTemplate. If you do override this method, you should make sure that your 
	 * overridden version also does the same.</p>
	 * @return {Ext.XTemplate} The event XTemplate
	 */
	getEventTemplate: Ext.emptyFn,
	
	/**
	 * @event eventsrendered
	 * Fires after events are finished rendering in the view
	 * @param {Sonicle.calendar.view.AbstractCalendar} this 
	 */

	/**
	 * @event eventclick
	 * Fires after the user clicks on an event element
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.EventRecord} rec The {@link Sonicle.calendar.EventRecord record} for the event that was clicked on
	 * @param {HTMLNode} el The DOM node that was clicked on
	 */

	/**
	 * @event eventover
	 * Fires anytime the mouse is over an event element
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.EventRecord} rec The {@link Sonicle.calendar.EventRecord record} for the event that the cursor is over
	 * @param {HTMLNode} el The DOM node that is being moused over
	 */

	/**
	 * @event eventout
	 * Fires anytime the mouse exits an event element
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.EventRecord} rec The {@link Sonicle.calendar.EventRecord record} for the event that the cursor exited
	 * @param {HTMLNode} el The DOM node that was exited
	 */

	/**
	 * @event datechange
	 * Fires after the start date of the view changes
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Date} startDate The start date of the view (as explained in {@link #getStartDate}
	 * @param {Date} viewStart The first displayed date in the view
	 * @param {Date} viewEnd The last displayed date in the view
	 */

	/**
	 * @event rangeselect
	 * Fires after the user drags on the calendar to select a range of dates/times in which to create an event
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Object} dates An object containing the start (StartDate property) and end (EndDate property) dates selected
	 * @param {Function} callback A callback function that MUST be called after the event handling is complete so that
	 * the view is properly cleaned up (shim elements are persisted in the view while the user is prompted to handle the
	 * range selection). The callback is already created in the proper scope, so it simply needs to be executed as a standard
	 * function call (e.g., callback()).
	 */

	/**
	 * @event beforeeventcopy
	 * Fires before an existing event is duplicated by the user via the "copy" command. This is a
	 * cancelable event, so returning false from a handler will cancel the copy operation.
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.data.EventModel} rec The {@link Sonicle.calendar.data.EventModel
	 * record} for the event that will be copied
	 * @param {Date} dt The new start date to be set in the copy (the end date will be automaticaly
	 * adjusted to match the original event duration)
	 */

	/**
	 * @event eventcopy
	 * Fires after an event has been duplicated by the user via the "copy" command. If you need to
	 * cancel the copy operation you should handle the {@link #beforeeventcopy} event and return
	 * false from your handler function.
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.data.EventModel} rec The {@link Sonicle.calendar.data.EventModel
	 * record} for the event that was copied (with updated start and end dates)
	 */

	/**
	 * @event beforeeventmove
	 * Fires after an event element has been dragged by the user and dropped in a new position, but before
	 * the event record is updated with the new dates, providing a hook for canceling the update.
	 * To cancel the move, return false from a handling function. This could be useful for validating
	 * that a user can only move events within a certain date range, for example.
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.data.EventModel} rec The {@link Sonicle.calendar.data.EventModel record}
	 * for the event that will be moved. Start and end dates will be the original values before the move started.
	 * @param {Date} dt The new start date to be set (the end date will be automaticaly calculated to match
	 * based on the event duration)
	 */

	/**
	 * @event eventmove
	 * Fires after an event element has been moved to a new position and its data updated. If you need to
	 * cancel the move operation you should handle the {@link #beforeeventmove} event and return false
	 * from your handler function.
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.data.EventModel} rec The {@link Sonicle.calendar.data.EventModel record}
	 * for the event that was moved with updated start and end dates
	 */

	/**
	 * @event initdrag
	 * Fires when a drag operation is initiated in the view
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 */

	/**
	 * @event dayover
	 * Fires while the mouse is over a day element 
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Date} dt The date that is being moused over
	 * @param {Ext.core.Element} el The day Element that is being moused over
	 */

	/**
	 * @event dayout
	 * Fires when the mouse exits a day element 
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Date} dt The date that is exited
	 * @param {Ext.core.Element} el The day Element that is exited
	 */

	/*
	 * @event eventdelete
	 * Fires after an event element is deleted by the user. Not currently implemented directly at the view level -- currently 
	 * deletes only happen from one of the forms.
	 * @param {Sonicle.calendar.view.AbstractCalendar} this
	 * @param {Sonicle.calendar.EventRecord} rec The {@link Sonicle.calendar.EventRecord record} for the event that was deleted
	 */

	// must be implemented by a subclass
	// private
	initComponent: function () {
		var me = this;
		me.setStartDate(me.startDate || new Date());

		me.callParent(arguments);
	},
	
	// private
	afterRender: function () {
		var me = this;
		me.callParent(arguments);

		me.renderTemplate();

		if (me.store) {
			me.setStore(me.store, true);
		}

		me.el.on({
			'mouseover': me.onMouseOver,
			'mouseout': me.onMouseOut,
			'click': me.onClick,
			scope: me
		});

		me.el.unselectable();

		if (me.enableDD && me.initDD) {
			me.initDD();
		}

		me.on('eventsrendered', this.forceSize);
		Ext.defer(me.forceSize, 100, me);
	},
	
	// private
	forceSize: function () {
		if (this.el && this.el.down) {
			var hd = this.el.down('.ext-cal-hd-ct'),
					bd = this.el.down('.ext-cal-body-ct');

			if (bd == null || hd == null) {
				return;
			}

			var headerHeight = hd.getHeight(),
					sz = this.el.parent().getSize();

			bd.setHeight(sz.height - headerHeight);
		}
	},
	
	refresh: function () {
		this.prepareData();
		this.renderTemplate();
		this.renderItems();
	},
	
	getWeekCount: function () {
		var days = Sonicle.Date.diffDays(this.viewStart, this.viewEnd);
		return Math.ceil(days / this.dayCount);
	},
	
	// private
	prepareData: function () {
		var lastInMonth = Ext.Date.getLastDateOfMonth(this.startDate),
				w = 0, d,
				dt = Ext.Date.clone(this.viewStart),
				weeks = this.weekCount < 1 ? 6 : this.weekCount;

		this.eventGrid = [[]];
		this.allDayGrid = [[]];
		this.evtMaxCount = [];

		var evtsInView = this.store.queryBy(function (rec) {
			return this.isEventVisible(rec.data);
		},
				this);

		for (; w < weeks; w++) {
			this.evtMaxCount[w] = 0;
			if (this.weekCount === -1 && dt > lastInMonth) {
				//current week is fully in next month so skip
				break;
			}
			this.eventGrid[w] = this.eventGrid[w] || [];
			this.allDayGrid[w] = this.allDayGrid[w] || [];

			for (d = 0; d < this.dayCount; d++) {
				if (evtsInView.getCount() > 0) {
					var evts = evtsInView.filterBy(function (rec) {
						var startDt = Ext.Date.clearTime(rec.data[Sonicle.calendar.data.EventMappings.StartDate.name], true),
								startsOnDate = dt.getTime() == startDt.getTime(),
								spansFromPrevView = (w == 0 && d == 0 && (dt > rec.data[Sonicle.calendar.data.EventMappings.StartDate.name]));

						return startsOnDate || spansFromPrevView;
					},
							this);

					this.sortEventRecordsForDay(evts);
					this.prepareEventGrid(evts, w, d);
				}
				dt = Sonicle.Date.add(dt, {days: 1});
			}
		}
		this.currentWeekCount = w;
	},
	
	// private
	prepareEventGrid: function (evts, w, d) {
		var me = this,
				soDate = Sonicle.Date,
				row = 0,
				max = me.maxEventsPerDay ? me.maxEventsPerDay : 999;

		evts.each(function (evt) {
			var M = Sonicle.calendar.data.EventMappings,
					days = soDate.diffDays(
							soDate.max(me.viewStart, evt.data[M.StartDate.name]),
							soDate.min(me.viewEnd, evt.data[M.EndDate.name])) + 1;

			if (days > 1 || soDate.diffDays(evt.data[M.StartDate.name], evt.data[M.EndDate.name]) > 1) {
				me.prepareEventGridSpans(evt, me.eventGrid, w, d, days);
				me.prepareEventGridSpans(evt, me.allDayGrid, w, d, days, true);
			} else {
				row = me.findEmptyRowIndex(w, d);
				me.eventGrid[w][d] = me.eventGrid[w][d] || [];
				me.eventGrid[w][d][row] = evt;

				if (evt.data[M.IsAllDay.name]) {
					row = me.findEmptyRowIndex(w, d, true);
					me.allDayGrid[w][d] = me.allDayGrid[w][d] || [];
					me.allDayGrid[w][d][row] = evt;
				}
			}

			if (me.evtMaxCount[w] < me.eventGrid[w][d].length) {
				me.evtMaxCount[w] = Math.min(max + 1, me.eventGrid[w][d].length);
			}
			return true;
		});
	},
	
	// private
	prepareEventGridSpans: function (evt, grid, w, d, days, allday) {
		// this event spans multiple days/weeks, so we have to preprocess
		// the events and store special span events as placeholders so that
		// the render routine can build the necessary TD spans correctly.
		var w1 = w,
				d1 = d,
				row = this.findEmptyRowIndex(w, d, allday),
				dt = Ext.Date.clone(this.viewStart);

		var start = {
			event: evt,
			isSpan: true,
			isSpanStart: true,
			spanLeft: false,
			spanRight: (d === 6)
		};
		grid[w][d] = grid[w][d] || [];
		grid[w][d][row] = start;

		//TODO...................................................
		//this.setMaxEventsForDay(w, d);

		while (--days) {
			dt = Sonicle.Date.add(dt, {days: 1});
			if (dt > this.viewEnd) {
				break;
			}
			if (++d1 > 6) {
				// reset counters to the next week
				d1 = 0;
				w1++;
				row = this.findEmptyRowIndex(w1, 0);
			}
			grid[w1] = grid[w1] || [];
			grid[w1][d1] = grid[w1][d1] || [];

			grid[w1][d1][row] = {
				event: evt,
				isSpan: true,
				isSpanStart: (d1 === 0),
				spanLeft: (w1 > w) && (d1 % 7 === 0),
				spanRight: (d1 === 6) && (days > 1)
			};

			// In this loop we are pre-processing empty span placeholders. In the case
			// where a given week might only contain such spans, we have to make this
			// max event check on each iteration to make sure that our empty placeholder
			// divs get created correctly even without "real" events:
			//this.setMaxEventsForDay(w1, d1);
			//TODO...................................................
		}
	},
	
	// private
	findEmptyRowIndex: function (w, d, allday) {
		var grid = allday ? this.allDayGrid : this.eventGrid,
				day = grid[w] ? grid[w][d] || [] : [],
				i = 0,
				ln = day.length;

		for (; i < ln; i++) {
			if (!day[i])
				return i;
		}
		return ln;
	},
	
	// private
	renderTemplate: function () {
		if (this.tpl) {
			this.el.select('*').destroy();
			this.tpl.overwrite(this.el, this.getParams());
			this.lastRenderStart = Ext.Date.clone(this.viewStart);
			this.lastRenderEnd = Ext.Date.clone(this.viewEnd);
		}
	},
	
	disableStoreEvents: function () {
		this.monitorStoreEvents = false;
	},
	
	enableStoreEvents: function (refresh) {
		this.monitorStoreEvents = true;
		if (refresh === true) {
			this.refresh();
		}
	},
	
	// private
	onResize: function () {
		this.callParent(arguments);
		this.refresh();
	},
	
	// private
	onInitDrag: function () {
		this.fireEvent('initdrag', this);
	},
	
	// private
	onEventDrop: function (rec, dt, mode) {
		this[(mode || 'move') + 'Event'](rec, dt);
	},
	
	// private
	onCalendarEndDrag: function (start, end, onComplete) {
		if (start && end) {
			// set this flag for other event handlers that might conflict while we're waiting
			this.dragPending = true;

			// have to wait for the user to save or cancel before finalizing the dd interation
			var o = {};
			o[Sonicle.calendar.data.EventMappings.StartDate.name] = start;
			o[Sonicle.calendar.data.EventMappings.EndDate.name] = end;

			this.fireEvent('rangeselect', this, o, Ext.bind(this.onCalendarEndDragComplete, this, [onComplete]));
		}
	},
	
	// private
	onCalendarEndDragComplete: function (onComplete) {
		// callback for the drop zone to clean up
		onComplete();
		// clear flag for other events to resume normally
		this.dragPending = false;
	},
	
	// private
	onUpdate: function (ds, rec, operation) {
		if (this.monitorStoreEvents === false) {
			return;
		}
		if (operation === Ext.data.Record.COMMIT) {
			this.refresh();
			if (this.enableFx && this.enableUpdateFx) {
				this.doUpdateFx(this.getEventEls(rec.data[Sonicle.calendar.data.EventMappings.EventId.name]), {
					scope: this
				});
			}
		}
	},
	
	doUpdateFx: function (els, o) {
		this.highlightEvent(els, null, o);
	},
	
	// private
	onAdd: function (ds, records, index) {
		if (this.monitorStoreEvents === false) {
			return;
		}
		var rec = records[0];
		this.tempEventId = rec.id;
		this.refresh();

		if (this.enableFx && this.enableAddFx) {
			this.doAddFx(this.getEventEls(rec.data[Sonicle.calendar.data.EventMappings.EventId.name]), {
				scope: this
			});
		}
	},
	
	doAddFx: function (els, o) {
		els.fadeIn(Ext.apply(o, {
			duration: 2000
		}));
	},
	
	// private
	onRemove: function (ds, recs) {
		var name = Sonicle.calendar.data.EventMappings.EventId.name,
				i, len, rec, els;

		if (this.monitorStoreEvents === false) {
			return;
		}

		for (i = 0, len = recs.length; i < len; i++) {
			rec = recs[i];

			if (this.enableFx && this.enableRemoveFx) {
				els = this.getEventEls(rec.get(name));

				if (els.getCount() > 0) {
					this.doRemoveFx(els, {
						remove: true,
						scope: this,
						callback: this.refresh
					});
				}
			}
			else {
				this.getEventEls(rec.get(name)).remove();
				this.refresh();
			}
		}
	},
	
	doRemoveFx: function (els, o) {
		els.fadeOut(o);
	},
	
	/**
	 * Visually highlights an event using {@link Ext.Fx#highlight} config options.
	 * If {@link #highlightEventActions} is false this method will have no effect.
	 * @param {Ext.CompositeElement} els The element(s) to highlight
	 * @param {Object} color (optional) The highlight color. Should be a 6 char hex 
	 * color without the leading # (defaults to yellow: 'ffff9c')
	 * @param {Object} o (optional) Object literal with any of the {@link Ext.Fx} config 
	 * options. See {@link Ext.Fx#highlight} for usage examples.
	 */
	highlightEvent: function (els, color, o) {
		if (this.enableFx) {
			var c;
			!(Ext.isIE || Ext.isOpera) ?
					els.highlight(color, o) :
					// Fun IE/Opera handling:
					els.each(function (el) {
						el.highlight(color, Ext.applyIf({
							attr: 'color'
						},
						o));
						c = el.down('.ext-cal-evm');
						if (c) {
							c.highlight(color, o);
						}
					},
							this);
		}
	},
	
	/**
	 * Retrieve an Event object's id from its corresponding node in the DOM.
	 * @param {String/Element/HTMLElement} el An {@link Ext.core.Element}, DOM node or id
	 */
	getEventIdFromEl: function (el) {
		el = Ext.get(el);
		var id = el.id.split(this.eventElIdDelimiter)[1],
				lastHypen = id.lastIndexOf('-');

		// MUST look for last hyphen because autogenned record IDs can contain hyphens
		if (lastHypen > -1) {
			//This id has the index of the week it is rendered in as the suffix.
			//This allows events that span across weeks to still have reproducibly-unique DOM ids.
			id = id.substr(0, lastHypen);
		}
		return id;
	},
	
	// private
	getEventId: function (eventId) {
		if (eventId === undefined && this.tempEventId) {
			eventId = this.tempEventId;
		}
		return eventId;
	},
	
	/**
	 * 
	 * @param {String} eventId
	 * @param {Boolean} forSelect
	 * @return {String} The selector class
	 */
	getEventSelectorCls: function (eventId, forSelect) {
		var prefix = forSelect ? '.' : '';
		return prefix + this.id + this.eventElIdDelimiter + this.getEventId(eventId);
	},
	
	/**
	 * 
	 * @param {String} eventId
	 * @return {Ext.CompositeElement} The matching CompositeElement of nodes
	 * that comprise the rendered event.  Any event that spans across a view 
	 * boundary will contain more than one internal Element.
	 */
	getEventEls: function (eventId) {
		var els = Ext.select(this.getEventSelectorCls(this.getEventId(eventId), true), false, this.el.dom);
		return new Ext.CompositeElement(els);
	},
	
	/**
	 * Returns true if the view is currently displaying today's date, else false.
	 * @return {Boolean} True or false
	 */
	isToday: function () {
		var today = Ext.Date.clearTime(new Date()).getTime();
		return this.viewStart.getTime() <= today && this.viewEnd.getTime() >= today;
	},
	
	// private
	onDataChanged: function (store) {
		this.refresh();
	},
	
	// private
	isEventVisible: function (evt) {
		var M = Sonicle.calendar.data.EventMappings,
				data = evt.data || evt,
				start = this.viewStart.getTime(),
				end = this.viewEnd.getTime(),
				evStart = data[M.StartDate.name].getTime(),
				evEnd = data[M.EndDate.name].getTime();
		evEnd = Sonicle.Date.add(data[M.EndDate.name], {seconds: -1}).getTime();

		return this.rangesOverlap(start, end, evStart, evEnd);
	},
	
	rangesOverlap: function (start1, end1, start2, end2) {
		var startsInRange = (start1 >= start2 && start1 <= end2),
				endsInRange = (end1 >= start2 && end1 <= end2),
				spansRange = (start1 <= start2 && end1 >= end2);

		return (startsInRange || endsInRange || spansRange);
	},
	
	// private
	isOverlapping: function (evt1, evt2) {
		var ev1 = evt1.data ? evt1.data : evt1,
				ev2 = evt2.data ? evt2.data : evt2,
				M = Sonicle.calendar.data.EventMappings,
				start1 = ev1[M.StartDate.name].getTime(),
				end1 = Sonicle.Date.add(ev1[M.EndDate.name], {seconds: -1}).getTime(),
				start2 = ev2[M.StartDate.name].getTime(),
				end2 = Sonicle.Date.add(ev2[M.EndDate.name], {seconds: -1}).getTime();

		if (end1 < start1) {
			end1 = start1;
		}
		if (end2 < start2) {
			end2 = start2;
		}

		return (start1 <= end2 && end1 >= start2);
	},
	
	getDayEl: function (dt) {
		return Ext.get(this.getDayId(dt));
	},
	
	getDayId: function (dt) {
		if (Ext.isDate(dt)) {
			dt = Ext.Date.format(dt, 'Ymd');
		}
		return this.id + this.dayElIdDelimiter + dt;
	},
	
	/**
	 * Returns the start date of the view, as set by {@link #setStartDate}. Note that this may not 
	 * be the first date displayed in the rendered calendar -- to get the start and end dates displayed
	 * to the user use {@link #getViewBounds}.
	 * @return {Date} The start date
	 */
	getStartDate: function () {
		return this.startDate;
	},
	
	/**
	 * Sets the start date used to calculate the view boundaries to display. The displayed view will be the 
	 * earliest and latest dates that match the view requirements and contain the date passed to this function.
	 * @param {Date} dt The date used to calculate the new view boundaries
	 */
	setStartDate: function (start, refresh) {
		this.startDate = Ext.Date.clearTime(start);
		this.setViewBounds(start);
		this.store.load({
			params: {
				start: Ext.Date.format(this.viewStart, 'm-d-Y'),
				end: Ext.Date.format(this.viewEnd, 'm-d-Y')
			}
		});
		if (refresh === true) {
			this.refresh();
		}
		this.fireEvent('datechange', this, this.startDate, this.viewStart, this.viewEnd);
	},
	
	// private
	setViewBounds: function (startDate) {
		var me = this,
				start = startDate || me.startDate,
				offset = start.getDay() - me.startDay,
				soDate = Sonicle.Date;

		if (offset < 0) {
			// if the offset is negative then some days will be in the previous week so add a week to the offset
			offset += 7;
		}

		switch (this.weekCount) {
			case 0:
			case 1:
				me.viewStart = ((me.dayCount < 7) && !me.startDayIsStatic) ? start
						: soDate.add(start, {days: -offset, clearTime: true});
				me.viewEnd = soDate.add(me.viewStart, {days: me.dayCount || 7, seconds: -1});
				return;

			case -1:
				// auto by month
				start = Ext.Date.getFirstDateOfMonth(start);
				offset = start.getDay() - me.startDay;
				if (offset < 0) {
					// if the offset is negative then some days will be in the previous week so add a week to the offset
					offset += 7;
				}
				me.viewStart = soDate.add(start, {days: -offset, clearTime: true});

				// start from current month start, not view start:
				var end = soDate.add(start, {months: 1, seconds: -1});

				// fill out to the end of the week:
				offset = me.startDay;

				if (offset > end.getDay()) {
					// if the offset is larger than the end day index then the last row will be empty so skip it
					offset -= 7;
				}
				;

				me.viewEnd = soDate.add(end, {days: 6 - end.getDay() + offset});
				return;

			default:
				me.viewStart = soDate.add(start, {days: -offset, clearTime: true});
				me.viewEnd = soDate.add(me.viewStart, {days: me.weekCount * 7, seconds: -1});
		}
	},
	
	// private
	getViewBounds: function () {
		return {
			start: this.viewStart,
			end: this.viewEnd
		};
	},
	
	/* private
	 * Sort events for a single day for display in the calendar.  This sorts allday
	 * events first, then non-allday events are sorted either based on event start
	 * priority or span priority based on the value of {@link #spansHavePriority} 
	 * (defaults to event start priority).
	 * @param {MixedCollection} evts A {@link Ext.util.MixedCollection MixedCollection}  
	 * of {@link #Sonicle.calendar.EventRecord EventRecord} objects
	 */
	sortEventRecordsForDay: function (evts) {
		if (evts.length < 2) {
			return;
		}
		evts.sortBy(Ext.bind(function (evtA, evtB) {
			var a = evtA.data,
					b = evtB.data,
					M = Sonicle.calendar.data.EventMappings;

			// Always sort all day events before anything else
			if (a[M.IsAllDay.name]) {
				return -1;
			}
			else if (b[M.IsAllDay.name]) {
				return 1;
			}
			if (this.spansHavePriority) {
				// This logic always weights span events higher than non-span events
				// (at the possible expense of start time order). This seems to
				// be the approach used by Google calendar and can lead to a more
				// visually appealing layout in complex cases, but event order is
				// not guaranteed to be consistent.
				var diff = Sonicle.Date.diffDays;
				if (diff(a[M.StartDate.name], a[M.EndDate.name]) > 0) {
					if (diff(b[M.StartDate.name], b[M.EndDate.name]) > 0) {
						// Both events are multi-day
						if (a[M.StartDate.name].getTime() === b[M.StartDate.name].getTime()) {
							// If both events start at the same time, sort the one
							// that ends later (potentially longer span bar) first
							return b[M.EndDate.name].getTime() - a[M.EndDate.name].getTime();
						}
						return a[M.StartDate.name].getTime() - b[M.StartDate.name].getTime();
					}
					return -1;
				}
				else if (diff(b[M.StartDate.name], b[M.EndDate.name]) > 0) {
					return 1;
				}
				return a[M.StartDate.name].getTime() - b[M.StartDate.name].getTime();
			}
			else {
				// Doing this allows span and non-span events to intermingle but
				// remain sorted sequentially by start time. This seems more proper
				// but can make for a less visually-compact layout when there are
				// many such events mixed together closely on the calendar.
				return a[M.StartDate.name].getTime() - b[M.StartDate.name].getTime();
			}
		}, this));
	},
	
	/**
	 * Updates the view to contain the passed date
	 * @param {Date} dt The date to display
	 * @return {Date} The new view start date
	 */
	moveTo: function (dt, noRefresh) {
		if (Ext.isDate(dt)) {
			this.setStartDate(dt);
			if (noRefresh !== false)
				this.refresh();
			return this.startDate;
		}
		return dt;
	},
	
	/**
	 * Updates the view to the next consecutive date(s)
	 * @return {Date} The new view start date
	 */
	moveNext: function (noRefresh) {
		return this.moveTo(Sonicle.Date.add(this.viewEnd, {days: 1}));
	},
	
	/**
	 * Updates the view to the previous consecutive date(s)
	 * @return {Date} The new view start date
	 */
	movePrev: function (noRefresh) {
		var days = Sonicle.Date.diffDays(this.viewStart, this.viewEnd) + 1;
		return this.moveDays(-days, noRefresh);
	},
	
	/**
	 * Shifts the view by the passed number of months relative to the currently set date
	 * @param {Number} value The number of months (positive or negative) by which to shift the view
	 * @return {Date} The new view start date
	 */
	moveMonths: function (value, noRefresh) {
		return this.moveTo(Sonicle.Date.add(this.startDate, {months: value}), noRefresh);
	},
	
	/**
	 * Shifts the view by the passed number of weeks relative to the currently set date
	 * @param {Number} value The number of weeks (positive or negative) by which to shift the view
	 * @return {Date} The new view start date
	 */
	moveWeeks: function (value, noRefresh) {
		return this.moveTo(Sonicle.Date.add(this.startDate, {days: value * 7}), noRefresh);
	},
	
	/**
	 * Shifts the view by the passed number of days relative to the currently set date
	 * @param {Number} value The number of days (positive or negative) by which to shift the view
	 * @return {Date} The new view start date
	 */
	moveDays: function (value, noRefresh) {
		return this.moveTo(Sonicle.Date.add(this.startDate, {days: value}), noRefresh);
	},
	
	/**
	 * Updates the view to show today
	 * @return {Date} Today's date
	 */
	moveToday: function (noRefresh) {
		return this.moveTo(new Date(), noRefresh);
	},
	
	/**
	 * Sets the event store used by the calendar to display {@link Sonicle.calendar.EventRecord events}.
	 * @param {Ext.data.Store} store
	 */
	setStore: function (store, initial) {
		if (!initial && this.store) {
			this.store.un("datachanged", this.onDataChanged, this);
			this.store.un("add", this.onAdd, this);
			this.store.un("remove", this.onRemove, this);
			this.store.un("update", this.onUpdate, this);
			this.store.un("clear", this.refresh, this);
		}
		if (store) {
			store.on("datachanged", this.onDataChanged, this);
			store.on("add", this.onAdd, this);
			store.on("remove", this.onRemove, this);
			store.on("update", this.onUpdate, this);
			store.on("clear", this.refresh, this);
		}
		this.store = store;
		if (store && store.getCount() > 0) {
			this.refresh();
		}
	},
	
	getEventRecord: function (id) {
		var idx = this.store.find(Sonicle.calendar.data.EventMappings.EventId.name, id);
		return this.store.getAt(idx);
	},
	
	getEventRecordFromEl: function (el) {
		return this.getEventRecord(this.getEventIdFromEl(el));
	},
	
	// private
	getParams: function () {
		return {
			viewStart: this.viewStart,
			viewEnd: this.viewEnd,
			startDate: this.startDate,
			dayCount: this.dayCount,
			weekCount: this.weekCount,
			title: this.getTitle()
		};
	},
	
	getTitle: function () {
		return Ext.Date.format(this.startDate, 'F Y');
	},
	
	/*
	 * Shared click handling.  Each specific view also provides view-specific
	 * click handling that calls this first.  This method returns true if it
	 * can handle the click (and so the subclass should ignore it) else false.
	 */
	onClick: function (e, t) {
		var el = e.getTarget(this.eventSelector, 5);
		if (el) {
			var id = this.getEventIdFromEl(el);
			this.fireEvent('eventclick', this, this.getEventRecord(id), el);
			return true;
		}
	},
	
	// private
	onMouseOver: function (e, t) {
		if (this.trackMouseOver !== false && (this.dragZone == undefined || !this.dragZone.dragging)) {
			if (!this.handleEventMouseEvent(e, t, 'over')) {
				this.handleDayMouseEvent(e, t, 'over');
			}
		}
	},
	// private
	onMouseOut: function (e, t) {
		if (this.trackMouseOver !== false && (this.dragZone == undefined || !this.dragZone.dragging)) {
			if (!this.handleEventMouseEvent(e, t, 'out')) {
				this.handleDayMouseEvent(e, t, 'out');
			}
		}
	},
	
	// private
	handleEventMouseEvent: function (e, t, type) {
		var el = e.getTarget(this.eventSelector, 5, true),
				rel,
				els,
				evtId;
		if (el) {
			rel = Ext.get(e.getRelatedTarget());
			if (el === rel || el.contains(rel)) {
				return true;
			}

			evtId = this.getEventIdFromEl(el);

			if (this.eventOverClass) {
				els = this.getEventEls(evtId);
				els[type === 'over' ? 'addCls' : 'removeCls'](this.eventOverClass);
			}
			this.fireEvent('event' + type, this, this.getEventRecord(evtId), el);
			return true;
		}
		return false;
	},
	
	// private
	getDateFromId: function (id, delim) {
		var parts = id.split(delim);
		return parts[parts.length - 1];
	},
	
	// private
	handleDayMouseEvent: function (e, t, type) {
		t = e.getTarget('td', 3);
		if (t) {
			if (t.id && t.id.indexOf(this.dayElIdDelimiter) > -1) {
				var dt = this.getDateFromId(t.id, this.dayElIdDelimiter),
						rel = Ext.get(e.getRelatedTarget()),
						relTD,
						relDate;

				if (rel) {
					relTD = rel.is('td') ? rel : rel.up('td', 3);
					relDate = relTD && relTD.id ? this.getDateFromId(relTD.id, this.dayElIdDelimiter) : '';
				}
				if (!rel || (dt !== relDate)) {
					var el = this.getDayEl(dt);
					if (el && this.dayOverClass !== '') {
						el[(type === 'over') ? 'addCls' : 'removeCls'](this.dayOverClass);
					}
					this.fireEvent('day' + type, this, Ext.Date.parseDate(dt, "Ymd"), el);
				}
			}
		}
	},
	
	// private
	renderItems: function () {
		throw 'This method must be implemented by a subclass';
	},
	
	// private
	destroy: function () {
		this.callParent(arguments);

		if (this.el) {
			this.el.un('contextmenu', this.onContextMenu, this);
		}
		Ext.destroy(
			this.editWin,
			this.eventMenu,
			this.dragZone,
			this.dropZone
		);
	},
	
	/**
	 * Create a copy of the event with a new start date, preserving the original event duration.
	 * @param {Object} rec The original event {@link Extensible.calendar.data.EventModel record}
	 * @param {Object} newStartDate The new start date. The end date of the created event copy will be adjusted
	 * automatically to preserve the original duration.
	 */
	copyEvent: function (rec, newStartDate) {
		this.shiftEvent(rec, newStartDate, 'copy');
	},
	
	/**
	 * Move the event to a new start date, preserving the original event duration.
	 * @param {Object} rec The event {@link Extensible.calendar.data.EventModel record}
	 * @param {Object} newStartDate The new start date
	 */
	moveEvent: function (rec, newStartDate) {
		this.shiftEvent(rec, newStartDate, 'move');
	},
	
	// private
	shiftEvent: function (rec, newStartDate, moveOrCopy) {
		var me = this,
				newRec;

		if (moveOrCopy === 'move') {
			if (Sonicle.Date.compare(rec.data[Sonicle.calendar.data.EventMappings.StartDate.name], newStartDate) === 0) {
				// No changes, so we aren't actually moving. Copying to the same date is OK.
				return;
			}
			newRec = rec;
		} else {
			newRec = rec.clone();
		}

		if (me.fireEvent('beforeevent' + moveOrCopy, me, newRec, Ext.Date.clone(newStartDate)) !== false) {
			if (newRec.get('isRecurring')) {
				me.onRecurrenceEditModeSelected('single', newRec, newStartDate, moveOrCopy);
			} else {
				me.doShiftEvent(newRec, newStartDate, moveOrCopy);
			}
		}
	},
	
	// private
	onRecurrenceEditModeSelected: function (editMode, rec, newStartDate, moveOrCopy) {
		var EventMappings = Sonicle.calendar.data.EventMappings;

		if (editMode) {
			if (moveOrCopy === 'copy') {
				rec.clearRecurrence();
			}
			rec.data[EventMappings.REditMode.name] = editMode;
			rec.data[EventMappings.RInstanceStartDate.name] = rec.getStartDate();
			this.doShiftEvent(rec, newStartDate, moveOrCopy);
		}
		// else user canceled
	},
	
	// private
	doShiftEvent: function (rec, newStartDate, moveOrCopy) {
		var EventMappings = Sonicle.calendar.data.EventMappings,
				start = rec.data[EventMappings.StartDate.name],
				end = rec.data[EventMappings.EndDate.name],
				diff = newStartDate.getTime() - start.getTime();

		rec.set(EventMappings.StartDate.name, newStartDate);
		rec.set(EventMappings.EndDate.name, Sonicle.Date.add(end, {millis: diff}));
		if (rec.phantom)
			this.store.add(rec);
		rec.commit();

		this.fireEvent('event' + moveOrCopy, this, rec);
	}
});
