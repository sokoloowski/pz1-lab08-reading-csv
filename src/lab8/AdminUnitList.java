package lab8;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUnitList {
    public List<AdminUnit> units = new ArrayList<>();
    Map<Long, AdminUnit> idToAdminUnit = new HashMap<>();
    Map<AdminUnit, Long> adminUnitToParentId = new HashMap<>();
    Map<Long, List<AdminUnit>> parentIdToChildren = new HashMap<>();

    /**
     * Czyta rekordy pliku i dodaje do listy
     *
     * @param filename nazwa pliku
     */
    public void read(String filename) {
        CSVReader reader = null;

        try {
            reader = new CSVReader(filename, ",", true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (reader != null && reader.next()) {
            AdminUnit adminUnit = new AdminUnit();

            adminUnit.name = reader.get("name");

            try {
                adminUnit.adminLevel = reader.getInt("admin_level");
            } catch (Exception e) {
                adminUnit.adminLevel = -1;
            }

            try {
                adminUnit.population = reader.getInt("population");
            } catch (Exception e) {
                adminUnit.population = -1;
            }

            try {
                adminUnit.area = reader.getDouble("area");
            } catch (Exception e) {
                adminUnit.area = -1;
            }

            try {
                adminUnit.density = reader.getDouble("density");
            } catch (Exception e) {
                adminUnit.density = -1;
            }

            try {
                adminUnit.bbox.xmin = Math.min(
                        Math.min(
                                reader.getDouble("x1"),
                                reader.getDouble("x2")),
                        Math.min(
                                reader.getDouble("x3"),
                                reader.getDouble("x4")));
                adminUnit.bbox.ymin = Math.min(
                        Math.min(
                                reader.getDouble("y1"),
                                reader.getDouble("y2")),
                        Math.min(
                                reader.getDouble("y3"),
                                reader.getDouble("y4")));
                adminUnit.bbox.xmax = Math.max(
                        Math.max(
                                reader.getDouble("x1"),
                                reader.getDouble("x2")),
                        Math.max(
                                reader.getDouble("x3"),
                                reader.getDouble("x4")));
                adminUnit.bbox.ymax = Math.max(
                        Math.max(
                                reader.getDouble("y1"),
                                reader.getDouble("y2")),
                        Math.max(
                                reader.getDouble("y3"),
                                reader.getDouble("y4")));
            } catch (Exception e) {
                // Je??eli chocia?? jedna warto???? b??dzie pusta,
                // to ca??y bounding box b??dzie z??y
                adminUnit.bbox.xmin = Double.NaN;
                adminUnit.bbox.ymin = Double.NaN;
                adminUnit.bbox.xmax = Double.NaN;
                adminUnit.bbox.ymax = Double.NaN;
            }

            long parentId = -1;
            try {
                parentId = reader.getLong("parent");
            } catch (Exception e) {
                // nie ma parenta
            }

            try {
                this.idToAdminUnit.put(reader.getLong("id"), adminUnit);
            } catch (Exception e) {
                // nie ma opcji, ??e nie b??dzie ID
                e.printStackTrace();
            }
            this.adminUnitToParentId.put(adminUnit, parentId);

            if (!this.parentIdToChildren.containsKey(parentId)) {
                this.parentIdToChildren.put(parentId, new ArrayList<>());
            }

            this.parentIdToChildren.get(parentId).add(adminUnit);

            // dodaj do listy
            this.units.add(adminUnit);
        }

        for (AdminUnit unit : this.units) {
            long parentId = this.adminUnitToParentId.get(unit);
            unit.parent = this.idToAdminUnit.getOrDefault(parentId, null);
        }

        for (Map.Entry<Long, AdminUnit> entry : this.idToAdminUnit.entrySet()) {
            entry.getValue().children = this.parentIdToChildren.get(entry.getKey());
        }
    }

    /**
     * Wypisuje zawarto???? korzystaj??c z AdminUnit.toString()
     *
     * @param out - strumie?? wyjsciowy
     */
    public void list(PrintStream out) {
        for (AdminUnit unit : this.units) {
            out.print(unit);
        }
    }

    /**
     * Wypisuje co najwy??ej limit element??w pocz??wszy od elementu o indeksie offset
     *
     * @param out    - strumie?? wyjsciowy
     * @param offset - od kt??rego elementu rozpocz???? wypisywanie
     * @param limit  - ile (maksymalnie) element??w wypisa??
     */
    public void list(PrintStream out, int offset, int limit) {
        for (int i = 0; i < limit; i++) {
            out.print(this.units.get(i + offset));
        }
    }

    /**
     * Zwraca now?? list?? zawieraj??c?? te obiekty AdminUnit, kt??rych nazwa pasuje do wzorca
     *
     * @param pattern - wzorzec dla nazwy
     * @param regex   - je??li regex=true, u??yj funkcji String matches(); je??li false u??yj funkcji contains()
     * @return podzbi??r element??w, kt??rych nazwy spe??niaj?? kryterium wyboru
     */
    public AdminUnitList selectByName(String pattern, boolean regex) {
        AdminUnitList ret = new AdminUnitList();

        // przeiteruj po zawarto??ci units
        for (AdminUnit unit : this.units) {
            if (regex) {
                if (unit.name.matches(pattern)) {
                    // je??eli nazwa jednostki pasuje do wzorca dodaj do ret
                    ret.units.add(unit);
                }
            } else {
                if (unit.name.contains(pattern)) {
                    // je??eli nazwa jednostki pasuje do wzorca dodaj do ret
                    ret.units.add(unit);
                }
            }
        }
        return ret;
    }

    private void fixMissingValues() {
        for (AdminUnit unit : this.units) {
            unit.fixMissingValues();
        }
    }

    /**
     * Zwraca list?? jednostek s??siaduj??cych z jendostk?? unit na tym samym poziomie hierarchii admin_level.
     * Czyli s??siadami wojwe??dztw s?? wojew??dztwa, powiat??w - powiaty, gmin - gminy, miejscowo??ci - inne miejscowo??ci
     *
     * @param unit        - jednostka, kt??rej s??siedzi maj?? by?? wyznaczeni
     * @param maxdistance - parametr stosowany wy????cznie dla miejscowo??ci, maksymalny promie?? odleg??o??ci od ??rodka unit,
     *                    w kt??rym maj?? sie znale???? punkty ??rodkowe BoundingBox s??siad??w
     * @return lista wype??niona s??siadami
     */
    public AdminUnitList getNeighbors(AdminUnit unit, double maxdistance) {
        AdminUnitList ret = new AdminUnitList();

        for (AdminUnit _unit : this.units) {
            if (_unit.adminLevel == unit.adminLevel && _unit != unit) {
                if (unit.bbox.intersects(_unit.bbox)) {
                    ret.units.add(_unit);
                } else {
                    try {
                        if (unit.adminLevel == 8 && unit.bbox.distanceTo(_unit.bbox) <= maxdistance) {
                            ret.units.add(_unit);
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return ret;
    }
}
