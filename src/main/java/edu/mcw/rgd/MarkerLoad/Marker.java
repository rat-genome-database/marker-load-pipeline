package edu.mcw.rgd.MarkerLoad;

public class Marker {
    private String symbol;
    private String chr;
    private String strand;
    private Integer start;
    private Integer stop;
    private Integer expectedSize;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getStop() {
        return stop;
    }

    public void setStop(Integer stop) {
        this.stop = stop;
    }

    public Integer getExpectedSize() {
        return expectedSize;
    }

    public void setExpectedSize(Integer expectedSize) {
        this.expectedSize = expectedSize;
    }

    public String dump(String delim){
        return this.symbol + delim + this.chr + delim + this.strand + delim + this.start + delim + this.stop + delim + this.expectedSize;
    }
}
