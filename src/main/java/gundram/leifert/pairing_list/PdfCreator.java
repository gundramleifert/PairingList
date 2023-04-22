package gundram.leifert.pairing_list;

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
import gundram.leifert.pairing_list.types.Flight;
import gundram.leifert.pairing_list.types.Race;
import gundram.leifert.pairing_list.types.Schedule;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PdfCreator implements AutoCloseable {

    private ScheduleProps properties;
    private File outFile;
    private Document doc;
    private boolean isEmptyPage = true;
    private static Map<String, Color> colorMap = generateMap();

    private static Map<String, Color> generateMap() {
        HashMap<String, Color> res = new HashMap<>();
        res.put("BLACK", ColorConstants.BLACK);
        res.put("BLUE", ColorConstants.BLUE);
        res.put("CYAN", ColorConstants.CYAN);
        res.put("DARK_GRAY", ColorConstants.DARK_GRAY);
        res.put("GRAY", ColorConstants.GRAY);
        res.put("GREEN", ColorConstants.GREEN);
        res.put("LIGHT_GRAY", ColorConstants.LIGHT_GRAY);
        res.put("MAGENTA", ColorConstants.MAGENTA);
        res.put("ORANGE", ColorConstants.ORANGE);
        res.put("PINK", ColorConstants.PINK);
        res.put("RED", ColorConstants.RED);
        res.put("YELLOW", ColorConstants.YELLOW);
        return res;
    }

    private static float avg(Color color) {
        float[] f = color.getColorValue();
        float res = 0;
        for (int i = 0; i < f.length; i++) {
            res += f[i];
        }
        return res / f.length;
    }

    public PdfCreator(ScheduleProps properties,
                      File outFile) {
        this.properties = properties;
        this.outFile = outFile;
        try {
            PdfWriter writer = new PdfWriter(this.outFile);
            this.doc = new Document(new PdfDocument(writer));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        String[] colors = properties.boats;
        this.fgColors = new Color[colors.length];
        this.bgColors = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            Color color = colorMap.getOrDefault(colors[i].toUpperCase(), null);
            if (color == null) {
                throw new RuntimeException(String.format("cannot interpret key `%s` - choose one of %s",
                        colors[i],
                        String.join(",", colorMap.keySet())));
            }
            bgColors[i] = color;
            fgColors[i] = avg(color) > 0.3 ? ColorConstants.BLACK : ColorConstants.WHITE;
        }
    }

    private Color[] fgColors;
    private Color[] bgColors;

    private Cell getDft(int row, int col) {
        return new Cell(row, col)
                .setPadding(0.0f)
                .setFontSize(properties.fontsize);
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
        Cell cell = getCell(text);
        if (index >= 0) {
            cell.setBackgroundColor(bgColors[index])
                    .setFontColor(fgColors[index]);
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
        MatchMatrix matchMatrix = new MatchMatrix(this.properties.numTeams);
        int[][] values = new int[properties.flights][properties.flights + 1];
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
        Arrays.fill(columnWidths, properties.width / columns);
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
    private static String toString(String[] teams, List<Byte> lst){
        return lst
                .stream()
                .map(aByte -> teams[aByte])
                .collect(Collectors.joining(", "));
    }

    public PdfCreator createBoatDistribution(Schedule schedule) {
        newPage(false);
        BoatMatrix matchMatrix = new BoatMatrix(properties);
        int[][] values = new int[properties.flights][];
        for (int flightIdx = 0; flightIdx < schedule.flights.length; flightIdx++) {
            Flight flight = schedule.flights[flightIdx];
            matchMatrix.add(flight);
            values[flightIdx] = matchMatrix.getBoatDistribution();
        }
        int[] matchDistribution = values[properties.flights - 1];
        int columns = 0;
        for (int i = 0; i < matchDistribution.length; i++) {
            if (matchDistribution[i] > 0) {
                columns = i + 2;
            }
        }
        float[] columnWidths = new float[columns];
        Arrays.fill(columnWidths, properties.width / columns);
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

        String[] clubs = properties.teams;
        columnWidths = new float[5];
        Arrays.fill(columnWidths, properties.width / 10);
        Table table2 = new Table(columnWidths);
        table.setVerticalBorderSpacing(10f);
        Arrays.asList("At Flight", "On Boat","On Water 1", "On Water 2", "Boatchange")
                .forEach(s -> table2.addCell(getCell(s)));
        table2.addCell(getCellSep(5,0.3f));
        for (int i = 1; i < schedule.flights.length; i++) {
            CostCalculatorBoatSchedule.InterFlightStat interFlightStat =
                    CostCalculatorBoatSchedule.getInterFlightStat(schedule.flights[i - 1], schedule.flights[i]);
            table2.addCell(getCell(String.valueOf(i + 1)));
            table2.addCell(getCell(toString(clubs,interFlightStat.teamsStayOnBoat)));
            table2.addCell(getCell(toString(clubs,interFlightStat.teamsAtWaterAtLastRace)));
            table2.addCell(getCell(toString(clubs,interFlightStat.teamsAtWaterAtFirstRace)));
            table2.addCell(getCell(toString(clubs,interFlightStat.teamsChangeBoats)));
        }
        doc.add(table2);
        isEmptyPage = false;
        return this;
    }

    public PdfCreator createPageSchedule(
            Schedule schedule,
            String emphClub) throws FileNotFoundException {
        newPage(false);
        int columns = properties.numBoats + 2;
        float[] columnWidths = new float[columns];
        Arrays.fill(columnWidths, properties.width / columns);
        Table table = new Table(columnWidths);
        table.addCell(getCell("Flight", -1));
        table.addCell(getCell("Race", -1));
        for (int i = 0; i < properties.numBoats; i++) {
            table.addCell(getCell(String.format("Boat %d", i + 1), i));
        }
        int race = 1;
        String[] clubs = properties.teams;
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
                    Cell cell = getCell(team, col);
                    if (team.equals(emphClub)) {
                        cell = emph(cell, fgColors[col]);
                    }
                    table.addCell(cell);
                }
                while (col < columns - 2) {
                    //add empty cell
                    table.addCell(getCell("", col++));
                }
            }
        }
        doc.add(new Paragraph(properties.title)
                .setFontSize(properties.fontsize * 2)
                .setTextAlignment(TextAlignment.CENTER)
        );
        doc.add(table);
        isEmptyPage = false;
        return this;
    }

    public void close() throws Exception {
        this.doc.close();
    }
}
