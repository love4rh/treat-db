import { isundef, isvalid } from '../util/tool.js';
import { dateToString } from '../grid/common.js';


const LogType = {
	DEBUG: 0,
	INFO: 1,
	WARN: 2,
	ERROR: 3,
	FATAL: 4
};


const Log = {
	_limit_: 1024,
	_list_: [],
	_listner_: {},
	
	_add: (item) => {
		if( isundef(item) ) {
			return;
		}

		if( Log._list_.length >= 1024 ) {
			Log._list_ = Log._list_.slice(1);
		}

		item.time = dateToString(new Date());
		Log._list_.push(item);

		console.log('Log:', item);
	},

	i: (msg) => {
		Log._add({ type: LogType.INFO, text: msg });
	},

	d: (msg) => {
		Log._add({ type: LogType.DEBUG, text: msg });
	},

	w: (msg) => {
		Log._add({ type: LogType.WARN, text: msg });
	},

	e: (msg) => {
		Log._add({ type: LogType.ERROR, text: msg });
	},

	f: (msg) => {
		Log._add({ type: LogType.FATAL, text: msg });
	},

	clear: () => {
		Log._list_ = [];
	},

	size: () => {
		return Log._list_.length;
	},

	/**
	 * 
	 * @param {Number} idx 반환 받을 로그위 인덱스. idx가 유효하지 않으면 전체 로그 리스트를 반환함.
	 */
	get: (idx) => {
		return (isvalid(idx) && 0 <= idx && idx < Log._list_.length) ? Log._list_[idx] : Log._list_;
	}
};

export default Log;
export { Log, LogType };
