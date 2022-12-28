<template>
	<div id="genderShoppingTimeChart" :style="{ width: '100%', height: '300px', border: '0px #0298F9 solid' }"></div>
</template>

<script>
	import {ajaxGet} from '../api/api.js'
	
	export default {
		name: 'hello',
		data() {
			return {
				genderShoppingTimeChart : null,
				chartData : {},
				timer : null
			};
		},
		created: function() { //在组件创建完毕后加载
			let _this = this;
			this.timer = setInterval(function () {
				_this.getGenderShoppingTime();
			}, this.GLOBAL_VAR._intervalTime * 2);
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
				this.genderShoppingTimeChart = this.$echarts.init(document.getElementById('genderShoppingTimeChart'));
				this.genderShoppingTimeChart.showLoading();
				this.getGenderShoppingTime();
			},
			getGenderShoppingTime(){
				let _this = this;
				ajaxGet('/api/count/gender/shoppingTime', function(result){
					if (result.code === '1'){
						let data = result.data;
						let xAxisData = [];
						let seriesData1 = [];
						let seriesData2 = [];
						data.forEach((row, index)=>{
							xAxisData.push(row[0]);
							seriesData1.push(row[1]);
							seriesData2.push(row[2]);
						})
						_this.chartData = {xAxisData:xAxisData, seriesData1:seriesData1, seriesData2: seriesData2};
						_this.drawLine();
					}
				});
			},
			drawLine() {
				this.genderShoppingTimeChart.hideLoading();
				let option = {
					title: {
						text: '每10分钟销量',
            textStyle: {
              color: '#00a4eb',
              fontWeight:'normal'
            }
					},
					legend: {
						data: ['男', '女'],
            textStyle: {
              color:'#8997b0'
            },
					},
					tooltip: {
						trigger: 'item',
						formatter: '性别：{a} <br/>时间：{b} <br/>购买量：{c}'
					},
					xAxis: {
						data: this.chartData.xAxisData,
						splitLine: {
							show: true
						},
            axisLabel: {
              color: '#8997b0'
            },
            axisTick: {
              show: false
            },
					},
					yAxis: {
						splitLine: {show: false},
            axisLabel: {
              color: '#8997b0'
            },
					},
					grid: {
						top: '12%',
						left: '0%',
						right: '0%',
						bottom: '10%',
						containLabel: true
					},
					series: [
						{
							name: '男',
							type: 'bar',
              itemStyle: {
                color:'#72e700'
              },
							data: this.chartData.seriesData1,
							animationDelay: function (idx) {
								return idx * 10;
							}
						}, 
						{
							name: '女',
							type: 'bar',
              itemStyle: {
                color:'#ffcc00'
              },
							data: this.chartData.seriesData2,
							animationDelay: function (idx) {
								return idx * 10 + 100;
							}
						}
					],
					animationEasing: 'elasticOut',
					animationDelayUpdate: function (idx) {
						return idx * 5;
					}
				};
				// 绘制图表
				this.genderShoppingTimeChart.setOption(option);
			}
		}
	};
	
	
</script>

<style>
</style>
