package gundramleifert.pairing_list;

import gundramleifert.pairing_list.configs.DisplayConfig;
import gundramleifert.pairing_list.configs.ScheduleConfig;
import gundramleifert.pairing_list.types.Race;
import gundramleifert.pairing_list.types.Schedule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.Random;

/**
 * Renders <b>one</b> PDF from a pairing list that already exists.
 * <p>
 * {@link Optimizer} computes a list and prints it on the way out; {@link ReuseSchedule}
 * re-renders a directory full of event files — one PDF per title, plus the debug variant,
 * plus {@code pairing_list.csv}, {@code pairing_list.yml} and a {@code *_teams.yml} beside
 * each of them. Neither is what a caller wants that holds a finished list and needs a
 * single file to hand out: a web backend that drew the list elsewhere (teams and boat
 * colours come from its own database), or anyone who edited a list by hand and wants to
 * print it.
 * <p>
 * So this entry point does exactly one thing: schedule configuration + pairing list in,
 * one PDF out, nothing else written. The title is an argument rather than only a field of
 * the configuration, because the caller usually knows the occasion ("Pairing List — 1.
 * Segel-Bundesliga, Act 3") better than the file does.
 * <p>
 * Example:
 * <pre>
 * java -cp pairing-list-1.0-SNAPSHOT-jar-with-dependencies.jar \
 *      gundramleifert.pairing_list.PdfExport \
 *      -s schedule_cfg.yml -pli pairing_list.yml -plp out.pdf -t "Act 3"
 * </pre>
 */
public class PdfExport {

  /**
   * The same seed {@link Saver} uses. Which teams share a shuttle is drawn at random for
   * the team pages, so rendering the same list twice has to start from the same seed —
   * otherwise two prints of one draw would disagree about who rides with whom.
   */
  private static final long SHUTTLE_SEED = 1234;

  public static void main(String[] args) throws Exception {
    Options options = new Options();

    Option scheduleConfig = new Option(
            "s",
            "schedule_config",
            true,
            "the path to the yaml-file containing the schedule configuration (teams, boats, flights)");
    scheduleConfig.setRequired(false);
    options.addOption(scheduleConfig);

    Option input = new Option(
            "pli",
            "pairing_list_in",
            true,
            "the path to the yaml-file containing the pairing list to render");
    input.setRequired(false);
    options.addOption(input);

    Option displayConfig = new Option(
            "dc",
            "display",
            true,
            "the path to the yaml-file containing the display configuration for the pdf");
    displayConfig.setRequired(false);
    options.addOption(displayConfig);

    Option outPdf = new Option(
            "plp",
            "pairing_list_pdf",
            true,
            "the path of the pdf to write");
    outPdf.setRequired(false);
    options.addOption(outPdf);

    Option titleOption = new Option(
            "t",
            "title",
            true,
            "the title printed above the list; defaults to the first entry of `titles` in the schedule configuration");
    titleOption.setRequired(false);
    options.addOption(titleOption);

    Option teamOption = new Option(
            "team",
            "team_index",
            true,
            "if given, render only this team's page (0-based index into the teams of the schedule configuration)");
    teamOption.setRequired(false);
    options.addOption(teamOption);

    Option debugOption = new Option(
            "d",
            "debug",
            false,
            "prepend the statistic pages (matches, boat usage, shuttles) to the pdf");
    debugOption.setRequired(false);
    options.addOption(debugOption);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      new HelpFormatter().printHelp("Render a Pairing List for the Liga-Format as pdf", options);
      System.exit(1);
    }

    ScheduleConfig scheduleProps = ScheduleConfig.readYaml(
            cmd.getOptionValue(scheduleConfig, "schedule_cfg.yml"));
    Schedule schedule = Schedule.readYaml(
            new File(cmd.getOptionValue(input, "pairing_list.yml")), null);
    checkFits(scheduleProps, schedule);

    DisplayConfig displayProps = readDisplayConfig(cmd.getOptionValue(displayConfig));
    String title = cmd.getOptionValue(titleOption, defaultTitle(scheduleProps));
    File out = new File(cmd.getOptionValue(outPdf, "pairing_list.pdf"));

    PdfCreator creator = new PdfCreator(displayProps, scheduleProps, out);
    if (cmd.hasOption(teamOption)) {
      byte team = parseTeam(cmd.getOptionValue(teamOption), scheduleProps);
      creator.createForTeam(schedule, title, team, new Random(SHUTTLE_SEED));
    } else {
      creator.create(schedule, title, new Random(SHUTTLE_SEED), cmd.hasOption(debugOption));
    }
    System.out.printf("wrote %s%n", out.getAbsolutePath());
  }

  /**
   * A display configuration is optional here, unlike everywhere else in the tool.
   * <p>
   * Given explicitly, a missing file is an error — someone who names a file means it. Not
   * given, the one in the working directory is used if it is there, and otherwise the
   * built-in defaults: a caller that only wants the list on paper should not have to write
   * a file of display settings first.
   */
  private static DisplayConfig readDisplayConfig(String path) throws Exception {
    if (path != null) {
      return DisplayConfig.readYaml(path);
    }
    if (new File("display_cfg.yml").exists()) {
      return DisplayConfig.readYaml("display_cfg.yml");
    }
    return new DisplayConfig();
  }

  private static String defaultTitle(ScheduleConfig scheduleProps) {
    if (scheduleProps.titles != null && scheduleProps.titles.length > 0) {
      return scheduleProps.titles[0];
    }
    return "Pairing List";
  }

  /**
   * The index of the team to render, checked against the list it indexes.
   * <p>
   * An index out of range would otherwise reach {@code teams[i]} and fail as an
   * ArrayIndexOutOfBounds somewhere in the layout, which says nothing about the argument
   * that caused it. Padding seats of an unevenly divided fleet are refused too: they carry
   * no name and no crew.
   */
  private static byte parseTeam(String value, ScheduleConfig scheduleProps) {
    int index;
    try {
      index = Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new RuntimeException(String.format("team index `%s` is not a number.", value));
    }
    if (index < 0 || index >= scheduleProps.numTeams) {
      throw new RuntimeException(String.format(
              "team index %d is outside the %d teams of the schedule configuration.",
              index, scheduleProps.numTeams));
    }
    return (byte) index;
  }

  /**
   * Checks that this list was drawn for this configuration.
   * <p>
   * Without it a list and a configuration that disagree render happily and wrongly: team
   * indices are looked up in {@code scheduleProps.teams}, so a list drawn for a different
   * field prints the wrong club on every boat instead of failing.
   */
  public static void checkFits(ScheduleConfig scheduleProps, Schedule schedule) {
    if (schedule.size() != scheduleProps.flights) {
      throw new RuntimeException(String.format(
              "loaded schedule config has %d flights but loaded pairing list has %d.",
              scheduleProps.flights, schedule.size()));
    }
    int cntTeams = 0;
    int cntBoats = 0;
    for (Race race : schedule.get(0).races) {
      cntTeams += race.teams.length;
      cntBoats = Math.max(cntBoats, race.teams.length);
    }
    if (cntTeams != scheduleProps.teams.length) {
      throw new RuntimeException(String.format(
              "loaded schedule config has %d teams but loaded pairing list has %d.",
              scheduleProps.teams.length, cntTeams));
    }
    if (cntBoats != scheduleProps.boats.length) {
      throw new RuntimeException(String.format(
              "loaded schedule config has %d boats but loaded pairing list has %d.",
              scheduleProps.boats.length, cntBoats));
    }
  }
}
