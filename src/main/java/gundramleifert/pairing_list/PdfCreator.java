package gundramleifert.pairing_list;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.*;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import gundramleifert.pairing_list.configs.DisplayProps;
import gundramleifert.pairing_list.configs.ScheduleProps;
import gundramleifert.pairing_list.cost_calculators.CostCalculatorBoatSchedule;
import gundramleifert.pairing_list.types.BoatMatrix;
import gundramleifert.pairing_list.types.Flight;
import gundramleifert.pairing_list.types.Race;
import gundramleifert.pairing_list.types.Schedule;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PdfCreator implements AutoCloseable {

    private DisplayProps displayProps;
    private ScheduleProps scheduleProps;
    private File outFile;
    private Document doc;
    private boolean isEmptyPage = true;
    private Map<String, DisplayProps.DeviceRgbWithAlpha> colorMap;
    private DisplayProps.DeviceRgbWithAlpha[] fgColors;
    private DisplayProps.DeviceRgbWithAlpha[] bgColors;

    private static Map<String, DisplayProps.DeviceRgbWithAlpha> createColorMap(DisplayProps displayProps) {
        Map<String, DisplayProps.DeviceRgbWithAlpha> res = defaultColorMap();
        if (displayProps.additional_colors == null || displayProps.additional_colors.isEmpty()) {
            return res;
        }
        for (String name : displayProps.additional_colors.keySet()) {
            if (!name.toUpperCase().equals(name)) {
                throw new RuntimeException(String.format("color name %s is not upper.", name));
            }
            int[] rgba = displayProps.additional_colors.get(name);
            res.put(name, DisplayProps.DeviceRgbWithAlpha.fromArray(rgba));
        }
        return res;
    }

    private static Map<String, DisplayProps.DeviceRgbWithAlpha> defaultColorMap() {
        HashMap<String, DisplayProps.DeviceRgbWithAlpha> res = new HashMap<>();
        res.put("BLACK", DisplayProps.DeviceRgbWithAlpha.fromArray(0));
        res.put("BLUE", DisplayProps.DeviceRgbWithAlpha.fromArray(0, 0, 255));
        res.put("CYAN", DisplayProps.DeviceRgbWithAlpha.fromArray(0, 255, 255));
        res.put("DARK_GRAY", DisplayProps.DeviceRgbWithAlpha.fromArray(64));
        res.put("GRAY", DisplayProps.DeviceRgbWithAlpha.fromArray(128));
        res.put("GREEN", DisplayProps.DeviceRgbWithAlpha.fromArray(0, 255, 0));
        res.put("LIGHT_GRAY", DisplayProps.DeviceRgbWithAlpha.fromArray(192));
        res.put("MAGENTA", DisplayProps.DeviceRgbWithAlpha.fromArray(255, 0, 255));
        res.put("ORANGE", DisplayProps.DeviceRgbWithAlpha.fromArray(255, 200, 0));
        res.put("PINK", DisplayProps.DeviceRgbWithAlpha.fromArray(255, 175, 175));
        res.put("RED", DisplayProps.DeviceRgbWithAlpha.fromArray(255, 0, 0));
        res.put("YELLOW", DisplayProps.DeviceRgbWithAlpha.fromArray(255, 255, 0));
        res.put("WHITE", DisplayProps.DeviceRgbWithAlpha.fromArray(255));
        return res;
    }

    private static float avg(DisplayProps.DeviceRgbWithAlpha color) {
        float[] f = color.getColorValue();
        float res = 0;
        for (int i = 0; i < f.length; i++) {
            res += f[i];
        }
        return res / f.length;
    }

    public PdfCreator(DisplayProps displayProps,
                      ScheduleProps scheduleProps,
                      File outFile) {
        this.displayProps = displayProps;
        this.scheduleProps = scheduleProps;
        this.outFile = outFile;
    }

    @SneakyThrows
    public void init() {
        PdfWriter writer = new PdfWriter(this.outFile);
        this.doc = new Document(new PdfDocument(writer));

        String[] colors = scheduleProps.boats;
        this.fgColors = new DisplayProps.DeviceRgbWithAlpha[colors.length];
        this.bgColors = new DisplayProps.DeviceRgbWithAlpha[colors.length];
        colorMap = createColorMap(displayProps);
        for (int i = 0; i < colors.length; i++) {
            DisplayProps.DeviceRgbWithAlpha color = colorMap.getOrDefault(colors[i].toUpperCase(), null);
            if (color == null) {
                throw new RuntimeException(String.format("cannot interpret key `%s` - choose one of %s",
                        colors[i],
                        String.join(",", colorMap.keySet())));
            }
            bgColors[i] = color;
            fgColors[i] = avg(color) > 0.3 ? DisplayProps.DeviceRgbWithAlpha.BLACK : DisplayProps.DeviceRgbWithAlpha.WHITE;
        }
    }


    private Cell getDft(int row, int col) {
        Cell cell = new Cell(row, col)
                .setPadding(0.0f)
                .setFontSize(displayProps.fontsize);
        if (row * col == 1) {
            //cell.setMaxHeight(displayProps.cell_height);
        }
        return cell;
    }

    public static Cell emph(Cell cell, Color colorFg) {
        SolidBorder solidBorder = new SolidBorder(2f);
        solidBorder.setColor(colorFg);
        return cell
                .setBorder(solidBorder)
                .setBold();
    }

    private Cell getCell(String text, int row, int col) {
        return getDft(row, col)
                .add(new Paragraph(text))
                .setPadding(0.0f)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell getCell(String text) {
        return getCell(text, 1, 1);
    }

    private Cell getCell(String text, int index) {
        return getCell(text, index, 1.0f);
    }

    private Cell getCell(String text, int index, float opacity) {
        Cell cell = getCell(text);
        if (index >= 0) {
            DisplayProps.DeviceRgbWithAlpha bgColor = bgColors[index];
            float v = (1 - (1 - avg(bgColor)) * bgColor.alpha * opacity);
            DisplayProps.DeviceRgbWithAlpha fg = v > 0.3f ? DisplayProps.DeviceRgbWithAlpha.BLACK : DisplayProps.DeviceRgbWithAlpha.WHITE;
            cell.setBackgroundColor(bgColor, bgColor.alpha * opacity)
                    .setFontColor(fg);
        }
        return cell;
    }

    private Cell getCellSep(int colspan, float height) {
        return getDft(1, colspan)
                .setHeight(height)
                .setBorder(Border.NO_BORDER);
    }

    private Cell getCellSpan(String text, int rowspan) {
        return getDft(rowspan, 1)
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.CENTER)
                //.setBorderBottom(new DoubleBorder(1.0f))
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    public void newPage(boolean alsoIfEmpty) {
        if (alsoIfEmpty || !isEmptyPage) {
            doc.add(new AreaBreak());
            isEmptyPage = true;
        }
    }

    private static int max(int[] vec) {
        int res = vec[0];
        for (int i = 1; i < vec.length; i++) {
            res = Math.max(res, vec[i]);
        }
        return res;
    }

    private static int sum(int[] vec) {
        int res = vec[0];
        for (int i = 1; i < vec.length; i++) {
            res += vec[i];
        }
        return res;
    }

    public PdfCreator createScheduleDistribution(Schedule schedule, boolean sortBoats) {
        newPage(false);
        MatchMatrix matchMatrix = new MatchMatrix(scheduleProps.numTeams);
        int[][] values = new int[scheduleProps.flights][scheduleProps.flights + 1];
        for (int i = 0; i < schedule.flights.length; i++) {
            Flight flight = schedule.flights[i];
            matchMatrix.add(flight, sortBoats);
            int[] matchDistribution = matchMatrix.getMatchDistribution();
            for (int j = 0; j < matchDistribution.length; j++) {
                values[i][j] = matchDistribution[j];
            }
        }
        int[] matchDistribution = matchMatrix.getMatchDistribution();
        int columns = 0;
        for (int i = 0; i < matchDistribution.length; i++) {
            if (matchDistribution[i] > 0) {
                columns = i + 2;
            }
        }
        float[] columnWidths = new float[columns];
        Arrays.fill(columnWidths, displayProps.width / columns);
        Table table = new Table(columnWidths);
        table.addCell(getCell(""));
        table.addCell(getCell("number of matches", 1, columns - 1));
        table.addCell(getCell("flight"));
        for (int i = 0; i < columns - 1; i++) {
            table.addCell(getCell(String.valueOf(i)));
        }
        for (int flightIdx = 0; flightIdx < values.length; flightIdx++) {
            int[] valuesFlight = values[flightIdx];
            float max = sum(matchDistribution);
            table.addCell(getCell(String.valueOf(flightIdx + 1)));
            for (int row = 0; row < columns - 1; row++) {
                int cnt = valuesFlight[row];
                float weight = 1 - cnt / max;
                Cell cell = getCell(cnt == 0 ? "" : String.valueOf(cnt));
                if (weight < 0.5) {
                    cell.setFontColor(ColorConstants.WHITE);
                }
                cell.setBackgroundColor(new DeviceGray(weight));
                table.addCell(cell);
            }
        }
        doc.add(table);
        isEmptyPage = false;
        return this;
    }

    private static String toString(String[] teams, List<Byte> lst) {
        return lst
                .stream()
                .map(aByte -> teams[aByte])
                .collect(Collectors.joining(", "));
    }

    private static String toCount(List<Byte> lst) {
        return lst.size()>0?String.valueOf(lst.size()):"";
    }

    @SneakyThrows
    public void create(Schedule schedule) {
        init();
        if (displayProps.show_match_stat) {
            createScheduleDistribution(schedule, true);
        }
        if (displayProps.show_boat_stat) {
            createBoatDistribution(schedule);
        }
        if (displayProps.show_schuttle_stat) {
            createShuttleDistribution(schedule);
        }
        createSchedule(schedule, null);
        if (displayProps.teamwise_list) {
            for (int i = 0; i < scheduleProps.teams.length; i++) {
                createSchedule(schedule, scheduleProps.teams[i]);
            }
        }
        close();
    }

    public PdfCreator createBoatDistribution(Schedule schedule) {
        newPage(false);
        BoatMatrix matchMatrix = new BoatMatrix(scheduleProps);
        int[][] values = new int[scheduleProps.flights][];
        for (int flightIdx = 0; flightIdx < schedule.flights.length; flightIdx++) {
            Flight flight = schedule.flights[flightIdx];
            matchMatrix.add(flight);
            values[flightIdx] = matchMatrix.getBoatDistribution();
        }
        int[] matchDistribution = values[scheduleProps.flights - 1];
        int columns = 0;
        for (int i = 0; i < matchDistribution.length; i++) {
            if (matchDistribution[i] > 0) {
                columns = i + 2;
            }
        }
        float[] columnWidths = new float[columns];
        Arrays.fill(columnWidths, displayProps.width / columns);
        Table table = new Table(columnWidths);
        table.setVerticalBorderSpacing(10f);
        table.addCell(getCell(""));
        table.addCell(getCell("number of boat usages", 1, columns - 1));
        table.addCell(getCell("flight"));
        for (int i = 0; i < columns - 1; i++) {
            table.addCell(getCell(String.valueOf(i)));
        }
        for (int flightIdx = 0; flightIdx < values.length; flightIdx++) {
            int[] valuesFlight = values[flightIdx];
            float max = sum(matchDistribution);
            table.addCell(getCell(String.valueOf(flightIdx + 1)));
            for (int row = 0; row < columns - 1; row++) {
                int cnt = row < valuesFlight.length ? valuesFlight[row] : 0;
                float weight = 1 - cnt / max;
                Cell cell = getCell(cnt == 0 ? "" : String.valueOf(cnt));
                if (weight < 0.5) {
                    cell.setFontColor(ColorConstants.WHITE);
                }
                cell.setBackgroundColor(new DeviceGray(weight));
                table.addCell(cell);
            }
        }
        doc.add(table);
        isEmptyPage = false;
        return this;
    }

    public PdfCreator createShuttleDistribution(Schedule schedule) {
        newPage(false);
        String[] clubs = scheduleProps.teams;
        float[] columnWidths = new float[5];
        Arrays.fill(columnWidths, displayProps.width / 5);
        Table table = new Table(columnWidths);
        table.setVerticalBorderSpacing(10f);
        Arrays.asList("At Flight", "On Boat", "On Water 1", "On Water 2", "Boatchange")
                .forEach(s -> table.addCell(getCell(s)));
        table.addCell(getCellSep(5, 0.3f));
        for (int i = 1; i < schedule.flights.length; i++) {
            CostCalculatorBoatSchedule.InterFlightStat interFlightStat =
                    CostCalculatorBoatSchedule.getInterFlightStat(schedule.flights[i - 1], schedule.flights[i]);
            table.addCell(getCell(String.format("%d -> %d", i,i + 1)));
            table.addCell(getCell(toString(clubs, interFlightStat.teamsStayOnBoat)));
            table.addCell(getCell(toString(clubs, interFlightStat.teamsAtWaterAtLastRace)));
            table.addCell(getCell(toString(clubs, interFlightStat.teamsAtWaterAtFirstRace)));
            table.addCell(getCell(toString(clubs, interFlightStat.teamsChangeBoats)));
        }
        doc.add(table);
        isEmptyPage = false;
        return this;
    }

    @SneakyThrows
    public PdfCreator createSchedule(
            Schedule schedule,
            String emphClub) {
        newPage(false);
        float[] columnWidths = new float[scheduleProps.numBoats + 2];
        double basewith = displayProps.width / (scheduleProps.numBoats + 2 * displayProps.factor_flight_race_width);
        Arrays.fill(columnWidths, 2, columnWidths.length, (float) basewith);
        Arrays.fill(columnWidths, 0, 2, (float) (basewith * displayProps.factor_flight_race_width));
        Table table = new Table(columnWidths);
        table.addCell(getCell("Flight", -1));
        table.addCell(getCell("Race", -1));
        for (int i = 0; i < scheduleProps.numBoats; i++) {
            table.addCell(getCell(String.format("Boat %d", i + 1), i));
        }
        int race = 1;
        String[] clubs = scheduleProps.teams;
        for (int flight = 0; flight < schedule.flights.length; flight++) {
            table.addCell(getCellSep(columnWidths.length, 1.0f));
            Flight f = schedule.flights[flight];
            table.addCell(getCellSpan(String.valueOf(flight + 1), f.races.length));
            for (int i = 0; i < f.races.length; i++) {
                table.addCell(getCell(String.valueOf(race++), -1));
                Race r = f.races[i];
                int col = 0;
                for (; col < r.teams.length; col++) {
                    String team = clubs[r.teams[col]];
                    float opacity = emphClub != null && !team.equals(emphClub) ? 0.5f : 1.0f;
                    Cell cell = getCell(team, col, opacity);
                    table.addCell(cell);
                }
                while (col < scheduleProps.numBoats) {
                    //add empty cell
                    table.addCell(getCell("", col++));
                }
            }
        }
        doc.add(new Paragraph(displayProps.title)
                .setFontSize(displayProps.fontsize * 2)
                .setTextAlignment(TextAlignment.CENTER)
        );
        doc.add(table);
        isEmptyPage = false;
        return this;
    }

    @SneakyThrows
    public void close() {
        this.doc.close();
    }
}
