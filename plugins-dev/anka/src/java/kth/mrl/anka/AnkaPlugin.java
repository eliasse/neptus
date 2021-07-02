package kth.mrl.anka;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Collection;

import org.checkerframework.checker.units.qual.mPERs;

import com.beust.jcommander.internal.Console;
import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Announce;
import pt.lsts.imc.Map;
import pt.lsts.imc.MapFeature;
import pt.lsts.imc.MapFeature.FEATURE_TYPE;
import pt.lsts.imc.MapPoint;
import pt.lsts.imc.MsgList;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.*;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

@PluginDescription(name="ANKA Plugin")
public class AnkaPlugin extends SimpleRendererInteraction{
	public AnkaPlugin(ConsoleLayout console) {
		super(console);
	}
	
	String systemName = "anka";
	String sourceEntity = "mapper";
	int systemAddress = 0x0809;
	Map lastMap;
	
	@Subscribe
	public void consume(Announce msg) {
        if (msg.getSysName().equalsIgnoreCase(systemName)) {
        	post(Notification.info("Anka Plugin", "Announce Received"));
        	systemAddress = msg.getSrc();
        }
	}
	
	@Subscribe
	public void consume(Map msg) {
		if (msg.getSrc() == systemAddress) {
			lastMap = msg;
		}
		else {
			post(Notification.info("Anka plugin", "Recevied Map but source entity or system address is wrong."));
		}
	}
	
	@Override
	public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        
		for (MapFeature feature: lastMap.getFeatures()) {
			switch (feature.getFeatureType()) {
			case POI:
				System.out.println("Received POI");
				MapPoint mappoint = feature.getFeature().firstElement();
	            LocationType loc = new LocationType(Math.toDegrees(mappoint.getLat()),
	                    Math.toDegrees(mappoint.getLon()));
	            Point2D pt = renderer.getScreenPosition(loc);
	            Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - 100, pt.getY() - 100,
	                    100 * 2, 100 * 2);
	            
	            float[] hsb = Color.RGBtoHSB(feature.getRgbRed(), 
	            		feature.getRgbGreen(), 
	            		feature.getRgbBlue(), null);
	            
	            Color color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
	            
	            g.setColor(color);
	            //g.draw(pt);
	            g.fill(ellis);
				break;
			case LINE:
				System.out.println("Received LINE");
				break;
			case CONTOUREDPOLY:
				break;
			case FILLEDPOLY:
				break;
			}
		}
	}
	
	//Collection<MapFeature> mapFeatures = new Collection<MapFeature>()
	//ArrayList<MapFeature> mapfeatures = new ArrayList<MapFeature>();
	//Collection<MapPoint> linePoints = new Collection<MapPoint>()

	//MapPoint poi = new MapPoint(60.0, 20.0, (float)4.0);
	//MapFeature foi = new MapFeature("A",FEATURE_TYPE.POI, (short)0, (short)255, (short)0, poi);
	//Collection<MapFeature> mapfeatures = new Collection<>(foi);
	//Map map = new Map("FeatureOfInterest", mapfeatures);
	//MsgList msglist = new MsgList();
	
    @Override
    public void cleanSubPanel(){
        System.out.println("Anka plugin was destroyed.");
    }

    @Override
    public void initSubPanel() {
        System.out.println("Started Anka plugin");
    }

    @Override
    public boolean isExclusive() {
        return true;
    }
}
