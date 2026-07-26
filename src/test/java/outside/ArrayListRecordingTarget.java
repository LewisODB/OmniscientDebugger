package outside;

import java.util.ArrayList;
import java.util.List;

public final class ArrayListRecordingTarget {
    private ArrayListRecordingTarget() {
    }

    public static List mutate() {
        ArrayList values = new ArrayList();
        values.add("first");
        values.add(0, "zeroth");
        values.set(1, "changed");
        values.remove(0);
        return values;
    }
}
