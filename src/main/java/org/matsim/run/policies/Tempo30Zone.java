package org.matsim.run.policies;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Tempo30Zone {

    public static void implementPushMeasuresByModifyingNetworkInArea(Network network, List<PreparedGeometry> geometries) {
        Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.car)) //filter car links
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries)) //spatial filter
                .filter(link -> ! ((String)link.getAttributes().getAttribute("type")).contains("motorway") )//we won't change motorways and motorway_links
                .filter(link -> !((String)link.getAttributes().getAttribute("type")).contains("trunk") )
                .collect(Collectors.toSet());


        carLinksInArea.forEach(link -> {

            //TODO Check what free speed for 30km/h streets is used
            if(!link.getAttributes().getAttribute("type").equals("primary")){
                //apply 'tempo 30' to all roads but primary and motorways
                if(link.getFreespeed() > 7.5) link.setFreespeed(7.5); //27 km/h is used in the net for 30 km/h streets
            }
        });

    }

}
