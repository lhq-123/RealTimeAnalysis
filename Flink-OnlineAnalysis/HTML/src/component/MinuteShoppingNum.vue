<template>
	<div id="minuteShoppingNumChart" :style="{ width: '100%', height: '300px', border: '0px #0298F9 solid' }"></div>
</template>

<script>
	import {ajaxGet} from '../api/api.js'
  import echarts from "echarts";
	
	export default {
		name: 'hello',
		data() {
			return {
				minuteShoppingNumChart : null,
				chartData : [],
				timer: null
			};
		},
		created: function() { //在组件创建完毕后加载
			let _this = this;
			this.timer = setInterval(function () {
        console.log('1111111')
				_this.getMinuteNum();
			}, 1000*60);
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
				this.minuteShoppingNumChart = this.$echarts.init(document.getElementById('minuteShoppingNumChart'));
				this.minuteShoppingNumChart.showLoading();
				this.drawLine();
				this.getFullMinuteNum();
			},
			getFullMinuteNum(){
				let _this = this;
				ajaxGet('/api/count/minuteNum?flag=full', function(result){
					if (result.code === '1'){
						//{"name":"2020-10-09T13:02:08.261+00:00","value":["2020/10/09 21:02:08",9458]}
						//data.shift();
						if (result.data != null){
              console.log('resule.data',result.data)
							result.data.forEach((row, index)=>{
								_this.chartData.push(row);
							})
						}
						_this.minuteShoppingNumChart.setOption({
							series: [{
								data: _this.chartData
							}]
						});
					}
				});
			},
			getMinuteNum(){
				let _this = this;
				ajaxGet('/api/count/minuteNum', function(result){
					if (result.code === '1'){
						//{"name":"2020-10-09T13:02:08.261+00:00","value":["2020/10/09 21:02:08",9458]}
						//data.shift();
						_this.chartData.push(result.data);
            console.log('chartDataNone',_this.chartData)
						_this.minuteShoppingNumChart.setOption({
							series: [{
								data: _this.chartData
							}]
						});
					}
				});
			},
			drawLine() {
				this.minuteShoppingNumChart.hideLoading();
				let option = {
					color: [ '#01e0d9', '#4575b4', '#d68262',  '#d73027', '#f46d43', '#74add1'],
					title: {
						text: '每分钟销量',
            textStyle: {
              color: '#00a4eb',
              fontWeight:'normal'
            },
            left:'50%'
					},
					tooltip: {
						trigger: 'axis',
						formatter: function (params) {
							params = params[0];
							var date = new Date(params.name);
							return date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear() + ' : ' + params.value[1];
						},
						axisPointer: {
							animation: false
						}
					},
					xAxis: {
						type: 'time',
						splitLine: {
							show: false
						},
            axisLabel: {
              color: '#8997b0'
            }
					},
					yAxis: {
						type: 'value',
						boundaryGap: [0, '100%'],
						splitLine: {
							show: false
						},
            axisLabel: {show: true,color:'#8997b0'}
					},
					grid: {
						top: '12%',
						left: '0%',
						right: '0%',
						bottom: '9%',
						containLabel: true
					},
					series: [{
						name: '模拟数据',
						type: 'line',
						showSymbol: true,
						hoverAnimation: false,
            smooth:true,
						areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: '#61b419'
                },
                {
                  offset: 1,
                  color: '#36252d'
                }
              ])
            },
						data: this.chartData
					}]
				};
				// 绘制图表
				this.minuteShoppingNumChart.setOption(option);
			}
		}
	};
</script>

<style>
</style>
