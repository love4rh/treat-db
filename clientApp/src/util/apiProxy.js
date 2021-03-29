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

  signIn: (userID, password, cbSuccess, cbError) => {
    apiProxy.enterWaiting();
		
		axios({
			baseURL: _serverBaseUrl_,
			url: '/checkAuthority',
			method: 'post',
			timeout: 4000,
			headers: basicHeader,
			data: { userID, password }
		})
		.then(res => {
			apiProxy.leaveWaiting();
      const data = res.data;
      // console.log('signIn', res);

			if( isvalid(data) && data.returnCode === 0 ) {
        basicHeader['x-auth-code'] = data.response.authCode;
				if( cbSuccess ) cbSuccess(data);
			} else if( cbError ) {
			  cbError(res);
      }
		})
		.catch(res => {
			apiProxy.leaveWaiting();
			if( cbError ) cbError(res);
		});
  },

  signOut: () => {
    axios({
			baseURL: _serverBaseUrl_,
			url: '/signOut',
			method: 'post',
			timeout: 4000,
			headers: basicHeader
		})
		.then(res => {
			console.log('signed out');
		})
		.catch(res => {
			console.log('signed out');
		});
  },

	getMetaData: (cbSuccess, cbError) => {
		apiProxy.enterWaiting();
		
		axios({
			baseURL: _serverBaseUrl_,
			url: '/metadata',
			method: 'post',
			timeout: 24000,
			headers: basicHeader
		})
		.then(res => {
			apiProxy.leaveWaiting();
			if( isvalid(res.data) && res.data.returnCode === 0 ) {
				if( cbSuccess ) cbSuccess(res.data);
			} else if( cbError ) {
			  cbError(res);
      }
		})
		.catch(res => {
			apiProxy.leaveWaiting();
			if( cbError ) cbError(res);
		});
	},

	/**
	 * data: { dbIdx, query }
	 */
  executeQuery: (data, cbSuccess, cbError) => {
		apiProxy.enterWaiting();
		
		axios({
			baseURL: _serverBaseUrl_,
			url: '/executeSql',
			method: 'post',
			timeout: 60000,
			headers: basicHeader,
			data: data
		})
		.then(res => {
			apiProxy.leaveWaiting();
			if( isvalid(res.data) && res.data.returnCode === 0 ) {
				if( cbSuccess ) cbSuccess(res.data);
			} else if( cbError ) {
			  cbError(res);
      }
		})
		.catch(res => {
			apiProxy.leaveWaiting();
			if( cbError ) cbError(res);
		});
	},

	getMoreData: (data, cbSuccess, cbError) => {
		axios({
			baseURL: _serverBaseUrl_,
			url: '/moreData',
			method: 'post',
			timeout: 5000,
			headers: basicHeader,
			data: data // { qid, beginIdx, length }
		})
		.then(res => {
			if( isvalid(res.data) && res.data.returnCode === 0 ) {
				if( cbSuccess ) cbSuccess(res.data);
			} else if( cbError ) {
			  cbError(res);
      }
		})
		.catch(res => {
			if( cbError ) cbError(res);
		});
	},
};

export default apiProxy;
export {apiProxy};
