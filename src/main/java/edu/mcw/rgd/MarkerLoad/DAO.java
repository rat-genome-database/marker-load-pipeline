package edu.mcw.rgd.MarkerLoad;

import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.dao.impl.variants.VariantDAO;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.variants.VariantMapData;
import org.springframework.jdbc.object.BatchSqlUpdate;

import java.sql.Types;
import java.util.Collection;
import java.util.List;

/**
 * Created by llamers on 1/28/2020.
 */
public class DAO {

    private final SSLPDAO sdao = new SSLPDAO();
    private final MapDAO mdao = new MapDAO();
    private final VariantDAO vdao = new VariantDAO();
    private final StrainDAO strainDAO = new StrainDAO();
    private final AssociationDAO adao = new AssociationDAO();
    private final QTLDAO qdao = new QTLDAO();
    private final RGDManagementDAO managementDAO = new RGDManagementDAO();

    public String getConnection(){
        return sdao.getConnectionInfo();
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

    public List<SSLP> getSSLPsByType(String type) throws Exception{
        return sdao.getSSLPsByType(type);
    }

    public List<MapData> getMapData(int rgdId) throws Exception{
        return mdao.getMapData(rgdId);
    }
    public List<MappedSSLP> getMappedSSLPByType(String type) throws Exception{
        return sdao.getActiveMappedSSLPsByType(type);
    }
    public List<VariantMapData> getVariantByPosition(int mapKey, String chr, int start) throws Exception{
        return vdao.getVariantsByPosition(mapKey, chr, start);
    }

    public List<QTL> isMarkerForQTL(int rgdId) throws Exception{
        return qdao.isMarkerFor(rgdId);
    }

    public void updateQtl(QTL q) throws Exception{
        qdao.updateQTL(q);
    }

    public List<Strain> isMarkerForStrain(int rgdId) throws Exception{
        return strainDAO.isMarkerFor(rgdId);
    }

    public void removeStrainAssociation(int strainRgdId, int associationRgdId) throws Exception{
        adao.removeStrainAssociation(strainRgdId, associationRgdId);
    }

    public void insertStrainAssociation(int strainRgdId, int associationRgdId) throws Exception{
        adao.insertStrainAssociation(strainRgdId, associationRgdId);
    }

    public void insertStrainAssociation(Strain2MarkerAssociation assoc) throws Exception {
        adao.insertStrainAssociation(assoc);
    }

    public void insertVariants(Collection<VariantMapData> mapsData)  throws Exception{
        vdao.insertVariants(mapsData);
    }

    public int insertVariantRgdIds(Collection<VariantMapData> md) throws Exception{
        return vdao.insertVariantRgdIds(md);
    }
    public void insertVariantMapData(Collection<VariantMapData> mapsData)  throws Exception{
        vdao.insertVariantMapData(mapsData);
    }

    public void recordIdHistory(int fromRgdId, int toRgdId) throws Exception {
        managementDAO.recordIdHistory(fromRgdId, toRgdId);
    }

    public void retire(List<SSLP> rgdIds) throws Exception{
        for (SSLP s : rgdIds){
            managementDAO.retire(s);
        }
    }

    public void updateVariant(List<VariantMapData> mapsData) throws Exception {
        BatchSqlUpdate sql2 = new BatchSqlUpdate(DataSourceFactory.getInstance().getCarpeNovoDataSource(),
                "update variant set RS_ID=? where RGD_ID=?",
                new int[]{Types.VARCHAR,Types.INTEGER});
        sql2.compile();
        for( VariantMapData v: mapsData) {
            long id = v.getId();
            sql2.update(v.getRsId(),id);
        }
        sql2.flush();
    }

    public List<Strain2MarkerAssociation> getStrain2SslpAssociations(int strainRgdId) throws Exception {
        return adao.getStrain2SslpAssociations(strainRgdId);
    }
}
