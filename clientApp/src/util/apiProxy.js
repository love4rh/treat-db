import axios from 'axios';

import { makeid } from '../util/tool.js';

export const _serverBaseUrl_ = 'http://10.186.115.136:8888';
// export const _serverBaseUrl_ = 'http://127.0.0.1:8888';

const _userToken = makeid(8);


const getApi = axios.create({
  baseURL: _serverBaseUrl_,
  timeout: 30000,
  headers: {
  	'Content-Type': 'application/json;charset=utf-8'
  }
});



const apiProxy = {
	_handleWait: null,
	_handleDone: null,

	setWaitHandle: (waiting, done) => {
		apiProxy._handleWait = waiting;
		apiProxy._handleDone = done;
	},

	enterWaiting: () => {
		if( apiProxy._handleWait ) {
			apiProxy._handleWait()
		}
	},

	leaveWaiting: () => {
		if( apiProxy._handleDone ) {
			apiProxy._handleDone()
		}
	},

	test: (testString) => {
		getApi.get('/test?testString=' + testString);
	},

	go: (logUrl, cbSuccess, cbError) => {
		getApi.get('/inputLogPath?path=' + logUrl + '&userToken=' + _userToken) // encodeURIComponent
		.then(res => {
			if (cbSuccess) cbSuccess(res);
		})
		.catch(res => {
			if (cbError) cbSuccess(res);
		});
	},

	getMoreData: (dataKey, s, len, cbSuccess, cbError) => {
		getApi.get('/logData?dataKey=' + dataKey + '&start=' + s + '&len=' + len + '&userToken=' + _userToken)
		.then(res => {
			if (cbSuccess) cbSuccess(res);
		})
		.catch(res => {
			if (cbError) cbSuccess(res);
		});
	},

	filterData: (dataKey, category, selected, cbSuccess, cbError) => {
		apiProxy.enterWaiting();
		axios({
			baseURL: _serverBaseUrl_,
			url: '/filterData',
			method: 'post',
			timeout: 24000,
			headers: {
		  	'x-auth-code': _userToken,
		  	'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8'
		  },
			data: 'dataKey=' + dataKey + '&category=' + category + '&selected=' + selected + '&userToken=' + _userToken
		})
		.then(res => {
			apiProxy.leaveWaiting();
			if (cbSuccess) cbSuccess(res);
		})
		.catch(res => {
			apiProxy.leaveWaiting();
			if (cbError) cbSuccess(res);
		});
	},

	downloadLogFile: (dataKey) => {
		getApi.get('/download?dataKey=' + dataKey);
	}
};

export default apiProxy;
export {apiProxy};
