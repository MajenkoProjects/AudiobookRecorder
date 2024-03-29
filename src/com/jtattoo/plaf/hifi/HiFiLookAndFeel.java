/*
* Copyright (c) 2002 and later by MH Software-Entwicklung. All Rights Reserved.
*  
* JTattoo is multiple licensed. If your are an open source developer you can use
* it under the terms and conditions of the GNU General Public License version 2.0
* or later as published by the Free Software Foundation.
*  
* see: gpl-2.0.txt
* 
* If you pay for a license you will become a registered user who could use the
* software under the terms and conditions of the GNU Lesser General Public License
* version 2.0 or later with classpath exception as published by the Free Software
* Foundation.
* 
* see: lgpl-2.0.txt
* see: classpath-exception.txt
* 
* Registered users could also use JTattoo under the terms and conditions of the 
* Apache License, Version 2.0 as published by the Apache Software Foundation.
*  
* see: APACHE-LICENSE-2.0.txt
*/
 
package com.jtattoo.plaf.hifi;

import com.jtattoo.plaf.*;
import java.util.*;
import javax.swing.UIDefaults;

/**
 * @author Michael Hagen
 */
public class HiFiLookAndFeel extends AbstractLookAndFeel {

    private static HiFiDefaultTheme myTheme = null;

    private static final ArrayList<String> themesList = new ArrayList<String>();
    private static final HashMap<String, Properties> themesMap = new HashMap<String, Properties>();
    private static final Properties defaultProps = new Properties();
    private static final Properties smallFontProps = new Properties();
    private static final Properties largeFontProps = new Properties();
    private static final Properties giantFontProps = new Properties();

    static {
        smallFontProps.setProperty("controlTextFont", "Dialog 10"); // bold
        smallFontProps.setProperty("systemTextFont", "Dialog 10"); // bold
        smallFontProps.setProperty("userTextFont", "Dialog 10");
        smallFontProps.setProperty("menuTextFont", "Dialog 10"); // bold
        smallFontProps.setProperty("windowTitleFont", "Dialog 10"); // bold
        smallFontProps.setProperty("subTextFont", "Dialog 8");

        largeFontProps.setProperty("controlTextFont", "Dialog 14"); // bold
        largeFontProps.setProperty("systemTextFont", "Dialog 14"); // bold
        largeFontProps.setProperty("userTextFont", "Dialog 14"); // bold
        largeFontProps.setProperty("menuTextFont", "Dialog 14"); // bold
        largeFontProps.setProperty("windowTitleFont", "Dialog 14"); // bold
        largeFontProps.setProperty("subTextFont", "Dialog 12");

        giantFontProps.setProperty("controlTextFont", "Dialog 18");
        giantFontProps.setProperty("systemTextFont", "Dialog 18");
        giantFontProps.setProperty("userTextFont", "Dialog 18");
        giantFontProps.setProperty("menuTextFont", "Dialog 18");
        giantFontProps.setProperty("windowTitleFont", "Dialog 18");
        giantFontProps.setProperty("subTextFont", "Dialog 16");

        themesList.add("Default");
        themesList.add("Small-Font");
        themesList.add("Large-Font");
        themesList.add("Giant-Font");

        themesMap.put("Default", defaultProps);
        themesMap.put("Small-Font", smallFontProps);
        themesMap.put("Large-Font", largeFontProps);
        themesMap.put("Giant-Font", giantFontProps);
    }

    public static java.util.List getThemes() {
        return themesList;
    }

    public static Properties getThemeProperties(String name) {
        return ((Properties) themesMap.get(name));
    }

    public static void setTheme(String name) {
        setTheme((Properties) themesMap.get(name));
        if (myTheme != null) {
            AbstractTheme.setInternalName(name);
        }
    }

    public static void setTheme(String name, String licenseKey, String logoString) {
        Properties props = (Properties) themesMap.get(name);
        if (props != null) {
            props.put("licenseKey", licenseKey);
            props.put("logoString", logoString);
            setTheme(props);
            if (myTheme != null) {
                AbstractTheme.setInternalName(name);
            }
        }
    }

    public static void setTheme(Properties themesProps) {
        currentThemeName = "hifiTheme";
        if (myTheme == null) {
            myTheme = new HiFiDefaultTheme();
        }
        if ((myTheme != null) && (themesProps != null)) {
            myTheme.setUpColor();
            myTheme.setProperties(themesProps);
            myTheme.setUpColorArrs();
            AbstractLookAndFeel.setTheme(myTheme);
        }
    }

    public static void setCurrentTheme(Properties themesProps) {
        setTheme(themesProps);
    }

    public String getName() {
        return "HiFi";
    }

    public String getID() {
        return "HiFi";
    }

    public String getDescription() {
        return "The HiFi Look and Feel";
    }

    public boolean isNativeLookAndFeel() {
        return false;
    }

    public boolean isSupportedLookAndFeel() {
        return true;
    }

    public AbstractBorderFactory getBorderFactory() {
        return HiFiBorderFactory.getInstance();
    }

    public AbstractIconFactory getIconFactory() {
        return HiFiIconFactory.getInstance();
    }

    protected void createDefaultTheme() {
        if (myTheme == null) {
            myTheme = new HiFiDefaultTheme();
        }
        setTheme(myTheme);
    }
    
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        table.put("ScrollBar.incrementButtonGap", -1);
        table.put("ScrollBar.decrementButtonGap", -1);
    }
    
    protected void initClassDefaults(UIDefaults table) {
        if (!"hifiTheme".equals(currentThemeName)) {
            setTheme("Default");
        }
        super.initClassDefaults(table);
        Object[] uiDefaults = {
            // BaseLookAndFeel classes
            "SeparatorUI", BaseSeparatorUI.class.getName(),
            "TextFieldUI", BaseTextFieldUI.class.getName(),
            "TextAreaUI", BaseTextAreaUI.class.getName(),
            "EditorPaneUI", BaseEditorPaneUI.class.getName(),
            "PasswordFieldUI", BasePasswordFieldUI.class.getName(),
            "ToolTipUI", BaseToolTipUI.class.getName(),
            "TreeUI", BaseTreeUI.class.getName(),
            "TableUI", BaseTableUI.class.getName(),
            "TableHeaderUI", BaseTableHeaderUI.class.getName(),
            "SplitPaneUI", BaseSplitPaneUI.class.getName(),
            "ProgressBarUI", BaseProgressBarUI.class.getName(),
            "FileChooserUI", BaseFileChooserUI.class.getName(),
            "MenuUI", BaseMenuUI.class.getName(),
            "PopupMenuUI", BasePopupMenuUI.class.getName(),
            "MenuItemUI", BaseMenuItemUI.class.getName(),
            "CheckBoxMenuItemUI", BaseCheckBoxMenuItemUI.class.getName(),
            "RadioButtonMenuItemUI", BaseRadioButtonMenuItemUI.class.getName(),
            "PopupMenuSeparatorUI", BaseSeparatorUI.class.getName(),
            "DesktopPaneUI", BaseDesktopPaneUI.class.getName(),
            
            // HiFiLookAndFeel classes
            "LabelUI", HiFiLabelUI.class.getName(),
            "CheckBoxUI", HiFiCheckBoxUI.class.getName(),
            "RadioButtonUI", HiFiRadioButtonUI.class.getName(),
            "ButtonUI", HiFiButtonUI.class.getName(),
            "ToggleButtonUI", HiFiToggleButtonUI.class.getName(),
            "ComboBoxUI", HiFiComboBoxUI.class.getName(),
            "SliderUI", HiFiSliderUI.class.getName(),
            "PanelUI", HiFiPanelUI.class.getName(),
            "ScrollPaneUI", HiFiScrollPaneUI.class.getName(),
            "TabbedPaneUI", HiFiTabbedPaneUI.class.getName(),
            "ScrollBarUI", HiFiScrollBarUI.class.getName(),
            "ToolBarUI", HiFiToolBarUI.class.getName(),
            "MenuBarUI", HiFiMenuBarUI.class.getName(),
            "InternalFrameUI", HiFiInternalFrameUI.class.getName(),
            "RootPaneUI", HiFiRootPaneUI.class.getName(),};
        table.putDefaults(uiDefaults);
        if (JTattooUtilities.getJavaVersion() >= 1.5) {
            table.put("FormattedTextFieldUI", BaseFormattedTextFieldUI.class.getName());
            table.put("SpinnerUI", BaseSpinnerUI.class.getName());
        }
    }
}
