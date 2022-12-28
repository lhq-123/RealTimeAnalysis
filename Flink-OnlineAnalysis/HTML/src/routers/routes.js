import Main from '../views/Main.vue'


let routes = [
	{
	    path: '/',
		redirect: to => { //重定向
			return '/main'
		}
	}, 
	{
	    path: '/main',
	    component: Main,
	    name: '主页',
	    hidden: true,
	}
];

export default routes;