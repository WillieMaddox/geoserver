/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class LegendCapabilitiesTest extends WMSTestSupport {
    
    private static final String CAPABILITIES_REQUEST = "wms?request=getCapabilities&version=1.1.1";
    
    // Reusing layer and SLD files from another test; their content doesn't really matter.
    // What is important for this test is the legend info we are adding.
    private static final String LAYER_NAME = "watertemp";
    private static final QName LAYER_QNAME =
            new QName(MockData.DEFAULT_URI, LAYER_NAME, MockData.DEFAULT_PREFIX);
    private static final String LAYER_FILE = "custwatertemp.zip";
    private static final String STYLE_NAME = "temperature";
    private static final String STYLE_FILE = "../temperature.sld";
    
    private static final int LEGEND_WIDTH = 22;
    private static final int LEGEND_HEIGHT = 22;
    private static final String LEGEND_FORMAT = "image/jpeg";
    private static final String IMAGE_URL = "styles/legend.png";
    private static final String BASE = "src/test/resources/geoserver";
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        LegendInfo legend = new LegendInfoImpl();
        legend.setWidth(LEGEND_WIDTH);
        legend.setHeight(LEGEND_HEIGHT);
        legend.setFormat(LEGEND_FORMAT);
        legend.setOnlineResource(IMAGE_URL);
        
        // add legend.png to styles directory
        Resource resource = getResourceLoader().get("styles/legend.png");        
        getResourceLoader().copyFromClassPath( "../legend.png", resource.file(),  getClass() );
        
        // add layer
        testData.addStyle(null, STYLE_NAME, STYLE_FILE, getClass(), getCatalog(), legend);
        Map<SystemTestData.LayerProperty, Object> propertyMap =
                new HashMap<SystemTestData.LayerProperty, Object>();
        propertyMap.put(LayerProperty.STYLE, STYLE_NAME);
        testData.addRasterLayer(LAYER_QNAME, LAYER_FILE, null, propertyMap,
                SystemTestData.class, getCatalog());
        
        GeoServerInfo global = getGeoServer().getGlobal();
        
        global.getSettings().setProxyBaseUrl(BASE);
        getGeoServer().save(global);
        
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("", "http://www.opengis.net/wms");
        namespaces.put("wms", "http://www.opengis.net/wms");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
    
    /**
     * Looking for somethign similar to:
     * 
     * <pre>
     *           &lt;LegendURL height="22" width="22"&gt;
     *             &lt;Format&gt;image/jpeg&lt;/Format&gt;
     *             &lt;OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="styles/legend.png" xlink:type="simple"/&gt;
     *           &lt;/LegendURL&gt;
     * </pre>
     * 
     */
    @Test
    public void testCapabilities() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        //print(dom);

        final String legendUrlPath = "//Layer[Name='gs:" + LAYER_NAME + "']/Style/LegendURL";
        
        // Ensure capabilities document reflects the specified legend info
        assertXpathEvaluatesTo(String.valueOf(LEGEND_WIDTH), legendUrlPath + "/@width", dom);
        assertXpathEvaluatesTo(String.valueOf(LEGEND_HEIGHT), legendUrlPath + "/@height", dom);
        assertXpathEvaluatesTo(LEGEND_FORMAT, legendUrlPath + "/Format", dom);
        assertXpathEvaluatesTo(BASE+"/"+IMAGE_URL, legendUrlPath + "/OnlineResource/@xlink:href", dom);
    }
}
