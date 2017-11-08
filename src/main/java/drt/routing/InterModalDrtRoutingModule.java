package drt.routing;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import drt.run.DrtConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InterModalDrtRoutingModule implements RoutingModule {

    private final StageActivityTypes drtStageActivityType = new StageActivityTypesImpl();
    private final RoutingModule ptRouter;
    private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
    private final DrtConfigGroup drtconfig;
    private final Network network;
    private final Scenario scenario;
    @Inject
    public InterModalDrtRoutingModule(@Named(TransportMode.pt) RoutingModule ptRouter,
                                            @Named(DrtConfigGroup.DRT_MODE) TransitSchedule transitSchedule, Scenario scenario) {
        transitSchedule.getFacilities();
        this.ptRouter = ptRouter;
        this.stops = transitSchedule.getFacilities();
        this.drtconfig = (DrtConfigGroup)scenario.getConfig().getModules().get(DrtConfigGroup.GROUP_NAME);
        this.network = scenario.getNetwork();
        this.scenario = scenario;
    }

    @Override
    public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
        List<PlanElement> legList = new ArrayList<>();
        DrtRoutingModule drtRoutingModule = new DrtRoutingModule(scenario.getConfig(),network);
        TransitStopFacility accessFacility = findAccessFacility(fromFacility);
        if (accessFacility == null) {
            return (drtRoutingModule.calcRoute(fromFacility, toFacility, departureTime, person));
        }
        TransitStopFacility egressFacility = findEgressFacility(toFacility);
        if (egressFacility == null) {
            return (drtRoutingModule.calcRoute(fromFacility, toFacility, departureTime, person));
        }

        if (accessFacility.getLinkId() == egressFacility.getLinkId()) {
            return (drtRoutingModule.calcRoute(fromFacility, toFacility, departureTime, person));

        }
        legList.addAll(drtRoutingModule.calcRoute(fromFacility, accessFacility, departureTime, person));
        Leg drtLeg = (Leg)legList.get(0);
        Activity drtInt1 = scenario.getPopulation().getFactory()
                .createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, accessFacility.getCoord());
        drtInt1.setMaximumDuration(0);
        drtInt1.setLinkId(accessFacility.getLinkId());
        legList.add(drtInt1);

        legList.addAll(ptRouter.calcRoute(accessFacility,egressFacility,departureTime,person));

        Activity drtInt2 = scenario.getPopulation().getFactory()
                .createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, egressFacility.getCoord());
        drtInt2.setMaximumDuration(0);
        drtInt2.setLinkId(egressFacility.getLinkId());
        legList.add(drtInt2);
        legList.addAll(drtRoutingModule.calcRoute(egressFacility, toFacility,
                drtLeg.getDepartureTime() + drtLeg.getTravelTime() + 1, person));
        return legList;
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return drtStageActivityType;
    }

    private TransitStopFacility findAccessFacility(Facility<?> fromFacility) {
        Coord fromCoord = getFacilityCoord(fromFacility);
        TransitStopFacility accessFacility = findClosestStop(fromCoord);

        return accessFacility;
    }

    private TransitStopFacility findEgressFacility(Facility<?> toFacility) {
        Coord toCoord = getFacilityCoord(toFacility);
        TransitStopFacility stop = findClosestStop(toCoord);
        return stop;

    }


    private TransitStopFacility findClosestStop(Coord coord) {
        TransitStopFacility bestStop = null;
        double bestDist = Double.MAX_VALUE;
        for (TransitStopFacility stop : this.stops.values()) {
            double distance = CoordUtils.calcEuclideanDistance(coord, stop.getCoord());
            if (distance <= drtconfig.getMaxWalkDistance()) {
                if (distance<bestDist){
                    bestDist = distance;
                    bestStop = stop;
                }

            }

        }
        return bestStop;
    }

    Coord getFacilityCoord(Facility<?> facility) {
        Coord coord = facility.getCoord();
        if (coord == null) {
            coord = network.getLinks().get(facility.getLinkId()).getCoord();
            if (coord == null)
                throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
        }
        return coord;
    }

}
