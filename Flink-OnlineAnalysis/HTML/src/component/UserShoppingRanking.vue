<template>
	<div id="userShoppingRankingChart" :style="{ width: '100%', height: '300px', border: '0px #0298F9 solid' }"></div>
</template>

<script>
	import {ajaxGet} from '../api/api.js'
	export default {
		name: 'hello',
		data() {
			return {
				userShoppingRankingChart : null,
				chartData : {},
				timer : null
			};
		},
		created: function() { //在组件创建完毕后加载
			let _this = this;
			this.timer = setInterval(function () {
				_this.getUserRanking();
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
				this.userShoppingRankingChart = this.$echarts.init(document.getElementById('userShoppingRankingChart'));
				this.userShoppingRankingChart.showLoading();
				this.getUserRanking();
			},
			getUserRanking(){
				let _this = this;
				ajaxGet('/api/count/user/ranking', function(result){
					if (result.code === '1'){
						let data = result.data;
						let xAxisData = [];
						let seriesData = [];
						data.forEach((row, index)=>{
							xAxisData.push(row.name);
							seriesData.push(row.value);
						})
						_this.chartData = {xAxisData:xAxisData, seriesData:seriesData};
						_this.drawLine();
					}
				});
			},
			drawLine() {
				this.userShoppingRankingChart.hideLoading();
				let option = {
					color: [ '#74add1', '#d73027', '#f46d43', '#4575b4'],
					title: {
						text: '消费前十排名',
            textStyle: {
              color: '#00a4eb',
              fontWeight:'normal'
            }
					},
					tooltip: {
						trigger: 'axis',
						axisPointer: {
							type: 'shadow'
						},
						formatter: '姓名：{b}<br/> 消费: $ {c} (元)'
					},
					grid: {
						top: '12%',
						left: '3%',
						right: '4%',
						bottom: '9%',
						containLabel: true
					},
					xAxis: {
						axisLabel: {
							inside: true,
							textStyle: {
								color: '#fff'
							}
						},
						splitLine: { show: false },
						axisTick: { show: false },
						axisLine: { show: false },
					},
					yAxis: {
						axisLine: { show: false },
						axisTick: { show: false },
						inverse: true,
						type: 'category',
            axisLabel: {show: true,color:'#8997b0'},
						data:this.chartData.xAxisData
					},
					series: [
						{
						    name: '2011年',
						    type: 'bar',
							label: {
								show: true,
								position: 'inside'
							},
              itemStyle: {
                  color:'#ce9900'
              },
							data:this.chartData.seriesData
						}
					]
				};
				// 绘制图表
				this.userShoppingRankingChart.setOption(option);
			}
		}
	};
</script>

<style>
</style>
