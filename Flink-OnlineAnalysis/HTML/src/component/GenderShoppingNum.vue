<template>
	<div id="genderShoppingNumChart" :style="{ width: '100%', height: '300px', border: '0px #0298F9 solid' }"></div>
</template>

<script>
	import {ajaxGet} from '../api/api.js'
	
	export default {
		name: 'hello',
		data() {
			return {
				genderShoppingNumChart : null,
				chartData : [],
				timer: null
			};
		},
		created: function() { //在组件创建完毕后加载
			let _this = this;
			this.timer = setInterval(function () {
				_this.getGenderTotalNum();
			}, this.GLOBAL_VAR._intervalTime);
		},
		mounted() {
			this.init();
		},
		beforeDestroy() {
		    clearInterval(this.timer);        
		    this.timer = null;
		},
		methods: {
			init() {
				let _this = this;
				this.genderShoppingNumChart = this.$echarts.init(document.getElementById('genderShoppingNumChart'));
				this.genderShoppingNumChart.showLoading();
				this.getGenderTotalNum();
			},
			getGenderTotalNum(){
				let _this = this;
				ajaxGet('/api/count/gender/totalNum', function(result){
					if (result.code === '1'){
            console.log('resultData',result.data)
            result.data[0].itemStyle = {color: '#ac53d2'}
            result.data[1].itemStyle = {color:'#19b46c'}
						_this.chartData = result.data;
						_this.drawLine();
					}
				});
			},
			drawLine() {
				this.genderShoppingNumChart.hideLoading();
				let option = {
					title: {
						text: '性别购买力',
            textStyle: {
              color: '#00a4eb',
              fontWeight:'normal'
            }
					},
					tooltip: {
						trigger: 'item',
						formatter: '{a} <br/>{b} : {c} ({d}%)'
					},
					legend: {
						type: 'scroll',
						orient: 'vertical',
						right: 10,
						top: 20,
						bottom: 20,
            textStyle: {
              color:'#8997b0'
            },
					},
					series: [
						{
							name: '姓名',
							type: 'pie',
							radius: '75%',
							center: ['50%', '50%'],
							label: {
								position: 'inner',
								normal: {
									formatter: '$: {c}.00',
									position: 'inside'
								}
							},
							//data: [{name:'男',value:35218580.00},{name:'女',value:15289431.00}],
							data: this.chartData,
							emphasis: {
								itemStyle: {
									shadowBlur: 10,
									shadowOffsetX: 0,
									shadowColor: 'rgba(0, 0, 0, 0.5)'
								}
							}
						}
					]
				};
				// 绘制图表
				this.genderShoppingNumChart.setOption(option);
			}
		}
	};
</script>

<style>
</style>
