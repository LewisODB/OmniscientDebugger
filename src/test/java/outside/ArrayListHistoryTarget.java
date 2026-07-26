package outside;

import java.util.ArrayList;

public final class ArrayListHistoryTarget {
    private ArrayListHistoryTarget() {
    }

    public static void main(String[] args) {
        ArrayList values = new ArrayList();
        values.add("first");
        values.add(0, "zeroth");
        values.set(1, "changed");
        values.remove(0);
        values.clear();
    }
}
