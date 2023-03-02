package util;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    private static volatile Settings INSTANCE = null;
    private List<LatLng> boundaryPoints = new ArrayList<>();

    private Settings() {
    }

    public static Settings getInstance() {
        if (INSTANCE == null) {
            synchronized (Settings.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Settings();
                }
            }
        }
        return INSTANCE;
    }

    public List<LatLng> getBoundaryPoints() {
        List<LatLng> pointsToReturn = new ArrayList<>();
        for (LatLng point : boundaryPoints) {
            LatLng position = new LatLng(point.latitude, point.longitude);
            pointsToReturn.add(position);
        }
        return pointsToReturn;
    }

    public void setBoundaryPoints(List<LatLng> boundaryPoints) {
        this.boundaryPoints.clear();
        for (LatLng point : boundaryPoints) {
            LatLng position = new LatLng(point.latitude, point.longitude);
            this.boundaryPoints.add(position);
        }
    }
}
