import { isundef, isvalid, makeid, dateToString } from '../grid/common.js';


const LogType = {
	DEBUG: 0,
	INFO: 1,
	WARN: 2,
	ERROR: 3,
	FATAL: 4,
	NOTICE: 5,
};


const Log = {
	_limit_: 1024,
	_list_: [],
	_listener_: {},

	/**
	 * 로그 이벤트를 받았을 때 호출할 이벤트 핸들러 등록.
	 * @param {function} func 이벤트 처리용 핸들러. function(item). item은 {time, text, type}. type은 LogType참고
	 * @returns receiver id
	 */
	addReceiver: (func) => {
		const rid = makeid(16);
		Log._listener_[rid] = func;
		return rid;
	},

	/**
	 * 로그 이벤트 핸들러 제거
	 * @param {string} rid addReceiver() 호출 시 받은 ID
	 * @returns 
	 */
	removeReceiver: (rid) => {
		if( isundef(Log._listener_[rid]) ) {
			return;
		}
		delete Log._listener_[rid];
	},

	_broadcast: (item) => {
		for(let rid in Log._listener_) {
			Log._listener_[rid](item); // TODO 이벤트 정보를 넘겨야 하나?
		}
	},
	
	_add: (item) => {
		if( isundef(item) ) {
			return;
		}

		if( Log._list_.length >= 1024 ) {
			Log._list_ = Log._list_.slice(1);
		}

		item.time = dateToString(new Date());
		// console.log('Logging', item);

		Log._list_.push(item);
		Log._broadcast(item);
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

	n: (msg) => {
		Log._add({ type: LogType.NOTICE, text: msg });
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
