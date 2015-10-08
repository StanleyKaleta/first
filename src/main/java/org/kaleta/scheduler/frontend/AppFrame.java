package org.kaleta.scheduler.frontend;

import org.kaleta.scheduler.backend.entity.*;
import org.kaleta.scheduler.backend.service.Service;
import org.kaleta.scheduler.backend.service.ServiceFailureException;
import org.kaleta.scheduler.frontend.day.AccountingPanel;
import org.kaleta.scheduler.frontend.day.SchedulePanel;
import org.kaleta.scheduler.frontend.dialog.CreateMonthDialog;
import org.kaleta.scheduler.frontend.dialog.SelectMonthDialog;
import org.kaleta.scheduler.frontend.dialog.SettingsDialog;
import org.kaleta.scheduler.frontend.global.GlobalPanel;
import org.kaleta.scheduler.frontend.month.MonthPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by Stanislav Kaleta on 06.08.2015.
 */
public class AppFrame extends JFrame implements GuiModel{
    private Service service;
    private int selectedDay;
    private int selectedMonthId;

    public AppFrame(){
        service = new Service();
        selectedDay = -1;
        selectedMonthId = -1;
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        initComponents();
        applySettings();

        this.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = this.getSize();
        int centerPosX = (screenSize.width - frameSize.width) / 2;
        int centerPosY = (screenSize.height - frameSize.height) / 2;
        this.setLocation(centerPosX, centerPosY);
    }

    private void initComponents() {
        JTabbedPane dayTabbedPane = new JTabbedPane();
        dayTabbedPane.addTab("Schedule", new SchedulePanel());
        dayTabbedPane.addTab("Accounting", new AccountingPanel());

        GlobalPanel globalPanel = new GlobalPanel();
        globalPanel.setBackground(Color.YELLOW);

        MonthPanel monthPanel = new MonthPanel();
        monthPanel.setBackground(Color.BLUE);

        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(globalPanel)
                        .addComponent(monthPanel))
                .addComponent(dayTabbedPane, 230, 330, Short.MAX_VALUE));

        layout.setVerticalGroup(layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                        .addComponent(globalPanel, 100, 100, 100)
                        .addComponent(monthPanel))
                .addComponent(dayTabbedPane));

        initMenuBar();

    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenu monthMenu = new JMenu("Month");
        menuBar.add(monthMenu);
        JMenu statsMenu = new JMenu("Statistics");
        menuBar.add(statsMenu);
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem settingsMenuItem = new JMenuItem("Settings...");
        settingsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SettingsDialog dialog = new SettingsDialog();

                Settings settings = service.getSettings();
                String[] actuallyFilledSettings = new String[4];
                actuallyFilledSettings[0] = settings.getUserName();
                actuallyFilledSettings[1] = settings.getUiSchemeSelected();
                actuallyFilledSettings[2] = settings.getCurrency();
                actuallyFilledSettings[3] = settings.getLanguage();

                dialog.getContentPanel().setUserSettings(actuallyFilledSettings);

                dialog.getContentPanel().setTaskTypes(settings.getTaskTypes());
                dialog.getContentPanel().setItemTypes(settings.getItemTypes());
                dialog.getContentPanel().setGlobalTaskTypes(settings.getGlobalTaskTypes());

                dialog.setLocationRelativeTo(AppFrame.this);
                dialog.setVisible(true);

                if (dialog.getResult()){
                    String[] userSettings = dialog.getContentPanel().getUserSettings();
                    settings.setUserName(userSettings[0]);
                    settings.setUiSchemeSelected(userSettings[1]);
                    settings.setCurrency(userSettings[2]);
                    settings.setLanguage(userSettings[3]);

                    settings.getTaskTypes().clear();
                    settings.getTaskTypes().addAll(dialog.getContentPanel().getTaskTypes());

                    settings.getItemTypes().clear();
                    settings.getItemTypes().addAll(dialog.getContentPanel().getItemTypes());

                    settings.getGlobalTaskTypes().clear();
                    settings.getGlobalTaskTypes().addAll(dialog.getContentPanel().getGlobalTaskTypes());

                    service.updateSettings(settings);
                    AppFrame.this.applySettings();
                }
            }
        });

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        fileMenu.add(settingsMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(exitMenuItem);

        JMenuItem createMonthMenuItem = new JMenuItem("Create...");
        createMonthMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                CreateMonthDialog dialog = new CreateMonthDialog();
                dialog.setLocationRelativeTo(AppFrame.this);
                dialog.setVisible(true);

                if (dialog.getResult()){
                    Global global = service.getGlobal();
                    int lastId = 0;
                    int lastOrder = 0;
                    for (Integer usedId : global.getMonths().keySet()){
                        if (usedId > lastId){
                            lastId = usedId;
                        }
                        if (global.getMonths().get(usedId) > lastOrder){
                            lastOrder = global.getMonths().get(usedId);
                        }
                    }
                    int newId = lastId + 1;

                    Month createdMonth = dialog.getCreatedMonth();
                    createdMonth.setId(newId);
                    service.createMonth(createdMonth);

                    global.getMonths().put(newId, lastOrder + 1);
                    service.updateGlobal(global);

                    Initializer.LOG.info("New month \""+createdMonth.getName()
                            + "\" with id=" + newId + " created in file %DATA%/months-database/m" + newId + ".xml");

                    if (dialog.wantToSelectCreatedMonth()){
                        selectMonth(newId);
                    }
                }
            }
        });

        JMenuItem selectMonthMenuItem = new JMenuItem("Select...");
        selectMonthMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SelectMonthDialog dialog = new SelectMonthDialog();

                Map<Integer, Integer> orders = getMonthsOrder();
                String[] monthNames = new String[orders.size()];
                Integer[] monthIds = new Integer[orders.size()];
                int actualySelectedMonthIndex = -1;
                int index = 0;
                for (Integer key : orders.keySet()){
                    monthNames[index] = getMonthName(key);
                    monthIds[index] = key;

                    if(key == getSelectedMonthId()){
                        actualySelectedMonthIndex = index;
                    }

                    index++;
                }

                dialog.setMonthNames(monthNames, actualySelectedMonthIndex);
                dialog.setLocationRelativeTo(AppFrame.this);
                dialog.setVisible(true);

                if (dialog.getResult()){
                    Integer selectedMonthId = monthIds[dialog.getSelectedMonthIndex()];
                    selectMonth(selectedMonthId);
                }
            }
        });

        JMenuItem editMonthMenuItem = new JMenuItem("Edit...");
        editMonthMenuItem.setEnabled(false);/*TODO to version 2.y*/
        editMonthMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                /*TODO open edit dialog (+throw month changed action - cuz month control update)*/
                /*TODO edit month params + edit order*/
            }
        });

        monthMenu.add(createMonthMenuItem);
        monthMenu.add(selectMonthMenuItem);
        monthMenu.add(editMonthMenuItem);

        /*TODO create statistics menu*/

        JMenuItem documentationMenuItem = new JMenuItem("Documentation");
        documentationMenuItem.setEnabled(false);/*TODO to version 2.y*/
        documentationMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                /*TODO somehow open html file with doc.(devel doc + user guide)*/
            }
        });

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                StringBuilder sb = new StringBuilder();
                sb.append("name: "+Initializer.NAME+"\n");
                sb.append("version: "+Initializer.VERSION+"\n\n");
                sb.append("developer: Stanislav Kaleta \n");
                sb.append("contact: kstanleykale@gmail.com \n\n");
                sb.append("testers: Stanislav Kaleta, Ludmila Florekova \n\n");
                sb.append("\u00a9 2014 - 2015 Stanislav Kaleta All rights reserved");

                JOptionPane.showMessageDialog(AppFrame.this, sb.toString(),"About",JOptionPane.PLAIN_MESSAGE);
            }
        });

        helpMenu.add(documentationMenuItem);
        helpMenu.add(new JSeparator());
        helpMenu.add(aboutMenuItem);
    }

    private void applySettings(){
        Settings settings = service.getSettings();

        this.setTitle(Initializer.NAME + "-" + Initializer.VERSION
                + " - user: " + settings.getUserName());

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (settings.getUiSchemeSelected().equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(this);
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException
                | UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new ServiceFailureException(e);
        }

        int settingsSelectedMonthId = settings.getLastMonthSelectedId();
        if (settingsSelectedMonthId != selectedMonthId){
            selectedMonthId = settingsSelectedMonthId;
            update(GuiModel.MONTH_CHANGED);
        }

        int settingsSelectedDay = settings.getLastDaySelected();
        if (settingsSelectedDay != selectedDay){
            selectedDay = settingsSelectedDay;
            update(GuiModel.DAY_CHANGED);
        }
    }

    private void updateComponent(JComponent component,int command) {
        Action action = component.getActionMap().get(command);
        if (action != null){
            ActionEvent actionEvent = new ActionEvent(this,0,null);
            action.actionPerformed(actionEvent);
        }

        for (Component child : component.getComponents()) {
            try {
                updateComponent((JComponent) child, command);
            } catch (ClassCastException ex){
                // continue
            }
        }
    }

    @Override
    public void update(int command) {
        for (Component component : this.getComponents()) {
            updateComponent((JComponent) component, command);
        }
    }

    @Override
    public int getSelectedMonthId() {
        return selectedMonthId;
    }

    @Override
    public Map<Integer, Integer> getMonthsOrder() {
        Global global = service.getGlobal();
        /*TODO maybe sort by order*/
        return global.getMonths();
    }

    @Override
    public String getMonthName(int monthId) {
        Month month = service.getMonth(monthId);
        return month.getName();
    }

    @Override
    public void selectMonth(int monthId) {
        selectedMonthId = monthId;

        Settings settings = service.getSettings();
        settings.setLastMonthSelectedId(monthId);
        service.updateSettings(settings);

        update(GuiModel.MONTH_CHANGED);

        selectDay(1);
    }

    @Override
    public Day getDayAt(Point position) {
        Month month = service.getMonth(selectedMonthId);

        int dayNumber = 7 * (position.y - 1) + position.x - (month.getDayStartsWith() - 1);
        if (dayNumber < 1){
            dayNumber = -1;
        }
        if (dayNumber > month.getDaysNumber()){
            dayNumber = -1;
        }

        return getDay(dayNumber);
    }

    @Override
    public Day getDay(int dayNumber) {
        Day day = new Day();

        day.setDayNumber(dayNumber);
        if (dayNumber == -1){
            return day;
        }

        Month month = service.getMonth(selectedMonthId);

        for (Item item : month.getItems()){
            if (item.getDay() == dayNumber){
                day.getItems().add(item);
            }
        }

        for (Task task : month.getTasks()){
            if (task.getDay() == dayNumber){
                day.getTasks().add(task);
            }
        }

        day.setPublicFreeDay(Boolean.FALSE);
        for (Integer integer : month.getPublicFreeDays()){
            if (integer == dayNumber){
                day.setPublicFreeDay(Boolean.TRUE);
                break;
            }
        }

        return day;
    }

    @Override
    public int getSelectedDayNumber() {
        return selectedDay;
    }

    @Override
    public void selectDay(int dayNumber) {
        selectedDay = dayNumber;

        Settings settings = service.getSettings();
        settings.setLastDaySelected(dayNumber);
        service.updateSettings(settings);

        update(GuiModel.DAY_CHANGED);
    }

    @Override
    public List<UserType> getItemTypes() {
        Settings settings = service.getSettings();
        return settings.getItemTypes();
    }

    @Override
    public void addItem(Item item) {
        Month month = service.getMonth(selectedMonthId);

        int newId = 0;
        for (Item existingItem : month.getItems()){
            if (existingItem.getId() > newId){
                newId = existingItem.getId();
            }
        }
        newId = newId + 1;

        item.setDay(selectedDay);
        item.setId(newId);

        month.getItems().add(item);

        service.updateMonth(month);

        update(GuiModel.DAY_CHANGED);
    }

    @Override
    public List<Item> getRecentlyUsedItems() {
        int counter = 0;
        Map<Item,Integer> recentlyUsed = new HashMap<>();
        for (Item item : service.getMonth(selectedMonthId).getItems()){
            if (recentlyUsed.size() == 0){
                recentlyUsed.put(item, 1);
                counter = 1;
            } else {
                Set<Item> recentItems = new HashSet<>();
                recentItems.addAll(recentlyUsed.keySet());
                boolean set = false;
                for (Item setItem : recentItems){
                    if (setItem.getIncome().equals(item.getIncome())
                            && setItem.getType().equals(item.getType())
                            && setItem.getDescription().equals(item.getDescription())
                            && setItem.getAmount().equals(item.getAmount())){
                        int nUsed = recentlyUsed.get(setItem) + 1;
                        recentlyUsed.put(setItem,nUsed);
                        if (nUsed > counter){
                            counter = nUsed;
                        }
                        set = true;
                        break;
                    }
                }
                if (!set){
                    recentlyUsed.put(item, 1);
                }
            }
        }

        List<Item> subRecentlyUsed = new ArrayList<>();
        int subEdge = 5;
        while (subEdge > 0 && counter > 0){
            for (Item item : recentlyUsed.keySet()){
                if (recentlyUsed.get(item).equals(counter) && subEdge > 0){
                    subRecentlyUsed.add(item);
                    subEdge--;
                }
            }
            counter--;
        }
        return subRecentlyUsed;
    }

    @Override
    public void updateItem(Item item) {
        service.updateItem(item, selectedMonthId);
    }

    @Override
    public void deleteItem(Item item) {
        service.deleteItem(item, selectedMonthId);

        update(GuiModel.DAY_CHANGED);
    }
}