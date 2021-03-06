package models;


import javax.persistence.*;

/**
 * Entity, also eins zu eins Abbildung der Datenbanktabelle settings
 *
 * @author Oliver Frischknecht
 */

public class Setting {

    private int id;

    private String name;

    private String value;

    private int type;

    public Setting() {

    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public int getType(){ return type; }
    public void setType(int type) { this.type = type; }
}
