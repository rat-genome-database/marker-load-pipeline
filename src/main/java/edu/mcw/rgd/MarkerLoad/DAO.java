package edu.mcw.rgd.MarkerLoad;

import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.pheno.*;
import edu.mcw.rgd.datamodel.pheno.Sample;

import java.util.List;

/**
 * Created by llamers on 1/28/2020.
 */
public class DAO {

    private final SSLPDAO sdao = new SSLPDAO();
    private final MapDAO mdao = new MapDAO();
    private final RGDManagementDAO managementDAO = new RGDManagementDAO();

    public String getConnection(){
        return sdao.getConnectionInfo();
    }

    public List<Chromosome> getChromosomes(int mapKey) throws Exception{
        return mdao.getChromosomes(mapKey);
    }

    public List<SSLP> getActiveSSLPsByNameOnly(String name) throws Exception{
        return sdao.getActiveSSLPsByName(name,3);
    }

    public List<SSLP> getSSLPs(int rgdId) throws Exception {
        return sdao.getSSLPs(rgdId);
    }

    public RgdId createRgdId(int objectKey, String objectStatus, String notes, int mapKey) throws Exception{
        int speciesKey= SpeciesType.getSpeciesTypeKeyForMap(mapKey);
        return managementDAO.createRgdId(objectKey, objectStatus, notes, speciesKey);
    }

    public int insertSSLP(SSLP s) throws Exception{
        return sdao.insertSSLP(s);
    }

    public void updateSSLP(SSLP s) throws Exception{
        sdao.updateSSLP(s);
    }

    public int insertMapsData(List<MapData> mapDataList) throws Exception{
        return mdao.insertMapData(mapDataList);
    }

    public List<MapData> getMapData(int rgdId, int mapKey) throws Exception{
        return mdao.getMapData(rgdId, mapKey,"Marker Load Pipeline");
    }
}
