/*
 * Copyright (c) 2006 Ben Chappell (bwchappell@gmail.com) All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *    
 * - Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *   
 * The names of Ben Chappell, Sun Microsystems, Inc. or the names of
 * contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *    
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. BEN CHAPPELL,
 * SUN MICROSYSTEMS, INC. ("SUN"), AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL BEN
 * CHAPPELL, SUN, OR SUN'S LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT 
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF BEN
 * CHAPPELL OR SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *   
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package demos.particles.engine;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.swing.event.*;

public class ControlWindow extends JFrame implements ActionListener, ChangeListener {
    
  // For the engine
  private Engine engine;
  private GLComponent glComponent;
  private Integer numParticles;
    
  // Swing components    
  private JSlider greenSlider;
  private JSlider redSlider;
  private JSlider blueSlider;
    
  private JButton resetButton;
  private JButton closeButton;
    
  private JSpinner particleSpinner;
    
  public ControlWindow() {
    super("Particle Engine");
        
    Dimension d = getToolkit().getScreenSize();
        
    buildFrame(d);        
    initComponents();        
    setVisible(true);
  }
    
  private void buildFrame(Dimension d) {
    // Nicely center the window on the screen
    int width = 800;
    int x = (int)(d.getWidth()/2)-(int)(width/2);
        
    int height = 800;
    int y = (int)(d.getHeight()/2)-(int)(height/2);
        
    setBounds(x, y, width, height);
    setResizable(false);
    getContentPane().setLayout(new GridBagLayout());
    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
  }
    
  private void initComponents() {
    // This will be used when borders or GridBagConstraints are needed
    Border border;
    GridBagConstraints constraints;
        
    // Engine components 
    numParticles = new Integer(1000);
    engine = new Engine(numParticles.intValue(), "demos/particles/engine/images/particle.jpg");
    glComponent = new GLComponent(60, new RGBA(0.0f, 0.0f, 0.0f, 1.0f), new RGBA(0.0f, 0.0f, 0.0f, 1.0f), engine);        
    
    // Close and reset buttons
    resetButton = new JButton("Reset");
    resetButton.addActionListener(this);
    closeButton = new JButton("Close");
    closeButton.addActionListener(this);
        
    // The RGB sliders
    redSlider = new JSlider(0, 100, (int)engine.tendToColor.r*100) ;
    redSlider.addChangeListener(this);
    greenSlider = new JSlider(0, 100, (int)engine.tendToColor.g*100) ;
    greenSlider.addChangeListener(this);
    blueSlider = new JSlider(0, 100, (int)engine.tendToColor.b*100) ;
    blueSlider.addChangeListener(this);
        
    // Particle spinner
    particleSpinner = new JSpinner(new SpinnerNumberModel(numParticles,
                                                          new Integer(0),
                                                          null,
                                                          new Integer(1)));
    particleSpinner.addChangeListener(this);
    particleSpinner.setPreferredSize(
                                     new Dimension((int)(getBounds().width/3.5), 25));
        
    // The color control panel
    JPanel colorPanel = new JPanel(new GridBagLayout());
    border = BorderFactory.createTitledBorder(
                                              BorderFactory.createLineBorder(new Color(0.0f,0.0f,0.0f)),
                                              "Color Tendency");
    colorPanel.setBorder(border);        
        
    constraints = new GridBagConstraints();
    constraints.insets = new Insets(3,2,3,2);   
    constraints.fill=constraints.HORIZONTAL;
        
    constraints.gridx = 0;
    constraints.gridy = 0;
    colorPanel.add(new JLabel("Red"), constraints);      
    constraints.gridx = 1;
    colorPanel.add(redSlider, constraints);
        
    constraints.gridx = 0;
    constraints.gridy = 1;
    colorPanel.add(new JLabel("Green"), constraints);
    constraints.gridx = 1;
    colorPanel.add(greenSlider, constraints);
        
    constraints.gridx = 0;
    constraints.gridy = 2;
    colorPanel.add(new JLabel("Blue"), constraints);
    constraints.gridx = 1;
    colorPanel.add(blueSlider, constraints);
        
    // The panel containing the spinnger
    JPanel numPanel = new JPanel(new GridBagLayout());
    border = BorderFactory.createTitledBorder(
                                              BorderFactory.createLineBorder(new Color(0.0f,0.0f,0.0f)),
                                              "Number of Particles");
    numPanel.setBorder(border);        
        
    constraints = new GridBagConstraints();
    numPanel.add(particleSpinner, constraints);
        
    // The panel containing the reset and close buttons
    JPanel optionsPanel = new JPanel(new GridBagLayout());
    border = BorderFactory.createTitledBorder(
                                              BorderFactory.createLineBorder(new Color(0.0f,0.0f,0.0f)),
                                              "Options");
    optionsPanel.setBorder(border);
        
    constraints = new GridBagConstraints();
    constraints.insets = new Insets(5,5,5,5);
    optionsPanel.add(resetButton, constraints);
    constraints.gridy = GridBagConstraints.RELATIVE;
    constraints.gridx = GridBagConstraints.RELATIVE;
    optionsPanel.add(closeButton, constraints);
        
    // The panel containing the the OpenGL content
    JPanel glPanel = new JPanel(new BorderLayout());    
    glPanel.setPreferredSize(new Dimension(getBounds().width-10, (int)getBounds().height*3/4));
    glPanel.add(glComponent);        
        
    constraints = new GridBagConstraints();
    constraints.weightx = constraints.weighty = 1.0d;
    constraints.fill=constraints.BOTH;
        
    constraints.gridx = 0;
    constraints.gridy = 0;
    glPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    getContentPane().add(glPanel, constraints);
        
    // The panel containing the panels that contain all the other components
    JPanel bottomPanel = new JPanel(new GridLayout(1,3,3,3));
    bottomPanel.add(colorPanel);
    bottomPanel.add(numPanel);
    bottomPanel.add(optionsPanel);        
    constraints.gridx = 0;
    constraints.gridy = GridBagConstraints.RELATIVE;
    getContentPane().add(bottomPanel, constraints);
  }
    
  public static void main(String[] args) {
    new ControlWindow();
  }
    
  public void actionPerformed(ActionEvent e) {
    if(e.getSource().equals(closeButton)) {
      setVisible(false);
      dispose();
      System.exit(0);
    }
    if(e.getSource().equals(resetButton))
      engine.reset();
  }

  public void stateChanged(ChangeEvent e) {
    if(e.getSource().equals(particleSpinner)) {
      // Get the number; loop up or down until the vector in the engine has the proper number
      Integer newNum = (Integer)particleSpinner.getValue();
      int diff = newNum.intValue()-numParticles.intValue();
      if(diff>0)
        for(int i=0; i<diff; i++)
          engine.addParticle();
      else
        for(int i=0; i>diff; i--)
          engine.removeParticle();

      numParticles=newNum;
    }
    if(e.getSource().equals(redSlider)) {
      float value = (float)(redSlider.getValue());
      value/=100;
      engine.tendToColor.r = value;
    }
    if(e.getSource().equals(greenSlider)) {
      float value = (float)(greenSlider.getValue());
      value/=100;
      engine.tendToColor.g = value;            
    }
    if(e.getSource().equals(blueSlider)) {
      float value = (float)(blueSlider.getValue());
      value/=100;
      engine.tendToColor.b = value;            
    }
  }
}
