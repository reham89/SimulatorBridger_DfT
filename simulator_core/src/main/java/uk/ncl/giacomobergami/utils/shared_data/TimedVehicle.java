package uk.ncl.giacomobergami.utils.shared_data;

//import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
//import org.cloudbus.cloudsim.edge.core.edge.Mobility;

import uk.ncl.giacomobergami.utils.gir.CartesianPoint;

import java.util.Objects;

public class TimedVehicle implements CartesianPoint {
    public String id;
    public double x;
    public double y;
    public double angle;
    public String type;
    public double speed;
    public double pos;
    public String lane;
    public double slope;
    public double simtime;

    public double getSimtime() {
        return simtime;
    }

    public void setSimtime(double simtime) {
        this.simtime = simtime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    @Override
    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getPos() {
        return pos;
    }

    public void setPos(double pos) {
        this.pos = pos;
    }

    public String getLane() {
        return lane;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimedVehicle vehicle = (TimedVehicle) o;
        return Double.compare(vehicle.x, x) == 0 && Double.compare(vehicle.y, y) == 0 && Double.compare(vehicle.angle, angle) == 0 && Double.compare(vehicle.speed, speed) == 0 && Double.compare(vehicle.pos, pos) == 0 && Double.compare(vehicle.slope, slope) == 0 && Objects.equals(id, vehicle.id) && Objects.equals(type, vehicle.type) && Objects.equals(lane, vehicle.lane);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, angle, type, speed, pos, lane, slope);
    }
}
