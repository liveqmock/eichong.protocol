package com.epcentre.dao;

import java.util.List;

import com.epcentre.model.TblElectricPile;

public interface TblElectricPileDao {
	
	public List<?> getElectricpileForMap(TblElectricPile tblElectricpile);
	
	public List<TblElectricPile> findResultObjectBySpanId(int typeSpanId);
	
	public List<TblElectricPile> findResultObject(String code);
	
	public List<TblElectricPile> getLastUpdate();
	
	public int updateCommStatus(TblElectricPile epClient);
	public int updateAllCommStatus();
	
	public int updateCommStatusByStationId(TblElectricPile epClient);
	
	
	public List<TblElectricPile>  getEpsByStationId(int nStationId);
	public List<TblElectricPile>  getEpsByStatus(int nStationId);
	public int updateRateId(TblElectricPile epClient);

}
