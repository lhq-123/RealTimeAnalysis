<template>
	<div class="echarts-wrapper">
		
		<!-- <el-row>
			<el-col :span="14">&nbsp;</el-col>
			<el-col :span="10"><el-button @click="handleReset" round size="mini" style="margin-bottom: 10px; float: right;">点击刷新</el-button></el-col>
		</el-row> -->
		<el-collapse-transition>
			<div v-show="show3">
		    <div style="position:relative;display: flex;justify-content: center;color:#fff;margin-bottom: 8px">
          <div style="display: flex;position: absolute;bottom: 0;left: 0;">
            <div style="margin-right: 16px;font-size: 30px;background:linear-gradient(270deg, #2193b0 0%,#6dd5ed 100%);-webkit-background-clip:text;color:transparent">{{currentTime}}</div>
            <div>
              <div style="background:linear-gradient(270deg, #2193b0 0%,#6dd5ed 100%);-webkit-background-clip:text;color:transparent">{{currentYear}}</div>
              <div style="background:linear-gradient(270deg, #2193b0 0%,#6dd5ed 100%);-webkit-background-clip:text;color:transparent">{{currentWeek}}</div>
            </div>
          </div>
          <div style="font-size:30px;height: 80px;line-height:80px;background:linear-gradient(90deg, #EFD2FF 0%,#fcb69f 100%);-webkit-background-clip:text;color:transparent">拼夕夕实时大屏</div>
        </div>
		<el-row :gutter="16">
			<el-col :span="6">
				 <el-card shadow="always" style="height: 140px;">
				      <el-row :span="12">
				      	<el-col :span="6">
				      		<li class="el-icon-shopping-cart-full iconLi" style="color: #40c9c6;"></li>
				      	</el-col>
				      	<el-col :span="18" style="text-align: right;">
				      		<div>
				      			<span style="color:#00a4eb" class="textSpan">累计总额</span>
				      			<div class="bottom clearfix">
				      				<span class="countSpan">
										<countTo :startVal='startTotalPrice' :endVal='endTotalPrice' 
										:duration='4000' separator=',' prefix='' suffix=''></countTo>
									</span>
				      			</div>
				      		</div>
				      	</el-col>

				      </el-row>
           <div class="echarts-bottom"></div>
				 </el-card>
				 <el-card shadow="always" style="height: 140px; margin-top: 18px;">
				      <el-row :span="12">
				      	<el-col :span="6">
				      		<li class="el-icon-sell iconLi" style="color: #f4516c;"></li>
				      	</el-col>
				      	<el-col :span="18" style="text-align: right;">
				      		<div>
				      			<span style="color:#00a4eb" class="textSpan">累计销量</span>
				      			<div class="bottom clearfix">
				      				<span class="countSpan">
										<countTo :startVal='startTotalNum' :endVal='endTotalNum' :duration='3000' separator=',' prefix=''></countTo>
									</span>
				      			</div>
				      		</div>
				      	</el-col>
				      </el-row>
           <div class="echarts-bottom"></div>
				 </el-card>
			</el-col>
			<el-col :span="18">
				<el-card shadow="always" style="height: 300px;">
				     <minuteShoppingNumComponent/>
          <div class="echarts-bottom"></div>
				</el-card>
			</el-col>
		</el-row>
		
		<el-row :gutter="16" style="margin-top: 18px;" >
			<el-col :span="6">
				 <el-card shadow="always" style="height: 300px;">
				      <goodsSellNumComponent/>
           <div class="echarts-bottom"></div>
				 </el-card>
			</el-col>
			<el-col :span="10">
				 <el-card shadow="always" style="height: 300px;">
					  <brandSellNumComponent/>
           <div class="echarts-bottom"></div>
				 </el-card>
			</el-col>
			<el-col :span="4">
				 <el-card shadow="always" style="height: 300px;">
				      <userShoppingRankingComponent/>
           <div class="echarts-bottom"></div>
				 </el-card>
			</el-col>
			<el-col :span="4">
				<el-card shadow="always" style="height: 300px;">
				     <genderShoppingNumComponent/>
          <div class="echarts-bottom"></div>
				</el-card>
			</el-col>
		</el-row>
		
		<el-row style="margin-top: 18px;">
			<el-col :span="24">
				<el-card shadow="always" style="height: 300px;">
				     <genderShoppingTimeComponent/>
          <div class="echarts-bottom"></div>
				</el-card>
			</el-col>
		</el-row>
		
		</div>
		</el-collapse-transition>
	</div>
</template>

<script>
	import userShoppingRankingComponent from '../component/UserShoppingRanking.vue'
	import minuteShoppingNumComponent from '../component/MinuteShoppingNum.vue'
	import genderShoppingTimeComponent from '../component/GenderShoppingTime.vue'
	import goodsSellNumComponent from '../component/GoodsSellNum.vue'	
	import brandSellNumComponent from '../component/BrandSellNum.vue'
	import genderShoppingNumComponent from '../component/GenderShoppingNum.vue'
	
	import {ajaxGet} from '../api/api.js'
  import dayjs from 'dayjs'
	
	//数字滚动特殊组件，安装：npm install vue-count-to
	import countTo from 'vue-count-to'
	
	export default {
		data() {
			return {
				radio4:null,
				show3: true,
				totalNum:0,
				startTotalNum:0,
				endTotalNum:0,
				totalPrice: 0.0,
				startTotalPrice:0.0,
				endTotalPrice:0.0,
				timer: null,
        currentTime: undefined,
        currentYear:undefined,
        currentWeek:undefined,

			}
		},
		created: function() { //在组件创建完毕后加载
			let _this = this;
			this.timer = setInterval(function () {
				_this.getTotalNum();
				_this.getTotalPrice();
			}, this.GLOBAL_VAR._intervalTime);
		},
		components:{
			countTo,
			userShoppingRankingComponent,
			minuteShoppingNumComponent,
			genderShoppingTimeComponent,
			goodsSellNumComponent,
			brandSellNumComponent,
			genderShoppingNumComponent
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
				this.getTotalNum();
				this.getTotalPrice();
        this.getCurrentTime()
			},

      getCurrentTime() {
        setInterval(() => {
          this.currentTime = dayjs(new Date()).format('HH:mm:ss')
          this.currentYear = dayjs(new Date()).format('YYYY年MM月DD日')
          this.currentWeek = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六'][new Date().getDay()]
        },1000)
      },

			getTotalNum(){
				let _this = this;
				ajaxGet('/api/count/totalNum', function(result){
					if (result.code === '1'){
						if (_this.totalNum != result.data){
							_this.totalNum = result.data;
							_this.startTotalNum = _this.endTotalNum;
							_this.endTotalNum = _this.totalNum;
						}
					}
				});
			},
			getTotalPrice(){
				let _this = this;
				ajaxGet('/api/count/totalPrice', function(result){
					if (result.code === '1'){
						//要判断值有没有更新过，否则不累计
						if (_this.totalPrice != result.data){
							_this.totalPrice = result.data;
							_this.startTotalPrice = _this.endTotalPrice;
							_this.endTotalPrice = _this.totalPrice;
						}
					}
				});
			}
		}
	}
</script>

<style>
.echarts-wrapper {
  background: url('../../static/images/interact.png') no-repeat center 0;
}

.el-card {
  position: relative;
  padding: 0;
  background: #041734;
  border: none;
}

.el-card__body::after {
  content: '';
  width: 11px;
  height: 11px;
  border: 3px solid #005bba;
  position: absolute;
  top: 0;
  right: 0;
  border-left: none;
  border-bottom: none;
  display: block;
}

.el-card__body::before {
  content: '';
  width: 11px;
  height: 11px;
  border: 3px solid #005bba;
  display: block;
  border-right: none;
  border-bottom: none;
}


.el-card__body {
  padding: 0;
}

.echarts-bottom {
  /*position: absolute;*/
  /*bottom: 0;*/
}

.echarts-bottom::after {
  content: '';
  width: 11px;
  height: 11px;
  border: 3px solid #005bba;
  position: absolute;
  bottom: 0;
  right: 0;
  border-top: none;
  border-left: none;
  display: block;
}

.echarts-bottom::before {
  content: '';
  width: 11px;
  height: 11px;
  border: 3px solid #005bba;
  display: block;
  border-top: none;
  border-right: none;
  position: absolute;
  bottom: 0;
}

.countSpan {
  font-size: 42px;
  color: white !important;
  opacity: .7;
  overflow: hidden;
  display: inline-block;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 260px;
}

	.textSpan{
		line-height: 28px; color: rgba(0,0,0,.45); font-size: 16px; 
	}
	.countSpan{
		font-size: 48px; box-sizing: inherit;font-weight: 700;color: #666;cursor: pointer;
	}
	.iconLi{
		padding: 0px; font-size: 98px; box-sizing: inherit;
	}
</style>
