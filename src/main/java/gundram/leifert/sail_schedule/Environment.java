package gundram.leifert.sail_schedule;

import gundram.leifert.sail_schedule.types.Schedule;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Environment {
    private ScheduleProps properties;
    private OptimizationProperties optProps;
    private Random r;

    public void init(ScheduleProps properties, OptimizationProperties optimizationProperties) throws Exception {
        this.properties = properties;
        this.optProps = optimizationProperties;
        r = new Random(optProps.seed);

    }

    private static void printQuality(String prefix, ICostCalculator scorer, Schedule schedule) {
        System.out.format("Score:%6.2f Age:%3d %s\n", scorer.score(schedule), schedule.getAge(), prefix);
    }

    public Schedule optimizeMatchMatrix(Schedule schedule) throws Exception {
        List<Schedule> schedules = new ArrayList<>();
        schedules.add(schedule);
        final CostCalculatorMatchMatrix scorer = new CostCalculatorMatchMatrix(properties);
        for (OptimizationProperties.OptMatchMatrix optMatchMatrix : optProps.optMatchMatrix) {
            for (int i = 0; i < optMatchMatrix.loops; i++) {
                for (int j = 0; j < optMatchMatrix.swapTeams; j++) {
                    Schedule mutation = schedules.get(r.nextInt(schedules.size()));
                    mutation = MutationUtil.swapBetweenRaces(mutation, r);
                    if (!schedules.contains(mutation)) {
                        schedules.add(mutation);
                    }
                }
                for (int j = 0; j < optMatchMatrix.merges; j++) {
                    int idxFather = r.nextInt(schedules.size());
                    int idxMother = (idxFather + 1 + r.nextInt(schedules.size() - 1)) % schedules.size();
                    Schedule father = schedules.get(idxFather);
                    Schedule mother = schedules.get(idxMother);
                    Schedule mutation = MutationUtil.mutation(father, mother, r);
                    if (!schedules.contains(mutation)) {
                        schedules.add(mutation);
                    }
                }
                schedules.sort(Comparator.comparingDouble(scorer::scoreWithCache));
                if (schedules.size() > optMatchMatrix.individuals) {
                   /* for (Schedule schedule : schedules.subList(individuals, schedules.size())) {
                        hashes.remove(Integer.valueOf(schedule.hashCode()));
                    }*/
                    schedules = schedules.subList(0, optMatchMatrix.individuals);
                }
                if (i == optMatchMatrix.loops - 1 || i % (optMatchMatrix.loops / 10) == 0) {
                    System.out.println("------------  " + i + "  -----------------------");
                    //System.out.println("best1:" + scorer1.score(schedules.get(0)));
                    printQuality("best", scorer, schedules.get(0));
                    printQuality("middle", scorer, schedules.get(schedules.size() / 2));
                    printQuality("worst", scorer, schedules.get(schedules.size() - 1));
                    //System.out.println(saveFuel.score(properties, schedules.get(0)));
                    //Util.printMatchMatrix(properties, schedules.get(0));
                    Util.printCount(properties, schedules.get(0));
                }
                for (Schedule s : schedules) {
                    s.getOlder();
                }
            }
        }
        return schedules.get(0);

    }

    public Schedule optimizeBoatSchedule(Schedule schedule) throws Exception {
        List<Schedule> schedules = new ArrayList<>();
        schedules.add(schedule);
        for (OptimizationProperties.OptBoatUsage optBoatUsage : optProps.optBoatUsage) {
            final CostCalculatorBoatSchedule scorer = new CostCalculatorBoatSchedule(properties, optBoatUsage);
            for (int i = 0; i < optBoatUsage.loops; i++) {
                for (int j = 0; j < optBoatUsage.swapBoats; j++) {
                    Schedule mutation = schedules.get(r.nextInt(schedules.size()));
                    mutation = MutationUtil.swapBoats(mutation, r);
                    if (!schedules.contains(mutation)) {
                        schedules.add(mutation);
                    }
                }
                for (int j = 0; j < optBoatUsage.swapRaces; j++) {
                    Schedule mutation = schedules.get(r.nextInt(schedules.size()));
                    mutation = MutationUtil.swapRaces(mutation, r);
                    if (!schedules.contains(mutation)) {
                        schedules.add(mutation);
                    }
                }
                schedules.sort(Comparator.comparingDouble(scorer::scoreWithCache));
                if (schedules.size() > optBoatUsage.individuals) {
                   /* for (Schedule schedule : schedules.subList(individuals, schedules.size())) {
                        hashes.remove(Integer.valueOf(schedule.hashCode()));
                    }*/
                    schedules = schedules.subList(0, optBoatUsage.individuals);
                }
                if (i == optBoatUsage.loops - 1 || i % (optBoatUsage.loops / 10) == 0) {
                    System.out.println("------------  " + i + "  -----------------------");
                    //System.out.println("best1:" + scorer1.score(schedules.get(0)));
                    printQuality("best", scorer, schedules.get(0));
                    printQuality("middle", scorer, schedules.get(schedules.size() / 2));
                    printQuality("worst", scorer, schedules.get(schedules.size() - 1));
                    //System.out.println(saveFuel.score(properties, schedules.get(0)));
                    //Util.printMatchMatrix(properties, schedules.get(0));
                    Util.printCount(properties, schedules.get(0));
                }
                for (Schedule s : schedules) {
                    s.getOlder();
                }
            }
        }
        return schedules.get(0);

    }

    public void saveSchedule(Schedule schedule, File outSchedule, File outPdf, String markClub) throws Exception {
        if (outSchedule != null) {
            schedule.writeYaml(outSchedule);
        }
        if (outPdf != null) {
            new PdfCreator(properties, outPdf)
                    .createScheduleDistribution(schedule, true)
                    .createBoatDistribution(schedule)
                    .createPageSchedule(schedule, markClub == null ? "" : markClub)
                    .close();
        }

    }


    public static void main(String[] args) throws Exception {
        Environment environment = new Environment();
        OptimizationProperties optimizationProperties = OptimizationProperties.readYaml("optprops.yml");
        ScheduleProps properties = ScheduleProps.readYaml("dummy.yml");
        environment.init(properties, optimizationProperties);
        Schedule schedule = Util.getRandomSchedule(properties, new Random(optimizationProperties.seed));
        schedule = environment.optimizeMatchMatrix(schedule);
        System.out.println("before shuffle:");
        Util.printCount(properties, schedule);
        schedule = Util.shuffleBoats(schedule, new Random(optimizationProperties.seed));
        System.out.println("after shuffle:");
        Util.printCount(properties, schedule);
        schedule = environment.optimizeBoatSchedule(schedule);
        environment.saveSchedule(schedule, new File("winner.yml"), new File("res.pdf"), "RSC92");
    }
}
