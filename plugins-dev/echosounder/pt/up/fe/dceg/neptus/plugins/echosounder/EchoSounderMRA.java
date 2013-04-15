/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2009/09/05
 */
package pt.up.fe.dceg.neptus.plugins.echosounder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.Distance;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(author="zp", name="Echo Sounder Analysis", icon="pt/up/fe/dceg/neptus/plugins/echosounder/echosounder.png")
public class EchoSounderMRA extends JPanel implements MRAVisualization {

    private static final long serialVersionUID = 1L;

    private IMraLogGroup source;
    protected MRAPanel mraPanel;

    @NeptusProperty
    public ColorMap colormap = ColorMapFactory.createJetColorMap();

    private BufferedImage image = null;
    int imageWidth;
    int imageHeight;

    int maxRange;

    double yscale;

    public EchoSounderMRA(MRAPanel panel) {
        mraPanel = panel;

        this.addComponentListener(new ComponentAdapter() {
            /* (non-Javadoc)
             * @see java.awt.event.ComponentAdapter#componentResized(java.awt.event.ComponentEvent)
             */
            @Override
            public void componentResized(ComponentEvent e) {
                generateImage();
            }
        });
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return true;
    }


    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        this.source = source;
        generateImage();

        return this;
    }

    public void generateImage() {
        NeptusLog.pub().info("<###>Generating Echo sounder image");
        int c = 0;
        Iterator<IMCMessage> i= source.getLsfIndex().getIterator("SonarData");
        for(IMCMessage msg = i.next(); i.hasNext(); msg = i.next()) {
            if(msg.getInteger("type") == SonarData.TYPE.ECHOSOUNDER.value()) {
                c++;
                imageHeight = msg.getRawData("data").length;
                maxRange = msg.getInteger("max_range");
            }
        }
        imageWidth = c;
        image = ImageUtils.createCompatibleImage(imageWidth, imageHeight, Transparency.OPAQUE);
        Graphics2D g2d = (Graphics2D) image.getGraphics();

        i= source.getLsfIndex().getIterator("SonarData");
        int x = 0;
        for(IMCMessage msg = i.next(); i.hasNext(); msg = i.next()) {
            if(msg.getInteger("type") == SonarData.TYPE.ECHOSOUNDER.value()) {
                int y = 0;
                for(byte b : msg.getRawData("data")) {
                    //                    NeptusLog.pub().info("<###> "+x + " " + y + " " + b + " " + new Byte(b).doubleValue() + " " + colormap.getColor(new Byte(b).doubleValue()).getBlue());
                    image.setRGB(x, imageHeight - y - 1, colormap.getColor(new Byte(b).doubleValue() * 2 / 255).getRGB());
                    y++;
                }
                x++;
            }
        }

        i = source.getLsfIndex().getIterator("Distance");
        x = 0;

        int prevX = 0, prevY = 0;

        for (int j = source.getLsfIndex().getFirstMessageOfType(Distance.ID_STATIC); j != -1; j = source.getLsfIndex().getNextMessageOfType(Distance.ID_STATIC, j)) {
            if(source.getLsfIndex().entityNameOf(j).equals("Echo Sounder")) {
                // In case there is some more Distance messages that Echo Sounder sonar data points
                if(x >= imageWidth)
                    break;
                double y = imageHeight - (500 / maxRange * source.getLsfIndex().getMessage(j).getDouble("value")) - 1;
                image.setRGB(x, (int)y, Color.BLACK.getRGB());

                if(x != 0) {
                    g2d.setColor(Color.BLACK);
                    g2d.drawLine(prevX, prevY, x, (int)y);
                    prevX = x;
                    prevY = (int) y;
                }
                else {
                    prevX = x;
                    prevY = (int) y;
                }
                x++;
            }
        }
    }
    @Override
    public void paint(Graphics g) {
        NeptusLog.pub().info("<###> "+this.getWidth() + " " + this.getHeight());
        g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), 0, 0, imageWidth, imageHeight,null);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("SonarData") != null && source.getLog("Distance") != null;
    }

    @Override
    public Double getDefaultTimeStep() {
        return NeptusMRA.defaultTimestep;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(this.getClass()), 16, 16);
    }

    public String getName() 
    {		
        return I18n.text(PluginUtils.getPluginName(this.getClass()));
    }

    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public void onCleanup() {
        mraPanel = null;
    }

    @Override
    public void onHide() {
        // TODO Auto-generated method stub	   
    }

    public void onShow() {
        //nothing
    }

}
