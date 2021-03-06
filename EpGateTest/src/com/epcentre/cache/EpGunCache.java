package com.epcentre.cache;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.config.GameConfig;
import com.epcentre.config.Global;
import com.epcentre.constant.ChargeStatus;
import com.epcentre.constant.EpConstant;
import com.epcentre.constant.EpConstantErrorCode;
import com.epcentre.constant.EpProtocolConstant;
import com.epcentre.constant.EventConstant;
import com.epcentre.constant.UserConstant;
import com.epcentre.dao.DB;
import com.epcentre.epconsumer.ChinaMobileService;
import com.epcentre.epconsumer.StopCarOrganService;
import com.epcentre.model.RateInfo;
import com.epcentre.model.TblBespoke;
import com.epcentre.model.TblChargingrecord;
import com.epcentre.model.TblElectricPileGun;
import com.epcentre.net.message.MobiCommon;
import com.epcentre.protocol.ChargeCmdResp;
import com.epcentre.protocol.ChargeEvent;
import com.epcentre.protocol.EpBespResp;
import com.epcentre.protocol.EpCancelBespResp;
import com.epcentre.protocol.EpEncodeProtocol;
import com.epcentre.protocol.Iec104Constant;
import com.epcentre.protocol.NoCardConsumeRecord;
import com.epcentre.protocol.SingleInfo;
import com.epcentre.protocol.UtilProtocol;
import com.epcentre.protocol.WmIce104Util;
import com.epcentre.sender.EpMessageSender;
import com.epcentre.service.AnalyzeService;
import com.epcentre.service.AppApiService;
import com.epcentre.service.ChargingInfo;
import com.epcentre.service.EpBespokeService;
import com.epcentre.service.EpChargeService;
import com.epcentre.service.EpCommClientService;
import com.epcentre.service.EpGunService;
import com.epcentre.service.EpService;
import com.epcentre.service.PhoneService;
import com.epcentre.service.RateService;
import com.epcentre.service.StatService;
import com.epcentre.service.UserService;
import com.epcentre.test.ImitateEpService;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.ObjectCacheMap;
import com.epcentre.utils.StringUtil;
import com.netCore.queue.RepeatMessage;
//import com.netCore.queue.RepeatMessage;

public class EpGunCache {
	
	private static final Logger logger = LoggerFactory.getLogger(EpGunCache.class);
	
	private int concentratorId;

	private int pkEpId; 
	
	private String  epCode;
	
	private int pkEpGunId;
	
	private int epGunNo;
	
	int startEnteryNum; //查询电桩业务起始位置

	int currentType;
	
	private int status;
	
	private INetObject epNetObject;//电桩网络连接
	
	private ReadWriteLock myLock;//执行操作所需的锁对象 
	
	private BespCache bespCache;
	
	private ChargeCache chargeCache;
	
	RealChargeInfo realChargeInfo;
	
	private boolean isNeedFronzeAmt;
	
	private int  curUserId;//
	private String curUserAccount;
	
	private String identyCode;// 识别码
	
	private long createIdentyCodeTime;//生成识别码的时间
	
	private int chargingMethod;
	
	private long lastUDTime;//更新到数据库的信息
	
	private long lastUPTime;//手机更新时间
	
	private int cardOrigin;
	
	private long lastSendToMonitorTime; //记录上一次发送给监控系统的时间
	
	private long lastSendToStopCarOrganTime; //记录上一次发送实时数据给停车办的时间
	
	private ObjectCacheMap bespHisoryList;//预约历史表
	private ObjectCacheMap chargeHisoryList;////充电历史表

	public long getLastSendToMonitorTime() {
		return lastSendToMonitorTime;
	}
	public void setLastSendToMonitorTime(long lastSendToMonitorTime) {
		this.lastSendToMonitorTime = lastSendToMonitorTime;
	}

	public EpGunCache(){
		
		myLock =new ReentrantReadWriteLock();  
		
		lastUDTime =0;//更新到数据库的信息
		
		lastUPTime =0;//手机更新时间
		currentType =0;
		curUserAccount="";
		
		lastSendToMonitorTime = 0;//送实时监控的时间
		
		startEnteryNum=0;
		
		identyCode="";
		createIdentyCodeTime = 0;
		lastSendToStopCarOrganTime = 0;
		
	}
	
	
	public long getCreateIdentyCodeTime() {
		return createIdentyCodeTime;
	}
	public void setCreateIdentyCodeTime(long createIdentyCodeTime) {
		this.createIdentyCodeTime = createIdentyCodeTime;
	}
	
	public String getIdentyCode() {
		return identyCode;
	}
	
	public void setIdentyCode(String identyCode) {
		this.identyCode = identyCode;
	}
	
	
	public int getPkEpGunId() {
		return pkEpGunId;
	}

	public void setPkEpGunId(int pkEpGunId) {
		this.pkEpGunId = pkEpGunId;
	}

	public int getConcentratorId() {
		return concentratorId;
	}

	public void setConcentratorId(int concentratorId) {
		this.concentratorId = concentratorId;
	}

	public int getPkEpId() {
		return pkEpId;
	}
	
	@SuppressWarnings("unused")
	private void setUserInfo(int userId,String userAccount)
	{
		this.curUserId = userId;
		this.curUserAccount = userAccount;
	}
	public String getCurUserAccount() {
		return curUserAccount;
	}
	
	public int getCurChargeUserId()
	{
		if(curUserId!=0)
			return curUserId;
		else
		{
			if(this.chargeCache!=null)
			{
				return chargeCache.getUserId();
			}
			return curUserId;
		}
	}
	public int getCurBespokeUserId()
	{
		if(curUserId!=0)
			return curUserId;
		else
		{
			if(this.bespCache!=null)
			{
				return bespCache.getAccountId();
			}
			return curUserId;
		}
	}
	public int getCurUserId() {
		return curUserId;
	}
	

	
	public int getStartEnteryNum() {
		return startEnteryNum;
	}
	public void setStartEnteryNum(int startEnteryNum) {
		this.startEnteryNum = startEnteryNum;
	}
	

	public INetObject getEpNetObject() {
		return epNetObject;
	}

	public void setEpNetObject(INetObject epNetObject) {
		onNetStatus(1);
		this.epNetObject = epNetObject;
	}

	public void setPkEpId(int pkEpId) {
		this.pkEpId = pkEpId;
	}

	public String getEpCode() {
		return epCode;
	}

	public void setEpCode(String epCode) {
		this.epCode = epCode;
	}

	public int getEpGunNo() {
		return epGunNo;
	}

	public void setEpGunNo(int epGunNo) {
		this.epGunNo = epGunNo;
	}

	public int getStatus() {
		return status;
	}

	public BespCache getBespCache() {
		
		 myLock.readLock().lock(); 
		 
		 BespCache retBespCache= null;
		 retBespCache = this.bespCache;
         //释放读锁  
         myLock.readLock().unlock();  
         
		return retBespCache;
	}
	
	public int getCardOrigin() {
		return cardOrigin;
	}
	public void setCardOrigin(int cardOrigin) {
		this.cardOrigin = cardOrigin;
	}
	public void setBespCache(BespCache bespCache) {
		//获取写锁  
        myLock.writeLock().lock(); 
        
        this.bespCache = bespCache;
        
        myLock.writeLock().unlock(); 
	}

	public ChargeCache getChargeCache() {
		
		myLock.readLock().lock(); 
		 
		ChargeCache retChargeCache= null;
		retChargeCache = this.chargeCache;
         //释放读锁  
        myLock.readLock().unlock();  
         
		return retChargeCache;
	}

	public void setChargeCache(ChargeCache chargeCache) {
		myLock.readLock().lock(); 
		 
		
		this.chargeCache = chargeCache;
         //释放读锁  
        myLock.readLock().unlock();  
	}

	public RealChargeInfo getRealChargeInfo() {
		return realChargeInfo;
	}

	public void setRealChargeInfo(RealChargeInfo realChargeInfo) {
		this.realChargeInfo = realChargeInfo;
	}

	public boolean isNeedFronzeAmt() {
		return isNeedFronzeAmt;
	}

	public void setNeedFronzeAmt(boolean isNeedFronzeAmt) {
		this.isNeedFronzeAmt = isNeedFronzeAmt;
	}
	public int checkSingleYx(int value)
	{
		int ret=0;
		if(value!=0 && value!=1)
		{
			ret = -1;
		}
		return ret;
	}
	

	
	
	
	private boolean checkGunStatus(Map<Integer, SingleInfo> changePointMap)
	{
		boolean isGunStatus=false;
		Iterator iter = changePointMap.entrySet().iterator();
 		
 		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			int pointAddr = ((Integer)entry.getKey()).intValue();
			if(pointAddr == EpProtocolConstant.YC_WORKSTATUS||
					pointAddr ==EpProtocolConstant.YX_1_GUN_LID||
					pointAddr ==EpProtocolConstant.YX_1_GUN_SIT||
					pointAddr ==EpProtocolConstant.YX_1_LINKED_CAR||
					pointAddr ==EpProtocolConstant.YX_1_CAR_PLACE)
			{
				isGunStatus= true;
				break;
			}
		}
		
		return isGunStatus;
	}
	
	public void onRealDataChange(Map<Integer, SingleInfo> pointMap,int type)
	{
		int currentType = realChargeInfo.getCurrentType();
		if(currentType!= EpConstant.EP_AC_TYPE && currentType !=  EpConstant.EP_DC_TYPE )
			return ;
	
		Iterator iter = pointMap.entrySet().iterator();
		
		int oldEpWorkStatus= this.realChargeInfo.getWorkingStatus();
		

		Map<Integer, SingleInfo> changePointMap= new ConcurrentHashMap<Integer, SingleInfo>();
			
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			
			int pointAddr = ((Integer)entry.getKey()).intValue();
			SingleInfo info=(SingleInfo) entry.getValue();
			int ret=0;
			
			if(currentType== EpConstant.EP_AC_TYPE)
				ret = ((RealACChargeInfo)realChargeInfo).setFieldValue(pointAddr,info);
			else
				ret = ((RealDCChargeInfo)realChargeInfo).setFieldValue(pointAddr,info);
			
			if((type==3) && info.getAddress()>EpProtocolConstant.YX_2_START_POS&& info.getAddress()<EpProtocolConstant.YC_START_POS)
			{
				info.setAddress(info.getAddress()-EpProtocolConstant.YX_2_START_POS);
			}
			else if((type==11 ) && info.getAddress()>EpProtocolConstant.YC_START_POS&& info.getAddress()<EpProtocolConstant.YC_VAR_START_POS)
			{
				info.setAddress(info.getAddress()-EpProtocolConstant.YC_START_POS);
			}
			else if((type==132) && info.getAddress()>EpProtocolConstant.YC_VAR_START_POS)
			{
				info.setAddress(info.getAddress()-  EpProtocolConstant.YC_VAR_START_POS);
			}
			
			if(ret==-1)
			{
				logger.info("setFieldValue value invalid!pointAddr:{},value:{}",pointAddr,info.getIntValue());
			}
			else if(ret ==-3)
			{
				logger.info("setFieldValue address invalid!pointAddr:{}",pointAddr);
			}
			else if(ret == -2)
			{
				logger.debug("setFieldValue reserved address!pointAddr:{}",pointAddr);
			}
			else
			{
				if(ret==1)
				{
					if(type==1 || type==3 || type==11|| type==132 ||type==0)
					{
						changePointMap.put(info.getAddress(),info);

						
					}
				}
				
			}
		}
	
	
		int newStatus=-1;
		
		//如果电桩重来没有更新过工作状态，那么不处理工作状态
		if(realChargeInfo!=null && this.realChargeInfo.getWorkStatusUpdateTime()>0)//
		{
			newStatus = EpGunService.convertEpWorkStatus(this.realChargeInfo.getWorkingStatus());
			
			logger.debug("convertEpWorkStatus,realChargeInfo.getWorkingStatus():{},newStatus:{}",realChargeInfo.getWorkingStatus(),newStatus);
			
			if(newStatus!=-1)
			{
				this.onStatusChange(newStatus);
			}
			
			logger.debug("status:{},newStatus:{}",this.status,newStatus);

			logger.debug("oldEpWorkStatus:{},new EpWorkStatus:{}",oldEpWorkStatus,this.realChargeInfo.getWorkingStatus());
			logger.debug("changePointMap.size():{},type:{}",changePointMap.size(),type);
			//处理后台监控信息
			
			this.onEpWorkStatusChange(oldEpWorkStatus,changePointMap,type);
		
			sendRealDataToStopCarOrgan(changePointMap,type,oldEpWorkStatus,this.realChargeInfo.getWorkingStatus());

			if(this.status == EpConstant.EP_GUN_STATUS_CHARGE)
			{	
				saveRealInfoToDbWithCharge();
				
				Map<String ,Object> respMap = new ConcurrentHashMap<String, Object>();
				respMap.put("epcode", epCode);
				respMap.put("epgunno", epGunNo);
				
				ChargingInfo  chargingInfo = calcCharingInfo();
				
				if(chargingInfo != null)
				{
					if(this.cardOrigin == 5)
					{
						handleEventExtra(EventConstant.EVENT_REAL_CHARGING,this.cardOrigin,0,0,respMap,(Object)chargingInfo);
					}
					else
					{
						handleEvent(EventConstant.EVENT_REAL_CHARGING,0,0,respMap,(Object)chargingInfo);
					}
				}
			}
			else
			{
				saveRealInfoToDb();
			}
			
		}
		
	}
	
	
	public void dispatchRealChangeData(Map<Integer, SingleInfo> changePointInfo, Object paramsMap,int type,int oldEpWorkStatus, int newEpWorkStatus)
	{
		if(changePointInfo==null || paramsMap==null)
		{
			logger.error("dispatchWholeRealData");
		}
	
		//单位遥信
		Map<Integer, SingleInfo> oneBitYxRealInfo = new ConcurrentHashMap<Integer, SingleInfo>();
		Map<Integer, SingleInfo> twoBitYxRealInfo = new ConcurrentHashMap<Integer, SingleInfo>();
		Map<Integer, SingleInfo> ycRealInfo = new ConcurrentHashMap<Integer, SingleInfo>();
		Map<Integer, SingleInfo> varYcRealInfo = new ConcurrentHashMap<Integer, SingleInfo>();
		
		Iterator iter = changePointInfo.entrySet().iterator();
			
		while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();	
				int  address=(Integer) entry.getKey();
				
				
				if(address<EpProtocolConstant.YX_2_START_POS)
				{
					SingleInfo loopInfo = (SingleInfo)entry.getValue();
					oneBitYxRealInfo.put(address,(SingleInfo)entry.getValue());
				}
				else if(address<EpProtocolConstant.YC_START_POS)
				{
					address = address -EpProtocolConstant.YX_2_START_POS;
					
					SingleInfo loopInfo = (SingleInfo)entry.getValue();
					loopInfo.setAddress(address);
					twoBitYxRealInfo.put(address ,loopInfo);
				}
				else if(address<EpProtocolConstant.YC_VAR_START_POS)
				{
					address = address -EpProtocolConstant.YC_START_POS;
					logger.debug("yc address!{}",address);
					SingleInfo loopInfo = (SingleInfo)entry.getValue();
					loopInfo.setAddress(address);
					ycRealInfo.put(address ,loopInfo);
				}
				else
				{
					address = address -EpProtocolConstant.YC_VAR_START_POS;
					SingleInfo loopInfo = (SingleInfo)entry.getValue();
					loopInfo.setAddress(address);
					varYcRealInfo.put(address ,loopInfo);
				}
				
		}
		if(oneBitYxRealInfo.size()>0)
			AnalyzeService.onEvent(EventConstant.EVENT_ONE_BIT_YX,null,0,0,(Object)paramsMap,(Object)oneBitYxRealInfo);
		if(twoBitYxRealInfo.size()>0)
			AnalyzeService.onEvent(EventConstant.EVENT_TWO_BIT_YX,null,0,0,(Object)paramsMap,(Object)twoBitYxRealInfo);
		
		if(newEpWorkStatus==3 )
		{
			if(ycRealInfo.size()>0)
				AnalyzeService.onEvent(EventConstant.EVENT_YC,null,0,0,(Object)paramsMap,(Object)ycRealInfo);
			if(varYcRealInfo.size()>0)
				AnalyzeService.onEvent(EventConstant.EVENT_VAR_YC,null,0,0,(Object)paramsMap,(Object)varYcRealInfo);
		}
		else
		{
			//不充电情况下，只有工作状态和地锁才上报数据变化
			if(ycRealInfo.size()>0)
			{
				Iterator yciter = ycRealInfo.entrySet().iterator();
				
				while (yciter.hasNext()) 
				{
					Map.Entry entry = (Map.Entry) yciter.next();	
					int  address=(Integer) entry.getKey();
					
					if(address!= 1 &&  address !=2 )
					{
						yciter.remove();
					}
				}
			}
			if(ycRealInfo.size()>0)
			{
				AnalyzeService.onEvent(EventConstant.EVENT_YC,null,0,0,(Object)paramsMap,(Object)ycRealInfo);
			}
		}
	}
	public void sendRealDataToStopCarOrgan(Map<Integer, SingleInfo> changePointMap,int type,int oldEpWorkStatus, int newEpWorkStatus)
	{
		if(StopCarOrganService.getValid()==0)
		{
			return;
		}
		long statusTime= DateUtil.getCurrentSeconds();
		
		int faultCode =7;
		if(this.realChargeInfo.getWorkingStatus() ==EpConstant.EP_GUN_W_STATUS_URGENT_STOP)
			faultCode =0; //急停故障
		else if(this.realChargeInfo.getWorkingStatus() ==EpConstant.EP_GUN_W_STATUS_METER_FAULT)
			faultCode =1; //电表故障
		else if(this.realChargeInfo.getWorkingStatus() ==EpConstant.EP_GUN_W_STATUS_CONTACTOR_FAULT)
			faultCode =2; //接触器故障
		else if(this.realChargeInfo.getCardReaderFault() ==1)
			faultCode =3; //读卡器故障
		else if(this.realChargeInfo.getChargeOverTemp() ==1)
			faultCode =4; //内部过温故障
		else if(this.realChargeInfo.getWorkingStatus() ==EpConstant.EP_GUN_W_STATUS_INSULATION_FAULT)
			faultCode =6; //绝缘故障
		
		int workStatus = this.realChargeInfo.getWorkingStatus();
		int linkCarStatus = this.realChargeInfo.getLinkCarStatus();
		
		int soc = this.realChargeInfo.getSoc();
		int chargeRemainTime = this.realChargeInfo.getChargeRemainTime();
		BigDecimal vol_a = new BigDecimal(0.0);
		BigDecimal vol_b = new BigDecimal(0.0);
		BigDecimal vol_c = new BigDecimal(0.0);
		BigDecimal cur_a = new BigDecimal(0.0);
		BigDecimal cur_b = new BigDecimal(0.0);
		BigDecimal cur_c = new BigDecimal(0.0);
		BigDecimal power = new BigDecimal(0.0);
		BigDecimal volt = UtilProtocol.intToBigDecimal1(this.realChargeInfo.getOutVoltage());
		BigDecimal cur = UtilProtocol.intToBigDecimal2(this.realChargeInfo.getOutCurrent());
		if(currentType == EpConstant.EP_DC_TYPE)
		{
			if(((RealDCChargeInfo)realChargeInfo).getChargeOutLinkerStatus() !=1)
				   faultCode =5; //连接器故障
			vol_a = UtilProtocol.intToBigDecimal1(((RealDCChargeInfo)realChargeInfo).getInAVol());
			vol_b = UtilProtocol.intToBigDecimal1(((RealDCChargeInfo)realChargeInfo).getInBVol());
			vol_c = UtilProtocol.intToBigDecimal1(((RealDCChargeInfo)realChargeInfo).getInCVol());
			cur_a = UtilProtocol.intToBigDecimal2(((RealDCChargeInfo)realChargeInfo).getInACurrent());
			cur_b = UtilProtocol.intToBigDecimal2(((RealDCChargeInfo)realChargeInfo).getInBCurrent());
			cur_c = UtilProtocol.intToBigDecimal2(((RealDCChargeInfo)realChargeInfo).getInCCurrent());
			
		}
		int ret = 0;
		if(oldEpWorkStatus!=this.realChargeInfo.getWorkingStatus()){
			 ret = 1; //工作状态变化时送一次
		}
		else
		{
			if(type==1) //变化单点遥信判断
			{
				Iterator iter = changePointMap.entrySet().iterator();
				
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();	
					int  address=(Integer) entry.getKey();
					if(address == EpProtocolConstant.YX_1_LINKED_CAR 
							||  address ==EpProtocolConstant.YX_1_CARD_READER_FAULT) //是否连接电池(车辆)\读卡器通讯故障 变化
					{
						ret = 1;
						break;
					}
			     }
			}
			else if(type==3)//变化双点遥信判断
			{
                Iterator iter = changePointMap.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();	
					int  address=(Integer) entry.getKey();
					if(address == EpProtocolConstant.YX_2_CHARGE_OVER_TEMP-EpProtocolConstant.YX_2_START_POS )//充电机过温
					{
						ret = 1;
						break;
					}
			     }
			}
			else if(type == 0)//全数据包判断
			{
				 Iterator iter = changePointMap.entrySet().iterator();
				 while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();	
					int  address=(Integer) entry.getKey();
					if(address == EpProtocolConstant.YX_1_LINKED_CAR
					  || address ==EpProtocolConstant.YX_1_CARD_READER_FAULT 
					  ||address == EpProtocolConstant.YX_2_CHARGE_OVER_TEMP )//充电机过温
					{
						ret = 1;
						break;
					}
				}
			}
			if(newEpWorkStatus == EpConstant.EP_GUN_W_STATUS_WORK)
			{
				long diff = lastSendToStopCarOrganTime-statusTime;
				if(diff>=60){
					ret = 1;
				}
			}
			if(lastSendToStopCarOrganTime==0)//程序启动后第一次
				  ret = 1;
			
		}
		if(ret==1)
		{
			StopCarOrganService.realData(epCode, epGunNo,workStatus,linkCarStatus,faultCode,
					statusTime,vol_a, vol_b, vol_c, cur_a, cur_b,cur_c, power, volt,cur, soc, chargeRemainTime);
			lastSendToStopCarOrganTime = statusTime;
		}
		
	}
	
	
	public void sendRealChangetoMonitor(Map<Integer, SingleInfo> changePointMap,int type,int oldEpWorkStatus, int newEpWorkStatus)
	{
		
		int changeSize = changePointMap.size();
		if(changeSize<1)
		{
			return;
			
		}
		
		Map<String ,Object> paramsMap = new ConcurrentHashMap<String, Object>();
		paramsMap.put("epcode", epCode);
		paramsMap.put("epgunno", epGunNo);
		paramsMap.put("currenttype", this.realChargeInfo.getCurrentType());
			
		if(type==1)
		{
			AnalyzeService.onEvent(EventConstant.EVENT_ONE_BIT_YX,null,0,0,(Object)paramsMap,(Object)changePointMap);
			
		}
		else if(type==3)
		{
			 AnalyzeService.onEvent(EventConstant.EVENT_TWO_BIT_YX,null,0,0,(Object)paramsMap,(Object)changePointMap);
			  
		}
		if(type== 11 || type==132)
		{
			if(newEpWorkStatus == 3 )
			{
				if(type == 11)
					AnalyzeService.onEvent(EventConstant.EVENT_YC,null,0,0,(Object)paramsMap,(Object)changePointMap);
				else
					AnalyzeService.onEvent(EventConstant.EVENT_VAR_YC,null,0,0,(Object)paramsMap,(Object)changePointMap);
			}
			else
			{
				//不充电情况下，只有工作状态和地锁才上报数据变化
				if(type==11)
				{
					Iterator iter = changePointMap.entrySet().iterator();
					
					while (iter.hasNext()) {
							Map.Entry entry = (Map.Entry) iter.next();	
							int  address=(Integer) entry.getKey();
							
							if(address!= 1 &&  address !=2 )
							{
								iter.remove();
							}
					}
					
					AnalyzeService.onEvent(EventConstant.EVENT_YC,null,0,0,(Object)paramsMap,(Object)changePointMap);
				}
			}
		}
		else
		{
			dispatchRealChangeData(changePointMap,(Object)paramsMap,type, oldEpWorkStatus,  newEpWorkStatus);
		}
	}
	public ChargingInfo getCharingInfo()
	{
		if(chargeCache==null)
		{
			return null;
		}
		long now = DateUtil.getCurrentSeconds();
		long diff = now - this.lastUPTime;
		if(diff>EpConstant.U_PHONE_INTERVAL)//三分钟
		{
			this.lastUPTime= now;
			
			ChargingInfo charingInfo= calcCharingInfo();
			
			return charingInfo;
		}
		return null;
	}
	
	public void saveRealInfoToDb()
	{
		long now=DateUtil.getCurrentSeconds();
		long diff = DateUtil.getCurrentSeconds() - this.lastUDTime;
		if(diff>EpConstant.U_CHARGEINFO_INTERVAL)//三分钟
		{
			lastUDTime = now;
			int currentType = realChargeInfo.getCurrentType();
			if(currentType == EpConstant.EP_AC_TYPE){
				((RealACChargeInfo)realChargeInfo).saveDb();
			}
			else
			{		
				((RealDCChargeInfo)realChargeInfo).saveDb();
			}
		}
	}
	public void saveRealInfoToDbWithCharge()
	{
		logger.debug("saveRealInfoToDbWithCharge");
		long now=DateUtil.getCurrentSeconds();
		long diff = now - this.lastUDTime;
		if(diff<EpConstant.U_CHARGEINFO_INTERVAL)//三分钟
			return ;
		
		lastUDTime = now;
		int currentType = realChargeInfo.getCurrentType();
		if(currentType == EpConstant.EP_AC_TYPE){
			((RealACChargeInfo)realChargeInfo).saveDb();
		}
		else
		{		
			((RealDCChargeInfo)realChargeInfo).saveDb();
		}
			
		if(realChargeInfo.getWorkingStatus() == EpConstant.EP_GUN_W_STATUS_WORK&&
				currentType == EpConstant.EP_DC_TYPE)
		{
			logger.debug("saveRealInfoToDbWithCharge");
			String chargeSerialNo= chargeCache.getChargeSerialNo();
			
			((RealDCChargeInfo)realChargeInfo).saveChargeCarInfoToDB(chargeSerialNo);
			((RealDCChargeInfo)realChargeInfo).savePowerModuleToDB(chargeSerialNo);
		}
		
	}
	public void onRedoBespokeSuccess()
	{
		logger.info("onRedoBespokeSuccess,status:{}",this.status);
		if(bespCache.getRedo()!=1)
		{
			return ;
		}
		
		bespCache.setRedo((short)0);
		//预约成功，告警信息需要重置
		bespCache.setExpirWarn(false);
		
		long addingTime = 0;
		BigDecimal addingFronzeAmt= new BigDecimal(0.0);
		
		addingTime  =bespCache.getBuyTimes() * 60;// 秒
		addingFronzeAmt = RateService.calcBespAmt(bespCache.getRate(), addingTime / 60);
		
		long et = bespCache.getEndTime() + bespCache.getBuyTimes()* 60;

		bespCache.setEndTime(et);
		bespCache.setRealEndTime(et);
		bespCache.setFronzeAmt(bespCache.getFronzeAmt().add(addingFronzeAmt));
		bespCache.setBuyTimes(0);// 续约成功，买断时间归零
		
		
		EpBespokeService.updateRedoBespokeToDb(bespCache.getBespId(), bespCache);
		
		if(bespCache.getPayMode() ==  EpConstant.P_M_FIRST)
		{
			UserService.subAmt(bespCache.getAccountId(), addingFronzeAmt);
		}
	
		 do_bespoke_consume_resp(1,0,bespCache.getAccountId(),1,bespCache.getBespNo());
  	
		logger.info("onRedoBespokeSuccess,account:{},BespAmt:{}",bespCache.getAccount(),addingFronzeAmt);
	}
	/**
	 * 1:根据预约应答成功
	 * 2：根据状态预约成功
	 * @param method
	 */
	public void onBespokeSuccess(int method)
	{
		logger.info("onBespokeSuccess,epCode:{},epGunNo:{},method:{}",new Object[]{this.epCode,this.epGunNo,method});
		
		if(bespCache!=null && bespCache.getStatus()!= EpConstant.BESPOKE_STATUS_LOCK)
		{
			StatService.addBespoke();
			
			bespCache.setStatus(EpConstant.BESPOKE_STATUS_LOCK);
			
			long addingTime = 0;
			
			BigDecimal addingFronzeAmt= new BigDecimal(0.0);
			addingTime  = bespCache.getEndTime()- bespCache.getStartTime();// 秒
			addingFronzeAmt = RateService.calcBespAmt(bespCache.getRate(), addingTime / 60);
			bespCache.setFronzeAmt(addingFronzeAmt);
			if(bespCache.getPayMode() ==  EpConstant.P_M_FIRST)
			{
				logger.info("subamt userId:{},BespNo:{},BespAmt:{}",
		        		   new Object[]{bespCache.getAccount(),bespCache.getBespNo(),addingFronzeAmt});
		            
				UserService.subAmt(bespCache.getAccountId(), addingFronzeAmt);
			}
			
			long pkBespId = EpBespokeService.insertBespokeToDb(this.pkEpId, this.pkEpGunId, bespCache);	
			bespCache.setBespId(pkBespId);
			
			//logger.info("onBespokeSuccess,modifyStatus status:"+this.status+ EpGunService.getGunStatusDesc(this.status));
			UserCache u= UserService.getUserCache(bespCache.getAccount());
			
			u.setUseGunStaus(EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED);
            
            do_bespoke_consume_resp(1,0,bespCache.getAccountId(),0,bespCache.getBespNo());
    		
		   
		}
		
	}
	
	public void onBespokeFail(int errorCode)
	{
		logger.info("onBespokeFail,errorCode:"+errorCode);
		
		if(bespCache!=null && bespCache.getStatus()== EpConstant.BESPOKE_STATUS_CONFIRM)
		{
			StatService.subBespoke();
			
			bespCache.setStatus(EpConstant.BESPOKE_STATUS_FAIL);

			//通知前端
			Map<String, Object> chargeMap = new ConcurrentHashMap<String, Object>();
			
			chargeMap.put("epcode", epCode);
			chargeMap.put("epgunno", epGunNo);
			logger.debug("onBespokeFail EventConstant.EVENT_BESPOKE");
			handleEvent(EventConstant.EVENT_BESPOKE,0,errorCode,null,chargeMap);
		}
		
		cleanBespokeInfo();
	}
	
	public void onEndBespoke()
	{
		logger.debug("onEndBespoke");
		if(this.bespCache!=null)
		{
			long now = DateUtil.getCurrentSeconds();
			long diff = now - this.bespCache.getStartTime();
			if(diff >60)
			{
				endBespoke( EpConstant.END_BESPOKE_CANCEL );
			}
		}
	}
	
	public void onEndCharge()
	{
		logger.debug("onStartChargeSuccess");
	}

	
	public void onStartChargeSuccess()
	{
		logger.debug("onStartChargeSuccess");
		if(chargeCache!=null&& chargeCache.getStatus() == ChargeStatus.CS_ACCEPT_CONSUMEER_CMD )
		{
			chargeCache.setStatus(ChargeStatus.CS_WAIT_INSERT_GUN);
			
			TblChargingrecord record = new TblChargingrecord();
			record.setChreTransactionnumber(chargeCache.getChargeSerialNo());
			record.setStatus(ChargeStatus.CS_WAIT_INSERT_GUN);
			DB.chargingrecordDao.updateBeginRecordStatus(record);
			
			Map<String, Object> chargeMap = new ConcurrentHashMap<String, Object>();
			
			chargeMap.put("epcode", epCode);
			chargeMap.put("epgunno", epGunNo);
			logger.debug("onStartChargeSuccess EventConstant.EVENT_CHARGE");
			handleEvent(EventConstant.EVENT_CHARGE,1,0,null,chargeMap);
		}
		
	}
	
	public void onStartChargeFail(int method,int errorCode)
	{
		logger.debug("onStartChargeFail,method:{},errorCode:{}",method,errorCode);
	
		//电桩接受充电失败.
		if(chargeCache!=null && chargeCache.getStatus()== ChargeStatus.CS_ACCEPT_CONSUMEER_CMD) // 没有在充电状态
		{
			int userId= this.getCurChargeUserId();
			
			String messagekey = String.format("%03d%s", Iec104Constant.C_START_ELECTRICIZE,chargeCache.getChargeSerialNo());
			EpCommClientService.removeRepeatMsg(messagekey);
			
			//1.退钱和修改状态
			EpChargeService.onChargeFail(userId,chargeCache);
			
			//2.通知前端
			Map<String, Object> chargeMap = new ConcurrentHashMap<String, Object>();
			
			chargeMap.put("epcode", epCode);
			chargeMap.put("epgunno", epGunNo);
			
			logger.debug("onStartChargeFail EventConstant.EVENT_CHARGE");
			handleEvent(EventConstant.EVENT_CHARGE,0,errorCode,null,chargeMap);
			
			cleanChargeInfo();
		}
	}
	/**
	 * 1:表示通过电桩充电事件
	 * 2：表示通过状态
	 * @param method
	 */
	
	public int  onStartChargeEventFail(int method,int errorCode)
	{
		logger.debug("onStartChargeEventFail,method:{},chargeCache:{}",method,chargeCache);
		
		if(chargeCache != null && chargeCache.getStatus() == ChargeStatus.CS_WAIT_INSERT_GUN)
		{
			int userId= this.getCurChargeUserId();
		
			//1.退钱和修改状态
			EpChargeService.onChargeFail(userId,chargeCache);
			
			//2.清空实时表信息
			BigDecimal bdZero = new BigDecimal(0.0);
			EpGunService.updateChargeInfoToDbByEpCode(this.currentType,this.epCode,this.epGunNo,
					bdZero,"",bdZero,0,0);
			
			//3.跟前段应答
			Map<String ,Object> respMap = new ConcurrentHashMap<String, Object>();
			respMap.put("epcode", epCode);
			respMap.put("epgunno", epGunNo);
			respMap.put("account",chargeCache.getAccount());
			respMap.put("userId",chargeCache.getUserId());
			
			logger.debug("onStartChargeEventFail EventConstant.EVENT_START_CHARGE_EVENT");
			handleEvent(EventConstant.EVENT_START_CHARGE_EVENT,0,errorCode,null,respMap);
			
			cleanChargeInfo();
			
			return 1;
		}
		else
		{
			return 2;
		}

		
	}
	/**
	 * method,1:通过事件
	 *        2：通过状态
	 * @param method
	 */
	public int onStartChargeEventSuccess(int method,long st)
	{
		logger.debug("onStartChargeEventSuccess,method:"+method);
		
		if(chargeCache!=null && chargeCache.getStatus()!= ChargeStatus.CS_CHARGING)
		{
			String bespokeNo= StringUtil.repeat("0", 12);
			if(this.bespCache!=null)
			{
				bespokeNo =this.bespCache.getBespNo();
				//结束预约
				this.endBespoke(EpConstant.END_BESPOKE_CHARGE);
			}
			
			//给前段应答
			Map<String ,Object> respMap = new ConcurrentHashMap<String, Object>();
			respMap.put("epcode", epCode);
			respMap.put("epgunno", epGunNo);
			respMap.put("account",chargeCache.getAccount());
			respMap.put("userId",chargeCache.getUserId());
			
			logger.debug("onStartChargeEventSuccess EventConstant.EVENT_START_CHARGE_EVENT");
				
			handleEvent(EventConstant.EVENT_START_CHARGE_EVENT,1,0,null,respMap);
			
			UserCache u= UserService.getUserCache(chargeCache.getAccount());
			
			BigDecimal bdFronzeAmt =  UtilProtocol.intToBigDecimal2(chargeCache.getFronzeAmt());
					
		
			Date date = new Date();
			String chOrCode = (int)(date.getTime()/1000) + chargeCache.getAccount();
			
			chargeCache.setChOrCode(chOrCode);
			chargeCache.setStatus(0);
			
			chargeCache.setSt(st);
			
			u.setUseGunStaus(6);
				
			int chorType= EpChargeService.getOrType(u.getLevel());
			
			int userOrgNo=1000;
			if(chargeCache.getUserOrigin()!=null)
				userOrgNo= chargeCache.getUserOrigin().getOrgNo();
			int payMode = chargeCache.getPayMode();
			EpChargeService.insertChargeOrderToDb(chargeCache.getUserId(), chorType,chargeCache.getPkUserCard(),userOrgNo, 
					pkEpId, epCode, epGunNo, this.chargingMethod,bespokeNo, chOrCode, 
					chargeCache.getFronzeAmt(),payMode,userOrgNo,st,chargeCache.getChargeSerialNo(), this.chargeCache.getRateInfo());
			
				
			EpChargeService.updateBeginRecordToDb(chargeCache.getUserId(), chorType,curUserAccount,chargeCache.getPkUserCard(),userOrgNo, 
					pkEpId, epCode, epGunNo, this.chargingMethod,bespokeNo, chOrCode, 
					chargeCache.getFronzeAmt(),st,chargeCache.getChargeSerialNo(),0, this.chargeCache.getRateInfo(),0);
			
				
			pushFirstRealData();
					
            String msg = MessageFormat.format("onEpStartChargeEvent startCharge Success,modifyStatus status:{0},account:{1},SerialNo:{2},FronzeAmt:{3},epcode:{4},gunno:{5}",
					this.status,this.curUserAccount,this.chargeCache.getChargeSerialNo(),bdFronzeAmt,this.epCode,this.epGunNo);
			logger.info(msg);
				
			
			chargeCache.setStatus(ChargeStatus.CS_CHARGING);
			StatService.addCharge();
			
			EpGunService.clearIdentyCode(this.pkEpGunId);
			 
			 this.setIdentyCode("");
			 this.setCreateIdentyCodeTime(0);
			return 1;
		}
		else
		{
			logger.debug("");
			return 2;
		}
		
	}
	
	public void onNetStatus(int epStatus)
	{
		if(this.chargeCache!=null && this.status == EpConstant.EP_GUN_STATUS_CHARGE)
		{
			handleEvent( EventConstant.EVENT_EP_NET_STATUS,epStatus,0,null,null);
			
		}
	}
	private void checkWaitInsertGunCharge()
	{
		if(chargeCache!=null && chargeCache.getStatus()== ChargeStatus.CS_WAIT_INSERT_GUN)
		{
			long now = DateUtil.getCurrentSeconds();
			long diff = now - this.chargeCache.getSt();//超时判断为充电后10分钟
			if(diff > 660)
			{
				this.onStartChargeEventFail(2,6001);
				
			}
		}
	}
	public void checkChargeCmdTimeOut(int method)
	{
		if(chargeCache!=null && chargeCache.getStatus()== ChargeStatus.CS_ACCEPT_CONSUMEER_CMD)
		{
			long now = DateUtil.getCurrentSeconds();
			long diff = now - this.chargeCache.getLastCmdTime();//超时判断为充电后10分钟
			if(diff > GameConfig.chargeCmdTime)
			{
				this.onStartChargeFail(method,6001);
				
			}
		}
	}
	public void checkBespokeCmdTimeOut()
	{
		if(bespCache!=null && bespCache.getStatus()== EpConstant.BESPOKE_STATUS_CONFIRM)
		{
			long now = DateUtil.getCurrentSeconds();
			long diff = now - this.bespCache.getStartTime();
			if(diff>GameConfig.bespokeCmdTime)
			{
				onBespokeFail(6001);
			}
		}
	}
	private boolean gunFault(int status)
	{
		if(status==1 ||
				(status>=EpConstant.EP_GUN_W_STATUS_LESS_VOL_FAULT && status<=EpConstant.EP_GUN_W_STATUS_URGENT_STOP))
		{
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unused")
	private void handleEpTimeout(int oldWorkStatus,int newWorkStatus )
	{
		ElectricPileCache epCache= EpService.getEpByCode(epCode);
		if(epCache.getConcentratorId()<=0)
			return;
		
		if(newWorkStatus == EpConstant.EP_GUN_W_STATUS_OFF_LINE &&
				this.status != EpConstant.EP_GUN_STATUS_IDLE)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_IDLE, true);
			EpService.updateEpCommStatusToDb(epCache.getPkEpId(), 0, 0);
			
		}
		else if(oldWorkStatus == EpConstant.EP_GUN_W_STATUS_OFF_LINE &&
				oldWorkStatus !=newWorkStatus) 
		{
			EpService.updateEpCommStatusToDb(epCache.getPkEpId(), 1, 0);
		}
	}
	/**
	 * 进入电桩初始化状态,不处理电桩任何事物
	 */
	
	private void gotoEpInitStatus()
	{
		this.modifyStatus(EpConstant.EP_GUN_STATUS_EP_INIT, false);
		
	}
	/**
	 * 进入电桩空闲状态。
	 * 1.如果电桩有未完成的预约命令，那么终止预约
	 * 2.如果电桩有未结算的预约，终止并结算预约
	 * 3.如果有等待插枪的充电，那么终止充电
	 */
	private void gotoIdleStatus()//进入空闲状态
	{
		logger.debug("gotoIdleStatus,this.status:{}",this.status);
		
		if(chargeCache!=null)
		{
			logger.debug("gotoIdleStatus,chargeCache.getStatus():{}",chargeCache.getStatus());
			
			if(chargeCache.getStatus() ==  ChargeStatus.CS_ACCEPT_CONSUMEER_CMD)
			{
				checkChargeCmdTimeOut(2);
			}
			else if(chargeCache.getStatus() ==  ChargeStatus.CS_WAIT_INSERT_GUN)
			{
				//如果电桩已经变为空闲，还有等待擦枪，不需要等待10
				checkWaitInsertGunCharge();
			}
			else
			{
				 logger.debug("gotoIdleStatus,chargeCache.getStatus():{}",chargeCache.getStatus());
			}
			
		}
		if(bespCache!=null)
		{
			if( bespCache.getStatus() == EpConstant.BESPOKE_STATUS_LOCK)
			{
				long now = DateUtil.getCurrentSeconds();
				long diff = now - this.bespCache.getStartTime();
				if(diff>GameConfig.bespokeCmdTime)
				{
					endBespoke( EpConstant.END_BESPOKE_CANCEL );
				}
			}
			else if(bespCache.getStatus() == EpConstant.BESPOKE_STATUS_CONFIRM)
			{
				this.checkBespokeCmdTimeOut();
				
			}
			else
			{
				
			}
		}
		
		this.cleanUserInfo();
		
		if(this.status !=  EpConstant.EP_GUN_STATUS_IDLE)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_IDLE, true);
		}
	
	}
	private void gotoUserOperStatus()//插枪或者收枪状态
	{
		if(status != EpConstant.EP_GUN_STATUS_EP_OPER)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_EP_OPER, false);
		}
		
	}
	private void gotoUserAuthStatus()//进入用户鉴权状态
	{
		if(status != EpConstant.EP_GUN_STATUS_USER_AUTH)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_USER_AUTH, false);
		}
	}
	
	
	private void gotoBespokeStatus()//进入预约状态
	{
		this.onBespokeSuccess(2);
		if(this.status !=  EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED, true);
		}
	}
	private void gotoChargeStatus()//进入充电状态
	{
		this.onStartChargeEventSuccess(2,DateUtil.getCurrentSeconds());
		if(this.status !=  EpConstant.EP_GUN_STATUS_CHARGE)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_CHARGE, true);
		}
	}
	private void gotoFaultStatus()//进入故障状态
	{
		checkWaitInsertGunCharge();
		
		if(this.status !=  EpConstant.EP_GUN_STATUS_STOP_USE)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_STOP_USE, true);
		}
		
	}
	private void gotoSettingStatus()//进入设置状态
	{
		this.checkChargeCmdTimeOut(1);
		
		if(this.status!= EpConstant.EP_GUN_STATUS_SETTING)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_SETTING, false);
			EpGunService.updateGunState(getPkEpGunId(), 9);
		}
		
	}
	private void gotoChargeModeStatus()//进入充电模式选择状态
	{
		if(status != EpConstant.EP_GUN_STATUS_SELECT_CHARGE_MODE)
		{
			this.modifyStatus(EpConstant.EP_GUN_STATUS_SELECT_CHARGE_MODE, false);
		}
	}
	//该函数主要用来处理集中器内的电桩
	private void gotoOffLineStatus()
	{
		logger.debug("gotoOffLineStatus,this.status:{}",this.status);
		
		EpCommClient epCommClient = (EpCommClient)epNetObject;
		
		
		if(epCommClient==null || !epCommClient.isComm() || epCommClient.getMode()!=2)
			return;
	
		if(this.status!= EpConstant.EP_GUN_STATUS_OFF_LINE)//
		{
			EpService.updateEpCommStatusToDb(getPkEpId(), 0, 0);
		}
	}
	private void gotoUpgradeStatus()
	{
		if(this.status!= EpConstant.EP_GUN_STATUS_EP_UPGRADE)
		{
			EpGunService.updateGunState(this.getPkEpGunId(), EpConstant.EP_GUN_STATUS_STOP_USE);
			
			this.modifyStatus(EpConstant.EP_GUN_STATUS_EP_UPGRADE, false);
		}
	}
	
	private void onEpWorkStatusChange(int oldEpWorkStatus,Map<Integer, SingleInfo> changePointMap,int changeType)
	{
		
		if(oldEpWorkStatus!=this.realChargeInfo.getWorkingStatus())
		{
			if( oldEpWorkStatus==3)
			{
				this.realChargeInfo.endCharge();
			}
			
			dispatchWholeRealToMonitor(oldEpWorkStatus);

		}
		else
		{
			sendRealChangetoMonitor(changePointMap,changeType,oldEpWorkStatus, this.realChargeInfo.getWorkingStatus());		
		}
		
	}
	
	private void onStatusChange(int newStatus)
	{
		logger.debug("onStatusChange! newStatus:{},status:{}",newStatus,status);

		switch(newStatus)
		{
		case EpConstant.EP_GUN_STATUS_IDLE:
			this.gotoIdleStatus();
			break;
		case EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED:
			this.gotoBespokeStatus();
			break;
		case EpConstant.EP_GUN_STATUS_CHARGE:
			this.gotoChargeStatus();
			break;
		case EpConstant.EP_GUN_STATUS_STOP_USE:
			this.gotoFaultStatus();
			break;
		case EpConstant.EP_GUN_STATUS_EP_OPER:
			this.gotoUserOperStatus();
			break;
		case EpConstant.EP_GUN_STATUS_USER_AUTH:
			this.gotoUserAuthStatus();
			break;
		case EpConstant.EP_GUN_STATUS_SETTING:
			gotoSettingStatus();
			break;
		case EpConstant.EP_GUN_STATUS_FROZEN_AMT:
			break;
		case EpConstant.EP_GUN_STATUS_SELECT_CHARGE_MODE:
			gotoChargeModeStatus();
			break;
		case EpConstant.EP_GUN_STATUS_EP_UPGRADE:
			break;
		case EpConstant.EP_GUN_STATUS_OFF_LINE:
			gotoOffLineStatus();
			break;
		case EpConstant.EP_GUN_STATUS_EP_INIT:
			this.gotoEpInitStatus();
			break;
		default:
			break;
		}
	}

	
	
	
	
	/**
	 * 
	 * @param chargeCmdResp
	 * @return
	 */
	public int onEpStartCharge(ChargeCmdResp chargeCmdResp)
	{
		logger.debug("onEpStartCharge,chargeCmdResp:{}",chargeCmdResp);
		
		if(chargeCmdResp.getRet() ==  1) //电桩接受充电成功,变为等待插枪
		{
			onStartChargeSuccess();
		}
		else
		{
			//电桩接受充电失败
			onStartChargeFail(1,chargeCmdResp.getErrorCause());
		}
		
		return 0;
	}
	private void modifyStatus(int status,boolean modifyDb)
	{
		logger.debug("modifyStatus,this.status:{},status:{}",this.status,status);
		this.status = status;
		
		if(modifyDb)
		{
			EpGunService.updateGunState(this.getPkEpGunId(), status);
		}
	}
	public int onEpStopChargeEvent(int epRet,String userAccount, String aerialNo)
	{
		
		if(epRet ==  0)//充电桩,充电成功
		{
			if(this.status == EpConstant.EP_GUN_STATUS_CHARGE)
			{
				return 3;
			}
			this.status = EpConstant.EP_GUN_STATUS_IDLE;
		}
		else//没插枪超时，那么转为空闲
		{
			//失败.变为空闲
			this.status = EpConstant.EP_GUN_STATUS_IDLE;
		}
		
		return 0;
	}
	/**
	 * 
	 * @param epChargeEvent
	 * @return
	 */
	public int handleStartChargeEvent(ChargeEvent epChargeEvent)
	{
		logger.debug("handleStartChargeEvent,epChargeEvent:{},chargeCache:{}",epChargeEvent,chargeCache);	
		if(chargeCache==null )//特殊卡
		{
			logger.info("handleStartChargeEvent,chargeCache==null" );
			//if( epChargeEvent.getSuccessFlag()==1)
			//	this.onStartChargeEventSuccess(1, epChargeEvent.getStartChargeTime());
			return 2; //
		}
		if(epChargeEvent.getSerialNo().compareTo(chargeCache.getChargeSerialNo())!=0)
		{
			logger.error("onEpStartChargeEvent,invalid serialNo!epChargeEvent.getSerialNo():{},chargeCache.getChargeSerialNo():{}",epChargeEvent.getSerialNo(),chargeCache.getChargeSerialNo());
			return 2;//数据不存在
		}
		int retCode=0;
		if(epChargeEvent.getSuccessFlag() ==  1)//充电桩,充电成功
		{	
			retCode = this.onStartChargeEventSuccess(1,epChargeEvent.getStartChargeTime());
		}
		else//没插枪超时，那么转为空闲
		{
			retCode= onStartChargeEventFail(1,0);
		}
		
		return retCode;

	}
	
	/**
	 * 只负责清楚桩上的用户信息,用户自带的枪和状态信息由事件函数去处理
	 */
	public void cleanUserInfo()
	{
		this.curUserId=0;
		this.curUserAccount="";
	}
	
	public void cleanBespokeInfo()
	{
		if(bespCache!=null)
		{
			String userAccount = bespCache.getAccount();
			int userId = bespCache.getAccountId();
			if(userAccount.length()>0)
			{
				UserCache u1= UserService.getUserCache(userAccount);
				if(u1!=null && u1.getUseGunStaus()== 3)
				{
					u1.clean();
					UserService.putUserCache(u1);
				}
			}
			if(userId>0)
			{
				UserCache u2= UserService.getUserCache(userId);
				if(u2!=null && u2.getUseGunStaus()== 3)
				{
					u2.clean();
					UserService.putUserCache(u2);
				}
			}
		}
 		 setBespCache(null);
	}
	public void cleanChargeInfo()
	{
		if(chargeCache!=null)
		{
			String userAccount = chargeCache.getAccount();
			int userId = chargeCache.getUserId();
			if(userAccount.length()>0)
			{
				UserCache u1= UserService.getUserCache(userAccount);
				if(u1!=null )
				{
					u1.clean();
					UserService.putUserCache(u1);
				}
			}
			if(userId>0)
			{
				UserCache u2= UserService.getUserCache(userId);
				if(u2!=null )
				{
					u2.clean();
					UserService.putUserCache(u2);
				}
			}
		}
		//清除车端信息和电源模块数据
		if(this.currentType == EpConstant.EP_DC_TYPE)
		{
			((RealDCChargeInfo)realChargeInfo).cleanChargeInfo();
		}
		
		 setChargeCache(null);
	}
	public ChargeCache makeChargeInfo(UserCache chargeUser,RateInfo rateInfo,short chargeStyle,int nFrozenAmt,
			int payMode,int orgNo, int cmdFromSource,String cmdChIdentity)
	{
		ChargeCache chargingCacheObj = new ChargeCache();
		
		String serialNo = epCode + EpChargeService.makeSerialNo();
		chargingCacheObj.setChargeSerialNo(serialNo);
		
		chargingCacheObj.setSt(DateUtil.getCurrentSeconds());
		
		chargingCacheObj.setUserId(chargeUser.getId());
		chargingCacheObj.setAccount(chargeUser.getAccount());
		
		chargingCacheObj.setFronzeAmt(nFrozenAmt);
		chargingCacheObj.setRateInfo(rateInfo);
		chargingCacheObj.setStartChargeStyle(chargeStyle);
		chargingCacheObj.setPayMode(payMode);
		UserOrigin userOrigin = new UserOrigin(orgNo,cmdFromSource,cmdChIdentity);
		
		chargingCacheObj.setUserOrigin(userOrigin);
		
		chargingCacheObj.setStatus(ChargeStatus.CS_ACCEPT_CONSUMEER_CMD);
		
		chargingCacheObj.setLastCmdTime(DateUtil.getCurrentSeconds());
		
		return chargingCacheObj;
	}
		
	public int startChargeAction(UserCache chargeUser,RateInfo rateInfo,
			String bespNo,short chargeStyle,int frozenAmt,int payMode,int orgNo, int fromSource, String actionIdentity,byte[]cmdTimes)
	{
		logger.debug("startChargeAction enter");
		int chargingUserId = chargeUser.getId();
		String chargingAccout = chargeUser.getAccount();
		//1.有别人预约,不能充电
		BespCache bespCache = getBespCache();
		if(bespCache!=null && bespCache.getAccountId() != chargingUserId)
		{
				String msg = MessageFormat.format("startChargeAction fail! bespCache.getAccountId() != chargingUserId,chargingUserId:{0}," +
					"bespUserID:{1},epcode:{2},gunno:{3}",chargingUserId,bespCache.getAccountId(),this.epCode,this.epGunNo);
			logger.error(msg);
			return EpConstantErrorCode.EPE_OTHER_BESP;
		}
		
		if(epNetObject == null )
		{
			String msg = MessageFormat.format("startChargeAction fail! epNetObject == null,chargingAccout:{0}," +
					"epCode:{1},epGunNo:{2}",chargingAccout,this.epCode,this.epGunNo);
	
			logger.error(msg);
			return EpConstantErrorCode.EP_UNCONNECTED;
		}
		if( !epNetObject.isComm())
		{
			String msg = MessageFormat.format("startChargeAction fail! status:{0},chargingAccout:{1}" +
					"epCode:{2},epGunNo:{3}",epNetObject.getStatus(),chargingAccout,this.epCode,this.epGunNo);
			
			logger.error(msg);
			return EpConstantErrorCode.EP_UNCONNECTED;
		}
		if(this.chargeCache!=null  )
		{
			if(chargeCache.getUserId() != chargeUser.getId())
			{
				logger.debug("EpConstantErrorCode.EPE_OTHER_CHARGING chargeCache,{}",this.chargeCache);
				return EpConstantErrorCode.EPE_OTHER_CHARGING;
			}
			else
			{
				logger.debug("EpConstantErrorCode.EPE_REPEAT_CHARGE chargeCache,{}",this.chargeCache);
				return EpConstantErrorCode.EPE_REPEAT_CHARGE;
			}
		}
		
		EpCommClient epCommClient = (EpCommClient)epNetObject;
		
		if(this.status == EpConstant.EP_GUN_STATUS_STOP_USE)
		{
			return EpConstantErrorCode.EPE_GUN_FAULT;
		}
		if(this.status == EpConstant.EP_GUN_STATUS_CHARGE)
		{
			return EpConstantErrorCode.EPE_REPEAT_CHARGE;
		}
		 if(this.status == EpConstant.EP_GUN_STATUS_SETTING)
		 {
			return EpConstantErrorCode.EPE_IN_EP_OPER;
		 }

		if(payMode==1)
		{
			int userId = chargeUser.getId();
			BigDecimal bdRemainAmt = UserService.getRemainBalance(userId);
			
			logger.info("before startChargeAction userId:{},remainAmt:{}",userId,bdRemainAmt.doubleValue());
			//100倍后转为整数
			bdRemainAmt = bdRemainAmt.multiply(Global.DecTime2);
			int nRemainAmt= UtilProtocol.BigDecimal2ToInt(bdRemainAmt);
			BigDecimal bdFrozenAmt = UtilProtocol.intToBigDecimal2(frozenAmt);
			
			//冻结金额
			if(nRemainAmt<0 || frozenAmt<=0 || nRemainAmt<frozenAmt)
			{
				logger.info("bdRemainAmt:{},bdFrozenAmt:{}",bdRemainAmt.doubleValue(),bdFrozenAmt.doubleValue());
				return EpConstantErrorCode.EPE_NO_ENOUGH_MONEY;
			}
			//充电冻结金额
			logger.info("startChargeAction userId:{},bdFrozenAmt:{}",chargeUser.getId(),bdFrozenAmt.doubleValue());
			
			UserService.subAmt(chargeUser.getId(), bdFrozenAmt);
			
		}
		
		
		ChargeCache chargingCacheObj = makeChargeInfo(chargeUser, rateInfo, chargeStyle, frozenAmt, payMode,
				orgNo,fromSource,actionIdentity);
		
		setUserInfo(chargingUserId, chargeUser.getAccount());
		
		String transactionNumber = chargingCacheObj.getChargeSerialNo();
		
		this.chargeCache = chargingCacheObj;
		
		if(status != EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
		{
			modifyStatus(EpConstant.EP_GUN_STATUS_USER_AUTH, false);
				
			chargeUser.setUseGun(epCode+epGunNo);
		}
		
		if(bespNo.length()==0)
		{
			bespNo= StringUtil.repeat("0", 12);//如果预约编号空
		}
		chargingCacheObj.setBespNo(bespNo);
		byte[] bcdBespNo = WmIce104Util.str2Bcd(bespNo);
		assert(bcdBespNo.length==6);
		
		//logger.info("EventConstant.EVENT_CHARGE,source:{},actionIdentity{}",source,actionIdentity);
		
		if(ImitateEpService.IsStart())
			ImitateEpService.startCharge(epCode, epGunNo, chargeUser.getAccount());
		
		else
		{
			Date date = new Date();
			String chOrCode = (int)(date.getTime()/1000) + curUserAccount;
			
			ChargeEvent chargeEvent = 
					new ChargeEvent(epCode,epGunNo,transactionNumber,0,(int)(date.getTime()/1000),0,1,0);
			
			BigDecimal bdFronzeAmt = UtilProtocol.intToBigDecimal2(chargeCache.getFronzeAmt());
			
			EpGunService.updateChargeInfoToDbByEpCode(this.currentType,this.epCode,this.epGunNo,
					new BigDecimal(this.realChargeInfo.getChargedMeterNum()).multiply(Global.Dec3),chargeEvent.getSerialNo(),bdFronzeAmt,0,this.curUserId);
	
			
			chargeCache.setChOrCode(chOrCode);
			
			
			UserCache memUserInfo= UserService.getUserCache(curUserAccount);
			UserRealInfo realUserInfo= UserService.findUserRealInfo(curUserAccount);
			
			if(realUserInfo!=null && memUserInfo != null && memUserInfo.getCard() !=null)
			{
				logger.debug("memUserInfo.getCard().getUserId():{}",memUserInfo.getCard().getId());
			   chargeCache.setPkUserCard(memUserInfo.getCard().getId());
			}
			int chorType= EpChargeService.getOrType(memUserInfo.getLevel());
			
			String bespokeNo= StringUtil.repeat("0", 12);
			if(this.bespCache!=null)
			{
				bespokeNo =this.bespCache.getBespNo();
			}
		
				
			EpChargeService.insertChargeRecordToDb(curUserId, chorType,curUserAccount,chargeCache.getPkUserCard(),orgNo, 
			pkEpId, epCode, epGunNo, this.chargingMethod,bespokeNo, chOrCode, 
			chargeCache.getFronzeAmt(),payMode,orgNo,chargeEvent, this.chargeCache.getRateInfo(),4);
	
			logger.info("do_start_electricize serialNo:{},nFronzeAmt:{},epCode:{},epGunNo:{},account:{}"
			,new Object[]{chargeCache.getChargeSerialNo(),frozenAmt,epCode,epGunNo,chargingAccout});
			
			
			byte[] data = EpEncodeProtocol.do_start_electricize(epCode, (byte)epGunNo, 
					chargingAccout, 0, (byte)chargeStyle,frozenAmt,1,realUserInfo.getPassword(),chargeCache.getChargeSerialNo());
			
			if(data==null)
			{
				logger.error("startCharge do_start_electricize error!epGunNo:{},account:{}",epCode+epGunNo,chargingAccout);
			}
			
			//命令加时标
			String messagekey = String.format("%03d%s", Iec104Constant.C_START_ELECTRICIZE,chargeCache.getChargeSerialNo());
			
			// byte[] cmdTimes = WmIce104Util.timeToByte();
			
			EpMessageSender.sendRepeatMessage(epCommClient,messagekey,0,0,Iec104Constant.C_START_ELECTRICIZE, data,cmdTimes,epCommClient.getCommVersion());	
		}
		
		return 0;
	}
	
	public int stopChargeAction(int userId,int source,String actionIdentity)
	{
		EpCommClient commClient= (EpCommClient)getEpNetObject();
		if(commClient ==null || !commClient.isComm())
		{
			return EpConstantErrorCode.EP_UNCONNECTED;//
		}
		
		ChargeCache chargeCacheObj = getChargeCache();
		//没有在充电，不能结束充电
		if(chargeCacheObj==null )
		{
			logger.debug("stopChargeAction:chargeCacheObj==null");
			return  EpConstantErrorCode.EPE_NOT_ENABLE_STOP_WITHOUT_CHARGING;//
		}
		//不是充电的用户不能结束充电  
		if(chargeCacheObj.getUserId() != userId) 
		{
			logger.debug("stopChargeAction:chargeCacheObj.getUserId() != userId");
			return  EpConstantErrorCode.EPE_NOT_ENABLE_STOP_WITHOUT_CHARGING;
		}
		if(this.status != EpConstant.EP_GUN_STATUS_CHARGE )
		{
			logger.debug("stopChargeAction:this.status != EpConstant.EP_GUN_STATUS_CHARGE");
			return  EpConstantErrorCode.EPE_NOT_ENABLE_STOP_WITHOUT_CHARGING;//
		}
        
		byte bcdqno = (byte)epGunNo;
		
		java.util.Date dt = new Date(); // hly
		long now = dt.getTime() / 1000;
		
		//this.curAction = EpConstant.USER_ACTION_STTOP_CHARGE;
		//this.curActionOccurTime = now;
		
		if(ImitateEpService.IsStart())
		{
			ImitateEpService.ImitateStopCharge(this.epCode,this.epGunNo , this.curUserAccount, chargeCacheObj);
		}
		else
		{
			byte[] data= EpEncodeProtocol.do_stop_electricize(epCode, bcdqno);
			if(data == null)
			{
				logger.error("stopCharge do_stop_electricize exception!epCode:{},epGunNo:{}",epCode,epGunNo);
				return EpConstantErrorCode.EP_PACK_ERROR;
				
			}
			String msg=MessageFormat.format("do_stop_electricize,epCode:{0},epGunNo:{1},account:{2}",
					this.epCode,this.epGunNo,this.curUserAccount);
			logger.info(msg);
			byte[] cmdTimes = WmIce104Util.timeToByte();
			
			EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_STOP_ELECTRICIZE, data,cmdTimes,commClient.getCommVersion());
			
		}
		
		return 0;
	}
	
	
	public int startBespokeAction(UserCache userInfo,RateInfo rateInfo,int redo,int secBuyOutTime,String bespNo,
			int payMode,int orgNo,int cmdFromSource,String cmdIdentily)
	{	
		//1.充电桩未连接不能充电
		EpCommClient commClient = (EpCommClient)getEpNetObject();
		if(commClient==null || commClient.isComm()==false) {
			
			return EpConstantErrorCode.EP_UNCONNECTED;//
		}
		
		if( redo == 1 )
		{
			// 11.这个枪没有预约不能续约
			if(status != EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
			{
				
				return EpConstantErrorCode.BESP_NO_NOT_EXIST;
			}
			if (this.bespCache.getAccountId() != userInfo.getId()) {
				return EpConstantErrorCode.NOT_SELF_REDO_BESP;//
			}
		}
		if(redo == 0)
		{
			if(status ==EpConstant.EP_GUN_STATUS_EP_OPER)//用户使用状态，允许使用用户预约
			{
				if(this.curUserId!=0 && this.curUserId != userInfo.getId())
				{
					return EpConstantErrorCode.CAN_NOT_BESP_IN_BESP_COOLING;
				}
				return EpConstantErrorCode.USED_GUN;//
			}
			if(status == EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
				return EpConstantErrorCode.EPE_OTHER_BESP;//
			
			if(this.status == EpConstant.EP_GUN_STATUS_SETTING)
			 {
				return EpConstantErrorCode.EPE_IN_EP_OPER;
			 }
			if(this.status == EpConstant.EP_GUN_STATUS_SELECT_CHARGE_MODE)
			 {
				return EpConstantErrorCode.EPE_IN_EP_OPER;
			 }
			
		}
		if(status == EpConstant.EP_GUN_STATUS_IDLE )
		{
			status = EpConstant.EP_GUN_STATUS_EP_OPER;
		}
				
		if (redo == 1) {
			long bespTotalTime = this.bespCache.getEndTime() + secBuyOutTime
					- bespCache.getStartTime();
			if (bespTotalTime > (6 * 3600)) {
				return EpConstantErrorCode.BESP_TO_MAX_TIME;//
			}
		}
		

		BigDecimal fronzingAmt = RateService.calcBespAmt(rateInfo.getBespokeRate(),secBuyOutTime / 60);

		int userId= userInfo.getId();
		BigDecimal userRemainAmt= UserService.getRemainBalance(userId);
		logger.info("before startBespokeAction userId:{},payMode:{},remainAmt:{}",
				new Object[]{userId,payMode,userRemainAmt.doubleValue()});
		
		setUserInfo(userInfo.getId(), userInfo.getAccount());

		// 12.钱不够不能充电
		if (payMode == EpConstant.P_M_FIRST && userRemainAmt.compareTo(fronzingAmt)<0) {
			logger.error("startBespoke fail ,not enough! fronzing amt:{},userRemainAmt:{},curUserAccount{},bespNo{},epCode{},epGunNo{}"
					,new Object[]{fronzingAmt.doubleValue(),userRemainAmt,this.curUserAccount,bespNo,this.epCode,this.epGunNo});
			
			return EpConstantErrorCode.EPE_NO_ENOUGH_MONEY;//
		}
		if (redo ==1 && this.bespCache.getPayMode() == EpConstant.P_M_FIRST && userRemainAmt.compareTo(fronzingAmt)<0) {
			logger.error("startBespoke fail ,not enough! fronzing amt:{},userRemainAmt:{},curUserAccount{},bespNo{},epCode{},epGunNo{}"
					,new Object[]{fronzingAmt.doubleValue(),userRemainAmt,this.curUserAccount,bespNo,this.epCode,this.epGunNo});
			
			return EpConstantErrorCode.EPE_NO_ENOUGH_MONEY;//
		}
		

		byte bcdqno = (byte) epGunNo;
		byte bredo = (byte) redo;
		byte[] start_time = WmIce104Util.getP56Time2a();

		// todo:20150803
		String CardNo = new String("1234567891234567");// 充电卡号
		String CarCardNo = new String("1234567891234567");// 车牌号
		
		//String bespNoMd5 = Md5Encoder.encodeByMD5(bespNo);

		java.util.Date dt = new Date();
		long bespSt = dt.getTime() / 1000;

		if (redo == 0) {
			BespCache bespCacheObj = new BespCache();
			//this.curAction = EventConstant.EVENT_BESPOKE;
			//this.curActionOccurTime = DateUtil.getCurrentSeconds();
			this.curUserAccount = userInfo.getAccount();
			bespCacheObj.setAccount(userInfo.getAccount());
			bespCacheObj.setBespNo(bespNo);

			//bespCacheObj.setBespNoMd5(bespNoMd5);
			
			long et = bespSt + (long) (secBuyOutTime);
			
			bespCacheObj.setStartTime(bespSt);
			bespCacheObj.setEndTime(et);
			bespCacheObj.setRealEndTime(et);

			bespCacheObj.setAccountId(userInfo.getId());
			bespCacheObj.setRate(rateInfo.getBespokeRate());
			bespCacheObj.setStatus(EpConstant.BESPOKE_STATUS_CONFIRM);
			
			bespCacheObj.setPayMode(payMode);
			UserOrigin userOrigin = new UserOrigin(orgNo,cmdFromSource,cmdIdentily);
			
			bespCacheObj.setUserOrigin(userOrigin);
		
			this.bespCache = bespCacheObj;
			
		}
		bespCache.setBuyTimes(secBuyOutTime / 60);
		
		bespCache.setRedo((short)redo);
	
		if(ImitateEpService.IsStart())
			ImitateEpService.ImitateBespoke(this.epCode, this.epGunNo, redo, bespNo);
		
		else
		{
			byte[] bcdAccountNo = WmIce104Util.str2Bcd(userInfo.getAccount());
		
			byte[] orderdata = EpEncodeProtocol.do_bespoke(
					WmIce104Util.str2Bcd(epCode), bespNo, bcdqno, bredo,
					bcdAccountNo, WmIce104Util.str2Bcd(CardNo), start_time,
					(short)(secBuyOutTime/60), StringUtil.repeat("0", 16).getBytes());
	
			byte[] cmdTimes = WmIce104Util.timeToByte();
			EpMessageSender.sendMessage(commClient,  0, 0,Iec104Constant.C_BESPOKE, orderdata,cmdTimes,commClient.getCommVersion());
			String msg=MessageFormat.format("startBespoke fronzing amt:{0},bredo:{1},curUserAccount{2},bespNo{3},epCode{4},epGunNo{5}"
					,fronzingAmt.doubleValue(),bredo,this.curUserAccount,bespNo,this.epCode,this.epGunNo);
		    logger.info(msg);
		}					
		return 0;
		
	}
	public int stopBespokeAction(int source,String srcIdentity ,String bespno)
	{
		//this.curAction = EventConstant.EVENT_CANNEL_BESPOKE;
		//this.curActionOccurTime = DateUtil.getCurrentSeconds();
		
		if(ImitateEpService.IsStart())
			ImitateEpService.ImitateCancelBespoke(srcIdentity, source, bespno);
		
		else
		{
			byte[] sendMsg = EpEncodeProtocol.do_cancel_bespoke(epCode, this.epGunNo,bespno);
			
			byte[] cmdTimes = WmIce104Util.timeToByte();
			EpCommClient commClient = (EpCommClient)this.epNetObject;
			EpMessageSender.sendMessage(commClient, 0, 0, Iec104Constant.C_CANCEL_BESPOKE, sendMsg,cmdTimes,commClient.getCommVersion());
			logger.info("stopBespoke curUserAccount:{},bespNo:{},epCode:{},epGunNo:{}"
					,new Object[]{this.bespCache.getAccount(),bespno,this.epCode,this.epGunNo});
		  
		}
	
		return 0;
	}
	
	
	public void cleanChargeInfoInRealData()
	{
		try
		{
		BigDecimal bdZero = new BigDecimal(0.0);
		EpGunService.updateChargeInfoToDbByEpCode(this.currentType,this.epCode,this.epGunNo,
				bdZero,"",bdZero,0,0);
		}
		catch(Exception e)
		{
			logger.error("cleanChargeInfoInRealData,getStackTrace:{}",e.getStackTrace());
			
		}
	
	}
	public UserCache getChargeUser(int userIdInCache,String userAccountInCache,int chargeStyle,String account)
	{
		int userId =  getCurChargeUserId();
		
		UserCache u = null;
		if(userIdInCache==0)
		{
			
			if(chargeStyle == 1)
			{
				u= UserService.getUserCache(account);
			}
			else
			{
				u=UserService.getCardUser(account);
			}
		}
		else
		{
			u= UserService.getUserCache(userId);
			if(u == null)
			{
			//	logger.info("SSSSSSSSSSSSSSSSSSSSSSSss,curUserId:{},curUserAccount:{}",curUserId,curUserAccount);
				
				u= UserService.getUserCache(userAccountInCache);
			}
		}
		
		return u;
	}
	
	public NoCardConsumeRecord statChargeEvent(int userId,NoCardConsumeRecord consumeRecord)
	{
		consumeRecord.setStatRet(0);
		
		int chargeTime = (int)((consumeRecord.getEndTime() - consumeRecord.getStartTime())/60);
		if(chargeTime<0 || chargeTime> 1440)
		{
			consumeRecord.setStatRet(-1);
		}
		consumeRecord.setChargeUseTimes(chargeTime);
		if(consumeRecord.getTotalDl()>EpConstant.MAX_CHARGE_METER_NUM  || consumeRecord.getTotalDl()<0 )
		{
			consumeRecord.setStatRet(-2);
		}
		return consumeRecord;
		
	}
	
	
	public NoCardConsumeRecord statChargeAmt(int userId,int fronzeAmt,int payMode,NoCardConsumeRecord consumeRecord)
	{
		consumeRecord.setStatAmtRet(0);
		int chargeAmt = consumeRecord.getTotalChargeAmt();
		int serviceAmt = consumeRecord.getServiceAmt();
		String chargeSerialNo = consumeRecord.getSerialNo();
		if(chargeAmt<0 || chargeAmt>EpConstant.MAX_CHARGE_AMT)
		{
			logger.error("statCharge invalid,chargeAmt:{}",chargeAmt);
			consumeRecord.setStatAmtRet(-1);
			return consumeRecord;
		}
		if(serviceAmt<0 || serviceAmt>EpConstant.MAX_CHARGE_SERVICE_AMT)
		{
			logger.error("statCharge invalid,serviceAmt:{}",serviceAmt);
			consumeRecord.setStatAmtRet(-2);
			return consumeRecord;
		}
		logger.info("statCharge.userId:{},chargeAmt:{},serviceAmt:{}",new Object[]{userId,chargeAmt,serviceAmt});
		int consumeAmt = chargeAmt+serviceAmt;
		if(consumeAmt<0 || consumeAmt>EpConstant.MAX_CHARGE_COST)
		{
			logger.error("statCharge invalid,consumeAmt:{}",consumeAmt);
			consumeRecord.setStatAmtRet(-3);
			return consumeRecord;
		}
		int remainAmt=0;
		int chargeCost=0;
		if(payMode== EpConstant.P_M_FIRST)
		{
			if(fronzeAmt< consumeAmt)
			{
				logger.error("statCharge error!fronzeAmt:{} < totalConsumeAmt:{},diff:{}",new Object[]{fronzeAmt,consumeAmt,(consumeAmt-fronzeAmt)});	
				chargeCost= fronzeAmt;
			}
			else
			{
				remainAmt = fronzeAmt- consumeAmt;
				chargeCost = consumeAmt;
			}
		}
		else
		{
			chargeCost= consumeAmt;
			remainAmt=0;
		}
		
		
		consumeRecord.setServiceAmt(serviceAmt);
		consumeRecord.setTotalAmt(chargeCost);
				
		//5.先付费的结算资金
		if(payMode== EpConstant.P_M_FIRST)
		{
			RateService.addPurchaseHistoryToDB(UtilProtocol.intToBigDecimal2(chargeCost),1,userId,0,"充电消费",epCode,chargeSerialNo,"");
			
			logger.info("statCharge.userId:{},remainAmt:{}",userId,remainAmt);
			
			if(remainAmt>0)
			{
				BigDecimal bdRemainAmt= UtilProtocol.intToBigDecimal2(remainAmt);
				UserService.addAmt(userId, bdRemainAmt);
			}
		}
		return consumeRecord;
		
	}
	
	
	
	
	/**
	 *
	 * @param consumeRecord
	 * @return  4：无效的交易流水号
	 * 			3:已经处理
				2:数据不存在
				1:处理成功
	 */
	public int endChargeWithConsumeRecord(NoCardConsumeRecord consumeRecord)
	{	
		logger.debug("endChargeWithConsumeRecord,curUserAccount:{},consumeRecord:{}",this.curUserAccount,consumeRecord);
	   try
		{
		   
		   String consumeSerialNo = consumeRecord.getSerialNo();
		 
		   //2.检查是否有充电记录
		   ChargeCache chargeCacheObj = getChargeCache();
			if(chargeCacheObj==null)//如果内存中，没有那么从数据库中插找
			{
				//先查数据库订单状态
				int orderStatus = EpChargeService.getChargeOrderStatus(consumeRecord.getSerialNo());
				logger.debug("serialNo,{},orderStatus:{}",consumeRecord.getSerialNo(),orderStatus);
				if(orderStatus==-1)
				{
					return 2;
				}
				if(orderStatus==2|| orderStatus==3)//
					return 3;
				chargeCacheObj = EpChargeService.GetChargeCacheFromDb(consumeRecord.getSerialNo());
				if(chargeCacheObj ==null)//充电记录没有
					return 2;
			}
			
			
			String chargeSerialNo = chargeCacheObj.getChargeSerialNo();
			
			//3.和最后的充电流水号不一致，认为不存在
			if(chargeSerialNo.compareTo(consumeSerialNo)!=0)
			{
				logger.error("endChargeWithConsumeRecord error!chargeSerialNo:{},consumeSerialNo:{}",chargeSerialNo,consumeSerialNo);
				return 2;
			}
		
			//4.如果已经结算,那么立即还回
			if(chargeCacheObj.getStatus() >= ChargeStatus.CS_STAT)
			{
				logger.info("endChargeWithConsumeRecord had been end,epGunNo:{}:chargeSerialNo:{}",epCode+epGunNo,chargeCacheObj.getChargeSerialNo());
				return 3;
			}
			UserCache chargeUser =  getChargeUser(chargeCacheObj.getUserId(),this.curUserAccount,chargeCacheObj.getStartChargeStyle(),consumeRecord.getEpUserAccount());
			if(chargeUser==null)
			{
				return -5;
			}
			int chargeUserId= chargeUser.getId();
			String chargeUserAccount = chargeUser.getAccount();
			chargeCacheObj.setEt(consumeRecord.getEndTime());
			//1.结算钱
			consumeRecord = statChargeAmt(chargeUserId,chargeCacheObj.getFronzeAmt(),
					chargeCacheObj.getPayMode(),consumeRecord);
				
			
			chargeCacheObj.setStatus(ChargeStatus.CS_STAT);
			//2.检查度数和时间
			consumeRecord = statChargeEvent(chargeUserId,consumeRecord);
			//3.记录到数据库
			if(consumeRecord.getStatAmtRet()>=0 && consumeRecord.getStatRet()>=0)
			{
				int totalAmt = consumeRecord.getTotalAmt();
				//记录正常数据
				EpChargeService.updateChargeToDb(pkEpId,epCode,chargeCacheObj.getPayMode(),
						chargeCacheObj.getAccount(),chargeCacheObj.getUserId(), 
						epGunNo,chargeCacheObj.getRateInfo(),chargeCacheObj.getChOrCode(), 
						consumeRecord,false);
				//记录充电统计
				logger.info("endChargeWithConsumeRecord addChargeStat,epCode:{},epGunNo:{},SerialNo:{},TotalDl:{},getChargeUseTimes:{}",
						new Object[]{consumeRecord.getEpCode(),consumeRecord.getEpGunNo(),consumeRecord.getSerialNo(),
						consumeRecord.getTotalDl(),consumeRecord.getChargeUseTimes()});
				EpChargeService.addChargeStat(this.pkEpGunId,consumeRecord.getTotalDl(),consumeRecord.getChargeUseTimes(),totalAmt);
			    //送充电记录数据到停车办
				if(StopCarOrganService.getValid()!=0)
				{
					BigDecimal total = UtilProtocol.intToBigDecimal3(consumeRecord.getTotalDl());
					
				     StopCarOrganService.chargeRecord(consumeRecord.getEpCode(),
						consumeRecord.getEpGunNo(),total,consumeRecord.getStartTime(),
						consumeRecord.getEndTime());
				}
			}
			else
			{
				logger.error("statChargeEvent error,userId:{},epCode:{},epGunNo:{},meterNum:{},chargeTime:{}", 
						new Object[]{chargeUserId,consumeRecord.getEpCode(),consumeRecord.getEpGunNo(),
						consumeRecord.getTotalDl(),consumeRecord.getChargeUseTimes()});
				//记录异常订单
				EpChargeService.updateChargeToDb(pkEpId,epCode,chargeCacheObj.getPayMode(),
						chargeCacheObj.getAccount(),chargeCacheObj.getUserId(), 
						epGunNo,chargeCacheObj.getRateInfo(),chargeCacheObj.getChOrCode(), 
						consumeRecord,true);
				
			}
			//4.故障记录到故障表
			EpChargeService.insertFaultRecord((int)consumeRecord.getStopCause(),epCode,this.pkEpId,epGunNo,consumeRecord.getSerialNo(),new Date(consumeRecord.getEndTime()*1000));
			//5.非主动停止充电,给用户发短信
			if(consumeRecord.getStopCause() >2 && chargeUserAccount!=null)
			{
				this.onChargeNotice(consumeRecord.getStopCause(),chargeUserAccount);
			}
			//6.清除实时数据信息
			cleanChargeInfoInRealData();
			//7.给前段发消息
			if(PhoneService.isComm(chargeCacheObj.getUserOrigin()))
			{
				logger.debug("endChargeWithConsumeRecord send phone,epCode:{},epGunNo:{}, chargeUserId:{}",new Object[]{epCode,epGunNo, chargeUserId});
				
				
				String chOrCode = chargeCacheObj.getChOrCode();
				Map<String, Object> consumeRecordMap = new ConcurrentHashMap<String, Object>();
				consumeRecordMap.put("userId",chargeUserId);
				consumeRecordMap.put("orderid",chOrCode);
				consumeRecordMap.put("st",consumeRecord.getStartTime());
				consumeRecordMap.put("et",consumeRecord.getEndTime());
				consumeRecordMap.put("totalMeterNum",consumeRecord.getTotalDl());
				consumeRecordMap.put("totalAmt",consumeRecord.getTotalChargeAmt());
				consumeRecordMap.put("serviceAmt",consumeRecord.getServiceAmt());
				consumeRecordMap.put("pkEpId",getPkEpId());
				consumeRecordMap.put("epCode",getEpCode());
				consumeRecordMap.put("chargStartEnergy",consumeRecord.getStartMeterNum());
				consumeRecordMap.put("chargEndEnergy",consumeRecord.getEndMeterNum());
				consumeRecordMap.put("account",consumeRecord.getEpUserAccount());
				consumeRecordMap.put("serialNo",consumeRecord.getSerialNo());
				
				handleEvent(EventConstant.EVENT_CONSUME_RECORD,0,0,null,(Object)consumeRecordMap);
			}
			else
			{
				logger.debug("endChargeWithConsumeRecord sendStopChargeByPhoneDisconnect,epCode:{},epGunNo:{}, chargeUserId:{}",new Object[]{epCode,epGunNo, chargeUserId});
				
					
				AppApiService.sendStopChargeByPhoneDisconnect(epCode,epGunNo, chargeUserId,1,0,
						consumeRecord.getChargeUseTimes());
			}
			
			//8.清空掉充电缓存对象
			cleanChargeInfo();
		
			return 1;
		}
		catch (Exception e) {
			logger.info(e.getMessage());
			return 0;
		}
		
	}
	
	
	
	
	
	
	/***
	 * 1:取消预约,2：预约到期，3：充电开始
	 */
	public void endBespoke(int style)
	{
		logger.debug("endBespoke,epCode:{},epGunNo:{}style:{}",new Object[]{epCode,epGunNo,style});
		BespCache bespCacheObj = getBespCache();
		if(bespCacheObj == null)
		{
			logger.info("endBespoke fail!bespCacheObj ==null",bespCacheObj);
			return ;
		}
        logger.debug("endBespoke bespCacheObj:{}",bespCacheObj);
		if( bespCacheObj.getStatus() != EpConstant.BESPOKE_STATUS_LOCK)
		{
			logger.info("endBespoke fail!bespCacheObj:{}",bespCacheObj);
			return ;	
		}
		Date now = new Date();
		bespCacheObj.setEndTime(now.getTime()/1000);
		bespCacheObj.setRealEndTime(now.getTime()/1000);
		
		BigDecimal realBespAmt = EpBespokeService.statBespoke(bespCacheObj);
		
		EpBespokeService.endBespoke(epCode, realBespAmt, bespCacheObj, now);
		
		StatService.subBespoke();
	
		//5.给前端应答
		Map<String, Object> bespokeMap = new ConcurrentHashMap<String, Object>();

		bespokeMap.put("userId",this.curUserId);
		bespokeMap.put("bespNo",bespCache.getBespNo());
		
		bespokeMap.put("amt",realBespAmt.doubleValue());
		
		BigDecimal curUserBalance = UserService.getRemainBalance(bespCacheObj.getAccountId());
		bespokeMap.put("remainAmt",curUserBalance.doubleValue());
		
		bespokeMap.put("account",this.curUserAccount);
		
		bespokeMap.put("epCode",epCode);
		bespokeMap.put("epGunNo",epGunNo);
		
		bespokeMap.put("st",bespCacheObj.getStartTime());
		bespokeMap.put("et",bespCacheObj.getEndTime());
		bespokeMap.put("style",style);
		
		handleEvent(EventConstant.EVENT_CANNEL_BESPOKE, 1, 0, null, bespokeMap);
		
		String msg = MessageFormat.format("endBespoke success,BespNo:{0},account:{1},realBespAmt:{2},remainAmt:{3},epcode:{4},gunno:{5}",
				bespCache.getBespNo(),bespCache.getAccount(),realBespAmt.doubleValue(),curUserBalance.doubleValue(),this.epCode,this.epGunNo);
		
		logger.info(msg);
		
		cleanBespokeInfo();
	}

	public boolean init(ElectricPileCache epCache,int epGunNo)
	{
		String epCode = epCache.getCode();
		int currentType = epCache.getCurrentType();
	
		if(currentType!=EpConstant.EP_DC_TYPE && currentType!= EpConstant.EP_AC_TYPE)
		{
			logger.error("init error!invalid current type:{}",currentType);
			return false;
		}
		
		this.currentType = currentType;
		TblElectricPileGun tblEpGun= EpGunService.getDbEpGun(pkEpId,epGunNo);
		if(tblEpGun==null)
		{
			logger.error("init error!did not find gun,pkEpId:{},epGunNo:{}",pkEpId,epGunNo);
			return false;
		}
		
		this.chargeCache = null;
		this.bespCache = null;
	
		this.setPkEpGunId(tblEpGun.getPkEpGunId());
		
		this.concentratorId = epCache.getConcentratorId();
		this.identyCode = tblEpGun.getQr_codes();
		this.createIdentyCodeTime = tblEpGun.getQrdate()-EpConstant.IDENTYCODE_INVALID_TIME2;
		
		
		//1.初始化实时数据
		RealChargeInfo tmpRealChargeInfo =null;
		if(currentType == EpConstant.EP_DC_TYPE)
		{
			RealDCChargeInfo chargeInfo = new RealDCChargeInfo();
			tmpRealChargeInfo = chargeInfo;
		}
		else
		{
			RealACChargeInfo chargeInfo = new RealACChargeInfo();
			tmpRealChargeInfo = chargeInfo;
		}
		tmpRealChargeInfo.init();
		tmpRealChargeInfo.setCurrentType(currentType);
		tmpRealChargeInfo.setEpCode(epCode);
		tmpRealChargeInfo.setEpGunNo(epGunNo);
		//1.装载实时数据
		boolean loadSuccess = tmpRealChargeInfo.loadFromDb(epCode, epGunNo);
		
		if(!loadSuccess)
		{
			if(currentType==EpConstant.EP_DC_TYPE)
			{
				logger.error("init error!did not load in tbl_chargeinfo_dc:{}",epCode, epGunNo);
				return false;
			}
			else
			{
				logger.error("init error!did not load in tbl_chargeinfo_ac:{}",epCode, epGunNo);
				
			}
			return false;
		}
		this.realChargeInfo = tmpRealChargeInfo;
		
		
		int epGunStatusInDb= tblEpGun.getEpState();
		//以数据库最后枪头状态为准
		this.modifyStatus(epGunStatusInDb, false);

		//2.取最新的预约中的预约记录
		BespCache tmpBespCache=null;
		TblBespoke besp = EpBespokeService.getUnStopBespokeFromDb(this.pkEpId, this.pkEpGunId);
		if (besp != null) {
			tmpBespCache = EpBespokeService.makeBespokeCache(besp);
			
			// 检查是否过期,如果过期.那么结算
			long diff  = EpBespokeService.expireTime(tmpBespCache);
		
			if (diff > 0) {
				//结算
				Date now = new Date();
				tmpBespCache.setRealEndTime(now.getTime()/1000);
				if(tmpBespCache.getRealEndTime() > tmpBespCache.getEndTime())
				{
					tmpBespCache.setRealEndTime(tmpBespCache.getEndTime());
				}
				
				BigDecimal realBespAmt = EpBespokeService.statBespoke(tmpBespCache);
				EpBespokeService.endBespoke(epCode, realBespAmt, tmpBespCache, now);
			
				tmpBespCache=null;
			} else {
				tmpBespCache.setStatus(EpConstant.BESPOKE_STATUS_LOCK);
			}
		}
		if(tmpBespCache!=null)
		{
			
			String chargeAccount = tmpBespCache.getAccount();
			//装载未完成充电用户
			UserCache userCache = UserService.getUserCache(chargeAccount);
			
			if(userCache!=null)
			{
				userCache.setUseGun(epCode + epGunNo);
				userCache.setUseGunStaus(EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED);
			}
			this.bespCache = tmpBespCache;
			
		}
		logger.error("init 1");
		//3.取最新的未完成的充电记录
		ChargeCache tmpChargeCache=EpChargeService.GetUnFinishChargeCache(epCode, epGunNo);
		if(tmpChargeCache!=null)
		{
			logger.debug("tmpChargeCache.getStatus():{}",tmpChargeCache.getStatus());
			logger.error("init 2");
			String chargeAccount = tmpChargeCache.getAccount();
			//装载未完成充电用户
			UserCache userCache = UserService.getUserCache(chargeAccount);
			
			logger.error("init 4");
			if(userCache!=null)
			{
				logger.error("init 5");
				if(tmpChargeCache.getPkUserCard()>0)
				{
					logger.error("init 6");
					ChargeCardCache cardCache=UserService.getChargeCardCache(tmpChargeCache.getPkUserCard());
					userCache.setCard(cardCache);
				}
				userCache.setUseGun(epCode + epGunNo);
				userCache.setUseGunStaus(EpConstant.EP_GUN_STATUS_CHARGE);
			}
			this.chargeCache = tmpChargeCache;
		}
		
		logger.error("init 7");
		if(chargeCache!=null)
		{
			logger.error("init 8");
			this.setUserInfo(chargeCache.getUserId(), chargeCache.getAccount());
		}
		else
		{
			logger.error("init 9");
			if(bespCache!=null)
			{
				logger.error("init 10");
				
				this.setUserInfo(bespCache.getAccountId(),  bespCache.getAccount());
				
			}
		}
		
	
		String msg = MessageFormat.format("gun init status:{0},epcode:{1},gunno:{2}",
    		   status,this.epCode,this.epGunNo);
		logger.info(msg);
		
		return true;
	}


	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		
        sb.append("EpGunCache\n");
        sb.append("集中器pkId = ").append(this.getConcentratorId()).append("\n");
        sb.append("电桩pkId = ").append(this.getPkEpId()).append("\n");
        sb.append("电桩编号 = ").append(this.getEpCode()).append("\n");
        
        sb.append("枪口pkId = ").append(this.pkEpGunId).append("\n");
        sb.append("枪口编号 = ").append(this.epGunNo).append("\n");
        sb.append("当前用户ID = ").append(curUserId).append("\n");
        sb.append("当前用户账号 = ").append(curUserAccount).append("\n");
        
        sb.append("识别码 = ").append(identyCode).append("\n");
        
        String sTime= DateUtil.StringYourDate(DateUtil.toDate(createIdentyCodeTime*1000));
        sb.append("识别码产生时间 = ").append(sTime).append("\n");
        
        sTime= DateUtil.StringYourDate(DateUtil.toDate(lastUDTime*1000));
        
        sb.append("数据库实时数据更新时间 = ").append(sTime).append("\n");
        
        sTime= DateUtil.StringYourDate(DateUtil.toDate(lastUPTime*1000));
        sb.append("手机充电信息更新时间  = ").append(sTime).append("\n");
        
        EpCommClient commClient =(EpCommClient)this.epNetObject;
        if(commClient == null)
        {
        	sb.append("not find comm client\n");
        }
        else
        {
        	sb.append("\nEpCommClient:").append(commClient.toString()).append("\n");
        }
       
        sb.append("枪口状态 = "+status+"(" ).append( EpGunService.getGunStatusDesc(status)).append( ")\n");
        if(this.realChargeInfo ==null)
        {
        	sb.append("无实时数据\n");
        }
        else
        {
        	sb.append("实时数据工作状态 = ").append(this.realChargeInfo.getWorkingStatus() ).append(this.realChargeInfo.getWorkingStatusDesc()).append("\n\n");
        }
        
        
        if(this.bespCache ==null)
        {
        	sb.append("无预约\n\r\n");
        }
        else
        {
        	sb.append(this.bespCache.toString() ).append("\n");
        }
        
        if(this.chargeCache ==null)
        {
        	sb.append("无充电\n\r\n");
        }
        else
        {
        	sb.append(this.chargeCache.toString() ).append("\n");
        }
        
      
        return sb.toString();
	}
	
	public UserOrigin getBespokeUserOrigin()
	{
		if(bespCache==null)
			return null;
		return bespCache.getUserOrigin();
	}
	public UserOrigin getChargeUserOrigin()
	{
		if(chargeCache==null)
			return null;
		return chargeCache.getUserOrigin();
	}
	public UserOrigin getActionUserOrigin(int action)
	{
		UserOrigin userOrigin=null;
	
		switch(action)
		{
		case EventConstant.EVENT_BESPOKE:
			userOrigin = getBespokeUserOrigin();
			break;
		case EventConstant.EVENT_CANNEL_BESPOKE:
			userOrigin = getBespokeUserOrigin();
			break;
		case EventConstant.EVENT_CHARGE:
			userOrigin = getChargeUserOrigin();
			break;
		case EventConstant.EVENT_STOP_CHARGE:
			userOrigin = getChargeUserOrigin();
			break;
		case EventConstant.EVENT_START_CHARGE_EVENT:
			userOrigin = getChargeUserOrigin();
			break;
		case EventConstant.EVENT_REAL_CHARGING:
			userOrigin = getChargeUserOrigin();
			break;
		
		case EventConstant.EVENT_CARD_AUTH:
			userOrigin = getChargeUserOrigin();
			break;
		case EventConstant.EVENT_EP_STATUS:
			userOrigin = getChargeUserOrigin();
			break;
		case EventConstant.EVENT_CONSUME_RECORD:
			userOrigin = getChargeUserOrigin();
			break;
		
		case EventConstant.EVENT_EP_STAT:
			userOrigin = getChargeUserOrigin();
			break;
		case EventConstant.EVENT_EP_NET_STATUS:
			userOrigin = getChargeUserOrigin();
			break;
		
		default:
			break;
		}
		return userOrigin;
	}
	public int getDefaultUserOrigin(int action)
	{
		int userOrigin=-1;
	
		switch(action)
		{
		case EventConstant.EVENT_BESPOKE:
			
			break;
		case EventConstant.EVENT_CANNEL_BESPOKE:
			userOrigin = 0;//取消预约默认
			break;
		case EventConstant.EVENT_CHARGE:
			
			break;
		case EventConstant.EVENT_STOP_CHARGE:
			
			break;
		case EventConstant.EVENT_START_CHARGE_EVENT:
			
			break;
		case EventConstant.EVENT_REAL_CHARGING:
			
			break;
		
		case EventConstant.EVENT_CARD_AUTH:
			
			break;
		case EventConstant.EVENT_EP_STATUS:
			break;
		case EventConstant.EVENT_CONSUME_RECORD:
			userOrigin=0;
			break;
		
		case EventConstant.EVENT_EP_STAT:
			
			break;
		case EventConstant.EVENT_EP_NET_STATUS:
			
			break;
		case EventConstant.EVENT_ONE_BIT_YX:
		case EventConstant.EVENT_TWO_BIT_YX:
		case EventConstant.EVENT_YC:
		case EventConstant.EVENT_VAR_YC:
		
			userOrigin= UserConstant.CMD_FROM_MONTIOR;
			break;
		default:
			break;
		}
		return userOrigin;
	}
	public void onEvent(int action,int source,UserOrigin userOrigin,int ret,int cause,Object w,Object extraData)
	{
		switch(source)
		{
		case UserConstant.CMD_FROM_API://app api
			AppApiService.onEvent(action,userOrigin,ret,cause,w,extraData);
			break;
		case UserConstant.CMD_FROM_PHONE://phone client
			
			PhoneService.onEvent(action,userOrigin,ret,cause,w,extraData);
			break;
		case UserConstant.CMD_FROM_MONTIOR://phone client
			AnalyzeService.onEvent(action,userOrigin,ret,cause,w,extraData);
		case UserConstant.ORG_PARTNER_MOBILE:
			ChinaMobileService.onEvent(action, userOrigin,ret,cause,w,extraData);
			break;
		
		default:
			logger.error("onEvent error source:{}",source);
			break;
				
		}
		
	}
	public void handleEvent(int action,int ret,int cause,Object w,Object extraData)
	{
		logger.debug("handle Event action"+action);
		UserOrigin userOrigin = getActionUserOrigin(action);
		
		logger.debug("handle Event userOrigin:"+userOrigin);
		if(userOrigin!=null)
		{
			onEvent(action,userOrigin.getCmdFromSource(),userOrigin,ret,cause,w,extraData);
		}
		else
		{
			logger.debug("epcode:{},curUserId:{}",this.epCode,this.curUserId);
		}
		
	}
	
	public void handleEventExtra(int action,int source,int ret,int cause,Object w,Object extraData)
	{
		
		onEvent(action,source,null,ret,cause,w,extraData);
		
	}
	

	public int onEpCancelBespRet(EpCommClient epCommClient, EpCancelBespResp cancelBespResp)
	{
		logger.debug("onEpCancelBespRet,epCommClient:{},cancelBespResp:{}",epCommClient,cancelBespResp);
		
		if(bespCache!=null)
		{
			if(bespCache.getBespNo().compareTo(cancelBespResp.getBespNo()) !=0)
			{
				logger.debug("onEpCancelBespRet bespCache.getBespNo:{},cancelBespResp.getBespNo:{}", bespCache.getBespNo(),cancelBespResp.getBespNo());
				return 0;
			}
			Map<String, Object> bespokeMap = new ConcurrentHashMap<String, Object>();
			bespokeMap.put("userId",this.curUserId);
			//bespokeMap.put("redo",nRedo);
			bespokeMap.put("bespNo",bespCache.getBespNo());
			
			
			if(cancelBespResp.getSuccessFlag() ==0)//取消预约失败
			{	//5.给前端应答
				handleEvent(EventConstant.EVENT_CANNEL_BESPOKE, 0, 0, null, bespokeMap);
			}
			else////取消预约成功
			{
				this.endBespoke( EpConstant.END_BESPOKE_CANCEL);
			}
		}
		else
		{
			logger.debug("onEpCancelBespRet bespCache=null,epCode:{},epGunNo:{}", this.epCode,this.epGunNo);
		}
	
		return 0;
	}
	public int onEpBespokeResp(EpBespResp bespResp)
	{
		if(bespCache==null)
			return 2; //数据不存在

		String epBespokeNo= bespResp.getBespNo();
		String bespokeNo= this.bespCache.getBespNo();
		
		if(bespokeNo.compareTo(epBespokeNo)!=0)
		{
			return 2; //数据不存在
		}
		if(getStatus() == EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED && bespCache.getRedo()!=1)
		{
			return 3;//已经处理
		}
		int nEpRedo = bespResp.getnRedo();
		if(bespCache.getRedo()!=nEpRedo) //下发的预约标识和回复的预约标识不一致
		{
			return 2;//数据不存在
		}
		
		int errorCode=2;
		if(bespResp.getSuccessFlag() == 1)
		{
			errorCode = onEpBespSuccess( bespResp.getBespNo(), bespResp.getnRedo());
		}
		else
		{
			errorCode = onEpBespFail(bespResp.getBespNo(),bespResp.getnRedo());
		}
		return errorCode;
	}
	public int onEpBespSuccess(String bespNo,int nRedo)
	{
		//算钱和时间,并且保存到数据库
		if (nRedo == 0) {
			
			this.onBespokeSuccess(1);
		} else {
			onRedoBespokeSuccess();
		}
		    
		return 1;
	}
	public void do_bespoke_consume_resp(int ret,int cause,int userId,int redo,String bespokeNo)
	{
		//5.给前端应答
		Map<String, Object> bespokeMap = new ConcurrentHashMap<String, Object>();

		bespokeMap.put("userId",userId);
		bespokeMap.put("redo",redo);
		bespokeMap.put("bespNo",bespokeNo);
		
		logger.debug("do_bespoke_consume_resp,EventConstant.EVENT_BESPOKE.ret:{}, cause:{}",ret, cause);
		
		handleEvent(EventConstant.EVENT_BESPOKE, ret, cause, null, bespokeMap);
	}
	public int onEpBespFail(String bespNo,int nRedo)
	{
		logger.debug("onEpBespFail,bespNo:{},epCode:{}",bespNo,epCode);
		int userId = this.getCurBespokeUserId();
		do_bespoke_consume_resp(0,0,userId,nRedo,bespNo);
		
		if (nRedo == 0)// 预约失败
		{
			bespCache.setStatus(EpConstant.BESPOKE_STATUS_FAIL);
			
			this.cleanBespokeInfo();
		}
		
		return 1;
	}
	
	public int dropCarPlaceLockAction()
	{
		EpCommClient commClient= (EpCommClient)getEpNetObject();
		if(commClient ==null || !commClient.isComm())
		{
			logger.debug("dropCarPlaceLockAction commClient is null,epCode:{},epGunNo:{}",this.epCode,this.epGunNo);
			return EpConstantErrorCode.EP_UNCONNECTED;//
		}
		
		if(!commClient.isComm())
		{
			
			return EpConstantErrorCode.EP_UNCONNECTED;
		}
		byte[] data = EpEncodeProtocol.do_drop_carplace_lock(epCode, epGunNo);
		
		byte[] cmdTimes = WmIce104Util.timeToByte();
		EpMessageSender.sendMessage(commClient,0,0, Iec104Constant.C_DROP_CARPLACE_LOCK,data,cmdTimes,commClient.getCommVersion());
		
		return 0;
	}

	/*public void onActionTimeOut(int action,int ret,int errorCode,Object w,Object extraData)
	{
		handleEvent(action,ret,errorCode,w,extraData);
		cleanAction();
	}*/
	
	// 过期,强制结束
	public void forceEndBespoke() 
	{
		BespCache bespokeCache = this.getBespCache();
		if(bespokeCache==null || bespokeCache.getStatus() != EpConstant.BESPOKE_STATUS_LOCK)
			return ;
		
		EpCommClient commClient = (EpCommClient)getEpNetObject();
		if(commClient!=null && commClient.isComm())
		{
			//向桩发取消预约
			byte[] cancelMsg = EpEncodeProtocol.do_cancel_bespoke( epCode, (byte)epGunNo, bespokeCache.getBespNo());
		    byte[] cmdTimes = WmIce104Util.timeToByte();
			EpMessageSender.sendMessage(commClient, (short)0, 0, (byte)Iec104Constant.C_CANCEL_BESPOKE, cancelMsg,cmdTimes,commClient.getCommVersion());
		}
		else
		{
			this.endBespoke( EpConstant.END_BESPOKE_EXPIRE_TIME);
			
		}
	}
	private ChargingInfo calcCharingInfo()
	{
		/*if(chargeCache==null)
		{
			return null;
		}*/
		long now = DateUtil.getCurrentSeconds();
		
		this.lastUPTime= now;
		ChargingInfo charingInfo = new ChargingInfo();
		
		charingInfo.setTotalTime(this.realChargeInfo.getChargedTime());
		charingInfo.setChargeMeterNum(this.realChargeInfo.getChargedMeterNum());
		if(this.chargeCache!=null)
		{
			charingInfo.setFronzeAmt(this.chargeCache.getFronzeAmt());
		}
		else
		{
			charingInfo.setFronzeAmt(0);
		}
		
		
		charingInfo.setOutCurrent(this.realChargeInfo.getOutVoltage());
		charingInfo.setOutVol(this.realChargeInfo.getOutCurrent());
		charingInfo.setChargeAmt(this.realChargeInfo.getChargedCost());
	
		if(currentType == EpConstant.EP_DC_TYPE)
		{
			charingInfo.setRateInfo(realChargeInfo.getChargePrice()/10);//目前交流传的是100
			charingInfo.setSoc(((RealDCChargeInfo)realChargeInfo).getSoc());
		}
		else
		{
			charingInfo.setRateInfo(realChargeInfo.getChargePrice());
			charingInfo.setSoc(0);
		}
		charingInfo.setDeviceStatus(0);
		charingInfo.setWarns(0);
		charingInfo.setWorkStatus(this.status);
		
		return charingInfo;
		
	}
	public void pushFirstRealData()
	{
		ChargingInfo  chargingInfo = calcCharingInfo();
		if(chargingInfo!=null)
		{
			Map<String ,Object> respMap = new ConcurrentHashMap<String, Object>();
			respMap.put("epcode", epCode);
			respMap.put("epgunno", epGunNo);
			
			handleEvent(EventConstant.EVENT_REAL_CHARGING,0,0,respMap,(Object)chargingInfo);
		}
	}
	public void onBespokeExpiringWarn(String address,String userAccount)
	{
		logger.debug("onBespokeExpiringWarn");
		if(this.bespCache!=null && !bespCache.isExpirWarn())
		{
			logger.debug("onBespokeExpiringWarn,userAccount:{}",userAccount);
			bespCache.setExpirWarn(true);
			
			String content = MessageFormat.format("预约即将到期：尊敬的用户，您的预约还剩{0}分钟，请及时前往{1}充电。温馨提示：如逾期，该桩可能会被其他小伙伴占用哦",15, address);
			MobiCommon.sendWanMatMessage(content,userAccount);
			
			
		}
	
	}
	
	
	public  void  onChargeNotice(int stopCause,String curUserAccount)
	{
		logger.debug("onChargeNotice send msg,stopCause:{},curUserAccount:{}",stopCause,curUserAccount);
		String stopChargeDesc= EpChargeService.getStopChargeDesc(stopCause);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		String content = MessageFormat.format("结束充电提醒：尊敬的用户，由于{0}，您的本次充电于{1}结束，请收枪后查看结算信息。", stopChargeDesc,dateFormat.format(new Date()));
		MobiCommon.sendWanMatMessage(content,curUserAccount);
	}
	
	public void dispatchWholeRealToMonitor(int preWorkStatus)
	{
		Map<String ,Object> paramsMap = new ConcurrentHashMap<String, Object>();
		paramsMap.put("epcode", epCode);
		paramsMap.put("epgunno", epGunNo);
		paramsMap.put("currenttype", currentType);
		
		logger.debug("dispatchWholeRealToMonitor");
		Map<Integer, SingleInfo> oneYxRealInfo=null;
		if(currentType== EpConstant.EP_AC_TYPE)
			oneYxRealInfo = ((RealACChargeInfo)realChargeInfo).getWholeOneBitYx();
		else
			oneYxRealInfo = ((RealDCChargeInfo)realChargeInfo).getWholeOneBitYx();
		
		
		 AnalyzeService.onEvent(EventConstant.EVENT_ONE_BIT_YX,null,0,0,(Object)paramsMap,(Object)oneYxRealInfo);
			
		 Map<Integer, SingleInfo> twoYxRealInfo=null;
		 if(currentType== EpConstant.EP_AC_TYPE)
			 twoYxRealInfo = ((RealACChargeInfo)realChargeInfo).getWholeTwoBitYx();
		else
			twoYxRealInfo = ((RealDCChargeInfo)realChargeInfo).getWholeTwoBitYx();
			
		 
		 AnalyzeService.onEvent(EventConstant.EVENT_TWO_BIT_YX,null,0,0,(Object)paramsMap,(Object)twoYxRealInfo);
			
		 Map<Integer, SingleInfo> ycRealInfo=null;
		 if(currentType== EpConstant.EP_AC_TYPE)
			 ycRealInfo = ((RealACChargeInfo)realChargeInfo).getWholeYc();
		else
			ycRealInfo = ((RealDCChargeInfo)realChargeInfo).getWholeYc(preWorkStatus);
			
		
		//遥测
		 AnalyzeService.onEvent(EventConstant.EVENT_YC,null,0,0,(Object)paramsMap,(Object)ycRealInfo);
		 
		 
		 Map<Integer, SingleInfo> varYcRealInfo=null;
		 if(currentType== EpConstant.EP_AC_TYPE)
			 varYcRealInfo = ((RealACChargeInfo)realChargeInfo).getWholeVarYc();
		else
			varYcRealInfo = ((RealDCChargeInfo)realChargeInfo).getWholeVarYc();
		
		 
		AnalyzeService.onEvent(EventConstant.EVENT_VAR_YC,null,0,0,(Object)paramsMap,(Object)varYcRealInfo);
	
		long now = DateUtil.getCurrentSeconds();
		setLastSendToMonitorTime(now);
		
	}
	
	
}
