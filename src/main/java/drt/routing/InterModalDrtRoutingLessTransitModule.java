package drt.routing;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import drt.run.DrtConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InterModalDrtRoutingLessTransitModule implements RoutingModule {

    private final StageActivityTypes drtStageActivityType = new StageActivityTypesImpl();
    private final RoutingModule ptRouter;
    private final RoutingModule walkRouter;
    private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
    private final DrtConfigGroup drtconfig;
    private final Network network;
    private final Scenario scenario;

    @Inject
    public InterModalDrtRoutingLessTransitModule(@Named(TransportMode.pt) RoutingModule ptRouter, @Named(TransportMode.walk) RoutingModule walkRouter,
                                      @Named(DrtConfigGroup.DRT_MODE) TransitSchedule transitSchedule, Scenario scenario) {
        transitSchedule.getFacilities();
        this.ptRouter = ptRouter;
        this.walkRouter = walkRouter;
        this.stops = transitSchedule.getFacilities();
        this.drtconfig = (DrtConfigGroup) scenario.getConfig().getModules().get(DrtConfigGroup.GROUP_NAME);
        this.network = scenario.getNetwork();
        this.scenario = scenario;
    }


    @Override
    public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
        List<PlanElement> legList = new ArrayList<>();
        DrtRoutingModule drtRoutingModule = new DrtRoutingModule(scenario.getConfig(),network);
        List<? extends PlanElement> ptLegs = ptRouter.calcRoute(fromFacility,toFacility,departureTime,person);
        if (ptLegs.size() == 1){
            return walkRouter.calcRoute(fromFacility,toFacility,departureTime,person);
        }
        int index = findBestPt(ptLegs);
        Facility accessFacility = findClosestStop(((Activity) ptLegs.get(index-1)).getCoord());
        Facility egressFacility = findClosestStop(((Activity) ptLegs.get(index+1)).getCoord());
        List<? extends PlanElement> drtLeg1 = drtRoutingModule.calcRoute(fromFacility, accessFacility,departureTime,person);
        List<? extends PlanElement> drtLeg2 = drtRoutingModule.calcRoute(egressFacility,toFacility,departureTime,person);
        legList.addAll(drtLeg1);
        legList.addAll(ptRouter.calcRoute(accessFacility,egressFacility,departureTime,person));
        legList.addAll(drtLeg2);
        return legList;
    }

    private int findBestPt(List<? extends PlanElement> ptLegs) {
        int i = 0;
        int bestIdx = 0;
        Leg bestPt = null;
        for (PlanElement planElement:ptLegs){
            if (planElement instanceof Leg){
               if (((Leg) planElement).getMode().equals(TransportMode.pt)){
                   if (bestPt == null ){
                       bestPt = (Leg) planElement;
                       bestIdx = i;
                   }else{
                       if (bestPt.getTravelTime() < ((Leg) planElement).getTravelTime()) {
                           bestPt = (Leg) planElement;
                           bestIdx = i;
                       }
                   }
               }
            }
            i++;
        }
        return bestIdx;
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return drtStageActivityType;
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


}