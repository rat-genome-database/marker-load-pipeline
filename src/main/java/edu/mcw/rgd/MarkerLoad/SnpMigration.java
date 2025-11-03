package edu.mcw.rgd.MarkerLoad;

import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.variants.VariantMapData;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnpMigration {
    private String version;
    private Integer primaryAssembly;
    protected Logger logger = LogManager.getLogger("migrateStatus");
    protected Logger checkAgain = LogManager.getLogger("checkAgain");
    private DAO dao = new DAO();

    public void run() throws Exception{
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logger.info(getVersion());
        long pipeStart = System.currentTimeMillis();
        logger.info("   Pipeline started at "+sdt.format(new Date(pipeStart))+"\n");
        geneCacheMap = new HashMap<>();
        int existingVars = 0;
        List<SSLP> sslps = new ArrayList<>();
        List<VariantMapData> newVariants = new ArrayList<>();
        List<VariantMapData> updateVars = new ArrayList<>();
        HashMap<Integer, List<Integer>> sslpHistoryMap = new HashMap<>();
        List<Integer> skipIds = new ArrayList<>();
        List<SSLP> list1 = dao.getSSLPsByType("SNP");
        List<SSLP> list2 = dao.getSSLPsByType("SNV");
        sslps.addAll(list1);
        sslps.addAll(list2);

        for (SSLP sslp : sslps){
            try {
                String refNuc = "";
                String varNuc = "";
                String rsId = "";
                // ref and var are in the notes or seq template
                // will be ACGT>ACGT, ACGT/ACGT, [ACGT>ACGT], [ACGT/ACGT] in one of the columns
                if (Utils.isStringEmpty(sslp.getNotes()) && Utils.isStringEmpty(sslp.getTemplateSeq())) {
                    // log in file for re-curation
                    if (!skipIds.contains(sslp.getRgdId())) {
                        checkAgain.info(sslp.getName() + "|" + sslp.getRgdId() + "|" + sslp.getSslpType());
                        skipIds.add(sslp.getRgdId());
                    }
                    continue;
                } else if (!Utils.isStringEmpty(sslp.getNotes()) && Utils.isStringEmpty(sslp.getTemplateSeq())) {
                    // get nucleotide change from notes
                    String[] arr = sslp.getNotes().split("[\\>\\/\\s]");
                    refNuc = arr[0];
                    varNuc = arr[1];
                } else if (Utils.isStringEmpty(sslp.getNotes()) && !Utils.isStringEmpty(sslp.getTemplateSeq())) {
                    // check if there is '[]' and grab innards, otherwise use what is there
                    String templateSeq = sslp.getTemplateSeq();
                    String nucSeq = "";
                    if (templateSeq.contains("[")) {
                        String[] strArray = templateSeq.split("[\\[\\]]", -1);
                        nucSeq = strArray.length == 3 ? strArray[1] : "";
                    } else {
                        nucSeq = templateSeq;
                    }
                    String[] arr = nucSeq.split("[\\>\\/]");
                    refNuc = arr[0];
                    varNuc = arr[1];
                } else { // has something in notes and seq_template
                    // check template if there are '[]', yes then ignore notes
                    String templateSeq = sslp.getTemplateSeq();
                    String nucSeq = "";
                    if (templateSeq.contains("[")) {
                        String[] strArray = templateSeq.split("[\\[\\]]", -1);
                        nucSeq = strArray.length == 3 ? strArray[1] : "";
                        String[] arr = nucSeq.split("[\\>\\/]");
                        refNuc = arr[0];
                        varNuc = arr[1];
                    } else { // no, then check if the nucleotide change is in notes
                        String[] arr = sslp.getNotes().split("[\\>\\/\\s\\;]");
                        refNuc = arr[0];
                        varNuc = arr[1];
                    }

                }

                if (!sslp.getName().startsWith("rs")){
                    try {
                        String subScript = getStringBetweenTwoCharacters(sslp.getName(), ">", "<");
                        if (subScript.startsWith("rs")) {
                            rsId = subScript;
                        }
                    }
                    catch (Exception ignore){}
                }
                else
                    rsId = sslp.getName();

                List<MapData> sslpMapData = dao.getMapData(sslp.getRgdId());
                List<QTL> qtlWithSSLP = dao.isMarkerForQTL(sslp.getRgdId());
                List<Strain> strainWithSSLP = dao.isMarkerForStrain(sslp.getRgdId());
                int mapKey = 0;
                int latestRgdId = 0;
                ArrayList<Integer> varRgdIds = new ArrayList<>();
                for (MapData mappedSslp : sslpMapData) {
                    if (mappedSslp.getMapKey()==15)
                        continue;
                    VariantMapData vmd = new VariantMapData();
                    vmd.setChromosome(mappedSslp.getChromosome());
                    vmd.setStartPos(mappedSslp.getStartPos());
                    vmd.setEndPos(mappedSslp.getStartPos() + 1);
                    vmd.setReferenceNucleotide(refNuc);
                    vmd.setVariantNucleotide(varNuc);
                    vmd.setMapKey(mappedSslp.getMapKey());
                    vmd.setSpeciesTypeKey(sslp.getSpeciesTypeKey());
                    vmd.setVariantType("SNP");
                    if (!Utils.isStringEmpty(rsId))
                        vmd.setRsId(rsId);

                    boolean newVariant = true;
                    List<VariantMapData> vars = dao.getVariantByPosition(vmd.getMapKey(), vmd.getChromosome(), (int) vmd.getStartPos());
                    if (!vars.isEmpty()) {
                        for (VariantMapData v : vars) {
                            if (Utils.stringsAreEqualIgnoreCase(v.getReferenceNucleotide(), vmd.getReferenceNucleotide())
                                    && Utils.stringsAreEqualIgnoreCase(v.getVariantNucleotide(), vmd.getVariantNucleotide())) {

                                if (Utils.isStringEmpty(v.getRsId()) && !Utils.isStringEmpty(rsId)){
                                    v.setRsId(rsId);
                                    updateVars.add(v);
                                }
                                else if (!Utils.stringsAreEqualIgnoreCase(v.getRsId(), rsId)){
                                    logger.info("\t\t Variant rs Id: |"+v.getRsId()+"|"+rsId+"|");
                                }
                                newVariant = false;
                                vmd = v;
                                existingVars++;
                                break;
                            }
                        }
                    }
                    if (newVariant) {
                        RgdId r = dao.createRgdId(RgdId.OBJECT_KEY_VARIANTS, "ACTIVE", "created by Marker Load Pipeline", mappedSslp.getMapKey());
                        vmd.setId(r.getRgdId());
                        String genicStat = isGenic(vmd) ? "GENIC":"INTERGENIC";
                        vmd.setGenicStatus(genicStat);
//                        vmd.setId(12345678);
                        newVariants.add(vmd);
                    }
                    if (mappedSslp.getMapKey()>mapKey){
                        mapKey = mappedSslp.getMapKey();
                        latestRgdId = (int)vmd.getId();
                    }
                    varRgdIds.add((int)vmd.getId());
                }
                sslpHistoryMap.put(sslp.getRgdId(), varRgdIds);
                if (!qtlWithSSLP.isEmpty()){
                    // figure out if flanking or peak marker uses rgdId and update with variant rgdId
                    for (QTL qtl : qtlWithSSLP){
                        logger.info("\t\tQTL having an updated marker: "+qtl.getSymbol()+":"+qtl.getRgdId());
                        if (Utils.intsAreEqual(qtl.getFlank1RgdId(), sslp.getRgdId()) ) {
                            logger.info("\t\t\tOld Flank 1 RGD Id: "+qtl.getFlank1RgdId()+" | New RGD Id: "+latestRgdId);
                            qtl.setFlank1RgdId(latestRgdId);
                        }
                        if (Utils.intsAreEqual(qtl.getFlank2RgdId(), sslp.getRgdId()) ){
                            logger.info("\t\t\tOld Flank 2 RGD Id: "+qtl.getFlank2RgdId()+" | New RGD Id: "+latestRgdId);
                            qtl.setFlank2RgdId(latestRgdId);
                        }
                        if (Utils.intsAreEqual(qtl.getPeakRgdId(), sslp.getRgdId()) ) {
                            logger.info("\t\t\tOld Peak RGD Id: "+qtl.getPeakRgdId()+" | New RGD Id: "+latestRgdId);
                            qtl.setPeakRgdId(latestRgdId);
                        }
                        dao.updateQtl(qtl);
                    }
                }
                if (!strainWithSSLP.isEmpty()){
                    // have to delete old association and insert new one
                    for (Strain s : strainWithSSLP){
                        logger.info("\t\tStrain with an updated marker: "+s.getSymbol()+":"+s.getRgdId());
                        try {
                            dao.removeStrainAssociation(s.getRgdId(), sslp.getRgdId());
                            dao.insertStrainAssociation(s.getRgdId(), latestRgdId);
                        }catch (Exception ignore) {}
                    }
                }
            }
            catch (Exception e){
                logger.warn(e);
                logger.warn("SSLP RGD ID: " + sslp.getRgdId());
            }

        }
        /*
         * DO NOT DELETE SSLPS, ONLY RETIRE THE RGD_ID
         * isMarkerFor method in QTLDAO to find QTLs associated with sslp vars
         * same method with StrainDAO
         * */

        logger.info("\tRetiring SSLPs: "+sslps.size());
        dao.retire(sslps);
        if (!newVariants.isEmpty()){
            logger.info("\tInserting new SSLPs to Variant tables: " + newVariants.size());
            dao.insertVariantRgdIds(newVariants);
            dao.insertVariants(newVariants);
            dao.insertVariantMapData(newVariants);
        }
        if (!sslpHistoryMap.isEmpty()){
            for (Integer fromRgd : sslpHistoryMap.keySet()){
                for (Integer toRgd : sslpHistoryMap.get(fromRgd)){
                    dao.recordIdHistory(fromRgd, toRgd);
                }
            }
        }
        if (!updateVars.isEmpty()){
            logger.info("\tUpdating variant rs Ids: "+ updateVars.size());

        }
        logger.info("\nTotal pipeline runtime -- elapsed time: "+
                Utils.formatElapsedTime(pipeStart,System.currentTimeMillis()));
    }

    public static String getStringBetweenTwoCharacters(String input, String to, String from) throws Exception
    {
        return input.substring(input.indexOf(to)+1, input.lastIndexOf(from));
    }

    boolean isGenic(VariantMapData vmd) throws Exception {

        GeneCache geneCache = geneCacheMap.get(vmd.getChromosome());
        if( geneCache==null ) {
            geneCache = new GeneCache();
            geneCacheMap.put(vmd.getChromosome(), geneCache);
            geneCache.loadCache(vmd.getMapKey(), vmd.getChromosome(), DataSourceFactory.getInstance().getDataSource());
        }
        List<Integer> geneRgdIds = geneCache.getGeneRgdIds((int)vmd.getStartPos(),(int)vmd.getEndPos());
        return !geneRgdIds.isEmpty();
    }

    Map<String, GeneCache> geneCacheMap;

    public String getVersion(){
        return version;
    }
    public void setVersion(String version) {
        this.version=version;
    }

    public void setPrimaryAssembly(int primAssembly) {
        this.primaryAssembly = primAssembly;
    }

    public Integer getPrimaryAssembly() {
        return primaryAssembly;
    }
}
