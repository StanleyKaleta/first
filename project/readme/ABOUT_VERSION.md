# 1.0 
Released: summer 2014  
    
    First usable version of this application. 
    Implemented functionality like manager for crud operations (xml database), create new month, select month, retrieve items for day, item for day, item types and painting first simple graphs.
    Also first draft of gui implemented. 
    Attempt for l18n to cz,sk,en,de,fr.
    
### 1.1
Released: summer 2014 

    Implemented data importer from android application(Scheduler android app. used as support for collecting datas (Andr. ver. 2.3.)). 
    Implemented settings resource to remember last config (in this version only last selected month). 
    Disabled l18n due wrong chars in some languages.
    
### 1.1.1
Released: 8.10.2014
    
    Added menu Help with item About to show basic infos about app., refactored packages according to maven standard, fixed bug in settings. 
    Implemented settings resource to remember last config (added last selected week).
    
## 2.0-BETA
Released: september 2015

    Completely reworked back-end of application (the core are services which using implementations of managers). 
    For now, managers has only one implementation - using JAXB API and XSD schema. 
    Added possibility to log to file.
    Added new entities (task, global task and types for them). 
    Settings are now entity too. 
    List of months (and their order) is stored in Global entity. 
    Added before-start checks for resources and first use(= lack of settings, now start-up wizard). 
    Completely reworked front-end of application (the core view changed from week view to month view). 
    New dialogs, gui actions,...
    Partially finished (just accounting - now focus on testing and using - rest in 2.0).
    
# 2.0
Released: 31.12.2015

    Improved services, configuration, migrated actions to classes. 
    Added possibility to import months from version 1.x. 
    Improved day preview panel.
    Added possibility to import items from android app.
    Added possibility to export item types to android app.
    Minor bug fixes.

## 2.1
Released: in progress

    ...
