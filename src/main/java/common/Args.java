package common;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonObject;

/**
 * @author Mack_TB
 * @version 1.0.7
 * @since 12/27/2020
 */

public class Args {

    @Parameter(names = "-t", description = "type")
    private String type;

    @Parameter(names = "-k", description = "key")
    private Object key;

    @Parameter(names = "-v", description = "value")
    private Object value;

    /*@Parameter(names = "-in",  converter = FileConverter.class)
    private File inputFile;*/
    @Parameter(names = "-in")
    private String fileName;

    public Args() { }

    public Args(String type, Object key) {
        this.type = type;
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getFileName() {
        return fileName;
    }
}
