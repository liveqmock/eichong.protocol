package com.epcentre.cache;

import com.epcentre.config.GameConfig;



public class StopCarOrganConfig {
	
	private  String serverIp;
	private  int serverPort;
	private  String baseUrl;
	private  String partnerId;
	private  String partnerKey;
	private  int valid=0;
	private  long sendRealDataCyc=60;//发送实时数据周期
	private  String statusMethod;//实时数据接口
	private  String chargeMethod;//充电记录数据接口

	
	
	public void setBaseUrl() {
		this.baseUrl = "https://"+getServerIp()+":"+getServerPort();
	}
	
	public String getBaseUrl() {
		 
		return baseUrl;
	}
	
	public String getServerIp() {
		return serverIp;
	}
	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}
	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	public String getPartnerId() {
		return partnerId;
	}
	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}
	public String getPartnerKey() {
		return partnerKey;
	}
	public void setPartnerKey(String partnerKey) {
		this.partnerKey = partnerKey;
	}
	public int getValid() {
		return valid;
	}
	public void setValid(int valid) {
		this.valid = valid;
	}
	public long getSendRealDataCyc() {
		return sendRealDataCyc;
	}
	public void setSendRealDataCyc(long sendRealDataCyc) {
		this.sendRealDataCyc = sendRealDataCyc;
	}
	public String getStatusMethod() {
		return statusMethod;
	}
	public void setStatusMethod(String statusMethod) {
		this.statusMethod = statusMethod;
	}
	public String getChargeMethod() {
		return chargeMethod;
	}
	public void setChargeMethod(String chargeMethod) {
		this.chargeMethod = chargeMethod;
	}
	
	
	
	
	
	
	
	
}
