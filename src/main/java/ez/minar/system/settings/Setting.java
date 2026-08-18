package ez.minar.system.settings;

import lombok.Getter;
import lombok.Setter;

public class Setting {
    @Getter String name;
    @Getter @Setter boolean visible;

    public Setting(String name) {
        this.name = name;
        this.visible = true;
    }
}