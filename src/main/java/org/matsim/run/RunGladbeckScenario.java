package org.matsim.run;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.prepare.AssignIncome;
import org.matsim.run.policies.PtFlatrate;
import org.matsim.run.policies.SchoolRoadsClosure;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import java.util.HashMap;
import java.util.List;

@CommandLine.Command(header = ":: Gladbeck Scenario ::", version = "v1.0")
public class RunGladbeckScenario extends RunMetropoleRuhrScenario {

    private static final Logger log = Logger.getLogger(RunGladbeckScenario.class);

    @CommandLine.Option(names = "--ptFlat", defaultValue = "false", description = "measures to allow car users to have free pt")
    private boolean ptFlat;

    @CommandLine.Option(names = "--schoolClosure", defaultValue = "false", description = "measures to ban car on certain streets")
    boolean schoolClosure;

    static HashMap<Id<Person>, Integer> personsEligibleForPtFlat = new HashMap<>();

    public RunGladbeckScenario() {
        super("./scenarios/gladbeck-v1.0/input/gladbeck-v1.0-25pct.config.xml");
    }

    public static void main(String args []) {MATSimApplication.run(RunGladbeckScenario.class, args);}

        @Override
        public Config prepareConfig(Config config) {
            var preparedConfig = super.prepareConfig(config);
            log.info("changing config");
            preparedConfig.controler().setLastIteration(5);

            // TODO: create separate config
            preparedConfig.network().setInputFile("/Users/gregorr/Desktop/Test_ScenarioCutOut/network_reduced.xml");
            preparedConfig.plans().setInputFile("/Users/gregorr/Desktop/Test_ScenarioCutOut/population_reduced.xml");
            return preparedConfig;
        }

        @Override
        protected void prepareScenario(Scenario scenario) {
            super.prepareScenario(scenario);
            log.info("Adding income attribute to the population");
            AssignIncome.assignIncomeToPersonSubpopulationAccordingToSNZData(scenario.getPopulation());

            if (ptFlat) {
                for (Person p: scenario.getPopulation().getPersons().values()) {
                    Plan plan = p.getSelectedPlan();
                    List<Leg> legs = TripStructureUtils.getLegs(plan);
                    //only car users are allowed to enjoy the pt flatrate
                    for (Leg leg: legs) {
                        if (leg.getMode().equals(TransportMode.car)) {
                            personsEligibleForPtFlat.put(p.getId(),0);
                        }
                    }
                }
                log.info("Number of Agents eligible for pt flat: " + personsEligibleForPtFlat.size());
            }

            if (schoolClosure) {
                new SchoolRoadsClosure().closeSchoolRoads(scenario.getNetwork());
            }
        }

        @Override
        protected void prepareControler(Controler controler) {
            super.prepareControler(controler);
            //use income-dependent marginal utility of money for scoring
            log.info("Using income dependent scoring");
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);
                }
            });

            if (ptFlat) {
                log.info("using pt flat");
                double ptConstant = controler.getConfig().planCalcScore().getScoringParameters("person").getModes().get(TransportMode.pt).getConstant();
                log.info("pt constant is: " +ptConstant);
                double ptDailyMonetaryConstant = controler.getConfig().planCalcScore().getScoringParameters("person").getModes().get(TransportMode.pt).getDailyMonetaryConstant();
                log.info("daily monetary constant is: " + ptDailyMonetaryConstant);
                PtFlatrate ptFlatrate = new PtFlatrate(personsEligibleForPtFlat, -1*ptConstant, -1*ptDailyMonetaryConstant);
                controler.addOverridingModule(new AbstractModule() {
                    @Override
                    public void install() {
                        addEventHandlerBinding().toInstance(ptFlatrate);
                        addControlerListenerBinding().toInstance(ptFlatrate);
                        install(new PersonMoneyEventsAnalysisModule());
                    }
                });
            }
        }
}
