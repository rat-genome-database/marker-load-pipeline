package edu.mcw.rgd.MarkerLoad;

import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SSLP;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Main {

    private String version;
    private String markerFile;
    private int mapKey;
    private final DAO dao = new DAO();
    protected Logger logger = LogManager.getLogger("status");
    protected Logger oldDataLog = LogManager.getLogger("oldData");
    protected Logger aboveCeilLog = LogManager.getLogger("largeExpSize");
    private SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        try {
//            BulkSampleLoad bulkLoad = (BulkSampleLoad) bf.getBean("bulkLoad");
            Main main = (Main) bf.getBean("main");
            main.run();
        }
        catch (Exception e) {
            Utils.printStackTrace(e, LogManager.getLogger("status"));
            throw e;
        }
    }

    void run() throws Exception{

        logger.info(getVersion());
        logger.info("   "+dao.getConnection());

        long pipeStart = System.currentTimeMillis();
        logger.info("Pipeline started at "+sdt.format(new Date(pipeStart))+"\n");

//        HashMap<String, String> chrMap = getChromosomeMap();
        Map<Integer, List<Marker>> markerMap = new HashMap<>();

        BufferedReader br = openFile(markerFile);
        String lineData;
        int z = 0;
        while ((lineData = br.readLine()) != null) {
            if (z>2){
//                System.out.println(lineData);
                String[] lineSplit = lineData.split("\\s");

                Marker m = new Marker();
                String[] nameSplit = lineSplit[0].split("_");
                m.setRgdId(Integer.parseInt(nameSplit[0]));
                m.setSymbol(nameSplit[1]);
                m.setChr(lineSplit[1].replace("chr",""));
                m.setStrand(lineSplit[2]);
                m.setStart(Integer.parseInt(lineSplit[3]));
                m.setStop(Integer.parseInt(lineSplit[4]));
                m.setExpectedSize(Integer.parseInt(lineSplit[5]));

                List<Marker> mList = markerMap.get(m.getRgdId());
                if (mList == null){
                    mList= new ArrayList<>();
                    mList.add(m);
                    markerMap.put(m.getRgdId(),mList);
                }
                else {
                    mList.add(m);
                    markerMap.put(m.getRgdId(),mList);
                }

            } // end if
            z++;
        }// end file loop

        br.close();
        logger.info("RGDID|Name|Chr|Strand|Start|Stop|Expected Size");
        int aboveThreshCnt = 0;
        int noneThreshCnt = 0;
        int aboveThreshCeilCnt = 0;
        List<MapData> mapDataList = new ArrayList<>();
        for (Integer key : markerMap.keySet()){
            List<Marker> markers = markerMap.get(key);
            List<Marker> aboveThresh = new ArrayList<>();
            List<Marker> aboveCeiling = new ArrayList<>();
            // loop through markers
            for(Marker m : markers) {
                if (m.getExpectedSize()>60 && m.getExpectedSize()<=1000)
                    aboveThresh.add(m);
                if (m.getExpectedSize()>1000)
                    aboveCeiling.add(m);
            }


            if (aboveThresh.size()==1){
                // if 1 is above threshold, add/update that obj
                Marker m = aboveThresh.get(0);
                List<SSLP> sslps = dao.getSSLPs(m.getRgdId());
                if (sslps.isEmpty()){
                    SSLP sslp = new SSLP();
                    sslp.setName(m.getSymbol());
                    sslp.setSpeciesTypeKey(3);
                    sslp.setExpectedSize(m.getExpectedSize());
                    RgdId r = dao.createRgdId(RgdId.OBJECT_KEY_SSLPS, "ACTIVE", "created by Marker Load Pipeline", mapKey);
                    sslp.setRgdId(r.getRgdId());
                    dao.insertSSLP(sslp);
                    MapData md = new MapData();
                    md.setChromosome(m.getChr());
                    md.setRgdId(r.getRgdId());
                    md.setStartPos(m.getStart());
                    md.setStopPos(m.getStop());
                    md.setStrand(m.getStrand());
                    md.setMapKey(mapKey);
                    md.setSrcPipeline("Marker Load Pipeline");
                    mapDataList.add(md);
                }
                else {
                    SSLP sslp = null;
                    for (SSLP s : sslps){
                        if (s.getExpectedSize()!=0){
                            sslp = s;
                            break;
                        }
                    }
                    if (sslp==null){
                        sslp = sslps.get(0);
                    }
                    if (!Utils.intsAreEqual(sslp.getExpectedSize(), m.getExpectedSize())) {
                        oldDataLog.info(sslp.getName()+"|OLD: "+sslp.getExpectedSize()+"|"+"NEW: "+m.getExpectedSize());
                        sslp.setExpectedSize(m.getExpectedSize());
                        dao.updateSSLP(sslp);
                        List<MapData> mapsData = dao.getMapData(sslp.getRgdId(),mapKey);
                        if (mapsData.isEmpty()) {
                            MapData md = new MapData();
                            md.setChromosome(m.getChr());
                            md.setRgdId(sslp.getRgdId());
                            md.setStartPos(m.getStart());
                            md.setStopPos(m.getStop());
                            md.setStrand(m.getStrand());
                            md.setMapKey(mapKey);
                            md.setSrcPipeline("Marker Load Pipeline");
                            mapDataList.add(md);
                        }
                    }
                }
            }
            else if (aboveThresh.size()>1){
                // if multiple is above threshold, need curator
                aboveThreshCnt++;
                logger.info("Multiple above threshold:");
                for (Marker m : aboveThresh){
                    logger.info(m.dump("|"));
                }
                logger.info("");
            }
            else {
                // if none are above threshold, need curator
                noneThreshCnt++;
                logger.info("None above threshold:");
                for (Marker m : markers){
                    logger.info(m.dump("|"));
                }
                logger.info("");
            }
            if (!aboveCeiling.isEmpty()){
                aboveThreshCeilCnt++;
                for(Marker m : aboveCeiling){
                    aboveCeilLog.info(m.dump("|"));
                }
            }
        }

        logger.info("total above thresh: "+aboveThreshCnt);
        logger.info("total with none above thresh: "+ noneThreshCnt);
        logger.info("Total above ceiling threshold: "+aboveThreshCeilCnt);
        if (!mapDataList.isEmpty()){
            logger.info("New marker mapdata being made: " +mapDataList.size());
            dao.insertMapsData(mapDataList);
        }
        logger.info(" Total Elapsed time -- elapsed time: "+
                Utils.formatElapsedTime(pipeStart,System.currentTimeMillis())+"\n");
    }

    private BufferedReader openFile(String fileName) throws IOException {

        String encoding = "UTF-8"; // default encoding

        InputStream is;
        if( fileName.endsWith(".gz") ) {
            is = new GZIPInputStream(new FileInputStream(fileName));
        } else {
            is = new FileInputStream(fileName);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
        return reader;
    }

    HashMap<String,String> getChromosomeMap() throws Exception{
        List<Chromosome> chrs = dao.getChromosomes(mapKey);
        HashMap<String,String> map = new HashMap<>();
        for (Chromosome chr : chrs){
            map.put(chr.getGenbankId(),chr.getChromosome());
        }
        return map;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setMarkerFile(String markerFile) {
        this.markerFile = markerFile;
    }

    public String getMarkerFile() {
        return markerFile;
    }

    public void setMapKey(int mapKey) {
        this.mapKey = mapKey;
    }

    public int getMapKey() {
        return mapKey;
    }
}