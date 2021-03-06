package org.pillarone.riskanalytics.graph.formeditor.ui.view;


import com.canoo.ulc.detachabletabbedpane.server.ITabListener;
import com.canoo.ulc.detachabletabbedpane.server.TabEvent;
import com.canoo.ulc.detachabletabbedpane.server.ULCCloseableTabbedPane;
import com.ulcjava.applicationframework.application.AbstractBean;
import com.ulcjava.applicationframework.application.Action;
import com.ulcjava.applicationframework.application.ApplicationContext;
import com.ulcjava.applicationframework.application.ToolBarFactory;
import com.ulcjava.base.application.*;
import com.ulcjava.base.application.event.ActionEvent;
import com.ulcjava.base.application.event.IActionListener;
import com.ulcjava.base.application.event.KeyEvent;
import com.ulcjava.base.application.util.Dimension;
import com.ulcjava.base.application.util.IFileLoadHandler;
import com.ulcjava.base.application.util.KeyStroke;
import com.ulcjava.base.shared.FileChooserConfig;
import groovy.util.ConfigObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.model.Model;
import org.pillarone.riskanalytics.core.simulation.item.Parameterization;
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.persistence.GraphPersistenceException;
import org.pillarone.riskanalytics.graph.core.graph.persistence.GraphPersistenceService;
import org.pillarone.riskanalytics.graph.core.graphimport.AbstractGraphImport;
import org.pillarone.riskanalytics.graph.core.graphimport.ComposedComponentGraphImport;
import org.pillarone.riskanalytics.graph.core.graphimport.GraphImportService;
import org.pillarone.riskanalytics.graph.core.graphimport.ModelGraphImport;
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService;
import org.pillarone.riskanalytics.graph.formeditor.ui.IGraphModelHandler;
import org.pillarone.riskanalytics.graph.formeditor.ui.IHelpViewable;
import org.pillarone.riskanalytics.graph.formeditor.ui.IModelRenameListener;
import org.pillarone.riskanalytics.graph.formeditor.ui.ISaveListener;
import org.pillarone.riskanalytics.graph.formeditor.ui.model.TypeDefinitionFormModel;
import org.pillarone.riskanalytics.graph.formeditor.ui.model.beans.TypeDefinitionBean;
import org.pillarone.riskanalytics.graph.formeditor.ui.view.dialogs.ModelRenameDialog;
import org.pillarone.riskanalytics.graph.formeditor.ui.view.dialogs.TypeDefinitionDialog;
import org.pillarone.riskanalytics.graph.formeditor.ui.view.dialogs.TypeImportDialog;
import org.pillarone.riskanalytics.graph.formeditor.util.FileStoreHandler;
import org.pillarone.riskanalytics.graph.formeditor.util.GraphModelUtilities;
import org.pillarone.riskanalytics.graph.formeditor.util.ParameterUtilities;

import java.io.*;
import java.util.*;

/**
 * The main window with the form editor view.
 * It inherits from {@link AbstractBean} to benefit from its property change support.
 * <p/>
 *
 * The main window is a tabbed pane which contains for each model or component to be edited a tab.
 * Furthermore, it contains also a palette which contains the components available to DnD to a model under construction.
 * Finally, a model repository contains the the elements that have been saved, but may be still under construction and not deployed yet.
 *
 * @author martin.melchior
 */
public class GraphModelEditor extends AbstractBean implements IGraphModelHandler, IModelRenameListener {

    private static Log LOG = LogFactory.getLog(GraphModelEditor.class);

    /* Context is needed to load resources (such as icons, etc).*/
    private ApplicationContext fContext;

    private ULCSplitPane fContentView;

    /* The editor view.*/
    private ULCCloseableTabbedPane fEditorArea;
    /* Set of currently opened type defs - check that type defs does not already exist. */
    private Set<TypeDefinitionBean> fEditedTypeDefinitions;
    /* Is used for remembering what types have already been declared */
    private Map<ULCComponent, SingleModelMultiEditView> fModelTabs;
    /* A dialog for models or composed components to be imported.*/
    private TypeImportDialog fTypeImportView;
    /* A dialog to rename models */
    private ModelRenameDialog fRenameModelDialog;

    private ModelRepositoryTree fModelRepositoryTree;

    private GraphPersistenceService fPersistenceService;

    private List<ISaveListener> saveListeners = new ArrayList<ISaveListener>();

    /**
     * @param context Application context is used for accessing and using resources (such as icons, etc.).
     */
    public GraphModelEditor(ApplicationContext context) {
        fContext = context;
        fEditedTypeDefinitions = new HashSet<TypeDefinitionBean>();
        fModelTabs = new HashMap<ULCComponent, SingleModelMultiEditView>();
        createView();

        TypeDefinitionBean typeDef = new TypeDefinitionBean();
        typeDef.setBaseType(TypeDefinitionBean.MODEL);
        typeDef.setPackageName("models");
        typeDef.setName("untitled");

        AbstractGraphModel model = new ModelGraphModel();
        model.setPackageName("models");
        model.setName("untitled");
        addModel(model, typeDef, true);
    }

    /**
     * Returns the split pane - consisting of the palette view and the model edit view in tabbed pane -
     * as content view.
     *
     * @return model edit and palette view
     */
    public ULCComponent getContentView() {
        return fContentView;
    }

    private void createView() {
        // initialize & decorate components
        ULCBoxPane modelEdit = new ULCBoxPane(true);
        modelEdit.setPreferredSize(new Dimension(600, 600));
        ULCSeparator separator = new ULCSeparator();
        separator.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        fEditorArea = new ULCCloseableTabbedPane();

        ULCBoxPane palettePane = new ULCBoxPane(1, 2);
        TabularFilterView filterView = new TabularFilterView();
        ULCBoxPane paletteArea = getPalettePane(filterView);

        ULCBoxPane repositoryTreePane = new ULCBoxPane(true);
        repositoryTreePane.setPreferredSize(new Dimension(200, 200));
        fModelRepositoryTree = new ModelRepositoryTree(this);
        ULCTabbedPane tabbedPane = new ULCTabbedPane();

        ULCSplitPane typeSelectionPane = new ULCSplitPane(ULCSplitPane.VERTICAL_SPLIT);
        typeSelectionPane.setPreferredSize(new Dimension(150, 600));
        typeSelectionPane.setTopComponent(palettePane);
        typeSelectionPane.setBottomComponent(new ULCScrollPane(repositoryTreePane));
        typeSelectionPane.setOneTouchExpandable(true);
        typeSelectionPane.setDividerLocation(0.8);
        typeSelectionPane.setDividerSize(10);

        fContentView = new ULCSplitPane(ULCSplitPane.HORIZONTAL_SPLIT);
        fContentView.setPreferredSize(new Dimension(800, 600));
        fContentView.setLeftComponent(typeSelectionPane);
        fContentView.setRightComponent(new ULCScrollPane(modelEdit));
        fContentView.setOneTouchExpandable(true);
        fContentView.setDividerLocation(0.3);
        fContentView.setDividerSize(10);

        // layout
        modelEdit.add(ULCBoxPane.BOX_EXPAND_BOTTOM, ULCFiller.createVerticalStrut(3));
        modelEdit.add(ULCBoxPane.BOX_EXPAND_BOTTOM, separator);
        modelEdit.add(ULCBoxPane.BOX_EXPAND_EXPAND, fEditorArea);

        palettePane.add(ULCBoxPane.BOX_LEFT_TOP, filterView.getContent());
        palettePane.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCScrollPane(paletteArea));

        tabbedPane.addTab("Models",fModelRepositoryTree);
        repositoryTreePane.add(ULCBoxPane.BOX_EXPAND_EXPAND, tabbedPane);

        // attach listeners
        fEditorArea.addTabListener(new ITabListener() {
            public void tabClosing(TabEvent event) {
                int tabClosingIndex = event.getTabClosingIndex();
                ULCComponent component = event.getClosableTabbedPane().getComponentAt(tabClosingIndex);
                SingleModelMultiEditView modelView = fModelTabs.get(component);
                saveListeners.remove(modelView);
                fModelTabs.remove(component);
                event.getClosableTabbedPane().closeCloseableTab(tabClosingIndex);
                if (fEditorArea.getTabCount() > 0) {
                    event.getClosableTabbedPane().setSelectedIndex(0);
                }
                fRenameModelDialog.removeModelRenameListener(modelView);
            }
        });

        // dialog for renaming models
        fRenameModelDialog = new ModelRenameDialog(fContentView);
        fRenameModelDialog.addModelRenameListener(fModelRepositoryTree);
        fRenameModelDialog.addModelRenameListener(this);
    }

    /**
     * Create the palette pane.
     * @param filterView
     * @return
     */
    private ULCBoxPane getPalettePane(TabularFilterView filterView) {
        // initialize components
        ULCBoxPane viewSelector = new ULCBoxPane(false);
        ULCRadioButton categoryTreeSelectButton = new ULCRadioButton("Categories", true);
        ULCRadioButton packageSelectButton = new ULCRadioButton("Package");
        ULCRadioButton alphabeticalButton = new ULCRadioButton("Alphabetical");
        ULCButtonGroup buttonGroup = new ULCButtonGroup();
        packageSelectButton.setGroup(buttonGroup);
        categoryTreeSelectButton.setGroup(buttonGroup);
        alphabeticalButton.setGroup(buttonGroup);
        final ULCCardPane views = new ULCCardPane();
        final ComponentCategoryTree categoryTree = new ComponentCategoryTree(this);
        final ComponentTypeTree packagePalette = new ComponentTypeTree(this);
        final SortedComponentDefinitionsTree alphabeticalPalette = new SortedComponentDefinitionsTree(this);
        ULCBoxPane paletteArea = new ULCBoxPane(true);


        // layout
        viewSelector.add(ULCBoxPane.BOX_LEFT_CENTER, categoryTreeSelectButton);
        viewSelector.add(ULCBoxPane.BOX_LEFT_CENTER, alphabeticalButton);
        viewSelector.add(ULCBoxPane.BOX_LEFT_CENTER, packageSelectButton);
        viewSelector.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCFiller());
        views.addCard("categoryTree", categoryTree);
        views.addCard("packagePalette", packagePalette);
        views.addCard("alphabeticalPalette", alphabeticalPalette);
        paletteArea.add(ULCBoxPane.BOX_EXPAND_TOP, viewSelector);
        paletteArea.add(ULCBoxPane.BOX_EXPAND_EXPAND, views);

        // attach listeners
        filterView.addSearchListener(categoryTree);
        filterView.addSearchListener(packagePalette);
        filterView.addSearchListener(alphabeticalPalette);
        categoryTreeSelectButton.addActionListener(new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                views.setSelectedComponent(categoryTree);
            }
        });
        packageSelectButton.addActionListener(new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                views.setSelectedComponent(packagePalette);
            }
        });
        alphabeticalButton.addActionListener(new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                views.setSelectedComponent(alphabeticalPalette);
            }
        });

        return paletteArea;
    }

    /**
     * Show the type definition dialog - create the dialog if not yet instantiated
     */
    private void showTypeDefinitionDialog() {
        final TypeDefinitionDialog fTypeDefView = new TypeDefinitionDialog(UlcUtilities.getWindowAncestor(fEditorArea), fEditedTypeDefinitions);
        IActionListener newModelListener = new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                TypeDefinitionFormModel typeDefinitionFormModel = fTypeDefView.getBeanForm().getModel();
                if (typeDefinitionFormModel.hasErrors()) return;
                TypeDefinitionBean typeDef = typeDefinitionFormModel.getBean();
                AbstractGraphModel model = typeDef.getBaseType().equals(TypeDefinitionBean.MODEL) ? new ModelGraphModel() : new ComposedComponentGraphModel();
                model.setPackageName(typeDef.getPackageName());
                model.setName(typeDef.getName());
                addModel(model, typeDef, true);
                fTypeDefView.setVisible(false);
            }
        };
        fTypeDefView.getBeanForm().addSaveActionListener(newModelListener);
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        fTypeDefView.getTypeDefinitionForm().registerKeyboardAction(enter, newModelListener);
        fTypeDefView.getTypeDefinitionForm().addKeyListener();
        fTypeDefView.setVisible(true);
        ULCComponent nameTextField = fTypeDefView.getTypeDefinitionForm().getComponent("name");
        if (nameTextField != null)
            nameTextField.requestFocus();
    }

    /**
     * Returns the active help view - to show help entries for component definitions selected in the palette
     * @return
     */
    IHelpViewable getHelpView(){
        ULCComponent comp = fEditorArea.getSelectedComponent();
        return comp != null ? fModelTabs.get(comp).getHelpView() : null;
    }

    /**
     * Create toolbar using the toolbar factory and referring to the actions declared by an '@Action' annotations.
     * @return
     */
    public ULCToolBar getToolBar() {
        ULCToolBar designerToolBar = new ToolBarFactory(fContext.getActionMap(this)).createToolBar(
                "newModelAction", "importModelAction", "saveModelAction",
                "exportModelToGroovyAction", "createParametersAction",
                "importParametersAction", "exportParametersAction", "simulateAction",
                "exportModelToApplicationAction");

        // inserting separators shifts later items therefore the numbers look strange
        designerToolBar.add(new ULCToolBar.ULCSeparator(), 4);
        designerToolBar.add(new ULCToolBar.ULCSeparator(), 8);
        designerToolBar.add(new ULCToolBar.ULCSeparator(), 10);

        return designerToolBar;
    }

    /**
     * Creates a new tab for a given or a new model with given type definition
     * @param model
     * @param typeDef
     */
    public void addModel(AbstractGraphModel model, TypeDefinitionBean typeDef, boolean isEditable) {
        SingleModelMultiEditView modelView = new SingleModelMultiEditView(fContext, model, this);
        fModelTabs.put(modelView.getView(), modelView);
        fEditorArea.addTab(typeDef.getName(), modelView.getView());
        fEditorArea.setSelectedIndex(fEditorArea.getComponentCount() - 1);
        fEditorArea.setToolTipTextAt(fEditorArea.getComponentCount() - 1, model.getPackageName() + "." + model.getName());
        fEditedTypeDefinitions.add(typeDef);
        
        saveListeners.add(modelView);
        modelView.setGraphModelHandler(this);
        fRenameModelDialog.addModelRenameListener(modelView);
    }

    public void removeModel(AbstractGraphModel graphModel) {
        // remove it from the editor pane
        ULCComponent modelTab = null;
        for (Iterator<ULCComponent> it = fModelTabs.keySet().iterator(); it.hasNext() && modelTab==null;){
            modelTab = it.next();
            if (!fModelTabs.get(modelTab).getGraphModel().equals(graphModel)) {
                modelTab=null;
            }
        }
        fEditorArea.remove(modelTab);

        // remove it from the repository
        fModelRepositoryTree.removeModel(graphModel);

        // remove it from the repository
        getPersistenceService().delete(graphModel);
    }

    public void renameModel(AbstractGraphModel model) {
        fRenameModelDialog.setGraphModel(model);
        fRenameModelDialog.setVisible(true);
    }

    public GraphPersistenceService getPersistenceService() {
        if (fPersistenceService == null) {
            org.springframework.context.ApplicationContext ctx = ApplicationHolder.getApplication().getMainContext();
            fPersistenceService = ctx.getBean(GraphPersistenceService.class);
        }
        return fPersistenceService;
    }

    public void modelRenamed(AbstractGraphModel model, String oldName, String oldPackageName) {
        ULCComponent modelTab = null;
        for (Iterator<ULCComponent> it = fModelTabs.keySet().iterator(); it.hasNext() && modelTab==null;){
            modelTab = it.next();
            if (!fModelTabs.get(modelTab).getGraphModel().equals(model)) {
                modelTab=null;
            }
        }
        if (modelTab != null) {
            int index = fEditorArea.indexOfComponent(modelTab);
            fEditorArea.setTitleAt(index, model.getName());
        } 
    }
    
    @Action
    public void renameModelAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null) {
            renameModel(fModelTabs.get(comp).getGraphModel());
        }
    }
    
    
    
    /**
     * Adds the given parameterization to the selected model view.
     * May be used for imports of parameterizations.
     * @param p
     * @param name
     */
    private void addParameterSet(Parameterization p, String name) {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null && fModelTabs.containsKey(comp)) {
            SingleModelMultiEditView modelView = fModelTabs.get(comp);
            modelView.addParameterSet(p, name);
        } else {
            ULCAlert alert = new ULCAlert("No model view available", "Create or load the model view before you ingest the parametrization.", "ok");
            alert.show();
        }
    }

    /**
     * Import the component type specified by <code>clazzName</code>.
     * It means that for a given class (of type <code>ComposedComponent</code> or <code>AbstractModel</code>
     * a corresponding graph model is created and added to the model edit pane (as a new tab).
     *
     * @param clazzName name of the class to be imported
     * @return boolean to indicate whether the import was successful.
     * @throws ClassNotFoundException
     */
    public boolean importComponentType(String clazzName) throws ClassNotFoundException {
        Class clazz = PaletteService.getInstance().getComponentDefinition(clazzName).getTypeClass(); // getClass().getClassLoader().loadClass(clazzName);
        AbstractGraphImport importer = null;
        if (ComposedComponent.class.isAssignableFrom(clazz)) {
            importer = new ComposedComponentGraphImport();
        } else if (Model.class.isAssignableFrom(clazz)) {
            importer = new ModelGraphImport();
        }
        if (importer != null) {
            AbstractGraphModel model = importer.importGraph(clazz, null);
            TypeDefinitionBean typeDef = new TypeDefinitionBean();
            typeDef.setBaseType(model instanceof ModelGraphModel ? TypeDefinitionBean.MODEL : TypeDefinitionBean.COMPOSED_COMPONENT);
            typeDef.setName(model.getName());
            typeDef.setPackageName(model.getPackageName());
            addModel(model, typeDef, false);
            return true;
        }
        return false;
    }

    /**
     * Action for creating a new graph model and preparing a new tab in the editor pane.
     * Delegates to showTypDefinitionDialog.
     */
    @Action
    public void newModelAction() {
        showTypeDefinitionDialog();
    }

    /**
     * Deploys the model in RA application.
     */
    @Action
    public void exportToApplication() {
        if (fEditorArea.getComponentCount() == 0) {
            ULCAlert alert = new ULCAlert("No Model found", "No model or component to be saved", "ok");
            alert.show();
        } else {
            int i = fEditorArea.getSelectedIndex();
            ULCComponent component = fEditorArea.getComponentAt(i);
            if (fModelTabs.containsKey(component)) {
                AbstractGraphModel model = fModelTabs.get(component).getGraphModel();
                GraphModelUtilities.exportToApplication((ModelGraphModel) model);
            } else {
                ULCAlert alert = new ULCAlert("No Model found", "Current page does not contain a model or component specification.", "ok");
                alert.show();
            }

        }
    }

    @SuppressWarnings("serial")
    @Action
    public void importModelAction() {
        FileChooserConfig config = new FileChooserConfig();
        config.setDialogTitle("Open file");
        config.setDialogType(FileChooserConfig.FILES_ONLY);
        config.addFileFilterConfig(new FileChooserConfig.FileFilterConfig(
                new String[]{"groovy"}, "groovy files (*.groovy)")
        );

        IFileLoadHandler handler = new IFileLoadHandler() {
            public void onSuccess(InputStream[] inputStreams, String[] filePaths, String[] fileNames) {
                try {
                    InputStream in = inputStreams[0];
                    Writer writer = new StringWriter();
                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        in.close();
                    }
                    String content = writer.toString();
                    GraphImportService importService = new GraphImportService();
                    AbstractGraphModel model = importService.importGraph(content);
                    TypeDefinitionBean typeDef = new TypeDefinitionBean();
                    typeDef.setBaseType(model instanceof ModelGraphModel ? TypeDefinitionBean.MODEL : TypeDefinitionBean.COMPOSED_COMPONENT);
                    typeDef.setName(model.getName());
                    typeDef.setPackageName(model.getPackageName());
                    addModel(model, typeDef, true);
                } catch (Exception ex) {
                    new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported. Reason: " + ex.getMessage(), "Ok").show();
                }
            }

            public void onFailure(int reason, String description) {
                new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported.", "Ok").show();
            }
        };

        ClientContext.loadFile(handler, config, fEditorArea);
    }

    
    @Action
    public void createParametersAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null) {
            fModelTabs.get(comp).addParameterSet(null);
        }
    }

    @Action
    public void simulateAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null) {
            fModelTabs.get(comp).simulateAction(false);
        }
    }

    @Action
    public void saveModelAction() {
        SingleModelMultiEditView view = fModelTabs.get(fEditorArea.getSelectedComponent());
        if (view.isReadOnly()) {
            ULCAlert alert = new ULCAlert("Model is read only - cannot be saved.", "read-only models cannot be saved.", "ok");
            alert.show();
            return;
        }

        AbstractGraphModel model = view.getGraphModel();
        if (fModelRepositoryTree.getTreeModel().getModelNode(model) == null) {
            fModelRepositoryTree.getTreeModel().addNode(model);
        }
        try {
            getPersistenceService().save(model);
            for (ISaveListener saveListener : saveListeners) {
                saveListener.save();
            }
        } catch (Exception ex) {
            ULCAlert alert = new ULCAlert("Model not saved.", "Model could not be saved.", "ok");
            alert.show();
        }
    }

    @Action
    public void exportModelToGroovyAction() {
        AbstractGraphModel model = getSelectedModel();
        String text = GraphModelUtilities.getGroovyModelCode(model);
        FileStoreHandler.saveOutput(model.getName() + ".groovy", text, UlcUtilities.getWindowAncestor(fModelRepositoryTree));
    }

    private AbstractGraphModel getSelectedModel() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        AbstractGraphModel model = fModelTabs.get(comp).getGraphModel();
        return model;
    }


    @Action
    public void exportModelToApplicationAction() {
        SingleModelMultiEditView view = fModelTabs.get(fEditorArea.getSelectedComponent());
        AbstractGraphModel model = view.getGraphModel();

        try {
            // include it in the (RA) model registry
            GraphModelUtilities.exportToApplication(model);

            // declare the view as read only
            view.setReadOnly();

            // remove it from the repository and the repository tree
            fModelRepositoryTree.removeModel(model);

            //remove it from the (graph) model registry
            try {
                getPersistenceService().delete(model);
            } catch (GraphPersistenceException e) {
                //ignore, happens when model to be deployed was not persisted, so can't be deleted
            }
        } catch (Exception ex) {
            ULCAlert alert = new ULCAlert("Model not deployed.", "Model could not be deployed. Reason: " + ex.getMessage(), "ok");
            alert.show();
            LOG.error("Model could not be deployed", ex);
        }
    }

    @Action
    public void importParametersAction() {
        FileChooserConfig config = new FileChooserConfig();
        config.setDialogTitle("Choose Parameter File");
        config.setDialogType(FileChooserConfig.FILES_ONLY);
        config.addFileFilterConfig(new FileChooserConfig.FileFilterConfig(
                new String[]{"groovy"}, "groovy files (*.groovy)")
        );

        IFileLoadHandler handler = new IFileLoadHandler() {
            public void onSuccess(InputStream[] inputStreams, String[] filePaths, String[] fileNames) {
                try {
                    InputStream in = inputStreams[0];
                    String name = fileNames[0];
                    int suffixPos = name.indexOf(".groovy");
                    name = name.substring(0, suffixPos);
                    Writer writer = new StringWriter();
                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        in.close();
                    }
                    String content = writer.toString();
                    Parameterization params = ParameterUtilities.loadParametrization(content, name);
                    addParameterSet(params, name);
                } catch (Exception ex) {
                    new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported. Reason: " + ex.getMessage(), "Ok").show();
                }
            }

            public void onFailure(int reason, String description) {
                new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported.", "Ok").show();
            }
        };

        ClientContext.loadFile(handler, config, fEditorArea);
    }

    @Action
    public void exportParametersAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        Parameterization parameterization = fModelTabs.get(comp).getSelectedParametrization();
        AbstractGraphModel model = fModelTabs.get(comp).getGraphModel();
        if (parameterization == null) {
            ULCAlert alert = new ULCAlert("No data selected", "No data to export", "ok");
            alert.show();
            return;
        }
        ConfigObject configObject = parameterization.toConfigObject();
        configObject.put("model", model.getPackageName() + "." + model.getName());
        configObject.put("package", model.getPackageName());
        FileStoreHandler.saveOutput(parameterization.getName() + ".groovy", configObject, UlcUtilities.getWindowAncestor(fModelRepositoryTree));

    }
}
