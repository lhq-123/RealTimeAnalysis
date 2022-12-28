<template>
	<div id="goodsSellNumChart" :style="{ width: '100%', height: '300px', border: '0px #0298F9 solid' }"></div>
</template>

<script>
	import {ajaxGet} from '../api/api.js'
  import echarts from "echarts";
	export default {
		name: 'hello',
		data() {
			return {
				goodsSellNumChart : null,
				chartData : {xAxisData:[], lineData:[], barData: []},
				timer : null
			};
		},
		created: function() { //在组件创建完毕后加载
			let _this = this;
			this.timer = setInterval(function () {
				_this.getBrandTotalNum();
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
				this.goodsSellNumChart = this.$echarts.init(document.getElementById('goodsSellNumChart'));
				this.goodsSellNumChart.showLoading();
				this.getBrandTotalNum();
			},
			getBrandTotalNum(){
				let _this = this;
				ajaxGet('/api/count/brand/sell', function(result){
					if (result.code === '1'){
						let data = result.data;
						let xAxisData = [];
						let lineData = [];
						let barData = [];
						data.forEach((row, index)=>{
							xAxisData.push(row[0]);
							lineData.push(row[1]);
							barData.push(row[2]);
						})
						_this.chartData = {xAxisData:xAxisData, lineData:lineData, barData: barData};
						_this.drawLine();
					}
				});
			},
			drawLine() {
				this.goodsSellNumChart.hideLoading();
				let option = {
					title: {
						text: '品牌营销能力',
            textStyle: {
              color: '#00a4eb',
              fontWeight:'normal'
            }
					},
					tooltip: {
						trigger: 'axis',
						axisPointer: {
							type: 'shadow'
						}
					},
					legend: {
            textStyle: {
              color:'#8997b0'
            },
						data: ['累计销量(件)', '累计额(万)']
					},
					xAxis: {
            axisLabel: {
              color: '#8997b0'
            },
            axisTick: {
              show: false
            },
						data: this.chartData.xAxisData
					},
					yAxis: {
						splitLine: {show: false},
            axisLabel: {
              color: '#8997b0'
            },
            axisTick: {

            },
					},
					grid: {
						top: '12%',
						left: '0%',
						right: '0%',
						bottom: '9%',
						containLabel: true
					},
					series: [
						{
							name: '累计销量(件)',
							type: 'line',
							smooth: true,
							showAllSymbol: true,
							symbol: 'emptyCircle',
							symbolSize: 8,
              itemStyle: {
                color:'#ff7811'
              },
              lineStyle: {
                color: '#00d000'
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  {
                    offset: 0,
                    color: '#ff1892'
                  },
                  {
                    offset: 1,
                    color: '#36252d'
                  }
                ])
              },
							data: this.chartData.lineData
						}, 
						{
							name: '累计额(万)',
							type: 'bar',
							barWidth: 10,
							itemStyle: {
								barBorderRadius: 5,
								color: '#ff7811'
							},
							symbol: 'rect',
							symbolRepeat: true,
							symbolSize: [12, 4],
							symbolMargin: 1,
							z: -10,
							data: this.chartData.barData
						}
					]
				};
				// 绘制图表
				this.goodsSellNumChart.setOption(option);
			}
		}
	};
	
	
</script>

<style>
</style>
