package com.felicekarl.foragingtech.views.fragments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.mapgenerator.JobTheme;

import com.felicekarl.foragingtech.R;
import com.nutiteq.components.Components;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Options;
import com.nutiteq.components.Vector;
import com.nutiteq.datasources.raster.MapsforgeRasterDataSource;
import com.nutiteq.editable.EditableMapView;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Marker;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.rasterlayers.RasterLayer;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.ui.Label;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;
import com.nutiteq.vectorlayers.MarkerLayer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ZoomControls;

public class MapFragment extends BaseFragment implements LocationListener {
	private static final String TAG = MapFragment.class.getSimpleName();
	
	private static final String MAP_FILE = "MAP_FILE";
	private String mapFile;
	
	//The minimum distance to change updates in metters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //10 metters
	
	//The minimum time beetwen updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
	
	private EditableMapView mapView;
	private RasterLayer mapLayer;
	private MarkerLayer markerLayer;
	private GeometryLayer geoLayer;
	
	private MarkerStyle markerStyle;
	private MarkerStyle markerStyle2;
	private StyleSet<PointStyle> dronePointStyleSet;
	private StyleSet<PointStyle> userPointStyleSet;
	private StyleSet<PointStyle> targetPointStyleSet;
	
	private Point droneCurPoint;
	private Point userCurPoint;
	private MapPos[] startingPos;
	private Line targetLine;
	private ArrayList<MapPos> targets;
	//private Point primaryTargetPoint;
	
//	private Marker droneCurLocMarker;
//	private Marker droneTargetMarker;
	
	private Location location;
	private LocationManager locationManager;
	private boolean isGPSEnabled;
	private boolean isNetworkEnabled;
	private double latitude = -84.3917223d;
	private double longitude = 33.7704934d;
	
	public MapFragment() {
    	
    }
	
	public static MapFragment create() {
		return new MapFragment();
	}
	
	public static MapFragment create(String mapFile) {
		MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(MAP_FILE, mapFile);
        fragment.setArguments(args);
        return fragment;
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        	mapFile = getArguments().getString(MAP_FILE);
        }
        if (mapFile == null || mapFile == "") {
        	File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/maps", "blank.map");
        	if (file.exists()) {
        		mapFile = file.getAbsolutePath();
        	} else {
        		int resourceId = getActivity().getResources().getIdentifier("delaware", "raw", getActivity().getPackageName());
            	InputStream is = getActivity().getResources().openRawResource(resourceId);
            	try {
            		OutputStream out = new FileOutputStream(file);
                    byte buf[] = new byte[1024];
                    int len;
                    while((len = is.read(buf)) > 0) {
                    	out.write(buf,0,len);
                    }
                    out.close();
                    is.close();
                    mapFile = file.getAbsolutePath();
            	} catch (IOException e){
            		e.printStackTrace();
            	}
            	
        	}
        }
        Log.d(TAG, "mapFile: " + mapFile);
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	view = (ViewGroup) inflater.inflate(R.layout.fragment_map, container, false);
    	
    	initMapView(mapFile);
    	
    	return view;
    }
	
	public void initMapView(String mapFile) {
		Log.d(TAG, "mapFile: " + mapFile);
		// 1. Get the MapView from the Layout xml - mandatory
        mapView = (EditableMapView) view.findViewById(R.id.mapView);
        
        // 2. create and set MapView components - mandatory
        Components components = new Components();
        // set stereo view: works if you rotate to landscape and device has HTC 3D or LG Real3D
        mapView.setComponents(components);

        // 3. Define map layer for basemap - mandatory.
        // read filename from extras
//        Bundle b = getIntent().getExtras();
//        String mapFile = b.getString("selectedFile");
        //String mapFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/map/georgia.map";

        JobTheme renderTheme = MapsforgeRasterDataSource.InternalRenderTheme.OSMARENDER;
        MapsforgeRasterDataSource dataSource = new MapsforgeRasterDataSource(new EPSG3857(), 0, 20, mapFile, renderTheme);
        mapLayer = new RasterLayer(dataSource, 1044);

        mapView.getLayers().setBaseLayer(mapLayer);
        
        // update User Position 
     // initialize location manager
     		locationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);
     		// getting GPS status
     		isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
     		// getting network status
     		isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
     		
     		if (!isGPSEnabled && !isNetworkEnabled) {
     			// no network provider is enabled
     		} else {
     			// if GPS Enabled get lat/long using GPS Services
     			if (isGPSEnabled) {
     				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
     						MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
     				//Log.d(TAG, "GPS Enabled");
     				if (locationManager != null) {
     					location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
     				}
     			} else if (isNetworkEnabled) {
     		         locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
     		         		MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
     		         //Log.d(TAG, "Network Enabled");
     		         if (locationManager != null) {
     		             location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
     		         }
     		     }
     		     if (location != null) {
     		         latitude = location.getLatitude();
     		         longitude = location.getLongitude();
     		         //Log.d(TAG, "latitude: " + latitude + " | longitude" + longitude);
     		     }
     		}
        
		
    
        // set initial map view camera from database
        MapPos mapCenter = new MapPos(longitude, latitude,dataSource.getMapDatabase().getMapFileInfo().startZoomLevel);
        //Log.d(TAG, "center: " + mapCenter);
        
        
//        Marker marker = new Marker(mapCenter, null, normalMarkerStyle, new MarkerState(mapCenter, "Current Location"));
//		markerLayer.add(marker);
        
        mapView.setFocusPoint(mapView.getLayers().getBaseLayer().getProjection().fromWgs84(mapCenter.x,mapCenter.y));
        mapView.setZoom((float) mapCenter.z * 1.4f);

        // rotation - 0 = north-up
        mapView.setMapRotation(0f);
        // tilt means perspective view. Default is 90 degrees for "normal" 2D map view, minimum allowed is 30 degrees.
        mapView.setTilt(90.0f);

        // Activate some mapview options to make it smoother - optional
        mapView.getOptions().setPreloading(false);
        mapView.getOptions().setSeamlessHorizontalPan(true);
        mapView.getOptions().setTileFading(false);
        mapView.getOptions().setKineticPanning(true);
        mapView.getOptions().setDoubleClickZoomIn(true);
        mapView.getOptions().setDualClickZoomOut(true);

        // set sky bitmap - optional, default - white
        mapView.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
        mapView.getOptions().setSkyOffset(4.86f);
        mapView.getOptions().setSkyBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.sky_small));

        // Map background, visible if no map tiles loaded - optional, default - white
        mapView.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
        mapView.getOptions().setBackgroundPlaneBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.background_plane));
        mapView.getOptions().setClearColor(Color.WHITE);

        // configure texture caching - optional, suggested
        mapView.getOptions().setTextureMemoryCacheSize(20 * 1024 * 1024);
        mapView.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);

        // define online map persistent caching - optional, suggested. Default - no caching
        //  mapView.getOptions().setPersistentCachePath(this.getDatabasePath("mapcache").getPath());
        // set persistent raster cache limit to 100MB
        mapView.getOptions().setPersistentCacheSize(100 * 1024 * 1024);

//      mapView.getOptions().setRasterTaskPoolSize(1);

        // 4. zoom buttons using Android widgets - optional
        // get the zoomcontrols that was defined in main.xml
        ZoomControls zoomControls = (ZoomControls) view.findViewById(R.id.zoomcontrols);
        // set zoomcontrols listeners to enable zooming
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                mapView.zoomIn();
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                mapView.zoomOut();
            }
        });
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState); 
		// Create marker layer
        markerLayer = new MarkerLayer(mapLayer.getProjection());
        mapView.getLayers().addLayer(markerLayer);

        
//        // Styles for markers (for drone)
//        markerStyle = MarkerStyle.builder().setSize(0.4f).setBitmap(
//            UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.marker_north_1)
//          ).build();
//        
//        // Styles for markers (target)
//        markerStyle2 = MarkerStyle.builder().setSize(0.4f).setBitmap(
//            UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.marker_red)
//          ).build();
        
        // define label what is shown when you click on marker
        Label droneLabel = new DefaultLabel("Parrot", "Here is your parrot.");
        Label userLabel = new DefaultLabel("Destination", "Here is your location.");

        // define location of the marker, it must be converted to base map coordinate system
        MapPos markerLocation = mapLayer.getProjection().fromWgs84(longitude, latitude);
//        markerLocation = new MapPos(markerLocation.x, markerLocation.y, 10d);
        
        
        
        mapView.getLayers().addLayer(markerLayer);
        // drone point
        dronePointStyleSet = new StyleSet<PointStyle>();
		Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.marker_point_north_1);
		PointStyle pointStyle = PointStyle.builder()
		         .setBitmap(pointMarker).setSize(0.5f).setColor(Color.BLACK).setPickingSize(1f)
		         .build();
		dronePointStyleSet.setZoomStyle(0, pointStyle);

		geoLayer = new GeometryLayer(mapView.getLayers().getBaseProjection());
		mapView.getLayers().addLayer(geoLayer);
		
		droneCurPoint = new Point(markerLocation, droneLabel, dronePointStyleSet, new MarkerState(markerLocation, "Drone"));
		geoLayer.add(droneCurPoint);
		
		// user point
		userPointStyleSet = new StyleSet<PointStyle>();
		pointMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.marker_point);
		pointStyle = PointStyle.builder()
		         .setBitmap(pointMarker).setSize(0.2f).setColor(Color.parseColor("#a20025")).setPickingSize(1f)
		         .build();
		userPointStyleSet.setZoomStyle(0, pointStyle);
		
		userCurPoint = new Point(markerLocation, userLabel, userPointStyleSet, new MarkerState(markerLocation, "User"));
		geoLayer.add(userCurPoint);
		
		// line style (target points)
		
		targetPointStyleSet = new StyleSet<PointStyle>();
		pointMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.marker_point);
		pointStyle = PointStyle.builder()
		         .setBitmap(pointMarker).setSize(0.15f).setColor(Color.parseColor("#fa6800")).setPickingSize(1f)
		         .build();
		targetPointStyleSet.setZoomStyle(0, pointStyle);
		
		StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
		Bitmap lineMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.marker_line_arrow);
        lineStyleSet.setZoomStyle(0, LineStyle.builder().setBitmap(lineMarker).setWidth(0.1f).setColor(Color.parseColor("#f0a30a")).setPointStyle(pointStyle).build());
		
        // reset path
        startingPos = new MapPos[3];
		startingPos[0] = mapLayer.getProjection().fromWgs84(longitude, latitude);
		startingPos[1] = mapLayer.getProjection().fromWgs84(longitude - 0.0002d, latitude + 0.0002d);
		startingPos[2] = mapLayer.getProjection().fromWgs84(longitude + 0.0002d, latitude + 0.0002d);
		targets = new ArrayList<MapPos>();
		targets.add(startingPos[0]);
		targets.add(startingPos[1]);
		targets.add(startingPos[2]);
		targets.add(startingPos[0]);
		
		targetLine = new Line(targets, new DefaultLabel("Path"), lineStyleSet, null);
		geoLayer.add(targetLine);
		
		
        mapView.setElementListener(new EditableMapView.EditEventListener() {
        	VectorElement selectedElement;
            VectorElement dragElement;
            
			@Override
			public void updateUI() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Vector snapElement(VectorElement element, Vector delta) {
				return delta;
			}

			@Override
			public MapPos snapElementVertex(VectorElement element, int index,
					MapPos mapPos) {
				return mapPos;
			}

			@Override
			public void onElementCreated(VectorElement element) {
				Log.d(TAG, "onElementCreated");
//				Point point = (Point) element;
//	            point.setStyleSet(pointStyleSet);
//	            layer.add(point);
			}

			@Override
			public void onBeforeElementChange(VectorElement element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onElementChanged(VectorElement element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onElementDeleted(VectorElement element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean onElementSelected(VectorElement element) {
				Log.d(TAG, "onElementSelected");
				if ( ((MarkerState) droneCurPoint.userData).equals(((MarkerState) element.userData)) ) {
					selectedElement = null;
					return false;
				} else if ( ((MarkerState) userCurPoint.userData).equals(((MarkerState) element.userData)) ) {
					selectedElement = null;
					return false;
				}
				
				
				selectedElement = element;
                return true;
			}

			@Override
			public void onElementDeselected(VectorElement element) {
				selectedElement = null;
			}

			@Override
			public void onDragStart(VectorElement element, float x, float y) {
				//Log.d(TAG, "element: " + element.toString());
				//Log.d(TAG, "OnDrageStart: x: " +  x + " | y: " + y);
				if ( ((MarkerState) droneCurPoint.userData).equals(((MarkerState) element.userData)) ) {
				
				} else if ( ((MarkerState) userCurPoint.userData).equals(((MarkerState) element.userData)) ) {
					
				} else if (element instanceof Line) {
					dragElement = null;
				} else {
					dragElement = element;
				}
				
			}

			@Override
			public void onDrag(float x, float y) {
				//Log.d(TAG, "onDrag: x: " +  x + " | y: " + y);
				
				Rect rect = new Rect();
			}

			@Override
			public boolean onDragEnd(float x, float y) {
				//Log.d(TAG, "onDragEnd: x: " +  x + " | y: " + y);
				if (selectedElement instanceof Point) {
					Log.d(TAG, "drag Point");
				}
//				if (selectedElement instanceof Line) {
//					ArrayList<MapPos> result = new ArrayList<MapPos>();
//					result.add(startingPos[0]);
//					List<MapPos> list = ((Line) selectedElement).getVertexList();
//					for (int i=1; i<list.size()-1; i++) {
//						result.add(list.get(i));
//					}
//					result.add(startingPos[0]);
//					((Line) selectedElement).setVertexList(result);
//					((Line) selectedElement).calculateInternalState();
//				}
				dragElement = null;
                Rect rect = new Rect();
                return rect.contains((int) x, (int) y);
			}
        	
        });
        
        //Map<String, String> userData = new HashMap<String, String>();
        mapView.createElement(Point.class, new String("target"));
        
//        droneCurLocMarker = new Marker(markerLocation, markerLabel, markerStyle, markerLayer);
//        markerLayer.add(droneCurLocMarker);
//        droneTargetMarker = new Marker(markerLocation, markerLabel2, markerStyle2, markerLayer);
//        markerLayer.add(droneTargetMarker);
	}
	
	public void updateDroneCurLocMarker(double lat, double lon, double alt) {
//		if (mapLayer != null && droneCurLocMarker != null) {
//			MapPos curPos = mapLayer.getProjection().fromWgs84(lon, lat);
//			curPos = new MapPos(curPos.x, curPos.y, alt);
//			droneCurLocMarker.setMapPos(curPos);
//		}
	}
	
//	public void updatePrimaryTargetPositionListener(PrimaryTargetPositionListener _mPrimaryTargetPositionListener) {
//		mPrimaryTargetPositionListener = _mPrimaryTargetPositionListener;
//	}
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	  public void onStart() {
	    super.onStart();
	    mapView.startMapping();
	  }

	  @Override
	  public void onStop() {
	    mapView.stopMapping();
	    super.onStop();
	  }
	  
	  
	
	/**
	 * State class for each marker, supports serialization to Bundle class
	 */
	private class MarkerState {
		MapPos mapPos;
		String info;
		boolean selected;
		
		MarkerState(MapPos mapPos, String info) {
			this.mapPos = mapPos;
			this.info = info;
			this.selected = false;
		}
	    
		MarkerState(Bundle bundle) {
			mapPos = new MapPos(bundle.getDoubleArray("mapPos"));
			info = bundle.getString("info");
			selected = bundle.getBoolean("selected");
		}
	
		Bundle saveState() {
			Bundle bundle = new Bundle();
			bundle.putDoubleArray("mapPos", mapPos.toArray());
			bundle.putString("info", info);
			bundle.putBoolean("selected", selected);
			return bundle;
		}
	}
	
//	public MapPos getTargetMapPos() {
//		return droneTargetMarker.getMapPos();
//	}
//	
//	public MapPos getCurMapPos() {
//		return droneCurLocMarker.getMapPos();
//	}
	
	@Override
	protected void enableEditText() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void disableEditText() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetFragment() {
		// TODO Auto-generated method stub
		
	}

	public List<MapPos> getPath() {
		List<MapPos> result = new ArrayList<MapPos>();
		List<MapPos> list = targetLine.getVertexList();
		for (MapPos pos : list) {
			result.add(pos);
		}
		return result;
	}

	public void resetPath() {
		startingPos = new MapPos[3];
		startingPos[0] = mapLayer.getProjection().fromWgs84(longitude, latitude);
		startingPos[1] = mapLayer.getProjection().fromWgs84(longitude - 0.0002d, latitude + 0.0002d);
		startingPos[2] = mapLayer.getProjection().fromWgs84(longitude + 0.0002d, latitude + 0.0002d);
		targets = new ArrayList<MapPos>();
		targets.add(startingPos[0]);
		targets.add(startingPos[1]);
		targets.add(startingPos[2]);
		targets.add(startingPos[0]);
		
		targetLine.setVertexList(targets);
	}

	public void setDroneCurPos(double lat, double lon) {
		MapPos curPos = mapLayer.getProjection().fromWgs84(lon, lat);
		droneCurPoint.setMapPos(curPos);
	}
	
	public void updateUserCurPos() {
		if (!isGPSEnabled && !isNetworkEnabled) {
			Log.d(TAG, "CANNOT FIND CUR POSITION!");
		} else {
			// if GPS Enabled get lat/long using GPS Services
			if (isGPSEnabled) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
						MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
				//Log.d(TAG, "GPS Enabled");
				if (locationManager != null) {
					location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				}
			} else if (isNetworkEnabled) {
		         locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
		         		MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
		         //Log.d(TAG, "Network Enabled");
		         if (locationManager != null) {
		             location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		         }
		     }
		     if (location != null) {
		         latitude = location.getLatitude();
		         longitude = location.getLongitude();
		         //Log.d(TAG, "latitude: " + latitude + " | longitude" + longitude);
		         MapPos curPos = mapLayer.getProjection().fromWgs84(longitude, latitude);
		         userCurPoint.setMapPos(curPos);
		     }
		}
	}

	public MapPos getDroneCurPos() {
		return droneCurPoint.getMapPos();
	}

	public void changeMap(String mapFile) {
		Log.d(TAG, "change mapFile: " + mapFile);
		JobTheme renderTheme = MapsforgeRasterDataSource.InternalRenderTheme.OSMARENDER;
        MapsforgeRasterDataSource dataSource = new MapsforgeRasterDataSource(new EPSG3857(), 0, 20, mapFile, renderTheme);
        mapLayer = new RasterLayer(dataSource, 1044);

        mapView.getLayers().setBaseLayer(mapLayer);
	}

}
