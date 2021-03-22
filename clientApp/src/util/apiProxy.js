import axios from 'axios';

import { makeid, isvalid } from '../util/tool.js';

export const _serverBaseUrl_ = 'http://10.186.115.136:8080';
// export const _serverBaseUrl_ = 'http://127.0.0.1:8888';

const _userToken = makeid(8);

const basicHeader = {
	'Content-Type': 'application/json;charset=utf-8',
	'x-user-token': _userToken
};


const GET = axios.create({
  baseURL: _serverBaseUrl_,
  timeout: 12000,
  headers: basicHeader
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
		GET.get('/test?testString=' + testString)
			.then(res => {
				// to do something with res
			})
			.catch(err => {
				// to do something with err
			});
	},

	getMetaData: (authCode, cbSuccess, cbError) => {
		apiProxy.enterWaiting();
		
		axios({
			baseURL: _serverBaseUrl_,
			url: '/metadata',
			method: 'post',
			timeout: 24000,
			headers: basicHeader,
			data: { authCode }
		})
		.then(res => {
			apiProxy.leaveWaiting();
			if( isvalid(res.data) && res.data.returnCode === 0 ) {
				if( cbSuccess ) cbSuccess(res);
			} else if( cbError )
			cbError(res);
		})
		.catch(res => {
			apiProxy.leaveWaiting();
			if( cbError ) cbSuccess(res);
		});
	}
};

export default apiProxy;
export {apiProxy};
