package de.haug.sensor_location;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LocatorTest {

    Sensor sensor1;
    WirelessContact wirelessContact1_0;
    WirelessContact wirelessContact1_1;
    WirelessContact wirelessContact1_2;
    WirelessContact wirelessContact2_0;
    WirelessContact wirelessContact2_1;
    WirelessContact wirelessContact2_2;
    WirelessContact wirelessContact4_0;
    WirelessContact wirelessContact4_1;
    WirelessContact wirelessContact4_2;
    WirelessContact wirelessContactS3_0;
    WirelessContact wirelessContactS3_1;
    WirelessContact wirelessContactS2_0;
    WirelessContact wirelessContactS2_1;
    Package p1;
    Package p2;
    Package p3;
    Package p4;
    Package p5;
    Package p6;
    Package p7;
    Package p8;
    Package p9;
    Package p10;
    Package p11;
    Package p12;
    Package p13;
    Package p14;
    Package p15;
    Package p16;
    Package p17;
    Package p18;
    Package p19;
    Package p20;
    Relay r1;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        sensor1 = new Sensor(2);

        wirelessContact1_0 = new WirelessContact(1001, .1f);
        wirelessContact1_1 = new WirelessContact(1001, .7f);
        wirelessContact1_2 = new WirelessContact(1001, .95f);

        wirelessContact2_0 = new WirelessContact(1002, .1f);
        wirelessContact2_1 = new WirelessContact(1002, .7f);
        wirelessContact2_2 = new WirelessContact(1002, .95f);

        wirelessContact4_0 = new WirelessContact(1004, .1f);
        wirelessContact4_1 = new WirelessContact(1004, .7f);
        wirelessContact4_2 = new WirelessContact(1004, .95f);

        wirelessContactS3_0 = new WirelessContact(3, .3f);
        wirelessContactS3_1 = new WirelessContact(3, .7f);

        wirelessContactS2_0 = new WirelessContact(2, .3f);
        wirelessContactS2_1 = new WirelessContact(2, .7f);

        p1 = new Package(2, 1, wirelessContact1_2);
        p2 = new Package(2, 3, wirelessContact1_1);
        p3 = new Package(2, 4, wirelessContact1_0);
        p4 = new Package(2, 6);
        p5 = new Package(2, 7);
        p6 = new Package(2, 8);
        p13 = new Package(2, 15);
        p14 = new Package(2, 19);
        p15 = new Package(2, 21);
        p7 = new Package(2, 9, wirelessContact2_0);
        p8 = new Package(2, 10, wirelessContact2_1);
        p19 = new Package(2, 11, wirelessContact2_1);
        p20 = new Package(2, 14, wirelessContact2_0);
        p9 = new Package(2, 14, wirelessContact2_2);
        p10 = new Package(2, 9, wirelessContact4_0);
        p11 = new Package(2, 10, wirelessContact4_1);
        p12 = new Package(2, 11, wirelessContact4_2);
        p16 = new Package(2, 22, wirelessContact4_0);
        p17 = new Package(2, 23, wirelessContact4_1);
        p18 = new Package(2, 24, wirelessContact4_2);
        r1 = new Relay(1001);
    }

    @org.junit.jupiter.api.Test
    void addToEpochs() throws EpochException {
        var p = new Package(1, 1);
        Locator.addToEpochs(sensor1, p, Epoch.EpochType.RELAY_APPROACH);
        assertTrue(sensor1.mysteryEpochs.size() == 1);
        assertTrue(sensor1.mysteryEpochs.get(0).getLatest().equals(p));
    }

    @org.junit.jupiter.api.Test
    void addToExistingEpoch() throws EpochException {
        var p = new Package(1, 1);
        var p2 = new Package(1, 2);
        Locator.addToEpochs(sensor1, p, Epoch.EpochType.RELAY_APPROACH);
        Locator.addToEpochs(sensor1, p2, Epoch.EpochType.RELAY_APPROACH);
        assertTrue(sensor1.mysteryEpochs.size() == 1);
        assertTrue(sensor1.mysteryEpochs.get(0).getPackages().size() == 2);
        assertTrue(sensor1.mysteryEpochs.get(0).getLatest().equals(p2));
    }

    @org.junit.jupiter.api.Test
    void clearSensorEpochsPlain() throws Exception {
        var locator = new Locator();
        locator.sensors.put(2L, sensor1);
        p1.setPosition(new Position(r1, null, 0, 0));

        Locator.addToEpochs(sensor1, p1, Epoch.EpochType.RELAY_APPROACH);
        var result = locator.clearSensorEpochs(sensor1);
        assertEquals(0, result.size());
        assertEquals(1, sensor1.mysteryEpochs.size());
    }

    @org.junit.jupiter.api.Test
    void feedIntegrationTest() throws Exception {
        var locator = new Locator();
        assertEquals(0, locator.feed(p1).size());
        assertEquals(1, locator.sensors.size());
        assertEquals(1, locator.sensors.get(2L).mysteryEpochs.size());
        assertEquals(0, locator.feed(p2).size());
        assertEquals(0, locator.feed(p3).size());
        assertEquals(1, locator.sensors.get(2L).mysteryEpochs.size());
        assertEquals(3, locator.sensors.get(2L).mysteryEpochs.get(0).getPackages().size());
        assertEquals(locator.sensors.get(2L).mysteryEpochs.get(0).getType(), Epoch.EpochType.RELAY_WITHDRAWAL);
        assertEquals(0, locator.feed(p4).size());
        assertEquals(2, locator.sensors.get(2L).mysteryEpochs.size());
        assertEquals(locator.sensors.get(2L).mysteryEpochs.get(1).getType(), Epoch.EpochType.VOYAGE);
        assertEquals(0, locator.feed(p5).size());
        assertEquals(0, locator.feed(p6).size());
        assertEquals(3, locator.sensors.get(2L).mysteryEpochs.get(1).getPackages().size());
        assertEquals(0, locator.feed(p7).size());
        assertEquals(3, locator.sensors.get(2L).mysteryEpochs.size());
        assertEquals(locator.sensors.get(2L).mysteryEpochs.get(2).getType(), Epoch.EpochType.RELAY_APPROACH);
        assertEquals(0, locator.feed(p8).size());
        var result = locator.feed(p9);
        assertEquals(9, result.size());
        assertEquals(0, locator.sensors.get(2L).mysteryEpochs.size());
        assertEquals(p9.getTimestamp(), locator.sensors.get(2L).lastEpochEnd);

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).position.getPositionInBetween() <= result.get(i).position.getPositionInBetween());
            assertEquals(result.get(i - 1).position.getTotalDistance(), result.get(i).position.getTotalDistance());
        }

    }

    @org.junit.jupiter.api.Test
    void feedIntegrationTest2() throws Exception {
        var locator = new Locator();
        assertEquals(0, locator.feed(p1).size());
        assertEquals(0, locator.feed(p2).size());
        assertEquals(0, locator.feed(p3).size());
        assertEquals(0, locator.feed(p4).size());
        assertEquals(0, locator.feed(p5).size());
        assertEquals(0, locator.feed(p6).size());
        assertEquals(0, locator.feed(p10).size());
        assertEquals(0, locator.feed(p11).size());
        var result = locator.feed(p12);
        assertEquals(9, result.size());
        assertEquals(0, locator.sensors.get(2L).mysteryEpochs.size());

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).position.getPositionInBetween() <= result.get(i).position.getPositionInBetween());
            assertEquals(150, result.get(i).position.getTotalDistance());
        }

    }

    @org.junit.jupiter.api.Test
    void feedTwoRelayTest() throws Exception {
        var locator = new Locator();
        assertEquals(0, locator.feed(p1).size());
        assertEquals(0, locator.feed(p2).size());
        assertEquals(0, locator.feed(p3).size());
        assertEquals(0, locator.feed(p4).size());
        assertEquals(0, locator.feed(p5).size());
        assertEquals(0, locator.feed(p6).size());
        assertEquals(0, locator.feed(p7).size());
        assertEquals(0, locator.feed(p8).size());
        var result = locator.feed(p9);
        assertEquals(9, result.size());
        assertEquals(0, locator.sensors.get(2L).mysteryEpochs.size());

        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).position.getPositionInBetween() <= result.get(i).position.getPositionInBetween());
            assertEquals(100, result.get(i).position.getTotalDistance());
        }

        assertEquals(0, locator.feed(p13).size());
        assertEquals(0, locator.feed(p14).size());
        assertEquals(0, locator.feed(p15).size());

        assertEquals(0, locator.feed(p16).size());
        assertEquals(0, locator.feed(p17).size());
        assertEquals(6, locator.feed(p18).size());
    }


    @org.junit.jupiter.api.Test
    void feedNoFullStrength() throws Exception {
        var locator = new Locator();
        assertEquals(0, locator.feed(p2).size());
        assertEquals(0, locator.feed(p3).size());
        assertEquals(0, locator.feed(p4).size());
        assertEquals(0, locator.feed(p5).size());
        assertEquals(0, locator.feed(p6).size());
        assertEquals(0, locator.feed(p7).size());
        assertEquals(0, locator.feed(p8).size());

        var res1 = locator.feed(p19);

        for (int i = 1; i < res1.size(); i++) {
            assertTrue(res1.get(i - 1).position.getPositionInBetween() <= res1.get(i).position.getPositionInBetween());
            assertEquals(100, res1.get(i).position.getTotalDistance());
            assertEquals(1001L, res1.get(i).position.getStart().id);
            assertEquals(1002L, res1.get(i).position.getDest().id);
        }

        assertEquals(7, res1.size());
        assertEquals(0, locator.feed(p20).size());

        assertEquals(0, locator.feed(p13).size());
        assertEquals(0, locator.feed(p14).size());
        assertEquals(0, locator.feed(p15).size());

        assertEquals(0, locator.feed(p16).size());
        assertEquals(0, locator.feed(p17).size());
        var res2 = locator.feed(p18);
        for (int i = 1; i < res2.size(); i++) {
            assertTrue(res2.get(i - 1).position.getPositionInBetween() <= res2.get(i).position.getPositionInBetween());
            assertEquals(50, res2.get(i).position.getTotalDistance());
            assertEquals(1002L, res2.get(i).position.getStart().id);
            assertEquals(1004L, res2.get(i).position.getDest().id);
        }

        assertEquals(8, res2.size());
    }

    @org.junit.jupiter.api.Test
    void feedRendezVous() throws Exception {
        var locator = new Locator();

        // Start sensor two first and sensor three second
        assertEquals(0, locator.feed(p1).size());
        assertEquals(0, locator.feed(p2).size());
        assertEquals(0, locator.feed(p3).size());

        assertEquals(0, locator.feed(new Package(3, 5, wirelessContact1_2)).size());
        assertEquals(0, locator.feed(new Package(3, 6, wirelessContact1_0)).size());

        // Sensor three comes in first at 1002 and transmits data about its rdv with two
        assertEquals(0, locator.feed(new Package(3, 7)).size());
        assertEquals(0, locator.feed(new Package(3, 8, wirelessContactS2_0)).size());
        assertEquals(0, locator.feed(new Package(3, 10, wirelessContactS2_1)).size());
        assertEquals(0, locator.feed(new Package(3, 12, wirelessContactS2_0)).size());

        assertEquals(0, locator.feed(new Package(3, 13)).size());
        assertEquals(0, locator.feed(new Package(3, 14, wirelessContact2_0)).size());
        assertEquals(0, locator.feed(new Package(3, 15, wirelessContact2_1)).size());
        var resultS3 = locator.feed(new Package(3, 16, wirelessContact2_2));
        assertEquals(10, resultS3.size());
        
        // Sensor two arrived now as well
        assertEquals(0, locator.feed(new Package(2, 5)).size());
        assertEquals(0, locator.feed(new Package(2, 6)).size());
        assertEquals(0, locator.feed(new Package(2, 7)).size());
        assertEquals(0, locator.feed(new Package(2, 8, wirelessContactS3_0)).size());
        assertEquals(0, locator.feed(new Package(2, 10, wirelessContactS3_1)).size());
        assertEquals(0, locator.feed(new Package(2, 12, wirelessContactS3_0)).size());
        assertEquals(0, locator.feed(new Package(2, 13)).size());
        assertEquals(0, locator.feed(new Package(2, 15)).size());
        assertEquals(0, locator.feed(new Package(2, 16, wirelessContact2_0)).size());
        assertEquals(0, locator.feed(new Package(2, 18, wirelessContact2_1)).size());
        var resultS2 = locator.feed(new Package(2, 20, wirelessContact2_2));
        assertEquals(14, resultS2.size());

        // Expect location of slower sensor to be equal to location of second sensor at time of Rdv
        assertEquals(resultS3.get(4).getPosition().getPositionInBetween(),
                resultS2.get(7).getPosition().getPositionInBetween(), .1);
    }

    @org.junit.jupiter.api.Test
    void relayDistanceTest() throws Exception {
        var t = new TopologyAnalyzer();
        assertEquals(100, t.getDistance(1001, 1002));
        assertEquals(150, t.getDistance(1001, 1004));
    }

    @org.junit.jupiter.api.Test
    void getGraphEdgePositionTest() throws Exception {
        var ta = new TopologyAnalyzer();
        var p = new Position(ta.getRelay(1001), ta.getRelay(1004), 110, 150);
        var r = ta.getGraphEdgePosition(p);
        assertEquals(1002L, r.getStart().getId());
        assertEquals(1004L, r.getDest().getId());
        assertEquals(50, r.getTotalDistance(), .001);
        assertEquals(10, r.getPositionInBetween(), .001);
    }
}