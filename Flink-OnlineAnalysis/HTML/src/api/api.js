import axios from "axios";
// 专门放置 全局函数
import {Loading,Message} from 'element-ui'

import GLOBAL_VAR from './global_variable.js'

export const ajaxGet = (url, callback) => {
	axios.get(url,callback)
		.then(function(response) {
			// console.log(response);
			if (response.status === 200){
				callback(response.data);
			}
		})
		.catch(function(error) {
			console.log(error);
			Message({
				message: GLOBAL_VAR.errMsg + ':' + error,
				showClose: true,
				type: 'error',
				duration : 3000
			});
		});
}
