package com.iremember.iremember;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class DirectionsParser {

    public static final int SHIFT = 5;
    public static final int ZERO = 0;
    public static final int INT = 63;
    public static final double DOUBLE = 1E5;
    public static final int HEX = 0x20;
    public static final int HEX2 = 0x1f;
    public static final int ONE = 1;

    public List<List<HashMap<String,String>>> parse(JSONObject jObject){
        List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String,String>>>() ;
        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;
        try {
            jRoutes = jObject.getJSONArray("routes");
            for(int i=ZERO;i<jRoutes.length();i++){
                jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                List path = new ArrayList<HashMap<String, String>>();
                for(int j=ZERO;j<jLegs.length();j++){
                    jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");
                    for(int k=ZERO;k<jSteps.length();k++){
                        String polyline = "";
                        polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);
                        for(int l=ZERO;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                            hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
        }
        return routes;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = ZERO, len = encoded.length();
        int lat = ZERO, lng = ZERO;
        while (index < len) {
            int b, shift = ZERO, result = ZERO;
            do {
                b = encoded.charAt(index++) - INT;
                result |= (b & HEX2) << shift;
                shift += SHIFT;
            } while (b >= HEX);
            int dlat = ((result & ONE) != ZERO ? ~(result >> ONE) : (result >> ONE));
            lat += dlat;
            shift = ZERO;
            result = ZERO;
            do {
                b = encoded.charAt(index++) - INT;
                result |= (b & HEX2) << shift;
                shift += SHIFT;
            } while (b >= HEX);
            int dlng = ((result & ONE) != ZERO ? ~(result >> ONE) : (result >> ONE));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / DOUBLE)),
                    (((double) lng / DOUBLE)));
            poly.add(p);
        }
        return poly;
    }
}
